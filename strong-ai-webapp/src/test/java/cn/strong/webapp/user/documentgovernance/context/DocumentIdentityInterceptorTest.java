package cn.strong.webapp.user.documentgovernance.context;

import cn.strong.common.exception.ParamValiateException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DocumentIdentityInterceptorTest {

    private DocumentIdentityInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new DocumentIdentityInterceptor();
    }

    @Test
    void resolvesTrustedGatewayIdentity() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Strong-User-Id", "u-1");
        request.addHeader("X-Strong-Team-Id", "team-a");
        request.addHeader("X-Strong-Role", "admin");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        DocumentIdentity identity = (DocumentIdentity) request.getAttribute("documentIdentity");
        assertEquals("u-1", identity.getUserId());
        assertEquals("team-a", identity.getTeamId());
        assertTrue(identity.isAdmin());
    }

    @Test
    void rejectsMissingOrUnsupportedGatewayRole() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Strong-User-Id", "u-1");
        request.addHeader("X-Strong-Team-Id", "team-a");
        request.addHeader("X-Strong-Role", "owner");

        assertThrows(ParamValiateException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
