package com.company;
import com.company.Main.Bean;
import com.company.Main.Inject;

@Bean
public class MyService2 {
    @Inject
    private MyService3 myService3;

    public void test(){
        System.out.println("MyService2: test");
    }
}
