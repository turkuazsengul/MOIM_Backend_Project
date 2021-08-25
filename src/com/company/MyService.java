package com.company;
import com.company.Main.Bean;
import com.company.Main.Inject;
import com.company.Main.Init;

@Bean
public class MyService {

    @Inject
    private MyDao myDao;

    @Inject
    private MyService2 myService2;

    @Init
    public void init(){
        System.out.println("MyService Created");
    }

    public void test(){
        System.out.println("\nMyService: test");
        myService2.test();

    }


}
