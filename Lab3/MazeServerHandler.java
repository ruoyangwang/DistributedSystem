
import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Random;
import java.util.Vector; 

public class MazeServerHandler extends Thread{
	private Socket socket = null;

	static ServerSendHandler ServerSendHandler = null;		//holding a send pointer thread to send message to servers
	static ConcurrentHashMap<String, ClientEventData> clientMap = new ConcurrentHashMap<String, ClientEventData>();
	//static HashMap<String, int> peerServerMap = new HashMap<String, int>();

	static BlockingQueue<String> clientQueue = new ArrayBlockingQueue<String>(100);
	static BlockingQueue<Serialized_Client_Data> sendQueue = new ArrayBlockingQueue<Serialized_Client_Data>(100);
	/*Priority queue with comparable to sort */
	static PriorityQueue<Serialized_Client_Data> eventQueue= new PriorityQueue<Serialized_Client_Data>(100);
	static ClientEventData clientData ;
	static Serialized_Client_Data S_ClientData;
	Client self;
        
	MazePacket packetFromClient;
	static boolean score_initialized = false;
	static boolean firstTime = true;

	static ObjectInputStream fromClient= null;
	static ObjectOutputStream toClient = null;
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

					}
		              if(packetFromClient.type==MazePacket.CLIENT_QUIT){
						this.maze.removeClient(clientMap.get(packetFromClient.Cname).client);
						this.clientMap.remove(packetFromClient.Cname);
		              	break;
					}
				}
		
		
		
				from_Client.close();
				to_Client.close();
				socket.close();
			
		}catch (IOException e) {
			if(!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
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
			
				ServerSendHandler.start();
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
		for(String key:MazeServer.peerServerMap.keySet())
			System.out.println("print out server records: "+key);
		
		collection.add(toClient);		//collection of server outputStream, for later sending purpose
		
		if(this.ServerSendHandler==null){
			this.ServerSendHandler= new ServerSendHandler();	
			ServerSendHandler.start();
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
				if(packetFromClient.ServerData == null)
					maze.addClient(guiClient,null,null);
			
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

					S_ClientData = new Serialized_Client_Data(		//seriliazed version of above data, for passing into socket back to clients
										guiClient.getName(),
										guiClient.getPoint(),
										guiClient.getOrientation(),
										packetFromClient.Ctype,
										packetFromClient.type,
										maze.get_score(guiClient.getName())
								);
				}
				else{
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
			
				Server_Broadcast();
			
				Broad_cast();
		}
		else{
			Error_sending(MazePacket.CLIENT_REGISTER_ERROR);
		}
	
	}
	


	public synchronized void Client_Quit(){
		System.out.println("client quitting");
		if(clientMap.get(this.packetFromClient.Cname)!=null){
			try{
				
				clientMap.get(packetFromClient.Cname).Update_Event(
					packetFromClient.Clocation,
					packetFromClient.Cdirection,
					packetFromClient.type,
					0
				);
				clientQueue.put(this.packetFromClient.Cname);
				
			}catch(Exception e){
				e.printStackTrace();
			}
			Broad_cast();
			try{
				System.out.println(packetFromClient.Cname+" is QUITTING!!!");
			
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
		if(clientMap.get(this.packetFromClient.Cname)!=null){
	
			try{
				clientQueue.put(this.packetFromClient.Cname);
				
				switch(packetFromClient.type){
					case MazePacket.CLIENT_FORWARD:
								clientMap.get(packetFromClient.Cname).client.forward();
								break;
					case MazePacket.CLIENT_LEFT:
								clientMap.get(packetFromClient.Cname).client.turnLeft();
								break;
					case MazePacket.CLIENT_RIGHT:
								clientMap.get(packetFromClient.Cname).client.turnRight();
								break;
					case MazePacket.CLIENT_BACKWARD:
								clientMap.get(packetFromClient.Cname).client.backup();
								break;
				
				}
				clientMap.get(packetFromClient.Cname).Update_Event(
					clientMap.get(packetFromClient.Cname).client.getPoint(),
					clientMap.get(packetFromClient.Cname).client.getOrientation(),
					packetFromClient.type,
					0
				);
			}catch(Exception e){
				e.printStackTrace();
			}
			Broad_cast();
		}
		else{
			Error_sending(MazePacket.CLIENT_REGISTER_ERROR);
		}
	}
	



	public synchronized void Client_Fire(){
		
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
	}



	public synchronized void Client_Reborn(){
		
		/*
		Calculate the reborn location and position
		*/
		//System.out.println("score for reborn "+packetFromClient.score);
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
	}




	public synchronized static void Projectile_Update(){
			try{
			clientQueue.put(ProjUpdate);
			Broad_cast();
			}catch(Exception e){
				e.printStackTrace();
			}
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


	
	
	public synchronized static void Error_sending(int err_code){
		try{
			MazePacket packetToClient = new MazePacket();
			packetToClient.type = err_code;
			toClient.writeObject(packetToClient);
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
	

}

