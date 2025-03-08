package com.liveramp.ts.livyramp;


import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.liveramp.ts.livyramp.mapper")
@EnableScheduling
@ComponentScan(value = {"com.liveramp.ts"})
@Slf4j
public class LivyRampApplication {

    public static void main(String[] args) {
        SpringApplication.run(LivyRampApplication.class, args);
    }




}
