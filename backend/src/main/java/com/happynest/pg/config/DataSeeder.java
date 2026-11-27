package com.happynest.pg.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // Each owner creates their own building when they register.
    }
}
