package com.example.project1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 电商订单与库存管理系统的主应用类。
 *
 * 此类作为Spring Boot应用的入口点。它引导应用初始化Spring上下文、扫描组件并启动嵌入式Web服务器。
 *
 * {@code @SpringBootApplication}注解是一个便利注解，它组合了：
 * - {@code @Configuration}：将此类标记为应用上下文的bean定义源。
 * - {@code @EnableAutoConfiguration}：启用Spring Boot的自动配置机制，根据类路径和现有配置自动配置bean。
 * - {@code @ComponentScan}：启用组件扫描，允许Spring在指定包及其子包中查找和注册bean。
 * <p>
 * 此设置使应用能够以最小的配置运行，利用Spring Boot的约定优于配置方法。
 */
@SpringBootApplication
@EnableScheduling
public class Project1Application {

    /**
     * 应用的JVM入口点。
     *
     * 此方法在应用启动时被调用。它使用{@link SpringApplication#run(Class, String[])}
     * 来引导Spring Boot应用，该过程执行以下关键操作：
     * - 创建{@link SpringApplication}实例。
     * - 运行应用，初始化Spring上下文。
     * - 在配置的端口上启动嵌入式Web服务器（默认为Tomcat）。
     * - 加载并应用application.properties或application.yml中的配置。
     * - 初始化数据库连接、缓存和其他基础设施组件。
     *
     * @param args 启动时传入的命令行参数。这些可以用来覆盖默认配置，
     *             例如{@code --spring.profiles.active=dev}来激活特定配置文件，或
     *             {@code --server.port=8081}来更改服务器端口。
     */
    public static void main(String[] args) {
        // 启动嵌入式容器（默认为Tomcat）并初始化Spring上下文。
        SpringApplication.run(Project1Application.class, args);
    }
}
