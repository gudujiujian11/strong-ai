package cn.strong.webapp.user.documentgovernance.controller;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.documentgovernance.config.DocumentGovernanceTestLoginProperties;
import java.util.Collections;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/document-governance/test-login")
public class DocumentGovernanceTestLoginController {

    private final DocumentGovernanceTestLoginProperties properties;

    public DocumentGovernanceTestLoginController(DocumentGovernanceTestLoginProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/config")
    public Result<Map<String, Boolean>> config() {
        return Result.ok(Collections.singletonMap("enabled", properties.isEnabled()));
    }
}
