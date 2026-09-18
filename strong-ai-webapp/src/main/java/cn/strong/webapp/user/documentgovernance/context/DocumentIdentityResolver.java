package cn.strong.webapp.user.documentgovernance.context;

import cn.strong.common.exception.ParamValiateException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class DocumentIdentityResolver {

    public static final String ATTRIBUTE_NAME = "documentIdentity";

    public DocumentIdentity current() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            throw new ParamValiateException("缺少文档治理身份上下文");
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        Object identity = request.getAttribute(ATTRIBUTE_NAME);
        if (!(identity instanceof DocumentIdentity)) {
            throw new ParamValiateException("缺少文档治理身份上下文");
        }
        return (DocumentIdentity) identity;
    }
}
