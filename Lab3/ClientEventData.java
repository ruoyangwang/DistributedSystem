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
		int event;
		Socket socket =null;
		ObjectOutputStream toClient = null;
		ObjectInputStream fromClient = null;
		Client client;
		int score=0;

		public ClientEventData(String name, Point location, Direction direction, int type, int event, Socket soc, ObjectOutputStream toClient, Client client) {		//singleton class 
			this.Cname = name;
			this.Clocation = location;
			this.Cdirection = direction;
			this.Ctype = type;
			this.event = event;
			this.socket = soc;
			this.toClient = toClient;
			this.client = client;
		}

		public void Update_Event(Point location, Direction direction, int event, int score){
			this.Clocation = location;
			this.Cdirection = direction;
			this.event = event;
			if(score!=0)
				this.score=score;

		}
		
}
