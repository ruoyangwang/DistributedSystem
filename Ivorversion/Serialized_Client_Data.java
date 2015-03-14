import java.io.*;
import java.net.*;
import java.io.Serializable;


public class Serialized_Client_Data implements Serializable,Comparable<Serialized_Client_Data>{
		String Cname;
		String serverHostName;
		Point Clocation;
		Direction Cdirection;
		int Ctype;			//0 is remote client, 1 is robot
		int event;
		int score;
		double Lamport = 0;
		int ACK =0;
		int pid;
		
		public Serialized_Client_Data(String name, Point location, Direction direction, int type, int event, int score){
			this.Cname = name;
			this.Clocation = location;
			this.Cdirection = direction;
			this.Ctype = type;
			this.event = event;
			this.score= score;
		}
		
		public Serialized_Client_Data(){


		}

		@Override
		public int compareTo(Serialized_Client_Data o) {
			String[] str1=Double.toString(this.Lamport).split(".");
			String[] str2=Double.toString(o.Lamport).split(".");
			int l1=Integer.parseInt(str1[0]+str1[1]);
			int l2=Integer.parseInt(str2[0]+str2[1]);
   			 return l1-l2;
		}
}
