package net.shygoo.plugin.debugger;

import net.shygoo.emuname.*;
import net.shygoo.plugin.debugger.*;

import javax.swing.*;

public class Debugger extends ENPlugin {
	public Debugger(){
		super("debugger", 1000);
	}
	public void init(Emuname emuname){
		MemoryWindow2 memWindow = new MemoryWindow2(emuname);
		CommandsWindow cmdWindow = new CommandsWindow(emuname);
		MemoryScanner memScanner = new MemoryScanner(emuname);
		ScriptWindow scriptWindow = new ScriptWindow(emuname);
		PPUWindow ppuWindow = new PPUWindow(emuname);
		
		//emuname.addPluginMenuItem("dbg_memory", "dbg", memWindow, "Memory");
		emuname.addPluginMenuItem("dbg_scanner", "dbg", memScanner, "Scanner...");
		//emuname.addPluginMenuItem("dbg_commands", "dbg", cmdWindow, "Commands");
		emuname.addPluginMenuItem("dbg_script", "dbg", scriptWindow, "Script engine...");
		//emuname.addPluginMenuItem("dbg_script", "dbg", ppuWindow, "PPU");
	}
}