package com.mrbhatt;

import com.mrbhatt.config.ProjectConfig;
import com.mrbhatt.config.ProjectConfigFactory;
import com.mrbhatt.generator.CodeGenerator;
import com.sun.codemodel.*;

import java.io.File;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

public class Main {

    public static void main(String... args) throws Exception {
        generateCode(ProjectConfigFactory.get(args));
    }

    private static void generateCode(ProjectConfig projectConfig) throws Exception {
        Map<String, Object> projectConfigValues = projectConfig.getValues();

        Boolean bootApplication = (Boolean) projectConfigValues.get("boot-application");
        String rootPackageName = (String) projectConfigValues.get("package");
        String resourceName = (String) projectConfigValues.get("resource");
        Map<String, Object> resourceAttributes = (Map<String, Object>) projectConfigValues.get("resource-attributes");

        CodeGenerator codeGenerator = new CodeGenerator();
        JCodeModel codeModel = new JCodeModel();

        // Create the root package
        JPackage rootPackage = codeModel._package(rootPackageName);

        codeGenerator.setCodeModel(codeModel);
        codeGenerator.setResourceAttributes(resourceAttributes);
        codeGenerator.setResourceName(resourceName);
        codeGenerator.setRootPackage(rootPackage);
        codeGenerator.generate();

        // ----------------------------------------------------------------
        //   Create Boot Application (if true in config)
        // ----------------------------------------------------------------
        if (!Objects.isNull(bootApplication) && bootApplication == Boolean.TRUE) {
            createBootApplication(codeModel, rootPackage);
        }

        File dir = new File("./generated_code");
        if (!dir.exists()) {
            dir.mkdir();
        }

        // Generate the code
        codeModel.build(dir);
    }



    private static final void createBootApplication(JCodeModel codeModel, JPackage rootPackage) throws Exception {
        JDefinedClass applicationClass = rootPackage._class("Application");
        applicationClass.annotate(SpringBootApplication.class);
        JMethod mainMethod = applicationClass.method(JMod.PUBLIC | JMod.STATIC, codeModel.VOID, "main");
        JVar mainArgs = mainMethod.param(String[].class, "args");
        JBlock mainMethodBody = mainMethod.body();
        mainMethodBody.add(codeModel.ref(SpringApplication.class).staticInvoke("run")
                .arg(applicationClass.dotclass()).arg(mainArgs));
    }
}
