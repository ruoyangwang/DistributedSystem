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

import java.io.*;
import java.net.*;

public class JobTracker{

	private static ZkConnector zkc;
	Watcher CurrJobWatcher;
	Watcher Primary;
	
	static boolean primary = false;
	static String JTServerInfo;	//IP:Port
	
	//persistent directory paths
	final static String JOB_TRACKER= "/JobTracker";
	final static String WORKER = "/Worker";
	final static String CURRENT_JOB = "/CurrentJob";
	final static String JOBS = "/JobPool";
	final static String RESULT ="/Result";


	public static void main(String[] args) throws IOException{
        if (args.length != 1) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. JobTracker zkServer:clientPort");
            System.exit(-1);
        }        
        
        JobTracker JT = new JobTracker(args[0]);
	}
	
	public JobTracker(String host){
		zkc = new ZkConnector();
		try{
			zkc.connect(host);
		}catch(Exception e){
			e.printStackTrace();
		}
	
	}


	private static synchronized void createOnePersistentFolder(String path, String value){	
 		Stat stat = zkc.exists(path,null);
        if (stat == null) { 
	        System.out.println("Creating " + path);
	        Code ret = zkc.create(
	                    path,         // Path of znode
	                    value,        // Data
	                    CreateMode.PERSISTENT   // Znode type, set to PERSISTENT.
	                    );			
	        if (ret == Code.OK) {
				System.out.println(path.toString()+" path created!");
	   	 	} else {
				System.out.println(path.toString()+" path creation failed!");
			}
        } else {
			System.out.println(path.toString() + " path already exists");
		}
    }

	private void createPersistentFolders(){
		// create jobTracker folder
		createOnePersistentFolder(JOB_TRACKER, null);
		
		// create worker folder
		createOnePersistentFolder(WORKER, null);

		// create jobpool folder
		createOnePersistentFolder(JOBS, "1");

		// create current job folder
		createOnePersistentFolder(CURRENT_JOB, null);
		
		// create result folder
		createOnePersistentFolder(RESULT, null);
    }










}
