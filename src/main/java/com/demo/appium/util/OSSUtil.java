package com.demo.appium.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.openqa.selenium.OutputType;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.demo.appium.AIModel;
import com.openai.models.chat.completions.ChatCompletionContentPart;

import io.appium.java_client.AppiumDriver;

/**
 * OSS工具类，用于处理与阿里云OSS相关的操作
 */
public class OSSUtil {

    // 阿里云OSS配置
    private static String ENDPOINT = ""; // 替换为你的OSS区域
    private static String ALIYUN_ACCESS_KEY_ID = ""; // 替换为你的AccessKey ID
    private static String ALIYUN_ACCESS_KEY_SECRET = ""; // 替换为你的AccessKey Secret
    private static String BUCKET_NAME = ""; // 替换为你的Bucket名称

    public static void main(String[] args) {
        String imgUrl = OSSUtil.uploadFileToOSS(new File("./screenshot_hk.png"), "screenshot_hk.png");



        List<String> textParts = new ArrayList<>();
        List<String> imageUrls = new ArrayList<>();

        textParts.add("将下面两张图片中的数据先独立整理成为两个表格,"
                    + "一个表格列为：股票名称,股票代码,持仓比例,变动股份，另一个表列为：股票名称,股票代码,变动比例,持股市值"
                    + ",然后将两个表格根据'股票名称'做关联，合并为一个表格，表格列为：'股票名称,股票代码,持仓比例,变动股份,变动比例,持股市值';"
                    + " 请不要返回合并前的两个表格,仅返回合并后的表格！");

        imageUrls.add(imgUrl);

        List<ChatCompletionContentPart> arrayOfContentParts = AIUtil.buildChatCompletionContentParts(textParts,
                    imageUrls);

        // String aiResponseString = AIUtil.callAIModel(arrayOfContentParts,
        // AIModel.QWEN_VL_PLUS, true);
        String aiResponseString = AIUtil.callAIModel(arrayOfContentParts, AIModel.QWEN_OMNI_TURBO, true);

        System.out.println(aiResponseString);
    }

    /**
     * 截屏并上传到OSS
     * @param driver Appium驱动实例
     * @param screenshotTempName 截图临时文件名
     * @return 上传后的文件URL
     */
    public static String captureAndUploadScreenshot(AppiumDriver driver,String screenshotTempName)
    {
        File screenshotFile = new File("./" + screenshotTempName);
        driver.getScreenshotAs(OutputType.FILE).renameTo(screenshotFile);
       
        return OSSUtil.uploadFileToOSS(screenshotFile, screenshotTempName);
    }
    
    /**
     * 上传文件到OSS
     * @param file 要上传的文件
     * @param objectName OSS中的对象名称
     * @return 上传后的文件URL
     */
    public static String uploadFileToOSS(File file, String objectName) {

        String resulString = "";

        if (ALIYUN_ACCESS_KEY_ID.isEmpty() || ALIYUN_ACCESS_KEY_SECRET.isEmpty()) {
            // 从AIConfig.res中读取配置
            Properties prop = new Properties();
            try (InputStream input = OSSUtil.class.getClassLoader().getResourceAsStream("AIConfig.res")) {
                if (input == null) {
                    System.err.println("无法找到AIConfig.res配置文件");
                    return resulString;
                }
                prop.load(input);
                ALIYUN_ACCESS_KEY_ID = prop.getProperty("ALIYUN_ACCESS_KEY_ID");
                ALIYUN_ACCESS_KEY_SECRET = prop.getProperty("ALIYUN_ACCESS_KEY_SECRET");
                ENDPOINT = prop.getProperty("ENDPOINT");
                BUCKET_NAME = prop.getProperty("BUCKET_NAME");

            } catch (Exception ex) {
                System.err.println("读取AIConfig.res配置文件失败：" + ex.getMessage());
                return resulString; 
            }
        }

        // 创建OSS客户端
        OSS ossClient = new OSSClientBuilder().build(ENDPOINT, ALIYUN_ACCESS_KEY_ID, ALIYUN_ACCESS_KEY_SECRET);

        try (InputStream inputStream = new FileInputStream(file)) {
            // 上传文件
            ossClient.putObject(BUCKET_NAME, objectName, inputStream);

            resulString = generatePublicUrl(objectName);
        } 
        catch(Exception ex)
        {
            System.err.println(ex.getMessage());
        }
        finally {
            // 关闭OSS客户端
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        return resulString;
    }

    /**
     * 生成公共访问URL
     * @param objectName OSS中的对象名称
     * @return 可公开访问的URL
     */
    public static String generatePublicUrl(String objectName) {
        return ENDPOINT.replace("https://", "http://" + BUCKET_NAME + ".") + "/" + objectName;
    }

}
