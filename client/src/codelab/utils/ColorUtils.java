package codelab.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.lang.reflect.Field;

/**
*	Classe de méthodes utilitaires orientées couleurs
*	@author Jérôme Lehuen
*	@version 08/05/23
*/

public class ColorUtils {

	public static Color str2color(String name) throws Exception {
		Field field = Class.forName("java.awt.Color").getField(name);
		return (Color) field.get(null);
	}

	public static int rgb2gray(int rgb) {
		final int r = (rgb >> 16) & 0xFF;
		final int g = (rgb >> 8) & 0xFF;
		final int b = (rgb) & 0xFF;
		return (r + g + b) / 3;
	}

	public static int getBrightness(int hex) {
		int r = (hex & 0xFF0000) >> 16;
		int g = (hex & 0xFF00) >> 8;
		int b = (hex & 0xFF);
		return (int) (r + g + b) / 3;
	}

	public static String getColorNameFromHex(int hex) {
		int r = (hex & 0xFF0000) >> 16;
		int g = (hex & 0xFF00) >> 8;
		int b = (hex & 0xFF);
		return getColorNameFromRGB(r, g, b);
	}

	public static String getColorNameFromRGB(int r, int g, int b) {
		ColorName closestMatch = null;
		int minMSE = Integer.MAX_VALUE;
		for (ColorName color: colorDB) {
			int mse = color.computeMSE(r, g, b);
			if (mse < minMSE) {
				minMSE = mse;
				closestMatch = color;
			}
		}
		return closestMatch != null ? closestMatch.getName() : "COLOR_ERROR";
	}

	public static ColorCode rgb2color(int hex) {
		String name = getColorNameFromHex(hex);
		switch (name) {
			case "COLOR_WHITE": return ColorCode.COLOR_WHITE;
			case "COLOR_BLACK": return ColorCode.COLOR_BLACK;
			case "COLOR_RED": return ColorCode.COLOR_RED;
			case "COLOR_GREEN": return ColorCode.COLOR_GREEN;
			case "COLOR_BLUE": return ColorCode.COLOR_BLUE;
			case "COLOR_CYAN": return ColorCode.COLOR_CYAN;
			case "COLOR_YELLOW": return ColorCode.COLOR_YELLOW;
			case "COLOR_MAGENTA": return ColorCode.COLOR_MAGENTA;
			default: return ColorCode.COLOR_ERROR;
		}
	}

	///////////////////////////////////////////////////
	// Private classe for color database
	///////////////////////////////////////////////////

	private static class ColorName {

		private int r;
		private int g;
		private int b;
		private String name;

		public String getName() { return name; }

		public ColorName(String name, int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.name = name;
		}

		public int computeMSE(int pixR, int pixG, int pixB) {
			return ((pixR - r) * (pixR - r) + (pixG - g) * (pixG - g) + (pixB - b) * (pixB - b)) / 3;
		}
	}

	///////////////////////////////////////////////////
	// Color database
	///////////////////////////////////////////////////

	private static final ArrayList<ColorName> colorDB = new ArrayList<ColorName>() {{

		add(new ColorName("COLOR_RED", 0xFF, 0x00, 0x00));
		add(new ColorName("COLOR_GREEN", 0x00, 0xFF, 0x00));
		add(new ColorName("COLOR_BLUE", 0x00, 0x00, 0xFF));
		add(new ColorName("COLOR_CYAN", 0x00, 0xFF, 0xFF));
		add(new ColorName("COLOR_YELLOW", 0xFF, 0xFF, 0x00));
		add(new ColorName("COLOR_MAGENTA", 0xFF, 0x00, 0xFF));
		add(new ColorName("COLOR_BLACK", 0x00, 0x00, 0x00));
		add(new ColorName("COLOR_WHITE", 0xFF, 0xFF, 0xFF));
		/*
		add(new ColorName("AliceBlue", 0xF0, 0xF8, 0xFF));
		add(new ColorName("AntiqueWhite", 0xFA, 0xEB, 0xD7));
		add(new ColorName("Aquamarine", 0x7F, 0xFF, 0xD4));
		add(new ColorName("Azure", 0xF0, 0xFF, 0xFF));
		add(new ColorName("Beige", 0xF5, 0xF5, 0xDC));
		add(new ColorName("Bisque", 0xFF, 0xE4, 0xC4));
		add(new ColorName("BlanchedAlmond", 0xFF, 0xEB, 0xCD));
		add(new ColorName("BlueViolet", 0x8A, 0x2B, 0xE2));
		add(new ColorName("Brown", 0xA5, 0x2A, 0x2A));
		add(new ColorName("BurlyWood", 0xDE, 0xB8, 0x87));
		add(new ColorName("CadetBlue", 0x5F, 0x9E, 0xA0));
		add(new ColorName("Chartreuse", 0x7F, 0xFF, 0x00));
		add(new ColorName("Chocolate", 0xD2, 0x69, 0x1E));
		add(new ColorName("Coral", 0xFF, 0x7F, 0x50));
		add(new ColorName("CornflowerBlue", 0x64, 0x95, 0xED));
		add(new ColorName("Cornsilk", 0xFF, 0xF8, 0xDC));
		add(new ColorName("Crimson", 0xDC, 0x14, 0x3C));
		add(new ColorName("DarkBlue", 0x00, 0x00, 0x8B));
		add(new ColorName("DarkCyan", 0x00, 0x8B, 0x8B));
		add(new ColorName("DarkGoldenRod", 0xB8, 0x86, 0x0B));
		add(new ColorName("DarkGray", 0xA9, 0xA9, 0xA9));
		add(new ColorName("DarkGreen", 0x00, 0x64, 0x00));
		add(new ColorName("DarkKhaki", 0xBD, 0xB7, 0x6B));
		add(new ColorName("DarkMagenta", 0x8B, 0x00, 0x8B));
		add(new ColorName("DarkOliveGreen", 0x55, 0x6B, 0x2F));
		add(new ColorName("DarkOrange", 0xFF, 0x8C, 0x00));
		add(new ColorName("DarkOrchid", 0x99, 0x32, 0xCC));
		add(new ColorName("DarkRed", 0x8B, 0x00, 0x00));
		add(new ColorName("DarkSalmon", 0xE9, 0x96, 0x7A));
		add(new ColorName("DarkSeaGreen", 0x8F, 0xBC, 0x8F));
		add(new ColorName("DarkSlateBlue", 0x48, 0x3D, 0x8B));
		add(new ColorName("DarkSlateGray", 0x2F, 0x4F, 0x4F));
		add(new ColorName("DarkTurquoise", 0x00, 0xCE, 0xD1));
		add(new ColorName("DarkViolet", 0x94, 0x00, 0xD3));
		add(new ColorName("DeepPink", 0xFF, 0x14, 0x93));
		add(new ColorName("DeepSkyBlue", 0x00, 0xBF, 0xFF));
		add(new ColorName("DimGray", 0x69, 0x69, 0x69));
		add(new ColorName("DodgerBlue", 0x1E, 0x90, 0xFF));
		add(new ColorName("FireBrick", 0xB2, 0x22, 0x22));
		add(new ColorName("FloralWhite", 0xFF, 0xFA, 0xF0));
		add(new ColorName("ForestGreen", 0x22, 0x8B, 0x22));
		add(new ColorName("Fuchsia", 0xFF, 0x00, 0xFF));
		add(new ColorName("Gainsboro", 0xDC, 0xDC, 0xDC));
		add(new ColorName("GhostWhite", 0xF8, 0xF8, 0xFF));
		add(new ColorName("Gold", 0xFF, 0xD7, 0x00));
		add(new ColorName("GoldenRod", 0xDA, 0xA5, 0x20));
		add(new ColorName("Gray", 0x80, 0x80, 0x80));
		add(new ColorName("GreenYellow", 0xAD, 0xFF, 0x2F));
		add(new ColorName("HoneyDew", 0xF0, 0xFF, 0xF0));
		add(new ColorName("HotPink", 0xFF, 0x69, 0xB4));
		add(new ColorName("IndianRed", 0xCD, 0x5C, 0x5C));
		add(new ColorName("Indigo", 0x4B, 0x00, 0x82));
		add(new ColorName("Ivory", 0xFF, 0xFF, 0xF0));
		add(new ColorName("Khaki", 0xF0, 0xE6, 0x8C));
		add(new ColorName("Lavender", 0xE6, 0xE6, 0xFA));
		add(new ColorName("LavenderBlush", 0xFF, 0xF0, 0xF5));
		add(new ColorName("LawnGreen", 0x7C, 0xFC, 0x00));
		add(new ColorName("LemonChiffon", 0xFF, 0xFA, 0xCD));
		add(new ColorName("LightBlue", 0xAD, 0xD8, 0xE6));
		add(new ColorName("LightCoral", 0xF0, 0x80, 0x80));
		add(new ColorName("LightCyan", 0xE0, 0xFF, 0xFF));
		add(new ColorName("LightGoldenRodYellow", 0xFA, 0xFA, 0xD2));
		add(new ColorName("LightGray", 0xD3, 0xD3, 0xD3));
		add(new ColorName("LightGreen", 0x90, 0xEE, 0x90));
		add(new ColorName("LightPink", 0xFF, 0xB6, 0xC1));
		add(new ColorName("LightSalmon", 0xFF, 0xA0, 0x7A));
		add(new ColorName("LightSeaGreen", 0x20, 0xB2, 0xAA));
		add(new ColorName("LightSkyBlue", 0x87, 0xCE, 0xFA));
		add(new ColorName("LightSlateGray", 0x77, 0x88, 0x99));
		add(new ColorName("LightSteelBlue", 0xB0, 0xC4, 0xDE));
		add(new ColorName("LightYellow", 0xFF, 0xFF, 0xE0));
		add(new ColorName("LimeGreen", 0x32, 0xCD, 0x32));
		add(new ColorName("Linen", 0xFA, 0xF0, 0xE6));
		add(new ColorName("Maroon", 0x80, 0x00, 0x00));
		add(new ColorName("MediumAquaMarine", 0x66, 0xCD, 0xAA));
		add(new ColorName("MediumBlue", 0x00, 0x00, 0xCD));
		add(new ColorName("MediumOrchid", 0xBA, 0x55, 0xD3));
		add(new ColorName("MediumPurple", 0x93, 0x70, 0xDB));
		add(new ColorName("MediumSeaGreen", 0x3C, 0xB3, 0x71));
		add(new ColorName("MediumSlateBlue", 0x7B, 0x68, 0xEE));
		add(new ColorName("MediumSpringGreen", 0x00, 0xFA, 0x9A));
		add(new ColorName("MediumTurquoise", 0x48, 0xD1, 0xCC));
		add(new ColorName("MediumVioletRed", 0xC7, 0x15, 0x85));
		add(new ColorName("MidnightBlue", 0x19, 0x19, 0x70));
		add(new ColorName("MintCream", 0xF5, 0xFF, 0xFA));
		add(new ColorName("MistyRose", 0xFF, 0xE4, 0xE1));
		add(new ColorName("Moccasin", 0xFF, 0xE4, 0xB5));
		add(new ColorName("NavajoWhite", 0xFF, 0xDE, 0xAD));
		add(new ColorName("Navy", 0x00, 0x00, 0x80));
		add(new ColorName("OldLace", 0xFD, 0xF5, 0xE6));
		add(new ColorName("Olive", 0x80, 0x80, 0x00));
		add(new ColorName("OliveDrab", 0x6B, 0x8E, 0x23));
		add(new ColorName("Orange", 0xFF, 0xA5, 0x00));
		add(new ColorName("OrangeRed", 0xFF, 0x45, 0x00));
		add(new ColorName("Orchid", 0xDA, 0x70, 0xD6));
		add(new ColorName("PaleGoldenRod", 0xEE, 0xE8, 0xAA));
		add(new ColorName("PaleGreen", 0x98, 0xFB, 0x98));
		add(new ColorName("PaleTurquoise", 0xAF, 0xEE, 0xEE));
		add(new ColorName("PaleVioletRed", 0xDB, 0x70, 0x93));
		add(new ColorName("PapayaWhip", 0xFF, 0xEF, 0xD5));
		add(new ColorName("PeachPuff", 0xFF, 0xDA, 0xB9));
		add(new ColorName("Peru", 0xCD, 0x85, 0x3F));
		add(new ColorName("Pink", 0xFF, 0xC0, 0xCB));
		add(new ColorName("Plum", 0xDD, 0xA0, 0xDD));
		add(new ColorName("PowderBlue", 0xB0, 0xE0, 0xE6));
		add(new ColorName("Purple", 0x80, 0x00, 0x80));
		add(new ColorName("RosyBrown", 0xBC, 0x8F, 0x8F));
		add(new ColorName("RoyalBlue", 0x41, 0x69, 0xE1));
		add(new ColorName("SaddleBrown", 0x8B, 0x45, 0x13));
		add(new ColorName("Salmon", 0xFA, 0x80, 0x72));
		add(new ColorName("SandyBrown", 0xF4, 0xA4, 0x60));
		add(new ColorName("SeaGreen", 0x2E, 0x8B, 0x57));
		add(new ColorName("SeaShell", 0xFF, 0xF5, 0xEE));
		add(new ColorName("Sienna", 0xA0, 0x52, 0x2D));
		add(new ColorName("Silver", 0xC0, 0xC0, 0xC0));
		add(new ColorName("SkyBlue", 0x87, 0xCE, 0xEB));
		add(new ColorName("SlateBlue", 0x6A, 0x5A, 0xCD));
		add(new ColorName("SlateGray", 0x70, 0x80, 0x90));
		add(new ColorName("Snow", 0xFF, 0xFA, 0xFA));
		add(new ColorName("SpringGreen", 0x00, 0xFF, 0x7F));
		add(new ColorName("SteelBlue", 0x46, 0x82, 0xB4));
		add(new ColorName("Tan", 0xD2, 0xB4, 0x8C));
		add(new ColorName("Teal", 0x00, 0x80, 0x80));
		add(new ColorName("Thistle", 0xD8, 0xBF, 0xD8));
		add(new ColorName("Tomato", 0xFF, 0x63, 0x47));
		add(new ColorName("Turquoise", 0x40, 0xE0, 0xD0));
		add(new ColorName("Violet", 0xEE, 0x82, 0xEE));
		add(new ColorName("Wheat", 0xF5, 0xDE, 0xB3));
		add(new ColorName("WhiteSmoke", 0xF5, 0xF5, 0xF5));
		add(new ColorName("YellowGreen", 0x9A, 0xCD, 0x32));
		*/
	}};
}
