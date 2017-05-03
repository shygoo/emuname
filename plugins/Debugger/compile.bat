@echo off
set pluginName=Debugger
set packagePath=net.shygoo.plugin.debugger/
echo + %pluginName%

echo Manifest-Version: 1.0>> Manifest.txt
echo Main-Class: %packagePath%%pluginName%>> Manifest.txt

mkdir bin
javac -Xlint:unchecked -d bin -cp .;../../_release/emuname.jar @sources.txt
cd bin
jar -cmf ../Manifest.txt ../../../_release/plugins/%pluginName%.jar .
cd ..
rmdir /q /s bin

del Manifest.txt