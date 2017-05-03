package net.shygoo.jconsolecolors;

import java.io.PrintStream;

public class JConsoleColors {
	
	private static class FilteredStream extends PrintStream {
		private boolean escaped = false; // set when sequence has started
		private boolean collectingNumber = false; // set when ready to collect number
		private String number = ""; // append characters here while collectingNumber == true
		// todo override write(int) instead for best speed & accuracy
		
		public FilteredStream(PrintStream stream){
			super(stream);
		}
		
		@Override
		public void print(String s){
			for(int i = 0; i < s.length(); i++){
				char currentChar = s.charAt(i);
				if(currentChar == '\033'){
					escaped = true;
					continue;
				}
				if(escaped && currentChar == ';'){ // hit additional color
					int unixColor = Integer.parseInt(number);
					int winColor = mapColor(unixColor);
					if(libraryPresent) setColor(mapColor(unixColor));
					number = "";
					continue;
				}
				if(escaped && currentChar == 'm'){ // hit end of sequence, set the color
					escaped = false;
					collectingNumber = false;
					int unixColor = Integer.parseInt(number);
					int winColor = mapColor(unixColor);
					if(libraryPresent) setColor(mapColor(unixColor));
					number = "";
					continue;
				}
				if(collectingNumber){ // append current character to number string
					number += currentChar;
					continue;
				}
				if(escaped && currentChar == '['){ // hit [, allow number collection
					collectingNumber = true; 
				} else {
					escaped = false; // sequence cancelled
				}
				if(!escaped) super.print(s.charAt(i));
			}
		}
	}
	
	private static boolean libraryPresent = true;
	
	private static int defaultColor = 0;
	
	//private int foreground = 0;
	//private int background = 0;
	// reference http://i.imgur.com/oRvcm5L.png
	// reference http://misc.flogisoft.com/_media/bash/colors_format/colors_and_formatting.sh.png
	
	private final static int mapColor(int color){ // convert unix console color to windows console color
		switch(color){ // # 38 = 256 colors switch
			case 0:  return defaultColor; // clear formatting
			case 39: return defaultColor;
			case 30: return 0;
			case 31: return 4;
			case 32: return 2;
			case 33: return 6;
			case 34: return 1; //darkblue
			case 35: return 5;
			case 36: return 3;
			case 37: return 7;
			case 90: return 8;
			case 91: return 12;
			case 92: return 10;
			case 93: return 14;
			case 94: return 9;
			case 95: return 13;
			case 96: return 11;
			case 97: return 15;
			default: return defaultColor;
		}
	}
	
	// windows colors
	// dark  - black 0000, blue 0001, green 0010, cyan 0011, red 0100, magenta 0101, yellow 0110, white 0111
	// light - black 1000, blue 1001, green 1010, cyan 1011, red 1100, magenta 1101, yellow 1110, white 1111
	
	public static void init(){
		boolean windows = System.getProperty("os.name").matches("[Ww]indows.*");
		if(windows){
			// fetch the current color and save it as default
			try {
				defaultColor = getColor();
			} catch(UnsatisfiedLinkError e){
				// silently disable all color changes if there is a link error
				libraryPresent = false;
			}
			// replace System.out with a wrapper stream that filters printing
			System.setOut(new FilteredStream(System.out));
			System.setErr(new FilteredStream(System.err));
		}
	}
	
	public static boolean isLibraryPresent(){
		return libraryPresent;
	}
	
	private static native void setColor(int c); // wraps SetConsoleTextAttribute(hCon, c);
	private static native int  getColor(); // wraps GetConsoleScreenBufferInfo(hCon, &consoleInfo); consoleInfo.wAttributes
}