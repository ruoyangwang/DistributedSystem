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
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public class JobTracker{

	static ZkConnector zkc;
	static String currhash;
	
	public static ServerSocket serverSocket = null;
	
	boolean isPrimary = false;
	static String JTServerInfo;	//IP:Port
	
	static int JTport;
	//persistent directory paths
	final static String JOB_TRACKER= "/JobTracker";
	final static String WORKER = "/Worker";
	final static String CURRENT_JOB = "/CurrentJob";
	final static String JOBS = "/JobPool";
	final static String RESULT ="/Result";
	final static String FILE_SERVER = "/FileServer";
	
	final static String PRIMARY = JOB_TRACKER+"/primary";
	
	//watch if primary down
	static Watcher PrimaryWatcher ;
	static Watcher CurrJobWatcher ;
	static Watcher CurrJobChildWatcher;
	public static void main(String[] args) throws IOException{
        if (args.length != 1) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. JobTracker zkServer:clientPort");
            System.exit(-1);
        }        
        JTport = 5000+ Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0])%7;
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
							String job = NewJob.get(0);
							if(job !=null){
								
								zkc.delete(JOBS+"/"+job,-1);
								currhash = job;
								zkc.create(
							        CURRENT_JOB+"/"+job,         // Path of znode
							        job,        // Data
							        CreateMode.PERSISTENT   // Znode type, set to PERSISTENT.
							        );	
							   	zkc.getChildren(CURRENT_JOB+"/"+job,CurrJobChildWatcher); 
							    System.out.println("delete one from jobpool and create onto CurrJob");
							}
							else
								System.out.println("There is no job anymore");
						
						}
						
						
				}
				zkc.getChildren(CURRENT_JOB,CurrJobWatcher);	//re-enable watcher
			}
		
		};
		
		
		CurrJobChildWatcher = new Watcher(){
			@Override
			public void process(WatchedEvent event) {
				try{
					Thread.sleep(1000);
				}catch(Exception e){
				
				}
				if(event.getType()==EventType.NodeChildrenChanged && isPrimary){
					System.out.println("******** ********* "+CURRENT_JOB+"/"+currhash);
					List<String> children=zkc.getChildren(CURRENT_JOB+"/"+currhash);
					Stat stat = zkc.exists(FILE_SERVER+"/total_workers",null);
					int count = Integer.parseInt(zkc.getData(FILE_SERVER+"/total_workers",null,stat));
					System.out.println("count of Workers now:  "+count+ "  "+children.size());
					if(children.size()==count){
						zkc.create(
						    RESULT+"/"+currhash,         // Path of znode
						    null,        // Data
						    CreateMode.PERSISTENT   // Znode type, set to PERSISTENT.
						);	
						for(String child: children){
							zkc.delete(CURRENT_JOB+"/"+currhash+"/"+child,-1);
						
						}
						zkc.delete(CURRENT_JOB+"/"+currhash,-1);
				
					}
					else
						zkc.getChildren(CURRENT_JOB+"/"+currhash,CurrJobChildWatcher); 
				}
				
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
		//createOnePersistentFolder(WORKER, null);

		// create jobpool folder
		createOnePersistentFolder(JOBS, "1");

		// create current job folder
		createOnePersistentFolder(CURRENT_JOB, null);
		
		// create result folder
		createOnePersistentFolder(RESULT, null);
    }


}
