# 平台图标

## 现在的情况

`app/src/main/assets/logos/` 里有 **12 个** 平台 logo，全部处理成统一规格：**256×256 PNG、圆角 56px**。

| 有 logo | 来源 | 原始尺寸 |
|---|---|---|
| DeepSeek | 官网 SVG（矢量） | 无损 |
| 阶跃星辰 StepFun | 官网 SVG（矢量） | 无损 |
| OpenAI | 官网 SVG（矢量） | 无损 |
| Groq | 官网 SVG（矢量） | 无损 |
| 阿里百炼 · 通义千问 | 阿里 CDN | 1024² |
| 百度千帆 · 文心 | 百度 CDN | 1024² |
| OpenRouter | 官网 | 512² |
| MiniMax | 官网 CDN | 300² |
| 腾讯混元 | 腾讯 CDN | 300² |
| Kimi 月之暗面 | `www.kimi.com/pwa-192.png` | 192² |
| 火山方舟 · 豆包 | 火山 CDN | 192² |
| 智谱 GLM | 官网 | 90² |

## 没有 logo 的 6 家

| 平台 | 原因 |
|---|---|
| 硅基流动 | 官网只提供 48×48，放大必糊 |
| 零一万物 Yi | 只提供 32×32 |
| 百川智能 | 只提供 16×16 |
| 科大讯飞 · 星火 | 只提供 32×32 ico |
| Anthropic Claude | 只提供 32×32 |
| Google Gemini | 中国大陆网络无法访问官网 |

这 6 家自动回退到「品牌色圆角块 + 首字母」，不影响使用。

## 处理方法（可复现）

原始抓取与处理脚本不在仓库内（依赖本机环境）。处理规则如下，换图时需要遵循：

1. **判断类型**：算整图不透明像素占比。
   - 占比 > 45%：视为自带底色的完整图标，裁成正方形后铺满画布。
   - 占比 ≤ 45%：视为透明图形，垫一层品牌色半透明圆角底，图形居中占 62%。
2. **纯黑 logo 反白**：算非透明像素平均亮度，低于 70 就把 RGB 取反。
   否则黑图标放在深色界面上看不见（DeepSeek 的鲸鱼就是这样处理的）。
3. **原始像素小于 60 的直接不用**，交给首字母兜底，不要硬放大。
4. 输出统一 **256×256 PNG**，圆角半径 **56px**。

> ⚠️ 常见坑：PIL 的 `Image.thumbnail()` **只缩不放**，用来处理小图会让 logo 缩成角落里的一个点。
> 必须用 `resize()`。

> ⚠️ SVG 渲染：本机没有 cairosvg / svglib / wand，用 Edge 无头截图。
> 但 `msedge --screenshot file.svg` **不会撑满画布**（会按 SVG 自身尺寸渲染在左上角），
> 必须包一层 HTML：`<img src="..." style="width:512px;height:512px;object-fit:contain">`，
> 并加 `--default-background-color=00000000` 保留透明底。

## 怎么换图

**替换已有一家**：把新的 PNG 覆盖到 `app/src/main/assets/logos/<平台id>.png`，重新打包即可，不用改代码。

**给没有 logo 的 6 家补上**：放一个 `<平台id>.png` 进去就自动生效。

平台 id 必须和 `Provider.java` 里的 id 完全一致：

```
deepseek  zhipu  moonshot  dashscope  volc  hunyuan  qianfan  siliconflow
minimax   stepfun  xfyun   yi  baichuan  openai  anthropic  gemini  xai
openrouter  groq
```

**换自定义平台图标**：应用内「新建平台」时可以从相册选图，存在应用私有目录，优先级高于内置 logo。

## 版权说明

这些是各家公司的官方标识，**版权与商标权归各自所有者**。
本项目仅将其用于「指示对应平台」这一用途，不声称任何所有权，也不表示与该等公司存在关联、合作或授权关系。

如果你要商用或二次分发，请自行评估商标使用风险，或改用自绘图标。
