import java.net.*;
import java.io.*;

public class BroadCastEvent extends Thread{



		public void run(){
//System.out.println("BroadCastEvent thread starts");
			while(true){
				/*if(MazeServerHandler.eventList.size()>=2){
							for(Serialized_Client_Data SCD: MazeServerHandler.eventList){
								SCD.ACK =MazeServer.serverCount;
							}
				}*/
//System.out.println("what is in the eventList:  "+MazeServerHandler.eventList.size());
				while(MazeServerHandler.eventList.peek()!=null && MazeServerHandler.eventList.peek().ACK == MazeServer.serverCount){
						//if(MazeServerHandler.eventList.size()>=1)
							//MazeServerHandler.eventList.peek().ACK =MazeServer.serverCount;
						//MazeServerHandler.eventLock.lock();
						Serialized_Client_Data SCD;
						synchronized(MazeServerHandler.eventList){
							SCD = MazeServerHandler.eventList.poll();
						}
						
						System.out.println("********************  can dequeue and broadcast this event   && my Lamport  "+MazeServer.LamportClock);
						try{
							switch (SCD.event) {
								case MazePacket.CLIENT_FORWARD:
								case MazePacket.CLIENT_LEFT:
								case MazePacket.CLIENT_RIGHT:
								case MazePacket.CLIENT_BACKWARD:
									//MazeServerHandler.eventList.remove(0);
									MazeServer.LamportClock = SCD.Lamport;
									MazeServerHandler.event_Move(SCD.event, SCD.Cname);
									break;
								case MazePacket.CLIENT_QUIT:
									MazeServer.LamportClock = SCD.Lamport;
									MazeServerHandler.event_Quit(SCD.Cname, SCD.serverHostName);
									break;
								case MazePacket.CLIENT_FIRE:
									MazeServer.LamportClock = SCD.Lamport;
									MazeServerHandler.event_Fire(SCD.Cname, SCD.serverHostName);
									break;
								case MazePacket.CLIENT_REBORN:
									MazeServer.LamportClock = SCD.Lamport;
									System.out.println("-8-8-8-8-8 reborn client "+SCD.Cname+" "+SCD.Clocation+" "+SCD.Cdirection);
									MazeServerHandler.event_Reborn(SCD.Cname,SCD.Clocation,SCD.Cdirection);
									break;
								case MazePacket.PROJ_UPDATE:
									MazeServer.LamportClock = SCD.Lamport;
									System.out.println("now update projectile");
									MazeServerHandler.Projectile_Update();
									break;
							}
					
						}catch(Exception e){
							e.printStackTrace();
						}
					
						//MazeServerHandler.eventLock.unlock();
					
				}

			}

	}


}
