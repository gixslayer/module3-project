package test.network;

import static org.junit.Assert.*;

import network.Priority;
import network.PriorityQueue;

import org.junit.Test;

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
