package tests.containers;

import static org.junit.Assert.*;

import org.junit.Test;

import containers.Priority;
import containers.PriorityQueue;

public class PriorityQueueTest {
	@Test
	public void pollTest() {
		PriorityQueue<String> queue = new PriorityQueue<>();
		queue.add("normal", Priority.Normal);
		queue.add("high", Priority.High);
		queue.add("low", Priority.Low);
		assertEquals("high", queue.poll());
		assertEquals("normal", queue.poll());
		assertEquals("low", queue.poll());
	}
	
	@Test
	public void peekTest() {
		PriorityQueue<String> queue = new PriorityQueue<>();
		queue.add("normal", Priority.Normal);
		queue.add("high", Priority.High);
		queue.add("low", Priority.Low);
		assertEquals("high", queue.peek());
		assertEquals("high", queue.peek());
		assertEquals("high", queue.peek());
	}
}
