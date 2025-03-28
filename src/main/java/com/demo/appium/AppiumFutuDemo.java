package com.demo.appium;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

/**
 * Hello world!
 *
 */
public class AppiumFutuDemo {

    IOSDriver driver = null;

    public static void main(String[] args) {

        System.out.println("start to futu!");

        AppiumFutuDemo futuDemo = new AppiumFutuDemo();

        try {

            String udid = "00008101-000D196A2691001E";
            String platformVersion = "18.3.2";

            udid = "f42f8aaa0d1e87d055a67fec69cbc78af07c8730";
            platformVersion = "15.8.2";

            futuDemo.init(udid,platformVersion);

            futuDemo.doGatherFutuInfo();

            //futuDemo.test();
        } finally {
            futuDemo.destory();

            System.out.println("end to futu!");
        }

    }

    void test() {
        findElementByScroll(driver, "//XCUIElementTypeStaticText[@name='持仓行业分布']", 10);
    }

    public void init(String udid,String platformVersion) {
        try {

            XCUITestOptions options = new XCUITestOptions()
                    .setUdid(udid)
                    .setPlatformName("iOS")
                    .setPlatformVersion(platformVersion)
                    .setBundleId("cn.futu.FutuTraderPhone");

            driver = new IOSDriver(
                    // The default URL in Appium 1 is http://127.0.0.1:4723/wd/hub
                    new URI("http://127.0.0.1:4723").toURL(), options);

        } catch (Exception ex) {
            System.err.println("AppiumFutu 初始化执行失败：" + ex.getMessage());
        }
    }

    public void doGatherFutuInfo() {
        WebElement el = driver
                .findElement(AppiumBy.xpath("//XCUIElementTypeImage[contains(@name,'icon_tabbar_markets')]"));

        el.click();

        el = driver
                .findElement(AppiumBy.xpath("//XCUIElementTypeStaticText[@name='美股']"));
        el.click();

        WebElement targetElement = findElementByScroll(driver, "//XCUIElementTypeStaticText[@name='机构追踪']", 10);

        if (targetElement != null)
            targetElement.click();

        el = driver
                .findElement(AppiumBy.xpath("//XCUIElementTypeStaticText[@name='热门机构']"));
        el.click();

        el = driver
                .findElement(AppiumBy.xpath(
                        "//XCUIElementTypeOther[@name='全部']/following-sibling::XCUIElementTypeCell/XCUIElementTypeStaticText"));
        el.click();

        WebElement blackElement = driver
                .findElement(AppiumBy.xpath("//XCUIElementTypeButton[contains(@name,'accessidentifier.futu.main')]"));

        findElementByScroll(driver, "//XCUIElementTypeStaticText[@name='持股列表']", 10);

        // scrollHorizontal(2,0.3,false);
        // scrollDown(2);

        List<String[]> data = new ArrayList<>();

        List<WebElement> elements = driver.findElements(
                AppiumBy.xpath("//XCUIElementTypeStaticText[@name='添利']/ancestor::XCUIElementTypeCell[position()]"));

        for (WebElement element : elements) {
            // 获取每个 element 中的所有 XCUIElementTypeStaticText，并按照页面顺序排列
            List<WebElement> staticTexts = element
                    .findElements(AppiumBy.xpath(".//XCUIElementTypeStaticText[position()]"));

            if (staticTexts.size() >= 6) {
                List<String> row = new ArrayList<>();
                for (WebElement staticText : staticTexts) {
                    if (!staticText.getText().equals("添利")) {
                        row.add(staticText.getText());
                    }
                }
                data.add(row.toArray(new String[0]));
            }
        }

        // 将数据写入CSV文件
        writeToCSV("futu_data.csv", data, false);

        blackElement.click();

        // 停顿5秒（5000毫秒）
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将数据写入CSV文件
     * 
     * @param fileName 文件名
     * @param data     数据列表
     * @param append   是否追加到文件末尾
     */
    public void writeToCSV(String fileName, List<String[]> data, boolean append) {
        try {
            File file = new File(fileName);
            boolean fileExists = file.exists();

            // 如果文件存在且不追加，则删除文件
            if (fileExists && !append) {
                file.delete();
            }

            try (FileWriter writer = new FileWriter(file, append)) {

                // 写入数据
                for (String[] row : data) {
                    writer.append(String.join(",", row));
                    writer.append("\n");
                }
            }
        } catch (IOException e) {
            System.err.println("写入CSV文件时出错：" + e.getMessage());
        }
    }

    public void destory() {
        if (driver != null) {
            driver.quit();
        }
    }

    void scroll(int maxScrollAttempts, boolean scrollUp) {
        for (int i = 0; i < maxScrollAttempts; i++) {
            Dimension size = driver.manage().window().getSize();
            int startX = size.getWidth() - 10; // 靠近屏幕右边缘
            int startY = scrollUp ? (int) (size.getHeight() * 0.3) : (int) (size.getHeight() * 0.7);
            int endY = scrollUp ? (int) (size.getHeight() * 0.7) : (int) (size.getHeight() * 0.3);

            // 使用W3C Actions API
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

    void scrollHorizontal(int maxScrollAttempts, double rate, boolean scrollRight) {
        for (int i = 0; i < maxScrollAttempts; i++) {
            Dimension size = driver.manage().window().getSize();
            int startX = scrollRight ? (int) (size.getWidth() * rate) : (int) (size.getWidth() * (1 - rate));
            int endX = scrollRight ? (int) (size.getWidth() * (1 - rate)) : (int) (size.getWidth() * rate);
            int startY = size.getHeight() / 2; // 屏幕中间位置

            // 使用W3C Actions API
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence scroll = new Sequence(finger, 0);
            scroll.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
            scroll.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            scroll.addAction(
                    finger.createPointerMove(Duration.ofMillis(1000), PointerInput.Origin.viewport(), endX, startY)); // 增加滑动时间
            scroll.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(scroll));
        }
    }

    WebElement findElementByScroll(IOSDriver driver, String targetElementXpath, int maxScrollAttempts)
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
                        scrollToPosition(scrollDistance);
                    }
                    
                    isElementVisible = true;
                    break; // 如果元素在屏幕内，退出循环
                }
                else
                {
                    //在下方
                    if (elementY > 0)
                    {
                        scroll(1, false);
                    }
                    else //在上方
                    {
                        scroll(1, true);
                    }
                }
            } catch (Exception e) {
                // 如果元素未找到，继续滚动
                scroll(1, false);
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
    void scrollToPosition(int targetY) {
        // 获取屏幕尺寸
        Dimension size = driver.manage().window().getSize();
        
        // 限制最大滚动距离为屏幕高度的1/3
        int maxScrollDistance = (int) (size.getHeight() * 0.33);
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
