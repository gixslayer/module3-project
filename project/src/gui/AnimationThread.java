package gui;

public class AnimationThread extends Thread {
	private float hue = 0;
	private boolean cont;
	
	public void setCont(boolean cont) {
		this.cont = cont;
	}
	
	public float getHue() {
		return hue;
	}
	
	@Override
	public void run() {
		while(cont) {
			hue+=0.001;
			if(hue == 1) hue = 0;
			try{
				Thread.sleep(10);
			}
			catch (InterruptedException e) { }
		}
	}
}
