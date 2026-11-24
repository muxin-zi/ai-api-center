# v2.4 修复：换站点时"先闪出上一家平台"（历史叠加）

## 现象

在真机上复现：进过 DeepSeek 的网页之后，再打开其他任何平台的网页，
**会先看到 DeepSeek 的页面**，然后才变成目标站点；按返回键也会退回 DeepSeek。
用户描述为"叠加了"。

## 两个真实原因

### 原因一：WebView 容器显示时，里面还是上一家平台的页面

`closeWeb()` 只做 `webPage.setVisibility(GONE)`，**WebView 里的页面并没有被清掉**。
下次 `openWeb()` 一执行 `webPage.setVisibility(VISIBLE)`，这一帧画出来的就是上一家的页面，
新地址的网络请求还没回来。已缓存的控制台站点（如 DeepSeek）渲染极快，
所以这个"旧页面"停留得非常明显。

### 原因二：`clearHistory()` 不会清掉"当前页"

原代码在 `loadUrl()` 之前调用 `web.clearHistory()`。Android 文档写的是：
清空前进/后退列表，**但当前页会保留**。所以历史实际是 `[DeepSeek, 新站点]`，
`canGoBack()` 为 true → 按返回就退回 DeepSeek。

## 修复方式（改动都在 `MainActivity.java`）

### 1. 加一层遮罩 `webVeil`

在 `buildWebPage()` 里，`web` 之后、进度条之前插入一个和网页区等高、同底色的 FrameLayout，
中间一行"载入中…"。

### 2. `openWeb()` 打开遮罩，不再在加载前调 `clearHistory()`

```java
web.getSettings().setUserAgentString(ua());
showVeil();
historyResetPending = true;   // 等新页面加载完再清历史
web.stopLoading();
web.loadUrl(url);
```

### 3. `onPageFinished()` 里清历史

```java
@Override
public void onPageFinished(WebView v, String url) {
    if (historyResetPending) {
        historyResetPending = false;
        v.clearHistory();   // 此时"当前页"是新站点，被清掉的正好是上一家的记录
    }
    hideVeil();
}
```

### 4. `onPageCommitVisible()` 提前揭开遮罩

新页面一可以绘制就揭开，不用等图片全部加载完：

```java
@Override
public void onPageCommitVisible(WebView v, String url) {
    hideVeil();
}
```

### 5. 兜底：错误与超时

- `onReceivedError(..., req.isForMainFrame())` → `hideVeil()`，让用户看到错误页而不是一直空屏
- `showVeil()` 里挂一个 8 秒的 `postDelayed`，页面始终没提交也会揭开

## 注意

- `onPageCommitVisible` 需要 API 23，本项目 `minSdk 23`，正好满足
- `WebResourceError` 需要 `import android.webkit.WebResourceError;`
- 不要为了"干净"在 `closeWeb()` 里加载 `about:blank`：那会丢掉页面状态，
  下次进同一家要整页重载，而且 `about:blank` 自己会留在历史里

## 真机验证方法

1. 进 DeepSeek → 点「API 页」→ 等页面出来 → 点左上 ‹ 返回
2. 返回主页 → 进 Kimi → 点「费用页」
3. 预期：看到的是"载入中…"，**不应先闪出 DeepSeek**；按系统返回键应直接回到 Kimi 的本地详情页，
   **不应退回 DeepSeek**
