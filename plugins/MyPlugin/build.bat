@echo off
set /p emunamejar="Enter path to emuname.jar: "
mkdir bin
javac -d bin -cp .;%emunamejar% MyPlugin.java
cd bin
jar -cmf ../Manifest.txt ../MyPlugin.jar .
cd ..
rmdir /q /s bin
pause 