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
import java.util.concurrent.BlockingQueue;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
public class Worker {
    
    final static String Workers = "/Workers";
    //final static String Workers="/Worker";
    final static String FS = "/FileServer/primary";
    final static String CURRENT_JOB = "/CurrentJob";
    final static String Result="/Result";
    private static String myWorker;
    public static ZkConnector zkc;
    private static Watcher fs_watcher;
    private static Watcher job_watcher;
    public static Socket workerSocket = null;
    static ObjectInputStream ins=null;
    static ObjectOutputStream outs = null;
    //static BlockingQueue<String> clientQueue=new ArrayBlockingQueue<String>;;
    static ConcurrentLinkedQueue<String> taskqueue = new ConcurrentLinkedQueue<String>();
    //My dictionary
    //public static String[] Dictionary;
    //static int line_num=0;
    static zkPacket packetFromFS;
    static String CurJob;
    //my worker handler
    private WorkerHandler myHandler;
    static boolean flag;
    static int queuesize;
    static String start;
    //task queue
    //a string thread safe fifo queue
    public static void main(String[] args) {
      
        if (args.length != 1) {
            System.out.println("one Argument hostaddr:port#");
	    return;
        }
	try{
        Worker t = new Worker(args[0]);   
	}catch (Exception e){
		e.printStackTrace();
	}
	System.out.println("Sleeping...");
	while(true){
        while (!flag) {
            try{ Thread.sleep(5000); } catch (Exception e) {}
        }
	FSLisenter();
	flag=false;
	}
     }

    public Worker(String hosts) throws Exception{
    	flag=false;
	start=null;
    	packetFromFS=new zkPacket();
	zkc = new ZkConnector();
        try {
            zkc.connect(hosts);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
	    System.exit(1);
        }
 	//createPersistentFolders();
        job_watcher = new Watcher() { // Anonymous Watcher
                            @Override
                            public void process(WatchedEvent event) {
                                handleJobEvent(event);                
                            } };
	fs_watcher = new Watcher() {
			    @Override
			    public void process(WatchedEvent event){
			    	handleFSEvent(event);
			    } };
	//workerhandler
	
	myHandler=new WorkerHandler();
	myHandler.start();
	//readDictionary();
	getFS();
	//workerReg();
	createMyWorker();
	workerReg();
	FSLisenter();
	}

    private void getFS(){
   	System.out.println("enter getFS");
   	Stat stat=null;
	while(stat == null){
		stat=zkc.exists(FS, fs_watcher);
		try{
			Thread.sleep(200);
		}catch(Exception e){};		
	}
	String data = zkc.getData(FS,null,stat);
	String [] token = data.split(":");
	String FSip = token[0];
	String FSport = token[1];
	if(FSconnection(FSip, Integer.parseInt(FSport))==false){
		System.out.println("cannot connect to FileServer, please try again");
		System.exit(-1);
	}
	System.out.println("Connection Established");
	zkc.exists(FS, fs_watcher);//re-enable the watch
   }
    
    private boolean FSconnection(String ip, int port){
    	System.out.println("try to connect to FS at "+ip+":"+port);
    	try {
			workerSocket = new Socket(ip, port);
			outs = new ObjectOutputStream(workerSocket.getOutputStream());
			ins = new ObjectInputStream(workerSocket.getInputStream());
			return true;
			
	}catch(Exception e) {
			e.printStackTrace();
			return false;
		}

    }

    public void handleJobEvent(WatchedEvent event){
    //sent job request to FS
    	String path = event.getPath();
        EventType type = event.getType();
	System.out.println(CURRENT_JOB + " created! "+path);
	//if(path.equalsIgnoreCase(CURRENT_JOB)) {
            if (type == EventType.NodeDeleted) {
                System.out.println(CURRENT_JOB+ " deleted! wait for a new one!");
		//TODO clear the task queue and stop the handler
            }
            if (type == EventType.NodeChildrenChanged) {
                System.out.println(CURRENT_JOB + " created!"); 
		//TODO get node data and send request to server
		Stat stat=null;
		
		//CurJob
		List<String> children =zkc.getChildren(CURRENT_JOB,null);
		if(children.size()==1){
		System.out.println("get a job "+children.get(0));
		CurJob=children.get(0);
		zkc.setData(CURRENT_JOB+"/"+CurJob, "In Progress",-1);
		sendReq(CurJob);
		}

            }
		zkc.getChildren(CURRENT_JOB,job_watcher);
        //}
    }

    public void handleFSEvent(WatchedEvent event){
    //primary FS down or changed
	String path = event.getPath();
        EventType type = event.getType();
	
	if(path.equalsIgnoreCase(FS)) {
            if (type == EventType.NodeDeleted) {
                System.out.println(FS + " deleted! wait for a new one!");
		zkc.exists(FS, fs_watcher);
            }
            if (type == EventType.NodeCreated) {
                System.out.println(FS + " created!");  
		getFS();
		workerReg();
		flag=true;
		//FSLisenter();
            }
        }
	
    }
    
    public void createMyWorker(){
        System.out.println("creating my worker path");
    	try{
    	myWorker=zkc.getZooKeeper().create(Workers+"/"+"worker-", null,zkc.getacl(), CreateMode.EPHEMERAL_SEQUENTIAL);
	if(myWorker!=null){
		System.out.println("Done creating my worker path: "+myWorker);
	}
	}catch(KeeperException e) {
            e.printStackTrace();
	    System.exit(1);
        } catch(Exception e) {
            e.printStackTrace();
	    System.exit(1);
        }

    }

    public void workerReg(){
    	System.out.println("worker regester");
	zkPacket packetToFS = new zkPacket();
	packetToFS.type=zkPacket.WORKER_REGISTER;
	packetToFS.worker_name=myWorker;
	try{
	outs.writeObject(packetToFS);
	}catch(Exception e) {
            e.printStackTrace();
	    System.exit(1);
	}
    }

    public void sendReq(String job){
    	System.out.println("Send request to file server "+job);
	zkPacket packetToFS = new zkPacket();
	packetToFS.type=zkPacket.WORKER_REQUEST;
	packetToFS.jobid=job;
	packetToFS.worker_name=myWorker;
	try{
	outs.writeObject(packetToFS);
	}catch(Exception e) {
            e.printStackTrace();
	    System.exit(1);
	}
    }

    public static void FSLisenter(){
    	//enable job event
	List<String> children=null;
	while(children==null){
	children=zkc.getChildren(CURRENT_JOB, job_watcher);
	//starting listening to FS
	}
	System.out.println("enter listener");
	try{
	while((packetFromFS = (zkPacket) ins.readObject())!= null){ 
		if(packetFromFS.type==zkPacket.FSJOB_ASSIGN){
			if(packetFromFS.jobid!=null&&CurJob!=null&&packetFromFS.jobid.equals(CurJob)){
				/*
				TODO
				add task to queue
				*/
				System.out.println("dictionary size is "+packetFromFS.dictionary.length);
				synchronized (taskqueue){
				for(int i=0;i<packetFromFS.dictionary.length;i++){
					//System.out.println("adding "+packetFromFS.dictionary[i]);
					taskqueue.offer(packetFromFS.dictionary[i]);
				}
				System.out.println("after adding size of taskqueue is "+taskqueue.size());
				queuesize=taskqueue.size();
				start=packetFromFS.dictionary[0];
				}
			}
		}
	}
	}catch(Exception e){
		e.printStackTrace();
	}finally{
		
	}
    }

    public static void SendAnswer(String answer){
    //create path
    System.out.println("setting result and delete current job");
    zkc.create(Result+"/"+CurJob,answer,CreateMode.PERSISTENT);
    zkc.delete(CURRENT_JOB+"/"+CurJob,-1);
    }

    public static void setDone(){
    //create path
    System.out.println("setting done");
    String[] tmp= myWorker.split("/");
    System.out.println("setting done on current job "+CurJob + " "+tmp[2]);
    zkc.create(CURRENT_JOB+"/"+CurJob+"/"+tmp[2]+start,null,CreateMode.PERSISTENT);
    
    }
}
