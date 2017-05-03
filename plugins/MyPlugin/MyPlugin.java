package example.plugin;
import net.shygoo.emuname.*;
import java.awt.Graphics2D;
import java.awt.Color;

public class MyPlugin extends ENPlugin {
  public MyPlugin(){
    super("my_plugin", 0);
  }
  public void init(Emuname emuname){
    emuname.on("drawend", (e) -> {
      Graphics2D g2d = (Graphics2D) e;
	  g2d.setColor(Color.WHITE);
      g2d.drawString("Hello World!", 20, 20);
    });
  }
}