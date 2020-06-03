package com.gmail.marcosav2010.config;

class Property<T> {

    private final PropertyCategory category;
    private final Class<T> classType;
    private final T def;

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