package com.demo.appium;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotSame;
import org.junit.Test;

import com.demo.appium.util.AIUtil;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartImage;
import com.openai.models.chat.completions.ChatCompletionContentPartText;

/**
 * Unit test for simple App.
 */
public class AIUtilTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void callAIModelTest()
    {

        List<ChatCompletionContentPart> arrayOfContentParts = new ArrayList<ChatCompletionContentPart>();

        arrayOfContentParts.add(ChatCompletionContentPart.ofText(ChatCompletionContentPartText.builder().text("你认为人工智能最大的优势是什么？").build()));

        String result = AIUtil.callAIModel(arrayOfContentParts,AIModel.DEEP_SEEK,true);
        
        System.out.println(result);


        arrayOfContentParts = new ArrayList<ChatCompletionContentPart>();

        arrayOfContentParts.add(ChatCompletionContentPart.ofText(ChatCompletionContentPartText.builder().text("图片中的猫是什么品种的猫？").build()));

        String imgUrl = "https://p0.meituan.net/imgupload/0ff9ea17050bceb4a366dace0f7528237025166.jpg%40_100q%7Cwatermark%3D0";

        ChatCompletionContentPartImage imageUrl = ChatCompletionContentPartImage.builder().imageUrl(ChatCompletionContentPartImage.ImageUrl.builder().url(imgUrl).build()).build();      

        arrayOfContentParts.add(ChatCompletionContentPart.ofImageUrl(imageUrl));

        result = AIUtil.callAIModel(arrayOfContentParts,AIModel.QWEN_OMNI_TURBO,true);
        
        System.out.println(result);
        
        result = AIUtil.callAIModel(arrayOfContentParts,AIModel.QWEN_VL_PLUS,false);

        System.out.println(result);

        assertNotSame(result,"");
    }
}
