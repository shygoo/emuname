package net.shygoo.emuname;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

import java.io.File;

import net.shygoo.misc.ResourceLoader;
import net.shygoo.emuname.NES;
import net.shygoo.emuname.Emuname;

import net.shygoo.jconsolecolors.JConsoleColors;

public class Application {	

	private final static String buildInfoResPath  = "/res/_BUILDINFO";
	
	private static String libPath = "./natives/";
	
	private final static String libExtension = System.mapLibraryName(""); // should return .dll, .so, .dylib
	
	private ArrayList<String> messages;
	
	private String buildInfoText;
	private String buildName;
	private String buildDate;
	
	public Application() throws Exception {
	
		String arch = System.getProperty("os.arch");
		
		if(arch == "x86"){
			libPath = "./natives/x86/";
		}
		
		messages = new ArrayList<String>();
		
		loadNativeLibraries();
		JConsoleColors.init();
		
		if(!JConsoleColors.isLibraryPresent()){
			messages.add("[Notice: Console colors disabled]");
		}
		
		// Load the automated build information and credits text
		
		buildInfoText = ResourceLoader.loadAsString(buildInfoResPath);
		buildName     = buildInfoText.split("\r\n")[0];
		buildDate     = buildInfoText.split("\r\n")[1];
		
		System.out.println("\n \033[96m" + buildName + "\033[0m\n Built " + buildDate + "\n");
		
		for(int i = 0; i < messages.size(); i++){
			System.out.println(messages.get(i));
		}
		
		//SwingUtilities.invokeAndWait(() -> {
		Emuname mainwindow = new Emuname(buildName);
		//});
	}
	
	public void loadNativeLibraries(){
	
		String[] libFiles = new File(libPath).list();
		
		for(int i = 0; i < libFiles.length; i++){
			String libName = libFiles[i].substring(0, libFiles[i].length() - libExtension.length());
			try {
				if(libFiles[i].matches(".+\\" + libExtension)){
					System.loadLibrary(libPath + libName);
					messages.add("[\033[92mNative\033[0m: \033[93m" + libName + "\033[0m (\033[90m" + libPath + libFiles[i] + "\033[0m)]");
				}
			} catch(UnsatisfiedLinkError e){
				messages.add("[\033[91mError\033[0m: \033[91m" + e.getMessage()  + "\033[0m]");
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		new Application();
	}
}