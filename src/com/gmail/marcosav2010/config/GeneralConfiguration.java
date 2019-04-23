package com.gmail.marcosav2010.config;

import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

public class GeneralConfiguration extends Configuration {

	private static final String GENERAL_CONFIG_NAME = "general";
	
	public GeneralConfiguration() {
		super(GENERAL_CONFIG_NAME);
		load();
	}
	
	public String getVerboseLevel() {
		return get("verboseLevel", VerboseLevel.HIGH.name()).toUpperCase();
	}
}
