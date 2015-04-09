package subscription;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SubscriptionCollection<T> implements Subscribable<T>, Iterable<T> {
	private final List<T> subscribers;
	
	public SubscriptionCollection() {
		this.subscribers = new ArrayList<T>();
	}
	
	@Override
	public void subscribe(T subscription) {
		subscribers.add(subscription);
	}

	@Override
	public void unsubscribe(T subscription) {
		subscribers.remove(subscription);
	}
	
	@Override
	public Iterator<T> iterator() {
		return subscribers.iterator();
	}
}
