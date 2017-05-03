echo Creating plugin jars...
cd _release
mkdir plugins
cd ../plugins/CRTEffects
call compile.bat
cd ../Debugger
call compile.bat
cd ../DefaultMenus
call compile.bat
cd ../BFMouse
call compile.bat
cd ../..