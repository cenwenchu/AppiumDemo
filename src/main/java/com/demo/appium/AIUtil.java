package com.demo.appium;

import java.util.List;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

public class AIUtil {

    static String API_Key = "sk-9e627e4006a1489ca50c998ac1579e9b";
    static String Base_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";


    public static String callAIModel(List<ChatCompletionContentPart> arrayOfContentParts,AIModel aiModel,boolean isStream)
    {
        return callAIModel(arrayOfContentParts, aiModel,API_Key,Base_URL,isStream);
    }

    public static String callAIModel(List<ChatCompletionContentPart> arrayOfContentParts,AIModel aiModel,String api_key,String base_url,boolean isStream)
    {
        String result = "";

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(api_key)
                .baseUrl(base_url) 
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
