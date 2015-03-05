import java.net.*;
import java.io.*;

public class ServerSendHandler extends Thread{

	ObjectOutputStream toClient;
	
	public ServerSendHandler(ObjectOutputStream toClient){
		
		this.toClient = toClient;
	}

	public void run(){

		while(true){
			if(MazeServerHandler.sendQueue.size()!=0){
				MazePacket packetToClient = new MazePacket();
				try{
					packetToClient.ServerData = MazeServerHandler.sendQueue.take();
					this.toClient.writeObject(packetToClient);
				}catch(Exception e){
					e.printStackTrace();
				}
			}

		}

	}
}
