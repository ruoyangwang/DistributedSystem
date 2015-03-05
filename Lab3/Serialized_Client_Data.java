import java.io.*;
import java.net.*;
import java.io.Serializable;


public class Serialized_Client_Data implements Serializable,Comparable<Serialized_Client_Data>{
		String Cname;
		Point Clocation;
		Direction Cdirection;
		int Ctype;			//0 is remote client, 1 is robot
		int event;
		int score;
		int Lamport = 0;
		int ACK =0;

		public Serialized_Client_Data(String name, Point location, Direction direction, int type, int event, int score){
			this.Cname = name;
			this.Clocation = location;
			this.Cdirection = direction;
			this.Ctype = type;
			this.event = event;
			this.score= score;
		}
		
		@Override
		public int compareTo(Serialized_Client_Data o) {
   			 return this.Lamport-o.Lamport;
		}
}
