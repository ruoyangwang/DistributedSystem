
/*
Copyright (C) 2004 Geoffrey Alan Washburn
   
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
   
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
   
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/

import java.lang.Thread;
import java.lang.Runnable;
import java.io.Serializable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;  
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A concrete implementation of a {@link Maze}.  
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: MazeImpl.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class MazeImpl extends Maze implements Serializable, ClientListener, Runnable {
	

	private static ConcurrentHashMap<String, Client> DeadClQueue = new ConcurrentHashMap<String, Client>();
	//private Client rebornClient=null;


        /**
         * Create a {@link Maze}.
         * @param point Treat the {@link Point} as a magintude specifying the
         * size of the maze.
         * @param seed Initial seed for the random number generator.
         */
        public MazeImpl(Point point, long seed) {
                maxX = point.getX();
                assert(maxX > 0);
                maxY = point.getY();
                assert(maxY > 0);
                
                // Initialize the maze matrix of cells
                mazeVector = new Vector(maxX);
                for(int i = 0; i < maxX; i++) {
                        Vector colVector = new Vector(maxY);
                        
                        for(int j = 0; j < maxY; j++) {
                                colVector.insertElementAt(new CellImpl(), j);
                        }
                        
                        mazeVector.insertElementAt(colVector, i);
                }

                thread = new Thread(this);

                // Initialized the random number generator
                randomGen = new Random(seed);
                
                // Build the maze starting at the corner
                buildMaze(new Point(0,0));

                thread.start();
        }
       
        /** 
         * Create a maze from a serialized {@link MazeImpl} object written to a file.
         * @param mazefile The filename to load the serialized object from.
         * @return A reconstituted {@link MazeImpl}. 
         */
        public static Maze readMazeFile(String mazefile)
                throws IOException, ClassNotFoundException {
                        assert(mazefile != null);
                        FileInputStream in = new FileInputStream(mazefile);
                        ObjectInputStream s = new ObjectInputStream(in);
                        Maze maze = (Maze) s.readObject();
                        
                        return maze;
                }
        
        /** 
         * Serialize this {@link MazeImpl} to a file.
         * @param mazefile The filename to write the serialized object to.
         * */
        public void save(String mazefile)
                throws IOException {
                        assert(mazefile != null);
                        FileOutputStream out = new FileOutputStream(mazefile);
                        ObjectOutputStream s = new ObjectOutputStream(out);
                        s.writeObject(this);
                        s.flush();
                }
       
        /** 
         * Display an ASCII version of the maze to stdout for debugging purposes.  
         */
        public void print() {
                for(int i = 0; i < maxY; i++) {
                        for(int j = 0; j < maxX; j++) {
                                CellImpl cell = getCellImpl(new Point(j,i));
                                if(j == maxY - 1) {
                                        if(cell.isWall(Direction.South)) {
                                                System.out.print("+-+");
                                        } else {
                                                System.out.print("+ +");
                                        }
                                } else {
                                        if(cell.isWall(Direction.South)) {
                                                System.out.print("+-");
                                        } else {
                                                System.out.print("+ ");
                                        }
                                }
                                
                        }	    
                        System.out.print("\n");
                        for(int j = 0; j < maxX; j++) {
                                CellImpl cell = getCellImpl(new Point(j,i));
                                if(cell.getContents() != null) {
                                        if(cell.isWall(Direction.West)) {
                                                System.out.print("|*");
                                        } else {
                                                System.out.print(" *");
                                        }
                                } else {
                                        if(cell.isWall(Direction.West)) {
                                                System.out.print("| ");
                                        } else {
                                                System.out.print("  ");
                                        }
                                }
                                if(j == maxY - 1) {
                                        if(cell.isWall(Direction.East)) {
                                                System.out.print("|");
                                        } else {
                                                System.out.print(" ");
                                        }
                                }
                        }
                        System.out.print("\n");
                        if(i == maxX - 1) {
                                for(int j = 0; j < maxX; j++) {
                                        CellImpl cell = getCellImpl(new Point(j,i));
                                        if(j == maxY - 1) {
                                                if(cell.isWall(Direction.North)) {
                                                        System.out.print("+-+");
                                                } else {
                                                        System.out.print("+ +");
                                                }
                                        } else {
                                                if(cell.isWall(Direction.North)) {
                                                        System.out.print("+-");
                                                } else {
                                                        System.out.print("+ ");
                                                }
                                        }		
                                }
                                System.out.print("\n");     
                        }   
                }
                
        }
       
        
        public boolean checkBounds(Point point) {
                assert(point != null);
                return (point.getX() >= 0) && (point.getY() >= 0) && 
                        (point.getX() < maxX) && (point.getY() < maxY);
        }
        
        public Point getSize() {
                return new Point(maxX, maxY);
        }
        
        public synchronized Cell getCell(Point point) {
                assert(point != null);
                return getCellImpl(point);
        }
        
        public synchronized void addClient(Client client, Point point, Direction direction) {
                assert(client != null);
				if(point==null && direction ==null){
					//System.out.println("No point and direction are given for client: ----"+client);
		            // Pick a random starting point, and check to see if it is already occupied
		            Point p = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
		            CellImpl cell = getCellImpl(p);
		            // Repeat until we find an empty cell
		            while(cell.getContents() != null) {
		                    p = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
		                    cell = getCellImpl(p);
		            }
					add_Client(client, p, null,0);
				} 
				else{
					//System.out.println("create complete new client:------- "+client +"--"+point+"--"+direction);
					add_Client(client, point, direction, 0);
				}

        }
        

		public synchronized void addRemoteClient(Client client, Point point, Direction direction, int score) {
        		add_Client(client, point, direction, score);
    		}
		
        public synchronized Point getClientPoint(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof Point);
                return (Point)o;
        }
        
        public synchronized Direction getClientOrientation(Client client) {
        //System.out.println("????which one??????????????????    "+client+"   "+client.getName());
                assert(client != null);
                Object o = clientMap.get(client);
         // System.out.println(o);
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
          //System.out.println(dp);
                return dp.getDirection();
        }
       
        public synchronized void removeClient(Client client) {
                assert(client != null);
                Object o = clientMap.remove(client);
                assert(o instanceof Point);
                Point point = (Point)o;
                CellImpl cell = getCellImpl(point);
                cell.setContents(null);
                clientMap.remove(client);
                client.unregisterMaze();
                client.removeClientListener(this);
                update();
                notifyClientRemove(client);
        }

        public boolean clientFire(Client client) {
        		System.out.println("client fire!!!!!");
                assert(client != null);
                // If the client already has a projectile in play
                // fail.
                if(clientFired.contains(client)) {
                        return false;
                }
                
                Point point = getClientPoint(client);
                Direction d = getClientOrientation(client);
                CellImpl cell = getCellImpl(point);
                
                /* Check that you can fire in that direction */
                if(cell.isWall(d)) {
                        return false;
                }
                
                DirectedPoint newPoint = new DirectedPoint(point.move(d), d);
                /* Is the point withint the bounds of maze? */
                assert(checkBounds(newPoint));
                
                CellImpl newCell = getCellImpl(newPoint);
                Object contents = newCell.getContents();
                if(contents != null) {
                        // If it is a Client, kill it outright
                        if(contents instanceof Client) {
                                notifyClientFired(client);
                                System.out.println(client.getName());
                                killClient(client, (Client)contents);
                                update();
                                return true; 
                        } else {
                        // Otherwise fail (bullets will destroy each other)
                                return false;
                        }
                }
                
                clientFired.add(client);
                Projectile prj = new Projectile(client);
                
                /* Write the new cell */

                	projectileMap.put(prj, newPoint);

				
                newCell.setContents(prj);
                notifyClientFired(client);
                update();
				//try{
				//Thread.sleep(10000);}catch(Exception e){}
                return true; 
        }
        
        public boolean moveClientForward(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                return moveClient(client, dp.getDirection());
        }
        
        public boolean moveClientBackward(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                return moveClient(client, dp.getDirection().invert());
        }
        
       
        public synchronized Iterator getClients() {
                return clientMap.keySet().iterator();
        }
        
        
        public void addMazeListener(MazeListener ml) {
				System.out.println("add mazelistener");
                listenerSet.add(ml);
        }

        public void removeMazeListener(MazeListener ml) {
                listenerSet.remove(ml);
        }

        /**
         * Listen for notifications about action performed by 
         * {@link Client}s in the maze.
         * @param c The {@link Client} that acted.
         * @param ce The action the {@link Client} performed.
         */
        public void clientUpdate(Client c, ClientEvent ce) {
                // When a client turns, update our state.
                if(ce == ClientEvent.turnLeft) {
                        rotateClientLeft(c);
                } else if(ce == ClientEvent.turnRight) {
                        rotateClientRight(c);
                }
        }

        /**
         * Control loop for {@link Projectile}s.
         */
		//private boolean bossing = false;
		
		public synchronized boolean is_boss(){
			boolean exist = false;
			boolean isboss = true;
			
		//	System.out.print("\n\n\n\n\n\nwhat's the owner now?: try to be boss\n\n");
			synchronized(projectileMap) {
				Iterator it = projectileMap.keySet().iterator();
				if(projectileMap.size()==0)return false;
				while(it.hasNext()){
					Object o = it.next();
					Projectile prj = (Projectile)o;
					//System.out.println("what's the owner now?:  "+prj.getOwner().getName()+"   "+prj.getOwner().serverHostName);
					if(prj.getOwner().serverHostName!=null&&MazeServer.myHostName.equals(prj.getOwner().serverHostName))
					{
						//System.out.println("^^^^^^^^^ current Prj Owner:   "+prj.getOwner().getName()+"   "+projectileMap.size());
						exist = true;
					}

					if(MazeServer.pid>prj.getOwner().pid)
					{
						System.out.println("smallest pid:  "+	MazeServer.myHostName + "   my pid:  "+MazeServer.pid+ "  prjPID   "+ prj.getOwner().pid);
						isboss = false;
					}
				
				}
			}

			if(exist && isboss ){	
				System.out.print("\n\nI am the boss:  "+MazeServer.myHostName+" "+projectileMap.size()+"\n\n\n\n");
				return true;	
			}
			//System.out.print("\n\nI am not the boss\n");
			return false;


		}


		


        public void run() {
                
                while(true) {
			//System.out.println("-===--=-==--=-=-=-=-=event size "+MazeServerHandler.eventList.size());
                        if(ServerPointer!=null){
				if(is_boss()){
				synchronized (projectileMap){
				//if(ServerPointer.boss&&projectileMap.size()!=0) {
								System.out.println("I am the boss! look at my pid:  "+MazeServer.pid+" "+ MazeServer.myHostName);
								if(!ServerPointer.check_proj_event_list()){
			        				Serialized_Client_Data scd = new Serialized_Client_Data();
								scd.event = MazePacket.PROJ_UPDATE;
								scd.serverHostName = MazeServer.myHostName;
								scd.Cname = "ProjUpdate";
								ServerPointer.increment_assign_LamportClock(scd);
								try{
									ServerPointer.add_One_Event(scd);
									ServerPointer.sendQueue.put(scd);
								}catch(Exception e){
									e.printStackTrace();

								}
								}
                                					    
                        }
			}
			}
                        try {
                                thread.sleep(200);
                        } catch(Exception e) {
                                // shouldn't happen
                        }
                }
        }

	public void move_Proj(){
			System.out.println("Inside move_Proj(), Now i can update all projectiles!  --- "+MazeServerHandler.clientQueue.peek());
			Collection deadPrj = new HashSet();
			
			if(!projectileMap.isEmpty()){
				Iterator it = projectileMap.keySet().iterator();
			            synchronized(projectileMap) {
			                    while(it.hasNext()) {   
			                            Object o = it.next();
			                            assert(o instanceof Projectile);
			                            deadPrj.addAll(moveProjectile((Projectile)o));
			                    }               
			                    it = deadPrj.iterator();
			                    while(it.hasNext()) {
			                            Object o = it.next();
			                            assert(o instanceof Projectile);
			                            Projectile prj = (Projectile)o;
			                            projectileMap.remove(prj);
			                            clientFired.remove(prj.getOwner());
			                    }
			                    deadPrj.clear();
			            }
			}
			System.out.println("OUT move_Proj()!  --- "+MazeServerHandler.clientQueue.peek());

	}

	
	/*client side, execute after client receive a missile time out update (200ms)*/
	public void projectileCheck(){
		System.out.println("=-=-=-client update projectile");
		Collection deadPrj = new HashSet();
		if(!projectileMap.isEmpty()) {
			        
                                Iterator it = projectileMap.keySet().iterator();
                                synchronized(projectileMap) {
                                        while(it.hasNext()) {   
                                                Object o = it.next();
                                                assert(o instanceof Projectile);
                                                deadPrj.addAll(moveProjectile((Projectile)o));
                                        }               
                                        it = deadPrj.iterator();
                                        while(it.hasNext()) {
                                                Object o = it.next();
                                                assert(o instanceof Projectile);
                                                Projectile prj = (Projectile)o;
                                                projectileMap.remove(prj);
                                                clientFired.remove(prj.getOwner());
                                        }
                                        deadPrj.clear();
                                }
                        }	
	
	}
        
        /* Internals */
        
        private synchronized Collection moveProjectile(Projectile prj) {
                Collection deadPrj = new LinkedList();
                assert(prj != null);
                
                Object o = projectileMap.get(prj);
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                Direction d = dp.getDirection();
                CellImpl cell = getCellImpl(dp);
                
                /* Check for a wall */
                if(cell.isWall(d)) {
                        // If there is a wall, the projectile goes away.
                        cell.setContents(null);
                        deadPrj.add(prj);
                        update();
                        return deadPrj;
                }
                
                DirectedPoint newPoint = new DirectedPoint(dp.move(d), d);
                /* Is the point within the bounds of maze? */
                assert(checkBounds(newPoint));
                
                CellImpl newCell = getCellImpl(newPoint);
                Object contents = newCell.getContents();
                if(contents != null) {
                        // If it is a Client, kill it outright
                        if(contents instanceof Client) {
                                killClient(prj.getOwner(), (Client)contents);
                                cell.setContents(null);
                                deadPrj.add(prj);
                                update();
                                return deadPrj;
                        } else {
                        // Bullets destroy each other
                                assert(contents instanceof Projectile);
                                newCell.setContents(null);
                                cell.setContents(null);
                                deadPrj.add(prj);
                                deadPrj.add(contents);
                                update();
                                return deadPrj;
                        }
                }

                /* Clear the old cell */
                cell.setContents(null);
                /* Write the new cell */
                projectileMap.put(prj, newPoint);
                newCell.setContents(prj);
                update();
                return deadPrj;
        }
        /**
         * Internal helper for adding a {@link Client} to the {@link Maze}.
         * @param client The {@link Client} to be added.
         * @param point The location the {@link Client} should be added.
         */
        private synchronized void add_Client(Client client, Point point, Direction direction,int score) {
        		
				//System.out.println("inside add_client: "+client+" ----  "+score);
                assert(client != null);
                assert(checkBounds(point));
                CellImpl cell = getCellImpl(point);
                Direction d = direction;
               
				if (d == null) {
					System.out.println("generate random direction");
					d = Direction.random();
		            while(cell.isWall(d)) {
		              d = Direction.random();
		            }
				}
				
				else{
					 if(direction.equals("NORTH")){
		            	d =  Direction.target(0);
				     }
		            else if(direction.equals("WEST")){
		            	d = Direction.target(3);
		            }
		            else if(direction.equals("EAST")){
		            	d = Direction.target(1);
		            }
		            else if(direction.equals("SOUTH")){
		            	d = Direction.target(2);
                	}
				
				}
                cell.setContents(client);
                System.out.println("check direction before create: "+d);
                clientMap.put(client, new DirectedPoint(point, d));
                client.registerMaze(this);
                client.addClientListener(this);
                update();
                notifyClientAdd(client, score);
        }
        
	/*
	 * This function is used by server to calculate 
	 * the reborn position then broadcast the 
	 * result to all client 
	 * need to be synchronized, since only one
	 * thread can do this at any time
	 * --Recommand to lock the maze but not implemented yet
	 */
	public void reborn_Position(Client DeadClient){
		assert(DeadClient!=null);

		/*
		Delete the dead client from the clientMap and the maze
		*/
		Object o = clientMap.remove(DeadClient);
                assert(o instanceof Point);
                Point point = (Point)o;
                CellImpl cell = getCellImpl(point);
                cell.setContents(null);

		System.out.println("Calculating the reborn position for: "+DeadClient.getName());
		Direction direction = Direction.random();
				
		// Pick a random starting point, and check to see if it is already occupied
                point = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
                cell = getCellImpl(point);
               	// Repeat until we find an empty cell
                while(cell.getContents() != null) {
                       	point = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
                       	cell = getCellImpl(point);
                }
		
	 	cell = getCellImpl(point);
                while(cell.isWall(direction)) {
                       	direction = Direction.random();
                }

		cell.setContents(DeadClient);
		DirectedPoint temp = new DirectedPoint(point, direction);
                clientMap.put(DeadClient, temp);
		update();

	}

	/*
	 * Client side reborn function
	 * used by client handler
	 */
	public void reborn_Client(Point p, Direction direction, Client source, Client rebornClient) {
        	
		//assert(DeadClQueue.get(DeadClName)!=null);
		//if(DeadClQueue.get(DeadClName)==null)
		//System.exit(0);
		//Client rebornClient= (Client)DeadClQueue.get(DeadClName);
		//if(rebornClient.getName()==null){
		//	rebornClient.updateName(DeadClName);
		//}
		//this.removeClient(rebornClient);

		
		Object o = clientMap.remove(rebornClient);
                assert(o instanceof Point);
                Point point = (Point)o;
                CellImpl cell = getCellImpl(point);
                cell.setContents(null);
                update();
		        //notifyClientKilled(source, rebornClient);    
		            
		System.out.println("reborn_client: "+rebornClient.getName());
		cell = getCellImpl(p);
        	cell.setContents(rebornClient);
        
        
         	clientMap.put(rebornClient, new DirectedPoint(p, direction));
		//DeadClQueue.remove(DeadClName);
            	update();
            	//
        }
	
	/*
	 * Other server side reborn function
	 * used by server handler
	 */
	public void server_reborn_Client(Point p, Direction direction, String source, String target) {
        	
		//assert(DeadClQueue.get(DeadClName)!=null);
		//if(DeadClQueue.get(DeadClName)==null)
		//System.exit(0);
		//Client rebornClient= (Client)DeadClQueue.get(DeadClName);
		//if(rebornClient.getName()==null){
		//	rebornClient.updateName(DeadClName);
		//}
		//this.removeClient(rebornClient);
		Client _source=null;
		Client rebornClient=null;
		for(Object oo: clientMap.keySet()){
			assert(oo instanceof Client);
			Client tmp = (Client)oo;
			System.out.print("\n\nserver finding target and source\n\n\n ");
			if(tmp.getName().equals(source))
			_source = tmp;
			if(tmp.getName().equals(target))
			rebornClient = tmp;
			if(_source!=null&&rebornClient!=null)
			break;
		}
		assert(_source!=null&&rebornClient!=null);
		if(_source==null||rebornClient==null)
		System.out.println("server maze: cant get source or target");
		Object o = clientMap.remove(rebornClient);
                assert(o instanceof Point);
                Point point = (Point)o;
                CellImpl cell = getCellImpl(point);
                cell.setContents(null);
                update();
		        //notifyClientKilled(source, rebornClient);             
		System.out.println("reborn_client: "+rebornClient.getName()+"   also check the ClientQueue now:  "+MazeServerHandler.clientQueue.size()+"  "+MazeServerHandler.clientQueue.peek());
		cell = getCellImpl(p);
        	cell.setContents(rebornClient);
        
        
         	clientMap.put(rebornClient, new DirectedPoint(p, direction));
		//DeadClQueue.remove(DeadClName);
            	update();
            	//
        }

	public int get_score(String name){
		Iterator i = listenerSet.iterator();
//System.out.println("inside get_score to get:   "+name);
        while (i.hasNext()) {
                        Object o = i.next();
                        /*assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;*/
						//System.out.println("object inside get:-------"+o);
						if(o instanceof ScoreTableModel){
							MazeListener ml = (MazeListener)o;
							//System.out.println("ready to get score from scoretable!!=======  "+ml.get_score(name));
							return ml.get_score(name);

						}	
         }
		return -1; 
	}
        /**
         * Internal helper for handling the death of a {@link Client}.
         * @param source The {@link Client} that fired the projectile.
         * @param target The {@link Client} that was killed.
         */
    
	 
     private void killClient(Client source, Client target) {
     			//System.out.println("inside kill client");
                assert(source != null);
                assert(target != null);
                Mazewar.consolePrintLn(source.getName() + " just vaporized " + 
                                target.getName());
                ServerPointer.source=source.getName();
		ServerPointer.target=target.getName();
                
		//client side
                if(CHT!=null){ 
			notifyClientKilled(source, target);  
                	CHT.source = source;
			//dead client execute here
                	if(CHT.self.getName().equals(target.getName()))
                		CHT.reborn(target.getName());
                
                }
                
                
                
                // Pick a random starting point, and check to see if it is already occupied
                // the server with the dead client execute this
                if(clientQueue!=null){
                System.out.println("inside server handle kill");
		            Object o = clientMap.remove(target);
		            assert(o instanceof Point);
		            Point point = (Point)o;
		            CellImpl cell = getCellImpl(point);
		            cell.setContents(null);
		            update();
		            notifyClientKilled(source, target);
		            	point = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
				        cell = getCellImpl(point);
				        // Repeat until we find an empty cell
				        while(cell.getContents() != null) {
				                point = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
				                cell = getCellImpl(point);
				        }
				        Direction d = Direction.random();
				        while(cell.isWall(d)) {
				                d = Direction.random();
				        }
				        cell.setContents(target);
				      
					
				        clientMap.put(target, new DirectedPoint(point, d));
				        update();
				
					this.finished=true;
						//ServerPointer.Broad_cast();
		}
				
        }
        
        /**
         * Internal helper called when a {@link Client} emits a turnLeft action.
         * @param client The {@link Client} to rotate.
         */
        private void rotateClientLeft(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                clientMap.put(client, new DirectedPoint(dp, dp.getDirection().turnLeft()));
                update();
        }
        
        /**
         * Internal helper called when a {@link Client} emits a turnRight action.
         * @param client The {@link Client} to rotate.
         */
        private void rotateClientRight(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                clientMap.put(client, new DirectedPoint(dp, dp.getDirection().turnRight()));
                update();
        }
        
        /**
         * Internal helper called to move a {@link Client} in the specified
         * {@link Direction}.
         * @param client The {@link Client} to be move.
         * @param d The {@link Direction} to move.
         * @return If the {@link Client} cannot move in that {@link Direction}
         * for some reason, return <code>false</code>, otherwise return 
         * <code>true</code> indicating success.
         */
        private boolean moveClient(Client client, Direction d) {
                assert(client != null);
                assert(d != null);
                Point oldPoint = getClientPoint(client);
                CellImpl oldCell = getCellImpl(oldPoint);
                
                /* Check that you can move in the given direction */
                if(oldCell.isWall(d)) {
                        /* Move failed */
                        clientMap.put(client, oldPoint);
                        return false;
                }
                
                DirectedPoint newPoint = new DirectedPoint(oldPoint.move(d), getClientOrientation(client));
                
                /* Is the point withint the bounds of maze? */
                assert(checkBounds(newPoint));
                CellImpl newCell = getCellImpl(newPoint);
                if(newCell.getContents() != null) {
                        /* Move failed */
                        clientMap.put(client, oldPoint);
                        return false;
                }
                
                /* Write the new cell */
                clientMap.put(client, newPoint);
                newCell.setContents(client);
                /* Clear the old cell */
                oldCell.setContents(null);	
                
                update();
                return true; 
        }
       
        /**
         * The random number generator used by the {@link Maze}.
         */
        private final Random randomGen;

        /**
         * The maximum X coordinate of the {@link Maze}.
         */
        private final int maxX;

        /**
         * The maximum Y coordinate of the {@link Maze}.
         */
        private final int maxY;

        /** 
         * The {@link Vector} of {@link Vector}s holding the
         * {@link Cell}s of the {@link Maze}.
         */
        private final Vector mazeVector;

        /**
         * A map between {@link Client}s and {@link DirectedPoint}s
         * locating them in the {@link Maze}.
         */
        private final Map clientMap = new HashMap();

        /**
         * The set of {@link MazeListener}s that are presently
         * in the notification queue.
         */
        private final Set listenerSet = new HashSet();

        /**
         * Mapping from {@link Projectile}s to {@link DirectedPoint}s. 
         */
        private final Map projectileMap = new HashMap();
		//public static final Map pMap = new HashMap();
        
        /**
         * The set of {@link Client}s that have {@link Projectile}s in 
         * play.
         */
        private final Set clientFired = new HashSet();
       
        /**
         * The thread used to manage {@link Projectile}s.
         */
        private final Thread thread;
        
        /**
         * Generate a notification to listeners that a
         * {@link Client} has been added.
         * @param c The {@link Client} that was added.
         */
        private void notifyClientAdd(Client c, int score) {
                assert(c != null);
                Iterator i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;

							 ml.clientAdded(c,score);

                } 
        }
        
        /**
         * Generate a notification to listeners that a 
         * {@link Client} has been removed.
         * @param c The {@link Client} that was removed.
         */
        private void notifyClientRemove(Client c) {
                assert(c != null);
                Iterator i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.clientRemoved(c);
                } 
        }
        
        /**
         * Generate a notification to listeners that a
         * {@link Client} has fired.
         * @param c The {@link Client} that fired.
         */
        private void notifyClientFired(Client c) {
                assert(c != null);
                Iterator i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.clientFired(c);
                } 
        }
        
        /**
         * Generate a notification to listeners that a
         * {@link Client} has been killed.
         * @param source The {@link Client} that fired the projectile.
         * @param target The {@link Client} that was killed.
         */
        private void notifyClientKilled(Client source, Client target) {
                assert(source != null);
                assert(target != null);
                Iterator i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.clientKilled(source, target);
                } 
        }
        
        /**
         * Generate a notification that the {@link Maze} has 
         * changed in some fashion.
         */
        private void update() {
                Iterator i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.mazeUpdate();
                } 
        }

        /**
         * A concrete implementation of the {@link Cell} class that
         * special to this implementation of {@link Maze}s.
         */
        private class CellImpl extends Cell implements Serializable {
                /**
                 * Has this {@link CellImpl} been visited while
                 * constructing the {@link Maze}.
                 */
                private boolean visited = false;

                /**
                 * The walls of this {@link Cell}.
                 */
                private boolean walls[] = {true, true, true, true};

                /**
                 * The contents of the {@link Cell}. 
                 * <code>null</code> indicates that it is empty.
                 */
                private Object contents = null;
                
                /**
                 * Helper function to convert a {@link Direction} into 
                 * an array index for easier access.
                 * @param d The {@link Direction} to convert.
                 * @return An integer index into <code>walls</code>.
                 */
                private int directionToArrayIndex(Direction d) {
                        assert(d != null);
                        if(d.equals(Direction.North)) {
                                return 0;
                        } else if(d.equals(Direction.East)) {
                                return 1;
                        } else if(d.equals(Direction.South)) {
                                return 2;
                        } else if(d.equals(Direction.West)) {
                                return 3;
                        }
                        /* Impossible */
                        return -1; 
                }
                
                /* Required for the abstract implementation */
                
                public boolean isWall(Direction d) {
                        assert(d != null);
                        return this.walls[directionToArrayIndex(d)];
                }
                
                public synchronized Object getContents() {
                        return this.contents;
                }
                
                /* Internals used by MazeImpl */
                
                /**
                 * Indicate that this {@link Cell} has been
                 * visited while building the {@link MazeImpl}.
                 */
                public void setVisited() {
                        visited = true;
                }
                
                /**
                 * Has this {@link Cell} been visited in the process
                 * of recursviely building the {@link Maze}?
                 * @return <code>true</code> if visited, <code>false</code> 
                 * otherwise.
                 */
                public boolean visited() {
                        return visited;
                }
                
                /**
                 * Add a wall to this {@link Cell} in the specified
                 * Cardinal {@link Direction}.
                 * @param d Which wall to add.
                 */
                public void setWall(Direction d) {
                        assert(d != null);
                        this.walls[directionToArrayIndex(d)] = true;
                }
                
                /**
                 * Remove the wall from this {@link Cell} in the specified
                 * Cardinal {@link Direction}.
                 * @param d Which wall to remove.
                 */
                public void removeWall(Direction d) {
                        assert(d != null);
                        this.walls[directionToArrayIndex(d)] = false;
                }
                
                /**
                 * Set the contents of this {@link Cell}.
                 * @param contents Object to place in the {@link Cell}.
                 * Use <code>null</code> if you want to empty it.
                 */
                public synchronized void setContents(Object contents) {
                        this.contents = contents;
                }
                
        }
        
        /** 
         * Removes the wall in the {@link Cell} at the specified {@link Point} and 
         * {@link Direction}, and the opposite wall in the adjacent {@link Cell}.
         * @param point Location to remove the wall.
         * @param d Cardinal {@link Direction} specifying the wall to be removed.
         */
        private void removeWall(Point point, Direction d) {
                assert(point != null);
                assert(d != null);
                CellImpl cell = getCellImpl(point);
                cell.removeWall(d);
                Point adjacentPoint = point.move(d);
                CellImpl adjacentCell = getCellImpl(adjacentPoint);
                adjacentCell.removeWall(d.invert());
        }
        
        /** 
         * Pick randomly pick an unvisited neighboring {@link CellImpl}, 
         * if none return <code>null</code>. 
         * @param point The location to pick a neighboring {@link CellImpl} from.
         * @return The Cardinal {@link Direction} of a {@link CellImpl} that hasn't
         * yet been visited.
         */
        private Direction pickNeighbor(Point point) {
                assert(point != null);
                Direction directions[] = { 
                        Direction.North, 
                        Direction.East, 
                        Direction.West, 
                        Direction.South };
                        
                        // Create a vector of the possible choices
                        Vector options = new Vector();	       
                        
                        // Iterate through the directions and see which
                        // Cells have been visited, adding those that haven't
                        for(int i = 0; i < 4; i++) {
                                Point newPoint = point.move(directions[i]);
                                if(checkBounds(newPoint)) {
                                        CellImpl cell = getCellImpl(newPoint);
                                        if(!cell.visited()) {
                                                options.add(directions[i]);
                                        }
                                }
                        }
                        
                        // If there are no choices just return null
                        if(options.size() == 0) {
                                return null;
                        }
                        
                        // If there is at least one option, randomly choose one.
                        int n = randomGen.nextInt(options.size());
                        
                        Object o = options.get(n);
                        assert(o instanceof Direction);
                        return (Direction)o;
        }
        
        /**
         * Recursively carve out a {@link Maze}
         * @param point The location in the {@link Maze} to start carving.
         */
        private void buildMaze(Point point) {
                assert(point != null);
                CellImpl cell = getCellImpl(point);
                cell.setVisited();
                Direction d = pickNeighbor(point);
                while(d != null) {	    
                        removeWall(point, d);
                        Point newPoint = point.move(d);
                        buildMaze(newPoint);
                        d = pickNeighbor(point);
                }
        }
       
        /** 
         * Obtain the {@link CellImpl} at the specified point. 
         * @param point Location in the {@link Maze}.
         * @return The {@link CellImpl} representing that location.
         */
        private CellImpl getCellImpl(Point point) {
                assert(point != null);
                Object o1 = mazeVector.get(point.getX());
                assert(o1 instanceof Vector);
                Vector v1 = (Vector)o1;
                Object o2 = v1.get(point.getY());
                assert(o2 instanceof CellImpl);
                return (CellImpl)o2;
        }
}
