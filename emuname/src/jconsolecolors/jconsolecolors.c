#include "net_shygoo_jconsolecolors_JConsoleColors.h"
#include <windows.h>

JNIEXPORT void JNICALL Java_net_shygoo_jconsolecolors_JConsoleColors_setColor(JNIEnv * jenv, jclass jcl, jint c){ // set windows console color
	HANDLE hCon = GetStdHandle(STD_OUTPUT_HANDLE);
	SetConsoleTextAttribute(hCon, c);
}

JNIEXPORT jint JNICALL Java_net_shygoo_jconsolecolors_JConsoleColors_getColor(JNIEnv * jenv, jclass jcl){ // return windows console color
	HANDLE hCon = GetStdHandle(STD_OUTPUT_HANDLE);
	CONSOLE_SCREEN_BUFFER_INFO consoleInfo;
	GetConsoleScreenBufferInfo(hCon, &consoleInfo);
	return consoleInfo.wAttributes;
}