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

package org.disrupted.rumble.message;


import android.util.Log;
import android.webkit.MimeTypeMap;

import org.disrupted.rumble.util.HashUtil;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Marlinski
 */
public class StatusMessage extends Message {

    private static final String TAG  = "StatusMessage";
    public  static final String TYPE = "STATUS";

    protected String      uuid;
    protected Integer     score;
    protected String      author;
    protected String      status;
    protected Set<String> hashtagSet;
    protected String      attachedFile;
    protected long        fileSize; // move it to file database
    protected long        timeOfCreation;
    protected long        timeOfArrival;
    protected Integer     hopCount;
    protected long        ttl;
    protected Integer     like;
    protected Integer     replication;
    protected boolean     read;
    protected Set<String> forwarderList;

    public StatusMessage(String post, String author, long timeOfCreation) {
        this.messageType = TYPE;

        this.status   = post;
        this.author = author;
        hashtagSet  = new HashSet<String>();
        Pattern hashtagPattern = Pattern.compile("#(\\w+|\\W+)");
        Matcher hashtagMatcher = hashtagPattern.matcher(post);
        hashtagSet  = new HashSet<String>();
        while (hashtagMatcher.find()) {
            hashtagSet.add(hashtagMatcher.group(0));
        }

        attachedFile   = "";
        fileSize       = 0;
        this.timeOfCreation = timeOfCreation;
        timeOfArrival  = (System.currentTimeMillis() / 1000L);
        hopCount       = 0;
        forwarderList  = new HashSet<String>();
        score          = 0;
        ttl            = 0;
        like           = 0;
        replication    = 0;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(author.getBytes());
            md.update(post.getBytes());
            md.update(ByteBuffer.allocate(8).putLong(timeOfCreation).array());
            byte[] digest = md.digest();
            uuid = new String(digest).substring(0,16);
        }
        catch (NoSuchAlgorithmException ignore) {}
    }

    public String  getUuid() {              return this.uuid; }
    public Integer getScore(){              return this.score;}
    public String  getAuthor(){             return this.author; }
    public String  getPost(){               return this.status; }
    public Set<String> getHashtagSet(){     return this.hashtagSet; }
    public long  getTimeOfCreation(){       return this.timeOfCreation; }
    public long  getTimeOfArrival(){        return this.timeOfArrival; }
    public Integer getHopCount(){           return this.hopCount; }
    public Set<String> getForwarderList(){  return this.forwarderList; }
    public long getTTL(){                   return this.ttl;}
    public String  getFileName(){           return this.attachedFile; }
    public long    getFileSize(){           return this.fileSize; }
    public long    getFileID(){             return 0; }
    public long    getLike(){               return like; }
    public long    getReplication(){        return replication; }

    public void setUuid(String uuid) {            this.uuid = uuid;               }
    public void setFileName(String filename){     this.attachedFile   = filename; }
    public void setFileSize(long size) {          this.fileSize       = size;     }
    public void setTimeOfCreation(long toc){      this.timeOfCreation = toc;      }
    public void setTimeOfArrival(long toa){       this.timeOfArrival  = toa;      }
    public void setHopCount(Integer hopcount){    this.hopCount       = hopcount; }
    public void setScore(Integer score){          this.score          = score;    }
    public void setLike(Integer like){            this.like           = like;    }
    public void setTTL(long ttl){              this.ttl            = ttl;      }
    public void addHashtag(String tag){           this.hashtagSet.add(tag);       }
    public void setHashtagSet(Set<String> hashtagSet) {
        if(hashtagSet.size() > 0)
            hashtagSet.clear();
        this.hashtagSet = hashtagSet;
    }
    public void setReplication(Integer replication){ this.replication    = replication; }
    public void setRead(boolean read){            this.read = read; }
    public void setForwarderList(Set<String> fl){
        if(forwarderList.size() > 0)
            forwarderList.clear();
        this.forwarderList  = fl;
    }
    public void addForwarder(String macAddress, String protocolID) {
        forwarderList.add(HashUtil.computeHash(macAddress,protocolID));
    }


    public boolean hasBeenReadAlready(){ return read; }
    public boolean hasAttachedFile() {
        return (attachedFile != "");
    }
    public boolean isForwarder(String macAddress, String protocolID) {
        return forwarderList.contains(HashUtil.computeHash(macAddress,protocolID));
    }

    public String toString() {
        String s = new String();
        s += "Author: "+this.author+"\n";
        s += "Status:" +this.status+"\n";
        s += "Time:" +this.timeOfCreation+"\n";
        return s;
    }
}