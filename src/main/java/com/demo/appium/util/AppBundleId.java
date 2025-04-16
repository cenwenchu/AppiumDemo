package com.demo.appium.util;

public enum AppBundleId {
    AMAP("com.autonavi.amap", "高德地图"),
    TENCENT_VIDEO("com.tencent.live4iphone", "腾讯视频"),
    WEWORK("com.tencent.ww", "企业微信"),
    BAIDU("com.baidu.BaiduMobile", "百度"),
    HOMELINK("com.exmart.HomeLink", "链家"),
    WECHAT("com.tencent.xin", "微信"),
    DOUBAN("com.douban.frodo", "豆瓣"),
    FACEBOOK("com.facebook.Facebook", "Facebook"),
    DAMAI("cn.damai.iphone", "大麦"),
    RAILWAY("cn.12306.rails12306", "铁路12306"),
    DIANPING("com.dianping.dpscope", "大众点评"),
    DINGTALK("com.laiwang.DingTalk", "钉钉"),
    FUTU("cn.futu.FutuTraderPhone", "富途牛牛"),
    TENCENT_NEWS("com.tencent.info", "腾讯新闻"),
    DEWU("com.siwuai.duapp", "得物"),
    GOOGLE("com.google.GoogleMobile", "Google"),
    ELEME("me.ele.ios.eleme", "饿了么"),
    MOJI_WEATHER("com.moji.MojiWeather", "墨迹天气"),
    ALIBABA("com.alibaba.wireless", "阿里巴巴"),
    ALIYUN("com.aliyun.wstudio.amc.AliyunMobileApp", "阿里云"),
    CHROME("com.google.chrome.ios", "Chrome"),
    BEIKE("com.lianjia.beike", "贝壳找房"),
    DOUBAO("com.bot.doubao", "豆包"),
    BAIDU_MAP("com.baidu.map", "百度地图"),
    DOUDIAN("com.ss.ios.merchant", "抖店"),
    DOUYIN("com.ss.iphone.ugc.Aweme", "抖音"),
    JUSHUITAN("com.jushuitan.crm", "聚水潭CRM"),
    WEIBO("com.sina.weibo", "微博"),
    WEREAD("com.tencent.weread", "微信读书"),
    BILIBILI("tv.danmaku.bilianime", "哔哩哔哩"),
    CTRIP("ctrip.com", "携程旅行"),
    XIAOHONGSHU("com.xingin.discover", "小红书"),
    QQ_MUSIC("com.tencent.QQMusic", "QQ音乐"),
    XIANYU("com.taobao.fleamarket", "闲鱼"),
    PINDUODUO("com.xunmeng.pinduoduo", "拼多多"),
    DEEPSEEK("com.deepseek.chat", "DeepSeek"),
    MEITUAN("com.meituan.imeituan", "美团"),
    HEMA("com.wdk.hmxs", "盒马"),
    TAOBAO("com.taobao.taobao4iphone", "淘宝"),
    QUNAR("com.qunar.iphoneclient8", "去哪儿旅行"),
    ALIPAY("com.alipay.iphoneclient", "支付宝"),
    GOOGLE_TRANSLATE("com.google.Translate", "Google 翻译");

    private final String bundleId;
    private final String appName;

    AppBundleId(String bundleId, String appName) {
        this.bundleId = bundleId;
        this.appName = appName;
    }

    public String getBundleId() {
        return bundleId;
    }

    public String getAppName() {
        return appName;
    }
}
