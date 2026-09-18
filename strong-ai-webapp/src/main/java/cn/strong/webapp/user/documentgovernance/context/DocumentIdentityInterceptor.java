package cn.strong.webapp.user.documentgovernance.context;

import cn.strong.common.exception.ParamValiateException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class DocumentIdentityInterceptor implements HandlerInterceptor {

    private static final String USER_ID_HEADER = "X-Strong-User-Id";
    private static final String TEAM_ID_HEADER = "X-Strong-Team-Id";
    private static final String ROLE_HEADER = "X-Strong-Role";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String role = requiredHeader(request, ROLE_HEADER);
        if (!"member".equals(role) && !"admin".equals(role)) {
            throw new ParamValiateException("网关身份角色无效");
        }
        request.setAttribute(DocumentIdentityResolver.ATTRIBUTE_NAME, new DocumentIdentity(
                requiredHeader(request, USER_ID_HEADER), requiredHeader(request, TEAM_ID_HEADER), role));
        return true;
    }

    private String requiredHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.trim().isEmpty()) {
            throw new ParamValiateException("缺少网关身份信息: " + name);
        }
        return value.trim();
    }
}
