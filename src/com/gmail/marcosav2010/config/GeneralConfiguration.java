package com.gmail.marcosav2010.config;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gmail.marcosav2010.handshake.HandshakeAuthentificator.HandshakeRequirementLevel;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

public class GeneralConfiguration extends Configuration {

	private static final String GENERAL_CONFIG_NAME = "general";

	public static final String VERBOSE_LEVEL = "verboseLevel", HANDSHAKE_REQUIREMENT_LEVEL = "defHandshakeRequirementLevel";

	private static final Map<String, Property<?>> propCategory = new HashMap<>();

	static {
		propCategory.put(VERBOSE_LEVEL, new Property<VerboseLevel>(PropertyCategory.APPLICATION, VerboseLevel.class, VerboseLevel.MINIMAL));
		propCategory.put(HANDSHAKE_REQUIREMENT_LEVEL,
				new Property<HandshakeRequirementLevel>(PropertyCategory.PEER, HandshakeRequirementLevel.class, HandshakeRequirementLevel.PRIVATE));
	}

	public GeneralConfiguration() {
		super(GENERAL_CONFIG_NAME);
		
		var dp = new java.util.Properties();
		propCategory.forEach((s, p) -> dp.setProperty(s, p.getDefault().toString()));
		
		load(dp);
	}

	public String get(String key) {
		return get(key, propCategory.get(key).getDefault().toString());
	}

	public String getVerboseLevel() {
		return get(VERBOSE_LEVEL).toUpperCase();
	}

	public static enum PropertyCategory {
		APPLICATION, PEER;
	}

	private static class Property<T> {

		private PropertyCategory category;
		private Class<T> classType;
		private T def;

		public Property(PropertyCategory category, Class<T> classType, T def) {
			this.category = category;
			this.classType = classType;
			this.def = def;
		}

		public PropertyCategory getCategory() {
			return category;
		}

		public Class<T> getType() {
			return classType;
		}

		public T getDefault() {
			return def;
		}
	}

	public static class Properties {

		private PropertyCategory category;
		private Map<String, Object> properties;

		public Properties(PropertyCategory category, GeneralConfiguration config) {
			this.category = category;
			properties = new HashMap<>();
			propCategory.entrySet().stream().filter(e -> e.getValue().getCategory() == category).forEach(e -> set(e.getKey(), config.get(e.getKey())));
		}

		public PropertyCategory getCategory() {
			return category;
		}

		public <T> T get(String prop) {
			return (T) propCategory.get(prop).getType().cast(properties.get(prop));
		}

		@SuppressWarnings("rawtypes")
		public boolean set(String prop, String value) {
			try {
				Class<?> t = propCategory.get(prop).getType();
				Object o = value;

				if (t.isEnum()) {
					o = Enum.valueOf((Class<? extends Enum>) t, value.toUpperCase());
				} else if (t.isAssignableFrom(Integer.class)) {
					o = Integer.parseInt(value);
				}

				properties.put(prop, o);
				return true;
			} catch (Exception ex) {
				return false;
			}
		}

		public <T> boolean set(String prop, T value) {
			try {
				properties.put(prop, propCategory.get(prop).getType().cast(value));
				return true;
			} catch (Exception ex) {
				return false;
			}
		}

		public boolean exist(String prop) {
			return properties.containsKey(prop);
		}

		public String toString() {
			return propCategory.entrySet().stream().filter(e -> e.getValue().getCategory() == getCategory()).map(e -> {
				String p = "  - NAME: " + e.getKey() + "\tVALUE: " + get(e.getKey()) + "\tOPTIONS(";
				var t = e.getValue().getType();
				if (t.isEnum())
					p += Stream.of((Enum<?>[]) t.getEnumConstants()).map(Enum::toString).collect(Collectors.joining(", "));
				else
					p += t.getSimpleName();
				p += ")";
				return p;
			}).collect(Collectors.joining("\n"));
		}
	}
}
