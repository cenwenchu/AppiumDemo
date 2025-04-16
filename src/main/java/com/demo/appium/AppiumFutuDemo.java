package com.demo.appium;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.WebElement;

import com.demo.appium.util.AIUtil;
import com.demo.appium.util.AppBundleId;
import com.demo.appium.util.AppiumUtil;
import com.demo.appium.util.AppiumUtil.PlatformName;
import com.demo.appium.util.OSSUtil;
import com.demo.appium.util.SQLiteStorage;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartText;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;

/**
 * Hello world!
 *
 */
public class AppiumFutuDemo {

    AppiumDriver driver = null;
    SQLiteStorage sqLiteStorage = null;
    Set<String> processedAgents = new HashSet<>();
    Set<String> processedAgentsByAI = new HashSet<>();

    static String AI_SUFFIX = "-AI";
    static String TEMP_PIC_FILE = "screenshot.png";
    static String TEMP_PIC_FILE2 = "screenshot2.png";

    String[] columnDefinition = new String[] { "机构名称", "股票名称", "股票代码", "持仓比例", "变动股份", "变动比例","持股市值" };
    String[] columnDefinitionDB = new String[] { "机构名称", "股票名称", "股票代码", "持仓比例:real", "变动股份", "变动比例:real","持股市值" };
    String[] columnDefinitionCSV = new String[] { "股票名称", "股票代码", "持仓比例", "变动股份", "变动比例","持股市值"};


    String dbFileString = "futu.db";
    String tableNameString = "futuAgencies";


    public static void main(String[] args) {

        System.out.println("开始分析富途牛牛!");

        boolean isByAIProcess = true;


        String udid = "00008101-000D196A2691001E";
        String platformVersion = "18.3.2";

        udid = "f42f8aaa0d1e87d055a67fec69cbc78af07c8730";
        platformVersion = "15.8.2";

        long consumeTime = System.currentTimeMillis();

        AppiumFutuDemo futuDemo = new AppiumFutuDemo();

        try {

            futuDemo.init(udid, platformVersion);

            futuDemo.doGatherFutuInfo(1, 3,isByAIProcess);

            if(!isByAIProcess)
                futuDemo.callAIToProcessCSV(true);
            else
            {
                futuDemo.sqLiteStorage.batchSaveCSVFilesToDB(futuDemo.tableNameString, 
                        futuDemo.columnDefinition, ".","", true);
            }

        } catch (Exception ex) {
            System.err.println("富途牛牛分析 分析执行出错：" + ex.getMessage());
        } finally {
            futuDemo.destory();

            consumeTime = System.currentTimeMillis() - consumeTime;

            System.out.println("富途牛牛分析结束! 耗时：" + consumeTime / 1000 + " 秒");
        }

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

            sqLiteStorage = new SQLiteStorage(dbFileString);
            // sqLiteStorage.dropTable("futuAgencies");
            sqLiteStorage.createTables(tableNameString, columnDefinitionDB, null);

            driver = AppiumUtil.createAppiumDriver(udid,platformVersion,PlatformName.IOS,
                    AppBundleId.FUTU.getBundleId(),"http://127.0.0.1:4723");

        } catch (Exception ex) {
            System.err.println("AppiumFutu 初始化执行失败：" + ex.getMessage());
        }
    }

    public void destory() {

        AppiumUtil.destoryAppiumDriver(driver);


        if (this.sqLiteStorage != null) {
            try {
                sqLiteStorage.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                System.err.println("关闭sqliteStorage 出错:" + e.getMessage());
            }
        }
    }

    public void doGatherFutuInfo(int agencyCount, int maxScrollCount,boolean isByAIProcess) {
        AppiumUtil.findElementAndClick(driver,"//XCUIElementTypeImage[contains(@name,'icon_tabbar_markets')]");

        AppiumUtil.findElementAndClick(driver,
                "//XCUIElementTypeCollectionView//XCUIElementTypeCell/XCUIElementTypeOther/XCUIElementTypeStaticText[@name='美股']");

        WebElement targetElement = AppiumUtil.findElementByScroll(driver,
                "//XCUIElementTypeButton/XCUIElementTypeStaticText[@name='机构追踪']", 10);

        if (targetElement != null)
            targetElement.click();

        AppiumUtil.findElementAndClick(driver,"//XCUIElementTypeStaticText[@name='热门机构']");

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

                    if (processedAgents.contains(agencyName)) {
                        System.out.println("机构： " + agencyName + " 已经抓取过，忽略 ");
                        continue;
                    }

                    System.out.println("机构： " + agencyName + " 开始处理... ");

                    //String csvNameString = agencyName;
                    agencyElements.get(i).click();

                    WebElement backElement = driver
                            .findElement(AppiumBy
                                    .xpath("//XCUIElementTypeButton[contains(@name,'accessidentifier.futu.main')]"));

                    AppiumUtil.findElementByScroll(driver, "//XCUIElementTypeStaticText[@name='持股列表']", 10);

                    getAgencyDetail(agencyName, backElement, maxScrollCount, false ,isByAIProcess);

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
                AppiumUtil.scroll(driver,1, false);
            }

        }

        // 停顿5秒（5000毫秒）
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void getAgencyDetail(String agencyName, WebElement backElement, int maxScrollCount, boolean isAppend,boolean isByAIProcess) {
        List<String[]> data = new ArrayList<>();
        Set<String> processedRows = new HashSet<>(); // 用于存储已处理的行
        int consecutiveDuplicates = 0;
        int maxConsecutiveDuplicates = 1; // 连续发现重复数据的次数阈值

        int scrollCount = 0;

        while (consecutiveDuplicates < maxConsecutiveDuplicates && scrollCount < maxScrollCount) {

            // this.scrollHorizontal(1, 0.1,false,0);
            // this.scrollHorizontal(1, 0.1,true,200);

            long timeConsume = System.currentTimeMillis();

            boolean foundNewData = false;

            if (isByAIProcess)
                foundNewData = getAgentcyStocksByAI(agencyName,processedRows, data);
            else
                foundNewData = getAgencyStocksByXPath(processedRows, data);

            System.out.println(" process stock elements  time consume: " + (System.currentTimeMillis() - timeConsume));

            if (!foundNewData) {
                consecutiveDuplicates++;
            } else {
                consecutiveDuplicates = 0;
            }

            // 向下滚动并增加计数
            AppiumUtil.scroll(driver,1, false);
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
            if (isByAIProcess)
                writeToCSV(agencyName, data, isAppend, String.join(",", columnDefinition));
            else
                writeToCSV(agencyName, data, isAppend, String.join(",", columnDefinitionCSV));

        System.out.println("一共有效的数据为：" + data.size() + " 条");

        backElement.click();
    }

    boolean getAgentcyStocksByAI(String agencyName,Set<String> processedRows, List<String[]> data) {
        boolean result = false;

        // 截取当前屏幕
        String imgUrl =  OSSUtil.captureAndUploadScreenshot(driver, TEMP_PIC_FILE);

        AppiumUtil.scrollHorizontal(driver,1, 0.2, false, 0);

        String imgUrl2 = OSSUtil.captureAndUploadScreenshot(driver, TEMP_PIC_FILE2);

        AppiumUtil.scrollHorizontal(driver,1, 0.1, true, 200);

        try {

            List<String> textParts = new ArrayList<>();
            List<String> imageUrls = new ArrayList<>();

            textParts.add("将下面两张图片中的数据先独立整理成为两个表格,"
                            + "一个表格列为：股票名称,股票代码,持仓比例,变动股份，另一个表列为：股票名称,股票代码,变动比例,持股市值"
                            + ",然后将两个表格根据'股票名称'做关联，合并为一个表格，表格列为：'股票名称,股票代码,持仓比例,变动股份,变动比例,持股市值';"
                            + " 请不要返回合并前的两个表格,仅返回合并后的表格！");

            imageUrls.add(imgUrl);
            imageUrls.add(imgUrl2);


            List<ChatCompletionContentPart> arrayOfContentParts = AIUtil.buildChatCompletionContentParts(textParts,imageUrls);


            // String aiResponseString = AIUtil.callAIModel(arrayOfContentParts,
            // AIModel.QWEN_VL_PLUS, true);
            String aiResponseString = AIUtil.callAIModel(arrayOfContentParts, AIModel.QWEN_OMNI_TURBO, true);

            // 按行分割AI返回的字符串
            String[] lines = aiResponseString.split("\n");

            // 遍历每一行
            for (String line : lines) {
                // 用逗号分割当前行
                String[] columns = Arrays.stream(line.split("\\|"))
                                        .map(s -> s.replace("$", "").replace("%", "").replace("<", "").replace(">", "").trim())
                                        .filter(s -> !s.isEmpty())
                                        .toArray(String[]::new);

                // 如果列数大于等于5则处理
                if (columns.length >= 5) {
                    List<String> row = new ArrayList<>();
                    
                    row.addAll(Arrays.asList(columns));
                    row.add(0, agencyName);

                    if (!row.contains("股票名称") && !row.get(1).startsWith("--"))
                    {
                        String rowString = row.get(2);

                        if (processedRows.add(rowString)) {
                            data.add(row.toArray(new String[0]));

                            System.out.println("add to data: " + rowString);
                            result = true;
                        }
                    }

                }
            }

            System.out.println(aiResponseString);

        } catch (Exception e) {
            System.err.println("截图处理失败：" + e.getMessage());
        }

        return result;
    }

    boolean getAgencyStocksByXPath(Set<String> processedRows, List<String[]> data) {

        boolean result = false;

        List<WebElement> elements = driver.findElements(
                AppiumBy.xpath(
                        "//XCUIElementTypeCell[.//*[@name='添利']]"));

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
                    result = true;
                }
            }
        }

        return result;
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

                if (processedAgentsByAI.contains(fileNameString + AI_SUFFIX)) {
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

                            if (line.split(",").length == columnDefinition.length - 2) {
                                String[] newArray = new String[line.split(",").length + 2];
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

                    this.writeToCSV(fileNameString + AI_SUFFIX, data, false, String.join(",", columnDefinition));
                    this.writeToDB("futuAgencies", data, columnDefinition);

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

}
