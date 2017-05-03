package net.shygoo.plugin;

import net.shygoo.emuname.*;

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

/*

003C phase
0041 lives

0088 player 1 state

0490-0491 lightning balls x
04A4-04A5 lightning balls y

0567-056F bonus stage balloons x
057B-0584 bonus stage balloons y

05CD bonus stage collected balloons

*/

public class BFMouse extends ENPlugin {
	
	public BFMouse(){
		super("balloon_fight_mouse", 1000);
	}
	
	private NES nes;
	
	private boolean bfMouseEnabled = false;
	
	private final static int[] entitiesX = {
		0x91, 0x92, // players
		0x93, 0x94, 0x95, 0x96, 0x97, 0x98, // enemies
		0x490, 0x491, // lightning
		0x567, 0x568, 0x569, 0x56A, 0x56B, 0x56C, 0x56D, 0x56E, 0x56F, 0x570 // balloons
	};
	
	private final static int[] entitiesY = {
		0x9A, 0x9B, // players
		0x9C, 0x9D, 0x9E, 0x9F, 0xA0, 0xA1, // enemies
		0x4A4, 0x4A5, // lighnting
		0x57B, 0x57C, 0x57D, 0x57E, 0x57F, 0x580, 0x581, 0x582, 0x583, 0x584 // balloons
	};
	
	private int selectedEntity = 0;
	private boolean noSelection = true;
	
	private boolean mouseDown = false;
	
	private int mouseX = 0;
	private int mouseY = 0;
	
	private int getEntityX(int entity){
		return nes.mem.read(entitiesX[entity]);
	}
	
	private int getEntityY(int entity){
		return nes.mem.read(entitiesY[entity]);
	}
	
	private void setSelectedEntityPos(int x, int y){
		nes.mem.write(entitiesX[selectedEntity], x);
		nes.mem.write(entitiesY[selectedEntity], y);
	}
	
	private void selectNearestEntity(int x, int y){
		noSelection = true;
		int bestDist = 32;
		int entity = 0;
		for(int i = 0; i < entitiesX.length; i++){
			int dist = Math.abs(x - getEntityX(i)) + Math.abs(y - getEntityY(i));
			if(dist < bestDist){
				noSelection = false;
				entity = i;
				bestDist = dist;
			}
		}
		selectedEntity = entity;
	}
	
	public void init(Emuname emuname){
		
		nes = emuname.nes;
		
		emuname.addPluginMenuItem("games_balloon_fight_mouse", "games", new JCheckBoxMenuItem(new AbstractAction("Balloon Fight mouse hack"){
			public void actionPerformed(ActionEvent e){
				bfMouseEnabled = !bfMouseEnabled;
			}
		}));
		
		emuname.on("mousemove", (e) -> {
			mouseX = e.getX();
			mouseY = e.getY();
		});
		
		emuname.on("mousedown", (e) -> {
			//System.out.printf("%d %d\n", e.getKeyCode(), e.getY());
			mouseX = e.getX();
			mouseY = e.getY();
			selectNearestEntity(e.getX(), e.getY());
			mouseDown = true;
		});
		
		emuname.on("mouseup", (e) -> {
			mouseDown = false;
		});
		
		emuname.on("drawstart", (e) -> {
			if(bfMouseEnabled && mouseDown && !noSelection){
				setSelectedEntityPos(mouseX, mouseY);
			}
		});
	}
}