package com.mrbhatt.generator;

import com.sun.codemodel.*;
import lombok.Data;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.List;
import java.util.Map;

public class CodeGenerator {

    @Setter
    private JCodeModel codeModel;
    @Setter
    private String resourceName;
    @Setter
    private Map<String, Object> resourceAttributes;
    @Setter
    private JPackage rootPackage;

    public void generate() throws Exception {
        final String repository = "Repository";
        String repositoryName = getModifiedResourceName(resourceName, Modifier.CAPITAL).concat(repository);
        String repositoryFieldName = getModifiedResourceName(resourceName, Modifier.LOWER).concat(repository);
        // -------------------------------------------------------------------
        //     Create model
        // -------------------------------------------------------------------
        JDefinedClass modelClass = buildModelClass(codeModel, resourceName, resourceAttributes);
        // -------------------------------------------------------------------
        //     Create repository
        // -------------------------------------------------------------------
        JDefinedClass repositoryInterface = buildRepoClass(codeModel, repositoryName, modelClass);
        // ----------------------------------------------------------------
        //    Create service and controller
        // ----------------------------------------------------------------
        buildServiceAndController(codeModel, resourceName, rootPackage, repositoryFieldName, modelClass, repositoryInterface);
    }

    /**
     *  Create abstract entity and model
     *
     */
    private final JDefinedClass buildModelClass(JCodeModel codeModel, String resourceName,
                                                       Map<String, Object> resourceAttributes) throws Exception {
        JPackage modelPackage = rootPackage.subPackage("model");
        JDefinedClass abstractEntityClass = modelPackage._class("AbstractEntity");
        abstractEntityClass.annotate(MappedSuperclass.class);
        JFieldVar idField = abstractEntityClass.field(JMod.PRIVATE, codeModel.LONG, "id");
        idField.annotate(Id.class);
        JAnnotationUse generatedValueAnnotation = idField.annotate(GeneratedValue.class);
        generatedValueAnnotation.param("strategy", GenerationType.AUTO);

        JDefinedClass modelClass = modelPackage._class(getModifiedResourceName(resourceName, Modifier.CAPITAL));
        modelClass._extends(abstractEntityClass);
        modelClass.field(JMod.STATIC | JMod.FINAL, Long.class, "serialVersionUID", JExpr.lit(1L));
        if (null != resourceAttributes && !resourceAttributes.isEmpty()) {
            resourceAttributes.entrySet().forEach(entry -> {
                modelClass.field(JMod.PRIVATE, getType(entry.getValue()), entry.getKey());
            });
            modelClass.annotate(Data.class);
            modelClass.annotate(Entity.class);
        }

        return modelClass;
    }

    private final JDefinedClass buildRepoClass(JCodeModel codeModel, String repositoryName,
                                                      JDefinedClass modelClass) throws Exception {

        JPackage persistencePackage = rootPackage.subPackage("persistence");

        JDefinedClass repositoryInterface = persistencePackage._interface(repositoryName);
        JClass narrowJpaRepository = codeModel.ref(JpaRepository.class).narrow(modelClass).narrow(codeModel.LONG);
        repositoryInterface._extends(narrowJpaRepository);
        repositoryInterface.annotate(codeModel.ref(Repository.class));
        return repositoryInterface;
    }

    private void buildServiceAndController(JCodeModel codeModel,
                                                  String resourceName,
                                                  JPackage rootPackage,
                                                  String repositoryFieldName,
                                                  JDefinedClass modelClass,
                                                  JDefinedClass repositoryInterface) throws JClassAlreadyExistsException {
        JPackage controllerPackage = rootPackage.subPackage("controller");
        JPackage servicePackage = rootPackage.subPackage("service");
        JClass rawListClass = codeModel.ref(List.class);
        JClass modelListClass = rawListClass.narrow(modelClass);

        String resourceNameCapital = getModifiedResourceName(resourceName, Modifier.CAPITAL);
        String resourceNameCapitalPlural = getModifiedResourceName(resourceName, Modifier.CAPITALPLURAL);
        String resourceNameLower = getModifiedResourceName(resourceName, Modifier.LOWER);
        String resourceNameLowerPlural = getModifiedResourceName(resourceName, Modifier.LOWERPLURAL);

        String serviceName = resourceNameCapital.concat("Service");
        String serviceNameLower = resourceNameLower.concat("Service");
        JDefinedClass serviceInterface = servicePackage._interface(serviceName);
        JDefinedClass serviceClass = servicePackage._class(serviceName.concat("Impl"));
        serviceClass._implements(serviceInterface);
        serviceClass.annotate(Service.class);

        // add repository field to serviceClass
        JFieldVar wiredRepo = serviceClass.field(JMod.PRIVATE, repositoryInterface, repositoryFieldName);
        wiredRepo.annotate(Autowired.class);

        String getPrefix = "get";

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
    }

    private static final Class getType(Object providedTypeInfo) {
        String providedTypeInfoString = (String) providedTypeInfo;

        switch (providedTypeInfoString.toLowerCase()) {
            case "string":
                return String.class;
            case "int":
                return Integer.class;
        }
        return Object.class;
    }

    private static final String getModifiedResourceName(String resourceName, Modifier modifier) {

        String modifiedResourceName = resourceName;

        switch (modifier) {
            case CAPITAL:
                modifiedResourceName = StringUtils.capitalize(resourceName);
                break;
            case CAPITALPLURAL:
                modifiedResourceName = StringUtils.capitalize(resourceName).concat("s");
                break;
            case LOWER:
                modifiedResourceName = StringUtils.lowerCase(resourceName);
                break;
            case LOWERPLURAL:
                modifiedResourceName = StringUtils.lowerCase(resourceName).concat("s");
        }

        return modifiedResourceName;
    }

    private enum Modifier {
        LOWER, LOWERPLURAL, CAPITAL, CAPITALPLURAL
    }
}
