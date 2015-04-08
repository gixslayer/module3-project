package gui;

public enum ColoringColors {
	NORMAL, FIFTY_SHADES, RAINBOW, ANIMATED_RAINBOW;
	
	public String toString() {
		switch(this) {
			case NORMAL: return "NORMAL";
			case FIFTY_SHADES: return "FIFTY_SHADES";
			case RAINBOW: return "RAINBOW";
			case ANIMATED_RAINBOW: return "ANIMATED_RAINBOW";
			default: return "NOPE";
		}
	}
}
