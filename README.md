# 词光 Ciguang

词光是一款面向英语词汇学习的 Android 应用，核心目标是帮助用户完成单词背诵、拼写训练、发音检查、错词复习与写作检测。

本项目使用 Kotlin + Jetpack Compose 开发，界面采用奶油白卡片化风格。

## Core Features

- 背诵模式：看词记义，建立初始印象
- 发音检查：播放标准音，跟读并检查发音结果
- 释义回忆：根据中文释义回忆英文单词
- 拼写挑战：播放单词发音并完成拼写
- 错词复习：自动聚合高频错误词
- 对战模式：支持三种玩法
  - 多人对战：局域网建房或加入别人房间
  - 拼写 AI 挑战：5 题拼写赛，比正确率和总耗时
  - 释义速选：根据中文释义进行四选一挑战
- AI 写作检测：支持粘贴作文或导入图片 OCR，再进行评分与纠错
- 登录注册：本地账号注册与登录
- 学习统计：每日目标、XP、连胜天数、本地进度保存
- 个人页 Profile：头像上传、目标设置、词库与错词本二级入口
- 词库搜索：支持按英文或中文释义检索

## Tech Stack

- Kotlin
- Jetpack Compose
- Navigation Compose
- SharedPreferences
- Android TextToSpeech
- Android SpeechRecognizer
- ML Kit Text Recognition
- Local socket-based LAN battle
- OpenAI-compatible AI API integration

## Environment

- Android Studio Hedgehog 或更高版本
- JDK 17
- Gradle Wrapper: 8.10.2
- minSdk 26
- targetSdk 35

## How To Run

1. Clone or download this repository.
2. Open [e:/INT4074](e:/INT4074) in Android Studio.
3. Make sure the Gradle JDK is set to JDK 17.
4. Sync the project.
5. Run on an Android 8.0+ device or emulator.

## Build APK

You can build the debug APK with:

```powershell
cd e:\INT4074
.\gradlew.bat assembleDebug
```

After build, the APK will be under:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Repository Structure

```text
app/src/main/java/com/int4074/wordduo/
  MainActivity.kt
  data/
    BattleRepository.kt
    Models.kt
    StudyRepository.kt
  ui/
    AdvancedFeatureScreens.kt
    AiEssayService.kt
    AppViewModel.kt
    AuthScreen.kt
    EssayReviewEngine.kt
    LibrarySettingsScreens.kt
    PracticeScreens.kt
    SpeechTools.kt
    UiChrome.kt
    WordDuoApp.kt
    theme/Theme.kt
app/src/main/assets/
  words.json
```

## Word Bank Source

The formal word bank has been converted from the provided PDF vocabulary list and imported into:

```text
app/src/main/assets/words.json
```

Current word bank size:

- 2397 entries

Word fields:

```json
{
  "id": "ability",
  "word": "ability",
  "phonetic": "",
  "meaning": "能力",
  "example": "",
  "level": "IELTS",
  "tags": ["听写", "名词"]
}
```

Note:

- The original PDF source mainly provides word, part of speech and meaning.
- Many entries do not include phonetic symbols or example sentences.
- The app therefore uses conditional display and generated fallback examples in some learning screens.

## Feature Notes

- 发音检查需要麦克风权限、系统语音识别服务和可用的英文 TTS 引擎。
- AI 写作检测支持真实 API 接入，也保留本地回退评估逻辑，用于第三方接口失败时继续演示。
- 多人对战需要两台设备连接同一可互访网络，演示时建议优先使用同一个手机热点。
- 模拟器的语音能力可能不完整，发音相关功能建议优先使用真机测试。

## Source Code Download

Source code can be obtained by either:

1. Downloading the ZIP directly from GitHub
2. Cloning the repository:

```bash
git clone https://github.com/SleepyPao/RememberMe.git
```


