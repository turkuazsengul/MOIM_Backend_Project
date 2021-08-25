package com.company;

import org.reflections.Reflections;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.*;
import java.util.*;
import java.util.stream.Collectors;

import com.company.Main.*;

import javax.management.RuntimeErrorException;

public class CustomInjector {

    private Map<Class<?>, Class<?>> diMap;
    private Map<Class<?>, Object> applicationScope;
    private static CustomInjector injector;

    private CustomInjector() {
        super();
        diMap = new HashMap<>();
        applicationScope = new HashMap<>();
    }

    public static void startApplication(Class<?> mainClass) {
        try {
            synchronized (CustomInjector.class) {
                if (injector == null) {
                    injector = new CustomInjector();
                    injector.initFramework(mainClass);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static <T> T getService(Class<T> classz) {
        try {
            return injector.getBeanInstance(classz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initFramework(Class<?> mainClass)
        throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
        Class<?>[] classes = ClassLoaderUtil.getClasses(mainClass.getPackage().getName());
        Reflections reflections = new Reflections(mainClass.getPackage().getName());
        Set<Class<?>> types = reflections.getTypesAnnotatedWith(Bean.class);
        for (Class<?> implementationClass : types) {
            Class<?>[] interfaces = implementationClass.getInterfaces();
            if (interfaces.length == 0) {
                diMap.put(implementationClass, implementationClass);
            } else {
                for (Class<?> iface : interfaces) {
                    diMap.put(implementationClass, iface);
                }
            }
        }
        for (Class<?> classz : classes) {
            if (classz.isAnnotationPresent(Bean.class)) {
                Object classInstance = classz.newInstance();
                applicationScope.put(classz, classInstance);
                InjectionUtil.autowire(this, classz, classInstance);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getBeanInstance(Class<T> interfaceClass) throws InstantiationException, IllegalAccessException {
        return (T) getBeanInstance(interfaceClass, null, null);
    }
    public <T> Object getBeanInstance(Class<T> interfaceClass, String fieldName, String qualifier)
        throws InstantiationException, IllegalAccessException {
        Class<?> implementationClass = getImplimentationClass(interfaceClass, fieldName, qualifier);
        if (applicationScope.containsKey(implementationClass)) {
            return applicationScope.get(implementationClass);
        }
        synchronized (applicationScope) {
            Object service = implementationClass.newInstance();
            applicationScope.put(implementationClass, service);
            return service;
        }
    }

    private Class<?> getImplimentationClass(Class<?> interfaceClass, final String fieldName, final String qualifier) {
        Set<Entry<Class<?>, Class<?>>> implementationClasses = diMap.entrySet().stream()
                .filter(entry -> entry.getValue() == interfaceClass).collect(Collectors.toSet());
        String errorMessage = "";
        if (implementationClasses == null || implementationClasses.size() == 0) {
            errorMessage = "no implementation found for interface " + interfaceClass.getName();
        } else if (implementationClasses.size() == 1) {
            Optional<Entry<Class<?>, Class<?>>> optional = implementationClasses.stream().findFirst();
            if (optional.isPresent()) {
                return optional.get().getKey();
            }
        } else if (implementationClasses.size() > 1) {
            final String findBy = (qualifier == null || qualifier.trim().length() == 0) ? fieldName : qualifier;
            Optional<Entry<Class<?>, Class<?>>> optional = implementationClasses.stream()
                    .filter(entry -> entry.getKey().getSimpleName().equalsIgnoreCase(findBy)).findAny();
            if (optional.isPresent()) {
                return optional.get().getKey();
            } else {
                errorMessage = "There are " + implementationClasses.size() + " of interface " + interfaceClass.getName()
                        + " Expected single implementation or make use of @CustomQualifier to resolve conflict";
            }
        }
        throw new RuntimeErrorException(new Error(errorMessage));
    }
}
