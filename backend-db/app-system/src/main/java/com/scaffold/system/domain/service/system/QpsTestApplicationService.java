package com.scaffold.system.domain.service.system;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.system.domain.entity.rbac.User;
import com.scaffold.system.domain.query.rbac.UserQuery;
import com.scaffold.system.domain.repository.rbac.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * QPS 测试接口应用服务。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-29
 */
@Service
@RequiredArgsConstructor
public class QpsTestApplicationService {

    private static final int MEDIUM_SIZE = 20;
    private static final int HEAVY_PAYLOAD_SIZE_BYTES = 50 * 1024;

    private final UserRepository userRepository;

    public Map<String, Object> ping() {
        Map<String, Object> data = baseResponse();
        data.put("server", hostname());
        data.put("version", version());
        return data;
    }

    public Map<String, Object> medium() {
        PageDTO<User> userPage = userRepository.queryUserPage(1, MEDIUM_SIZE, new UserQuery());
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> data = baseResponse(requestId);
        data.put("random", ThreadLocalRandom.current().nextInt(1, 1_000_000));
        addUserPageSummary(data, userPage);
        data.put("products", products(MEDIUM_SIZE));
        return data;
    }

    public Map<String, Object> heavy() {
        PageDTO<User> userPage = userRepository.queryUserPage(1, MEDIUM_SIZE, new UserQuery());
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> data = baseResponse(requestId);
        addUserPageSummary(data, userPage);
        data.put("checksum", checksum());
        data.put("payload", payload());
        return data;
    }

    private Map<String, Object> baseResponse() {
        return baseResponse(UUID.randomUUID().toString());
    }

    private Map<String, Object> baseResponse(String requestId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", LocalDateTime.now());
        data.put("requestId", requestId);
        data.put("hotStore", "db");
        return data;
    }

    private void addUserPageSummary(Map<String, Object> data, PageDTO<User> userPage) {
        data.put("userTotal", userPage == null ? 0 : userPage.getTotal());
        data.put("userRecordSize", userPage == null || userPage.getRecords() == null ? 0 : userPage.getRecords().size());
    }

    private List<Map<String, Object>> products(int size) {
        List<Map<String, Object>> products = new ArrayList<>(size);
        for (int i = 1; i <= size; i++) {
            Map<String, Object> product = new LinkedHashMap<>();
            product.put("id", i);
            product.put("code", "QPS-PRODUCT-" + i);
            product.put("name", "QPS Test Product " + i);
            product.put("price", 100 + i);
            product.put("stock", 1_000 - i);
            product.put("category", "benchmark");
            products.add(product);
        }
        return products;
    }

    private long checksum() {
        long checksum = 0;
        for (int i = 1; i <= 10_000; i++) {
            checksum = checksum * 31 + i;
        }
        return checksum;
    }

    private String payload() {
        return "x".repeat(HEAVY_PAYLOAD_SIZE_BYTES);
    }

    private String hostname() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isBlank()) {
            return hostname;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "unknown";
        }
    }

    private String version() {
        String implementationVersion = getClass().getPackage().getImplementationVersion();
        return implementationVersion == null || implementationVersion.isBlank()
                ? "0.0.1-SNAPSHOT"
                : implementationVersion;
    }
}
