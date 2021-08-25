package com.company;

import java.beans.ConstructorProperties;
import java.lang.annotation.*;
import java.util.Objects;

public class Main {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Bean {
    }

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Inject {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Init {

    }

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ApplicationContext(Main.class);
        MyService myService = (MyService) applicationContext.getBeanByName("MyService");
        myService.test();
        applicationContext.displayAllBeans();
//        CustomInjector.startApplication(Main.class);
//        CustomInjector.getService(MyService.class).test();
    }
}
