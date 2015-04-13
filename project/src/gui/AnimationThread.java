package gui;

import java.awt.Color;

public class AnimationThread extends Thread {
	private float hue = 0;
	private boolean cont;
	private MainGUI main;
	
	public AnimationThread(MainGUI main) {
		this.main = main;
	}
	
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
			if(main.getRainBowMode()) main.setBGColor(Color.getHSBColor(hue, 1, 1));
			try{
				Thread.sleep(10);
			}
			catch (InterruptedException e) { }
		}
	}
}
