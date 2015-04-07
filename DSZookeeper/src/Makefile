all: JobTracker client Worker FileServer

JobTracker:
	javac -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar ZkConnector.java JobTracker.java JTHandler.java zkPacket.java
	
client:
	javac -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar ZkConnector.java ClientDriver.java zkPacket.java
	
Worker:
	javac -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar ZkConnector.java Worker.java WorkerHandler.java zkPacket.java

FileServer:
	javac -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar ZkConnector.java FSHandler.java FileServer.java zkPacket.java workerData.java

clean:
	rm -f *.class *.*~
