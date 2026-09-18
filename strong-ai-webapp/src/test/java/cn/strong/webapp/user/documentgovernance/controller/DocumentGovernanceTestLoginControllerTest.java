package cn.strong.webapp.user.documentgovernance.controller;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.documentgovernance.config.DocumentGovernanceTestLoginProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentGovernanceTestLoginControllerTest {

    @Test
    void exposesEnabledFlagWhenTestLoginIsEnabled() {
        DocumentGovernanceTestLoginProperties properties = new DocumentGovernanceTestLoginProperties();
        properties.setEnabled(true);

        Result<Map<String, Boolean>> result = new DocumentGovernanceTestLoginController(properties).config();

        assertEquals(Boolean.TRUE, result.getData().get("enabled"));
        assertEquals(1, result.getData().size());
    }

    @Test
    void defaultsTestLoginToDisabled() {
        Result<Map<String, Boolean>> result = new DocumentGovernanceTestLoginController(
                new DocumentGovernanceTestLoginProperties()).config();

        assertEquals(Boolean.FALSE, result.getData().get("enabled"));
    }
}
