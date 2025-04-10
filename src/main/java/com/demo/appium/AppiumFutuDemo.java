package com.demo.appium;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.demo.appium.util.AIUtil;
import com.demo.appium.util.SQLiteStorage;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartText;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

/**
 * Hello world!
 *
 */
public class AppiumFutuDemo {

    IOSDriver driver = null;
    SQLiteStorage sqLiteStorage = null;
    Set<String> processedAgents = new HashSet<>();
    Set<String> processedAgentsByAI = new HashSet<>();

    static String AI_SUFFIX = "-AI";

    String[] columnDefinitions = new String[] { "机构名称", "股票名称", "股票代码", "持仓比例", "变动股份", "变动比例" };
    String[] columnDefinitionsDB = new String[] { "机构名称", "股票名称", "股票代码", "持仓比例:real", "变动股份", "变动比例:real" };
    String[] columnDefinitionsCSV = new String[] { "股票名称", "股票代码", "持仓比例", "变动股份", "变动比例" };

    public static void main(String[] args) {

        System.out.println("开始分析富途牛牛!");

        long consumeTime = System.currentTimeMillis();

        AppiumFutuDemo futuDemo = new AppiumFutuDemo();

        try {

            String udid = "00008101-000D196A2691001E";
            String platformVersion = "18.3.2";

            udid = "f42f8aaa0d1e87d055a67fec69cbc78af07c8730";
            platformVersion = "15.8.2";

            futuDemo.init(udid, platformVersion);

            //futuDemo.test();

             futuDemo.doGatherFutuInfo(1, 5);

             futuDemo.callAIToProcessCSV(true);
        } catch (Exception ex) {
            System.err.println("富途牛牛分析 分析执行出错：" + ex.getMessage());
        } finally {
            futuDemo.destory();

            consumeTime = System.currentTimeMillis() - consumeTime;

            System.out.println("富途牛牛分析结束! 耗时：" + consumeTime / 1000 + " 秒");
        }

    }

    void test() {
        callAIToProcessCSV(true);
    }

    public void init(String udid, String platformVersion) {
        try {
            // 获取当前目录下的所有csv文件
            File currentDir = new File(".");
            File[] csvFiles = currentDir.listFiles((dir, name) -> name.endsWith(".csv"));

            // 将csv文件名（去掉.csv后缀）添加到processedAgents集合中
            if (csvFiles != null) {
                for (File csvFile : csvFiles) {
                    String fileName = csvFile.getName().replace(".csv", "");
                    if (fileName.endsWith(AI_SUFFIX)) {
                        processedAgentsByAI.add(fileName);
                    } else {
                        processedAgents.add(fileName);
                    }
                }
            }

            sqLiteStorage = new SQLiteStorage("futu.db");
            // sqLiteStorage.dropTable("futuAgencies");
            sqLiteStorage.createTables("futuAgencies", columnDefinitionsDB, null);

            XCUITestOptions options = new XCUITestOptions()
                    .setUdid(udid)
                    .setPlatformName("iOS")
                    .setPlatformVersion(platformVersion)
                    .setCommandTimeouts(Duration.ofMinutes(60))
                    .setBundleId("cn.futu.FutuTraderPhone");

            driver = new IOSDriver(
                    // The default URL in Appium 1 is http://127.0.0.1:4723/wd/hub
                    new URI("http://127.0.0.1:4723").toURL(), options);

        } catch (Exception ex) {
            System.err.println("AppiumFutu 初始化执行失败：" + ex.getMessage());
        }
    }

    public void callAIToProcessCSV(boolean isSaveToDB) {

        System.out.println("开始调用AI 分析统计数据!");

        // 将文件读出来，然后调用AI的工具
        // 获取当前目录
        File currentDir = new File(".");
        // 获取所有.csv文件
        File[] csvFiles = currentDir.listFiles((dir, name) -> name.endsWith(".csv") && !name.contains(AI_SUFFIX));

        StringBuilder csvContent;

        if (csvFiles != null) {
            for (File csvFile : csvFiles) {

                String fileNameString = csvFile.getName().replace(".csv", "");

                if (processedAgentsByAI.contains(fileNameString+AI_SUFFIX))
                {
                    System.out.println("机构： " + fileNameString + " 已经通过AI处理过～ ");
                    continue;
                 }

                try {

                    csvContent = new StringBuilder();
                    csvContent.append(fileNameString).append("持股情况如下:").append("\n");

                    try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            csvContent.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        System.err.println("读取CSV文件时出错：" + e.getMessage());
                    }

                    //
                    List<ChatCompletionContentPart> arrayOfContentParts = new ArrayList<ChatCompletionContentPart>();

                    arrayOfContentParts.add(ChatCompletionContentPart.ofText(ChatCompletionContentPartText.builder()
                            .text("以下为机构持股的情况，其中有些数据的列前后位置放错，请帮忙纠正一下；纠正以后，请帮忙输出正确的数据（注意，请直接输出最终结果，不要增加任何处理说明的文字，返回结果列为：股票名称,股票代码,持仓比例,变动股份,变动比例）。具体需要处理的数据如下：")
                            .build()));

                    arrayOfContentParts.add(ChatCompletionContentPart
                            .ofText(ChatCompletionContentPartText.builder().text(csvContent.toString()).build()));

                    String result = AIUtil.callAIModel(arrayOfContentParts, AIModel.QWEN_PLUS, true);

                    System.out.println("AI result:" + result);

                    // 将AI返回的结果按行分割，并转换为List<String[]>格式
                    String[] lines = result.split("\n");

                    boolean isData = false;

                    List<String[]> data = new ArrayList<>();
                    for (String line : lines) {

                        if (line.contains("变动比例")) {
                            isData = true;
                            continue;
                        } else if (line.contains("万")) {
                            isData = true;
                        }

                        if (isData) {
                            line = line.replaceAll("%", "").replaceAll("<", "");

                            if (line.split(",").length == columnDefinitions.length-1) {
                                String[] newArray = new String[line.split(",").length + 1];
                                newArray[0] = fileNameString;
                                System.arraycopy(line.split(","), 0, newArray, 1, line.split(",").length);
                                data.add(newArray);

                                System.out.println("add line:" + line);
                            } else {
                                System.out.println("ignore line:" + line);
                            }

                        } else {
                            System.out.println("ignore line:" + line);
                        }
                    }

                    this.writeToCSV(fileNameString + AI_SUFFIX, data, false, String.join(",", columnDefinitions));
                    this.writeToDB("futuAgencies", data, columnDefinitions);

                } catch (Exception ex) {
                    System.err.println(
                            "callAIToProcessCSV 执行出错," + "fileName: " + csvFile.getName() + " ," + ex.getMessage());
                }

            }
        }

    }

    public void writeToDB(String tableName, List<String[]> data, String[] title) {
        if (sqLiteStorage == null) {
            try {
                sqLiteStorage = new SQLiteStorage("futu.db");
            } catch (SQLException e) {
                System.err.println("创建SQLiteStorage失败：" + e.getMessage());
                return;
            }
        }

        try {
            // 直接插入数据，假设表已经存在
            for (String[] row : data) {
                sqLiteStorage.storeData(tableName, title, row);
            }
        } catch (SQLException e) {
            System.err.println("写入数据库失败：" + e.getMessage());
        }
    }

    public void doGatherFutuInfo(int agencyCount, int maxScrollCount) {
        findElementAndClick("//XCUIElementTypeImage[contains(@name,'icon_tabbar_markets')]");

        findElementAndClick(
                "//XCUIElementTypeCollectionView//XCUIElementTypeCell/XCUIElementTypeOther/XCUIElementTypeStaticText[@name='美股']");

        WebElement targetElement = findElementByScroll(driver,
                "//XCUIElementTypeButton/XCUIElementTypeStaticText[@name='机构追踪']", 10);

        if (targetElement != null)
            targetElement.click();

        findElementAndClick("//XCUIElementTypeStaticText[@name='热门机构']");

        while (agencyCount > 0) {
            List<WebElement> agencyElements = driver
                    .findElements(AppiumBy.xpath(
                            "//XCUIElementTypeOther[@name='全部']/following-sibling::XCUIElementTypeCell/XCUIElementTypeStaticText[position()]"));

            if (agencyElements.isEmpty())
                break;

            boolean processedRows = false;
            int elementCount = agencyElements.size();

            for (int i = 0; i < elementCount; i++) {

                try {

                    String agencyName = agencyElements.get(i).getText();

                    if (agencyCount <= 0)
                        break;

                    try {
                        // 尝试将字符串转换为数字
                        Double.parseDouble(agencyName);
                        continue; // 如果是数字，跳过当前循环
                    } catch (NumberFormatException e) {
                        // 如果不是数字，继续执行后续代码
                    }

                    if (!agencyElements.get(i).isDisplayed())
                        continue;

                    if (processedAgents.contains(agencyName))
                    {
                        System.out.println("机构： " + agencyName + " 已经抓取过，忽略 ");
                        continue;
                    }

                    System.out.println("机构： " + agencyName + " 开始处理... ");

                    String csvNameString = agencyName;
                    agencyElements.get(i).click();

                    WebElement backElement = driver
                            .findElement(AppiumBy
                                    .xpath("//XCUIElementTypeButton[contains(@name,'accessidentifier.futu.main')]"));

                    findElementByScroll(driver, "//XCUIElementTypeStaticText[@name='持股列表']", 10);

                    getAgencyDetail(csvNameString, backElement, maxScrollCount, false);

                    System.out.println("机构： " + agencyName + " 处理完毕. ");

                    agencyCount -= 1;
                    processedAgents.add(agencyName);
                    processedRows = true;

                } catch (Exception ex) {
                    processedRows = true;
                    System.err.println(ex.getCause());
                    break;
                }
            }

            if (!processedRows)
                break;
            else {
                scroll(1, false);
            }

        }

        // 停顿5秒（5000毫秒）
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void getAgencyDetail(String csvFileName, WebElement backElement, int maxScrollCount, boolean isAppend) {
        List<String[]> data = new ArrayList<>();
        Set<String> processedRows = new HashSet<>(); // 用于存储已处理的行
        int consecutiveDuplicates = 0;
        int maxConsecutiveDuplicates = 1; // 连续发现重复数据的次数阈值

        int scrollCount = 0;

        while (consecutiveDuplicates < maxConsecutiveDuplicates && scrollCount < maxScrollCount) {

            // this.scrollHorizontal(1, 0.1,false,0);
            // this.scrollHorizontal(1, 0.1,true,200);

            long timeConsume = System.currentTimeMillis();

            List<WebElement> elements = driver.findElements(
                    AppiumBy.xpath(
                            "//XCUIElementTypeCell[.//*[@name='添利']]"));

            System.out.println(" get stock elements  time consume: " + (System.currentTimeMillis() - timeConsume));
            
            boolean foundNewData = false;

            for (WebElement element : elements) {

                if (!element.isDisplayed())
                    continue;

                List<WebElement> staticTexts = element
                        .findElements(AppiumBy.xpath(".//XCUIElementTypeStaticText[position()]"));

                if (staticTexts.size() >= 6) {
                    List<String> row = new ArrayList<>();
                    for (WebElement staticText : staticTexts) {
                        if (!staticText.getText().equals("添利")) {
                            row.add(staticText.getText());
                        }
                    }

                    String rowString = String.join("|", row);

                    if (processedRows.add(rowString)) {
                        data.add(row.toArray(new String[0]));
                        foundNewData = true;
                    }
                }
            }

            System.out.println(" process stock elements  time consume: " + (System.currentTimeMillis() - timeConsume));

            if (!foundNewData) {
                consecutiveDuplicates++;
            } else {
                consecutiveDuplicates = 0;
            }

            // 向下滚动并增加计数
            scroll(1, false);
            scrollCount++;

            System.out.println("已下翻 " + scrollCount + " 次，处理数据 " + data.size() + " 条");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 打印结束原因
        if (scrollCount >= maxScrollCount) {
            System.out.println("达到最大下翻次数限制：" + maxScrollCount + " 次");
        } else {
            System.out.println("连续 " + maxConsecutiveDuplicates + " 次未发现新数据，停止处理");
        }

        // 将数据写入CSV文件
        if (!data.isEmpty())
            writeToCSV(csvFileName, data, isAppend, String.join(",", columnDefinitionsCSV));

        System.out.println("一共有效的数据为：" + data.size() + " 条");

        backElement.click();
    }

    public WebElement findElementAndClick(String elementXPathString) {
        return findElementAndClick(elementXPathString, true);
    }

    public WebElement findElementAndClick(String elementXPathString, boolean waitForElement) {
        try {
            WebElement el;

            if (waitForElement) {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
                // 先等待元素可见
                el = wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath(elementXPathString)));
                // 再等待元素可点击
                wait.until(ExpectedConditions.elementToBeClickable(el));
            } else {
                el = driver.findElement(AppiumBy.xpath(elementXPathString));
            }

            // 尝试点击2次
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
     * 将数据写入CSV文件
     * 
     * @param fileName 文件名
     * @param data     数据列表
     * @param append   是否追加到文件末尾
     */
    public void writeToCSV(String fileName, List<String[]> data, boolean append, String title) {
        try {
            if (!fileName.endsWith(".csv")) {
                fileName += ".csv";
            }

            File file = new File(fileName);
            boolean fileExists = file.exists();

            // 如果文件存在且不追加，则删除文件
            if (fileExists && !append) {
                file.delete();
            }

            try (FileWriter writer = new FileWriter(file, append)) {

                if (title != null)
                    writer.append(title).append("\n");

                // 写入数据
                for (String[] row : data) {
                    writer.append(String.join(",", row));
                    writer.append("\n");
                }
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("写入CSV文件时出错：" + e.getMessage());
        }
    }

    public void destory() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error while quitting driver: " + e.getMessage());
                // Additional cleanup if needed
            }
        }

        if (this.sqLiteStorage != null) {
            try {
                sqLiteStorage.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                System.err.println("关闭sqliteStorage 出错:" + e.getMessage());
            }
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

    void scrollHorizontal(int maxScrollAttempts, double rate, boolean scrollRight, int XOffset) {
        for (int i = 0; i < maxScrollAttempts; i++) {
            Dimension size = driver.manage().window().getSize();
            int startX = scrollRight ? (int) (size.getWidth() * rate) + XOffset : (int) (size.getWidth() * (1 - rate));
            int endX = scrollRight ? (int) (size.getWidth() * (1 - rate)) + XOffset : (int) (size.getWidth() * rate);
            int startY = size.getHeight() / 2; // 屏幕中间位置

            // 使用W3C Actions API
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence scroll = new Sequence(finger, 0);

            scroll.addAction(
                    finger.createPointerMove(Duration.ofMillis(100), PointerInput.Origin.viewport(), startX, startY));
            scroll.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));

            scroll.addAction(
                    finger.createPointerMove(Duration.ofMillis(1000), PointerInput.Origin.viewport(), endX, startY)); // 增加滑动时间
            scroll.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(scroll));

            // 添加短暂停顿让滑动完成
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
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
                } else {
                    // 在下方
                    if (elementY > 0) {
                        scroll(1, false);
                    } else // 在上方
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
