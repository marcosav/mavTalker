package com.gmail.marcosav2010.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import com.gmail.marcosav2010.logger.Logger;

public class Configuration {

	private String configName;
	
	private Properties defaultProperties;
	private Properties properties;
	
	public Configuration(String configName) {
		this.configName = configName + ".properties";
		
		properties = new Properties(defaultProperties);
	}
	
	public void load() {
		File f = new File(configName);
		if (f.exists()) {
			
			try {
				properties.load(Files.newInputStream(f.toPath()));
			} catch (IOException e) {
				Logger.log(e);
			}
			
		} else {
			try {
				f.createNewFile();
			} catch (IOException e) {
				Logger.log(e);
			}
		}
	}
	
	public String get(String key) {
		return properties.getProperty(key);
	}
	
	public String get(String key, String def) {
		return properties.getProperty(key, def);
	}
}
