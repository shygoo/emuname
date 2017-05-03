package net.shygoo.emuname;

// subclasses:
//  constructor must have no parameters
//  constructor must call super(id, initOrder)
//  init(T mainObject) must be overriden

// initOrder determines when the plugin will be initialised
// id allows cross-plugin communication via mainWindow.getPlugin(id)

public abstract class ENPlugin {
	private final int initOrder;
	private final String id;
	public ENPlugin(String id, int initOrder){
		this.id = id;
		this.initOrder = initOrder;
	}
	public String getId(){
		return id;
	}
	public int getInitOrder(){
		return initOrder;
	}
	// init serves as a post-constructor which provides the main object to the plugin
	public abstract void init(Emuname emuname);
}