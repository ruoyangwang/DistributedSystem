import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

public class MazeServerHandler extends Thread{
	private Socket socket = null;
	static ConcurrentHashMap<String, ClientEventData> clientMap = new ConcurrentHashMap<String, ClientEventData>();
	static  BlockingQueue<String> clientQueue = new ArrayBlockingQueue<String>(100);
	static ClientEventData clientData ;
	Client self;
	MazePacket packetFromClient;
	MazePacket packetToClient;
	static ObjectInputStream fromClient= null;
	static ObjectOutputStream toClient = null;
	public MazeServerHandler(Socket socket){
		super("MazeServerHandlerThread");
		this.socket = socket;
		System.out.println("Created new Thread to handle client");
	}


	public void run() {
		boolean gotByePacket = false;
		try{
				fromClient = new ObjectInputStream(socket.getInputStream());
				
			
				/* stream to write back to client */
				toClient = new ObjectOutputStream(socket.getOutputStream());
				while (( packetFromClient = (MazePacket) fromClient.readObject()) != null) {
					  switch (packetFromClient.type) {
		                case MazePacket.CLIENT_REGISTER:
		                    Client_Register();
		                    break;
		                case MazePacket.CLIENT_QUIT:
		                    Client_Quit();
		                    break;
		                case MazePacket.CLIENT_FORWARD:
		                    Client_Forward();
		                    break;  
		                case MazePacket.CLIENT_LEFT:
		                    Client_Left();
		                    break;  
		                    
		                case MazePacket.CLIENT_RIGHT:
		                    Client_Right();
		                    break;  
		                case MazePacket.CLIENT_BACKWARD:
		                    Client_Backward();
		                    break;  
		              }
			
				}
		
		
		
				fromClient.close();
				toClient.close();
				socket.close();
			
		}catch (IOException e) {
			if(!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
	}
	
	
	public void Client_Register(){
		//this.packetToClient = new MazePacket();
		System.out.println("client registering for the first time");
		if(clientMap.get(this.packetFromClient.Cname)==null){
			System.out.println("client does not exist");
			//System.out.println(this.packetFromClient.Cdirection);
			//System.out.println(this.packetFromClient.Clocation);
			clientData = new ClientEventData(
							packetFromClient.Cname,
							packetFromClient.Clocation,
							packetFromClient.Cdirection,
							packetFromClient.Ctype,
							packetFromClient.type,	//event type the same as MazePacket event type
							this.socket			
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
	
	public void Client_Quit(){
		System.out.println("client quitting");
	
	}

	public void Client_Forward(){
		System.out.println("client moving forward");
		if(clientMap.get(this.packetFromClient.Cname)!=null){
			System.out.println("found client, can execute forward motion");
			//System.out.println(this.packetFromClient.Cdirection);
			//System.out.println(this.packetFromClient.Clocation);
			clientData = new ClientEventData(
							packetFromClient.Cname,
							packetFromClient.Clocation,
							packetFromClient.Cdirection,
							packetFromClient.Ctype,
							packetFromClient.type,	//event type the same as MazePacket event type
							this.socket			
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
	
	public void Client_Left(){
		System.out.println("client turning left");
	
	}
	
	public void Client_Right(){
		System.out.println("client turning right");
	
	}
	
	public void Client_Backward(){
		System.out.println("client turning backward");
	
	}
	
	public static void Broad_cast(){
		//System.out.println("registered client name is : "+this.clientData.get_client(cname));
		//System.out.println(this.clientData);
		while(clientQueue.size()>0){
			String clientEvent;
			try{
				clientEvent = clientQueue.take();
				for (String key: clientMap.keySet()) {
				Socket holderSocket = clientMap.get(key).socket;
				
				MazePacket packetToClient = new MazePacket();
				packetToClient.Cname = clientMap.get(clientEvent).Cname;
				packetToClient.Clocation = clientMap.get(clientEvent).Clocation;
				packetToClient.Cdirection = clientMap.get(clientEvent).Cdirection;
				packetToClient.type = clientMap.get(clientEvent).event;
				
				System.out.println(packetToClient.Cname+packetToClient.Cdirection);

				try{
					/* send reply back to client */
					toClient.writeObject(packetToClient);
				}catch (Exception e) {
					e.printStackTrace();
				}
				
				}
			}catch (Exception e) {
					e.printStackTrace();
			}

			
			

		}
	}	
	
	
	public void Error_sending(int err_code){
		try{
			packetToClient.type = err_code;
			/* stream to write back to client */
			/* send reply back to client */
			toClient.writeObject(packetToClient);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

}
