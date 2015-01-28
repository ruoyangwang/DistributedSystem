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


public class MazeClientHandlerSender{
	

	//ObjectInputStream in = null;
	private Socket clsocket = null;
	ConcurrentHashMap<String, Client> clientMap = null;
	MazePacket packetToServer;
	ObjectOutputStream out = null;
	//gui client
        Client self = null;

/******************************************************************
 *Constructor							  * 
 *Arguments: 							  *
 *Socket client_socket:						  *
 *				the network socket create in	  *
 *				MazeClientHandlerThread	   	  *
 *								  *
 *ObjectInputStream output_stream:				  *
 *				the socket input stream, used	  *
 *				to grap packets			  *
 *								  *
 *ConcurrentHashMap<String, Client> clientMap:			  *
 *				pointer to the map which          *
 *				stores client info                *
 ******************************************************************/
	public MazeClientHandlerSender(Socket client_socket){
		if(client_socket == null){
			System.out.println("Sender: Client Sender argument is null. Exiting");	
			System.exit(0);
		}
		clsocket = client_socket;
		try{
			out = new ObjectOutputStream(clsocket.getOutputStream());
		}
		catch(Exception e){
			System.out.println("Sender: Fail to get the OutputStream from the client socket. Exiting");
			e.printStackTrace();
		}
		
	}

	public void quit(){
			try{
				packetToServer.Cname = self.getName();
				packetToServer.type = MazePacket.CLIENT_QUIT;
				packetToServer.Ctype = 0;
				
				out.writeObject(packetToServer);
				System.out.println("Sender: Exiting");
				MazePacket packetFromServer;
				//packetFromServer = (MazePacket) in.readObject();
				
			}catch(Exception e){
				e.printStackTrace();
			}

			try{
				clsocket.close();
			}
			catch (Exception e){
				e.printStackTrace();
			}
			System.exit(0);
		
		}

	
	public void registerserver(Client self){
				this.self=self;
				packetToServer= new MazePacket();
				packetToServer.Cname = self.getName();
				System.out.println(self.getName());
				
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

	public void forward(){
		System.out.println("Sender: Moving forward");
	}

	public void backward(){
		System.out.println("Sender: Moving backward");
	}
	
	public void turnLeft(){
		System.out.println("Sender: Turning Left");
	}

	public void turnRight(){
		System.out.println("Sender: Turning Right");
	}

	public void fire(){
		System.out.println("Sender: Fire!!");
	}



}
