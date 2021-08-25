package com.company;

import com.company.Main.*;

@Bean
public class MyDaoImpl implements MyDao{
    @Init
    public void init(){
        System.out.println("MyDao Created");
    }

    @Override
    public void test() {
        System.out.println("MyDao: test");
    }
}
