# 自动记账（AutoLedger）

安卓本地自用记账 App：监听微信/支付宝的支付通知 → 自动解析金额/商户 → 存进手机本地数据库。
**只给自己一个人用，不上架任何应用商店。** 除「检查新版本」会联网读一下仓库版本号外，**不采集、不上传任何账单或个人数据**。

---

## ⚠️ 重要：关于「读取通知」这个权限

本 App 的核心功能依赖系统的**「通知使用权（通知读取权限）」**，这是一个**比较敏感、级别较高的权限**：

- 开启后，App 能**读到手机上所有 App 弹出的通知内容**——不限于微信/支付宝，理论上包括短信、其他 App 的推送等。
- 因此系统在你授权时会弹出**醒目警告**，这是正常的、必须的提示，不是 App 异常。
- **本项目实际只处理**：你在设置里勾选的监听对象（默认微信 + 支付宝），以及**只提取其中的支付类通知**（有金额的那种），其余通知一律直接忽略、不记录、不存储。
- **所有数据只存在你手机本地**，不上传任何服务器；App 也不申请短信、联系人、定位、相机等权限。

> 一句话：这个权限权限确实敏感，但本 App 只拿它做「自动记账」这一件事，且数据完全本地。如果你介意读取通知这个动作本身，那就不适合用「通知监听」类的记账方案（自动记账的前提就是读通知）。

---

## 版本更新（检查更新）

- App 每次打开会联网读取仓库根目录的 `version.json`（一个只读请求），和本地版本号对比，云端更新就弹「发现新版本」，点「去更新」跳转下载 APK。**这个联网请求不包含任何个人信息**。
- 具体发版流程见文末「附录：发新版流程」。

---

## 更新记录

### v1.4.1
| 项 | 变化 |
|---|---|
| 识图更准 | 重写账单解析：正确识别「转账-转给XX / 扫二维码付款-给XX / 直接商户名」三种格式；修复收入（红包/收款）被误判为支出；正确解析每条账单的具体日期和时间（不再全归到某一天） |
| 结果可编辑 | 识别出的每条账单可点编辑，改商户名、金额、收支方向、日期、时分 |
| 版本 | 1.4.1 |

### v1.4.0
| 项 | 变化 |
|---|---|
| 识图记账 | 主页顶部新增「识图」按钮，导入账单截图 → **本地离线 OCR 识别**（图片不出手机、不联网）→ 列出候选账单可逐条勾选 → 确认入账；多账单流水按「一行一条」切分 |
| 未识别商户 | 只有金额、解析不出商户时，自动按来源填「微信支付 / 支付宝」，不再显示「未识别商户」 |
| 误记修复 | 解析前先校验通知来源，只有「微信支付/支付宝」等支付服务通知才处理，朋友聊天消息不再误记成假账单 |
| APK 命名 | 产物改为 `AutoLedger-vX.Y.Z.apk`（带版本号），不再叫 `app-release.apk` |
| 体积 | 因内置中文 OCR 模型，APK 约 52MB（本地离线识别的代价） |

### v1.3.1
| 项 | 变化 |
|---|---|
| 首页 | 卡片金额统一黑色；「本月支出/收入/结余」三个标签轮换显示，点箭头调换顺序 |
| 账单/报表 | 左右滑动切月方向调整：从左往右滑 = 上一月 |
| 检查更新 | 支持联网检查新版本（v1.3.0 引入） |

### v1.1

| 项 | 变化 |
|---|---|
| 底部导航 | 从 3 个 Tab 变 4 个：**主页 / 账单 / 报表 / 我的** |
| 新增主页 | 顶部大卡片默认显示本月支出，点左侧圆形箭头依次切到**本月收入 → 月结余**；下方只展示**近三日账单** |
| 账单页 | 月度汇总 + **按日柱状图** + 月结余/日均支出 + 按日期分组的明细，增加搜索与分类筛选 |
| 报表页 | 环形占比图 + 分类进度条，顶部还能切到**日历**视图，直接在月历上看每天花了多少 |
| 记一笔 | 改成**自绘数字键盘**（不弹系统输入法）+ 分类图标网格，顶部只保留支出/收入 |
| 配色 | 墨绿 → **青绿/薄荷**，顶部色带延伸到状态栏下方 |
| 性能 | 修掉「我的」页进入卡顿（识别规则 38 条一次性渲染 + 进页面就全机扫描应用列表） |
| 分类 | 细分为餐饮/购物/交通/住宿/日常/学习/人情/娱乐/美妆/旅游/医疗/通讯/基金/其他，老数据的「日用/居住」会自动改名 |
| 签名 | 已配好正式签名证书，后续版本可以直接覆盖升级，不用卸载 |

| 项 | 变化 |
|---|---|
| 底部导航 | 从 3 个 Tab 变 4 个：**主页 / 账单 / 报表 / 我的** |
| 新增主页 | 顶部大卡片默认显示本月支出，点左侧圆形箭头依次切到**本月收入 → 月结余**；下方只展示**近三日账单** |
| 账单页 | 月度汇总 + **按日柱状图** + 月结余/日均支出 + 按日期分组的明细，增加搜索与分类筛选 |
| 报表页 | 环形占比图 + 分类进度条，可在**大类 / 小类**间切换；顶部还能切到**日历**视图，直接在月历上看每天花了多少 |
| 记一笔 | 改成**自绘数字键盘**（不弹系统输入法）+ 分类图标网格，顶部只保留支出/收入 |
| 配色 | 墨绿 → **青绿/薄荷**，顶部色带延伸到状态栏下方 |
| 性能 | 修掉「我的」页进入卡顿（识别规则 38 条一次性渲染 + 进页面就全机扫描应用列表） |
| 分类 | 细分为餐饮/购物/交通/住宿/日常/学习/人情/娱乐/美妆/旅游/医疗/通讯/基金/其他，老数据的「日用/居住」会自动改名 |
| 签名 | 已配好正式签名证书，后续版本可以直接覆盖升级，不用卸载 |

---

## 一、环境准备（一次性）

| 工具 | 版本要求 | 说明 |
|---|---|---|
| Android Studio | 2024.2 或更新 | 本机装在 `D:\Android Studio` |
| **Gradle JDK** | **21**（必需） | 本机已装 `D:\dev\jdk-21.0.2`，并已写进 `gradle.properties` 的 `org.gradle.java.home`。用 JDK 25 会报 "Gradle 8.9 incompatible with JVM 25" |
| Android SDK | API 35（Platform 35） | SDK Manager 里勾上 Android 15 (API 35) + Build-Tools 35 + Command-line Tools |
| 手机 | Android 8.0 及以上 | 设置里打开「开发者选项 → USB 调试」 |

---

## 二、打开工程

1. Android Studio → `Open` → 选本目录 `AutoLedger`
2. 首次会提示 **Gradle wrapper 缺失**，点 `OK / Use Gradle from: gradle-wrapper.properties`，它会自动下载 Gradle 8.9
   - 若始终报错，在这个目录执行一次（需本机已装 Gradle 8.9+）：`gradle wrapper --gradle-version 8.9`
3. 等待 `Gradle Sync` 完成（第一次要下依赖，5~10 分钟）

---

## 三、打包 APK

### 方式 A：直接出 debug 包（自己用，最快）

菜单 `Build → Build App Bundle(s) / APK(s) → Build APK(s)`
产物：`app/build/outputs/apk/debug/app-debug.apk`

或者命令行（Windows 下用 `gradlew.bat`）：

```bash
./gradlew assembleDebug
```

### 方式 B：正式签名包（推荐，升级不会被系统拦）

**签名证书已经生成好了**，不用再自己跑 keytool：

| 文件 | 位置 | 说明 |
|---|---|---|
| 证书 | `keystore/autoledger.jks` | 有效期 30 年（到 2056 年） |
| 口令 | `key.properties` | storePassword / keyPassword 已写入本文件（**不公开**，见下方安全说明） |

> 🔒 **安全说明**：`keystore/` 和 `key.properties` 两个文件都在 `.gitignore` 里，**不会被提交到仓库**，也不会公开。签名口令只存在于你本机的 `key.properties`，请勿把口令写进任何提交的内容里。

⚠️ **这两个文件一定要备份到网盘或 U 盘**。丢了就再也签不出同一签名的包，只能卸载重装，账目数据会一起清空。

**打包：**

```bash
./gradlew assembleRelease
```

产物：`app/build/outputs/apk/release/AutoLedger-v1.4.0.apk`（约 52MB，含本地 OCR 模型）

**从 v1.0 升级到 v1.1 要注意**：v1.0 装的是 debug 签名包，和这个正式签名不是同一个证书，系统会拒绝覆盖安装（报 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`）。
所以第一次要把旧版**卸载**再装新版（会清掉 v1.0 的测试数据）。从 v1.1 往后，只要 keystore 不变，都能直接覆盖升级、数据保留。

### 装到手机

- 数据线连电脑：`./gradlew installDebug`，或直接把 APK 拷到手机点安装
- 首次安装需在手机上允许「安装未知来源应用」

---

## 四、装完必做的 4 件事（不做等于白装）

1. 打开 App → 底部「我的」→ **通知读取权限** → 在系统列表里找到「自动记账 · 支付通知读取」并打开
   > ⚠️ 这一步系统会弹「允许读取所有通知」之类的醒目警告，**请先读一下开头的隐私说明再决定是否开启**。这是自动记账的必需权限，无法绕过。
2. **电池优化白名单** → 设为「不限制 / 允许后台运行」
3. **自启动** → 应用详情里打开「自启动 / 关联启动 / 后台弹窗」
4. 用微信或支付宝真实付一笔小额（如 0.01 元），回 App 看是否自动出现一条

> 小米/红米：还需在「安全中心 → 应用管理 → 自动记账 → 省电策略」选「无限制」，并在最近任务里给 App 上锁。
> 华为/荣耀：应用启动管理里关掉「自动管理」，手动打开三个开关。
> OPPO/vivo：允许「后台运行」「自启动」「关联启动」。

---

## 五、识别不准怎么修（不用重新打包）

「我的 → 识别规则」：

- **新增「商户→分类」规则**：关键词写商户名的一部分，如 `星巴克` → 餐饮
- **新增「收入词」**：如 `红包`，让这类通知判成收入
- **测试一句话**：把真机通知原文粘进去，立刻看到解析结果（金额/方向/商户/分类）

如果通知里根本没有金额（例如只写「你有一笔新的收款」），说明这条通知本身信息不足，只能手动补记 —— 这是通知方案的天然边界。

---

## 六、常见报错

| 报错 | 原因与解决 |
|---|---|
| `Could not install Gradle distribution ... SocketTimeoutException` | 连不上 services.gradle.org。`gradle-wrapper.properties` 里已改成腾讯云镜像，若仍失败则手动下载 `gradle-8.9-bin.zip` 放到 `C:\Users\<用户名>\.gradle\wrapper\dists\gradle-8.9-bin\<一串随机字符>\` |
| `Gradle 8.9 is incompatible with JVM 25` / `Unsupported class file major version 65` | Gradle JDK 选错。已用 `gradle.properties` 的 `org.gradle.java.home` 固定到 `D:\dev\jdk-21.0.2`，确认该目录存在 |
| `Unsupported class file major version 61` / `invalid source release: 17` | 用了 JDK 8，同上，检查 Gradle JDK |
| `Failed to find Build Tools revision` | SDK Manager 里装 API 35 的 Build-Tools |
| `cmdline-tools component is missing` | SDK Manager → SDK Tools → 勾 Android SDK Command-line Tools |
| `Unexpected exception while resolving Gradle distribution sources ... gradle-8.9-src.zip` | **可以忽略**。这是 Android Studio 为构建脚本下载「Gradle 源码包」失败（该地址在 Gradle 内部固定为 services.gradle.org，改不了镜像）。不影响编译打包，只影响在 `build.gradle.kts` 里按住 Ctrl 点进 Gradle API 源码。已在 `~/.gradle/gradle.properties` 里把超时压到 10 秒，让它快速失败而不是卡几分钟 |
| `Gradle build failed`（改完配置后） | `./gradlew clean` 再编；仍失败删 `.gradle` 和 `build` 目录 |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 手机上有不同签名版本，先卸载旧版再装 |
| App 装了但一条记录都没有 | 99% 是通知使用权没开，或后台被系统杀了，按第四节检查 |
| 通知能收到，但记账重复/漏记 | 调 `我的 → 全自动入账` 开关；重复记录看下方「去重窗口」说明 |

**去重规则**：同一来源、同一金额、60 秒内只记一条（`AppSettings.dedupeSeconds`，默认 60）。

---

## 七、代码结构

```
app/src/main/java/com/xtgxiso/autoledger/
├── AutoLedgerApp.kt            应用入口，持有数据库/仓库/设置
├── MainActivity.kt             唯一 Activity
├── data/
│   ├── Transaction.kt          账目实体（金额用「分」存 Long）
│   ├── TransactionDao.kt       账目查询/去重/增删改
│   ├── RuleEntity.kt           识别规则 + 默认规则表
│   ├── AppDatabase.kt          Room 数据库
│   ├── AppSettings.kt          DataStore 设置（监听范围/自动入账）
│   └── TransactionRepository.kt 通知→解析→去重→入库全流程
├── parse/
│   └── NotificationParser.kt   纯文本解析（正则 + 规则），无 Android 依赖
├── service/
│   └── PaymentNotificationListener.kt  通知监听服务
├── update/
│   └── UpdateChecker.kt    联网检查新版本（读云端 version.json，静默失败）
├── ocr/
│   ├── OcrEngine.kt        本地 OCR 识别（ML Kit 中文模型，离线）
│   └── OcrBillParser.kt    把 OCR 文字解析成多条候选账单（商户+金额+方向+日期）
├── ui/
│   ├── AppRoot.kt              4 个 Tab 的骨架
│   ├── HomeScreen.kt           首页卡片 + 近三日账单
│   ├── BillsScreen.kt          账单页（月汇总/柱状图/明细）
│   ├── StatsScreen.kt          报表页（环形图/日历）
│   ├── OcrImportScreen.kt      识图导入（选图→识别→勾选→入账）
│   ├── SettingsScreen.kt       权限/监听应用/规则/保活/数据
│   ├── EditScreen.kt           记一笔 / 编辑 / 删除
│   ├── Pickers.kt              滚轮选择器 / 月份选择器
│   ├── Common.kt               公共组件与权限状态 Hook
│   ├── LedgerViewModel.kt      全部业务状态
│   └── theme/Theme.kt          配色
└── util/
    ├── Utils.kt                金额（分↔元）、日期、分类常量
    └── SystemIntents.kt        跳系统设置页、读取已安装应用
```

---

## 八、边界声明

- 本项目**仅限个人本地自用**，不上架任何应用商店（通知监听权限属高危敏感权限，审核必驳）。
- 除「检查新版本」的只读联网请求外，**不采集、不上传任何数据**；所有账单只存在本机，卸载即清空，无云同步、无账号体系。
- 微信/支付宝更新通知格式后可能需要调整规则，这是该方案的固有成本。

---

## 附录：发新版流程

改一次代码、发一个新版本，照着下面 5 步走（版本号以 v1.3.1 → v1.4.0 为例）：

1. **改版本号**：`app/build.gradle.kts` 里 `versionCode`（+1）和 `versionName` 改新值。
2. **编译**：`./gradlew assembleRelease`，产物在 `app/build/outputs/apk/release/AutoLedger-vX.Y.Z.apk`。
3. **上传发行版**：Gitee 仓库 →「发行版/Release」→ 新建，标签填新版本号（如 `v1.4`），上传 APK 附件，复制下载直链（格式 `https://gitee.com/mouyu-gitee/zidongjizhanganzhuoapp/releases/download/v1.4/AutoLedger-v1.4.0.apk`）。
4. **更新 version.json**：把 `versionCode`/`versionName` 改成新值，`downloadUrl` 填第 3 步的直链，`note` 写更新说明。
5. **推送**：`git push`。之后所有老用户打开 App 就会自动弹「发现新版本」。
