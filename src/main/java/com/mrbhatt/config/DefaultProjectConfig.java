package com.mrbhatt.config;

import java.io.InputStream;
import java.util.Map;

import org.yaml.snakeyaml.*;

public class DefaultProjectConfig implements ProjectConfig {
  public Map<String, Object> get() {
      Yaml yaml = new Yaml();
      InputStream appConfig = null;
      try {
        appConfig = DefaultProjectConfig.class.getResourceAsStream("/appconfig.yaml");
      } catch(Exception ex) {
        System.out.println("ex:" + ex.getMessage());
      }
      return (Map<String, Object>) yaml.load(appConfig);
  }
}
