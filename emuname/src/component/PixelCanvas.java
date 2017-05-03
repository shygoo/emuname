package net.shygoo.component;

import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.RenderingHints;
//import java.awt.RenderingHints.Key;

// JPanel component that displays provided RGB888 rgbArray whenever repaint() is called

public class PixelCanvas extends JPanel {
	private BufferedImage image;
	private int width;
	private int height;
	private int displayWidth;
	private int displayHeight;
	private Rectangle sizeRect;
	private double scale = 1;
	private int[] rgbArray;
	private boolean defaultDrawingEnabled = true;
	
	private Graphics2D activeG2d;
	
	private Object g2dInterpolation = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
	
	public PixelCanvas(int width, int height, int[] rgbArray){
		this.width = displayWidth = width;
		this.height = displayHeight = height;
		this.rgbArray = rgbArray;
		this.image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		this.setSize(width, height);
	}
	
	public void setImageSmoothingEnabled(boolean setting){
		g2dInterpolation = setting ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
	}
	
	public void scale(double scale){
		this.scale = scale;
		setDisplaySize((int)(width*scale), (int)(height*scale));
	}
	
	public int getDisplayWidth(){
		return displayWidth;
	}
	
	public int getDisplayHeight(){
		return displayHeight;
	}
	
	public double getWidthFactor(){
		return (double)displayWidth / (double)width;
	}
	
	public double getHeightFactor(){
		return (double)displayHeight / (double)height;
	}
	
	public void setDisplaySize(int displayWidth, int displayHeight){
		this.displayWidth = displayWidth;
		this.displayHeight = displayHeight;
		this.setSize(displayWidth, displayHeight);
		this.setPreferredSize(new Dimension(displayWidth, displayHeight));
	}
	
	public void setRgbArray(int[] rgbArray){
		this.rgbArray = rgbArray;
	}
	
	@Override
	public void paint(Graphics g){
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, g2dInterpolation);
		activeG2d = g2d;
		preframe(g2d);
		if(defaultDrawingEnabled){
			image.setRGB(0, 0, width, height, rgbArray, 0, width); // CLEARS THE CANVAS startX, startY, width, height, rgbArray, offset, scansize (scanline stride)
			g2d.drawImage(image, 0, 0, displayWidth, displayHeight, null);
		}
		postframe(g2d);
	}
	
	// methods for plugin/extension hooking:
	
	public Graphics2D getActiveG2d(){
		// returns the Graphics2D object passed to paint()
		// will return a dead object if it has been fetched at the incorrect time
		return activeG2d;
	}
	
	public void preframe(Graphics2D g2d){
		// can perform edits to the rgbarray here
		return;
	}
	
	public void postframe(Graphics2D g2d){
		// use getActiveG2d() here to get the active graphics2D object at the correct time
		return;
	}
	
	public void setDefaultDrawingEnabled(boolean setting){
		defaultDrawingEnabled = setting;
	}
	
	public boolean getDefaultDrawingEnabled(){
		return defaultDrawingEnabled;
	}
}