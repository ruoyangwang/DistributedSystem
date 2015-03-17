

import java.io.Serializable;


public class MazePacket implements Serializable {
	/*Maze code representation*/
	public static final int MAZE_NULL    = 0;
	public static final int MAZE_CONNECT = 101;
	public static final int MAZE_QUOTE   = 102;
	public static final int MAZE_ERROR   = 103;
	public static final int MAZE_PRINT = 104;
	public static final int MAZE_BYE     = 199;
	
	/*Client movement representations*/
	public static final int CLIENT_REGISTER   = 200;
	public static final int CLIENT_FORWARD   = 201;
	public static final int CLIENT_LEFT = 202;
	public static final int CLIENT_RIGHT = 203;
	public static final int CLIENT_BACKWARD  = 204;
	public static final int CLIENT_FIRE  = 205;
	public static final int CLIENT_QUIT  = 206;

	public static final int CLIENT_REBORN= 207;

	public static final int CLIENT_UPDATE  = 208;

	public static final int PROJ_UPDATE    =209;
	
	public static final int CLIENT_REGISTER_ERROR   = 300;
	public static final int CLIENT_QUIT_ERROR   = 301;

	public static final int SERVER_REGISTER = 500;
	
	public static final int ACK = 800;
	public static final int FINAL_ACK = 801;
	public static final int FINAL_EXIT = 802;
	public int type = MazePacket.MAZE_NULL;
	public String symbol;
	 
    public String Cname;
    public Point Clocation;
    public int Ctype;		//0 is remote, 1 is robot
    public Direction Cdirection;
	public int score;
	public Serialized_Client_Data[] clientData = new Serialized_Client_Data[10];
	public Serialized_Client_Data ServerData =null;

	public String ServerHostName; 
	//Client newclient;
	public String         buffer;
	public int            num_locations;
}



