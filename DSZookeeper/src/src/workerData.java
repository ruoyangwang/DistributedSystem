import java.net.*;
import java.io.*;
import java.util.*;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.CreateMode;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class workerData{

	public String workername=null;
	public int from=0;
	public int to =0;
	public ObjectOutputStream toClient=null;
}
