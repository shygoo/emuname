package net.shygoo.plugin;

import net.shygoo.emuname.*;

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import java.util.Random;

public class DefaultMenus extends ENPlugin {

	public DefaultMenus(){
		super("default_menus", 0);
	}
	
	public void init(Emuname emuname){
		emuname.addPluginMenuItem("dbg",   new JMenu("Debugging"));
		emuname.addPluginMenuItem("vid", new JMenu("Video"));
		emuname.addPluginMenuItem("games", new JMenu("Games"));
	}
}