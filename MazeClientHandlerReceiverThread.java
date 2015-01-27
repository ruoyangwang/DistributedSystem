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
	public MazeClientHandlerReceiverThread(Socket client_socket, ObjectInputStream input_stream, ConcurrentHashMap<String, Client> _clientMap){
		if(client_socket == null || input_stream== null || _clientMap == null){
			System.out.println("Error: Client Receiver argument is null");	
			System.exit(0);
		}
		in = input_stream;
		clsocket = client_socket;
		clientMap = _clientMap;
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


}
