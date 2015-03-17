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

	//static Lock eventLock = new ReentrantLock();
	static ServerSendHandler ServerSendHandler = null;		//holding a send pointer thread to send message to servers
	static BroadCastEvent BCE = null;
	static volatile ConcurrentHashMap<String, ClientEventData> clientMap = new ConcurrentHashMap<String, ClientEventData>();
	//static HashMap<String, int> peerServerMap = new HashMap<String, int>();
	//static boolean boss=true;

	static volatile BlockingQueue<String> clientQueue = new ArrayBlockingQueue<String>(1000);
	static volatile BlockingQueue<Serialized_Client_Data> sendQueue = new ArrayBlockingQueue<Serialized_Client_Data>(1000);
	static Object Lamport_Lock=new Object();
	
	
	static volatile Comparator<Serialized_Client_Data> eventComparator = new Comparator<Serialized_Client_Data>() {
		@Override
		public int compare(Serialized_Client_Data d1, Serialized_Client_Data d2) {
			
			return (d1.Lamport>d2.Lamport)? 1:-1;
			//else if (d1.Lamport<d2.Lamport) return 1;
			//else return 0;
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
	private static double newpid;
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
		String appendstr= "0."+MazeServer.pid;
		newpid=Double.parseDouble(appendstr);
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
					System.out.println("print the packet type   "+packetFromClient.type + "   "+ MazeServer.pid);
					
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
						Missile_Handle();
						break;
					    case MazePacket.ACK:
						Server_ACK();
						break;
					    case MazePacket.FINAL_EXIT:
					    	FINAL_EXIT();
						break;
				            
					}
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
		String CN = packetFromClient.Cname;
		
		//if(clientMap.get(CN)==null){
		//	return;
		//}

		while(waitForEvent){
			System.out.println("Inside ACK function:  .....  from Server:");
			boolean lost =true;
			System.out.println("+++++++++server ack ");
			for(Serialized_Client_Data SCD: eventList){
				System.out.println("~~~~~~~server ack ");
				if(SCD.Cname.equals(CN)&&SCD.Lamport==packetFromClient.ServerData.Lamport&& SCD.ACK <MazeServer.serverCount){
					
					waitForEvent = false;
					SCD.ACK+=1;
					lost=false;
					//System.out.println("~~~~~~~server found ack ");
					break;
					
				}
				
				}
			
			if(lost){
				System.out.print("\n\n\n!!!waiting for event, catch a lost ack\n"+CN+"\n"+packetFromClient.ServerData.Lamport);
				System.out.print("\nMy Lamport clock is "+MazeServer.LamportClock+"\n");
				
			}
		
		}
		System.out.println("Leave ACK function:  .....  from Server:");
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
			System.out.println("primove_Proj()nt out server records: "+key);
		
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
				if(packetFromClient.ServerData == null){				//client side joining
					guiClient.serverHostName = MazeServer.myHostName;
					guiClient.pid = MazeServer.pid;
					maze.addClient(guiClient,null,null);
					MyClientName=packetFromClient.Cname;
					}
			
				else{
					System.out.println("A client joined from another server||||  "+packetFromClient.ServerData.serverHostName);
					OtherSide = true;	
					guiClient.pid = packetFromClient.ServerData.pid;
					//if(guiClient.pid<MazeServer.pid&&boss)
					//boss=false;
					guiClient.serverHostName = packetFromClient.ServerData.serverHostName;
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
					/*
					increment_assign_LamportClock(SCD);
					tempLamport=SCD.Lamport;
					SCD.serverHostName = MazeServer.myHostName;
					add_One_Event(SCD);
					sendQueue.put(SCD);
					*/
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
			
				Server_Broad_cast(this.packetFromClient.Cname);
		}
		else{
			Error_sending(MazePacket.CLIENT_REGISTER_ERROR);
		}
	
	}
	


	public synchronized void Client_Quit(){
		System.out.println("client quitting");
		String CN = packetFromClient.Cname;
		double tempLamport=0;
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
					if(clientMap.size()>1){
					Serialized_Client_Data SCD= new Serialized_Client_Data(		//seriliazed version of above data, for passing into socket back to clients
										CN,
										clientMap.get(CN).client.getPoint(),
										clientMap.get(CN).client.getOrientation(),
										packetFromClient.Ctype,
										packetFromClient.type,
										packetFromClient.score
								);
					increment_assign_LamportClock(SCD);
					tempLamport=SCD.Lamport;
					SCD.serverHostName = MazeServer.myHostName;
					add_One_Event(SCD);
					sendQueue.put(SCD);
					}
					else{
						System.out.println("my self leaving "+clientMap.size());
						String name = this.packetFromClient.Cname;
						Server_Broad_cast(name);

						clientMap.get(name).fromClient.close();
						clientMap.get(name).toClient.close();
						clientMap.get(name).socket.close();
						Serialized_Client_Data SCD= new Serialized_Client_Data(		//seriliazed version of above data, for passing into socket back to clients
										null,
										null,
										null,
										0,
										MazePacket.FINAL_EXIT,
										0
								);
						sendQueue.put(SCD);
						clientMap.get(name).fromClient.close();
						clientMap.get(name).toClient.close();
						clientMap.get(name).socket.close();
						maze.removeClient(clientMap.get(name).client);
						clientMap.remove(name);
						maze.removeMazeListener(scoreModel);
						scoreModel=null;
						scoreModel=new ScoreTableModel();
						score_initialized=false;
						System.exit(1);
					}

				}
		
				else{			//request from server
					System.out.println("this is a request from server, not client .....");
					if(MazeServer.LamportClock < packetFromClient.ServerData.Lamport){
						update_LamportClock(packetFromClient.ServerData.Lamport);
					}
					tempLamport=packetFromClient.ServerData.Lamport;
					packetFromClient.ServerData.ACK+=1;
					add_One_Event(packetFromClient.ServerData);
					
						Serialized_Client_Data scd = new Serialized_Client_Data();
						scd.event = MazePacket.ACK;
						scd.Cname = CN;
						scd.Lamport=tempLamport;
						sendQueue.put(scd);
						

				}	

	
				
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
		else{
			Error_sending(MazePacket.CLIENT_QUIT_ERROR);
		}
		
	}
	
	private void FINAL_EXIT(){
	System.exit(1);
	}
	
	
	public synchronized void Missile_Handle(){
		System.out.println("got a Missile from other server "+ packetFromClient.type);
		String CN = packetFromClient.Cname;
		try{
			//clientQueue.put(CN);
			packetFromClient.ServerData.ACK+=1;
			add_One_Event(packetFromClient.ServerData);
			//sort_Event_List();											//sort event again based on new Lamport clock
			Serialized_Client_Data scd = new Serialized_Client_Data();
			scd.event = MazePacket.ACK;
			scd.Lamport=packetFromClient.ServerData.Lamport;
			scd.Cname = CN;
			//synchronized (sendQueue){
			//while(sendQueue.size()>1);
			sendQueue.put(scd);
			//}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		
	}

	public synchronized void Client_Move(){
		System.out.println("client moving "+ packetFromClient.type);
		boolean OtherSide = false;
		boolean myTurn = false;
		String CN = packetFromClient.Cname;
		

		
		if(clientMap.get(this.packetFromClient.Cname)!=null){
	
			try{
				/*serverData null means it's a client request not a request broadcast from server*/
				double tempLamport=0;
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
					increment_assign_LamportClock(SCD);
					SCD.serverHostName = MazeServer.myHostName;
					tempLamport=SCD.Lamport;
					System.out.println("=sd=sdf=df sendQueue size "+sendQueue.size());
					add_One_Event(SCD);
					sendQueue.put(SCD);
				}
		
				else{			//request from server
					System.out.println("this is a request from server, not client .....");
					if(MazeServer.LamportClock < packetFromClient.ServerData.Lamport){
						update_LamportClock(packetFromClient.ServerData.Lamport);
					}
					tempLamport=packetFromClient.ServerData.Lamport;
					packetFromClient.ServerData.ACK+=1;
					add_One_Event(packetFromClient.ServerData);
					
					
						Serialized_Client_Data scd = new Serialized_Client_Data();
						scd.event = MazePacket.ACK;
						scd.Lamport=tempLamport;
						scd.Cname = CN;
						sendQueue.put(scd);
					}
					
			
			}catch(Exception e){
					e.printStackTrace();
			}
		}
		else{
			Error_sending(MazePacket.CLIENT_REGISTER_ERROR);
		}
	}

	public synchronized void Client_Fire(){

		boolean OtherSide = false;
		boolean myTurn = false;
		String CN = packetFromClient.Cname;
		if(clientMap.get(this.packetFromClient.Cname)!=null){
	
			try{
				//clientQueue.put(this.packetFromClient.Cname);
				/*serverData null means it's a client request not a request broadcast from server*/
				double tempLamport=0;
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
					increment_assign_LamportClock(SCD);
					SCD.serverHostName = MazeServer.myHostName;
					tempLamport=SCD.Lamport;
					add_One_Event(SCD);
					sendQueue.put(SCD);
				}
		
				else{			//request from server
					System.out.println("this is a request from server, not client .....");
					if(MazeServer.LamportClock < packetFromClient.ServerData.Lamport){
						update_LamportClock(packetFromClient.ServerData.Lamport);
					}
					tempLamport=packetFromClient.ServerData.Lamport;
					packetFromClient.ServerData.ACK+=1;
					add_One_Event(packetFromClient.ServerData);
					
						Serialized_Client_Data scd = new Serialized_Client_Data();
						scd.event = MazePacket.ACK;
						scd.Cname = CN;
						scd.Lamport=tempLamport;
						sendQueue.put(scd);
						}
			
			}catch(Exception e){
					e.printStackTrace();
			}
		}
		else{
			Error_sending(MazePacket.CLIENT_REGISTER_ERROR);
		}
	}



	public synchronized void Client_Reborn(){	
		boolean OtherSide = false;
		boolean myTurn = false;
		String CN = packetFromClient.Cname;
		if(clientMap.get(this.packetFromClient.Cname)!=null){
	
			try{
				
				/*serverData null means it's a client request not a request broadcast from server*/
				double tempLamport;
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
					increment_assign_LamportClock(SCD);
					SCD.serverHostName = MazeServer.myHostName;
					tempLamport=SCD.Lamport;
					add_One_Event(SCD);
					sendQueue.put(SCD);
				}
		
				else{			//request from server
					System.out.println("this is a request from server, not client .....");
					if((int)MazeServer.LamportClock <(int) packetFromClient.ServerData.Lamport){
						update_LamportClock(packetFromClient.ServerData.Lamport);
					}
					tempLamport=packetFromClient.ServerData.Lamport;
					packetFromClient.ServerData.ACK+=1;
					add_One_Event(packetFromClient.ServerData);
					
						Serialized_Client_Data scd = new Serialized_Client_Data();
						scd.event = MazePacket.ACK;
						scd.Cname = CN;
						scd.Lamport=tempLamport;
						sendQueue.put(scd);	
					
				}
				//System.out.println("Size before get:  "+eventList.size());
				//System.out.println("Size after get:  "+eventList.size());
			
			}catch(Exception e){
					e.printStackTrace();
			}
		}
		else{
			Error_sending(MazePacket.CLIENT_REGISTER_ERROR);
		}
	}




	public static void Projectile_Update(){
	
			try{
				maze.move_Proj();
				
			}catch(Exception e){
				e.printStackTrace();
			}
			
	}

	
	
	public synchronized static void Broad_cast(){
		System.out.println("~~~~~~~~~~~ beginning?~~~~~~~  "+clientQueue.size() +"    "+clientQueue.peek());
		if(clientQueue.size()>0){
			String clientEvent;

			try{
				clientEvent = clientQueue.take();
				//System.out.println("what's the event type inside BORADCAST????  "+clientMap.get(clientEvent).event);
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
		System.out.println("~~~~~~~~~~~ in the end what's the size?~~~~~~~  "+clientQueue.size() +"    "+clientQueue.peek());
	}	

	
	public static void Server_Broad_cast(String clientEvent){
		System.out.println("Server_Broad_cast~~~~~~~~~~~ beginning?~~~~~~~");
		//if(clientQueue.size()>0){
		//	String clientEvent;

			try{
				//clientEvent = Cname;
				//System.out.println("what's the event type inside BORADCAST????  "+clientMap.get(clientEvent).event);
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
		//}
		System.out.println("~~~~~~~~~~~ in the end what's the size?~~~~~~~  "+clientQueue.size() +"    "+clientQueue.peek());
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



	public static void increment_LamportClock(){
		synchronized(Lamport_Lock){
		MazeServer.LamportClock+=1.0;
		System.out.println("~~~~~current clock is "+MazeServer.LamportClock);
		}
	}

	public static void increment_assign_LamportClock(Serialized_Client_Data SCD){
		synchronized(Lamport_Lock){
		MazeServer.LamportClock+=1.0;
		System.out.println("~~~~~current clock is "+MazeServer.LamportClock);
		
		SCD.Lamport=MazeServer.LamportClock+newpid;
		System.out.println("~~~~~Assign clock "+SCD.Lamport);
		}
		}
		
	public static void update_LamportClock(double val){
		synchronized(Lamport_Lock){
		if(val==0)
		System.out.print("\n\n\nwhat! update_LamportClock val is incorrect "+val);
		int _val=(int)val;
		MazeServer.LamportClock=(double)_val+1.0;
		System.out.println("~~~~~Update clock "+MazeServer.LamportClock);
		}
	}

	
	public static void add_One_Event(Serialized_Client_Data Data){
		synchronized(eventList){
		System.out.println("?????enter add_One_event");
		eventList.add(Data);
		eventList.peek();		//just for purpose of sorting
		System.out.println("after add get this event immediately to check   " +eventList.peek().Cname);
		}
	}
	
	public synchronized static boolean check_proj_event_list(){
		synchronized(eventList){
		System.out.print("\n");
		System.out.println("Print event list");
		for(Serialized_Client_Data SCD: eventList){
			System.out.println("Event "+SCD.Cname+" Lamport Clock "+SCD.Lamport +SCD.serverHostName);
				if(SCD.Cname.equals(ProjUpdate)&&SCD.serverHostName.equals(MazeServer.myHostName)){
				System.out.println("already has pro update event in envent list");
				return true;	
				}

			}
			
		}
		return false;
	}
	
	public static void print_event_list(){
		//synchronized(eventList){
		System.out.print("\n");
		System.out.println("Print event list");
		for(Serialized_Client_Data SCD: eventList){
			System.out.println("Event "+SCD.Cname+" Lamport Clock "+SCD.Lamport +" type "+SCD.event);
			}
			
		//}
	}

	public synchronized static void sort_Event_List(){
		//Collections.sort(eventList, eventComparator);
		eventList.peek();

	}

	public static void event_Fire(String name, String ServerHostName){
		if(clientMap.get(name)!=null){

			//System.out.println("client firing event"+ this.maze.get_score(packetFromClient.Cname)+"    "+packetFromClient.type);
			try{
				System.out.println("client firing event");
				clientQueue.put(name);
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
			//Server_Broad_cast(name);
		}
	}
	
	public static void event_Reborn(String Cname,Point Clocation,Direction Cdirection){
		assert(clientMap.get(Cname).client!=null);
		//Client target = clientMap.get(Cname).client;
		System.out.println("sdfsfsfsdf "+" "+Cname+" "+Clocation+" "+Cdirection);
		//while(maze.finished==false);
		//maze.finished=false;
		if(clientMap.get(Cname)!=null&&source!=null&&target!=null){
			
			if(!MyClientName.equals(Cname))
			maze.server_reborn_Client(Clocation, Cdirection, source, target);

			try{
				System.out.println("now reborn, but check the clientQueue First!!!!!! "+clientQueue.size()+"    "+clientQueue.peek());
				//clientQueue.take();
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
			//Server_Broad_cast(Cname);
		}
	}


	public static void event_Move(int type, String name){
			try{
				clientQueue.put(name);
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
			System.out.println("asdsdfsd event Move ready to enter bc");
			//Server_Broad_cast(name);

	}

	public synchronized static void event_Quit(String name, String HN){
		try{
			System.out.println("execute the clietn quit now!");	
			
			Server_Broad_cast(name);
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

	}

}

