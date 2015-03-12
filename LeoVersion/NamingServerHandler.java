
import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NamingServerHandler extends Thread{
	private Socket socket = null;
	static HashMap<String, Integer> peerServerMap = new HashMap<String, Integer>();
	static ObjectInputStream in= null;
	static ObjectOutputStream out = null;
	
	
	public NamingServerHandler(Socket socket){
		super("NamingServerHandler");
		this.socket = socket;
		System.out.println("----------Created new Thread to handle server join----------");
		try{
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void run(){
		try{
			NamingServerPacket packetFromServer = null;
			while (( packetFromServer = (NamingServerPacket) in.readObject()) != null) {
				if(packetFromServer.type == NamingServerPacket.SERVER_REGISTER)
				{
					System.out.println("peer server names?:   " +peerServerMap);
					NamingServerPacket NP = new NamingServerPacket();
					//peerServerMap.remove(packetFromServer.hostname);
					int i = 0;
					for(String key: peerServerMap.keySet()){
						System.out.println("retrieve peer server names:----" +key +"  portNum: "+ peerServerMap.get(key));
						peerServerObject PO = new peerServerObject(key, peerServerMap.get(key));
						NP.peerServer[i] = PO;
						System.out.println("PO content? :  " +NP.peerServer[i].HostName);
						i+=1;
					}
					NP.type =NamingServerPacket.SERVER_REGISTER;
					//System.out.println("before write out:   " +NP.peerServer[0].HostName);
					out.writeObject(NP);
					peerServerMap.put(packetFromServer.hostname, packetFromServer.portNum);
					NamingServer.serverCount+=1;
					System.out.println("before exit, check peerServerMap:   " +peerServerMap+ "  NamingServer.serverCount  " +NamingServer.serverCount );
					
					break;

				}
				
				else if(packetFromServer.type == NamingServerPacket.CLIENT_REGISTER){			
					System.out.println("a client coming in to join   "+NamingServer.serverCount +"   iterator?: "+NamingServer.iterator);
					System.out.println("check peeerServerMap  " +peerServerMap.keySet());
					List<String> keysAsArray = new ArrayList<String>(peerServerMap.keySet());
					Random r = new Random();
					NamingServerPacket NP = new NamingServerPacket();
					NP.type =NamingServerPacket.CLIENT_REGISTER;
					
					String hn = keysAsArray.get(NamingServer.iterator);
					System.out.println("check keysAsArray:   "+keysAsArray.get(NamingServer.iterator));
					NamingServer.increment_iterator();
					System.out.println("iterator is:  "+NamingServer.iterator +"  and serverCount is :  "+NamingServer.serverCount);
					if(NamingServer.iterator==NamingServer.serverCount)
						NamingServer.reset_iterator();
					System.out.println("randomly chosen hostname:   " +hn + "iterator?  " +NamingServer.iterator);
					NP.hostname = hn;
					NP.portNum = peerServerMap.get(hn);
					out.writeObject(NP);
					break;
				}

			}



		}catch (Exception e) {
				e.printStackTrace();
		}

	}

}
