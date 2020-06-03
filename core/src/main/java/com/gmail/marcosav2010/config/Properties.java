package com.gmail.marcosav2010.config;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class Properties {

    private final PropertyCategory category;
    private final Map<String, Object> properties;

    public Properties(PropertyCategory category, IConfiguration config) {
        this.category = category;
        properties = new HashMap<>();
        GeneralConfiguration.propCategory.entrySet().stream().filter(e -> e.getValue().getCategory() == category)
                .forEach(e -> set(e.getKey(), config.get(e.getKey())));
    }

    public PropertyCategory getCategory() {
        return category;
    }

    public <T> T get(String prop) {
        return (T) GeneralConfiguration.propCategory.get(prop).getType().cast(properties.get(prop));
    }

    @SuppressWarnings("rawtypes")
    public boolean set(String prop, String value) {
        try {
            Class<?> t = GeneralConfiguration.propCategory.get(prop).getType();
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
            properties.put(prop, GeneralConfiguration.propCategory.get(prop).getType().cast(value));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean exist(String prop) {
        return properties.containsKey(prop);
    }

    public String toString() {
        return GeneralConfiguration.propCategory.entrySet().stream().filter(e -> e.getValue().getCategory() == getCategory()).map(e -> {
            String p = "  - NAME: " + e.getKey() + "\tVALUE: " + get(e.getKey()) + "\tOPTIONS(";
            var t = e.getValue().getType();
            if (t.isEnum())
                p += Stream.of((Enum<?>[]) t.getEnumConstants()).map(Enum::toString)
                        .collect(Collectors.joining(", "));
            else
                p += t.getSimpleName();
            p += ")";
            return p;
        }).collect(Collectors.joining("\n"));
    }
}