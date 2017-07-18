package com.mrbhatt.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.yaml.snakeyaml.*;

public class LocalProjectConfig implements ProjectConfig {
    private String configFilePath = null;
    //private String generatedCodePath = "/generated_code";

    LocalProjectConfig(String[] args) {
        File configFile = new File(args[0]);

        // fail fast if the config file does not exist or the directory for generated code is not writable
        if (!configFile.exists()) {
            throw new IllegalArgumentException("config yaml file not found");
        } else {
            configFilePath = args[0];
        }
    }

    public Map<String, Object> getValues() {
        Yaml yaml = new Yaml();
        String configContent = null;
        try {
            configContent = new String(Files.readAllBytes(Paths.get(configFilePath)));
        } catch (Exception ex) {
            System.out.println("ex: " + ex.getMessage());
        }
        return (Map<String, Object>) yaml.load(configContent);
    }
}
