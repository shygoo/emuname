emuname readme
2016 Mar 09
shygoo

------------------------------------------

  Included files

   emuname.jar ........ The emulator
   emuname.htm ........ Documentation
   emuname.ini ........ emuname configuration
   wrapper.exe ........ Windows executable wrapper to provide an icon for shortcuts, forwards command line argument to system()
   natives/ ........... Native libraries directory, emuname loads libraries automatically on startup
    jconsolecolors.* ... Provides colored console output for Windows
    jxgamepads.* ....... Provides controller support (Xinput only at the moment)
    x86/ ............... 32-bit libraries
     *.* ................ 32-bit versions of each library
   plugins/ ........... The plugins directory
    Debugger.jar ....... Provides all of the debugging windows
    DebugHUD.jar ....... (random testing)
    Noise.jar .......... Example plugin that produces CRT-like noise
    Scanlines.jar ...... Example plugin that produces CRT-like scanlines

------------------------------------------
 
  Default button mappings

   A ....... m
   B ....... n
   SELECT .. q
   START ... e
   UP ...... w
   DOWN .... s
   LEFT .... a
   RIGHT ... d

------------------------------------------

  emuname is thanks to:

   http://nesdev.com/NESDoc.pdf
   http://54.148.172.211/nes/6502%20Reference.htm (originally @ obelisk.demon.co.uk)
   http://tuxnes.sourceforge.net/mappers-0.80.txt
   http://blargg.8bitalley.com/nes-tests/instr_test-v4.zip
   http://nickmass.com/images/nestest.nes
   http://nickmass.com/images/nestest.log
   http://wiki.nesdev.com/w/index.php/INES