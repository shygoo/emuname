@echo off

echo Refreshing release directory...
rmdir /s /q _release
mkdir _release
cd _release
rem mkdir lib
cd ..

echo emuname a%DATE:~4,2%.%DATE:~7,2%> emuname/res/_BUILDINFO
echo %DATE%>> emuname/res/_BUILDINFO

rem echo Generating javadocs...
rem cd emuname
rem rmdir /s /q "../_doc"
rem mkdir "../_doc"
rem javadoc -quiet -d ../_doc @sources.txt

cd emuname

echo Creating emuname jar...
mkdir bin
javac -g:none -Xlint:deprecation -Xlint:unchecked -d bin @sources.txt
cd bin
jar -cmf ../src/Manifest.txt ../../_release/emuname.jar  . ../res
cd ..

rmdir /s /q bin
cd ..

call build-plugins.bat

echo Compiling wrapper exe...
cd winwrapper
windres wrapper.rc -O coff -o wrapper.res
gcc -s wrapper.c wrapper.res -o ../_release/shortcut.exe
del wrapper.res
cd ..

echo Copying default release files...
xcopy /s /q releasefiles _release

pause