package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.webapp.user.documentgovernance.entity.OperationLogEntity;
import cn.strong.webapp.user.documentgovernance.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

@Service
public class DocumentOperationLogService {

    private final OperationLogMapper operationLogMapper;

    public DocumentOperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void record(String teamId, Long documentId, String actorUserId, String action, String metadataJson) {
        OperationLogEntity log = new OperationLogEntity();
        log.setTeamId(teamId);
        log.setDocumentId(documentId);
        log.setActorUserId(actorUserId);
        log.setAction(action);
        log.setMetadataJson(metadataJson == null || metadataJson.trim().isEmpty() ? "{}" : metadataJson);
        log.setCreatedAt(System.currentTimeMillis());
        operationLogMapper.insert(log);
    }
}
