package net.shygoo.misc;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class KeyFilter extends KeyAdapter {
	public final static String HEX = "[A-Fa-f0-9]";
	public final static String NUM = "[0-9]";
	public final static String ANY = ".";
	private String regexFilter;
	//private int maxLength;
	public KeyFilter(String regexFilter){
		this.regexFilter = regexFilter;
	}
	public void keyTyped(KeyEvent e){
		String c = String.valueOf(e.getKeyChar());
		if(!c.matches(regexFilter)) e.consume();
	}
	public void setRegex(String regexFilter){
		this.regexFilter = regexFilter;
	}
}