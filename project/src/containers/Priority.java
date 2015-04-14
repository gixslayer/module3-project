package containers;

public enum Priority {
	Low(0),
	Normal(1),
	High(2);
	
	public static final int NUM_PRIORITIES = 3;
	
	private final int value;
	
	Priority(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}
