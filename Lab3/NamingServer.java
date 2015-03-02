import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;


public class NamingServer{
	public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;
			

		
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




}
