package com.demo.appium;

public enum AIModel {
    QWEN_VL_PLUS("qwen-vl-plus"),
    QWEN_OMNI_TURBO("qwen-omni-turbo"),
    QWEN_PLUS("qwen-plus"),
    DEEP_SEEK(""),
    GPT_4("gpt-4");

    private final String modelName;

    AIModel(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
} 