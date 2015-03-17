import java.net.*;
import java.io.*;

public class BroadCastEvent extends Thread{



		public void run(){
//System.out.println("BroadCastEvent thread starts");
			while(true){
//System.out.println("what is in the eventList:  "+MazeServerHandler.eventList.size());
				//synchronized (MazeServerHandler.eventList){
				if(MazeServerHandler.eventList.peek()!=null && MazeServerHandler.eventList.peek().ACK >= MazeServer.serverCount){
						System.out.println("``````````````````  trying to acquire lock! ```````````");
						//MazeServerHandler.eventLock.lock();
						Serialized_Client_Data SCD;
						synchronized (MazeServerHandler.eventList){
						//MazeServerHandler.print_event_list();
						SCD = MazeServerHandler.eventList.poll();
						MazeServerHandler.print_event_list();
						}
						System.out.println("********************  can dequeue and broadcast this event");
						try{
							switch (SCD.event) {
								case MazePacket.CLIENT_FORWARD:
								case MazePacket.CLIENT_LEFT:
								case MazePacket.CLIENT_RIGHT:
								case MazePacket.CLIENT_BACKWARD:
									//MazeServerHandler.eventList.remove(0);
									System.out.println("The size of this eventList after remove?  ");
									MazeServerHandler.event_Move(SCD.event, SCD.Cname);
									Server_Broad_cast(SCD.Cname);
									break;
								case MazePacket.CLIENT_QUIT:
									MazeServerHandler.event_Quit(SCD.Cname, SCD.serverHostName);
									break;
								case MazePacket.CLIENT_FIRE:
									MazeServerHandler.event_Fire(SCD.Cname, SCD.serverHostName);
									Server_Broad_cast(SCD.Cname);
									break;
								case MazePacket.CLIENT_REBORN:
									System.out.println("-8-8-8-8-8 reborn client "+SCD.Cname+" "+SCD.Clocation+" "+SCD.Cdirection);
									MazeServerHandler.event_Reborn(SCD.Cname,SCD.Clocation,SCD.Cdirection);
									Server_Broad_cast(SCD.Cname);
									break;
								case MazePacket.PROJ_UPDATE:
									System.out.println("now update projectile");
									MazeServerHandler.Projectile_Update();
									Server_Broad_cast("ProjUpdate");
									break;
							}
					
						}catch(Exception e){
							e.printStackTrace();
						}
						//}
						//MazeServerHandler.eventLock.unlock();
					
				}
				}
			//}

	}

	public static void Server_Broad_cast(String clientEvent){
		System.out.println("Server_Broad_cast~~~~~~~~~~~ beginning?~~~~~~~");
		//if(clientQueue.size()>0){
		//	String clientEvent;

			try{
				//clientEvent = Cname;
				//System.out.println("what's the event type inside BORADCAST????  "+clientMap.get(clientEvent).event);
				for (String key: MazeServerHandler.clientMap.keySet()) {
					//Socket holderSocket = clientMap.get(key).socket;
					
					MazePacket packetToClient = new MazePacket();
					
					//projectile update
					if(clientEvent.equals("ProjUpdate")){
						packetToClient.Cname = null;
						packetToClient.Clocation = null;
						packetToClient.Cdirection = null;
						packetToClient.type = MazePacket.PROJ_UPDATE;
						System.out.println("=-=-=-update projectile");

					}
					//other actions
					else{
						if(clientEvent.equals(key)){
						System.out.println("Event "+clientEvent+" "+MazeServerHandler.clientMap.get(clientEvent).event);
							
							int i = 0;
							for (String key2: MazeServerHandler.clientMap.keySet()) {
								 Serialized_Client_Data S_ClientData = new Serialized_Client_Data(		//seriliazed version of above data, for passing into socket back to clients
									MazeServerHandler.clientMap.get(key2).Cname,
									MazeServerHandler.clientMap.get(key2).Clocation,

									MazeServerHandler.clientMap.get(key2).Cdirection,
									MazeServerHandler.clientMap.get(key2).Ctype,
									MazeServerHandler.clientMap.get(key2).event,
									MazeServerHandler.maze.get_score(MazeServerHandler.clientMap.get(key2).Cname)
								);
								//System.out.println("S_client data score:   "+maze.get_score(clientMap.get(key2).Cname)+"  --name:  "+clientMap.get(key2).Cname);
								packetToClient.clientData[i]= S_ClientData;
								i+=1;
							}
						}
					
						packetToClient.Cname = MazeServerHandler.clientMap.get(clientEvent).Cname;
						packetToClient.Clocation = MazeServerHandler.clientMap.get(clientEvent).client.getPoint();
						packetToClient.Cdirection = MazeServerHandler.clientMap.get(clientEvent).client.getOrientation();
						packetToClient.type = MazeServerHandler.clientMap.get(clientEvent).event;
					}
					try{
						/* send reply back to client */
						if(MazeServerHandler.clientMap.get(key).toClient!=null)
							MazeServerHandler.clientMap.get(key).toClient.writeObject(packetToClient);
						
					}catch (Exception e) {
						e.printStackTrace();
					}
				
				}
			}catch (Exception e) {
					e.printStackTrace();
			}
		//}
		System.out.println("~~~~~~~~~~~ in the end what's the size?~~~~~~~  ");
	}


}
