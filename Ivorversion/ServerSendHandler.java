import java.net.*;
import java.io.*;

public class ServerSendHandler extends Thread{

	
	public void run(){

		while(true){
			
				try{
				
					Serialized_Client_Data ServerData = MazeServerHandler.sendQueue.take();
					if(ServerData!=null){
					MazePacket packetToClient = new MazePacket();
					ServerData.pid = MazeServer.pid;
					
					packetToClient.ServerData = ServerData;
					
					packetToClient.type = ServerData.event;
					//System.out.println("Server sender sent to "+packetToClient.ServerData.Cname+" "+packetToClient.type+"lamport "+packetToClient.ServerData.Lamport);
					packetToClient.Cname = ServerData.Cname;
					for(ObjectOutputStream TC: MazeServerHandler.collection){
						if(TC!=null){
							TC.writeObject(packetToClient);
							
						}
					}
					System.out.println("Server sender done one sending "+MazeServerHandler.eventList.size());
					System.out.println("Server sender done one sending "+MazeServerHandler.sendQueue.size());
					}
					
				}catch(Exception e){
					e.printStackTrace();
				}

		}

	}
}
