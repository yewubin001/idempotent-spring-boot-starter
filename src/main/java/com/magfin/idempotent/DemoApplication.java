package com.magfin.idempotent;

import com.magfin.idempotent.annotation.Idempotent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.time.Instant;

@SpringBootApplication
@EnableConfigurationProperties
@RestController
public class DemoApplication {

    private Logger logger = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @PostMapping("/idempotent")
    @Idempotent(value = "print", express = "#user.name")
    public void print(@RequestBody User user) {
        logger.info("post请求当前时间：time={}, name={}", Instant.now(), user.getName());
    }

    static public class User implements Serializable {

        public User(){

        }

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
