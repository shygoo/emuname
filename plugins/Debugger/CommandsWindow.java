package net.shygoo.emuname;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.awt.Dimension;

import net.shygoo.emuname.NES;
import net.shygoo.misc.Disassembler6502;

public class CommandsWindow extends JFrame {
	
	private JPanel      disPanel;
	private JLabel[]    addressLabels;
	private JLabel[]    disLabels;
	private JTextField  offsetTextField;
	
	private NES nes;
	private Disassembler6502 dis;
	
	private Font monoFont = new Font("Consolas", 0, 11);
	
	public CommandsWindow(Emuname emuname){
		super("CPU Debugger");
		this.nes = emuname.nes;
		this.dis = new Disassembler6502(nes.cpu.mem);
		
		setLayout(null);
		setSize(400, 400);
		
		Insets insets = getInsets();
		
		offsetTextField = new JTextField();
			offsetTextField.setBounds(5+insets.left + 155, 5+insets.top, 60, 20);
		
		disPanel = new JPanel();
			disPanel.setOpaque(true);
			disPanel.setBackground(Color.white);
			disPanel.setBounds(5+insets.left, 5+insets.top, 150, 48*10 + 10);
			disPanel.setLayout(null);
			disPanel.setBorder(offsetTextField.getBorder()); // inherit border from the PC text box
		
		addressLabels = new JLabel[48];
		disLabels = new JLabel[48];
		
		add(offsetTextField);
		add(disPanel);
		
		dis.setOffset(0x8000);
		
		Color addressBgColor = new Color(0xEEEEEE);
		Color addressFgColor = new Color(0x888888);
		
		for(int i = 0; i < addressLabels.length; i++){
			addressLabels[i] = new JLabel("");
				addressLabels[i].setBounds(5+insets.left, 5+insets.top + (i*10), 30, 11);
				addressLabels[i].setOpaque(true);
				addressLabels[i].setBackground(addressBgColor);
				addressLabels[i].setForeground(addressFgColor);
				addressLabels[i].setFont(monoFont);
			disLabels[i] = new JLabel("");
				disLabels[i].setBounds(5+insets.left + 30, 5+insets.top + (i*10), 120, 11);
				disLabels[i].setFont(monoFont);
			disPanel.add(addressLabels[i]);
			disPanel.add(disLabels[i]);
		}
		
		setResizable(false);
		setSize(300,550);
		
		//pack();
		
		refresh();
		
		t1.start();
		
		//setVisible(true);
	}
	
	private Timer t1 = new Timer(1000, new ActionListener(){
		public void actionPerformed(ActionEvent e){
			refresh();
		}
	});
	
	private void refresh(){
		dis.setOffset(0x8000);
		for(int i = 0; i < addressLabels.length; i++){
			addressLabels[i].setText(String.format("%04X", dis.getOffset()));
			disLabels[i].setText(dis.next());
		}
	}
}