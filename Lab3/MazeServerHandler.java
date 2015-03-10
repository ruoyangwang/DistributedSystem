
import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Random;
import java.util.Vector; 

public class MazeServerHandler extends Thread{
	private Socket socket = null;

	static Lock eventLock = new ReentrantLock();
	static ServerSendHandler ServerSendHandler = null;		//holding a send pointer thread to send message to servers
	static BroadCastEvent BCE = null;
	static ConcurrentHashMap<String, ClientEventData> clientMap = new ConcurrentHashMap<String, ClientEventData>();
	//static HashMap<String, int> peerServerMap = new HashMap<String, int>();

	static BlockingQueue<String> clientQueue = new ArrayBlockingQueue<String>(100);
	static BlockingQueue<Serialized_Client_Data> sendQueue = new ArrayBlockingQueue<Serialized_Client_Data>(100);
	
	
	
	static volatile Comparator<Serialized_Client_Data> eventComparator = new Comparator<Serialized_Client_Data>() {
		@Override
		public int compare(Serialized_Client_Data d1, Serialized_Client_Data d2) {
		    return d1.Lamport - d2.Lamport; 
		}
	};
	/*Priority queue with comparable to sort */
	static volatile PriorityBlockingQueue<Serialized_Client_Data> eventList= new PriorityBlockingQueue<Serialized_Client_Data>(100,eventComparator);

	static ClientEventData clientData ;
	static Serialized_Client_Data S_ClientData;
	Client self;
	static String MyClientName;

	//use for reborn 
        public static String target=null;
	public static String source=null;

	MazePacket packetFromClient;
	static boolean score_initialized = false;
	static boolean firstTime = true;

	ObjectInputStream fromClient= null;
	ObjectOutputStream toClient = null;
	//private static final Random randomGen;
	ObjectInputStream FC= null;
	ObjectOutputStream TC = null;
	static List <ObjectOutputStream> collection = new ArrayList<ObjectOutputStream>();
	//-----------------------create a map on server side-------------------------
	private static final int mazeHeight = 10;
	private static final int mazeWidth = 20;
	private static final int mazeSeed = 42;
	private static final String ProjUpdate="ProjUpdate";
	//---------------------------------------------------------------------------
	static MazeImpl maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
	
	static ScoreTableModel scoreModel = new ScoreTableModel();
	static OverheadMazePanel overheadPanel;


	public MazeServerHandler(Socket socket){
		
		super("MazeServerHandlerThread");
		this.socket = socket;
		System.out.println("Created new Thread to handle client");
		if(maze.ServerPointer==null)
			maze.ServerPointer=this;
	}
	

	public void run() {
		boolean gotByePacket = false;
		try{		
				ObjectInputStream from_Client=this.FC;
				ObjectOutputStream to_Client=this.TC;
				this.fromClient = from_Client;
				this.toClient= to_Client;
				
				if(this.FC == null){
					from_Client = new ObjectInputStream(socket.getInputStream());
					this.fromClient = from_Client;
				}

				if(this.TC == null){
					/* stream to write back to client */
					to_Client = new ObjectOutputStream(socket.getOutputStream());
					this.toClient= to_Client;
				}
				//System.out.println("toClient Object address of this client:     ------  "+toClient);
				while (( packetFromClient = (MazePacket) from_Client.readObject()) != null) {
					System.out.println("print the packet type   "+packetFromClient.type);
					switch (packetFromClient.type) {
							case MazePacket.SERVER_REGISTER:
								New_Server_Coming();
								break;
				            case MazePacket.CLIENT_REGISTER:
				                Client_Register();
				                break;
				            case MazePacket.CLIENT_QUIT:
				                Client_Quit();
				                break;
				            case MazePacket.CLIENT_FORWARD:
				            case MazePacket.CLIENT_LEFT:
				            case MazePacket.CLIENT_RIGHT:
				            case MazePacket.CLIENT_BACKWARD:
				                Client_Move();
								break;
							case MazePacket.CLIENT_FIRE:
								Client_Fire();
								break;  
							case MazePacket.CLIENT_REBORN:
								Client_Reborn();
								break;
							case MazePacket.PROJ_UPDATE:
								Projectile_Update();
								break;
							case MazePacket.ACK:
								Server_ACK();
								break;
							case MazePacket.FINAL_ACK:
								Final_ACK();
								break;
					}
		              /*if(packetFromClient.type==MazePacket.CLIENT_QUIT){
						this.maze.removeClient(clientMap.get(packetFromClient.Cname).client);
						this.clientMap.remove(packetFromClient.Cname);
		              	break;
					}*/
				}
		
		
		
				from_Client.close();
				to_Client.close();
				socket.close();
			
		}catch (IOException e) {
			if(!gotByePacket)
				return;//e.printStackTrace();
			
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
	}

	public synchronized void Server_ACK(){
		boolean waitForEvent = true;
		System.out.println("Inside ACK function:  .....  from Server:  "+packetFromClient.ServerData.serverHostName);
		String CN = packetFromClient.Cname;
		while(waitForEvent){
			eventLock.lock();
			for(Serialized_Client_Data SCD: eventList){
				if(SCD.Cname.equals(CN) && SCD.ACK < MazeServer.serverCount){
					//
					waitForEvent = false;
					SCD.ACK+=1;
					//
					System.out.println("what's the ACK right now?  "+SCD.event+"  ACK#:  "+SCD.ACK+ "  size of eventList?  "+eventList.size());
					/*if get all ACKs and I am the event starter*/
					System.out.println("ServerHostName:    "+MazeServer.myHostName+"  eventServerHostName:   "+SCD.serverHostName);
					if(SCD.ACK == MazeServer.serverCount && SCD.serverHostName.equals(MazeServer.myHostName)){		
						Serialized_Client_Data scd = new Serialized_Client_Data();
						scd.event = MazePacket.FINAL_ACK;
						scd.Cname = CN;
						try{
							sendQueue.put(scd);
						
						}catch(Exception e){
							e.printStackTrace();
						}
					}
					break;

				}

			}
			eventLock.unlock();
		}
		
	}


	public synchronized void Final_ACK(){
		System.out.println("inside the final one ACK, means one event can be executed");
		String CN = packetFromClient.Cname;
		eventLock.lock();
		for(Serialized_Client_Data SCD: eventList){
			if(SCD.Cname.equals(CN)  && SCD.ACK < MazeServer.serverCount){
				//eventLock.lock();
				SCD.ACK=MazeServer.serverCount;
				//eventLock.unlock();
				System.out.println("found the one !!!!!!!!!!!!!!!!    type?:   "+SCD.event+"  ACK#: "+SCD.ACK+ "  size of eventList?  "+eventList.size());
				break;

			}

		}
		eventLock.unlock();

	}

	
	public void Server_Register(String hostname, Socket s){
		System.out.println("connecting to other servers! my hostname is:  "+hostname);
		MazePacket packetToClient = new MazePacket();
		packetToClient.type = MazePacket.SERVER_REGISTER;
		packetToClient.ServerHostName = hostname;
		try{
			ObjectOutputStream to_Client = new ObjectOutputStream(s.getOutputStream());
			to_Client.writeObject(packetToClient);
			
			this.TC = to_Client;
			collection.add(this.TC);
			if(this.ServerSendHandler==null){
				this.ServerSendHandler= new ServerSendHandler();
				this.BCE = new BroadCastEvent();
				ServerSendHandler.start();
				BCE.start();				//broadcast event handler
			}
			//to_Client=null;

		}catch(Exception e){
				e.printStackTrace();
		}

	}



	public void New_Server_Coming(){
		System.out.println("new server coming !");
		String newServerName = this.packetFromClient.ServerHostName;
		MazeServer.peerServerMap.put(newServerName, this.socket);
		MazeServer.serverCount+=1;
		for(String key:MazeServer.peerServerMap.keySet())
			System.out.println("print out server records: "+key);
		
		collection.add(toClient);		//collection of server outputStream, for later sending purpose
		
		if(this.ServerSendHandler==null){
			this.ServerSendHandler= new ServerSendHandler();	
			ServerSendHandler.start();
			this.BCE = new BroadCastEvent();
			BCE.start();	
		}
		
	}


	
	public synchronized void Client_Register(){
		//this.packetToClient = new MazePacket();
		System.out.println("client registering for the first time");
		boolean OtherSide = false;
		if(clientMap.get(this.packetFromClient.Cname)==null){
				if(score_initialized==false){
					/*create score table*/
				    assert(scoreModel != null);
				    maze.addMazeListener(scoreModel);
				
		            maze.ServerPointer =this;
		            maze.ServerClientMap= this.clientMap;
		            maze.clientQueue= this.clientQueue;
		            score_initialized=true;
				}
			
			
				GUIClient guiClient = new GUIClient(packetFromClient.Cname);
				if(packetFromClient.ServerData == null){
					maze.addClient(guiClient,null,null);
					MyClientName=packetFromClient.Cname;
					}
			
				else{
					System.out.println("A client joined another server");
					OtherSide = true;
					maze.addClient(guiClient,
									packetFromClient.ServerData.Clocation,
									packetFromClient.ServerData.Cdirection);
				}



				if(!OtherSide){
					clientData = new ClientEventData(
									packetFromClient.Cname,
									guiClient.getPoint(),
									guiClient.getOrientation(),
									packetFromClient.Ctype,
									packetFromClient.type,	//event type the same as MazePacket event type
									this.socket,
									this.toClient,
									guiClient			
								);	
					clientData.fromClient = this.fromClient;
				
					S_ClientData = new Serialized_Client_Data(		//seriliazed version of above data, for passing into socket back to clients
										guiClient.getName(),
										guiClient.getPoint(),
										guiClient.getOrientation(),
										packetFromClient.Ctype,
										packetFromClient.type,
										maze.get_score(guiClient.getName())
								);
				}
				else{									//received event doesn't need to resend	
					clientData = new ClientEventData(
									packetFromClient.Cname,
									guiClient.getPoint(),
									guiClient.getOrientation(),
									packetFromClient.Ctype,
									packetFromClient.type,	//event type the same as MazePacket event type
									null,
									null,
									guiClient			
								);	


				}
			
				try{
					if(!OtherSide)
						sendQueue.put(S_ClientData);
					clientMap.put(this.packetFromClient.Cname, clientData);
					clientQueue.put(this.packetFromClient.Cname);
				
				}catch(Exception e){
					e.printStackTrace();
				}
			
				//Server_Broadcast();
			
				Broad_cast();
		}
		else{
			Error_sending(MazePacket.CLIENT_REGISTER_ERROR);
		}
	
	}
	


	public synchronized void Client_Quit(){
		System.out.println("client quitting");
		String CN = packetFromClient.Cname;
		if(clientMap.get(CN)!=null){
			
			try{
				
				clientMap.get(CN).Update_Event(
					packetFromClient.Clocation,
					packetFromClient.Cdirection,
					packetFromClient.type,
					0
				);
				clientQueue.put(this.packetFromClient.Cname);
				/*serverData null means it's a client request not a request broadcast from server*/
				if(packetFromClient.ServerData == null){
					Serialized_Client_Data SCD= new Serialized_Client_Data(		//seriliazed version of above data, for passing into socket back to clients
							CN,
							clientMap.get(CN).client.getPoint(),
							clientMap.get(CN).client.getOrientation(),
							packetFromClient.Ctype,
							packetFromClient.type,
							packetFromClient.score
					);
					//SCD.Lamport = MazeServer.LamportClock;
					increment_LamportClock();
					SCD.Lamport = MazeServer.LamportClock;
					//SCD.serverHostName = packetFromClient.ServerData.serverHostName;
					//SCD.ACK =0;
					add_One_Event(SCD);
					sendQueue.put(SCD);
				}
				
				else{			//request from server
					System.out.println("this is a request from server, not client .....");
					add_One_Event(packetFromClient.ServerData);
					if(MazeServer.LamportClock == packetFromClient.ServerData.Lamport){		//check if there is conflicts on Lamport Clock
						System.out.println("this is the lamport clock conflict!");
						for(Serialized_Client_Data SCD: eventList){
							if(SCD.Lamport == packetFromClient.ServerData.Lamport ){
								if(MazeServer.pid > packetFromClient.ServerData.pid){
									increment_LamportClock();		//if lose, increment my lamport clock
									SCD.Lamport = MazeServer.LamportClock;							
									
								}
								else{
									packetFromClient.ServerData.Lamport+= 1;	//if win, target increment lamport clock
								}
	
								sort_Event_List();											//sort event again based on new Lamport clock
								Serialized_Client_Data scd = new Serialized_Client_Data();
								scd.event = MazePacket.ACK;
								scd.Cname = CN;
								sendQueue.put(scd);
								break;
							}

						}
					
					}
					
					else{
						System.out.println("i don't see any conflict here Lamport Clock");
						Serialized_Client_Data scd = new Serialized_Client_Data();
						scd.event = MazePacket.ACK;
						scd.Cname = CN;
						sendQueue.put(scd);	
						System.out.println("am i not returning?");
					}	
				}	
				
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
		else{
			Error_sending(MazePacket.CLIENT_QUIT_ERROR);
		}
		
	}



	public synchronized void Client_Move(){
		System.out.println("client moving "+ packetFromClient.type);
		boolean OtherSide = false;
		boolean myTurn = false;
		String CN = packetFromClient.Cname;
		

		
		if(clientMap.get(this.packetFromClient.Cname)!=null){
	
			try{
				clientQueue.put(this.packetFromClient.Cname);
				/*serverData null means it's a client request not a request broadcast from server*/
				if(packetFromClient.ServerData == null){
					OtherSide = false;
					Serialized_Client_Data SCD= new Serialized_Client_Data(		//seriliazed version of above data, for passing into socket back to clients
										CN,
										clientMap.get(CN).client.getPoint(),
										clientMap.get(CN).client.getOrientation(),
										packetFromClient.Ctype,
										packetFromClient.type,
										packetFromClient.score
								);
					//SCD.Lamport = MazeServer.LamportClock;
					increment_LamportClock();
					SCD.Lamport = MazeServer.LamportClock;
					//SCD.serverHostName = packetFromClient.ServerData.serverHostName;
					//SCD.ACK =0;
					add_One_Event(SCD);
					sendQueue.put(SCD);
				}
		
				else{			//request from server
					System.out.println("this is a request from server, not client .....");
					add_One_Event(packetFromClient.ServerData);
					if(MazeServer.LamportClock == packetFromClient.ServerData.Lamport){		//check if there is conflicts on Lamport Clock
						System.out.println("this is the lamport clock conflict!");
						for(Serialized_Client_Data SCD: eventList){
							if(SCD.Lamport == packetFromClient.ServerData.Lamport ){
								if(MazeServer.pid > packetFromClient.ServerData.pid){
									increment_LamportClock();		//if lose, increment my lamport clock
									SCD.Lamport = MazeServer.LamportClock;							
									
								}
								else{
									packetFromClient.ServerData.Lamport+= 1;	//if win, target increment lamport clock
								}
	
								sort_Event_List();											//sort event again based on new Lamport clock
								Serialized_Client_Data scd = new Serialized_Client_Data();
								scd.event = MazePacket.ACK;
								scd.Cname = CN;
								sendQueue.put(scd);
								break;
							}

						}
					
					}
					
					else{
						System.out.println("i don't see any conflict here Lamport Clock");
						Serialized_Client_Data scd = new Serialized_Client_Data();
						scd.event = MazePacket.ACK;
						scd.Cname = CN;
						sendQueue.put(scd);	
						System.out.println("am i not returning?");
					}	
				}	
System.out.println("Size before get:  "+eventList.size());
//System.out.println("whats the serverCOunt and ACK count?   "+MazeServer.serverCount + " .....  "+eventList.get(0).ACK);
System.out.println("Size after get:  "+eventList.size());
			
			}catch(Exception e){
					e.printStackTrace();
			}
		}
		else{
			Error_sending(MazePacket.CLIENT_REGISTER_ERROR);
		}
	}
	



	public synchronized void Client_Fire(){
/*		
		if(clientMap.get(this.packetFromClient.Cname)!=null){
			
			System.out.println("client firing event"+ this.maze.get_score(packetFromClient.Cname)+"    "+packetFromClient.type);
			try{
				clientQueue.put(this.packetFromClient.Cname);
				//Client killedClient=null;
				
				clientMap.get(packetFromClient.Cname).Update_Event(
					packetFromClient.Clocation,
					packetFromClient.Cdirection,
					packetFromClient.type,
					this.maze.get_score(packetFromClient.Cname)
				);
				
				clientMap.get(this.packetFromClient.Cname).client.fire();
				
			}catch(Exception e){
				e.printStackTrace();
			}
			Broad_cast();
		}
		else{
			Error_sending(MazePacket.CLIENT_REGISTER_ERROR);
		}
*/
		boolean OtherSide = false;
		boolean myTurn = false;
		String CN = packetFromClient.Cname;
		if(clientMap.get(this.packetFromClient.Cname)!=null){
	
			try{
				clientQueue.put(this.packetFromClient.Cname);
				/*serverData null means it's a client request not a request broadcast from server*/
				if(packetFromClient.ServerData == null){
					System.out.println("receive fire event from my client");
					OtherSide = false;
					Serialized_Client_Data SCD= new Serialized_Client_Data(		//seriliazed version of above data, for passing into socket back to clients
										CN,
										clientMap.get(CN).client.getPoint(),
										clientMap.get(CN).client.getOrientation(),
										packetFromClient.Ctype,
										packetFromClient.type,
										packetFromClient.score
								);
					//SCD.Lamport = MazeServer.LamportClock;
					increment_LamportClock();
					SCD.Lamport = MazeServer.LamportClock;
					//SCD.serverHostName = packetFromClient.ServerData.serverHostName;
					//SCD.ACK =0;
					add_One_Event(SCD);
					sendQueue.put(SCD);
				}
		
				else{			//request from server
					System.out.println("receive fire event from other server");
					add_One_Event(packetFromClient.ServerData);
					if(MazeServer.LamportClock == packetFromClient.ServerData.Lamport){		//check if there is conflicts on Lamport Clock
						System.out.println("this is the lamport clock conflict!");
						for(Serialized_Client_Data SCD: eventList){
							if(SCD.Lamport == packetFromClient.ServerData.Lamport ){
								if(MazeServer.pid > packetFromClient.ServerData.pid){
									increment_LamportClock();		//if lose, increment my lamport clock
									SCD.Lamport = MazeServer.LamportClock;							
									
								}
								else{
									packetFromClient.ServerData.Lamport+= 1;	//if win, target increment lamport clock
								}
	
								sort_Event_List();											//sort event again based on new Lamport clock
								Serialized_Client_Data scd = new Serialized_Client_Data();
								scd.event = MazePacket.ACK;
								scd.Cname = CN;
								sendQueue.put(scd);
								break;
							}

						}
					
					}
					
					else{
						System.out.println("i don't see any conflict here Lamport Clock");
						Serialized_Client_Data scd = new Serialized_Client_Data();
						scd.event = MazePacket.ACK;
						scd.Cname = CN;
						sendQueue.put(scd);	
						System.out.println("am i not returning?");
					}	
				}	
System.out.println("Size before get:  "+eventList.size());
//System.out.println("whats the serverCOunt and ACK count?   "+MazeServer.serverCount + " .....  "+eventList.get(0).ACK);
System.out.println("Size after get:  "+eventList.size());
			
			}catch(Exception e){
					e.printStackTrace();
			}
		}
		else{
			Error_sending(MazePacket.CLIENT_REGISTER_ERROR);
		}
	}



	public synchronized void Client_Reborn(){
		
		/*
		Calculate the reborn location and position
		*/
		//System.out.println("score for reborn "+packetFromClient.score);
		/*
		assert(clientMap.get(packetFromClient.Cname).client!=null);
		Client target = clientMap.get(packetFromClient.Cname).client;
		System.out.println("sdfsfsfsdf "+packetFromClient.Clocation+" "+packetFromClient.Cdirection);
		//while(maze.finished==false);
		//maze.finished=false;
		if(clientMap.get(this.packetFromClient.Cname)!=null){
			
			try{
				clientQueue.put(this.packetFromClient.Cname);
				        


						    //clientQueue.put(target.getName());
				System.out.println("-----------location and point----   "+target.getPoint() +"   " +target.getOrientation());
				clientMap.get(target.getName()).Update_Event(
						target.getPoint(),
						target.getOrientation(),
						MazePacket.CLIENT_REBORN,
						maze.get_score(target.getName())
				);

				
			}catch(Exception e){
				e.printStackTrace();
			}
			
			System.out.println("reborn orientation "+clientMap.get(packetFromClient.Cname).client.getOrientation());
			Broad_cast();
		}
		else{
			Error_sending(MazePacket.CLIENT_REGISTER_ERROR);
		}
		*/
		boolean OtherSide = false;
		boolean myTurn = false;
		String CN = packetFromClient.Cname;
		if(clientMap.get(this.packetFromClient.Cname)!=null){
	
			try{
				clientQueue.put(this.packetFromClient.Cname);
				/*serverData null means it's a client request not a request broadcast from server*/
				if(packetFromClient.ServerData == null){
					System.out.println("receive reborn event from my client");
					OtherSide = false;
					Serialized_Client_Data SCD= new Serialized_Client_Data(		//seriliazed version of above data, for passing into socket back to clients
										CN,
										clientMap.get(CN).client.getPoint(),
										clientMap.get(CN).client.getOrientation(),
										packetFromClient.Ctype,
										packetFromClient.type,
										packetFromClient.score
								);
					//SCD.Lamport = MazeServer.LamportClock;
					System.out.print("\n\ninside server reborn: "+	clientMap.get(CN).client.getPoint()+" "+clientMap.get(CN).client.getOrientation());
					increment_LamportClock();
					SCD.Lamport = MazeServer.LamportClock;
					//SCD.serverHostName = packetFromClient.ServerData.serverHostName;
					//SCD.ACK =0;
					add_One_Event(SCD);
					sendQueue.put(SCD);
				}
		
				else{			//request from server
					System.out.println("receive reborn event from other server");
					add_One_Event(packetFromClient.ServerData);
					if(MazeServer.LamportClock == packetFromClient.ServerData.Lamport){		//check if there is conflicts on Lamport Clock
						System.out.println("this is the lamport clock conflict!");
						for(Serialized_Client_Data SCD: eventList){
							if(SCD.Lamport == packetFromClient.ServerData.Lamport ){
								if(MazeServer.pid > packetFromClient.ServerData.pid){
									increment_LamportClock();		//if lose, increment my lamport clock
									SCD.Lamport = MazeServer.LamportClock;							
									
								}
								else{
									packetFromClient.ServerData.Lamport+= 1;	//if win, target increment lamport clock
								}
	
								sort_Event_List();											//sort event again based on new Lamport clock
								Serialized_Client_Data scd = new Serialized_Client_Data();
								scd.event = MazePacket.ACK;
								scd.Cname = CN;
								sendQueue.put(scd);
								break;
							}

						}
					
					}
					
					else{
						System.out.println("i don't see any conflict here Lamport Clock");
						Serialized_Client_Data scd = new Serialized_Client_Data();
						scd.event = MazePacket.ACK;
						scd.Cname = CN;
						sendQueue.put(scd);	
						System.out.println("am i not returning?");
					}	
				}	
				System.out.println("Size before get:  "+eventList.size());
				//System.out.println("whats the serverCOunt and ACK count?   "+MazeServer.serverCount + " .....  "+eventList.get(0).ACK);
				System.out.println("Size after get:  "+eventList.size());
			
			}catch(Exception e){
					e.printStackTrace();
			}
		}
		else{
			Error_sending(MazePacket.CLIENT_REGISTER_ERROR);
		}
	}




	public synchronized static void Projectile_Update(){
	
			try{
			clientQueue.put(ProjUpdate);
			Broad_cast();
			}catch(Exception e){
				e.printStackTrace();
			}
	/*
		boolean OtherSide = false;
		boolean myTurn = false;
		String CN = packetFromClient.Cname;
		if(clientMap.get(this.packetFromClient.Cname)!=null){
	
			try{
				clientQueue.put(this.packetFromClient.Cname);
				//client will not send this event
		
				//request must be from server
					System.out.println("receive reborn event from other server");
					add_One_Event(packetFromClient.ServerData);
					if(MazeServer.LamportClock == packetFromClient.ServerData.Lamport){		//check if there is conflicts on Lamport Clock
						System.out.println("this is the lamport clock conflict!");
						for(Serialized_Client_Data SCD: eventList){
							if(SCD.Lamport == packetFromClient.ServerData.Lamport ){
								if(MazeServer.pid > packetFromClient.ServerData.pid){
									increment_LamportClock();		//if lose, increment my lamport clock
									SCD.Lamport = MazeServer.LamportClock;							
									
								}
								else{
									packetFromClient.ServerData.Lamport+= 1;	//if win, target increment lamport clock
								}
	
								sort_Event_List();											//sort event again based on new Lamport clock
								Serialized_Client_Data scd = new Serialized_Client_Data();
								scd.event = MazePacket.ACK;
								scd.Cname = CN;
								sendQueue.put(scd);
								break;
							}

						}
					
					}
					
					else{
						System.out.println("i don't see any conflict here Lamport Clock");
						Serialized_Client_Data scd = new Serialized_Client_Data();
						scd.event = MazePacket.ACK;
						scd.Cname = CN;
						sendQueue.put(scd);	
						System.out.println("am i not returning?");
					}	
					
				System.out.println("Size before get:  "+eventList.size());
				//System.out.println("whats the serverCOunt and ACK count?   "+MazeServer.serverCount + " .....  "+eventList.get(0).ACK);
				System.out.println("Size after get:  "+eventList.size());
			
			}catch(Exception e){
					e.printStackTrace();
			}
		}
		else{
			Error_sending(MazePacket.CLIENT_REGISTER_ERROR);
		}
	}
	*/	
	}

	
	
	public synchronized static void Server_Broadcast(){
			


	}
	



	public synchronized static void Broad_cast(){
		while(clientQueue.size()>0){
			String clientEvent;
			try{
				clientEvent = clientQueue.take();
				for (String key: clientMap.keySet()) {
					//Socket holderSocket = clientMap.get(key).socket;
					
					MazePacket packetToClient = new MazePacket();
					
					//projectile update
					if(clientEvent.equals("ProjUpdate")){
						packetToClient.Cname = null;
						packetToClient.Clocation = null;
						packetToClient.Cdirection = null;
						packetToClient.type = MazePacket.PROJ_UPDATE;
						System.out.println("=-=-=-update projectile");

					}
					//other actions
					else{
						if(clientEvent.equals(key)){
							int i = 0;
							for (String key2: clientMap.keySet()) {
								S_ClientData = new Serialized_Client_Data(		//seriliazed version of above data, for passing into socket back to clients
									clientMap.get(key2).Cname,
									clientMap.get(key2).Clocation,
									clientMap.get(key2).Cdirection,
									clientMap.get(key2).Ctype,
									clientMap.get(key2).event,
									maze.get_score(clientMap.get(key2).Cname)
								);
								//System.out.println("S_client data score:   "+maze.get_score(clientMap.get(key2).Cname)+"  --name:  "+clientMap.get(key2).Cname);
								packetToClient.clientData[i]= S_ClientData;
								i+=1;
							}
						}
					
						packetToClient.Cname = clientMap.get(clientEvent).Cname;
						packetToClient.Clocation = clientMap.get(clientEvent).client.getPoint();
						packetToClient.Cdirection = clientMap.get(clientEvent).client.getOrientation();
						packetToClient.type = clientMap.get(clientEvent).event;
					}
					try{
						/* send reply back to client */
						if(clientMap.get(key).toClient!=null)
							clientMap.get(key).toClient.writeObject(packetToClient);
						
					}catch (Exception e) {
						e.printStackTrace();
					}
				
				}
			}catch (Exception e) {
					e.printStackTrace();
			}
		}
	}	


	
	
	public synchronized void Error_sending(int err_code){
		try{
			MazePacket packetToClient = new MazePacket();
			packetToClient.type = err_code;
			this.toClient.writeObject(packetToClient);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}



	public synchronized static void increment_LamportClock(){	
		MazeServer.LamportClock+=1;
	}
	
	public synchronized static void update_LamportClock(int val){

		MazeServer.LamportClock=val;
	}

	
	public synchronized static void add_One_Event(Serialized_Client_Data Data){
		eventList.add(Data);
		eventList.peek();		//just for purpose of sorting
		System.out.println("after add get this event immediately to check   " +eventList.peek().Cname);
	
	}

	public synchronized static void sort_Event_List(){
		//Collections.sort(eventList, eventComparator);
		eventList.peek();

	}

	public synchronized static void event_Fire(String name){
		if(clientMap.get(name)!=null){
			
			//System.out.println("client firing event"+ this.maze.get_score(packetFromClient.Cname)+"    "+packetFromClient.type);
			try{
				System.out.println("client firing event");
				//clientQueue.put(this.packetFromClient.Cname);
				//Client killedClient=null;
				
				clientMap.get(name).Update_Event(
					clientMap.get(name).client.getPoint(),
					clientMap.get(name).client.getOrientation(),
					MazePacket.CLIENT_FIRE,
					maze.get_score(name)
				);
				
				clientMap.get(name).client.fire();
				
			}catch(Exception e){
				e.printStackTrace();
			}
			Broad_cast();
		}
	}
	
	public synchronized static void event_Reborn(String Cname,Point Clocation,Direction Cdirection){
		assert(clientMap.get(Cname).client!=null);
		//Client target = clientMap.get(Cname).client;
		System.out.println("sdfsfsfsdf "+" "+Cname+" "+Clocation+" "+Cdirection);
		//while(maze.finished==false);
		//maze.finished=false;
		if(clientMap.get(Cname)!=null&&source!=null&&target!=null){
			
			if(!MyClientName.equals(Cname))
			maze.server_reborn_Client(Clocation, Cdirection, source, target);

			try{
				clientQueue.put(Cname);
				        

				
						    //clientQueue.put(target.getName());
				//System.out.println("-----------location and point----   "+target.getPoint() +"   " +target.getOrientation());
				clientMap.get(Cname).Update_Event(
						clientMap.get(Cname).client.getPoint(),
						clientMap.get(Cname).client.getOrientation(),
						MazePacket.CLIENT_REBORN,
						maze.get_score(Cname)
				);

				
			}catch(Exception e){
				e.printStackTrace();
			}
			
			//System.out.println("reborn orientation "+clientMap.get(packetFromClient.Cname).client.getOrientation());
			Broad_cast();
		}
	}


	public synchronized static void event_Move(int type, String name){
			try{
				switch(type){
							case MazePacket.CLIENT_FORWARD:
										clientMap.get(name).client.forward();
										break;
							case MazePacket.CLIENT_LEFT:
										clientMap.get(name).client.turnLeft();
										break;
							case MazePacket.CLIENT_RIGHT:
										clientMap.get(name).client.turnRight();
										break;
							case MazePacket.CLIENT_BACKWARD:
										clientMap.get(name).client.backup();
										break;
				
						}
				
				clientMap.get(name).Update_Event(
					clientMap.get(name).client.getPoint(),
					clientMap.get(name).client.getOrientation(),
					type,
					0
				);
			}catch(Exception e){
				e.printStackTrace();
			}
			Broad_cast();

	}

	public synchronized static void event_Quit(String name, String HN){
		try{
			System.out.println("execute the clietn quit now!");	
			
			Broad_cast();
			if(HN==MazeServer.myHostName){
				clientMap.get(name).fromClient.close();
				clientMap.get(name).toClient.close();
				clientMap.get(name).socket.close();
			}
			maze.removeClient(clientMap.get(name).client);
			clientMap.remove(name);
		}catch(Exception e){
			e.printStackTrace();
		}
		
			/*try{
				System.out.println(packetFromClient.Cname+" is QUITTING!!!");
			
			}catch(Exception e){
				e.printStackTrace();
			}*/

	}

}

