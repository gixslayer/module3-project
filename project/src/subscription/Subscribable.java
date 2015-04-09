package subscription;

public interface Subscribable<T> {
	void subscribe(T subscription);
	void unsubscribe(T subscription);
}
