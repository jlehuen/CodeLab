package codelab.utils;

public enum ColorCode {
	COLOR_WHITE,
	COLOR_BLACK,
	COLOR_RED,
	COLOR_GREEN,
	COLOR_BLUE,
	COLOR_CYAN,
	COLOR_YELLOW,
	COLOR_MAGENTA,
	COLOR_ERROR;

	public int getRGB() {
		// Returns an integer pixel in the default RGB color
		// model (TYPE_INT_ARGB) and default sRGB colorspace
		switch (this) {
			case COLOR_WHITE:	return -1;
			case COLOR_BLACK:	return -16777216;
			case COLOR_RED:		return -65536;
			case COLOR_GREEN:	return -16711936;
			case COLOR_BLUE:	return -16776961;
			case COLOR_CYAN:	return -16515585;
			case COLOR_YELLOW:	return -256;
			case COLOR_MAGENTA:	return -65281;
			default:			return 0;
		}
	}
}
