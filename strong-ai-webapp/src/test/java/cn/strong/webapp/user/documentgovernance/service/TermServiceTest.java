package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentity;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.mapper.TermMapper;
import cn.strong.webapp.user.model.documentgovernance.TermRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class TermServiceTest {

    private TermService termService;

    @BeforeEach
    void setUp() {
        termService = new TermService(mock(TermMapper.class), new DocumentIdentityResolver());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(DocumentIdentityResolver.ATTRIBUTE_NAME,
                new DocumentIdentity("u-1", "team-a", "member"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void memberCannotChangeTeamTerminology() {
        assertThrows(ParamValiateException.class,
                () -> termService.upsert(new TermRequest("SDK", "SDK", "")));
    }
}
