#define _WIN32_WINNT 0x0500

#include <windows.h>
#include <stdio.h>
#include <string.h>

int WINAPI WinMain (HINSTANCE hInstance, HINSTANCE hPrevInstance, PSTR szCmdParam, int iCmdShow){

	if(strlen(szCmdParam) == 0){
		printf("Command missing\n\nTry: shortcut.exe java -jar emuname.jar\n");
		system("pause");
		return 0;
	}
	
	HWND consoleWindow = GetConsoleWindow();
	ShowWindow(consoleWindow, SW_MINIMIZE);
	ShowWindow(consoleWindow, SW_HIDE);
	
	system(szCmdParam);
	return 0;
}