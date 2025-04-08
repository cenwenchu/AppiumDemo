package com.demo.appium;

public enum AIModel {
    QWEN_VL_PLUS("qwen-vl-plus", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    QWEN_OMNI_TURBO("qwen-omni-turbo", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    QWEN_PLUS("qwen-plus", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    QWEN_TURBO("qwen-turbo", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    DEEP_SEEK("deepseek-chat", "https://api.deepseek.com"),
    GPT_4("gpt-4", "https://api.openai.com/v1");

    private final String modelName;
    private final String apiUrl;

    AIModel(String modelName, String apiUrl) {
        this.modelName = modelName;
        this.apiUrl = apiUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public String getApiUrl() {
        return apiUrl;
    }
}