//Prototype implementation of Car Test class
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2016

//Hans Henrik Lovengreen    Oct 3, 2016

public class CarTest_135 extends Thread {

	CarTestingI cars;
	int testno;

	public CarTest_135(CarTestingI ct, int no) {
		cars = ct;
		testno = no;
	}

	public void run() {
		try {
			switch (testno) {
			case 0:
				// Demonstration of startAll/stopAll.
				// Should let the cars go one round (unless very fast)
				cars.startAll();
				sleep(3000);
				cars.stopAll();
				break;

			case 1:
				// Barrier toggle.
				// Activate the barrier and then quickly deactivate and activate it again.
				// Should stop most of the cars, except for those going really fast.
				// (And on return, of course, let them pass again)
				cars.barrierOn();
				sleep(100);
				
				//cars.startAll() sometimes omit starting car 0
				for (int i = 0; i < 9; i++) { 
					cars.startCar(i);
				}
				sleep(100);
				cars.barrierOff();
				sleep(100);
				cars.barrierOn();
				break;
			
			case 2:
				// Tile and Alley synchronization.
				// Ridiculously fast cars. (False 'Users : -X' may occur)
				// There should be no crashes
				cars.println("100x faster");
				for (int i = 1; i < 9; i++) {
					cars.setSpeed(i, 1);
				}
				cars.startAll();
				
				sleep(2000);
				cars.stopAll();
				break;
				
			case 3:
				// Set speed up, remove car 4 and 5 and return to normal speed
				// There should be no change except for two missing cars.
				// (Only for semaphore solution)
				for (int i = 1; i < 9; i++) {
					cars.setSpeed(i, 1);
				}
				cars.startAll();
				sleep(1000);
				cars.removeCar(4);
				cars.removeCar(5);
				
				for (int i = 1; i < 9; i++) {
					cars.setSpeed(i, 100);
				}
				
				sleep(1500);
				cars.stopAll();
				break;

			case 19:
				// Demonstration of speed setting.
				// Change speed to double of default values
				cars.println("Doubling speeds");
				for (int i = 1; i < 9; i++) {
					cars.setSpeed(i, 50);
				}
				;
				break;

			default:
				cars.println("Test " + testno + " not available");
			}

			cars.println("Test ended");

		} catch (Exception e) {
			System.err.println("Exception in test: " + e);
		}
	}

}
