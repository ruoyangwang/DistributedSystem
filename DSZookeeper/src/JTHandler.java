import java.net.*;
import java.io.*;
import java.util.*;




public class JTHandler extends Thread {
		private Socket socket = null;
		ObjectInputStream fromClient = null;
		ObjectOutputStream toClient = null;
		zkPacket packetFromClient;

		
		public JTHandler(Socket socket){
			super("Handle client request");
			this.socket = socket;
			try{
				fromClient = new ObjectInputStream(this.socket.getInputStream());
				toClient = new ObjectOutputStream(socket.getOutputStream());	
				System.out.println("Created a new Thread to handle client");
			}catch(Exception e){
				e.printStackTrace();
			}

		}

		public void run(){
			try{
				while (( packetFromClient = (zkPacket) fromClient.readObject()) != null) {
					

				}
			}catch(IOException e1){
				try{
					fromClient.close();
					toClient.close();
					socket.close();
					fromClient = null;
					toClient = null;
					socket = null;
				}catch(Exception E){}

			}catch(ClassNotFoundException e2){
				e2.printStackTrace();
			}
		}
}
