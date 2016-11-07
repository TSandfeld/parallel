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
		try {
			e.P();
		} catch (InterruptedException e) {
		}
		if (!isopen) {
			g.V();
			isopen = true;
		}
		e.V();
	}

	public void close() {
		try {
			e.P();
		} catch (InterruptedException e) {
		}
		if (isopen) {
			try {
				g.P();
			} catch (InterruptedException e) {
			}
			isopen = false;
		}
		e.V();
	}

}

class Alley {

	Semaphore up = new Semaphore(1);
	Semaphore sem = new Semaphore(1);
	Semaphore down = new Semaphore(1);
	
	int counterUp = 0;
	int counterDown = 0;
	int direction = 2;
	public Alley() {
	}

	public void checkCritPos(int no, Pos position) {
		Pos crit1 = new Pos(2, 0); // for cars 1 and 2 enter
		Pos crit2 = new Pos(1, 2); // in the top - car 3,4 enter
		Pos crit11 = new Pos(9, 1); // bottom - leave for 1,2,3,4

		Pos crit3 = new Pos(9, 0); // car 5-8 enter
		Pos crit33 = new Pos(0, 2); // car 5-8 leave

		switch (no) {
		case 1:
		case 2:
			enterLeave(no, position, crit1, crit11);
			break;

		case 3:
		case 4:
			enterLeave(no, position, crit2, crit11);
			break;

		default:
			enterLeave(no, position, crit3, crit33);
			break;
		}

	}

	public void enterLeave(int no, Pos position, Pos enter, Pos leave) {
		if (position.equals(enter)) {
			// System.out.println(position + " vs " + enter);
			try {
				enter(no);	
			} catch (InterruptedException e) {
				e.printStackTrace();
				// TODO: handle exception
			}
		} else if (position.equals(leave)) {
			leave(no);
		}
	}

	public void enter(int no) throws InterruptedException {
		if (no > 4) {
			up.P();
			counterUp++;
			if (counterUp == 1) {
				sem.P();
			}
			up.V();
		}
		if (no <= 4) {
			down.P();
			counterDown++;
			if (counterDown == 1) {
				sem.P();
			}
			down.V();
		}
	}

	public void leave(int no) {
		if (no > 4) {
				counterUp--;
				if (counterUp == 0) {
					sem.V();
				}
		}
		if (no <= 4) {
				counterDown--;
				if (counterDown == 0) {
					sem.V();
				}
		}
	}
	}
class Barrier {

	Semaphore[] semaphoreArray = new Semaphore[9];
	boolean isActive = false;
	int counter = 0;

	public Barrier() {
		for (int i = 0; i < semaphoreArray.length; i++) {
			semaphoreArray[i] = new Semaphore(1);
		}
	}

	public void sync(int num) throws InterruptedException {
		if (isActive) {
			checkCars();
			semaphoreArray[num].P();
			if (!isActive){
				semaphoreArray[num].V();
			}
		}

	} // Wait for others to arrive (if barrier active)

	public void checkCars() {
		counter++;
		if (counter == 9) {
			for (int i = 0; i < semaphoreArray.length; i++) {
				semaphoreArray[i].V();
			}
			counter = 0;
		}
	}

	public void on() throws InterruptedException {
		if (!isActive)
		for (int i = 0; i < semaphoreArray.length; i++) {
			semaphoreArray[i].P();
			isActive = true;
		}
		
	} // Activate barrier

	public void off() {
		if (isActive){
		for (int i = 0; i < semaphoreArray.length; i++) {
			semaphoreArray[i].V();
		}
		counter = 0;
		isActive = false;
		}
	} // Deactivate barrier

}

class Car extends Thread {

	int basespeed = 100; // Rather: degree of slowness
	int variation = 50; // Percentage of base speed

	CarDisplayI cd; // GUI part

	int no; // Car number
	Pos startpos; // Startpositon (provided by GUI)
	Pos barpos; // Barrierpositon (provided by GUI)
	Color col; // Car color
	Gate mygate; // Gate at startposition

	int speed; // Current car speed
	Pos curpos; // Current position
	Pos newpos; // New position to go to

	Alley alley;
	Barrier barrier;

	Semaphore[][] sems;

	public Car(int no, CarDisplayI cd, Gate g, Semaphore[][] semaphores, Alley alley, Barrier barrier) {

		this.no = no;
		this.cd = cd;
		mygate = g;
		startpos = cd.getStartPos(no);
		barpos = cd.getBarrierPos(no); // For later use

		col = chooseColor();

		this.sems = semaphores;
		this.alley = alley;
		this.barrier = barrier;

		// do not change the special settings for car no. 0
		if (no == 0) {
			basespeed = 0;
			variation = 0;
			setPriority(Thread.MAX_PRIORITY);
		}
	}

	public synchronized void setSpeed(int speed) {
		if (no != 0 && speed >= 0) {
			basespeed = speed;
		} else
			cd.println("Illegal speed settings");
	}

	public synchronized void setVariation(int var) {
		if (no != 0 && 0 <= var && var <= 100) {
			variation = var;
		} else
			cd.println("Illegal variation settings");
	}

	synchronized int chooseSpeed() {
		double factor = (1.0D + (Math.random() - 0.5D) * 2 * variation / 100);
		return (int) Math.round(factor * basespeed);
	}

	private int speed() {
		// Slow down if requested
		final int slowfactor = 3;
		return speed * (cd.isSlow(curpos) ? slowfactor : 1);
	}

	Color chooseColor() {
		return Color.blue; // You can get any color, as longs as it's blue
	}

	Pos nextPos(Pos pos) {
		// Get my track from display
		return cd.nextPos(no, pos);
	}

	boolean atGate(Pos pos) {
		return pos.equals(startpos);
	}

	public void run() {
		boolean isMoving = false;
		try {

			speed = chooseSpeed();
			curpos = startpos;
			cd.mark(curpos, col, no);
			while (true) {
				sleep(speed());

				if (atGate(curpos)) {
					mygate.pass();
					speed = chooseSpeed();
				}

				newpos = nextPos(curpos);

				alley.checkCritPos(no, newpos);

				if (curpos.equals(cd.getBarrierPos(no))) {
					//System.out.println(newpos.toString() + " and " + cd.getBarrierPos(no).toString());
					barrier.sync(no);

				}

				try {
					isMoving = true;
					sems[newpos.col][newpos.row].P();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// Move to new position
				cd.clear(curpos);
				cd.mark(curpos, newpos, col, no);
				sleep(speed());
				cd.clear(curpos, newpos);
				cd.mark(newpos, col, no);
				sems[curpos.col][curpos.row].V();
				isMoving = false;
				curpos = newpos;
			}

		}catch (InterruptedException e) {
	    	 	sems[curpos.col][curpos.row].V();
				cd.clear(curpos);
		     if (isMoving){
		    	 cd.clear(newpos);
		    	 sems[newpos.col][newpos.row].V();
		    	System.out.println(sems[newpos.col][newpos.row].toString()); 
		     }
		     
		}  catch (Exception e) {
			cd.println("Exception in Car no. " + no);
			System.err.println("Exception in Car no. " + no + ":" + e);
			e.printStackTrace();
		}
	}

}

public class CarControl implements CarControlI {

	CarDisplayI cd; // Reference to GUI
	Car[] car; // Cars
	Gate[] gate; // Gates

	Semaphore[][] sems = new Semaphore[12][11]; // 2D array of semaphores
	Alley alley = new Alley();
	Barrier barrier = new Barrier();

	public CarControl(CarDisplayI cd) {
		this.cd = cd;
		car = new Car[9];
		gate = new Gate[9];

		for (int i = 0; i < 12; i++) {
			for (int j = 0; j < 11; j++) {
				Semaphore s = new Semaphore(1);
				sems[i][j] = s;
			}
		}

		for (int no = 0; no < 9; no++) {
			gate[no] = new Gate();
			car[no] = new Car(no, cd, gate[no], sems, alley, barrier);
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
		try {
			barrier.on();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void barrierOff() {
		barrier.off();
	}

	public void barrierSet(int k) {
		cd.println("Barrier threshold setting not implemented in this version");
		// This sleep is for illustrating how blocking affects the GUI
		// Remove when feature is properly implemented.
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}
	}

	public void removeCar(int no) {
		if (car[no] != null){
		car[no].interrupt();
		car[no] = null;
		} 
	}

	public void restoreCar(int no) {
		if (car[no] == null){
		car[no] = new Car(no,cd,gate[no],sems,alley,barrier);
		car[no].start();
		}
		}
	/* Speed settings for testing purposes */

	public void setSpeed(int no, int speed) {
		car[no].setSpeed(speed);
	}

	public void setVariation(int no, int var) {
		car[no].setVariation(var);
	}

}
