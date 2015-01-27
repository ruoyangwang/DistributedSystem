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
	public MazeClientHandlerReceiverThread(Socket client_socket, ObjectInputStream input_stream, ConcurrentHashMap<String, Client> _clientMap){
		if(client_socket == null || input_stream== null || _clientMap == null){
			System.out.println("Error: Client Receiver argument is null");	
			System.exit(0);
		}
		in = input_stream;
		clsocket = client_socket;
		clientMap = _clientMap;
	}


}
