package com.gmail.marcosav2010.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.peer.HandshakeAuthentificator.HandshakeRequirementLevel;

public class GeneralConfiguration extends Configuration {

	private static final String GENERAL_CONFIG_NAME = "general";

	public static final String VERBOSE_LEVEL = "verboseLevel", HANDSHAKE_REQUIREMENT_LEVEL = "handshakeRequirementLevel";

	private static final Map<String, Property> propCategory = new HashMap<>();

	static {
		propCategory.put(VERBOSE_LEVEL, new Property(PropertyCategory.APPLICATION, VerboseLevel.class));
		propCategory.put(HANDSHAKE_REQUIREMENT_LEVEL, new Property(PropertyCategory.PEER, HandshakeRequirementLevel.class));
	}

	public GeneralConfiguration() {
		super(GENERAL_CONFIG_NAME);
		load();
	}

	public String getVerboseLevel() {
		return get(VERBOSE_LEVEL, VerboseLevel.HIGH.name()).toUpperCase();
	}

	public String getHandshakeRequirementLevel() {
		return get(HANDSHAKE_REQUIREMENT_LEVEL, HandshakeRequirementLevel.PRIVATE.name()).toUpperCase();
	}

	public static boolean isCategory(String prop, PropertyCategory category) {
		if (!propCategory.containsKey(prop))
			return false;

		return propCategory.get(prop).getCategory() == category;
	}

	public static Collection<String> getProperties(PropertyCategory category) {
		return propCategory.entrySet().stream().filter(e -> e.getValue().getCategory() == category).map(e -> e.getKey()).collect(Collectors.toList());
	}

	public static String propsToString(PropertyCategory category, GeneralConfiguration c) {
		return propCategory.entrySet().stream().filter(e -> e.getValue().getCategory() == category).map(e -> {
			String p = "  - NAME: " + e.getKey() + "\tVALUE: " + c.get(e.getKey()) + "\tOPTIONS(";
			var t = e.getValue().getType();
			if (t.isEnum())
				p += Stream.of((Enum<?>[]) t.getEnumConstants()).map(Enum::toString).collect(Collectors.joining(", "));
			else 
				p += t.getSimpleName();
			p += ")";
			return p;
		}).collect(Collectors.joining("\n"));
	}

	public static enum PropertyCategory {
		APPLICATION, PEER;
	}

	private static class Property {

		private PropertyCategory category;
		private Class<?> classType;

		public Property(PropertyCategory category, Class<?> classType) {
			this.category = category;
			this.classType = classType;
		}

		public PropertyCategory getCategory() {
			return category;
		}

		public Class<?> getType() {
			return classType;
		}
	}
}
