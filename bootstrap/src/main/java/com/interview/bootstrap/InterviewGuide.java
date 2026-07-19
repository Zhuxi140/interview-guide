package com.interview.bootstrap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author zhuxi
 * @apiNote 项目启动入口
 */
@SpringBootApplication(scanBasePackages = "interview")
@MapperScan("interview.**.mapper")
@EnableScheduling
public class InterviewGuide {
    public static void main(String[] args) {
        SpringApplication.run(InterviewGuide.class, args);
    }
}
