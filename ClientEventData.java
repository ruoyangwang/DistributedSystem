import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class ClientEventData {
		String Cname;
		Point Clocation;
		Direction Cdirection;
		int Ctype;			//0 is remote client, 1 is robot
		int event;
		Socket socket =null;

		public ClientEventData(String name, Point location, Direction direction, int type, int event, Socket soc) {		//singleton class 
			this.Cname = name;
			this.Clocation = location;
			this.Cdirection = direction;
			this.Ctype = type;
			this.event = event;
			this.socket = soc;
		}
		
}
