import java.net.*;
import java.io.*;
import java.util.*;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.CreateMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
public class WorkerHandler extends Thread {
		
		public String answer;
		private int size_queue;

		public WorkerHandler(){
			super("Handle decodeing");
			size_queue=0;
		}

		public void run(){
		//TODO dequeue task queue and decode md5 and compare
			//for loop decode one by on
			//
			System.out.println("worker handler running");
			String word=null;
			while(true){
				synchronized(Worker.taskqueue){
				word=Worker.taskqueue.poll();
				}
				if(word!=null){
					//System.out.println("get a word "+word);
					/*
					try{
					Thread.sleep(50);
					
					}catch(Exception e){}
					*/
					if(MD5Decode(word)){
						//send answer
						Worker.SendAnswer(answer);
					}
					size_queue=size_queue+1;
				}
				/*
				synchronized(Worker.taskqueue){
				size_queue=Worker.taskqueue.size();
				}
				*/
				if (size_queue==Worker.queuesize&&Worker.queuesize!=0){
					//try{}catch(Exception e){}
					Worker.setDone();
					size_queue=0;
					Worker.queuesize=0;
					System.out.println("fail to decode, cant find password");
				}
			}
		}
		
		public boolean MD5Decode(String word){
			String hash = null;
        		try {
            			MessageDigest md5 = MessageDigest.getInstance("MD5");
            			BigInteger hashint = new BigInteger(1, md5.digest(word.getBytes()));
            			hash = hashint.toString(16);
            			while (hash.length() < 32) hash = "0" + hash;
        		} catch (NoSuchAlgorithmException nsae) {
            		// ignore
        		}
			if(Worker.CurJob.equals(hash)){
				answer=word;
				System.out.println("decode successfully "+answer);
				Worker.taskqueue.clear();
				try{
					Thread.sleep(20000);
					
					}catch(Exception e){}

				return true;
			}
			else
				return false;
		}
}
