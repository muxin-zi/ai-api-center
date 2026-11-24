# 开发说明

面向想改代码的人。用户向的说明在 [README](../README.md)。

## 代码结构

| 文件 | 行数 | 职责 |
|---|---|---|
| `MainActivity.java` | 2557 | 全部界面。主页 / 详情页 / 网页页 / 密钥本 / 选平台页 / 自定义表单，靠 `setVisibility` 切换，单 Activity 无嵌套 |
| `Provider.java` | 274 | 19 家平台数据表 + 余额接口声明 `BalanceSpec` + 自定义图片路径表 |
| `Balance.java` | 275 | 余额请求与解析。`HttpURLConnection` + 子线程 + 主线程回调，结果缓存进 SP，失败不覆盖上次成功值 |
| `KeyVault.java` | 214 | 密钥存储。AndroidKeyStore 生成 AES-256 → AES/GCM → SharedPreferences |
| `CustomStore.java` | 86 | 自定义平台的 JSON 存储 |
| `KeyEntry.java` | 38 | 密钥条目：id / name / purpose / key，四者可空 |

零第三方依赖，只需要 Android SDK 34 + JDK 17（`compileOptions` 是 Java 11）。

## 容易踩的点

- 界面全部代码构建，**没有 layout XML**（只有 `styles.xml` 定义主题）。
- 弹窗统一走 `UiKit` 风格的内置方法，不要引入系统 `AlertDialog`，否则画风会突变。
- 图标走 `providerIcon()` 三级回退：用户指定图片 → `assets/logos/<pid>.png` → 品牌色 + 首字母。
- `assets/logos/<pid>.png` 里的 `<pid>` 必须和 `Provider.java` 里的 id **完全一致**，否则 logo 不显示。

## 请不要改的

| 不要动 | 原因 |
|---|---|
| `allowBackup` | 已设为 `false`，密钥不能进云备份，这是安全要求 |
| 示例代码里的 Key | 只能用 `sk-xxxxxxxx` 这类假值 |
| README 的免责声明 | 「与各公司无关联、未获授权」是必要声明 |
| logo 的版权声明 | `assets/logos/` 是各家官方标识，版权归各自公司，仅作识别用途 |

提交时不要带上 `local.properties`、`app/build/`、`.gradle/`（`.gitignore` 已排除）。

## 深链维护

核心资产是 `docs/providers.json` 里的 76 条深链，各家控制台改版频繁，需要持续维护。

可以加一个定时任务自动探测失效链接并开 issue：判断要两件事——HTTP 状态码，以及**返回体是不是登录页**。
多数控制台对未登录请求返回 200 + 登录页，只看状态码会全部误判为正常。
