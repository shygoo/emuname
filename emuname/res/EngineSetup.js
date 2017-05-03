// Engine setup code

// Create frame events management object
Object.defineProperty(this, 'frameEvents', {
	value: new (function(){
		var _events = {};
		var _index = 0;
		this.add = function(callback){
			var id = _index;
			_events[id] = callback;
			_index++;
		}
		this.remove = function(id){
			delete _events[id];
		}
		this.clear = function(){
			_events = {};
		}
		this.run = function(){
			for(var i in _events){
				_events[i]();
			}
		}
	}),
	enumerable: true,
	configurable: false,
	writable: false
});

// Create plugin management object
Object.defineProperty(this, 'plugins', {
	value: new (function(){
		var _plugins = {};
		this.add = function(settings){

			var sw = javax.swing;
			var pluginMenu = mainWindow.pluginMenu;
			
			pluginMenu.setVisible(true); // if it isn't visible already
			
			var id   = settings.id;
			var name = settings.caption || "null";
			var type = settings.type || "click";
			var action = settings.action || function(){};
			var init = settings.init || function(){};
			
			var plugin = {
				window:   null,
				state:    false,
				menuItem: null
			}
		
			switch(type){
				case 'toggle':
					plugin.menuItem = new sw.JCheckBoxMenuItem(new sw.AbstractAction(name){
						actionPerformed: function(){
							plugin.state = !plugin.state;
							action(plugin.state);
						}
					});
					break;
				case 'window':
					//print("creating window");
					plugin.window = new sw.JFrame(name);
					plugin.menuItem = new sw.JMenuItem(new sw.AbstractAction(name){
						actionPerformed: function(){
							plugin.window.setVisible(true);
						}
					});
					break;
				default: // 'click'
					plugin.menuItem = new sw.JMenuItem(new sw.AbstractAction(name){
						actionPerformed: action
					});
			}
			
			settings.init.call(plugin);
			
			pluginMenu.add(plugin.menuItem);
			_plugins[id] = plugin;
			return plugin;
		}
	}),
	enumerable: true,
	configurable: false,
	writable: false
});


// Lock objects for stability
Object.freeze(frameEvents);
Object.freeze(plugins);


print("[Script engine globals: " + Object.keys(this) + "]");