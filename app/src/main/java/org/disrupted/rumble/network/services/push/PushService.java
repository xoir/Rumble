package org.disrupted.rumble.network.services.push;

import android.util.Log;

import org.disrupted.rumble.app.RumbleApplication;
import org.disrupted.rumble.database.DatabaseExecutor;
import org.disrupted.rumble.database.DatabaseFactory;
import org.disrupted.rumble.database.GroupDatabase;
import org.disrupted.rumble.database.StatusDatabase;
import org.disrupted.rumble.database.events.StatusDeletedEvent;
import org.disrupted.rumble.database.events.StatusInsertedEvent;
import org.disrupted.rumble.database.objects.StatusMessage;
import org.disrupted.rumble.network.protocols.ProtocolWorker;
import org.disrupted.rumble.network.protocols.command.SendStatusMessageCommand;
import org.disrupted.rumble.network.services.exceptions.ServiceNotStarted;
import org.disrupted.rumble.network.services.exceptions.WorkerAlreadyBinded;
import org.disrupted.rumble.network.services.exceptions.WorkerNotBinded;
import org.disrupted.rumble.util.HashUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import de.greenrobot.event.EventBus;

/**
 * @author Marlinski
 */
public class PushService {

    private static final String TAG = "PushService";

    private static final Object lock = new Object();
    private static PushService instance;

    private static ReplicationDensityWatcher rdwatcher;
    private static final Random random = new Random();

    private static Map<String, MessageDispatcher> workerIdentifierTodispatcher;

    private PushService() {
        rdwatcher = new ReplicationDensityWatcher(1000*3600);
    }

    public static void startService() {
        if(instance != null)
            return;

        synchronized (lock) {
            Log.d(TAG, "[.] Starting PushService");
            if (instance == null) {
                instance = new PushService();
                rdwatcher.start();
                workerIdentifierTodispatcher = new HashMap<String, MessageDispatcher>();
            }
        }
    }

    public static void stopService() {
        if(instance == null)
                return;
        synchronized (lock) {
            Log.d(TAG, "[-] Stopping PushService");
            for(Map.Entry<String, MessageDispatcher> entry : instance.workerIdentifierTodispatcher.entrySet()) {
                MessageDispatcher dispatcher = entry.getValue();
                dispatcher.interrupt();
            }
            instance.workerIdentifierTodispatcher.clear();
            rdwatcher.stop();
            instance = null;
        }
    }

    public static void bind(ProtocolWorker worker) throws ServiceNotStarted, WorkerAlreadyBinded{

        if(instance == null)
            throw new ServiceNotStarted();
        synchronized (lock) {
            MessageDispatcher dispatcher = instance.workerIdentifierTodispatcher.get(worker.getWorkerIdentifier());
            if (dispatcher != null)
                throw new WorkerAlreadyBinded();
            dispatcher =  new MessageDispatcher(worker);
            instance.workerIdentifierTodispatcher.put(worker.getWorkerIdentifier(), dispatcher);
            dispatcher.startDispatcher();
        }
    }

    public static void unbind(ProtocolWorker worker) throws ServiceNotStarted, WorkerNotBinded {
        if(instance == null)
            throw new ServiceNotStarted();

        synchronized (lock) {
            MessageDispatcher dispatcher = instance.workerIdentifierTodispatcher.get(worker.getWorkerIdentifier());
            if (dispatcher == null)
                throw new WorkerNotBinded();
            dispatcher.stopDispatcher();
            instance.workerIdentifierTodispatcher.remove(worker.getWorkerIdentifier());
        }

    }

    private static float computeScore(StatusMessage message, InterestVector interestVector) {
        //todo InterestVector for relevance
        float relevance = 0;
        float replicationDensity = rdwatcher.computeMetric(message.getUuid());
        float quality =  (message.getDuplicate() == 0) ? 0 : (float)message.getLike()/(float)message.getDuplicate();
        float age = (message.getTTL() <= 0) ? 1 : (1- (System.currentTimeMillis() - message.getTimeOfCreation())/message.getTTL());
        boolean distance = true;

        float a = 0;
        float b = (float)0.6;
        float c = (float)0.4;

        float score = (a*relevance + b*replicationDensity + c*quality)*age*(distance ? 1 : 0);

        return score;
    }

    // todo: not being dependant on age would make it so much easier ....
    private static class MessageDispatcher extends Thread {

        private static final String TAG = "MessageDispatcher";

        private ProtocolWorker worker;
        private InterestVector interestVector;
        private ArrayList<Integer> statuses;
        private float threshold;

        // locks for managing the ArrayList
        private final ReentrantLock putLock = new ReentrantLock(true);
        private final ReentrantLock takeLock = new ReentrantLock(true);
        private final Condition notEmpty = takeLock.newCondition();

        private boolean running;

        private StatusMessage max;

        private void fullyLock() {
            putLock.lock();
            takeLock.lock();
        }
        private void fullyUnlock() {
            putLock.unlock();
            takeLock.unlock();
        }
        private void signalNotEmpty() {
            final ReentrantLock takeLock = this.takeLock;
            takeLock.lock();
            try {
                notEmpty.signal();
            } finally {
                takeLock.unlock();
            }
        }

        public MessageDispatcher(ProtocolWorker worker) {
            this.running = false;
            this.worker = worker;
            this.max = null;
            this.threshold = 0;
            this.interestVector = null;
            statuses = new ArrayList<Integer>();
        }

        @Override
        public void run() {
            try {
                Log.d(TAG, "[+] MessageDispatcher initiated");
                do {
                    // pickup a message and send it to the CommandExecutor
                    if (worker != null) {
                        StatusMessage message = pickMessage();
                        Log.d(TAG, "message picked");
                        worker.execute(new SendStatusMessageCommand(message));
                        //todo just for the sake of debugging
                        sleep(1000, 0);
                    }

                } while (running);

            } catch (InterruptedException ie) {
            } finally {
                clear();
                Log.d(TAG, "[-] MessageDispatcher stopped");
            }
        }

        public void startDispatcher() {
            running = true;
            initStatuses();
        }

        public void stopDispatcher() {
            this.interrupt();
            running = false;
            worker = null;
            if(EventBus.getDefault().isRegistered(this))
                EventBus.getDefault().unregister(this);
        }

        private void initStatuses() {
            StatusDatabase.StatusQueryOption options = new StatusDatabase.StatusQueryOption();
            options.filterFlags |= StatusDatabase.StatusQueryOption.FILTER_GROUP;
            options.groupList = new ArrayList<String>();
            options.groupList.add(GroupDatabase.DEFAULT_PUBLIC_GROUP);
            options.filterFlags |= StatusDatabase.StatusQueryOption.FILTER_NEVER_SEND;
            options.peerName = HashUtil.computeForwarderHash(
                    worker.getLinkLayerConnection().getRemoteLinkLayerAddress(),
                    worker.getProtocolIdentifier());
            options.query_result = StatusDatabase.StatusQueryOption.QUERY_RESULT.LIST_OF_IDS;
            DatabaseFactory.getStatusDatabase(RumbleApplication.getContext()).getStatuses(options, onStatusLoaded);
        }
        DatabaseExecutor.ReadableQueryCallback onStatusLoaded = new DatabaseExecutor.ReadableQueryCallback() {
            @Override
            public void onReadableQueryFinished(Object result) {
                if (result != null) {
                    Log.d(TAG, "[+] MessageDispatcher initiated");
                    final ArrayList<Integer> answer = (ArrayList<Integer>)result;
                    for (Integer s : answer) {
                        StatusMessage message = DatabaseFactory.getStatusDatabase(RumbleApplication.getContext())
                                .getStatus(s);
                        if(message != null) {
                            add(message);
                            message.discard();
                        }
                    }
                    EventBus.getDefault().register(MessageDispatcher.this);
                    start();
                }
            }
        };

        private void clear() {
            fullyLock();
            try {
                if(EventBus.getDefault().isRegistered(this))
                    EventBus.getDefault().unregister(this);
                statuses.clear();
            } finally {
                fullyUnlock();
            }
        }

        private boolean add(StatusMessage message){
            final ReentrantLock putlock = this.putLock;
            putlock.lock();
            try {
                float score = computeScore(message, interestVector);

                if (score <= threshold) {
                    message.discard();
                    return false;
                }

                statuses.add((int)message.getdbId());

                if (max == null) {
                    max = message;
                } else {
                    float maxScore = computeScore(max, interestVector);
                    if (score > maxScore) {
                        max.discard();
                        max = message;
                    } else
                        message.discard();
                }

                signalNotEmpty();
                return true;
            } finally {
                putlock.unlock();
            }
        }

        // todo: iterating over the entire array, the complexity is DAMN TOO HIGH !!
        private void updateMax() {
            float maxScore = 0;
            if(max != null) {
                maxScore = computeScore(max, interestVector);
                if(maxScore > threshold)
                    return;
            }

            Iterator<Integer> it = statuses.iterator();
            while(it.hasNext()) {
                Integer id = it.next();
                StatusMessage message = DatabaseFactory.getStatusDatabase(RumbleApplication.getContext())
                        .getStatus(id);
                float score = computeScore(max, interestVector);
                if(score <= threshold) {
                    message.discard();
                    statuses.remove(Integer.valueOf((int)message.getdbId()));
                    continue;
                }

                if(max == null) {
                    max = message;
                    maxScore = score;
                    continue;
                }

                if(score > maxScore) {
                    max.discard();
                    max = message;
                    maxScore = score;
                } else
                    message.discard();
            }
        }

        /*
         *  See the paper:
         *  "Roulette-wheel selection via stochastic acceptance"
         *  By Adam Lipowski, Dorota Lipowska
         */
        private StatusMessage pickMessage() throws InterruptedException {
            final ReentrantLock takelock = this.takeLock;
            final ReentrantLock putlock = this.takeLock;
            StatusMessage message;
            boolean pickup = false;
            takelock.lockInterruptibly();
            try {
                do {
                    while (statuses.size() == 0)
                        notEmpty.await();

                    putlock.lock();
                    try {
                        updateMax();

                        // randomly pickup an element homogeneously
                        int index = random.nextInt(statuses.size());
                        long id = statuses.get(index);
                        message = DatabaseFactory.getStatusDatabase(RumbleApplication.getContext()).getStatus(id);

                        // get max probability Pmax
                        float maxScore = computeScore(max, interestVector);
                        // get element probability Pu
                        float score = computeScore(message, interestVector);

                        if (score <= threshold) {
                            // the message is not valid anymore, that should happen very rarely
                            statuses.remove(Integer.valueOf((int)message.getdbId()));
                            message.discard();
                            message = null;
                            continue;
                        }

                        int shallwepick = random.nextInt((int) (maxScore * 1000));
                        if (shallwepick <= (score * 1000)) {
                            // we keep this status with probability Pu/Pmax
                            statuses.remove(Integer.valueOf((int)message.getdbId()));
                            pickup = true;
                        } else {
                            // else we pick another one
                            message.discard();
                        }
                    } finally {
                        putlock.unlock();
                    }
                } while(!pickup);
            } finally {
                takelock.unlock();
            }
            return message;
        }

        public void onEvent(StatusDeletedEvent event) {
            fullyLock();
            try {
                statuses.remove(Integer.valueOf((int) event.dbid));
            } finally {
                fullyUnlock();
            }
        }

        public void onEvent(StatusInsertedEvent event) {
            add(event.status);
        }
    }
}
