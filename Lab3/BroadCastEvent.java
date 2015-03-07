import java.net.*;
import java.io.*;

public class BroadCastEvent extends Thread{



		public void run(){
//System.out.println("BroadCastEvent thread starts");
			while(true){
//System.out.println("what is in the eventList:  "+MazeServerHandler.eventList.size());
				while(MazeServerHandler.eventList.peek()!=null && MazeServerHandler.eventList.peek().ACK == MazeServer.serverCount){
						//MazeServerHandler.eventLock.lock();
						Serialized_Client_Data SCD = MazeServerHandler.eventList.poll();
						System.out.println("********************  can dequeue and broadcast this event");
						try{
							switch (SCD.event) {
								case MazePacket.CLIENT_FORWARD:
								case MazePacket.CLIENT_LEFT:
								case MazePacket.CLIENT_RIGHT:
								case MazePacket.CLIENT_BACKWARD:
									//MazeServerHandler.eventList.remove(0);
									System.out.println("The size of this eventList after remove?  "+MazeServerHandler.eventList.size());
									MazeServerHandler.event_Move(SCD.event, SCD.Cname);
									break;
							}
					
						}catch(Exception e){
							e.printStackTrace();
						}
					
				}

			}

	}


}
