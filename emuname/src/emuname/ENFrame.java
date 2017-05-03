package net.shygoo.emuname;

import net.shygoo.misc.ResourceLoader;

import java.util.ArrayList;
import java.awt.event.*;
import java.awt.Image;
import java.awt.Point;
import javax.swing.JFrame;
import javax.swing.ImageIcon;

public class ENFrame extends JFrame {
	//private static ENFrame mainInstance;
	
	private final JFrame owner;
	private final boolean modal;
	
	private final ImageIcon // resource images from the jar
	resAppIcon16 = ResourceLoader.loadAsImageIcon("/res/icon16.png"),
	resAppIcon32 = ResourceLoader.loadAsImageIcon("/res/icon32.png");
	
	public ENFrame(String title, JFrame owner, boolean modal){
		super(title);
		ArrayList<Image> appIcons = new ArrayList<Image>(); // application icon set
		appIcons.add(resAppIcon16.getImage());
		appIcons.add(resAppIcon32.getImage());
		setIconImages(appIcons);
		this.owner = owner;
		this.modal = modal;
	}
	
	public ENFrame(String title, JFrame owner){
		this(title, owner, false);
	}
	
	public ENFrame(String title){
		this(title, null);
	}
	
	private void showFromOwner(){
		setLocation(owner.getLocation());
		super.setVisible(true);
	}
	
	private void showModal(){
		showFromOwner();
		JFrame self = this;
		addWindowListener(new WindowAdapter(){
			public void windowOpened(WindowEvent e){
				owner.setEnabled(false);
			}
			public void windowActivated(WindowEvent e){
				owner.setEnabled(false);
			}			
			public void windowClosing(WindowEvent e){
				owner.setEnabled(true);
				self.removeWindowListener(this);
			}
			public void windowClosed(WindowEvent e){
				owner.setEnabled(true);
				self.removeWindowListener(this);
			}
		});
		owner.addWindowListener(new WindowAdapter(){
			public void windowActivated(WindowEvent e){
				if(self.isShowing()){
					self.toFront();
				} else {
					owner.removeWindowListener(this);
					owner.toFront();
				}
			}
		});
	}
	
	public void setVisible(boolean visible){
		if(!isVisible() && visible){ // frame is currently hidden, to be shown
			if(modal){
				showModal();
			} else if(owner != null){
				showFromOwner();
			} else {
				super.setVisible(true);
			}
			return;
		}
		super.setVisible(visible);
	}
}