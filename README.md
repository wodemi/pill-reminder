# 按时吃药 · pill-reminder

一个开源的安卓吃药提醒 App，为「怕忘吃药」而做。

深色界面、闹钟级提醒（满屏响铃 + 震动，锁屏也响）、内置 123 种常用药、按时段分组、库存提醒。

## 功能

- ⏰ **闹钟级提醒**：`AlarmManager.setAlarmClock`，系统闹钟优先级，国产 ROM 不能推迟
- 📺 **全屏响铃页**：锁屏亮屏 + 循环闹钟铃声 + 震动，2 分钟无人处理自动停铃
- 💊 **内置药库**：123 种常用药，按 16 个分类筛选，支持搜索
- 🗂 **按时段分组**：早上 / 中午 / 下午 / 晚上 / 睡前
- 📦 **库存管理**：剩余数量，快吃完会标黄
- 🔔 **通知操作**：通知栏直接「已服用 / 10 分钟后」
- 🩺 **首次启动体检**：一次性引导你开 7 项必要权限（通知 / 精确闹钟 / 电池 / 自启动 / 悬浮窗 / 全屏通知 / 后台守护）
- 🌙 深色主题，`☾/☀` 一键切换

## 安装

从 [Releases](https://github.com/wodemi/pill-reminder/releases) 下载 `pill-reminder-v9.apk` 安装。

要求：Android 8.0（API 26）及以上，已针对 ARMv7 / ARM64 打包。

> 首次打开会有一个体检页，跟着点完 7 项，**尤其是「自启动」和「后台守护」**——不然国产 ROM 会杀进程导致不响。

## 编译

本项目不依赖 Android Studio，用 `aapt2 + javac + d8 + apksigner` 手工构建：

```powershell
# 需要：JDK 17、Android SDK build-tools 34、platform-35
$env:JAVA_HOME = 'C:\path\to\jdk-17'
.\build.ps1
```

产物在 `pill-reminder.apk`。

## 项目结构

```
app/
  AndroidManifest.xml
  src/com/ikun/pill/
    MainActivity.java      主界面（自绘进度环、时段分组、添加对话框）
    AlarmScheduler.java    闹钟排程（setAlarmClock / 稍后提醒）
    ReminderReceiver.java  闹钟触发 → 通知 + 拉起响铃页
    AlarmActivity.java     全屏响铃页（铃声 + 震动 + 三个按钮）
    Notify.java            通知渠道与通知构建
    GuardService.java      前台服务保活
    BootReceiver.java      开机重排闹钟
    Store.java             SharedPreferences 持久化
    Med.java               药品数据模型
    Drugs.java             内置药库（123 种 / 16 分类）
  res/                     图标与颜色
build.ps1                  一键构建脚本
```

## 已知问题

- 部分国产 ROM（如荣耀 MagicOS）需要手动开自启动，App 只能引导，查不到状态
- 蓝牙手柄/键盘不影响，但输入框只支持系统输入法
- 汉化只做了简体中文

## 许可

MIT License