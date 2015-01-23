import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.io.Serializable;

public class ClientEventData implements Serializable{
		String Cname;
		Point Clocation;
		Direction Cdirection;
		int Ctype;			//0 is remote client, 1 is robot
	
		public ClientEventData(String name, Point position, Direction direction, int type){
			this.Cname = name;
			this.Clocation = position;
			this.Cdirection = direction;
			this.Ctype = type;
		}

}
