#define WIN32_LEAN_AND_MEAN

#include <windows.h>
#include <Xinput.h>

#include <stdio.h>

#define MAX_GAMEPADS   4
#define MAX_BUTTONS   16
#define MAX_AXES       6

#define GAMEPAD_0      0
#define GAMEPAD_1      1
#define GAMEPAD_2      2
#define GAMEPAD_3      3

#define AXIS_LTRIGGER  0
#define AXIS_RTRIGGER  1
#define AXIS_LSTICK_X  2
#define AXIS_LSTICK_Y  3
#define AXIS_RSTICK_X  4
#define AXIS_RSTICK_Y  5

#define S16_AXIS(value)(((int)value + MINSHORT) / (double)65535 * 2 - 1) // analog stick range conversion; -32768:32767 = -1.0:1.0
#define U8_AXIS(value)((double)value / MAXBYTE) // bumper range conversion; 0:255 = 0.0:1.0

class XGamepads {
	public:
		static bool isConnected(int gamepadNum) { // returns true if controller is connected
			XINPUT_STATE state;
			return XInputGetState(gamepadNum, &state) == ERROR_SUCCESS;
		}
		static BYTE* getButtons(int gamepadNum) { // returns button down states of the controller, 0 = up, 1 = down
			XINPUT_STATE state;
			XInputGetState(gamepadNum % MAX_GAMEPADS, &state);
			static BYTE buttons[MAX_BUTTONS];
			for (int i = 0; i < MAX_BUTTONS; i++) {
				buttons[i] = (state.Gamepad.wButtons & (1 << i)) >> i;
			}
			return buttons;
		}
		static double* getAxes(int gamepadNum) { // returns axes states of the controller, -1.0 to 1.0 for each
			XINPUT_STATE state;
			XInputGetState(gamepadNum % MAX_GAMEPADS, &state);
			XINPUT_GAMEPAD gp = state.Gamepad;
			static double axes[MAX_AXES];
			axes[AXIS_LTRIGGER] = U8_AXIS(gp.bLeftTrigger);
			axes[AXIS_RTRIGGER] = U8_AXIS(gp.bRightTrigger);
			axes[AXIS_LSTICK_X] = S16_AXIS(gp.sThumbLX);
			axes[AXIS_LSTICK_Y] = S16_AXIS(gp.sThumbLY);
			axes[AXIS_RSTICK_X] = S16_AXIS(gp.sThumbRX);
			axes[AXIS_RSTICK_Y] = S16_AXIS(gp.sThumbRY);
			return axes;
		}
		static void setVibration(int gamepadNum, double left, double right){ // vibrates controller, max value of 1.0
			static XINPUT_VIBRATION vibration;
			vibration.wLeftMotorSpeed = (WORD)(MAXWORD * left) & MAXWORD;
			vibration.wRightMotorSpeed = (WORD)(MAXWORD * right) & MAXWORD;
			XInputSetState(gamepadNum % MAX_GAMEPADS, &vibration);
		}
};
