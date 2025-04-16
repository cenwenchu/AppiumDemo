package com.demo.appium;

/**
 * AI模型枚举类，定义不同AI模型及其API地址
 */
public enum AIModel {
    // 通义千问视觉增强版模型
    QWEN_VL_PLUS("qwen-vl-plus", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    // 通义千问全能版模型
    QWEN_OMNI_TURBO("qwen-omni-turbo", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    // 通义千问增强版模型
    QWEN_PLUS("qwen-plus", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    // 通义千问基础版模型
    QWEN_TURBO("qwen-turbo", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    // DeepSeek聊天模型
    DEEP_SEEK("deepseek-chat", "https://api.deepseek.com"),
    // GPT-4模型
    GPT_4("gpt-4", "https://api.openai.com/v1");

    // 模型名称
    private final String modelName;
    // API地址
    private final String apiUrl;

    /**
     * 构造函数
     * @param modelName 模型名称
     * @param apiUrl API地址
     */
    AIModel(String modelName, String apiUrl) {
        this.modelName = modelName;
        this.apiUrl = apiUrl;
    }

    /**
     * 获取模型名称
     * @return 模型名称
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * 获取API地址
     * @return API地址
     */
    public String getApiUrl() {
        return apiUrl;
    }

    
}