package net.shygoo.jxgamepads;

public class JXGamepads {
	
	public static final int
	MAX_GAMEPADS  =  4,
	MAX_BUTTONS   = 16,
	MAX_AXES      =  6,
	GAMEPAD_0     =  0,
	GAMEPAD_1     =  1,
	GAMEPAD_2     =  2,
	GAMEPAD_3     =  3,
	AXIS_LTRIGGER =  0,
	AXIS_RTRIGGER =  1,
	AXIS_LSTICK_X =  2,
	AXIS_LSTICK_Y =  3,
	AXIS_RSTICK_X =  4,
	AXIS_RSTICK_Y =  5;
	
	//static {
	//	System.loadLibrary("xgamepads");
	//}
	
	native static public boolean xgIsConnected(int gamepadNum);
	native static public double[] xgGetAxes(int gamepadNum);
	native static public byte[] xgGetButtons(int gamepadNum);
	native static public void xgSetVibration(int gamepadNum, double left, double right);
	
}