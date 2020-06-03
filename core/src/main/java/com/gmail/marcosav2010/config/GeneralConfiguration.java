package com.gmail.marcosav2010.config;

import com.gmail.marcosav2010.handshake.HandshakeAuthentificator.HandshakeRequirementLevel;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

import java.util.HashMap;
import java.util.Map;

public class GeneralConfiguration extends Configuration {

    public static final String VERBOSE_LEVEL = "verboseLevel",
            HANDSHAKE_REQUIREMENT_LEVEL = "defHandshakeRequirementLevel";
    static final Map<String, Property<?>> propCategory = new HashMap<>();
    private static final String GENERAL_CONFIG_NAME = "general";

    static {
        propCategory.put(VERBOSE_LEVEL,
                new Property<>(PropertyCategory.APPLICATION, VerboseLevel.class, VerboseLevel.MINIMAL));
        propCategory.put(HANDSHAKE_REQUIREMENT_LEVEL, new Property<>(PropertyCategory.PEER,
                HandshakeRequirementLevel.class, HandshakeRequirementLevel.PRIVATE));
    }

    public GeneralConfiguration() {
        super(GENERAL_CONFIG_NAME);

        var dp = new java.util.Properties();
        propCategory.forEach((s, p) -> dp.setProperty(s, p.getDefault().toString()));

        load(dp);
    }

    @Override
    public String get(String key) {
        return get(key, propCategory.get(key).getDefault().toString());
    }

    public String getVerboseLevel() {
        return get(VERBOSE_LEVEL).toUpperCase();
    }
}
