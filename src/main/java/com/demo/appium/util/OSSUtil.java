package com.demo.appium.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

public class OSSUtil {

    // 阿里云OSS配置
    private static final String ENDPOINT = "https://oss-cn-hangzhou.aliyuncs.com"; // 替换为你的OSS区域
    private static String ALIYUN_ACCESS_KEY_ID = ""; // 替换为你的AccessKey ID
    private static String ALIYUN_ACCESS_KEY_SECRET = ""; // 替换为你的AccessKey Secret
    private static final String BUCKET_NAME = "qiyi-linkusu"; // 替换为你的Bucket名称

    
    /**
     * 上传文件到OSS
     */
    public static void uploadFileToOSS(File file, String objectName) {


        if (ALIYUN_ACCESS_KEY_ID.isEmpty() || ALIYUN_ACCESS_KEY_SECRET.isEmpty()) {
            // 从AIConfig.res中读取配置
            Properties prop = new Properties();
            try (InputStream input = OSSUtil.class.getClassLoader().getResourceAsStream("AIConfig.res")) {
                if (input == null) {
                    System.err.println("无法找到AIConfig.res配置文件");
                    return;
                }
                prop.load(input);
                ALIYUN_ACCESS_KEY_ID = prop.getProperty("ALIYUN_ACCESS_KEY_ID");
                ALIYUN_ACCESS_KEY_SECRET = prop.getProperty("ALIYUN_ACCESS_KEY_SECRET");
            } catch (Exception ex) {
                System.err.println("读取AIConfig.res配置文件失败：" + ex.getMessage());
                return;
            }
        }

        // 创建OSS客户端
        OSS ossClient = new OSSClientBuilder().build(ENDPOINT, ALIYUN_ACCESS_KEY_ID, ALIYUN_ACCESS_KEY_SECRET);

        try (InputStream inputStream = new FileInputStream(file)) {
            // 上传文件
            ossClient.putObject(BUCKET_NAME, objectName, inputStream);
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
    }

    /**
     * 生成公共访问URL
     */
    public static String generatePublicUrl(String objectName) {
        return ENDPOINT.replace("https://", "http://" + BUCKET_NAME + ".") + "/" + objectName;
    }


}
