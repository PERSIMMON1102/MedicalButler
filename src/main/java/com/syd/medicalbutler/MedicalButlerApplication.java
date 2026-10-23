package com.syd.medicalbutler;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.syd.medicalbutler.mapper")
public class MedicalButlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalButlerApplication.class, args);
    }

}
