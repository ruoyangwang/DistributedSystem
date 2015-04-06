
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
import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.net.*;

import java.io.IOException;

public class FileServer {
    
    final static String FS = "/FileServer";
    final static String primary_FS= FS+"/primary";
    final static String total_Workers=FS+"/total_workers";
    final static String Workers="/Worker";
    ZkConnector zkc;
    static Watcher primary_watcher;
    static Watcher worker_watcher;
    public static ServerSocket serverSocket = null;

    //use to record the num of working workers
    static int worker_count=0;
    //use to record the num of workers that has assigned job
    static int worker_assigned=0;
    static Stirng FSInfo;
    static String FSport;

    public static void main(String[] args) {
      
        if (args.length != 1) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. FileServer zkServer:clientPort");
            return;
        }

        FileServer t = new FileServer(args[0]);   
 
        System.out.println("Sleeping...");
        try {
            Thread.sleep(5000);
        } catch (Exception e) {}
        
        t.checkpath();
        
        System.out.println("Sleeping...");
        while (true) {
            try{ Thread.sleep(5000); } catch (Exception e) {}
        }
    }

    public FileServer(String hosts) {
	
	String hostName = InetAddress.getLocalHost().getHostAddress();
	FSInfo=hostName+":";
    	FSport=null;
        zkc = new ZkConnector();
        try {
            zkc.connect(hosts);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
        }
 
        primary_watcher = new Watcher() { // Anonymous Watcher
                            @Override
                            public void process(WatchedEvent event) {
                                handlePrimaryEvent(event);                
                            } };
	worker_watcher = new Watcher() {
			    @Override
			    public void process(WatchedEvent event) {
			    	handleWorkerEvent(event);
			    } };
    }
    
    private void checkpath() {
        Stat stat = zkc.exists(primary_FS, watcher);
        if (stat == null) {              // znode doesn't exist; let's try creating it
	    //if(FSport==null){
	    serverSocket =new ServerSocket(0);
	    FSport=serverSocket.getLocalPort();
	    FSInfo=FSInfo+FSport;
	    System.out.println("FS server is using "+FSInfo);
	    //}
	    //else{
	    //serverSocket =new ServerSocket(0)
	    //}
            System.out.println("Creating " + myPath);
            Code ret = zkc.create(
                        primary_FS,         // Path of znode
                        FSInfo,           // Data not needed.
                        CreateMode.EPHEMERAL   // Znode type, set to EPHEMERAL.
                        );
            if (ret == Code.OK) System.out.println("the primary!");
	    //wait for workers to connect to
	    zkc.getChildren(Workers,workerwatcher);
	    waitforconnent();
        }

	//here get the dictionary file
    }

    private void waitforconnect(){
    	while (true) { 		//becomes primary, can handle client
			System.out.println("Listening for workers' connections...");
			try {
				//Thread.sleep(1000);
				// create a new thread to handle client connection.
        			new FSHandler(serverSocket.accept()).start();
			} catch (Exception e){
				System.out.println("Failed to create new client handler");
			}		   	            
            
        }
	serverSocket.close();

    }

    private void handlePrimaryEvent(WatchedEvent event) {
        String path = event.getPath();
        EventType type = event.getType();

	//here handle leader election event type
        if(path.equalsIgnoreCase(primary_FS)) {
            if (type == EventType.NodeDeleted) {
                System.out.println(primary_FS + " deleted! Let's go!");       
                checkpath(); // try to become the boss
            }
            if (type == EventType.NodeCreated) {
                System.out.println(primary_FS + " created!");       
                try{ Thread.sleep(5000); } catch (Exception e) {}
                checkpath(); // re-enable the watch
            }
        }

	//here handle other event type
	//else(path.equalsIgnoreCase())
	//here handle workers join type

	//here handle fetch dictionary partition event type


    }

    private void handleWorkerEvent(WatchedEvent event) {
    	String path = event.getPath();
        EventType type = event.getType();
	
	if(event.getType()==EventType.NodeChildrenChanged){
		
		List<String> children=zkc.getChildren(Workers);
						
		if(children.size()!=0){	//if workers exist
			worker_count=children.size().toString();
			Stat stat = null;
			while(stat == null){
			stat = zkc.exists(total_Workers, null);
			try{
				Thread.sleep(200);
			}catch(Exception e){};
			}
			zkc.setData(total_Workers,worker_count,-1);
		}
						
	}
		zkc.getChildren(Workers,workerwatcher);	//re-enable watcher
			
    }

   private void createPersistentFolders(){
		// 
		
		// create worker folder
		//createOnePersistentFolder(WORKER, null);
	}

}
