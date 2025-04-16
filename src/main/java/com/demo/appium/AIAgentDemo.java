package com.demo.appium;

import java.util.ArrayList;
import java.util.List;

import com.demo.appium.util.AIUtil;
import com.demo.appium.util.AppBundleId;
import com.demo.appium.util.AppiumUtil;
import com.demo.appium.util.AppiumUtil.PlatformName;
import com.demo.appium.util.OSSUtil;
import com.demo.appium.util.SQLiteStorage;
import com.openai.models.chat.completions.ChatCompletionContentPart;

import io.appium.java_client.AppiumDriver;

public class AIAgentDemo {

    public static void main(String[] args) {
        // 程序入口
        System.out.println("AI Agent Demo 启动");

        String URIString = "http://127.0.0.1:4723";

        String udid = "00008101-000D196A2691001E";
        String platformVersion = "18.3.2";

        udid = "f42f8aaa0d1e87d055a67fec69cbc78af07c8730";
        platformVersion = "15.8.2";
        AppiumDriver driver = null;
        String TEMP_PIC_FILE = "screenshot.png";
        SQLiteStorage sqLiteStorage = null;

        String[] columnDefinition = new String[] { "机构名称", "股票名称", "股票代码", "持仓比例", "变动股份", "变动比例","持股市值" };
        String[] columnDefinitionDB = new String[] { "机构名称", "股票名称", "股票代码", "持仓比例:real", "变动股份", "变动比例:real","持股市值" };


        String dbFileString = "futu.db";
        String tableNameString = "futuAgencies";

        String AI_SUFFIX = "-AI";

        try
        {

            sqLiteStorage = new SQLiteStorage(dbFileString);
            // sqLiteStorage.dropTable("futuAgencies");
            sqLiteStorage.createTables(tableNameString, columnDefinitionDB, null);

            sqLiteStorage.batchSaveCSVFilesToDB(tableNameString, columnDefinition, ".","", true);



            driver = AppiumUtil.createAppiumDriver(udid, platformVersion, 
                    PlatformName.IOS, AppBundleId.FUTU.getBundleId(), URIString);

            AppiumUtil.findElementAndClick(driver,"//XCUIElementTypeImage[contains(@name,'icon_tabbar_markets')]");

            Thread.sleep(1000);

            // 截取当前屏幕
            String imgUrl =  OSSUtil.captureAndUploadScreenshot(driver, TEMP_PIC_FILE);

            List<String> textParts = new ArrayList<>();
            List<String> imageUrls = new ArrayList<>();

            textParts.add("请识别图中'" + "股息排行" + "'文本的中心位置坐标(x,y),然后将 x/图片的宽得到 a, y/图片的高得到 b, 返回 (a,b)");

            imageUrls.add(imgUrl);

            List<ChatCompletionContentPart> arrayOfContentParts = AIUtil.buildChatCompletionContentParts(textParts,imageUrls);

            String aiResponseString = AIUtil.callAIModel(arrayOfContentParts, AIModel.QWEN_OMNI_TURBO, true);
            
            int x = 0;
            int y = 0;

            // 解析AI返回的坐标
            if (aiResponseString.matches("\\(\\d+,\\d+\\),\\(\\d+,\\d+\\)")) {
                // 提取两个坐标
                String[] coordinates = aiResponseString.replaceAll("[()]", "").split(",");
                int x1 = Integer.parseInt(coordinates[0]);
                int y1 = Integer.parseInt(coordinates[1]);
                int x2 = Integer.parseInt(coordinates[2]);
                int y2 = Integer.parseInt(coordinates[3]);
                
                // 计算中心点坐标
                x = (x1 + x2) / 2;
                y = (y1 + y2) / 2;
            } else if (aiResponseString.matches("\\(\\d+,\\d+\\)")) {
                // 如果只有一个坐标，直接使用
                String[] point = aiResponseString.replaceAll("[()]", "").split(",");
                x = Integer.parseInt(point[0]);
                y = Integer.parseInt(point[1]);
            }

            System.out.println(aiResponseString + ", x: " + x + ", y: " + y);

           
            if (x > 0 & y > 0)
                AppiumUtil.clickByCoordinates(driver, x, y);


            Thread.sleep(2000);
        }
        catch(Exception ex)
        {
            System.err.println(ex.getCause());
        }
        finally
        {
            AppiumUtil.destoryAppiumDriver(driver);
        }


        System.out.println("AI Agent Demo 结束");
    }

}
