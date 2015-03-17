

import java.io.Serializable;
import java.util.HashMap;

public class NamingServerPacket implements Serializable {

	public static final int SERVER_NULL = -1;
	public static final int SERVER_REGISTER   = 1;
	public static final int SERVER_QUIT		= 0;

	public static final int CLIENT_REGISTER = 10;
	

	public peerServerObject[] peerServer = new peerServerObject[10];

	public int type = SERVER_NULL;
	public String hostname;
	public int portNum;

}

