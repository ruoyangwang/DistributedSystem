import java.io.Serializable;



public class zkPacket implements Serializable{

	public static final int ZK_NULL = 0;
	//message from client 
	public static final int CLIENT_LOOKUP = 100;
	public static final int CLIENT_REQUEST = 101;
	public static final int CLIENT_STATUS = 102;
	public static final int CLIENT_QUIT = 110;
	public static final int CLIENT_NAK = 199;
	
	//message from JobTracker
	public static final int JT_SUBMITTED = 200;
	public static final int JT_QUIT = 210;
	
	//job status
	public static final int JOB_PROGRESS = 300;
	public static final int JOB_DONE = 301;
	public static final int JOB_FAIL = 302;
	public static final int JOB_NOT_FOUND = 303;

	public static final int WORKER_IN_PROGRESS = 400;
	public static final int WORKER_NOT_CONNECTED = 401;
	public static final int WORKER_DONE = 402;

	//message between workers and fileserver
	public static final int WORKER_REGISTER	= 501;
	public static final int WORKER_REQUEST	= 502;
	public static final int FSJOB_ASSIGN	= 503;
	
	public int type = zkPacket.ZK_NULL;
	public int status = zkPacket.JOB_NOT_FOUND;
	public String password = null;
	public String hash;
	public String symbol;
	//fs <-->worker
	public String[] dictionary;
	public String worker_name;
	public String jobid;
	//public int from;
	//public int to;

	}
