@echo off
set pluginName=BFMouse
set packagePath=net.shygoo.plugin.
echo + %pluginName%

echo Manifest-Version: 1.0>> Manifest.txt
echo Main-Class: %packagePath%%pluginName%>> Manifest.txt

mkdir bin
javac -d bin -cp .;../../_release/emuname.jar %pluginName%.java
cd bin
jar -cmf ../Manifest.txt ../../../_release/plugins/%pluginName%.jar .
cd ..
rmdir /q /s bin

del Manifest.txt