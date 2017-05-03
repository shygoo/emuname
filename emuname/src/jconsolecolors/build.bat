set jnilibs1="C:\Program Files\Java\jdk1.8.0_65\include"
set jnilibs2="C:\Program Files\Java\jdk1.8.0_65\include\win32"

gcc -s -I%jnilibs1% -I%jnilibs2% -shared -m64 jconsolecolors.c -o jconsolecolors.dll
pause