package com.gmail.marcosav2010.config;

import com.gmail.marcosav2010.logger.Logger;

public class GeneralConfiguration extends Configuration {

    private static final String GENERAL_CONFIG_NAME = "general";

    public GeneralConfiguration() {
        super(GENERAL_CONFIG_NAME);

        var dp = new java.util.Properties();
        Properties.propCategory.forEach((s, p) -> dp.setProperty(s, p.getDefault().toString()));

        load(dp);
    }

    @Override
    public String get(String key) {
        return get(key, Properties.propCategory.get(key).getDefault().toString());
    }

    public String getVerboseLevel() {
        return get(Logger.VERBOSE_LEVEL_PROP).toUpperCase();
    }
}
