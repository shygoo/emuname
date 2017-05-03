package net.shygoo.emuname;

import net.shygoo.emuname.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.script.*;
import java.nio.charset.Charset;
import java.net.URI;
import java.io.InputStream;

import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import net.shygoo.misc.ResourceLoader;

public class ScriptWindow extends JFrame {
	private Emuname  emuname;
	private JScrollPane scrollPane;
	private JTextArea   scriptTextArea;
	private JMenuBar    menuBar;
	private JMenu       scriptMenu;
	private JMenuItem   menuItemRunNow;
	private JMenuItem   menuItemResetEngine;
	private JMenuItem   menuItemClearFrameEvents;
	
	private NashornScriptEngineFactory nsef;
	private ScriptEngine engine;
	
	private boolean safeMode = false;
	
	private String engineSetupScript;
	private static final String engineSetupScriptPath = "/res/EngineSetup.js";
		
	private static final String[] unsafeAttributes = {
		"Java",
		"JavaImporter",
		"Packages",
		"java",
		"javax",
		"javafx",
		"org",
		"com",
		"net",
		"edu",
		"load",
		"loadWithNewGlobal",
		"exit",
		"quit"
	};
	
	private static final String[] unsafeClasses = {
		"java.lang.reflect",
		"java.lang.invoke",
	};
	
	private class SafeModeFilter implements ClassFilter {
		public boolean exposeToScripts(String requestedClass){
			for(int i = 0; i < unsafeClasses.length; i++){
				if(requestedClass.equals(unsafeClasses[i])) return false;
			}
			return true;
		}
	}
	
	public ScriptWindow(Emuname emuname){
		super("Script engine");
		this.emuname = emuname;
		
		nsef = new NashornScriptEngineFactory();
		
		setupEngine();
		
		menuBar    = new JMenuBar(); // window menu bar
		scriptMenu = new JMenu("Script");
		menuItemRunNow = new JMenuItem(new AbstractAction("Run now"){
			public void actionPerformed(ActionEvent e){
				if(e.getID() == 1001){
					try { 
						runScript();
					} catch(Exception err){
						System.out.println(err.toString());
					}
				}
			}
		});
		menuItemResetEngine = new JMenuItem(new AbstractAction("Reset engine"){
			public void actionPerformed(ActionEvent e){
				if(e.getID() == 1001){
					setupEngine();
					System.out.println("[Script engine reset]");
				}
			}
		});
		menuItemClearFrameEvents = new JMenuItem(new AbstractAction("Clear frame events"){
			public void actionPerformed(ActionEvent e){
				if(e.getID() == 1001){
					clearFrameEvents();
					System.out.println("[Frame events cleared]");
				}
			}
		});
		
		setJMenuBar(menuBar);
		menuBar.add(scriptMenu);
		scriptMenu.add(menuItemRunNow);
		scriptMenu.add(menuItemClearFrameEvents);
		scriptMenu.add(menuItemResetEngine);
		
		scriptTextArea = new JTextArea(40, 80);
		scriptTextArea.setFont(new Font("Consolas", Font.PLAIN, 12));
		scriptTextArea.setTabSize(4);
		scriptTextArea.setBackground(new Color(0x111120));
		scriptTextArea.setForeground(new Color(0x5D7AA2));
		scriptTextArea.setSelectedTextColor(new Color(0xEEEEDF));
		scriptTextArea.setSelectionColor(new Color(0xC2A57D));
		scriptTextArea.setCaretColor(Color.WHITE);
		
		scrollPane = new JScrollPane(scriptTextArea);
		
		add(scrollPane, BorderLayout.NORTH);
		//scrollPane.add(scriptTextArea);
		pack();
		
		emuname.on("drawstart", (e) -> {
			try{
				doFrameEvents();
			} catch(Exception ex){
				System.out.println(ex);
			}
		});
	}
	
	public void runScript() throws Exception {
		engine.eval(scriptTextArea.getText());
	}
	
	public void doFrameEvents() throws Exception {
		((Invocable)engine).invokeMethod(engine.get("frameEvents"), "run");
	}
	
	public void clearFrameEvents() {
		try {
			((Invocable)engine).invokeMethod(engine.get("frameEvents"), "clear");
		} catch(Exception e){
			System.out.println(e.toString());
		}
	}
	
	private void setupEngine() {
		if(safeMode){
			//If safeMode is enabled, construct the engine with a ClassFilter to disable reflection
			// and remove all potentially unsafe global attributes
			engine = nsef.getScriptEngine(new SafeModeFilter());
			ScriptContext ctx = engine.getContext();
			int globalScope = ctx.getScopes().get(0);
			for(int i = 0; i < unsafeAttributes.length; i++){
				ctx.removeAttribute(unsafeAttributes[i], globalScope);
			}
		} else {
			engine = nsef.getScriptEngine();
		}
		
		//Add references to emuname and nes objects
		engine.put("emuname", emuname);
		engine.put("nes", emuname.nes);
		
		//Load the environment setup script
		// (adds plugin and frameEvent management objects)
		
		engineSetupScript = ResourceLoader.loadAsString(engineSetupScriptPath);
		
		// execute the environment setup script
		try {
			engine.eval(engineSetupScript); 
		} catch(Exception e){
			System.out.println("An error occurred while setting up the script engine environment");
			System.out.println(e.toString());
		}
	}
}