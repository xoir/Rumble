/*
 * Copyright (C) 2014 Disrupted Systems
 *
 * This file is part of Rumble.
 *
 * Rumble is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rumble is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Rumble.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.disrupted.rumble.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.acl.Group;

/**
 * @author Marlinski
 */
public class DatabaseFactory {

    private static final String TAG = "DatabaseFactory";

    private static final int DATABASE_VERSION  = 1;
    private static final String DATABASE_NAME  = "rumble.db";
    private static final Object lock           = new Object();

    private static DatabaseFactory instance;

    private DatabaseHelper             databaseHelper;
    private final StatusDatabase       statusDatabase;
    private final HashtagDatabase      hashtagDatabase;
    private final StatusTagDatabase    statusTagDatabase;
    private final GroupDatabase        groupDatabase;
    private final SubscriptionDatabase subscriptionDatabase;
    private final ContactDatabase      contactDatabase;
    private final ForwarderDatabase    forwarderDatabase;
    private DatabaseExecutor           databaseExecutor;

    public static DatabaseFactory getInstance(Context context) {
        synchronized (lock) {
            if (instance == null)
                instance = new DatabaseFactory(context);

            return instance;
        }
    }

    public static StatusDatabase getStatusDatabase(Context context) {
            return getInstance(context).statusDatabase;
    }
    public static HashtagDatabase getHashtagDatabase(Context context) {
        return getInstance(context).hashtagDatabase;
    }
    public static StatusTagDatabase getStatusTagDatabase(Context context) {
        return getInstance(context).statusTagDatabase;
    }
    public static GroupDatabase getGroupDatabase(Context context) {
        return getInstance(context).groupDatabase;
    }
    public static SubscriptionDatabase getSubscriptionDatabase(Context context) {
        return getInstance(context).subscriptionDatabase;
    }
    public static ContactDatabase getContactDatabase(Context context) {
        return getInstance(context).contactDatabase;
    }
    public static ForwarderDatabase getForwarderDatabase(Context context) {
        return getInstance(context).forwarderDatabase;
    }
    public static DatabaseExecutor getDatabaseExecutor(Context context) {
        return getInstance(context).databaseExecutor;
    }


    private DatabaseFactory(Context context) {
        this.databaseHelper       = new DatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.statusDatabase       = new StatusDatabase(context, databaseHelper);
        this.hashtagDatabase      = new HashtagDatabase(context, databaseHelper);
        this.statusTagDatabase    = new StatusTagDatabase(context, databaseHelper);
        this.groupDatabase        = new GroupDatabase(context, databaseHelper);
        this.contactDatabase      = new ContactDatabase(context, databaseHelper);
        this.subscriptionDatabase = new SubscriptionDatabase(context, databaseHelper);
        this.forwarderDatabase    = new ForwarderDatabase(context, databaseHelper);
        this.databaseExecutor     = new DatabaseExecutor();
    }

    public void reset(Context context) {
        DatabaseHelper old = this.databaseHelper;
        this.databaseHelper = new DatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);

        this.contactDatabase.reset(databaseHelper);
        this.statusDatabase.reset(databaseHelper);
        this.hashtagDatabase.reset(databaseHelper);
        this.statusTagDatabase.reset(databaseHelper);
        this.groupDatabase.reset(databaseHelper);
        this.subscriptionDatabase.reset(databaseHelper);
        this.forwarderDatabase.reset(databaseHelper);
        old.close();
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //nothing for the moment
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(ContactDatabase.CREATE_TABLE);
            db.execSQL(GroupDatabase.CREATE_TABLE);
            db.execSQL(StatusDatabase.CREATE_TABLE);
            db.execSQL(HashtagDatabase.CREATE_TABLE);
            db.execSQL(StatusTagDatabase.CREATE_TABLE);
            db.execSQL(SubscriptionDatabase.CREATE_TABLE);
            db.execSQL(ForwarderDatabase.CREATE_TABLE);

            executeStatements(db, StatusTagDatabase.CREATE_INDEXS);
        }

        private void executeStatements(SQLiteDatabase db, String[] statements) {
            for (String statement : statements)
                db.execSQL(statement);
        }

    }
}
