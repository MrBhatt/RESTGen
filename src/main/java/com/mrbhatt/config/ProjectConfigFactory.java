package com.mrbhatt.config;

public class ProjectConfigFactory {

    public static ProjectConfig get(String... args) {
        if (args.length > 0) {
            return new LocalProjectConfig(args);
        } else {
            return new DefaultProjectConfig();
        }
    }
}
