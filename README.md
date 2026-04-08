# WordDuo

偏多邻国风格的 Android 背词应用，重点强化听写、发音和错词回顾。

## 已实现功能

- 背诵模式
- 发音检查
- 释义回忆
- 拼写挑战
- 错词复习
- 今日目标 / XP / 连续学习天数
- 本地词库搜索
- 本地离线进度保存
- 正式雅思听力拼写词库（已由 PDF 转换导入）

## 技术栈

- Kotlin
- Jetpack Compose
- Navigation Compose
- SharedPreferences
- TextToSpeech
- SpeechRecognizer

## 使用方式

1. 用 Android Studio 打开 `e:/INT4074`
2. 首次 Sync 下载 Gradle 与依赖
3. 运行到 Android 8.0+ 设备或模拟器

## 题库导入

当前已经将 `听力拼写词汇.pdf` 转换为正式词库，并写入 `app/src/main/assets/words.json`。

当前词库字段结构如下：

```json
{
  "id": "abandon",
  "word": "abandon",
  "phonetic": "",
  "meaning": "缺席",
  "example": "",
  "level": "IELTS",
  "tags": ["听写", "名词"]
}
```


