package net.shygoo.emuname;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

public class Config {
	private Map<String, String> map = new HashMap<String, String>();
	private Path path;
	
	private final static int
	SETTING_NAME  = 0,
	SETTING_VALUE = 1;
	
	public Config(String pathStr){
		path = Paths.get(pathStr);
		try {
			// load config file keys and values into hashmap
			List<String> configLines = Files.readAllLines(path);
			int len = configLines.size();
			for(int i = 0; i < len; i++){
				String[] setting = configLines.get(i).trim().split("=", 2);
				if(setting.length == 2){
					map.put(setting[SETTING_NAME], setting[SETTING_VALUE].trim());
					System.out.printf("[\033[92mConfig\033[0m: %s = \"%s\"]\n", setting[SETTING_NAME], setting[SETTING_VALUE]);
				}
			}
		} catch(Exception e){
			System.out.println("An error occurred while loading the config file");
			System.out.println(e.toString());
		}
	}
	private void save(){
		String data = "";
		for(Map.Entry<String, String> entry : map.entrySet()){
			data += entry.getKey() + "=" + entry.getValue() + "\n";
		}
		try {
			Files.write(path, data.getBytes());
		} catch(Exception e){
			System.out.println("Error occurred while saving the config file");
		}
	}
	
	// returns string value of key or defaultValue if key is not present
	public String getString(String key, String defaultValue){
		String value = map.get(key);
		if(value == null){
			set(key, defaultValue);
			return defaultValue;
		}
		return value;
	}
	// returns boolean value of key or defaultValue if key is not present
	public boolean getBoolean(String key, boolean defaultValue){
		String value = map.get(key);
		if(value == null){
			set(key, defaultValue);
			return defaultValue;
		}
		return Boolean.parseBoolean(value);
	}
	// returns int value of key or defaultValue if key is not present
	public int getInt(String key, int defaultValue){
		String value = map.get(key);
		if(value == null){
			set(key, defaultValue);
			return defaultValue;
		}
		return Integer.parseInt(value);
	}
	// returns double value of key or defaultValue if key is not present
	public double getDouble(String key, double defaultValue){
		String value = map.get(key);
		if(value == null){
			set(key, defaultValue);
			return defaultValue;
		}
		return Double.parseDouble(value);
	}
	// sets key to the string value of the object
	public void set(String key, String value){
		map.put(key, value.toString());
		save();
	}
	
	public void set(String key, double value){
		set(key, Double.toString(value));
	}
	
	public void set(String key, boolean value){
		set(key, Boolean.toString(value));
	}
}