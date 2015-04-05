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

import java.net.*;
import java.io.*;
import java.util.*;

public class JobTracker{

	static ZkConnector zkc;

	
	public static ServerSocket serverSocket = null;
	
	boolean isPrimary = false;
	static String JTServerInfo;	//IP:Port
	
	final static int JTport = 5000;
	//persistent directory paths
	final static String JOB_TRACKER= "/JobTracker";
	final static String WORKER = "/Worker";
	final static String CURRENT_JOB = "/CurrentJob";
	final static String JOBS = "/JobPool";
	final static String RESULT ="/Result";
	
	final static String PRIMARY = JOB_TRACKER+"/primary";
	
	//watch if primary down
	static Watcher PrimaryWatcher ;
	static Watcher CurrJobWatcher ;
	
	public static void main(String[] args) throws IOException{
        if (args.length != 1) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. JobTracker zkServer:clientPort");
            System.exit(-1);
        }        
        System.out.println("first point at coming");
        String hostName = InetAddress.getLocalHost().getHostAddress();
        JTServerInfo = hostName+":"+JTport;
        
        JobTracker JT = new JobTracker(args[0]);
        JT.setCurrJobWatch();
        JT.checkPrimary();
		
		
		while(JT.isPrimary==false){					//not primary, just wait
			 try{ Thread.sleep(1000); } catch (Exception e) {}

		}
		
        while (JT.isPrimary) { 		//becomes primary, can handle client
			System.out.println("Listening for client connection...");
			try {
				Thread.sleep(1000);
				// create a new thread to handle client connection.
        			new JTHandler(serverSocket.accept()).start();
			} catch (Exception e){
				System.out.println("Failed to create new client handler");
			}		   	            
            
        }
	}
	
	
	public void setCurrJobWatch(){
		zkc.getChildren(CURRENT_JOB,CurrJobWatcher);
	}
	
	
	public void checkPrimary(){
		Stat stat = zkc.exists(PRIMARY, PrimaryWatcher);
		
		if(stat == null){
		/*primary does not exist yet, try to be the primary */
				Code retcode = zkc.create(
				                PRIMARY,           // Path of znode
				                JTServerInfo,       // data
				                CreateMode.EPHEMERAL   // set to EPHEMERAL.
				       );
				if(retcode == Code.OK){
					isPrimary = true;
					
					System.out.println("I become a primary");
				}
				else{
					System.out.println("cannot become Primary for some reasons...");
								
				}
		}
		else
			System.out.println("someone already is primary");
	
	}
	
	
	
	
	public JobTracker(String host){
		try{
			serverSocket = new ServerSocket(JTport);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		
		PrimaryWatcher = new Watcher(){		//try to be primary
			 @Override
             public void process(WatchedEvent event) {
             	System.out.println("--- In jobTrackerWatcher ---");
             	EventType type = event.getType();
             	switch(type){
                      case NodeDeleted:
                      		System.out.println("previous primary being deleted");
                      		checkPrimary();
                      		break;

                }                       
             }
		
		};
		
		
		CurrJobWatcher = new Watcher(){
			@Override
			public void process(WatchedEvent event) {
				if(event.getType()==EventType.NodeChildrenChanged && isPrimary){
				
						List<String> children=zkc.getChildren(CURRENT_JOB);
						
						if(children.size()==0){	//current job is empty, can assign more
							List<String> NewJob = zkc.getChildren(JOBS);
							System.out.println("what's the child?   "+NewJob);
							if(NewJob.get(0) !=null){

								zkc.delete(JOBS+"/"+NewJob.get(0),-1);
							
								zkc.create(
							        JOBS+"/"+NewJob,         // Path of znode
							        NewJob.get(0),        // Data
							        CreateMode.PERSISTENT   // Znode type, set to PERSISTENT.
							        );	
							        
							    System.out.println("delete one from jobpool and create onto CurrJob");
							}
							else
								System.out.println("There is no job anymore");
						
						}
						
						
				}
				zkc.getChildren(CURRENT_JOB,CurrJobWatcher);	//re-enable watcher
			}
		
		};
		
		zkc = new ZkConnector();
		try{
			System.out.println("check the host-----" +host);
			zkc.connect(host);
			createPersistentFolders();
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
