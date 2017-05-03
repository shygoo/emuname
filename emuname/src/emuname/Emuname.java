package net.shygoo.emuname;

import net.shygoo.emuname.*;
import net.shygoo.jxgamepads.*;
import net.shygoo.component.*;
import net.shygoo.misc.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.jar.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.imageio.*;

public class Emuname extends ENFrame {
	private Emuname self;
	public final NES nes;
	private final Config config;
	
	private Thread emuThread; // calls nes.runUntilVBLank
	
	//private int mouseX;
	//private int mouseY;
	
	private boolean
	emuRunning = false, // emulation will pause when false
	emuStopped = true,  // emulation will terminate when true
	limitFPS = true,
	cpuOptimizationEnabled = false,
	useGamepad = false, // false on every keyboard event, true when any gamepad button is down // TODO refactor for multiple controllers
	showHotbar = false,
	fullScreen = false;
	
	private int emuThreadSleepTime = 10;
	
	// Plugin system
	private final ArrayList<ENPlugin> plugins;
	private final HashMap<String, ENPluginHook> hooks;
	private final HashMap<String, JMenuItem> pluginMenuItems; // access to plugin menu items by id
	
	private final ENPluginHook
	loadEvents,
	frameEvents,
	postFrameEvents,
	preStepEvents,
	postStepEvents,
	mouseClickedEvents,
	mouseDraggedEvents,
	mouseEnteredEvents,
	mouseExitedEvents,
	mouseMovedEvents,
	mousePressedEvents,
	mouseReleasedEvents,
	mouseWheelMovedEvents,
	keyPressedEvents,
	keyReleasedEvents;
	
	// Components
	
	private ImageIcon // resource images from the jar
	resPauseIcon          = ResourceLoader.loadAsImageIcon("/res/pause.png"),
	resPauseIcon_hover    = ResourceLoader.loadAsImageIcon("/res/pause_hover.png"),
	resPlayIcon           = ResourceLoader.loadAsImageIcon("/res/play.png"),
	resPlayIcon_hover     = ResourceLoader.loadAsImageIcon("/res/play_hover.png"),
	resPlayFastIcon       = ResourceLoader.loadAsImageIcon("/res/play_fast.png"),
	resPlayFastIcon_hover = ResourceLoader.loadAsImageIcon("/res/play_fast_hover.png"),
	resStopIcon           = ResourceLoader.loadAsImageIcon("/res/stop.png"),
	resStopIcon_hover     = ResourceLoader.loadAsImageIcon("/res/stop_hover.png"),
	resResetIcon          = ResourceLoader.loadAsImageIcon("/res/reset.png"),
	resResetIcon_hover    = ResourceLoader.loadAsImageIcon("/res/reset_hover.png");
	
	private ToolButton
	toolBtnPause,
	toolBtnPlay,
	toolBtnPlayFast,
	toolBtnStop,
	toolBtnReset;
	
	private JMenuBar menuBar;
	private JMenu pluginMenu;
	private JMenuItem endMenuItem;
	private JPanel fileSelect;
	private PixelCanvas screen;
	private JPanel hotbar;
	private JPanel fullScreenPanel;
	private JFrame fullScreenFrame;
	private JPanel mainPanel;
	
	public Emuname(String title) {
		super(title);
		self = this;
		
		// Define NES emulator object
		nes = new NESWithHooks();
		
		// Define config object for emuname.ini
		config = new Config("./emuname.ini");
		
		// Prepare the plugin system
		plugins = new ArrayList<ENPlugin>();
		hooks = new HashMap<String, ENPluginHook>();
		pluginMenuItems = new HashMap<String, JMenuItem>();
		
		loadEvents            = new ENPluginHook();
		frameEvents           = new ENPluginHook();
		postFrameEvents       = new ENPluginHook();
		preStepEvents         = new ENPluginHook();
		postStepEvents        = new ENPluginHook();
		mouseClickedEvents    = new ENPluginHook();
		mouseDraggedEvents    = new ENPluginHook();
		mouseEnteredEvents    = new ENPluginHook();
		mouseExitedEvents     = new ENPluginHook();
		mouseMovedEvents      = new ENPluginHook();
		mousePressedEvents    = new ENPluginHook();
		mouseReleasedEvents   = new ENPluginHook();
		mouseWheelMovedEvents = new ENPluginHook();
		keyPressedEvents      = new ENPluginHook();
		keyReleasedEvents     = new ENPluginHook();
		
		hooks.put("load",       loadEvents);
		hooks.put("drawstart",  frameEvents);
		hooks.put("draw",       postFrameEvents);
		hooks.put("stepstart",  preStepEvents);
		hooks.put("step",       postStepEvents);
		hooks.put("click",      mouseClickedEvents);
		hooks.put("wheel",      mouseWheelMovedEvents);
		hooks.put("mouseover",  mouseEnteredEvents);
		hooks.put("mouseout",   mouseExitedEvents);
		hooks.put("mousemove",  mouseMovedEvents);
		hooks.put("mousedown",  mousePressedEvents);
		hooks.put("mouseup",    mouseReleasedEvents);
		hooks.put("keydown",    keyPressedEvents);
		hooks.put("keyup",      keyReleasedEvents);
		
		// Set up and show window
		try {
			createFullScreenFrame(); // separate frame for fullscreen display, will be moved to a seperate class
			setupComponents();
		} catch(Exception e){
			System.out.println("failed to set up components");
			e.printStackTrace();
		}
		
		// Ready, load plugins
		loadPlugins();
		
		// on('load')
		loadEvents.invokeMethods(new ENEvent());
	}
	
	// NESTED CLASSES
	
	// NES emulator object extended with plugin hooks before and after cpu step
	public class NESWithHooks extends NES { // nashorn requires public modifier
		public void prestep(){
			preStepEvents.invokeMethods(new ENEvent());
		}
		public void poststep(){
			postStepEvents.invokeMethods(new ENEvent());
		}
	}
	
	public class EmuScreen extends PixelCanvas { // nashorn requires public modifier
		public EmuScreen(){
			super(256, 240, nes.ppu.display);
		}
		public void preframe(Graphics2D g2d){
			frameEvents.invokeMethods(new ENEvent(g2d));
		}
		public void postframe(Graphics2D g2d){
			postFrameEvents.invokeMethods(new ENEvent(g2d));
		}
	}
	
	private class EmuThread extends Thread {
		private final long nano60hzTime = 16666667;
		long currentTime = System.nanoTime();
		long targetTime = currentTime + nano60hzTime;
		public void run(){
			while(!emuStopped){
				while(currentTime < targetTime){
					currentTime = System.nanoTime();
					if(!limitFPS && emuRunning){
						processGamepadInput();
						nes.runUntilVBlank(); // let nes vblank as many times as possible per 60hz frame cycle
					}
				}
				targetTime = currentTime + nano60hzTime;
				if(limitFPS && emuRunning){
					processGamepadInput();
					nes.runUntilVBlank(); // let nes run until vblank and draw once per 60hz frame cycle
				}
				screen.repaint();
				try {
					sleep(emuThreadSleepTime);
					 // should reduce polling time
				} catch(Exception e){
					// what
				}
			}
		}
	}
		
	private interface Callback {
		public void run();
	}

	private class ToolButton extends JLabel {
		private ImageIcon defaultIcon;
		private ImageIcon hoverIcon;
		private Callback c;
		
		public ToolButton(String tip, ImageIcon defaultIcon, ImageIcon hoverIcon){
			super("", defaultIcon, JLabel.CENTER);
			this.defaultIcon = defaultIcon;
			this.hoverIcon = hoverIcon;
			setToolTipText(tip);
			addMouseListener(new MouseAdapter(){
				public void mouseClicked(MouseEvent e){
					c.run();
				}
				public void mouseEntered(MouseEvent e){
					setIcon(hoverIcon);
				}
				public void mouseExited(MouseEvent e){
					setIcon(defaultIcon);
				}
				public void mousePressed(MouseEvent e){
					setIcon(defaultIcon);
				}
				public void mouseReleased(MouseEvent e){
					setIcon(hoverIcon);
				}
			});
		}
		public void setCallback(Callback c){
			this.c = c;
		}
	}
	
	// METHODS
	
	public NES getNes(){
		return nes;
	}
	
	public Config getConfig(){
		return config;
	}
	
	public PixelCanvas getScreen(){
		return screen;
	}
	
	public void toggleFullScreen(){
		if(!fullScreen){
			fullScreenFrame.setVisible(true);
			fullScreenPanel.add(screen);
			//screen.setAlignmentX(Component.CENTER_ALIGNMENT);
			screen.setDisplaySize((2160/240) * 256, 2160);
			fullScreenPanel.setPreferredSize(new Dimension(3840, 2160));
			fullScreenPanel.setSize(3840, 2160);
			//fullScreen = true;
		} else {
			fullScreenPanel.removeAll();
			fullScreenFrame.setVisible(false);
			mainPanel.add(screen);
			setScreenScale(config.getDouble("scale", 1.0));
			//fullScreen = false;
		}
	}
	
	// System
	
	public void stop(){
		//nes.apu.setMasterVolume(0.0);
		emuRunning = false;
		emuStopped = true;
		if(emuThread != null){
			while(emuThread.getState() != Thread.State.TERMINATED);
		}
		nes.apu.stop();
		screen.setVisible(false);
		fileSelect.setVisible(true);
		endMenuItem.setEnabled(false);
		hotbar.setVisible(false);
	}
	
	public void pause(){
		emuRunning = false;
		nes.apu.setMasterVolume(0.0);
		toolBtnPlay.setVisible(true);
		toolBtnPause.setVisible(false);
	}
	
	public void resume(){
		//nes.apu.notify();
		nes.apu.setMasterVolume(0.5);
		emuRunning = true;
		toolBtnPlay.setVisible(false);
		toolBtnPause.setVisible(true);
	}
	
	public void reset(){
		nes.cpu.reset();
	}
	
	// results from last on-screen mouse event
		
	//public int getMouseX(){
	//	return mouseX;
	//}
	//
	//public int getMouseY(){
	//	return mouseY;
	//}
	
	public void setScreenScale(double scale){
		screen.scale(scale);
		fileSelect.setPreferredSize(screen.getPreferredSize());
		pack();
		config.set("scale", scale);
	}
	
	// Plugin management
	
	public ENPlugin getPlugin(String id){
		for(int i = 0; i < plugins.size(); i++){
			if(plugins.get(i).getId() == id){
				return plugins.get(i);
			}
		}
		return null;
	}
	
	public String[] getPluginIds(){
		int pluginCount = plugins.size();
		String[] ret = new String[plugins.size()];
		for(int i = 0; i < pluginCount; i++){
			ret[i] = plugins.get(i).getId();
		}
		return ret;
	}
	
	public void addPluginMenuItem(String id, JMenuItem menuItem){ // attach menu item to root
		pluginMenuItems.put(id, menuItem);
		pluginMenu.add(menuItem);
	}
	
	public void addPluginMenuItem(String id, String attachToId, JMenuItem menuItem){ // attach menu item to existing submenu
		pluginMenuItems.put(id, menuItem);
		getPluginMenuItem(attachToId).add(menuItem);
	}
	
	public void addPluginMenuItem(String id, JFrame frame, String itemCaption){ // attach menu item to root (shows frame)
		JMenuItem menuItem = new JMenuItem(new AbstractAction(itemCaption){
			public void actionPerformed(ActionEvent e){
				frame.setVisible(true);
			}
		});
		pluginMenuItems.put(id, menuItem);
		pluginMenu.add(menuItem);
	}
	
	public void addPluginMenuItem(String id, String attachToId, JFrame frame, String itemCaption){ // attach menu item to existing submenu (shows frame)
		JMenuItem menuItem = new JMenuItem(new AbstractAction(itemCaption){
			public void actionPerformed(ActionEvent e){
				frame.setVisible(true);
			}
		});
		pluginMenuItems.put(id, menuItem);
		getPluginMenuItem(attachToId).add(menuItem);
	}
	
	public JMenuItem getPluginMenuItem(String id){
		return pluginMenuItems.get(id);
	}
	
	public String[] getPluginMenuItemIds(){
		Set<String> s = pluginMenuItems.keySet();
		String[] ret = new String[s.size()];
		ret = s.toArray(ret);
		return ret;
	}
	
	// PLUGIN EVENT MANAGEMENT
	
	public int on(String hookId, ENMethod m, int order){
		ENPluginHook hook = hooks.get(hookId);
		if(hook == null){
			System.out.printf("Invalid hookId '%s' supplied\n", hookId);
			return -1;
		}
		return hook.addMethod(m, order);
	}
	
	public int on(String hookId, ENMethod m){ // attach to end if order is irrelevant
		ENPluginHook hook = hooks.get(hookId);
		if(hook == null){
			System.out.printf("Invalid hookId '%s' supplied\n", hookId);
			return -1;
		}
		return hook.addMethod(m);
	}
	
	public void off(String hookId, int id){
		ENPluginHook hook = hooks.get(hookId);
		hook.removeMethod(id);
	}
	
	// private methods //////////////////////////////////////
	
	private void setupComponents() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JFileChooser fc = new JFileChooser(); // select rom dialog window
			
			menuBar = new JMenuBar(); // window menu bar
				JMenu fileMenu = new JMenu("File");
					JMenuItem openRomMenuItem = new JMenuItem("Open ROM...");
					endMenuItem = new JMenuItem("End emulation");
				JMenu systemMenu = new JMenu("System");
					JMenuItem resetMenuItem  = new JMenuItem("Reset");
					JMenuItem pauseMenuItem  = new JMenuItem("Pause");
					JMenuItem resumeMenuItem = new JMenuItem("Resume");
					// [separator]
					JMenuItem fullScreenMenuItem = new JMenuItem("Full screen");
				JMenu optionsMenu = new JMenu("Options");
					JMenuItem settingsMenuItem = new JMenuItem("Settings...");
					JMenu scaleMenu = new JMenu("Scale");
						JMenuItem scale1xMenuItem = new JMenuItem("1x (256x240)");
						JMenuItem scale2xMenuItem = new JMenuItem("2x (512x480)");
						JMenuItem scale3xMenuItem = new JMenuItem("3x (768x720)");
						JMenuItem scale4xMenuItem = new JMenuItem("4x (1024x960)");	
						JMenuItem scale5xMenuItem = new JMenuItem("5x (1280x1200)");	
					JCheckBoxMenuItem cpuOptMenuItem = new JCheckBoxMenuItem("CPU usage optimization");
					JCheckBoxMenuItem showHotbarMenuItem = new JCheckBoxMenuItem("Show hotbar");
				pluginMenu = new JMenu("Plugins");
				//pluginMenu.setVisible(false);
			mainPanel = new JPanel();	
				hotbar = new JPanel();
					toolBtnPause    = new ToolButton("Pause", resPauseIcon, resPauseIcon_hover);
					toolBtnPlay     = new ToolButton("Play", resPlayIcon,  resPlayIcon_hover);
					toolBtnPlayFast = new ToolButton("Uncap framerate", resPlayFastIcon,  resPlayFastIcon_hover);
					toolBtnStop     = new ToolButton("Stop emulation", resStopIcon,  resStopIcon_hover);
					toolBtnReset    = new ToolButton("Reset", resResetIcon,  resResetIcon_hover);
				fileSelect = new JPanel();
				screen = new EmuScreen();
			
			// Structure all components
			
			this.setJMenuBar(menuBar); // add menu bar
				menuBar.add(fileMenu);
					fileMenu.add(openRomMenuItem);
					fileMenu.add(endMenuItem);
				menuBar.add(optionsMenu);
					//optionsMenu.add(settingsMenuItem);
					optionsMenu.add(showHotbarMenuItem);
					optionsMenu.add(scaleMenu);
						scaleMenu.add(scale1xMenuItem);
						scaleMenu.add(scale2xMenuItem);
						scaleMenu.add(scale3xMenuItem);
						scaleMenu.add(scale4xMenuItem);
						scaleMenu.add(scale5xMenuItem);
					optionsMenu.add(cpuOptMenuItem);
				menuBar.add(systemMenu);
					systemMenu.add(resetMenuItem);
					systemMenu.add(pauseMenuItem);
					systemMenu.add(resumeMenuItem);
					systemMenu.add(new JSeparator());
					systemMenu.add(fullScreenMenuItem);
				menuBar.add(pluginMenu);
			this.add(mainPanel); // add main panel
				mainPanel.add(hotbar);
					hotbar.add(toolBtnStop);
					hotbar.add(toolBtnReset);
					hotbar.add(toolBtnPause);
					hotbar.add(toolBtnPlay);
					hotbar.add(toolBtnPlayFast);
				mainPanel.add(fileSelect);
					fileSelect.add(new JLabel("[rom selection tree view]"));
				mainPanel.add(screen);
			
			// Apply component settings
			
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.setResizable(false);
			this.setLocation(200, 200);
			fc.setFileFilter(new FileNameExtensionFilter("INES ROM image (*.nes)", "nes"));
			fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
			cpuOptMenuItem.setToolTipText("Let the emulation thread sleep between frames (may cause framerate drops)");
			setScreenScale(config.getDouble("scale", 1.0));
			//System.out.println(config.getDouble("scale", 1.0));
			fileSelect.setPreferredSize(new Dimension(256, 240));
			fileSelect.setOpaque(true);
			fileSelect.setBackground(Color.WHITE);
			screen.setVisible(false);
			screen.setFocusable(true);
			//hotbar.setOpaque(true);
			hotbar.setBackground(new Color(0x111120));
			toolBtnPause.setVisible(false);
			endMenuItem.setEnabled(false);
			//hotbar.setVisible(false);
			showHotbar = config.getBoolean("showHotbar", false);
			hotbar.setVisible(showHotbar);
			showHotbarMenuItem.setState(showHotbar);
			
			fullScreenMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
			resumeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
			pauseMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
			resetMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
			
			// Apply component event logic
			
			openRomMenuItem.addActionListener((e) -> {
				int fcResult = fc.showOpenDialog(self);
				if(fcResult == JFileChooser.APPROVE_OPTION){
					emuStopped = true; // let the thread die
					if(emuThread != null){
						// wait until the thread is actually kill before continuing
						while(emuThread.getState() != Thread.State.TERMINATED);
					}
					String romPath = fc.getSelectedFile().getPath();
					nes.loadRom(romPath); // load rom/reinstance memories
					screen.setRgbArray(nes.ppu.display); // update the canvas memory
					screen.setVisible(true);
					fileSelect.setVisible(false);
					self.pack();
					screen.requestFocusInWindow();
					// start the thread
					emuRunning = true;
					emuStopped = false;
					emuThread = new EmuThread();
					emuThread.start();
					nes.apu.start();
					nes.apu.setMasterVolume(0.5);
					endMenuItem.setEnabled(true);
					hotbar.setVisible(showHotbar);
				}
			});
			
			endMenuItem.addActionListener((e) -> {
				stop();
			});
			
			fullScreenMenuItem.addActionListener((e) -> {
				toggleFullScreen();
			});
			
			scale1xMenuItem.addActionListener((e) -> {
				setScreenScale(1.0);
			});
			
			scale2xMenuItem.addActionListener((e) -> {
				setScreenScale(2.0);
			});
			
			scale3xMenuItem.addActionListener((e) -> {
				setScreenScale(3.0);
			});
			
			scale4xMenuItem.addActionListener((e) -> {
				setScreenScale(4.0);
			});
			
			scale5xMenuItem.addActionListener((e) -> {
				setScreenScale(5.0);
			});
			
			showHotbarMenuItem.addActionListener((e) -> {
				showHotbar = !showHotbar;
				config.set("showHotbar", showHotbar);
				hotbar.setVisible(showHotbar);
				self.pack();
			});
			
			cpuOptMenuItem.addActionListener((e) -> {
				cpuOptimizationEnabled = !cpuOptimizationEnabled;
			});
			
			toolBtnReset.setCallback(() -> {
				nes.cpu.reset();
			});
			
			toolBtnPause.setCallback(() -> {
				//toolBtnPause.setVisible(false);
				//toolBtnPlay.setVisible(true);
				pause();
			});
			
			toolBtnStop.setCallback(() -> {
				stop();
			});
			
			toolBtnPlay.setCallback(() -> {
				//toolBtnPause.setVisible(true);
				//toolBtnPlay.setVisible(false);
				resume();
			});
			
			toolBtnPlayFast.setCallback(() -> {
				limitFPS = !limitFPS;
			});
			
			screen.addKeyListener(new KeyListener(){
				public void keyTyped(KeyEvent e){}
				public void keyPressed(KeyEvent e){
					keyPressedEvents.invokeMethods(new ENEvent(e));
					if(!e.isConsumed()){
						if(e.getKeyCode() == KeyEvent.VK_ESCAPE && fullScreen){
							toggleFullScreen();
							return;
						}
						useGamepad = false;
						int button = buttonMap(e.getKeyChar());
						if(button != -1){
							nes.joypadPort1[button] = 1;
						}
					}
				}
				public void keyReleased(KeyEvent e){
					keyReleasedEvents.invokeMethods(new ENEvent(e));
					if(!e.isConsumed()){
						useGamepad = false;
						int button = buttonMap(e.getKeyChar());
						if(button != -1){
							nes.joypadPort1[button] = 0;
						}
					}
				}
			});
			
			screen.addMouseListener(new MouseAdapter(){
				public void mouseClicked(MouseEvent e){
					//setMouseCoordsFromEvent(e);
					mouseClickedEvents.invokeMethods(new ENEvent(e, screen.getWidthFactor(), screen.getHeightFactor()));
				}
				public void mouseEntered(MouseEvent e){
					//setMouseCoordsFromEvent(e);
					mouseEnteredEvents.invokeMethods(new ENEvent(e, screen.getWidthFactor(), screen.getHeightFactor()));
				}
				public void mouseExited(MouseEvent e){
					//setMouseCoordsFromEvent(e);
					mouseExitedEvents.invokeMethods(new ENEvent(e, screen.getWidthFactor(), screen.getHeightFactor()));
				}
				public void mousePressed(MouseEvent e){
					//setMouseCoordsFromEvent(e);
					mousePressedEvents.invokeMethods(new ENEvent(e, screen.getWidthFactor(), screen.getHeightFactor()));
				}
				public void mouseReleased(MouseEvent e){
					//setMouseCoordsFromEvent(e);
					mouseReleasedEvents.invokeMethods(new ENEvent(e, screen.getWidthFactor(), screen.getHeightFactor()));
				}
				public void mouseWheelMoved(MouseWheelEvent e){
					//setMouseCoordsFromEvent(e);
					mouseWheelMovedEvents.invokeMethods(new ENEvent(e, screen.getWidthFactor(), screen.getHeightFactor()));
				}
			});
			
			screen.addMouseMotionListener(new MouseAdapter(){
				public void mouseDragged(MouseEvent e){
					//setMouseCoordsFromEvent(e);
					mouseMovedEvents.invokeMethods(new ENEvent(e, screen.getWidthFactor(), screen.getHeightFactor()));
					//System.out.printf("%d %d\n", e.getX(), e.getY());
				}
				public void mouseMoved(MouseEvent e){
					//setMouseCoordsFromEvent(e);
					mouseMovedEvents.invokeMethods(new ENEvent(e, screen.getWidthFactor(), screen.getHeightFactor()));
				}
			});
		});
		pack();
		setVisible(true);
		setScreenScale(config.getDouble("scale", 1.0)); // doesnt work up there
		screen.requestFocusInWindow();
	}
	
	private void createFullScreenFrame() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			fullScreenFrame = new JFrame("emuname (fullscreen)");
			fullScreenFrame.addComponentListener(new ComponentAdapter() {
				public void componentHidden(ComponentEvent e) {
					fullScreen = false;
				}
				public void componentShown(ComponentEvent e) {
					fullScreen = true;
				}
			});
			fullScreenPanel = new JPanel();
			//fullScreenPanel.setLayout(new BoxLayout(fullScreenPanel, BoxLayout.Y_AXIS));
			fullScreenPanel.setBackground(Color.BLACK);
			fullScreenFrame.setAlwaysOnTop(true);
			fullScreenFrame.setUndecorated(true);
			fullScreenFrame.add(fullScreenPanel);
			DisplayMode mode = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getDisplayMode();
			int width = mode.getWidth();
			int height = mode.getHeight();
			fullScreenFrame.setSize(width, height);
			fullScreenPanel.setPreferredSize(new Dimension(width, height));
		});
	}
	
	
	//private void setMouseCoordsFromEvent(MouseEvent e){
	//	mouseX = (int)(e.getX()/screen.getWidthFactor());
	//	mouseY = (int)(e.getY()/screen.getHeightFactor());
	//}
	
	private void processGamepadInput(){
		double axesDeadZone = 0.45;
		try {
			double[] axes = JXGamepads.xgGetAxes(0);
			byte[] buttons = JXGamepads.xgGetButtons(0);
			for(int i = 0; i < buttons.length; i++){
				if(buttons[i] == 1){
					useGamepad = true;
					break;
				}
			}
			for(int i = 0; i < axes.length; i++){
				if(Math.abs(axes[i]) > axesDeadZone){
					useGamepad = true;
					break;
				}
			}
			if(!useGamepad) return;
			nes.setButtonPressed(0, NES.BUTTON_A, buttons[12] == 1);
			nes.setButtonPressed(0, NES.BUTTON_B, buttons[14] == 1);
			nes.setButtonPressed(0, NES.BUTTON_SELECT, buttons[5] == 1);
			nes.setButtonPressed(0, NES.BUTTON_START, buttons[4] == 1);
			nes.setButtonPressed(0, NES.BUTTON_UP, buttons[0] == 1 || axes[3] >= axesDeadZone);
			nes.setButtonPressed(0, NES.BUTTON_DOWN, buttons[1] == 1 || axes[3] <= -axesDeadZone);
			nes.setButtonPressed(0, NES.BUTTON_LEFT, buttons[2] == 1 || axes[2] <= -axesDeadZone);
			nes.setButtonPressed(0, NES.BUTTON_RIGHT, buttons[3] == 1 || axes[2] >= axesDeadZone);
		} catch(UnsatisfiedLinkError e){
			//bad lib
			System.out.println("bad gamepad input lib");
		}
	}
	
	private int buttonMap(char keyPressed){
		switch(keyPressed){
			case 'm': return NES.BUTTON_A;
			case 'n': return NES.BUTTON_B;
			case 'q': return NES.BUTTON_SELECT;
			case 'e': return NES.BUTTON_START;
			case 'w': return NES.BUTTON_UP;
			case 's': return NES.BUTTON_DOWN;
			case 'a': return NES.BUTTON_LEFT;
			case 'd': return NES.BUTTON_RIGHT;
			default: return -1;
		}
	}
	
	// Loads and initialises all plugins from the plugins directory
	private void loadPlugins(){
		try {
			String[] pluginFiles = new File("./plugins/").list();
			for(int i = 0; i < pluginFiles.length; i++){
				String jarPath = "./plugins/" + pluginFiles[i];
				// skip over files that aren't .jars
				if(!jarPath.matches(".+?\\.jar$")) continue;
				JarFile jarFile = new JarFile(jarPath);
				Manifest man = jarFile.getManifest();
				// fetch the name of the entry point class from the jar's manifest
				String mainClass = man.getMainAttributes().getValue("Main-class").replace("/", ".");
				URL[] urls = {new URL("jar:file:" + jarPath + "!/")};
				URLClassLoader cl = URLClassLoader.newInstance(urls);
				Class pluginClass = cl.loadClass(mainClass);
				// instantiate the class, save reference to 'plugins' to be sorted by getInitOrder()
				@SuppressWarnings("unchecked")
				ENPlugin enp = (ENPlugin) pluginClass.newInstance();
				String pluginId = enp.getId();
				plugins.add(enp);
			}
			
			// sort plugins by initiation order
			Collections.sort(plugins, new Comparator<ENPlugin>(){
				public int compare(ENPlugin p1, ENPlugin p2){
					if(p1.getInitOrder() < p2.getInitOrder()) return -1;
					if(p1.getInitOrder() > p2.getInitOrder()) return 1;
					return 0;
				}
			});
			
			// initiate all plugins
			for(int i = 0; i < plugins.size(); i++){
				ENPlugin enp = plugins.get(i);
				System.out.println("[\033[92mPlugin\033[0m: \033[93m" + enp.getId() + "\033[0m (" + enp.getInitOrder()  + ")]"); //  (\033[90m" + jarPath + ":" + mainClass + "\033[0m)
				enp.init(this);
			}
			
		} catch(Exception e){
			System.out.println("Error occurred while loading plugins");
			e.printStackTrace();
		}
	}
}