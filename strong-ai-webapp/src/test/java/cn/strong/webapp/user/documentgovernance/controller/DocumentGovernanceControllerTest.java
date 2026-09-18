package cn.strong.webapp.user.documentgovernance.controller;

import cn.strong.webapp.user.documentgovernance.service.DocumentService;
import cn.strong.webapp.user.model.documentgovernance.DocumentCreateRequest;
import cn.strong.webapp.user.model.documentgovernance.DocumentResponse;
import cn.strong.webapp.user.model.documentgovernance.DocumentSaveRequest;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentGovernanceControllerTest {

    @Test
    void createsDocumentThroughDocumentService() throws Exception {
        DocumentService service = mock(DocumentService.class);
        DocumentGovernanceController controller = new DocumentGovernanceController(service);
        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setTitle("部署说明");
        request.setDocumentType("DEPLOYMENT");
        request.setContent("# 部署");
        DocumentResponse response = new DocumentResponse();
        response.setId(1L);
        when(service.create(request)).thenReturn(response);

        Object result = controller.create(request);

        assertSame(response, data(result));
    }

    @Test
    void savesDraftThroughDocumentService() throws Exception {
        DocumentService service = mock(DocumentService.class);
        DocumentGovernanceController controller = new DocumentGovernanceController(service);
        DocumentSaveRequest request = new DocumentSaveRequest();
        request.setContent("# 已更新");
        DocumentResponse response = new DocumentResponse();
        response.setId(1L);
        when(service.saveDraft(1L, request)).thenReturn(response);

        Object result = controller.saveDraft(1L, request);

        assertEquals(1L, ((DocumentResponse) data(result)).getId());
    }

    private Object data(Object result) throws Exception {
        Field field = result.getClass().getDeclaredField("data");
        field.setAccessible(true);
        return field.get(result);
    }
}
