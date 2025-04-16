package com.demo.appium.util;

import java.net.URI;
import java.time.Duration;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

public class AppiumUtil {


    public enum PlatformName {
        IOS("iOS"),
        ANDROID("Android");

        private final String name;

        PlatformName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static AppiumDriver createAppiumDriver(String udid,String platformVersion,PlatformName platformName,String bundleId,String URIString)
    {

        AppiumDriver driver = null;

        try{

            if (platformName == PlatformName.IOS)
            {
                XCUITestOptions options = new XCUITestOptions()
                .setUdid(udid)
                .setPlatformName(platformName.getName())
                .setPlatformVersion(platformVersion)
                .setCommandTimeouts(Duration.ofMinutes(60))
                .setBundleId(bundleId);

                driver = new IOSDriver(new URI(URIString).toURL(), options);
                return driver;
            }
                
        }
        catch(Exception ex)
        {
            System.err.println(ex.getCause());
        }

        return driver;
        
    }

    public static void destoryAppiumDriver(AppiumDriver driver)
    {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error while quitting driver: " + e.getMessage());
                // Additional cleanup if needed
            }
        }
    }
    
}
