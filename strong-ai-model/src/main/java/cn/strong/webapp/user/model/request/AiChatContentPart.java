package cn.strong.webapp.user.model.request;

import java.io.Serializable;
import lombok.Data;

/** 服务端内部使用的多模态消息内容块。 */
@Data
public class AiChatContentPart implements Serializable {

    private String type;
    private String text;
    private String imageUrl;

    public static AiChatContentPart text(String value) {
        AiChatContentPart part = new AiChatContentPart();
        part.setType("text");
        part.setText(value);
        return part;
    }

    public static AiChatContentPart image(String dataUrl) {
        AiChatContentPart part = new AiChatContentPart();
        part.setType("image_url");
        part.setImageUrl(dataUrl);
        return part;
    }
}
