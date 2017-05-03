package net.shygoo.emuname;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.awt.Dimension;

import net.shygoo.component.PixelCanvas;
import net.shygoo.emuname.NES;
import net.shygoo.emuname.mapper.*;

public class PPUWindow extends JFrame {
	
	private NES nes;
	
	private int[] chrMemRGB;
	private PixelCanvas chrMemView;
	
	public PPUWindow(Emuname emuname){
		super("PPU");
		this.nes = emuname.nes;
		
		setLayout(null);
		setSize(512, 128);
		Insets insets = getInsets();
		
		chrMemRGB = new int[0x8000];
		
		JPanel mainPanel = new JPanel();
			mainPanel.setLayout(null);
			mainPanel.setSize(512, 128);
		
		JLabel chrMemLabel = new JLabel("CHR-MEM:");
			chrMemLabel.setBounds(insets.left + 10, insets.top+10, 80, 12);
		
		chrMemView = new PixelCanvas(512, 64, chrMemRGB);
			//chrMemView.setPreferredSize(new Dimension(512, 64));
			chrMemView.scale(1);
			chrMemView.setLocation(insets.left, insets.top+30);
		
		add(mainPanel);
		mainPanel.add(chrMemLabel);
		mainPanel.add(chrMemView);
		
		setResizable(false);
		//pack();
		
		Timer t = new Timer(500, new ActionListener(){
			public void actionPerformed(ActionEvent e){
				//System.out.println("tick");
				for(int i = 0; i < 512; i++){
					int x = (i % 64) * 8;
					int y = (i / 64) * 8;
					for(int j = 0; j < 8; j++){ // j
						int map0 = nes.mapper.chrMem.read(i*16 + j);
						int map1 = nes.mapper.chrMem.read(i*16 + j + 8);
						for(int k = 0; k < 8; k++){
							int color = ((map0 & (1 << k)) >> k) + ((map1 & (1 << k)) >> k) * 2;
							int absX = x + (7-k);
							int absY = y + j;
							int displayColor = 0;
							switch(color){
								case 1: displayColor = 0xFF0000; break;
								case 2: displayColor = 0x00FF00; break;
								case 3: displayColor = 0x0000FF; break;
							}
							chrMemRGB[absY*512 + absX] = displayColor;
						}
					}
				}
				chrMemView.repaint();
			}
		});
		
		addComponentListener(new ComponentAdapter() {
			public void componentHidden(ComponentEvent e) {
				t.stop();
			}
			public void componentShown(ComponentEvent e) {
				t.start();
			}
		});
		
		//t.start();
		
		//chrMemView.repaint();
	}
}