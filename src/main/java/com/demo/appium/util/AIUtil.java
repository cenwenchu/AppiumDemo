package com.demo.appium.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.demo.appium.AIModel;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartImage;
import com.openai.models.chat.completions.ChatCompletionContentPartText;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

public class AIUtil {

    // 阿里云API密钥
    static String Aliyun_Key = "";
    // DeepSeek API密钥
    static String DS_Key = "";

    /**
     * 构建ChatCompletionContentParts列表
     * @param textParts 文本内容列表
     * @param imageUrls 图片URL列表
     * @return 包含文本和图片的ChatCompletionContentParts列表
     */
    public static List<ChatCompletionContentPart> buildChatCompletionContentParts(List<String> textParts, List<String> imageUrls)
    {

        List<ChatCompletionContentPart> arrayOfContentParts = new ArrayList<ChatCompletionContentPart>();

        // 循环处理文本部分
        if (textParts != null) {
            for (String text : textParts) {
                arrayOfContentParts.add(ChatCompletionContentPart
                        .ofText(ChatCompletionContentPartText.builder().text(text).build()));
            }
        }

        // 循环处理图片URL
        if (imageUrls != null) {
            for (String imageUrl : imageUrls) {
                ChatCompletionContentPartImage imageUrlPart = ChatCompletionContentPartImage.builder()
                        .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder().url(imageUrl).build()).build();
                arrayOfContentParts.add(ChatCompletionContentPart.ofImageUrl(imageUrlPart));
            }
        }

        return arrayOfContentParts;
    }

    /**
     * 调用AI模型
     * @param arrayOfContentParts 输入内容列表
     * @param aiModel 使用的AI模型
     * @param isStream 是否使用流式响应
     * @return AI模型的响应结果
     */
    public static String callAIModel(List<ChatCompletionContentPart> arrayOfContentParts,AIModel aiModel,boolean isStream)
    {
        // 如果API密钥未加载，则从配置文件加载
        if (Aliyun_Key.equals("") || DS_Key.equals(""))
        {
            try (InputStream input = AIUtil.class.getResourceAsStream("/AIConfig.res")) {
                Properties prop = new Properties();
                prop.load(input);
                Aliyun_Key = prop.getProperty("aliyun_key");
                DS_Key = prop.getProperty("dk-key");
            } catch (IOException ex) {
                Logger.getLogger(AIUtil.class.getName()).log(Level.SEVERE, "加载AI配置时发生异常", ex);
            }
        }

        // 根据模型选择对应的API密钥
        if (aiModel.equals(AIModel.DEEP_SEEK))
            return callAIModel(arrayOfContentParts, aiModel,DS_Key,isStream);
        else
            return callAIModel(arrayOfContentParts, aiModel,Aliyun_Key,isStream);
    }

    /**
     * 调用AI模型的具体实现
     * @param arrayOfContentParts 输入内容列表
     * @param aiModel 使用的AI模型
     * @param api_key API密钥
     * @param isStream 是否使用流式响应
     * @return AI模型的响应结果
     */
    public static String callAIModel(List<ChatCompletionContentPart> arrayOfContentParts,AIModel aiModel,String api_key,boolean isStream)
    {
        String result = "";

        // 创建OpenAI客户端
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(api_key)
                .baseUrl(aiModel.getApiUrl()) 
                .build();

        // 构建请求参数
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessageOfArrayOfContentParts(arrayOfContentParts)
                .model(aiModel.getModelName())
                .build();

        client.chat().completions().createStreaming(params);

        // 处理流式响应
        if (isStream)
        {
            StringBuilder resultBuilder = new StringBuilder(); // 使用 StringBuilder 拼接字符串

            try (StreamResponse<ChatCompletionChunk> streamResponse = client.chat().completions().createStreaming(params)) {
                streamResponse.stream().forEach(chunk -> {
                    // 获取 chunk 中的内容并追加到 resultBuilder
                    chunk.choices().forEach(choice -> {
                        choice.delta().content().ifPresent(resultBuilder::append); // 拼接内容
                    });
                    
                });

                result = resultBuilder.toString();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            // 处理非流式响应
            ChatCompletion chatCompletion = client.chat().completions().create(params);

            result = chatCompletion.choices().get(0).message().content().orElse("");      
        }

        return result;
    }
}
