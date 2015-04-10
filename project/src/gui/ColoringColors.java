package gui;

public enum ColoringColors {
	NORMAL, FIFTY_SHADES, RAINBOW, ANIMATED_RAINBOW, G2G, G2R, G2B, R2B, LB2G, G2BL;
	
	public String toString() {
		switch(this) {
			case NORMAL: return "Normal";
			case FIFTY_SHADES: return "Fifty Shades of Gray";
			case RAINBOW: return "Rainbow";
			case ANIMATED_RAINBOW: return "Animated Rainbow";
			case G2G: return "Gray to Green";
			case G2R: return "Gray to Red";
			case G2B: return "Gray to Blue";
			case R2B: return "Red to Blue";
			case LB2G: return "Light Blue to Green";
			case G2BL: return "Gray to Black";
			default: return "NOPE";
		}
	}
	
	public static ColoringColors getColor(int color) {
		switch(color) {
			case 1: return FIFTY_SHADES;
			case 2: return RAINBOW;
			case 3: return ANIMATED_RAINBOW;
			case 4: return G2G;
			case 5: return G2R;
			case 6: return G2B;
			case 7: return R2B;
			case 8: return LB2G;
			case 9: return G2BL;
			default: return NORMAL;
		}
	}
}
