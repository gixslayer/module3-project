package containers;

import java.util.LinkedList;
import java.util.Queue;

public final class PriorityQueue<T> {
	private Queue<T>[] queues;
	
	@SuppressWarnings("unchecked")
	public PriorityQueue() {
		this.queues = new Queue[Priority.NUM_PRIORITIES];
		
		for(int i = 0; i < queues.length; i++) {
			queues[i] = new LinkedList<T>();
		}
	}
	
	public void add(T e, Priority priority) {
		queues[priority.getValue()].add(e);
	}
	
	public T poll() {
		for(int i = Priority.NUM_PRIORITIES - 1; i > -1; i--) {
			T value = queues[i].poll();
			
			if(value != null) {
				return value;
			}
		}
		
		return null;
	}
	
	public T peek() {
		for(int i = Priority.NUM_PRIORITIES - 1; i > -1; i--) {
			T value = queues[i].peek();
			
			if(value != null) {
				return value;
			}
		}
		
		return null;
	}
}
