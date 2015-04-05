all: JobTracker client Worker scheduler

JobTracker:
	javac -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar ZkConnector.java JobTracker.java JTHandler.java zkPacket.java
	
client:
	javac -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar ZkConnector.java ClientDriver.java
	
Worker:

FileServer:

clean:
	rm -f *.class *.*~
