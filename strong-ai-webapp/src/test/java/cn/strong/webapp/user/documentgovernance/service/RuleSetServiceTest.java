package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentity;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.RuleSetEntity;
import cn.strong.webapp.user.documentgovernance.mapper.RuleSetMapper;
import cn.strong.webapp.user.model.documentgovernance.QualitySeverity;
import cn.strong.webapp.user.model.documentgovernance.RuleConfig;
import cn.strong.webapp.user.model.documentgovernance.RuleSetRequest;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleSetServiceTest {

    private RuleSetMapper ruleSetMapper;
    private RuleSetService service;

    @BeforeEach
    void setUp() {
        ruleSetMapper = mock(RuleSetMapper.class);
        service = new RuleSetService(ruleSetMapper, new DocumentIdentityResolver());
        setIdentity("member");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void returnsDefaultConfigWhenTeamHasNoRuleSet() {
        when(ruleSetMapper.selectEnabledByTeamId("team-a")).thenReturn(Collections.emptyList());
        RuleConfig config = service.effectiveConfig("team-a");
        assertTrue(config.isEnabled("FORBIDDEN_TERM"));
        assertEquals(30, config.weight(QualitySeverity.HIGH));
    }

    @Test
    void malformedConfigFallsBackToDefaultsWithoutThrowing() {
        RuleSetEntity broken = ruleSet("{not valid json");
        when(ruleSetMapper.selectEnabledByTeamId("team-a")).thenReturn(Collections.singletonList(broken));
        RuleConfig config = service.effectiveConfig("team-a");
        assertTrue(config.isEnabled("HEADING_ORDER"));
        assertEquals(15, config.weight(QualitySeverity.MEDIUM));
    }

    @Test
    void appliesEnabledFlagSeverityAndWeightFromConfig() {
        RuleSetEntity ruleSet = ruleSet("{\"rules\":{\"FORBIDDEN_TERM\":{\"enabled\":false},"
                + "\"UNSAFE_LINK\":{\"severity\":\"LOW\"}},\"scoreWeights\":{\"HIGH\":50}}");
        when(ruleSetMapper.selectEnabledByTeamId("team-a")).thenReturn(Collections.singletonList(ruleSet));
        RuleConfig config = service.effectiveConfig("team-a");
        assertFalse(config.isEnabled("FORBIDDEN_TERM"));
        assertEquals(QualitySeverity.LOW, config.severity("UNSAFE_LINK", QualitySeverity.HIGH));
        assertEquals(50, config.weight(QualitySeverity.HIGH));
        // 未覆盖的权重仍走默认
        assertEquals(5, config.weight(QualitySeverity.LOW));
    }

    @Test
    void laterRuleSetOverridesEarlierOne() {
        RuleSetEntity earlier = ruleSet("{\"rules\":{\"UNSAFE_LINK\":{\"enabled\":true}}}");
        RuleSetEntity later = ruleSet("{\"rules\":{\"UNSAFE_LINK\":{\"enabled\":false}}}");
        // selectEnabledByTeamId 按 updated_at 升序返回
        when(ruleSetMapper.selectEnabledByTeamId("team-a")).thenReturn(Arrays.asList(earlier, later));
        RuleConfig config = service.effectiveConfig("team-a");
        assertFalse(config.isEnabled("UNSAFE_LINK"));
    }

    @Test
    void memberCannotUpsertRuleSet() {
        assertThrows(ParamValiateException.class,
                () -> service.upsert(new RuleSetRequest("default", "{}", true)));
    }

    @Test
    void adminCannotSaveMalformedConfig() {
        setIdentity("admin");
        assertThrows(ParamValiateException.class,
                () -> service.upsert(new RuleSetRequest("default", "{bad", true)));
    }

    private RuleSetEntity ruleSet(String configJson) {
        RuleSetEntity entity = new RuleSetEntity();
        entity.setTeamId("team-a");
        entity.setName("default");
        entity.setConfigJson(configJson);
        entity.setEnabled(true);
        return entity;
    }

    private void setIdentity(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(DocumentIdentityResolver.ATTRIBUTE_NAME,
                new DocumentIdentity("u-1", "team-a", role));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
