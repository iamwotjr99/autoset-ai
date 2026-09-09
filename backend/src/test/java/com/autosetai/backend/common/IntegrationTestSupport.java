package com.autosetai.backend.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // 테스트 종료 시 자동 롤백
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    protected ObjectMapper objectMapper = new ObjectMapper();

    // MySQL 8.0 컨테이너 띄우기
    static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("autoset_test")
            .withUsername("root")
            .withPassword("password");

    // Redis 컨테이너 띄우기
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:6-alpine"))
            .withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
    }

    // 띄워진 컨테이너의 랜덤 포트/접속 정보를 Spring Boot 환경 변수에 덮어쓰기
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);

        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
    }
}