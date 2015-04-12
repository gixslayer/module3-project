package events;

import java.util.LinkedList;
import java.util.Queue;

public final class EventQueue {
	private Queue<Event> buffer;
	private Queue<Event> backBuffer;
	
	public EventQueue() {
		this.buffer = new LinkedList<Event>();
		this.backBuffer = new LinkedList<Event>();
	}
	
	public void enqueue(Event event) {
		synchronized(buffer) {
			buffer.add(event);
		}
	}
	
	public Queue<Event> swapBuffers() {
		synchronized(buffer) {
			Queue<Event> oldBackBuffer = backBuffer;
			backBuffer = buffer;
			buffer = oldBackBuffer;
			
			return backBuffer;
		}
	}
}
