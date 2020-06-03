package com.gmail.marcosav2010.config;

public interface IConfiguration {

    boolean exists(String key);

    String get(String key);

    String get(String key, String def);

    void set(String key, String value);
}