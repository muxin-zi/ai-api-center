package io.github.muxinzi.aihub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台数据表。
 *
 * apiKey  : API Key 管理页（不是控制台首页，直接落到能建 Key 的那一页）
 * billing : 费用 / 充值页（能直接看到余额或充值按钮的那一页）
 * console : 控制台首页（兜底，上面两个万一登录后被重定向，还能从这里找）
 * docs    : 接口文档
 * baseUrl : OpenAI 兼容接口地址，直接粘到 SDK 的 base_url
 *
 * 余额自动读取能力记在 SPEC 里，不塞进构造函数 —— 加平台只需在 all() 里加一行数据，
 * 顺便决定要不要给它在 SPEC 里登记一个余额接口即可。没登记的就是「只能跳网页看」。
 */
public class Provider {

    public final String id;
    public final String name;
    public final String shortName;
    public final int color;
    public final String baseUrl;
    public final String apiKey;
    public final String billing;
    public final String console;
    public final String docs;
    public final boolean overseas;
    public final String note;

    public Provider(String id, String name, String shortName, int color,
                    String baseUrl, String apiKey, String billing, String console,
                    String docs, boolean overseas, String note) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.color = color;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.billing = billing;
        this.console = console;
        this.docs = docs;
        this.overseas = overseas;
        this.note = note;
    }

    public static List<Provider> all() {
        List<Provider> l = new ArrayList<>();

        /* ---------------- 国内直连 ---------------- */
        l.add(new Provider("deepseek", "DeepSeek 深度求索", "DS", 0xFF4D6BFE,
                "https://api.deepseek.com",
                "https://platform.deepseek.com/api_keys",
                "https://platform.deepseek.com/usage",
                "https://platform.deepseek.com/",
                "https://api-docs.deepseek.com/",
                false, "便宜、稳定、国内直连首选"));

        l.add(new Provider("zhipu", "智谱 GLM", "GLM", 0xFF2C6BED,
                "https://open.bigmodel.cn/api/paas/v4",
                "https://bigmodel.cn/usercenter/proj-mgmt/apikeys",
                "https://open.bigmodel.cn/finance/overview",
                "https://open.bigmodel.cn/",
                "https://docs.bigmodel.cn/",
                false, "GLM 系列，有长期免费额度"));

        l.add(new Provider("moonshot", "Kimi 月之暗面", "KM", 0xFF6B7280,
                "https://api.moonshot.cn/v1",
                "https://platform.moonshot.cn/console/api-keys",
                "https://platform.moonshot.cn/console/pay",
                "https://platform.moonshot.cn/console",
                "https://platform.moonshot.cn/docs",
                false, "长上下文见长，实名送代金券"));

        l.add(new Provider("dashscope", "阿里百炼 · 通义千问", "QW", 0xFF7C4DFF,
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "https://bailian.console.aliyun.com/?apiKey=1#/api-key",
                "https://billing.console.aliyun.com/",
                "https://bailian.console.aliyun.com/",
                "https://help.aliyun.com/zh/model-studio/",
                false, "模型最全，每款都有免费额度"));

        l.add(new Provider("volc", "火山方舟 · 豆包", "DB", 0xFF1664FF,
                "https://ark.cn-beijing.volces.com/api/v3",
                "https://console.volcengine.com/ark/region:ark+cn-beijing/apikey",
                "https://console.volcengine.com/finance/",
                "https://console.volcengine.com/ark",
                "https://www.volcengine.com/docs/82379",
                false, "豆包系列，每日免费额度"));

        l.add(new Provider("hunyuan", "腾讯混元", "HY", 0xFF0052D9,
                "https://api.hunyuan.cloud.tencent.com/v1",
                "https://console.cloud.tencent.com/hunyuan/api-key",
                "https://console.cloud.tencent.com/expense/recharge",
                "https://console.cloud.tencent.com/hunyuan",
                "https://cloud.tencent.com/document/product/1729",
                false, "混元 Lite 有免费额度"));

        l.add(new Provider("qianfan", "百度千帆 · 文心", "QF", 0xFF2932E1,
                "https://qianfan.baidubce.com/v2",
                "https://console.bce.baidu.com/iam/#/iam/apikey/list",
                "https://console.bce.baidu.com/finance/",
                "https://console.bce.baidu.com/qianfan/",
                "https://cloud.baidu.com/doc/qianfan/index.html",
                false, "注意是 v2 地址，不是 aip 老域名"));

        l.add(new Provider("siliconflow", "硅基流动 SiliconFlow", "SF", 0xFF7C3AED,
                "https://api.siliconflow.cn/v1",
                "https://cloud.siliconflow.cn/account/ak",
                "https://cloud.siliconflow.cn/expense/bill",
                "https://cloud.siliconflow.cn/",
                "https://docs.siliconflow.cn/",
                false, "一个 Key 打多家开源模型，注册送额度"));

        l.add(new Provider("minimax", "MiniMax", "MM", 0xFFFF5A1F,
                "https://api.minimaxi.com/v1",
                "https://platform.minimaxi.com/user-center/basic-information/interface-key",
                "https://platform.minimaxi.com/user-center/payment/balance",
                "https://platform.minimaxi.com/",
                "https://platform.minimaxi.com/document",
                false, "语音与长文本能力突出"));

        l.add(new Provider("stepfun", "阶跃星辰 StepFun", "ST", 0xFF0A84FF,
                "https://api.stepfun.com/v1",
                "https://platform.stepfun.com/interface-key",
                "https://platform.stepfun.com/top-up",
                "https://platform.stepfun.com/",
                "https://platform.stepfun.com/docs",
                false, "多模态模型，新用户赠额度"));

        l.add(new Provider("xfyun", "科大讯飞 · 星火", "XF", 0xFF00A6E0,
                "https://spark-api-open.xf-yun.com/v1",
                "https://console.xfyun.cn/app/myapp",
                "https://console.xfyun.cn/finance",
                "https://console.xfyun.cn/",
                "https://www.xfyun.cn/doc/spark/Web.html",
                false, "星火系列，Key 在「我的应用」里"));

        l.add(new Provider("yi", "零一万物 Yi", "YI", 0xFF2563EB,
                "https://api.lingyiwanwu.com/v1",
                "https://platform.lingyiwanwu.com/apikeys",
                "https://platform.lingyiwanwu.com/billing",
                "https://platform.lingyiwanwu.com/",
                "https://platform.lingyiwanwu.com/docs",
                false, "Yi 系列开源模型"));

        l.add(new Provider("baichuan", "百川智能", "BC", 0xFF00A385,
                "https://api.baichuan-ai.com/v1",
                "https://platform.baichuan-ai.com/console/apikey",
                "https://platform.baichuan-ai.com/console/finance",
                "https://platform.baichuan-ai.com/",
                "https://platform.baichuan-ai.com/docs",
                false, "Baichuan 系列"));

        /* ---------------- 海外服务 ---------------- */
        l.add(new Provider("openai", "OpenAI", "OA", 0xFF10A37F,
                "https://api.openai.com/v1",
                "https://platform.openai.com/api-keys",
                "https://platform.openai.com/settings/organization/billing/overview",
                "https://platform.openai.com/",
                "https://platform.openai.com/docs",
                true, "GPT 系列，最常见的兼容标准"));

        l.add(new Provider("anthropic", "Anthropic Claude", "CL", 0xFFD97757,
                "https://api.anthropic.com/v1",
                "https://console.anthropic.com/settings/keys",
                "https://console.anthropic.com/settings/billing",
                "https://console.anthropic.com/",
                "https://docs.anthropic.com/",
                true, "Claude 系列，长文本与代码强"));

        l.add(new Provider("gemini", "Google Gemini", "GM", 0xFF4285F4,
                "https://generativelanguage.googleapis.com/v1beta/openai/",
                "https://aistudio.google.com/app/apikey",
                "https://console.cloud.google.com/billing",
                "https://aistudio.google.com/",
                "https://ai.google.dev/gemini-api/docs",
                true, "有免费额度，OpenAI 兼容层"));

        l.add(new Provider("xai", "xAI Grok", "xAI", 0xFFC7CBD1,
                "https://api.x.ai/v1",
                "https://console.x.ai/team/default/api-keys",
                "https://console.x.ai/team/default/billing",
                "https://console.x.ai/",
                "https://docs.x.ai/",
                true, "Grok 系列"));

        l.add(new Provider("openrouter", "OpenRouter", "OR", 0xFF6467F2,
                "https://openrouter.ai/api/v1",
                "https://openrouter.ai/settings/keys",
                "https://openrouter.ai/settings/credits",
                "https://openrouter.ai/",
                "https://openrouter.ai/docs",
                true, "一个 Key 打几十家模型，含免费款"));

        l.add(new Provider("groq", "Groq", "GQ", 0xFFF55036,
                "https://api.groq.com/openai/v1",
                "https://console.groq.com/keys",
                "https://console.groq.com/settings/billing",
                "https://console.groq.com/",
                "https://console.groq.com/docs",
                true, "推理速度极快的开源模型"));

        return l;
    }

    /* ==================== 自定义平台的图标 ==================== */

    /** pid → 本地图片绝对路径。进程重启后由 CustomStore.load 重新填。 */
    private static final Map<String, String> ICONS = new HashMap<>();

    public static void putIcon(String pid, String path) {
        if (pid == null || path == null || path.isEmpty()) return;
        ICONS.put(pid, path);
    }

    public static String iconPath(String pid) {
        return pid == null ? null : ICONS.get(pid);
    }

    public static void removeIcon(String pid) {
        if (pid != null) ICONS.remove(pid);
    }

    /* ==================== 余额自动读取能力 ==================== */

    /** 一个平台的余额接口描述。url 为绝对地址，kind 决定用哪个解析器。 */
    public static class BalanceSpec {
        public final String url;
        public final String kind;
        /** 金额货币符号，仅用于展示兜底 */
        public final String symbol;
        /** 是否需要管理级密钥（OpenRouter 的 credits 接口要 management key） */
        public final boolean needAdminKey;

        public BalanceSpec(String url, String kind, String symbol, boolean needAdminKey) {
            this.url = url;
            this.kind = kind;
            this.symbol = symbol;
            this.needAdminKey = needAdminKey;
        }
    }

    private static final Map<String, BalanceSpec> SPEC = new HashMap<>();

    static {
        // 官方文档有明确接口、普通 key 即可
        SPEC.put("deepseek", new BalanceSpec(
                "https://api.deepseek.com/user/balance", "ds", "¥", false));
        SPEC.put("moonshot", new BalanceSpec(
                "https://api.moonshot.cn/v1/users/me/balance", "moonshot", "¥", false));
        SPEC.put("siliconflow", new BalanceSpec(
                "https://api.siliconflow.cn/v1/user/info", "siliconflow", "¥", false));
        // credits 接口要求 Management key，普通对话 key 会 401
        SPEC.put("openrouter", new BalanceSpec(
                "https://openrouter.ai/api/v1/credits", "openrouter", "$", true));
        // 智谱：未列入官方文档的旧版资源接口，字段可能随账号变化
        SPEC.put("zhipu", new BalanceSpec(
                "https://open.bigmodel.cn/api/paas/v4/users/me/balance", "zhipu", "¥", false));
    }

    /** 没有登记就返回 null，表示这家只能点「费用页」跳网页看 */
    public static BalanceSpec balanceSpec(String pid) {
        return SPEC.get(pid);
    }

    /** 能自动读余额的平台数量，给总览卡用 */
    public static int balanceCapableCount() {
        return SPEC.size();
    }
}
