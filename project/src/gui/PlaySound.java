package gui;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;
import javax.sound.sampled.LineEvent.Type;

public class PlaySound {

	public static void playClip(File clipFile) throws IOException, 
		UnsupportedAudioFileException, LineUnavailableException, InterruptedException {
		class AudioListener implements LineListener {
			private boolean done = false;
			@Override public synchronized void update(LineEvent event) {
				Type eventType = event.getType();
				if (eventType == Type.STOP || eventType == Type.CLOSE) {
					done = true;
					notifyAll();
				}
			}
			public synchronized void waitUntilDone() throws InterruptedException {
				while (!done) { wait(); }
			}
		}
		AudioListener listener = new AudioListener();
		System.out.println(AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE));
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(clipFile);
		try {
			Clip clip = AudioSystem.getClip();
			clip.addLineListener(listener);
			clip.open(audioInputStream);
			try {
				clip.start();
				listener.waitUntilDone();
			} finally {
				clip.close();
			}
		} finally {
			audioInputStream.close();
		}
	}
}
