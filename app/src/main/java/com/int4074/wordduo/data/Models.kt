package com.int4074.wordduo.data

data class WordEntry(
    val id: String,
    val word: String,
    val phonetic: String,
    val meaning: String,
    val example: String,
    val level: String,
    val tags: List<String>
)

data class WordProgress(
    val knownCount: Int = 0,
    val wrongCount: Int = 0,
    val lastMode: PracticeMode? = null
) {
    val isWeak: Boolean get() = wrongCount > knownCount
    val mastery: Float get() = (knownCount.toFloat() / (knownCount + wrongCount).coerceAtLeast(1)).coerceIn(0f, 1f)
}

data class UserStats(
    val xp: Int = 0,
    val streakDays: Int = 0,
    val dailyGoal: Int = 15,
    val studiedToday: Int = 0,
    val lastStudyDate: String = ""
)

data class AuthState(
    val isLoggedIn: Boolean = false,
    val currentUser: String = "",
    val lastAccount: String = "",
    val authMessage: String? = null
)

enum class PracticeMode(
    val title: String,
    val subtitle: String,
    val badge: String
) {
    Recite("背诵模式", "看词记义，建立初始印象", "背"),
    Pronunciation("发音检查", "跟读并识别发音是否接近", "读"),
    Meaning("释义回忆", "根据中文释义回忆英文", "义"),
    Spelling("拼写挑战", "听音拼写，训练听写能力", "拼"),
    MistakeReview("错词复习", "集中击破易错词", "错")
}

data class PracticeQuestion(
    val word: WordEntry,
    val mode: PracticeMode
)

data class SessionState(
    val mode: PracticeMode = PracticeMode.Recite,
    val queue: List<PracticeQuestion> = emptyList(),
    val index: Int = 0,
    val input: String = "",
    val revealAnswer: Boolean = false,
    val feedback: String? = null,
    val sessionXp: Int = 0,
    val completed: Boolean = false,
    val lastRecognition: String = ""
) {
    val current: PracticeQuestion? get() = queue.getOrNull(index)
}

data class LibraryState(
    val words: List<WordEntry> = emptyList(),
    val progress: Map<String, WordProgress> = emptyMap(),
    val stats: UserStats = UserStats()
) {
    val weakWords: List<WordEntry>
        get() = words.filter { progress[it.id]?.isWeak == true }
}
