package com.demo.appium;

import com.demo.appium.util.AppBundleId;
import com.demo.appium.util.AppiumUtil;
import com.demo.appium.util.AppiumUtil.PlatformName;

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

        try
        {
            driver = AppiumUtil.createAppiumDriver(udid, platformVersion, 
                    PlatformName.IOS, AppBundleId.FUTU.getBundleId(), URIString);


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
