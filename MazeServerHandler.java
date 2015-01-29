import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Random;
import java.util.Vector; 

public class MazeServerHandler extends Thread{
	private Socket socket = null;
	static ConcurrentHashMap<String, ClientEventData> clientMap = new ConcurrentHashMap<String, ClientEventData>();
	static  BlockingQueue<String> clientQueue = new ArrayBlockingQueue<String>(100);
	static ClientEventData clientData ;
	static Serialized_Client_Data S_ClientData;
	Client self;


	MazePacket packetFromClient;
	


	static ObjectInputStream fromClient= null;
	static ObjectOutputStream toClient = null;
	//private static final Random randomGen;


	//-----------------------create a map on server side-------------------------
	private static final int mazeHeight = 10;
	private static final int mazeWidth = 20;
	private static final int mazeSeed = 42;
	//---------------------------------------------------------------------------
	static Maze maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);

	public MazeServerHandler(Socket socket){
		super("MazeServerHandlerThread");
		this.socket = socket;
		System.out.println("Created new Thread to handle client");
	}


	public void run() {
		boolean gotByePacket = false;
		try{				
				ObjectInputStream from_Client = new ObjectInputStream(socket.getInputStream());
				this.fromClient = from_Client;
			
				/* stream to write back to client */
				ObjectOutputStream to_Client = new ObjectOutputStream(socket.getOutputStream());
				this.toClient= to_Client;

				//System.out.println("toClient Object address of this client:     ------  "+toClient);
				while (( packetFromClient = (MazePacket) from_Client.readObject()) != null) {
					  switch (packetFromClient.type) {
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
						/*case MazePacket.CLIENT_UPDATE:			//update the event for future new clients to know previous
						    Client_Update();
						     break;*/
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

	
	

	public synchronized static Direction generate_direction(){
		Direction d = Direction.random();
		return d;
	}
	
	public void Client_Register(){
		//this.packetToClient = new MazePacket();
		System.out.println("client registering for the first time");
		if(clientMap.get(this.packetFromClient.Cname)==null){
			GUIClient guiClient = new GUIClient(packetFromClient.Cname);
			maze.addClient(guiClient,null,null);
			System.out.println("print new client's point and direction:  "+guiClient.getName()+"  "+guiClient.getPoint().toString()+"  "+guiClient.getOrientation());
		
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
			
			try{
				clientMap.put(this.packetFromClient.Cname, clientData);
				clientQueue.put(this.packetFromClient.Cname);
				
			}catch(Exception e){
				e.printStackTrace();
			}
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
					packetFromClient.type
				);
				clientQueue.put(this.packetFromClient.Cname);
				
			}catch(Exception e){
				e.printStackTrace();
			}
			Broad_cast();
			try{
				System.out.println(packetFromClient.Cname+" is QUITTING!!!");
				//clientMap.get(packetFromClient.Cname).toClient.close();
				//clientMap.get(packetFromClient.Cname).socket.close();
				/*fromClient.close();
				toClient.close();
				socket.close();*/
				//System.out.println("quitting error check 1");
				//this.maze.removeClient(clientMap.get(packetFromClient.Cname).client);
				//System.out.println("quitting error check 2");
				//clientMap.remove(packetFromClient.Cname);
				//System.out.println("quitting error check 3");
				
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
					packetFromClient.type
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
	

	public void Client_Fire(){
		System.out.println("client moving "+ packetFromClient.type);
		if(clientMap.get(this.packetFromClient.Cname)!=null){
			
			try{
				clientQueue.put(this.packetFromClient.Cname);
				clientMap.get(packetFromClient.Cname).Update_Event(
					packetFromClient.Clocation,
					packetFromClient.Cdirection,
					packetFromClient.type
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

	
	
	
	public synchronized static void Broad_cast(){
		while(clientQueue.size()>0){
			String clientEvent;
			try{
				clientEvent = clientQueue.take();
				for (String key: clientMap.keySet()) {
					//Socket holderSocket = clientMap.get(key).socket;
					
					MazePacket packetToClient = new MazePacket();
					if(clientEvent.equals(key)){
						int i = 0;
						for (String key2: clientMap.keySet()) {
							S_ClientData = new Serialized_Client_Data(		//seriliazed version of above data, for passing into socket back to clients
								clientMap.get(key2).Cname,
								clientMap.get(key2).Clocation,
								clientMap.get(key2).Cdirection,
								clientMap.get(key2).Ctype,
								clientMap.get(key2).event
							);
							packetToClient.clientData[i]= S_ClientData;
							i+=1;
						}
					}
					packetToClient.Cname = clientMap.get(clientEvent).Cname;
					packetToClient.Clocation = clientMap.get(clientEvent).client.getPoint();
					packetToClient.Cdirection = clientMap.get(clientEvent).client.getOrientation();
					packetToClient.type = clientMap.get(clientEvent).event;
					//System.out.println("broadcast client location    "+clientMap.get(clientEvent).Clocation.getX() +":::"+clientMap.get(clientEvent).Clocation.getY());
					try{
						/* send reply back to client */
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

}
