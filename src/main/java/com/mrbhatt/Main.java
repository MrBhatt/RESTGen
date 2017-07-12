package com.mrbhatt;

import com.mrbhatt.config.ProjectConfig;
import com.mrbhatt.config.LocalProjectConfig;
import com.mrbhatt.config.DefaultProjectConfig;
import com.sun.codemodel.*;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Service;

public class Main {

    public static void main(String[] args) throws Exception {
        ProjectConfig projectConfig = null;
        if (args.length > 0) {
          projectConfig = new LocalProjectConfig(args);
        } else {
          projectConfig = new DefaultProjectConfig();
        }
        generateCode(projectConfig.get());
    }

    private static void generateCode(Map<String, Object> projectConfig) throws Exception {
      // Instantiate a new JCodeModel
      JCodeModel codeModel = new JCodeModel();
      final String application = "Application";
      final String repository = "Repository";
      final String abstractEntity = "AbstractEntity";
      final String getPrefix = "get";
      Boolean bootApplication = (Boolean) projectConfig.get("boot-application");
      //Boolean generateGradle = (Boolean) projectConfig.get("generate-gradle");
      String rootPackageName = (String) projectConfig.get("package");
      String resourceName = (String) projectConfig.get("resource");
      String resourceNameLower = StringUtils.lowerCase(resourceName);
      String resourceNameLowerPlural = resourceNameLower.concat("s");
      String resourceNameCapital = StringUtils.capitalize(resourceName);
      String resourceNameCapitalPlural = resourceNameCapital.concat("s");
      Map<String, Object> resourceAttributes = (Map<String, Object>) projectConfig.get("resource-attributes");
      String repositoryName = resourceNameCapital.concat(repository);
      String repositoryFieldName = resourceNameLower.concat(repository);

      // --------------------------------------------------------------------
      //    Create packages for Controllers, services and persistence layer
      // --------------------------------------------------------------------
      // Create the root package
      JPackage rootPackage = codeModel._package(rootPackageName);
      // create a sub package for controllers
      JPackage controllerPackage = rootPackage.subPackage("controller");
      // subpackages for services
      JPackage servicePackage = rootPackage.subPackage("service");
      // subpackages for model
      JPackage modelPackage = rootPackage.subPackage("model");
      // subPackages for persistence
      JPackage persistencePackage = rootPackage.subPackage("persistence");

      // ----------------------------------------------------------------
      //   Create Boot Application (if true in config)
      // ----------------------------------------------------------------
      if (!Objects.isNull(bootApplication) && bootApplication == Boolean.TRUE) {
          JDefinedClass applicationClass = rootPackage._class(application);
          applicationClass.annotate(SpringBootApplication.class);
          JMethod mainMethod = applicationClass.method(JMod.PUBLIC|JMod.STATIC, codeModel.VOID, "main");
          JVar mainArgs = mainMethod.param(String[].class, "args");
          JBlock mainMethodBody = mainMethod.body();
          mainMethodBody.add(codeModel.ref(SpringApplication.class).staticInvoke("run")
          .arg(applicationClass.dotclass()).arg(mainArgs));
      }

      // ----------------------------------------------------------------
      //    Create abstract entity and models
      // ----------------------------------------------------------------
      JDefinedClass abstractEntityClass = modelPackage._class(abstractEntity);
      abstractEntityClass.annotate(MappedSuperclass.class);
      JFieldVar idField = abstractEntityClass.field(JMod.PRIVATE, codeModel.LONG, "id");
      idField.annotate(Id.class);
      JAnnotationUse generatedValueAnnotation = idField.annotate(GeneratedValue.class);
      generatedValueAnnotation.param("strategy", GenerationType.AUTO);

      JDefinedClass modelClass = modelPackage._class(resourceNameCapital);
      modelClass._extends(abstractEntityClass);
      modelClass.field(JMod.STATIC | JMod.FINAL, Long.class, "serialVersionUID", JExpr.lit(1L));
      if (null != resourceAttributes && !resourceAttributes.isEmpty()) {
          resourceAttributes.entrySet().forEach(entry -> {
            modelClass.field(JMod.PRIVATE, getType(entry.getValue()), entry.getKey());
          });
          modelClass.annotate(Data.class);
          modelClass.annotate(Entity.class);
      }

      // -------------------------------------------------------------------
      //     Define resource collection to be returned for resource gets
      // -------------------------------------------------------------------
      // List of resource instances
      JClass rawListClass = codeModel.ref(List.class);
      JClass modelListClass = rawListClass.narrow(modelClass);

      // -------------------------------------------------------------------
      //     Create repository
      // -------------------------------------------------------------------
      JDefinedClass repositoryInterface = persistencePackage._interface(repositoryName);
      JClass narrowJpaRepository = codeModel.ref(JpaRepository.class).narrow(modelClass).narrow(codeModel.LONG);
      repositoryInterface._extends(narrowJpaRepository);

      // ----------------------------------------------------------------
      //    Create service
      // ----------------------------------------------------------------
      String serviceName = resourceNameCapital.concat("Service");
      String serviceNameLower = resourceNameLower.concat("Service");
      JDefinedClass serviceInterface = servicePackage._interface(serviceName);
      JDefinedClass serviceClass = servicePackage._class(serviceName.concat("Impl"));
      serviceClass._implements(serviceInterface);
      serviceClass.annotate(Service.class);

      // add repository field to serviceClass
      JFieldVar wiredRepo = serviceClass.field(JMod.PRIVATE, repositoryInterface, repositoryFieldName);
      wiredRepo.annotate(Autowired.class);

      // create methods in Service (to be called by the method in controller)
      String methodName = getPrefix.concat(resourceNameCapitalPlural);
      JMethod getResListServiceInterfaceMethod = serviceInterface.method(JMod.PUBLIC, modelListClass, methodName);
      JMethod getResListServiceClassMethod = serviceClass.method(JMod.NONE, modelListClass, methodName);
      getResListServiceClassMethod.body()._return(wiredRepo.invoke("findAll"));

      methodName = getPrefix.concat(resourceNameCapital);
      JMethod getResServiceInterfaceMethod = serviceInterface.method(JMod.PUBLIC, modelClass, methodName);
      getResServiceInterfaceMethod.param(codeModel.LONG, "id");
      JMethod getResServiceClassMethod = serviceClass.method(JMod.NONE, modelClass, methodName);
      JVar getResServiceClassMethodParamId = getResServiceClassMethod.param(codeModel.LONG, "id");
      getResServiceClassMethod.body()._return(wiredRepo.invoke("findOne").arg(getResServiceClassMethodParamId));

      // ----------------------------------------------------------------
      //     Create controller
      // ----------------------------------------------------------------
      String controllerName = resourceNameCapital.concat("Controller");
      JDefinedClass controllerClass = controllerPackage._class(controllerName);
      controllerClass.annotate(RestController.class);
      JAnnotationUse controllerReqMappingAnnotation = controllerClass.annotate(RequestMapping.class);
      controllerReqMappingAnnotation.param("value", "/v1/".concat(resourceNameLowerPlural));
      // Bind controller to service
      JFieldVar wiredService = controllerClass.field(JMod.PRIVATE, serviceInterface, serviceNameLower);
      wiredService.annotate(Autowired.class);
      // Add methods for operations over the resource (in controller)
      // 1. Get all instances of the resource
      methodName = getPrefix.concat(resourceNameCapitalPlural);
      JMethod getResourceListMethod = controllerClass.method(JMod.PUBLIC, modelListClass, methodName);
      JAnnotationUse getAllReqMappingAnnotation = getResourceListMethod.annotate(RequestMapping.class);
      getAllReqMappingAnnotation.param("method", RequestMethod.GET);
      JBlock getResourceListMethodBody = getResourceListMethod.body();
      getResourceListMethodBody._return(wiredService.invoke(getResListServiceInterfaceMethod));

      // 2. Get a single resource identified by the Id
      methodName = getPrefix.concat(resourceNameCapital);
      JMethod getResourceMethod = controllerClass.method(JMod.PUBLIC, modelClass, methodName);
      JAnnotationUse getResourceMethodMappingAnnotation = getResourceMethod.annotate(RequestMapping.class);
      getResourceMethodMappingAnnotation.param("method", RequestMethod.GET);
      JVar id = getResourceMethod.param(codeModel.LONG, "id");
      id.annotate(PathVariable.class);
      getResourceMethodMappingAnnotation.param("value", "/{".concat(id.name()).concat("}"));
      JBlock getResourceMethodBody = getResourceMethod.body();
      getResourceMethodBody._return(wiredService.invoke(getResServiceInterfaceMethod).arg(id));


      // ---------------------------------------------------------------
      //       Generate gradle file (if present in config)
      // ---------------------------------------------------------------

      // if (!Objects.isNull(generateGradle) && generateGradle == Boolean.TRUE) {
      //
      //     FileUtils.readFileToString()
      // }

      File dir = new File("./generated_code");
      if (!dir.exists()) {
        dir.mkdir();
      }

      // Generate the code
      codeModel.build(dir);
    }

    private static final Class getType(Object providedTypeInfo) {
      String providedTypeInfoString = (String) providedTypeInfo;

      switch(providedTypeInfoString.toLowerCase()) {
        case "string":
          return String.class;
        case "int":
          return Integer.class;
      }
      return Object.class;
    }
}
