package containers;

import java.util.LinkedList;
import java.util.Queue;

public final class SynchronizedQueue<T> {
	private Queue<T> buffer;
	private Queue<T> backBuffer;
	
	public SynchronizedQueue() {
		this.buffer = new LinkedList<T>();
		this.backBuffer = new LinkedList<T>();
	}
	
	public void enqueue(T e) {
		synchronized(buffer) {
			buffer.add(e);
		}
	}
	
	public Queue<T> swapBuffers() {
		synchronized(buffer) {
			Queue<T> oldBuffer = buffer;
			
			buffer = backBuffer;
			backBuffer = oldBuffer;
			
			return backBuffer;
		}
	}
}
