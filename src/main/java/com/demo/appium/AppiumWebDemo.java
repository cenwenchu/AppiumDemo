package com.demo.appium;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

/**
 * Hello world!
 *
 */
public class AppiumWebDemo {
    public static void main(String[] args) {
        System.out.println("Hello World!");

        IOSDriver driver = null;

        try {

            XCUITestOptions options = new XCUITestOptions()
                    .setUdid("00008101-000D196A2691001E")
                    .setPlatformName("iOS")
                    .setPlatformVersion("18.3.2")
                    .setBundleId("com.meituan.imeituan");

                    

            driver = new IOSDriver(
                    // The default URL in Appium 1 is http://127.0.0.1:4723/wd/hub
                    new URI("http://127.0.0.1:4723").toURL(), options);

            WebElement el = driver.findElement(AppiumBy.xpath("//XCUIElementTypeButton[@name='外卖']"));
            el.click();

            el = driver.findElements(AppiumBy.xpath("//XCUIElementTypeSearchField[@name='搜索']")).get(0);
            el.click();

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            el = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    AppiumBy.xpath(
                            "//XCUIElementTypeButton[@name='返回']/following-sibling::XCUIElementTypeOther/XCUIElementTypeTextField")));

            el.sendKeys("锄禾");

            el = driver.findElement(AppiumBy.xpath("//XCUIElementTypeButton[@name='搜索']"));
            el.click();

            // 使用WebDriverWait等待元素出现
            wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            el = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    AppiumBy.xpath("//XCUIElementTypeStaticText[contains(@name,'锄禾') and contains(@name,'创客空间') ]")));
            el.click();

            

            //清理一下购物车
            try{
                wait = new WebDriverWait(driver, Duration.ofSeconds(2));
                WebElement tableElement = wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//XCUIElementTypeStaticText[@name='到手约']")));

                tableElement.click();

                tableElement = wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//XCUIElementTypeStaticText[@name='清空']")));

                tableElement.click();


            }
            catch(Exception ex)
            {

            }
            

            wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement tableElement = wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//XCUIElementTypeOther[@name='进店福利']/..")));


            String targetElementXpath = "//XCUIElementTypeCell/XCUIElementTypeStaticText[contains(@name,'【纯素】自选餐')]/following-sibling::XCUIElementTypeOther[@name='选规格']";
            WebElement targetElement = null;
            boolean isElementVisible = false;
            int maxScrollAttempts = 10; // 最大滚动次数
            int scrollAttempts = 0;

            while (!isElementVisible && scrollAttempts < maxScrollAttempts) {
                try {
                    // 尝试查找目标元素
                    targetElement = driver.findElement(AppiumBy.xpath(targetElementXpath));

                    // 获取元素的位置和尺寸
                    int elementY = targetElement.getLocation().getY();
                    int elementHeight = targetElement.getSize().getHeight();

                    // 获取屏幕的尺寸
                    Dimension screenSize = driver.manage().window().getSize();
                    int screenHeight = screenSize.getHeight();

                    System.out.println("elementY:" + elementY);
                    System.out.println("elementY + elementHeight:" + (elementY + elementHeight));
                    System.out.println("screenHeight:" + screenHeight);

                    // 判断元素是否在屏幕内
                    if (elementY >= 0 && elementY + elementHeight <= screenHeight) {
                        isElementVisible = true;
                        break; // 如果元素在屏幕内，退出循环
                    }
                } catch (Exception e) {
                    // 如果元素未找到，继续滚动
                }

                // 滚动操作
                Map<String, Object> params = new HashMap<>();
                params.put("direction", "down");
                params.put("element", tableElement);
                driver.executeScript("mobile: scroll", params);

                scrollAttempts++;
            }

            if (isElementVisible) {
                // 如果元素可见，执行操作
                if (targetElement != null) {
                    targetElement.click();

                    wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                    el = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            AppiumBy.xpath("//XCUIElementTypeStaticText[@name='份量']/../following-sibling::XCUIElementTypeOther//XCUIElementTypeStaticText[@name='五谷丰登']")));
                    el.click();

                    wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                    el = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            AppiumBy.xpath("//XCUIElementTypeStaticText[@name='一半番茄炒蛋']")));
                    el.click();

                    wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                    el = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            AppiumBy.xpath("//XCUIElementTypeStaticText[@name='加入购物车']")));
                    el.click();

                    wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                    el = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            AppiumBy.xpath("//XCUIElementTypeOther[@name='关闭选规格']")));
                    el.click();


                    wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                    el = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            AppiumBy.xpath("//XCUIElementTypeStaticText[contains(@name,'结算')]")));
                    el.click();


                }
            } else {
                // 如果元素未找到，抛出异常或记录日志
                throw new RuntimeException("元素未找到，已达到最大滚动次数");
            }

            // 停顿5秒（5000毫秒）
            Thread.sleep(5000);

        } catch (Exception ex) {
            System.err.println("Appium测试执行失败：" + ex.getMessage());
        } finally {
            if (driver != null)
                driver.quit();
        }

    }
}
