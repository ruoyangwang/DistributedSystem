import java.io.*;
import java.net.*;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;
import java.lang.String;



public class ClientDriver {
	
	static ZkConnector zkc = null;
	static String JTip;
	static String JTport;
	
	zkPacket packetFromJT;
	
	String input;
	Socket JTSocket = null;
	ObjectInputStream FJT= null;
	ObjectOutputStream TJT = null;
	final static String JobTracker = "/JobTracker";
	final static String PRIMARY = JobTracker+"/primary";
	
	
	public static void main(String[] args){
	 	if (args.length != 3) {
        	System.out.println("3 Arguments $host:$port $pwdHash ");
        	System.exit(-1);        	
        }
        
        try{
			ClientDriver client = new ClientDriver(args[0]);
			client.getJT();
			System.out.println("`````` check the job info   "+ args[2]);
			client.send_request(args[1],args[2]);
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	
    	
 		/*while(true){
			// help text
			System.out.println("");
			System.out.println("Enter:");
			System.out.println("\"run\" followed by an input file, Q values and hosts to start a new job");
			System.out.println("ex: \"run inputfile 1-3,5 c123,c124,c125\" would run npairs to execute with inputfile and Q values 1,2,3,5 on machiens c123 c124 and c125");
			System.out.println("\"add\" followed by hosts to add more machies to the computation");
			System.out.println("\"status\" follow by tracking ID to get status for the job");
			System.out.println("\"kill\" follow by tracking ID to get kill the job");
			System.out.println("\"q\" to quit");
			System.out.println("");
		}*/
 		
	 	
	 
	 
	}
	
	
	public ClientDriver(String host) throws Exception{
			this.zkConnection(host);
	}
	
	
	public void send_request(String type, String hash){
		System.out.println("sending request:  "+type+"  "+hash);
		zkPacket packetToServer = new zkPacket();
		if(type.equals("job"))
			packetToServer.type = zkPacket.CLIENT_REQUEST;
		else if(type.equals("status"))
			packetToServer.type = zkPacket.CLIENT_STATUS;
		
		packetToServer.hash = hash;
		try{
			TJT.writeObject(packetToServer);   
		
			while((packetFromJT = (zkPacket) FJT.readObject())!= null){ 
				System.out.println("get response from JT");
				if(packetFromJT.type == zkPacket.CLIENT_STATUS){
					if(packetFromJT.status == zkPacket.JOB_PROGRESS)
						System.out.println("job in progress");

					else if(packetFromJT.status == zkPacket.JOB_DONE)
						System.out.println("job finished and password: "+packetFromJT.password);
	
					else if(packetFromJT.status == zkPacket.JOB_FAIL)
						System.out.println("job finished but password not found ");

					else
						System.out.println("no such job");      
				}
			
				else if(packetFromJT.type == zkPacket.CLIENT_REQUEST)
					System.out.println("job successfully submitted");

				else
					System.out.println("JobTracker cannot handle this request");
					
				break;
			}
			zkPacket PTS = new zkPacket();
			PTS.type = zkPacket.CLIENT_QUIT;
			TJT.writeObject(PTS); 
			Thread.sleep(1000);
			FJT.close();
			TJT.close();
			JTSocket.close();
			FJT = null;
			TJT = null;
			JTSocket = null;
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	
	}
	
	
	
	public void getJT(){
		Stat stat = null;
		while(stat == null){
			stat = zkc.exists(PRIMARY, null);
			try{
				Thread.sleep(200);
			}catch(Exception e){};
		}
		
		String data = zkc.getData(PRIMARY,null,stat);
		String [] token = data.split(":");
		JTip = token[0];
		JTport = token[1];
		//connect to JobTracker
		if(JTconnection(JTip, Integer.parseInt(JTport))==false){
			System.out.println("cannot connect to JobTracker, please try again");
			System.exit(-1);
		}
	}
	
	
	public boolean zkConnection(String host){
		zkc = new ZkConnector();
    	try {
            zkc.connect(host);
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
	
	}
	
	
	
	public boolean JTconnection(String ip, int port){
		try {
			JTSocket = new Socket(ip, port);
			TJT = new ObjectOutputStream(JTSocket.getOutputStream());
			FJT = new ObjectInputStream(JTSocket.getInputStream());
			return true;
			
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	
	}

}
