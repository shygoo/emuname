// ENPluginHook#invokeMethods(ENEvent e)
// ENMethod#run(ENEvent e)

// wrapper for AWT input events, and graphics2d

package net.shygoo.emuname;

import java.awt.event.*;
import java.awt.*;

public class ENEvent {

	public static final int
	EVENT_NULL     = 0,
	EVENT_MOUSE    = 1,
	EVENT_WHEEL    = 2,
	EVENT_KEY      = 3,
	EVENT_GRAPHICS = 4;
	
	private static String getTypeName(int type){
		switch(type){
			case EVENT_NULL:     return "EVENT_NULL";
			case EVENT_MOUSE:    return "EVENT_MOUSE";
			case EVENT_WHEEL:    return "EVENT_WHEEL";
			case EVENT_KEY:      return "EVENT_KEY";
			case EVENT_GRAPHICS: return "EVENT_GRAPHICS";
		}
		return null;
	}
	
	private class InvalidMethodException extends RuntimeException {
		public InvalidMethodException(String methodName, int requiredType){
			super(String.format("\n\033[91m%s is invalid for this event type (%s), required %s\033[0m", methodName, getTypeName(type), getTypeName(requiredType)));
			//System.exit(0);
		}
	}
	
	private int type;
	
	private boolean defaultPrevented = false;
	
	private MouseEvent      mouseEvent;
	 private double          xScale;
	 private double          yScale;
	private MouseWheelEvent mouseWheelEvent;
	private KeyEvent        keyEvent;
	private Graphics2D      graphics2d;
	
	public ENEvent(){
		type = EVENT_NULL;
	}
	
	public ENEvent(MouseEvent e, double xScale, double yScale){
		type = EVENT_MOUSE;
		this.xScale = xScale;
		this.yScale = yScale;
		mouseEvent = e;
	}
	
	public ENEvent(MouseWheelEvent e, double xScale, double yScale){
		type = EVENT_WHEEL;
		mouseWheelEvent = e;
		mouseEvent = e;
	}
	
	public ENEvent(KeyEvent e){
		type = EVENT_KEY;
		keyEvent = e;
	}
	
	public ENEvent(Graphics2D g){
		type = EVENT_GRAPHICS;
		graphics2d = g;
	}
	
	public int getType(){
		return type;
	}
	
	public String getTypeName(){
		return getTypeName(type);
	}
	
	public void preventDefault(){
		switch(type){
			case EVENT_NULL     : break;
			case EVENT_MOUSE    : mouseEvent.consume();
			case EVENT_WHEEL    : mouseWheelEvent.consume();
			case EVENT_KEY      : keyEvent.consume();
			case EVENT_GRAPHICS : break;
		}
		defaultPrevented = true;
	}
	
	public boolean isDefaultPrevented(){
		return defaultPrevented;
	}
	
	// EVENT_MOUSE
	
	public MouseEvent getMouseEvent(){
		if(type != EVENT_MOUSE && type != EVENT_WHEEL){
			throw new InvalidMethodException("getMouseEvent", EVENT_MOUSE);
		}
		return mouseEvent;
	}
	
	// EVENT_MOUSE & EVENT_WHEEL
	
	public int getX(){
		if(type != EVENT_MOUSE && type != EVENT_WHEEL){
			throw new InvalidMethodException("getX", EVENT_MOUSE);
		}
		return (int)(mouseEvent.getX() / xScale);
	}
	
	public int getY(){
		if(type != EVENT_MOUSE && type != EVENT_WHEEL){
			throw new InvalidMethodException("getY", EVENT_MOUSE);
		}
		return (int)(mouseEvent.getY() / yScale);
	}
	
	public int getAbsX(){
		if(type != EVENT_MOUSE && type != EVENT_WHEEL){
			throw new InvalidMethodException("getAbsX", EVENT_MOUSE);
		}
		return mouseEvent.getX();
	}
	
	public int getAbsY(){
		if(type != EVENT_MOUSE && type != EVENT_WHEEL){
			throw new InvalidMethodException("getAbsY", EVENT_MOUSE);
		}
		return mouseEvent.getY();
	}
	
	// EVENT_WHEEL
	
	public MouseWheelEvent getMouseWheelEvent(){
		if(type != EVENT_WHEEL){
			throw new InvalidMethodException("getMouseWheelEvent", EVENT_WHEEL);
		}
		return mouseWheelEvent;
	}
	
	public int getScrollAmount(){
		if(type != EVENT_WHEEL){
			throw new InvalidMethodException("getScrollAmount", EVENT_WHEEL);
		}
		return mouseWheelEvent.getScrollAmount();
	}
	
	// EVENT_GRAPHICS
	
	public Graphics2D getGraphics(){
		if(type != EVENT_GRAPHICS){
			throw new InvalidMethodException("getGraphics", EVENT_GRAPHICS);
		}
		return graphics2d;
	}
	
	// EVENT_KEY
	
	public KeyEvent getKeyEvent(){
		if(type != EVENT_KEY){
			throw new InvalidMethodException("getKeyEvent", EVENT_KEY);
		}
		return keyEvent;
	}
	
	public int getKeyCode(){
		if(type != EVENT_KEY){
			throw new InvalidMethodException("getKeyCode", EVENT_KEY);
		}
		return keyEvent.getKeyCode();
	}
	
	public char getKeyChar(){
		if(type != EVENT_KEY){
			throw new InvalidMethodException("getKeyChar", EVENT_KEY);
		}
		return keyEvent.getKeyChar();
	}
}