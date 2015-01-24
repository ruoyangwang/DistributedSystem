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
		//static HashMap<String, String> clientMap = new HashMap<String,String>();

		public MazeClientHandlerThread(String host, int port){
			
			try{
				Server_Socket = new Socket(host,port);
				System.out.println("before??");
				out = new ObjectOutputStream(Server_Socket.getOutputStream());
				System.out.println("get here??");
				in = new ObjectInputStream(Server_Socket.getInputStream());
				System.out.println("establishing connection to server");
			}catch(Exception e){e.printStackTrace();}
		}
		
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
				System.out.println("here???");
				try{
					out.writeObject(packetToServer);
					System.out.println("registering into the map:  "+ self.getName());
					MazePacket packetFromServer;
					packetFromServer = (MazePacket) in.readObject();

					if(packetFromServer.type==MazePacket.CLIENT_REGISTER)
						System.out.println("Registered!");
					else if(packetFromServer.type==MazePacket.CLIENT_REGISTER_ERROR){
						System.out.println("Cannot register, please choose another name!");
						System.exit(0);	
					}
						
				}catch(Exception e){
					e.printStackTrace();
				}	
		}

		public void joinMaze(Maze maze){
			this.maze = maze;

		}
		
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
				MazePacket packetFromServer;
				packetFromServer = (MazePacket) in.readObject();
				
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



}
