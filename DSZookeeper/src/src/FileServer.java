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
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
public class FileServer {
    
    final static String FS = "/FileServer";
    final static String primary_FS= FS+"/primary";
    final static String total_Workers=FS+"/total_workers";
    final static String Workers="/Workers";
    static ZkConnector zkc;
    private static Watcher primary_watcher;
    private static Watcher worker_watcher;
    public static ServerSocket serverSocket = null;

    //use to record the num of working workers
    static int worker_count=0;
    //use to record the num of workers that has assigned job
    static int worker_assigned=0;
    static String FSInfo;
    static int FSport;
    public static String[] Dictionary;
    static int line_num=0;
    static boolean flag;
    static String dictionary_path;
    public static void main(String[] args) {
      
        if (args.length != 2) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. FileServer zkServer:clientPort path_to_dictionary");
            return;
        }
	//dictionary_path="./lowercase.rand";
	dictionary_path=args[1];
        FileServer t = new FileServer(args[0]);   
 	//dictionary_path=args[1];
	
        //System.out.println("Sleeping...");
        //try {
         //   Thread.sleep(5000);
        //} catch (Exception e) {}
        
        t.checkpath();
        
        System.out.println("Sleeping...");
	/*
        while (true) {
            try{ Thread.sleep(5000); } catch (Exception e) {}
        }
	*/
	while(true){
	
        while (!flag) {
            try{ Thread.sleep(5000); } catch (Exception e) {}
        }
	
	//FSLisenter();
	waitforconnect();
	//flag=false;
	}
    }

    public FileServer(String hosts) {
	readDictionary();
	flag=false;
        zkc = new ZkConnector();
        try {
	    String hostName = InetAddress.getLocalHost().getHostAddress();
	    FSInfo=hostName+":";
    	    FSport=0;
            zkc.connect(hosts);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
        }
	System.out.println("create a fs==");
 	createPersistentFolders();
        primary_watcher = new Watcher() { // Anonymous Watcher
                            @Override
                            public void process(WatchedEvent event) {
                                handlePrimaryEvent(event);                
                            } };
	worker_watcher = new Watcher() {
			    @Override
			    public void process(WatchedEvent event) {
			    	System.out.println("catch a worker event");
			    	handleWorkerEvent(event);
			    } };
	System.out.println("create a fs");
    }
    
    private void checkpath() {
    	System.out.println("check path");
        Stat stat = zkc.exists(primary_FS, primary_watcher);
        if (stat == null) {              // znode doesn't exist; let's try creating it
	    //if(FSport==null){
	    try{
	    serverSocket =new ServerSocket(0);
	    FSport=serverSocket.getLocalPort();
	    FSInfo=FSInfo+FSport;
	    System.out.println("FS server is using "+FSInfo);
	    //}
	    //else{
	    //serverSocket =new ServerSocket(0)
	    //}
	    }catch (Exception e){
	    System.out.println("Zookeeper checkpath "+ e.getMessage());
	    }
            System.out.println("Creating " + primary_FS);
            Code ret = zkc.create(
                        primary_FS,         // Path of znode
                        FSInfo,           // Data not needed.
                        CreateMode.EPHEMERAL   // Znode type, set to EPHEMERAL.
                        );
            if (ret == Code.OK) System.out.println("the primary!");
	    //get the worker count from zookeeper
	    stat=zkc.exists(total_Workers,null);
	    if(stat!=null){
	    	String works_num=zkc.getData(total_Workers,null,stat);
		if(works_num!=null){
		worker_count=Integer.parseInt(works_num);
		System.out.println("update worker count "+worker_count);
		}
		else 
		System.out.println("cant update worker count"); 
	    }
	    //wait for workers to connect to
	    zkc.getChildren(Workers,worker_watcher);
	    //waitforconnect();
	    flag=true;
        }

	//here get the dictionary file
    }

    private static void waitforconnect(){
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
	//try{
	//serverSocket.close();
	//}catch (Exception e){}

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
	System.out.println("got a worker event "+path);
	if(event.getType()==EventType.NodeChildrenChanged){
		
		List<String> children=zkc.getChildren(Workers);
						
		if(children.size()!=0){	//if workers exist
			System.out.println("children size is "+children.size());
			worker_count=children.size();
			Stat stat = null;
			while(stat == null){
			stat = zkc.exists(total_Workers, null);
			try{
				Thread.sleep(200);
			}catch(Exception e){};
			}
			zkc.setData(total_Workers,Integer.toString(worker_count),-1);
		}
						
	}
		zkc.getChildren(Workers,worker_watcher);	//re-enable watcher
			
    }

   private void createPersistentFolders(){
		// 
		
		// create worker folder
		createOnePersistentFolder(FS, null);
		createOnePersistentFolder(Workers, null);
		createOnePersistentFolder(total_Workers,null);
	}

   public void readDictionary(){
    	System.out.println("reading dictionary... "+ dictionary_path);
	BufferedReader br=null;
	try {
	File fin=new File(dictionary_path);
    	br = new BufferedReader(new FileReader(fin));
	if(br==null) System.out.println("no file to read");
	List<String> temps = new ArrayList<String>();

        String line = br.readLine();

        while (line != null) {
	    //System.out.println(line);
            temps.add(line);
            line = br.readLine();
        }
        Dictionary = temps.toArray(new String[0]);
	line_num=Dictionary.length;
	System.out.println("Finish reading, it has "+line_num+" lines");
    	}
	catch(Exception e){}
	finally {
	if(br!=null){
	try{
        br.close();
	}catch (Exception e){}
	}
	if(line_num==0){
	System.out.println("file empty or file not found, exiting");
	System.exit(1);
	}
    	}
    }

   static synchronized void createOnePersistentFolder(String path, String value){	
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

    public static void workerbackup(String worker){
    workerData workerdata=FSHandler.workerMap.get(worker);
    
    int from=0;
    int to =0;
    from =workerdata.from;
    to = workerdata.to;
    FSHandler.workerMap.remove(worker);
    if(to!=0&&from!=to){
	  String takeover = FSHandler.workerMap.keySet().iterator().next();	
	  System.out.println(takeover+" take over the job "+from+" "+to);
       	 workerData data = FSHandler.workerMap.get(takeover);
	 if(data!=null){
	 	zkPacket pkttoworker=new  zkPacket();
		pkttoworker.type=zkPacket.FSJOB_ASSIGN;
		pkttoworker.jobid=FSHandler.JobID;
		pkttoworker.dictionary=new String[to-from];
		System.arraycopy(Dictionary,from,pkttoworker.dictionary,0,to-from);
		try{
		data.toClient.writeObject(pkttoworker);
		}catch(Exception e){}

	 }
	 else
		System.out.println(" i cant find worker");
    }
    else
    	System.out.println("cant find worker");
    } 
}
