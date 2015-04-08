package gui;

public enum ColoringColors {
	NORMAL, FIFTY_SHADES, RAINBOW;
	
	public String toString() {
		switch(this) {
			case NORMAL: return "NORMAL";
			case FIFTY_SHADES: return "FIFTY_SHADES";
			case RAINBOW: return "RAINBOW";
			default: return "NOPE";
		}
	}
}
