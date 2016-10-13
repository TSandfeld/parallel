//Prototype implementation of Car Control
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2016

//Hans Henrik Lovengreen    Oct 3, 2016


import java.awt.Color;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

class Gate {

    Semaphore g = new Semaphore(0);
    Semaphore e = new Semaphore(1);
    boolean isopen = false;

    public void pass() throws InterruptedException {
        g.P(); 
        g.V();
    }

    public void open() {
        try { e.P(); } catch (InterruptedException e) {}
        if (!isopen) { g.V();  isopen = true; }
        e.V();
    }

    public void close() {
        try { e.P(); } catch (InterruptedException e) {}
        if (isopen) { 
            try { g.P(); } catch (InterruptedException e) {}
            isopen = false;
        }
        e.V();
    }

}

class Alley {
	
	Semaphore g = new Semaphore(1);
	int CurrentDirection;
	
	public Alley() {}
	
	public void checkCritPos(int no, Pos position) {
		Pos crit1 = new Pos(2,0); //for cars 1 and 2 enter
		Pos crit2 = new Pos(1,2); //in the top - car 3,4 enter
		Pos crit11 = new Pos(9,1); //bottom - leave for 1,2,3,4
		
		Pos crit3 = new Pos(9,0); // car 5-8 enter
		Pos crit33 = new Pos(0,2); // car 5-8 leave
		
		switch (no) {
		case 1: case 2: 
			enterLeave(no, position, crit1, crit11);
			break;
			
		case 3: case 4:
			enterLeave(no, position, crit2, crit11);
			break;

		default:
			enterLeave(no, position, crit3, crit33);
			break;
		}
		
	}
	
	public void enterLeave(int no, Pos position, Pos enter, Pos leave) {
		if(position.equals(enter)) {
			System.out.println(position + " vs " + enter);
			enter(no);
		} else if(position.equals(leave)) {
			leave(no);
		}
	}
	
	public void enter(int no) {
		System.out.println("Car no " + no + " in.");
		try {
			g.P();
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void leave(int no) {
		System.out.println("Car no " + no + " out.");
		g.V();
	}
}


class Car extends Thread {

    int basespeed = 100;             // Rather: degree of slowness
    int variation =  50;             // Percentage of base speed

    CarDisplayI cd;                  // GUI part

    int no;                          // Car number
    Pos startpos;                    // Startpositon (provided by GUI)
    Pos barpos;                      // Barrierpositon (provided by GUI)
    Color col;                       // Car  color
    Gate mygate;                     // Gate at startposition


    int speed;                       // Current car speed
    Pos curpos;                      // Current position 
    Pos newpos;                      // New position to go to
    
    Alley alley;
    
    Semaphore[][] sems;

    public Car(int no, CarDisplayI cd, Gate g, Semaphore[][] semaphores, Alley alley) {

        this.no = no;
        this.cd = cd;
        mygate = g;
        startpos = cd.getStartPos(no);
        barpos = cd.getBarrierPos(no);  // For later use

        col = chooseColor();
        
        this.sems = semaphores;
        this.alley = alley;

        // do not change the special settings for car no. 0
        if (no==0) {
            basespeed = 0;  
            variation = 0; 
            setPriority(Thread.MAX_PRIORITY); 
        }
    }

    public synchronized void setSpeed(int speed) { 
        if (no != 0 && speed >= 0) {
            basespeed = speed;
        }
        else
            cd.println("Illegal speed settings");
    }

    public synchronized void setVariation(int var) { 
        if (no != 0 && 0 <= var && var <= 100) {
            variation = var;
        }
        else
            cd.println("Illegal variation settings");
    }

    synchronized int chooseSpeed() { 
        double factor = (1.0D+(Math.random()-0.5D)*2*variation/100);
        return (int) Math.round(factor*basespeed);
    }

    private int speed() {
        // Slow down if requested
        final int slowfactor = 3;  
        return speed * (cd.isSlow(curpos)? slowfactor : 1);
    }

    Color chooseColor() { 
        return Color.blue; // You can get any color, as longs as it's blue 
    }

    Pos nextPos(Pos pos) {
        // Get my track from display
        return cd.nextPos(no,pos);
    }

    boolean atGate(Pos pos) {
        return pos.equals(startpos);
    }

   public void run() {
        try {

            speed = chooseSpeed();
            curpos = startpos;
            cd.mark(curpos,col,no);

            while (true) { 
                sleep(speed());
  
                if (atGate(curpos)) { 
                    mygate.pass(); 
                    speed = chooseSpeed();
                }
                	
                newpos = nextPos(curpos);
                
                alley.checkCritPos(no, newpos);
                
                try {
                    sems[newpos.col][newpos.row].P();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
                
                
                //  Move to new position 
                cd.clear(curpos);
                cd.mark(curpos,newpos,col,no);
                sleep(speed());
                cd.clear(curpos,newpos);
                cd.mark(newpos,col,no);

                sems[curpos.col][curpos.row].V();
                curpos = newpos;
            }

        } catch (Exception e) {
            cd.println("Exception in Car no. " + no);
            System.err.println("Exception in Car no. " + no + ":" + e);
            e.printStackTrace();
        }
    }

}

public class CarControl implements CarControlI{

    CarDisplayI cd;           // Reference to GUI
    Car[]  car;               // Cars
    Gate[] gate;              // Gates
    
    Semaphore[][] sems = new Semaphore[12][11]; //2D array of semaphores
    Alley alley = new Alley();

    public CarControl(CarDisplayI cd) {
        this.cd = cd;
        car  = new  Car[9];
        gate = new Gate[9];
        
        for(int i = 0; i < 12; i++) {
        	for(int j = 0; j < 11; j++) {
        		Semaphore s = new Semaphore(1);
        		sems[i][j] = s;
        	}
        }

        for (int no = 0; no < 9; no++) {
            gate[no] = new Gate();
            car[no] = new Car(no,cd,gate[no], sems, alley);
            car[no].start();
        } 
    }

   public void startCar(int no) {
        gate[no].open();
    }

    public void stopCar(int no) {
        gate[no].close();
    }

    public void barrierOn() { 
        cd.println("Barrier On not implemented in this version");
    }

    public void barrierOff() { 
        cd.println("Barrier Off not implemented in this version");
    }

    public void barrierSet(int k) { 
        cd.println("Barrier threshold setting not implemented in this version");
         // This sleep is for illustrating how blocking affects the GUI
        // Remove when feature is properly implemented.
        try { Thread.sleep(3000); } catch (InterruptedException e) { }
     }

    public void removeCar(int no) { 
        cd.println("Remove Car not implemented in this version");
    }

    public void restoreCar(int no) { 
        cd.println("Restore Car not implemented in this version");
    }

    /* Speed settings for testing purposes */

    public void setSpeed(int no, int speed) { 
        car[no].setSpeed(speed);
    }

    public void setVariation(int no, int var) { 
        car[no].setVariation(var);
    }

}






