@echo off
set jnilibs1="C:\Program Files\Java\jdk1.8.0_45\include"
set jnilibs2="C:\Program Files\Java\jdk1.8.0_45\include\win32"
set xinputlib="C:\Windows\System32\XInput9_1_0.dll"

g++ -o jxgamepads.dll -s -shared -m64 %xinputlib% -I%jnilibs1% -I%jnilibs2% JXGamepads.cpp 
rem g++ -o jxgamepads32.dll -s -shared -m32 %xinputlib% -I%jnilibs1% -I%jnilibs2% JXGamepads.cpp 
pause