package com.demo.appium.util;

import java.io.IOException;
import java.io.InputStream;
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
import com.openai.models.chat.completions.ChatCompletionCreateParams;

public class AIUtil {

    static String Aliyun_Key = "";
    static String DS_Key = "";


    public static String callAIModel(List<ChatCompletionContentPart> arrayOfContentParts,AIModel aiModel,boolean isStream)
    {
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

        if (aiModel.equals(AIModel.DEEP_SEEK))
            return callAIModel(arrayOfContentParts, aiModel,DS_Key,isStream);
        else
            return callAIModel(arrayOfContentParts, aiModel,Aliyun_Key,isStream);
    }

    public static String callAIModel(List<ChatCompletionContentPart> arrayOfContentParts,AIModel aiModel,String api_key,boolean isStream)
    {
        String result = "";

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(api_key)
                .baseUrl(aiModel.getApiUrl()) 
                .build();


        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessageOfArrayOfContentParts(arrayOfContentParts)
                .model(aiModel.getModelName())
                .build();

        client.chat().completions().createStreaming(params);

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
            ChatCompletion chatCompletion = client.chat().completions().create(params);

            result = chatCompletion.choices().get(0).message().content().orElse("");      
        }

        return result;
    }



}
