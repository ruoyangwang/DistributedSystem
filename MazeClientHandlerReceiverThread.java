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

public class MazeClientHandlerReceiverThread extends Thread{
	
	//client socket
	private Socket clsocket = null;
	//porinter to input stream
	private ObjectInputStream in = null;
	//packet from server
	MazePacket packetFromServer;
	//maze
 	Maze maze;
	//gui client
        Client self = null;
	private ConcurrentHashMap<String, Client> clientMap = null;
/******************************************************************
 *Constructor							  * 
 *Arguments: 							  *
 *Socket client_socket:						  *
 *				the network socket create in	  *
 *				MazeClientHandlerThread	   	  *
 *								  *
 *ObjectInputStream input_stream:				  *
 *				the socket input stream, used	  *
 *				to grap packets			  *
 *								  *
 *ConcurrentHashMap<String, Client> clientMap:			  *
 *				pointer to the map which          *
 *				stores client info                *
 ******************************************************************/
	public MazeClientHandlerReceiverThread(Socket client_socket, ConcurrentHashMap<String, Client> _clientMap, Maze _maze){
		if(client_socket == null || _clientMap == null){
			System.out.println("Receiver: Client Receiver argument is null. Exiting");	
			System.exit(0);
		}
		clsocket = client_socket;
		try{
			in = new ObjectInputStream(clsocket.getInputStream());
		}
		catch(Exception e){
			System.out.println("Receiverer: Fail to get the InputStream from the client socket. Exiting");
			e.printStackTrace();
		}
		clientMap = _clientMap;
		maze = _maze;
	}


	public void run() {
			
			try {			
				packetFromServer = new MazePacket();			
				//move to client receiver
				while (( packetFromServer = (MazePacket) in.readObject()) != null) {
					System.out.println("Inside while loop run");
					switch (packetFromServer.type) {
						case MazePacket.CLIENT_REGISTER:
								update_map_register();
								break;
						case MazePacket.CLIENT_FORWARD:
								update_map_forward();
								break;
					}



				}
			}
			catch(Exception e){
				e.printStackTrace();
			}

	}
		
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

	
	public void joinMaze(Maze _maze){
			maze=_maze;
		}
	public void add_myself(Client _Client){
			self=_Client;
		}

 	public void update_map_forward(){
		System.out.println("Moving Forward.");
		}

	public void close(){
		try{
			in.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		System.exit(0);
	}
}
