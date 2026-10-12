package cn.strong.webapp.user.mdoptimizer.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.mdoptimizer.entity.ImageParseHistoryEntity;
import cn.strong.webapp.user.mdoptimizer.mapper.ImageParseHistoryMapper;
import cn.strong.webapp.user.mdoptimizer.model.ImageParseHistory;
import cn.strong.webapp.user.mdoptimizer.model.ImageParseHistoryPage;
import cn.strong.webapp.user.mdoptimizer.model.MarkdownOptimizeResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ImageParseHistoryService {

    private final ImageParseHistoryMapper historyMapper;
    private final ObjectMapper objectMapper;

    public ImageParseHistoryService(ImageParseHistoryMapper historyMapper, ObjectMapper objectMapper) {
        this.historyMapper = historyMapper;
        this.objectMapper = objectMapper;
    }

    public void save(String originalName, String contentType, byte[] content, MarkdownOptimizeResponse result) {
        ImageParseHistoryEntity entity = new ImageParseHistoryEntity();
        entity.setId(result.getJobId());
        entity.setOriginalName(originalName);
        entity.setContentType(contentType);
        entity.setSizeBytes((long) content.length);
        entity.setContent(content);
        entity.setProvider(result.getProvider());
        entity.setModel(result.getModel());
        entity.setCreatedAt(System.currentTimeMillis());
        try {
            entity.setResultJson(objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化图片解析历史失败", ex);
        }
        if (historyMapper.insert(entity) != 1) {
            throw new IllegalStateException("保存图片解析历史失败");
        }
    }

    public ImageParseHistoryPage list(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ParamValiateException("页码必须大于 0，每页记录数必须在 1 到 100 之间");
        }
        long total = historyMapper.countAll();
        List<ImageParseHistory> items = new ArrayList<>();
        for (ImageParseHistoryEntity entity : historyMapper.findPage((page - 1L) * pageSize, pageSize)) {
            items.add(toHistory(entity));
        }
        ImageParseHistoryPage result = new ImageParseHistoryPage();
        result.setItems(items);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }

    public ImageParseHistory get(String id) {
        ImageParseHistoryEntity entity = requireHistory(historyMapper.findDetail(id));
        ImageParseHistory history = toHistory(entity);
        try {
            history.setResult(objectMapper.readValue(entity.getResultJson(), MarkdownOptimizeResponse.class));
        } catch (IOException ex) {
            throw new IllegalStateException("读取图片解析历史失败", ex);
        }
        return history;
    }

    public ImageParseHistoryEntity getImage(String id) {
        return requireHistory(historyMapper.findImage(id));
    }

    public void delete(String id) {
        if (historyMapper.deleteHistory(id) == 0) {
            throw new ParamValiateException("图片解析历史不存在或已被删除");
        }
    }

    private ImageParseHistoryEntity requireHistory(ImageParseHistoryEntity entity) {
        if (entity == null) {
            throw new ParamValiateException("图片解析历史不存在或已被删除");
        }
        return entity;
    }

    private ImageParseHistory toHistory(ImageParseHistoryEntity entity) {
        ImageParseHistory history = new ImageParseHistory();
        history.setId(entity.getId());
        history.setOriginalName(entity.getOriginalName());
        history.setContentType(entity.getContentType());
        history.setSizeBytes(entity.getSizeBytes());
        history.setProvider(entity.getProvider());
        history.setModel(entity.getModel());
        history.setCreatedAt(entity.getCreatedAt());
        return history;
    }
}
