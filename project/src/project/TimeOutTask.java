package project;

import java.util.TimerTask;

import protocol.Packet;

public class TimeOutTask  extends Thread {

	private Packet packet;
	
	public TimeOutTask(Packet packet) {
		this.packet = packet;
	}
	
	public void cancel() {
		this.interrupt();
	}
	@Override
	public void run() {
		try {
			sleep(100);
		} catch (InterruptedException e) {
			return;
		}
		TCP.ackTimeOut(packet);
	}
}
