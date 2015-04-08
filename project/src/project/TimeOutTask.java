package project;

import java.util.TimerTask;

public class TimeOutTask  extends TimerTask {

	private int destination;
	
	public TimeOutTask(int destination) {
		this.destination = destination;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		TCP.timeOut(destination);
		
		this.cancel();
	}
}
