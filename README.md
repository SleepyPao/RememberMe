# 词光 Ciguang

词光是一款面向英语词汇学习的 Android 应用，核心目标是帮助用户完成单词背诵、拼写训练、发音检查和错词复习。

本项目使用 Kotlin + Jetpack Compose 开发，界面风格参考多邻国并做了更柔和的奶油白卡片化设计，适合作为课程项目演示与提交。

## Core Features

- 背诵模式：看词记义，建立初始印象
- 发音检查：播放标准音，跟读并检查发音结果
- 释义回忆：根据中文释义回忆英文单词
- 拼写挑战：播放单词发音并完成拼写
- 错词复习：自动聚合高频错误词
- 登录注册：本地账号注册与登录
- 学习统计：每日目标、XP、连胜天数、本地进度保存
- 词库搜索：支持按英文或中文释义检索
- 正式题库导入：已将课程提供的 PDF 词库转换并导入应用

## Tech Stack

- Kotlin
- Jetpack Compose
- Navigation Compose
- SharedPreferences
- Android TextToSpeech
- Android SpeechRecognizer

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
    Models.kt
    StudyRepository.kt
  ui/
    AppViewModel.kt
    AuthScreen.kt
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

## Speech Feature Notes

- 发音检查需要麦克风权限。
- 设备需要系统语音识别服务才能使用跟读检查。
- 设备需要可用的英文 TTS 引擎才能播放标准音。
- 模拟器语音能力可能不完整，演示时建议优先使用真机。

## Source Code Download

Teachers can obtain the source code by either:

1. Downloading the ZIP directly from GitHub
2. Cloning the repository:

```bash
git clone https://github.com/SleepyPao/RememberMe.git
```

## Submission Notes

This repository contains the Android Studio source project for the coursework submission. The app name shown on device is:

- 词光

If needed for assessment, this repository can be provided together with:

- APK file
- presentation slides
- demo video
- source code link
