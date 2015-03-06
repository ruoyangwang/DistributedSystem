import java.net.*;
import java.io.*;

public class ServerSendHandler extends Thread{

	
	public void run(){

		while(true){
			if(MazeServerHandler.sendQueue.size()!=0){
				System.out.println("found an event, let the other servers know");
				MazePacket packetToClient = new MazePacket();
				try{
				
					MazeServerHandler.increment_LamportClock();
					Serialized_Client_Data ServerData = MazeServerHandler.sendQueue.take();
					ServerData.Lamport = MazeServer.LamportClock;
					
					MazeServerHandler.add_One_Event(ServerData);
					
					packetToClient.ServerData = ServerData;
					packetToClient.type = ServerData.event;
					packetToClient.Cname = ServerData.Cname;
					for(ObjectOutputStream TC: MazeServerHandler.collection){
						if(TC!=null){
							TC.writeObject(packetToClient);
							System.out.println("send this event to the other side----- "+MazeServerHandler.sendQueue.size());
						}
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}

		}

	}
}
