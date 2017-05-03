package net.shygoo.plugin;

import net.shygoo.emuname.*;

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import java.util.Random;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;

public class CRTEffects extends ENPlugin {
	
	public CRTEffects(){
		super("crt_effects", 1);
	}
	
	private final static int SCR_HEIGHT = 240;
	private final static int SCR_WIDTH  = 256;
	private final static int SCR_SIZE = SCR_HEIGHT * SCR_WIDTH;
	
	private boolean noiseEnabled = false;
	private boolean scanlinesEnabled = false;
	private boolean smoothingEnabled = false;
	
	private NES nes;
	private Emuname emuname;
	
	private final Random rand = new Random();
	
	private void applyScanlines(){
		for(int i = 0; i < SCR_HEIGHT; i += 2){
			for(int j = 0; j < SCR_WIDTH; j++){
				nes.ppu.display[i*SCR_WIDTH + j] &= 0xC0C0C0;
			}
		}
	}
	
	private void applyNoise(){
		for(int i = 0; i < SCR_SIZE; i++){
			nes.ppu.display[i] = (nes.ppu.display[i] & 0xF0F0F0) | ((int)(rand.nextInt()*0xFFFFFF) & 0x0F0F0F);
			//nes.ppu.display[i] = ~nes.ppu.display[i];
		}
	}
	
	public void init(Emuname emuname){
		this.emuname = emuname;
		nes = emuname.nes;
		
		JCheckBoxMenuItem noiseCheck = new JCheckBoxMenuItem(new AbstractAction("Noise"){
			public void actionPerformed(ActionEvent e){
				noiseEnabled = !noiseEnabled;
			}
		});
		
		JCheckBoxMenuItem scanlinesCheck = new JCheckBoxMenuItem(new AbstractAction("Scanlines"){
			public void actionPerformed(ActionEvent e){
				scanlinesEnabled = !scanlinesEnabled;
			}
		});
		
		JCheckBoxMenuItem smoothingCheck = new JCheckBoxMenuItem(new AbstractAction("Smoothing"){
			public void actionPerformed(ActionEvent e){
				smoothingEnabled = !smoothingEnabled;
				emuname.getScreen().setImageSmoothingEnabled(smoothingEnabled);
			}
		});
		
		emuname.addPluginMenuItem("vid_noise", "vid", noiseCheck);
		emuname.addPluginMenuItem("vid_scanlines", "vid", scanlinesCheck);
		emuname.addPluginMenuItem("vid_smoothing", "vid", smoothingCheck);
		
		emuname.on("drawstart", (e) -> {
			// OR the lower 4 bits of every channel with random data
			if(noiseEnabled){
				//System.out.println("noise");
				applyNoise();
			}
			if(scanlinesEnabled) applyScanlines();
		});
	}
}