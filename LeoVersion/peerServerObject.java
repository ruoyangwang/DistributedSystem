import java.io.*;
import java.net.*;
import java.io.Serializable;

public class peerServerObject implements Serializable{
		String HostName;
		int portNum;

		public peerServerObject(String hostname, int portnum){
			this.HostName=hostname;
			this.portNum = portnum;

		}
}
