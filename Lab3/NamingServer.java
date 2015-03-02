import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;


public class NamingServer{
	public static int serverCount;
		public static int iterator;	
		
	public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;
		serverCount = 0;
		iterator= 0;

		
        try {
        		if(args.length == 1) {
		    		serverSocket = new ServerSocket(8000);
				
		    	} else {
		    		System.err.println("ERROR: Invalid arguments!");
		    		System.exit(-1);
		    	}
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        while (listening) {
       	 	new NamingServerHandler(serverSocket.accept()).start();
        }

        serverSocket.close();
    }
    
    public static void increment_iterator(){
    		iterator +=1;
    }

	public static void reset_iterator(){
		iterator = 0;
	}



}
