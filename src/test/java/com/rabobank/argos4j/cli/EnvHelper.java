package com.rabobank.argos4j.cli;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.Map;

public class EnvHelper {
    private EnvHelper() {
    }

    @SuppressWarnings({"unchecked"})
    @SneakyThrows
    static void updateEnv(String name, String val) {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).put(name, val);
    }

    @SneakyThrows
    static void removeEntry(String name) {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).remove(name);
    }
}
