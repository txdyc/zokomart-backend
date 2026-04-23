package com.zokomart.backend.web;

import com.zokomart.backend.common.api.RouteListResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/routes")
    public RouteListResponse routes() {
        return new RouteListResponse(List.of(
                "/products",
                "/cart",
                "/orders",
                "/merchant/orders"
        ));
    }

    @GetMapping("/stack")
    public Map<String, String> stack() {
        return Map.of(
                "backendFramework", "Spring Boot 3.5.13",
                "persistenceFramework", "MyBatis-Plus",
                "authFramework", "Sa-Token",
                "cache", "Redis",
                "search", "Elasticsearch"
        );
    }
}
