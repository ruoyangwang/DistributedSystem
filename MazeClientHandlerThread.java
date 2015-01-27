import java.util.HashMap;
import java.util.List;
import java.io.*;
import java.net.*;

import java.awt.event.KeyEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

public class MazeClientHandlerThread {
		private Socket Server_Socket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		Maze maze;
		Client self;
		//ClientEventData clientData;
		MazePacket packetToServer;
		MazePacket packetFromServer;
		static ConcurrentHashMap<String, Client> clientMap = new ConcurrentHashMap<String,Client>();
		
		MazeClientHandlerReceiverThread receiver= null;
		MazeClientHandlerSenderThread 	sender	= null;

		public MazeClientHandlerThread(String host, int port){
			
			try{
				Server_Socket = new Socket(host,port);
				out = new ObjectOutputStream(Server_Socket.getOutputStream());
				in = new ObjectInputStream(Server_Socket.getInputStream());
				receiver = new MazeClientHandlerReceiverThread(Server_Socket, in, maze);
				sender   = new MazeClientHandlerSenderThread(Server_Socker, out, maze);
				receiver.start();
				sender.start();
				System.out.println("establishing connection to server");
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		public void run() {
			
			try {			
				//String DELIMITER="\\s+";	
				packetFromServer = new MazePacket();
				//first time register server first
				registerServer();
				
			}
			catch(Exception e){
			e.printStackTrace();
			}


		}
		/*
		//move to client receiver 
		public void update_map_register(){
			String Cname = packetFromServer.Cname;
			if(packetFromServer.type==MazePacket.CLIENT_REGISTER && packetFromServer.Cname.equals(self.getName())){
						System.out.println("Registered!");
						clientMap.put(self.getName(),self);
					}

			else if(packetFromServer.type==MazePacket.CLIENT_REGISTER_ERROR && packetFromServer.Cname.equals(self.getName())){
						System.out.println("Cannot register, please choose another name!");
						System.exit(0);	
			}

			else{
				this.maze.addClient(clientMap.get(Cname));
				
			}
		}
	
		public void update_map_forward(){



		}
		*/
		public void add_myself(Client guiClient){
			this.self=guiClient;
		}


		
		public void registerServer(){
				packetToServer = new MazePacket();
				packetToServer.Cname = self.getName();
				System.out.println(this.self.getName());
				
            			packetToServer.Cdirection = self.getOrientation();
				packetToServer.Clocation = self.getPoint();
				packetToServer.type = MazePacket.CLIENT_REGISTER;
				packetToServer.Ctype = 0;		//0 remote client, 1 robot
				try{
					out.writeObject(packetToServer);
					System.out.println("registering into the map:  "+ self.getName());
				}catch(Exception e){
					e.printStackTrace();
				}	
		}
		
		public void joinMaze(Maze maze){
			this.maze = maze;

		}
		/*
		//move to client receiver
		public void quit(){
			try{
				packetToServer.Cname = self.getName();
				packetToServer.type = MazePacket.CLIENT_QUIT;
				packetToServer.Ctype = 0;
				
				out.writeObject(packetToServer);
				System.out.println("quitting");
				MazePacket packetFromServer;
				packetFromServer = (MazePacket) in.readObject();
				
			}catch(Exception e){
				e.printStackTrace();
			}
			System.exit(0);
		
		}
		
		public void forward(){
			try{
				//packetToServer.newclient = self;
				packetToServer.Cname = self.getName();
            	packetToServer.Cdirection = self.getOrientation();
			
				packetToServer.type = MazePacket.CLIENT_FORWARD;
				packetToServer.Ctype = 0;
				
				out.writeObject(packetToServer);
				System.out.println("move forward");
			}catch(Exception e){
				e.printStackTrace();
			}
		
		}
		
		
		public void backward(){
		
		}
		
		public void turnLeft(){
		
		}
		
		public void turnRight(){
		
		
		}
		
		public void fire(){
		
		
		}

		*/

}
