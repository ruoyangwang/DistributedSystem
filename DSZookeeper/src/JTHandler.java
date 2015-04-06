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


public class JTHandler extends Thread {
		private Socket socket = null;
		ObjectInputStream fromClient = null;
		ObjectOutputStream toClient = null;
		zkPacket packetFromClient;

		
		public JTHandler(Socket socket){
			super("Handle client request");
			this.socket = socket;
			try{
				fromClient = new ObjectInputStream(this.socket.getInputStream());
				toClient = new ObjectOutputStream(socket.getOutputStream());	
				System.out.println("Created a new Thread to handle client");
			}catch(Exception e){
				e.printStackTrace();
			}

		}

		public void run(){
			try{
				while(( packetFromClient = (zkPacket) fromClient.readObject()) != null ){
					System.out.println("get a packet from client");
					if(packetFromClient.type== zkPacket.CLIENT_QUIT)
						break;
						
					switch(packetFromClient.type){
						case zkPacket.CLIENT_LOOKUP:
							break;
						case zkPacket.CLIENT_REQUEST:
							submit_job(packetFromClient.hash);
							break;
						case zkPacket.CLIENT_STATUS:
							check_status(packetFromClient.hash);
							break;
					}

				}
				
				System.out.println("client exit");
				fromClient.close();
				toClient.close();
				socket.close();
				fromClient = null;
				toClient = null;
				socket = null;
				
			}catch(IOException e1){
				try{
					fromClient.close();
					toClient.close();
					socket.close();
					fromClient = null;
					toClient = null;
					socket = null;
				}catch(Exception E){E.printStackTrace();}

			}catch(ClassNotFoundException e2){
				e2.printStackTrace();
			}
		}
		
		
		public void submit_job(String hash){
			Code retcode;
			synchronized(JobTracker.zkc){
				retcode = JobTracker.zkc.create(
						            JobTracker.JOBS+"/"+hash,           // Path of znode (name of the path)
						            hash,       			// data
						            CreateMode.PERSISTENT   // set to EPHEMERAL.
						   );
			}
				if(retcode == Code.OK){
					System.out.println("successfully submit a new job  "+hash);
					zkPacket packetToClient = new zkPacket();
					packetToClient.type = zkPacket.CLIENT_REQUEST;
					try{
						toClient.writeObject(packetToClient);
					}catch(Exception e){
						e.printStackTrace();
					}
					
				}
				else{
					zkPacket packetToClient = new zkPacket();
					packetToClient.type = zkPacket.CLIENT_NAK;
					try{
						toClient.writeObject(packetToClient);
					}catch(Exception e){
						e.printStackTrace();
					}
					System.out.println("cannot submit job for some reasons, check connection first");	
				}
		}
		
		
		public void check_status(String hash){
			System.out.println("User status a job  "+hash);
			zkPacket packetToClient = new zkPacket();
			synchronized(JobTracker.zkc){
				packetToClient.type = zkPacket.CLIENT_STATUS;
				//check currJob if it's in progress
				System.out.println("check point 1  ");
				Stat stat1 = JobTracker.zkc.exists(JobTracker.CURRENT_JOB+"/"+hash, null);
				Stat stat2 = JobTracker.zkc.exists(JobTracker.JOBS+"/"+hash, null);
				if(stat1!=null || stat2!=null){
					packetToClient.status = zkPacket.JOB_PROGRESS;
					try{
							toClient.writeObject(packetToClient);
						}catch(Exception e){
							e.printStackTrace();
						}
					return;
				}
				/*List<String> children= JobTracker.zkc.getChildren(JobTracker.CURRENT_JOB);
				System.out.println("check point 2  ");
				for(String child: children){
					System.out.println("what's the child name  "+child);
					if(hash.equals(child)){
						
						packetToClient.status = zkPacket.JOB_PROGRESS;
						try{
							toClient.writeObject(packetToClient);
						}catch(Exception e){
							e.printStackTrace();
						}
						return;
					}
						
				}*/
				System.out.println("check point 3  ");
				//check result, see if it's complete or fail
				List<String> Result_children = JobTracker.zkc.getChildren(JobTracker.RESULT);
				for(String child: Result_children){
					if(hash.equals(child)){
						//get this result data to see if it's complete or fail
						Stat stat = JobTracker.zkc.exists(JobTracker.RESULT, null);
						String data = JobTracker.zkc.getData(JobTracker.RESULT+"/"+child,null,stat);
						
						String[] token = data.split(":");
						if(token.length>1){
							packetToClient.status = zkPacket.JOB_DONE;
							packetToClient.password = token[1];
							try{
								toClient.writeObject(packetToClient);
							}catch(Exception e){
								e.printStackTrace();
							}
							return;
						}
						
						else{
							packetToClient.status = zkPacket.JOB_FAIL;
							try{
								toClient.writeObject(packetToClient);
							}catch(Exception e){
								e.printStackTrace();
							}
							return;
						
						}
					
					}
				
				}

			}
		}
		
		
}
