import java.net.*;
import java.io.*;

public class BroadCastEvent extends Thread{



		public void run(){
//System.out.println("BroadCastEvent thread starts");
			while(true){
//System.out.println("what is in the eventList:  "+MazeServerHandler.eventList.size());
				while(MazeServerHandler.eventList.peek()!=null && MazeServerHandler.eventList.peek().ACK == MazeServer.serverCount){
						//System.out.println("``````````````````  trying to acquire lock! ```````````");
						MazeServerHandler.eventLock.lock();
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
								case MazePacket.CLIENT_QUIT:
									MazeServerHandler.event_Quit(SCD.Cname, SCD.serverHostName);
									break;
								case MazePacket.CLIENT_FIRE:
									MazeServerHandler.event_Fire(SCD.Cname);
									break;
								case MazePacket.CLIENT_REBORN:
									System.out.println("-8-8-8-8-8 reborn client "+SCD.Cname+" "+SCD.Clocation+" "+SCD.Cdirection);
									MazeServerHandler.event_Reborn(SCD.Cname,SCD.Clocation,SCD.Cdirection);
									break;
                                case MazePacket.PROJ_UPDATE:
                                    MazeServerHandler.event_ProjUpdate(SCD.Cname);
                                    break;
							}
					
						}catch(Exception e){
							e.printStackTrace();
						}
						MazeServerHandler.eventLock.unlock();
					
				}

			}

	}


}
