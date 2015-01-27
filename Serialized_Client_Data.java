import java.io.*;
import java.net.*;
import java.io.Serializable;


public class Serialized_Client_Data implements Serializable{
		String Cname;
		Point Clocation;
		Direction Cdirection;
		int Ctype;			//0 is remote client, 1 is robot
		int event;

		public Serialized_Client_Data(String name, Point location, Direction direction, int type, int event){
			this.Cname = name;
			this.Clocation = location;
			this.Cdirection = direction;
			this.Ctype = type;
			this.event = event;
		}
}
