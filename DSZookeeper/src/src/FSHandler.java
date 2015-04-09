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
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
public class FSHandler extends Thread {
		static String JobID;
		static int index_pointer=0;
		static int max_index;
		private Socket socket = null;
		ObjectInputStream fromClient = null;
		ObjectOutputStream tClient = null;
		zkPacket packetFromWorker;
		public static int section_num=0;
		private String myworker;
		static ConcurrentHashMap<String, workerData> workerMap = new ConcurrentHashMap<String, workerData>();
		
		public FSHandler(Socket socket){
			super("Handle worker request");
			JobID=null;
			FileServer.worker_count=FileServer.worker_count+1;
			this.socket = socket;
			try{
				fromClient = new ObjectInputStream(this.socket.getInputStream());
				tClient = new ObjectOutputStream(this.socket.getOutputStream());	
				System.out.println("Created a new Thread to handle client");
			}catch(Exception e){
				e.printStackTrace();
			}

		}

		public void run(){
			try{
				while(( packetFromWorker = (zkPacket) fromClient.readObject()) != null ){
					System.out.println("get a packet from worker");
					//if(packetFromClient.type== zkPacket.CLIENT_QUIT)
					//	break;
						
					switch(packetFromWorker.type){
						case zkPacket.WORKER_REGISTER:
							worker_reg();
							break;
						case zkPacket.WORKER_REQUEST:
							worker_req(packetFromWorker,tClient);
							break;
					}

				}
				
				System.out.println("client exit");
				fromClient.close();
				tClient.close();
				socket.close();
				fromClient = null;
				tClient = null;
				socket = null;
				
			}catch(IOException e1){
				try{
					fromClient.close();
					tClient.close();
					socket.close();
					fromClient = null;
					tClient = null;
					socket = null;
					FileServer.worker_count=FileServer.worker_count-1;
					FileServer.workerbackup(myworker);
				}catch(Exception E){E.printStackTrace();}

			}catch(ClassNotFoundException e2){
				e2.printStackTrace();
			}
		}
		
		
	private synchronized void worker_reg(){
	//first connection with worker
	System.out.println("worker register");
	//TODO
	workerData tmp=new workerData();
	tmp.workername=packetFromWorker.worker_name;
	if(tmp.workername!=null){
	myworker=tmp.workername;
	tmp.toClient=tClient;
	workerMap.put(tmp.workername,tmp);
	System.out.println("add one client");
	}
	} 

	private synchronized void worker_req(zkPacket packetWorker,ObjectOutputStream toClient ){
	//job delegation
	System.out.println("worker request "+packetWorker.jobid);
	
	if(JobID==null&& packetWorker.jobid!=null){
		JobID=packetWorker.jobid;
		System.out.println("First request");
		}
	else if (JobID!=null && !JobID.equals(packetWorker.jobid)){
		JobID=packetWorker.jobid;
		System.out.println("new request");
		section_num=0;
		index_pointer=0;
		//reset the array pointer
		//assign job
		//TODO
		}
	else if (JobID!=null && JobID.equals(packetWorker.jobid)){
		System.out.println("Same request");
	}
	else
		System.out.println("worker_req illegal");

		
		//start to assgin job
		System.out.println("divide line num " +FileServer.line_num+"/ "+FileServer.worker_count);
		if(section_num==0)
		section_num=FileServer.line_num/FileServer.worker_count;

		System.out.println("section num is "+section_num);
		zkPacket pkttoworker=new  zkPacket();
		pkttoworker.type=zkPacket.FSJOB_ASSIGN;
		pkttoworker.jobid=JobID;
		
		//get worker info
		workerData tempData=workerMap.get(packetWorker.worker_name);
		
		if((FileServer.line_num-(section_num*(index_pointer+1)))>=section_num){
			System.out.println("from "+(section_num*index_pointer)+" to "+(section_num*index_pointer+section_num-1));
			pkttoworker.dictionary=new String[section_num];
			System.arraycopy(FileServer.Dictionary,section_num*index_pointer,pkttoworker.dictionary,0,section_num);
			tempData.from=section_num*index_pointer;
			tempData.to=tempData.from+section_num;
			index_pointer=index_pointer+1;
			System.out.println("now index_pointer is "+index_pointer);
		}
		else{
			int assign=FileServer.line_num-section_num*index_pointer;
			pkttoworker.dictionary=new String[assign];
			System.arraycopy(FileServer.Dictionary,section_num*index_pointer,pkttoworker.dictionary,0,assign);
			tempData.from=section_num*index_pointer;
			tempData.to=section_num*index_pointer+assign;
			index_pointer=0;
			section_num=0;
		}
		try{
		toClient.writeObject(pkttoworker);
		}catch(Exception e){}
	}
}
