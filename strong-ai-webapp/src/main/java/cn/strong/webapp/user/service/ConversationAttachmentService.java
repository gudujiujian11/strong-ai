package cn.strong.webapp.user.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.conversation.ConversationAttachmentData;
import cn.strong.webapp.user.conversation.ConversationAttachmentStore;
import cn.strong.webapp.user.conversation.ConversationStore;
import cn.strong.webapp.user.model.conversation.Conversation;
import cn.strong.webapp.user.model.conversation.ConversationAttachment;
import cn.strong.webapp.user.model.conversation.ConversationMessage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ConversationAttachmentService {

    public static final long MAX_SIZE = 10L * 1024L * 1024L;
    private static final long UNBOUND_TTL = 24L * 60L * 60L * 1000L;
    private static final int MAX_DIMENSION = 12000;
    private static final long MAX_PIXELS = 40_000_000L;

    private final ConversationStore conversationStore;
    private final ConversationAttachmentStore attachmentStore;

    public ConversationAttachmentService(ConversationStore conversationStore,
                                         ConversationAttachmentStore attachmentStore) {
        this.conversationStore = conversationStore;
        this.attachmentStore = attachmentStore;
    }

    public ConversationAttachment upload(String conversationId, MultipartFile file) {
        requireConversation(conversationId);
        if (file == null || file.isEmpty()) {
            throw new ParamValiateException("图片文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ParamValiateException("图片大小不能超过 10 MB");
        }
        String name = normalizeName(file.getOriginalFilename());
        try {
            byte[] content = file.getBytes();
            String contentType = detectContentType(content, file.getContentType());
            ConversationAttachment attachment = new ConversationAttachment();
            attachment.setId(UUID.randomUUID().toString());
            attachment.setConversationId(conversationId);
            attachment.setName(name);
            attachment.setContentType(contentType);
            attachment.setSize(content.length);
            attachment.setExpiresAt(System.currentTimeMillis() + UNBOUND_TTL);
            return decorate(attachmentStore.save(attachment, content));
        } catch (IOException ex) {
            throw new IllegalStateException("读取图片失败", ex);
        }
    }

    public ConversationAttachmentData read(String conversationId, String attachmentId) {
        requireConversation(conversationId);
        return attachmentStore.find(conversationId, attachmentId)
                .orElseThrow(() -> new ParamValiateException("附件不存在或不属于当前会话"));
    }

    public boolean deleteUnbound(String conversationId, String attachmentId) {
        requireConversation(conversationId);
        return attachmentStore.deleteUnbound(conversationId, attachmentId);
    }

    public List<ConversationAttachment> validateUnbound(String conversationId, List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        if (attachmentIds.size() > 1) {
            throw new ParamValiateException("每条消息最多上传一张图片");
        }
        for (String attachmentId : attachmentIds) {
            if (attachmentId == null || attachmentId.trim().isEmpty()) {
                throw new ParamValiateException("附件 ID 无效");
            }
        }
        List<ConversationAttachment> attachments = attachmentStore.findUnbound(
                conversationId, attachmentIds, System.currentTimeMillis());
        if (attachments.size() != attachmentIds.size()) {
            throw new ParamValiateException("附件不存在、已过期、已绑定或不属于当前会话");
        }
        return decorateAll(attachments);
    }

    public void bind(String conversationId, String messageId, List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        int count = attachmentStore.bind(conversationId, messageId, attachmentIds, System.currentTimeMillis());
        if (count != attachmentIds.size()) {
            throw new ParamValiateException("附件已被其他请求占用或已过期");
        }
    }

    public void enrich(Conversation conversation) {
        enrich(Collections.singletonList(conversation));
    }

    public void enrich(List<Conversation> conversations) {
        if (conversations == null || conversations.isEmpty()) {
            return;
        }
        List<String> ids = new ArrayList<>();
        for (Conversation conversation : conversations) {
            ids.add(conversation.getId());
            for (ConversationMessage message : conversation.getMessages()) {
                message.setAttachments(new ArrayList<ConversationAttachment>());
            }
        }
        Map<String, List<ConversationAttachment>> byMessage = new HashMap<>();
        for (ConversationAttachment attachment : attachmentStore.findByConversationIds(ids)) {
            decorate(attachment);
            if (attachment.getMessageId() != null) {
                byMessage.computeIfAbsent(attachment.getMessageId(), ignored -> new ArrayList<>()).add(attachment);
            }
        }
        for (Conversation conversation : conversations) {
            for (ConversationMessage message : conversation.getMessages()) {
                List<ConversationAttachment> attachments = byMessage.get(message.getId());
                if (attachments != null) {
                    message.setAttachments(attachments);
                }
            }
        }
    }

    public String toDataUrl(String conversationId, String attachmentId) {
        ConversationAttachmentData data = read(conversationId, attachmentId);
        return "data:" + data.getMetadata().getContentType() + ";base64,"
                + Base64.getEncoder().encodeToString(data.getContent());
    }

    public int deleteByConversationId(String conversationId) {
        return attachmentStore.deleteByConversationId(conversationId);
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000L)
    public void cleanupExpired() {
        attachmentStore.deleteExpired(System.currentTimeMillis());
    }

    private void requireConversation(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()
                || !conversationStore.findById(conversationId).isPresent()) {
            throw new ParamValiateException("会话不存在或已过期");
        }
    }

    private String normalizeName(String originalName) {
        String name = originalName == null ? "image" : originalName.trim();
        if (name.isEmpty()) {
            name = "image";
        }
        name = name.replace('\\', '_').replace('/', '_');
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private String detectContentType(byte[] content, String declaredType) {
        if (isPng(content)) {
            validateDecodedImage(content, "PNG");
            return "image/png";
        }
        if (isJpeg(content)) {
            validateDecodedImage(content, "JPEG");
            return "image/jpeg";
        }
        if (isWebp(content)) {
            return "image/webp";
        }
        throw new ParamValiateException("仅支持 PNG、JPEG 或 WebP 图片");
    }

    private void validateDecodedImage(byte[] content, String format) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0
                    || image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION
                    || (long) image.getWidth() * image.getHeight() > MAX_PIXELS) {
                throw new ParamValiateException(format + " 图片尺寸或内容无效");
            }
        } catch (IOException ex) {
            throw new ParamValiateException(format + " 图片内容无效");
        }
    }

    private boolean isPng(byte[] content) {
        return content.length >= 8 && content[0] == (byte) 0x89 && content[1] == 0x50
                && content[2] == 0x4E && content[3] == 0x47 && content[4] == 0x0D
                && content[5] == 0x0A && content[6] == 0x1A && content[7] == 0x0A;
    }

    private boolean isJpeg(byte[] content) {
        return content.length >= 3 && (content[0] & 0xFF) == 0xFF
                && (content[1] & 0xFF) == 0xD8 && (content[2] & 0xFF) == 0xFF;
    }

    private boolean isWebp(byte[] content) {
        return content.length >= 12 && content[0] == 'R' && content[1] == 'I'
                && content[2] == 'F' && content[3] == 'F' && content[8] == 'W'
                && content[9] == 'E' && content[10] == 'B' && content[11] == 'P';
    }

    private List<ConversationAttachment> decorateAll(List<ConversationAttachment> attachments) {
        for (ConversationAttachment attachment : attachments) {
            decorate(attachment);
        }
        return attachments;
    }

    private ConversationAttachment decorate(ConversationAttachment attachment) {
        attachment.setStatus(attachment.getMessageId() == null ? "UPLOADED" : "BOUND");
        attachment.setPreviewUrl("/api/conversations/" + attachment.getConversationId()
                + "/attachments/" + attachment.getId());
        return attachment;
    }
}
