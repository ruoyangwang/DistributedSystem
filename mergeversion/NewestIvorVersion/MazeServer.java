import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;


public class MazeServer {
	
        static boolean listening = true;
		static String myHostName;
		//boolean firstTime = true;
		static HashMap<String, Socket> peerServerMap = new HashMap<String, Socket>();
		static int serverCount=0;
		static double LamportClock = 0;
		static int pid;
		
	public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
		Socket Nserver = null;

		
        try {
				if(args.length == 2) {
					
					/*socket as client to connect to NamingServer*/
					Nserver = new Socket(args[0],8000);
					ObjectOutputStream out = new ObjectOutputStream(Nserver.getOutputStream());				
					ObjectInputStream in = new ObjectInputStream(Nserver.getInputStream());

					
					NamingServerPacket NP = new NamingServerPacket();

					NP.type = NamingServerPacket.SERVER_REGISTER;
					pid = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
					NP.hostname = InetAddress.getLocalHost().getHostName()+"#"+pid;
					NP.portNum = Integer.parseInt(args[1]);
					myHostName = NP.hostname;
				
					out.writeObject(NP);
					NamingServerPacket packetFromServer = null;
					try{
							System.out.println("waiting for naming server to respond @_@");
							while (( packetFromServer = (NamingServerPacket) in.readObject()) != null) {
								System.out.println("got response from Naming server!");
								if(packetFromServer.type==NamingServerPacket.SERVER_REGISTER) {

										System.out.println("what's the key set?  "+packetFromServer.peerServer.length);

										for (peerServerObject PO: packetFromServer.peerServer){
											if(PO!=null){
												System.out.println("Inside for loop to get peers server name and PORT NUM:  "+PO.portNum);

												System.out.println(PO.HostName.split("#")[0]);
												//self connect to other servers as clients	
												String peerHostName = PO.HostName;
												int port = PO.portNum;
												Socket s = new Socket(peerHostName.split("#")[0],port);
												MazeServerHandler MST = new MazeServerHandler(s);
												MST.Server_Register(myHostName,s);		//
												MST.start();
												peerServerMap.put(peerHostName,s);
												serverCount+=1;
											}
									
										}
										break;
								}


							}
						/*after finishing the lookup, disconnect from Naming Server*/
						out.close();
						in.close();
						Nserver.close();

						
					}catch(Exception e){
						e.printStackTrace();
					}
	
					System.out.println("creating serverSocket for clients now, check serverCount:  "+serverCount);
					/*socket for other client connection*/
					serverSocket = new ServerSocket(Integer.parseInt(args[1]));
				} else {
					System.err.println("ERROR: Invalid arguments!");
					System.exit(-1);
				}
				
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        while (listening) {
        		new MazeServerHandler(serverSocket.accept()).start();
        }

        serverSocket.close();
    }
}

