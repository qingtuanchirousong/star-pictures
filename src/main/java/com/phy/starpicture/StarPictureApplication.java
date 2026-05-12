package com.phy.starpicture;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.phy.starpicture.mapper")
public class StarPictureApplication {

    public static void main(String[] args) {
        SpringApplication.run(StarPictureApplication.class, args);
    }

}
