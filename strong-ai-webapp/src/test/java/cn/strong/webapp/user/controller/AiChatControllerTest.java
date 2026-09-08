package cn.strong.webapp.user.controller;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.SpringBootTests;
import cn.strong.webapp.user.model.request.AiChatMessageRequest;
import cn.strong.webapp.user.model.request.AiChatRequest;
import cn.strong.webapp.user.model.response.AiChatResponse;
import com.alibaba.fastjson2.JSON;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import org.junit.jupiter.api.Test;

class AiChatControllerTest extends SpringBootTests {

    @Resource
    private AiChatController aiChatController;


    @Test
    void chat() {
        AiChatRequest request = new AiChatRequest();
        request.setProvider("deepseek");
        request.setModel("deepseek-v4-flash");
        List<AiChatMessageRequest> messages = new ArrayList<>();
        messages.add(new AiChatMessageRequest("user", "请介绍一下你自己"));
        request.setMessages(messages);
        request.setTemperature(0.7f);
        request.setTopP(0.9f);
        request.setMaxTokens(393216);
        Result<AiChatResponse> result = aiChatController.chat(request);
        System.out.println(JSON.toJSONString(result));
    }
}