package com.demo.appium.util;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

/**
 * Appium工具类，提供Appium相关操作方法的封装
 */
public class AppiumUtil {

    /**
     * 平台名称枚举类
     */
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

    /**
     * 创建Appium驱动实例
     * @param udid 设备唯一标识
     * @param platformVersion 平台版本号
     * @param platformName 平台名称
     * @param bundleId 应用包名
     * @param URIString Appium服务器地址
     * @return Appium驱动实例
     */
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
                .setCommandTimeouts(Duration.ofSeconds(3600)) 
                .setNewCommandTimeout(Duration.ofSeconds(3600)) // 将int转换为Duration对象
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

    /**
     * 销毁Appium驱动实例
     * @param driver Appium驱动实例
     */
    public static void destoryAppiumDriver(AppiumDriver driver)
    {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error while quitting driver: " + e.getMessage());
            }
        }
    }

    /**
     * 通过坐标点击屏幕
     * @param driver Appium驱动实例
     * @param x 横坐标比例（0-1）
     * @param y 纵坐标比例（0-1）
     */
    public static void clickByCoordinates(AppiumDriver driver,double x, double y) {
        Dimension size = driver.manage().window().getSize();

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), (int)x * size.getWidth(), (int)y * size.getHeight()));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(new Pause(finger, Duration.ofMillis(100)));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    /**
     * 查找元素并点击
     * @param driver Appium驱动实例
     * @param elementXPathString 元素XPath路径
     * @return 被点击的元素
     */
    public static WebElement findElementAndClick(AppiumDriver driver,String elementXPathString) {
        return findElementAndClick(driver,elementXPathString, true);
    }

    /**
     * 查找元素并点击
     * @param driver Appium驱动实例
     * @param elementXPathString 元素XPath路径
     * @param waitForElement 是否等待元素出现
     * @return 被点击的元素
     */
    public static WebElement findElementAndClick(AppiumDriver driver,String elementXPathString, boolean waitForElement) {
        try {
            WebElement el;

            if (waitForElement) {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
                el = wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath(elementXPathString)));
                wait.until(ExpectedConditions.elementToBeClickable(el));
            } else {
                el = driver.findElement(AppiumBy.xpath(elementXPathString));
            }

            int maxAttempts = 2;
            Exception lastException = null;

            for (int i = 0; i < maxAttempts; i++) {
                try {
                    el.click();
                    return el;
                } catch (Exception e) {
                    lastException = e;
                    Thread.sleep(500);
                }
            }

            throw new RuntimeException("点击元素失败，已重试" + maxAttempts + "次", lastException);

        } catch (TimeoutException e) {
            throw new RuntimeException("等待元素超时: " + elementXPathString, e);
        } catch (Exception e) {
            throw new RuntimeException("操作元素失败: " + elementXPathString, e);
        }
    }

    /**
     * 垂直滚动屏幕
     * @param driver Appium驱动实例
     * @param maxScrollAttempts 最大滚动次数
     * @param scrollUp 是否向上滚动
     */
    public static void scroll(AppiumDriver driver,int maxScrollAttempts, boolean scrollUp) {
        for (int i = 0; i < maxScrollAttempts; i++) {
            Dimension size = driver.manage().window().getSize();
            int startX = size.getWidth() - 10;
            int startY = scrollUp ? (int) (size.getHeight() * 0.3) : (int) (size.getHeight() * 0.7);
            int endY = scrollUp ? (int) (size.getHeight() * 0.7) : (int) (size.getHeight() * 0.3);

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence scroll = new Sequence(finger, 0);
            scroll.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
            scroll.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            scroll.addAction(
                    finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), startX, endY));
            scroll.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(scroll));
        }
    }

    /**
     * 水平滚动屏幕
     * @param driver Appium驱动实例
     * @param maxScrollAttempts 最大滚动次数
     * @param rate 滚动比例
     * @param scrollRight 是否向右滚动
     * @param XOffset X轴偏移量
     */
    public static void scrollHorizontal(AppiumDriver driver,int maxScrollAttempts, double rate, boolean scrollRight, int XOffset,boolean islowPosition) {
        for (int i = 0; i < maxScrollAttempts; i++) {
            Dimension size = driver.manage().window().getSize();
            int startX = scrollRight ? (int) (size.getWidth() * rate) + XOffset : (int) (size.getWidth() * (1 - rate));
            int endX = scrollRight ? (int) (size.getWidth() * (1 - rate)) + XOffset : (int) (size.getWidth() * rate);
            int startY = size.getHeight() / 2;

            if(islowPosition)
            {
                startY = (int) (size.getHeight() * 0.9);
            }

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence scroll = new Sequence(finger, 0);

            scroll.addAction(
                    finger.createPointerMove(Duration.ofMillis(100), PointerInput.Origin.viewport(), startX, startY));
            scroll.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));

            scroll.addAction(
                    finger.createPointerMove(Duration.ofMillis(1000), PointerInput.Origin.viewport(), endX, startY));
            scroll.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(scroll));

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * 通过滚动查找元素
     * @param driver Appium驱动实例
     * @param targetElementXpath 目标元素XPath路径
     * @param maxScrollAttempts 最大滚动次数
     * @return 找到的元素
     * @throws RuntimeException 如果未找到元素
     */
    public static WebElement findElementByScroll(AppiumDriver driver, String targetElementXpath, int maxScrollAttempts)
            throws RuntimeException {
        WebElement targetElement = null;
        boolean isElementVisible = false;
        int scrollAttempts = 0;

        while (!isElementVisible && scrollAttempts < maxScrollAttempts) {
            try {
                // 尝试查找目标元素
                targetElement = driver.findElement(AppiumBy.xpath(targetElementXpath));

                boolean isVisible = targetElement.isDisplayed();

                int elementY = targetElement.getLocation().getY();

                Dimension size = driver.manage().window().getSize();

                // 判断元素是否在屏幕内
                if (isVisible) {
                    // 获取元素位置和尺寸

                    int screenHeight = size.getHeight();

                    // 计算目标位置（屏幕高度的1/4处）
                    int targetY = (int) (screenHeight * 0.25);

                    // 计算需要滚动的距离
                    int scrollDistance = elementY - targetY;

                    // 如果需要滚动，则执行滚动
                    if (Math.abs(scrollDistance) > 10) {
                        scrollToPosition(driver,scrollDistance);
                    }

                    isElementVisible = true;
                    break; // 如果元素在屏幕内，退出循环
                } else {
                    // 在下方
                    if (elementY > 0) {
                        scroll(driver,1, false);
                    } else // 在上方
                    {
                        scroll(driver,1, true);
                    }
                }
            } catch (Exception e) {
                // 如果元素未找到，继续滚动
                scroll(driver,1, false);
                System.out.println(e.getMessage());
            }

            scrollAttempts++;
        }

        if (!isElementVisible) {
            throw new RuntimeException("元素未找到，已达到最大滚动次数");
        }

        return targetElement;
    }

    // 新增方法：滚动到指定位置
    public static void scrollToPosition(AppiumDriver driver,int targetY) {
        // 获取屏幕尺寸
        Dimension size = driver.manage().window().getSize();

        // 限制最大滚动距离为屏幕高度的1/3
        int maxScrollDistance = (int) (size.getHeight() * 0.5);
        //int maxScrollDistance = (int) (size.getHeight());
        int actualScrollDistance = Math.min(Math.abs(targetY), maxScrollDistance) * (targetY < 0 ? -1 : 1);

        // 设置起始X坐标为屏幕中间
        int startX = size.getWidth() / 2;

        // 设置起始Y坐标为屏幕高度的70%处（比之前更靠上）
        int startY = (int) (size.getHeight() * 0.7);

        // 计算结束Y坐标，根据调整后的滚动距离
        int endY = startY - actualScrollDistance;

        // 创建PointerInput对象
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");

        // 创建操作序列
        Sequence scroll = new Sequence(finger, 0);

        // 添加动作1：将手指移动到起始位置
        scroll.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));

        // 添加动作2：手指按下
        scroll.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));

        // 添加动作3：手指移动到结束位置，增加持续时间到800毫秒（更慢更平滑）
        scroll.addAction(
                finger.createPointerMove(Duration.ofMillis(800), PointerInput.Origin.viewport(), startX, endY));

        // 添加动作4：手指抬起
        scroll.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        // 执行整个滚动操作
        driver.perform(Collections.singletonList(scroll));
    }
    
}
