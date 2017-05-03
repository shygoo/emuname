package net.shygoo.misc;

import java.nio.charset.Charset;
import java.io.InputStream;
import javax.swing.ImageIcon;
import java.awt.Font;

// simple class just for grabbing stuff from the jar

public final class ResourceLoader {
	private final static String errMsg = "Error occurred while loading resource %s\n";
	public static byte[] loadAsByteArray(String path){
		try {
			InputStream resStream = ResourceLoader.class.getResourceAsStream(path);
			byte[] resData = new byte[resStream.available()];
			resStream.read(resData, 0, resData.length);
			return resData;
		} catch(Exception e){
			error(path);
			System.out.println(e);
			return new byte[0];
		}
	}
	public static Font loadAsFont(String path){
		try {
			return Font.createFont(Font.TRUETYPE_FONT, ResourceLoader.class.getResourceAsStream(path));
		} catch(Exception e){
			return null;
		}
	}
	public static String loadAsString(String path){
		try {
			byte[] resData = loadAsByteArray(path);
			return new String(resData, Charset.defaultCharset());
		} catch(Exception e){
			error(path);
			System.out.println(e);
			return "";
		}
	}
	public static ImageIcon loadAsImageIcon(String path){
		try {
			return new ImageIcon(ResourceLoader.class.getResource(path));
		} catch(Exception e) {
			error(path);
			return null;
		}
	}
	private static void error(String path){
		System.out.printf(errMsg, path);
	}
}