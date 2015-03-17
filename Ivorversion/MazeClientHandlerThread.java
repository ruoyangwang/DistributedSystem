import java.util.List;
import java.io.*;
import java.net.*;

import java.awt.event.KeyEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

public class MazeClientHandlerThread extends Thread {
		private Socket Server_Socket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		Maze maze;
		//private MazeImpl mazeimpl;
		public Client self;
		public Client source;
		//ClientEventData clientData;
		//MazePacket packetToServer;
		MazePacket packetFromServer;
		static ConcurrentHashMap<String, Client> clientMap = new ConcurrentHashMap<String,Client>();
		static String Deadclient;

		public MazeClientHandlerThread(String host, int port){
			
			try{
				Server_Socket = new Socket(host,port);
				out = new ObjectOutputStream(Server_Socket.getOutputStream());
				in = new ObjectInputStream(Server_Socket.getInputStream());
				System.out.println("establishing connection to server");
				
			}catch(Exception e){e.printStackTrace();}
		}
		
		public void run() {
			
			try {			
				String DELIMITER="\\s+";	
				//packetFromServer = new MazePacket();
				//registerServer();		//first time register server first
				while (( packetFromServer = (MazePacket) in.readObject()) != null) {
					System.out.println("Inside while loop run: "+packetFromServer.type);
					switch (packetFromServer.type) {
						case MazePacket.CLIENT_REGISTER:
								update_map_register();
								break;
						case MazePacket.CLIENT_FORWARD:
						case MazePacket.CLIENT_LEFT:
						case MazePacket.CLIENT_RIGHT:
						case MazePacket.CLIENT_BACKWARD:
						case MazePacket.CLIENT_REGISTER_ERROR:
								update_map_movement();
								break;
						case MazePacket.CLIENT_FIRE:
								update_fire();
								break;
						case MazePacket.CLIENT_REBORN:
								//Thread.sleep(1000);
								update_reborn();
								break;
						case MazePacket.PROJ_UPDATE:
								update_projectile();
								break;

						case MazePacket.CLIENT_QUIT:
								update_map_quit();
								break;
					}
					System.out.println("check final exception");


				}
			}catch(Exception e){e.printStackTrace();}


		}
		
		public void update_map_register(){
			String Cname = packetFromServer.Cname;
			if(packetFromServer.type==MazePacket.CLIENT_REGISTER && packetFromServer.Cname.equals(self.getName())){
						System.out.println("Registered!   "+packetFromServer.Clocation.getX()+":::"+packetFromServer.Clocation.getY());
						//clientMap.put(self.getName(),self);
						this.maze.addClient(self, packetFromServer.Clocation, packetFromServer.Cdirection);
						//System.out.println("my new location "+self.getPoint());
					 	for(Serialized_Client_Data data : packetFromServer.clientData){
							if(data!=null){
								//System.out.println("put previous client into my map");
								if(data.Cname.equals(self.getName())==false){
									System.out.println("add client and its score: ----"+data.Cname+"-----"+data.score);
									RemoteClient RC = new RemoteClient(data.Cname);
									clientMap.put(data.Cname,RC);	
									this.maze.addRemoteClient(RC, data.Clocation, data.Cdirection, data.score);
								}
							}

						}

					}

			else if(packetFromServer.type==MazePacket.CLIENT_REGISTER_ERROR && packetFromServer.Cname.equals(self.getName())){
						clientMap.remove(self.getName());
						System.out.println("Cannot register, please choose another name!");
						System.exit(0);	
			}

			else{
				System.out.println("Other client has registered! -----"+ packetFromServer.Clocation);
				RemoteClient RC = new RemoteClient(Cname);
				clientMap.put(Cname,RC);	
				this.maze.addRemoteClient(RC, packetFromServer.Clocation, packetFromServer.Cdirection,0);
			}
		}
	
		public void update_map_movement(){
			String Cname = packetFromServer.Cname;
			//assert(clientMap.get(Cname)!=null);
			System.out.println(packetFromServer.Cname+"---->  ready to take action");
			for (String key2: clientMap.keySet()) {
				System.out.println("check clients: "+key2);
			}
			switch(packetFromServer.type){
				case MazePacket.CLIENT_FORWARD:
							clientMap.get(Cname).forward();
							break;
				case MazePacket.CLIENT_LEFT:
							clientMap.get(Cname).turnLeft();
							break;
				case MazePacket.CLIENT_RIGHT:
							clientMap.get(Cname).turnRight();
							break;
				case MazePacket.CLIENT_BACKWARD:
							clientMap.get(Cname).backup();
							break;
				case MazePacket.CLIENT_REGISTER_ERROR:
							System.out.println(Cname+" client has not yet registered in server or already registered(please use another name)!");
							break;
				}
			/*if(self.getName().equals(packetFromServer.Cname)){
				try{
					MazePacket packetToServer = new MazePacket();
					packetToServer.Cname = self.getName();
		        	packetToServer.Cdirection = self.getOrientation();
					packetToServer.Clocation = self.getPoint();
					packetToServer.type = MazePacket.CLIENT_UPDATE;
					packetToServer.Ctype = 0;
					out.writeObject(packetToServer);
					System.out.println("Update client position to server:  "+packetToServer.type);
				
				}catch(Exception e){
					e.printStackTrace();
				}
			}*/

		}
		
		public void update_fire(){
			String Cname = packetFromServer.Cname;
			//assert(clientMap.get(Cname)!=null);
			System.out.println(packetFromServer.Cname+"---->  fire");
			clientMap.get(Cname).fire();
			System.out.println(this.maze.get_score(Cname));
		}

		public void update_reborn(){
			System.out.println(packetFromServer.Cname+"---->  reborn");
			//String Cname = packetFromServer.Cname;
			//clientMap.put(client, new DirectedPoint(packetFromServer.Clocation,packetFromServer.Cdirection));
			System.out.println("new location "+packetFromServer.Clocation);
			System.out.println("new orientation "+packetFromServer.Cdirection);
			//assert(mazeimpl!=null);
			//if(mazeimpl==null)
			//System.exit(0);
			System.out.println("Client name: "+clientMap.get(packetFromServer.Cname).getName());
			maze.reborn_Client(packetFromServer.Clocation, packetFromServer.Cdirection, this.source, clientMap.get(packetFromServer.Cname));//packetFromServer.Cname);
		}

		public void update_projectile(){
			maze.projectileCheck();
		}


		public void update_map_quit(){
			/*me quitting */
			String Cname = packetFromServer.Cname;
			if(packetFromServer.type==MazePacket.CLIENT_QUIT && packetFromServer.Cname.equals(self.getName())){
				System.out.println("myself quitting, bye");
				try{
					//clientMap.remove(Cname);
					out.close();
					in.close();
					Server_Socket.close();
				}catch(Exception e){
					e.printStackTrace();
				}	
				System.exit(-1);
			
			}
			
			/*someone else quitting*/
			else{
				System.out.println("Some others quitting: ----- "+clientMap.get(Cname).getName());
				this.maze.removeClient(clientMap.get(Cname));
				System.out.println("Check exception 1");					
			}
		
		}
		
		
		
		public void add_myself(Client guiClient){
			this.self=guiClient;
		}



		public void registerServer(){
				MazePacket packetToServer = new MazePacket();
				packetToServer.Cname = self.getName();
				System.out.println(this.self.getName());
				
            			packetToServer.Cdirection = null;
				packetToServer.Clocation = null;
				packetToServer.type = MazePacket.CLIENT_REGISTER;
				packetToServer.Ctype = 0;		//0 remote client, 1 robot
				try{
					out.writeObject(packetToServer);
					System.out.println("registering into the map:  "+ self.getName());
					clientMap.put(self.getName(),self);
					if((packetFromServer = (MazePacket) in.readObject()) != null)
					{
						if(packetFromServer.type==MazePacket.CLIENT_REGISTER){
								System.out.println("Print my location and direction:  "+ packetFromServer.Cname+" "+packetFromServer.Clocation.toString()+" "+packetFromServer.Cdirection);
								update_map_register();	
						}
					}
					else{
						System.out.println("Cannot register  "+ self.getName());
					}
						
				}catch(Exception e){
					e.printStackTrace();
				}	
		}

		public void joinMaze(Maze maze){
			this.maze = maze;
			maze.CHT = this;

		}
		
		public void quit(){
			try{
				MazePacket packetToServer = new MazePacket();
				packetToServer.Cname = self.getName();
				packetToServer.type = MazePacket.CLIENT_QUIT;
				packetToServer.Ctype = 0;
				
				out.writeObject(packetToServer);
				System.out.println("quitting");
				//MazePacket packetFromServer;
				//packetFromServer = (MazePacket) in.readObject();
				
			}catch(Exception e){
				e.printStackTrace();
			}
			//System.exit(0);
		
		}
		
		public void forward(){
			try{
				//packetToServer.newclient = self;
				MazePacket packetToServer = new MazePacket();
				packetToServer.Cname = self.getName();
            			packetToServer.Cdirection = self.getOrientation();
				packetToServer.Clocation = self.getPoint();
				packetToServer.type = MazePacket.CLIENT_FORWARD;
				packetToServer.Ctype = 0;
				
				out.writeObject(packetToServer);
				System.out.println("move forward+ type:  "+packetToServer.type);
				
			}catch(Exception e){
				e.printStackTrace();
			}
		
		}
		
		
		public void backward(){
			try{
				//packetToServer.newclient = self;
				MazePacket packetToServer = new MazePacket();
				packetToServer.Cname = self.getName();
            		packetToServer.Cdirection = self.getOrientation();
				packetToServer.Clocation = self.getPoint();
				packetToServer.type = MazePacket.CLIENT_BACKWARD;
				packetToServer.Ctype = 0;
				
				out.writeObject(packetToServer);
				System.out.println("move backward");
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		public void turnLeft(){
			try{
				//packetToServer.newclient = self;
				MazePacket packetToServer = new MazePacket();
				packetToServer.Cname = self.getName();
            		packetToServer.Cdirection = self.getOrientation();
				packetToServer.Clocation = self.getPoint();
				packetToServer.type = MazePacket.CLIENT_LEFT;
				packetToServer.Ctype = 0;
				
				out.writeObject(packetToServer);
				System.out.println("turn left");
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		public void turnRight(){
			try{
				//packetToServer.newclient = self;
				MazePacket packetToServer = new MazePacket();
				packetToServer.Cname = self.getName();
            		packetToServer.Cdirection = self.getOrientation();
				packetToServer.Clocation = self.getPoint();
				packetToServer.type = MazePacket.CLIENT_RIGHT;
				packetToServer.Ctype = 0;
				
				out.writeObject(packetToServer);
				System.out.println("turn right");
				
			}catch(Exception e){
				e.printStackTrace();
			}
		
		}
		
		public void fire(){
			try{
				MazePacket packetToServer = new MazePacket();
				packetToServer.Cname = self.getName();
            		packetToServer.Cdirection = self.getOrientation();
				packetToServer.Clocation = self.getPoint();
				packetToServer.type = MazePacket.CLIENT_FIRE;
				packetToServer.Ctype = 0;
				//packetToServer.score= this.maze.get_score(self.getName())-1;
				out.writeObject(packetToServer);
				System.out.println("fire");
				
			}catch(Exception e){
				e.printStackTrace();
			}

		
		}

		
		public void reborn(String _name){
			Deadclient=_name;
			try{

				MazePacket packetToServer = new MazePacket();
				packetToServer.Cname = self.getName();
				packetToServer.type = MazePacket.CLIENT_REBORN;
				packetToServer.Ctype = 0;
				packetToServer.score= this.maze.get_score(self.getName());
				System.out.println("Client "+self.getName()+" send reborn request.");
				out.writeObject(packetToServer);
			}catch(Exception e){
				e.printStackTrace();
			}
		}




}
