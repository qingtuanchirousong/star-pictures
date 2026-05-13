package com.phy.starpicture;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@MapperScan("com.phy.starpicture.mapper")
@EnableCaching
public class StarPictureApplication {

    public static void main(String[] args) {
        SpringApplication.run(StarPictureApplication.class, args);
    }

}
