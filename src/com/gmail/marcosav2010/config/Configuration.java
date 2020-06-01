package com.gmail.marcosav2010.config;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.main.Main;

public class Configuration {

	private final ILog log;

	private String configName;
	private Path path;

	private boolean save;

	private Properties properties;

	public Configuration(String configName) {
		this.configName = configName + ".properties";
		save = false;
		log = new Log(Main.getInstance(), "Conf-" + configName);
	}

	public void load(Properties defaultProperties) {
		properties = defaultProperties;

		File f = new File(configName);
		path = f.toPath();
		if (f.exists()) {

			try {
				properties.load(Files.newInputStream(path));
			} catch (IOException e) {
				log.log(e);
			}

		} else {
			try {
				f.createNewFile();
				_store();
			} catch (IOException e) {
				log.log(e);
			}
		}
	}

	public boolean exists(String key) {
		return properties.getProperty(key) != null;
	}

	public String get(String key) {
		return properties.getProperty(key);
	}

	public String get(String key, String def) {
		return properties.getProperty(key, def);
	}

	public void set(String key, String value) {
		properties.setProperty(key, value);
		save = true;
	}

	public void store() throws IOException {
		if (save)
			_store();
	}

	private void _store() throws IOException {
		try (OutputStream out = Files.newOutputStream(path)) {
			properties.store(out, null);
		}
	}
}
