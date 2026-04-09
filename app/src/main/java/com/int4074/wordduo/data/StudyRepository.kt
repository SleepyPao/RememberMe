package com.int4074.wordduo.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StudyRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("word_duo_prefs", Context.MODE_PRIVATE)
    private val seedWords = loadSeedWords(context)

    private val _state = MutableStateFlow(loadLibrary())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val _authState = MutableStateFlow(loadAuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun buildSession(mode: PracticeMode, size: Int = 8): SessionState {
        val current = _state.value
        val candidates = when (mode) {
            PracticeMode.MistakeReview -> current.weakWords.ifEmpty {
                current.words.sortedByDescending { current.progress[it.id]?.wrongCount ?: 0 }
            }
            PracticeMode.Recite -> current.words.sortedBy { current.progress[it.id]?.knownCount ?: 0 }
            else -> current.words.shuffled()
        }
        return SessionState(
            mode = mode,
            queue = candidates.take(size).map { PracticeQuestion(it, mode) }
        )
    }

    fun login(account: String, password: String) {
        val savedAccount = prefs.getString("auth_account", "").orEmpty()
        val savedPassword = prefs.getString("auth_password", "").orEmpty()
        val avatarUri = prefs.getString("auth_avatar_uri", "").orEmpty()
        _authState.value = when {
            account.isBlank() || password.isBlank() -> AuthState(lastAccount = account, authMessage = "请输入账号和密码")
            account == savedAccount && password == savedPassword -> {
                prefs.edit().putBoolean("auth_logged_in", true).apply()
                AuthState(isLoggedIn = true, currentUser = account, avatarUri = avatarUri, lastAccount = account)
            }
            else -> AuthState(lastAccount = account, authMessage = "账号或密码错误")
        }
    }

    fun register(account: String, password: String, confirmPassword: String) {
        _authState.value = when {
            account.isBlank() || password.isBlank() -> AuthState(lastAccount = account, authMessage = "请完整填写注册信息")
            password.length < 6 -> AuthState(lastAccount = account, authMessage = "密码至少需要 6 位")
            password != confirmPassword -> AuthState(lastAccount = account, authMessage = "两次密码输入不一致")
            else -> {
                prefs.edit()
                    .putString("auth_account", account)
                    .putString("auth_password", password)
                    .putBoolean("auth_logged_in", true)
                    .apply()
                AuthState(isLoggedIn = true, currentUser = account, lastAccount = account)
            }
        }
    }

    fun updateAvatarUri(uri: String) {
        prefs.edit().putString("auth_avatar_uri", uri).apply()
        _authState.value = _authState.value.copy(avatarUri = uri)
    }

    fun logout() {
        prefs.edit().putBoolean("auth_logged_in", false).apply()
        _authState.value = AuthState(lastAccount = _authState.value.currentUser, avatarUri = _authState.value.avatarUri)
    }

    fun clearAuthMessage() {
        _authState.value = _authState.value.copy(authMessage = null)
    }

    fun submitResult(wordId: String, mode: PracticeMode, correct: Boolean, xpDelta: Int) {
        val current = _state.value
        val old = current.progress[wordId] ?: WordProgress()
        val newProgress = old.copy(
            knownCount = old.knownCount + if (correct) 1 else 0,
            wrongCount = old.wrongCount + if (correct) 0 else 1,
            lastMode = mode
        )
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_DATE)
        val stats = current.stats.let {
            val sameDay = it.lastStudyDate == today
            it.copy(
                xp = it.xp + xpDelta,
                studiedToday = if (sameDay) it.studiedToday + 1 else 1,
                streakDays = when {
                    it.lastStudyDate.isBlank() -> 1
                    it.lastStudyDate == today -> it.streakDays
                    it.lastStudyDate == yesterday -> it.streakDays + 1
                    else -> 1
                },
                lastStudyDate = today
            )
        }
        val next = current.copy(
            progress = current.progress + (wordId to newProgress),
            stats = stats
        )
        save(next)
        _state.value = next
    }

    fun updateDailyGoal(goal: Int) {
        val next = _state.value.copy(stats = _state.value.stats.copy(dailyGoal = goal.coerceIn(1, 200)))
        save(next)
        _state.value = next
    }

    private fun loadLibrary(): LibraryState {
        val progressJson = JSONObject(prefs.getString("progress_json", "{}").orEmpty())
        val progress = buildMap {
            progressJson.keys().forEach { key ->
                val item = progressJson.getJSONObject(key)
                put(
                    key,
                    WordProgress(
                        knownCount = item.optInt("knownCount"),
                        wrongCount = item.optInt("wrongCount"),
                        lastMode = item.optString("lastMode").takeIf { it.isNotBlank() }?.let(PracticeMode::valueOf)
                    )
                )
            }
        }
        val statsJson = JSONObject(prefs.getString("stats_json", "{}").orEmpty())
        val rawStats = UserStats(
            xp = statsJson.optInt("xp"),
            streakDays = statsJson.optInt("streakDays"),
            dailyGoal = statsJson.optInt("dailyGoal", 15),
            studiedToday = statsJson.optInt("studiedToday"),
            lastStudyDate = statsJson.optString("lastStudyDate")
        )
        val normalizedStats = normalizeStats(rawStats)
        val library = LibraryState(
            words = seedWords,
            progress = progress,
            stats = normalizedStats
        )
        if (normalizedStats != rawStats) {
            save(library)
        }
        return library
    }

    private fun loadAuthState(): AuthState {
        val account = prefs.getString("auth_account", "").orEmpty()
        val avatarUri = prefs.getString("auth_avatar_uri", "").orEmpty()
        val loggedIn = prefs.getBoolean("auth_logged_in", false)
        return AuthState(
            isLoggedIn = loggedIn && account.isNotBlank(),
            currentUser = if (loggedIn) account else "",
            avatarUri = avatarUri,
            lastAccount = account
        )
    }

    private fun normalizeStats(stats: UserStats): UserStats {
        if (stats.lastStudyDate.isBlank()) return stats

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val lastStudyDate = runCatching { LocalDate.parse(stats.lastStudyDate, DateTimeFormatter.ISO_DATE) }.getOrNull()
            ?: return stats.copy(studiedToday = 0)

        return when {
            lastStudyDate == today -> stats
            lastStudyDate == yesterday -> stats.copy(studiedToday = 0)
            lastStudyDate.isBefore(yesterday) -> stats.copy(studiedToday = 0, streakDays = 0)
            else -> stats.copy(studiedToday = 0)
        }
    }

    private fun save(state: LibraryState) {
        val progressJson = JSONObject()
        state.progress.forEach { (key, progress) ->
            progressJson.put(
                key,
                JSONObject().apply {
                    put("knownCount", progress.knownCount)
                    put("wrongCount", progress.wrongCount)
                    put("lastMode", progress.lastMode?.name.orEmpty())
                }
            )
        }
        val statsJson = JSONObject().apply {
            put("xp", state.stats.xp)
            put("streakDays", state.stats.streakDays)
            put("dailyGoal", state.stats.dailyGoal)
            put("studiedToday", state.stats.studiedToday)
            put("lastStudyDate", state.stats.lastStudyDate)
        }
        prefs.edit()
            .putString("progress_json", progressJson.toString())
            .putString("stats_json", statsJson.toString())
            .apply()
    }

    private fun loadSeedWords(context: Context): List<WordEntry> {
        val json = context.assets.open("words.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    WordEntry(
                        id = item.getString("id"),
                        word = item.getString("word"),
                        phonetic = item.getString("phonetic"),
                        meaning = item.getString("meaning"),
                        example = item.getString("example"),
                        level = item.getString("level"),
                        tags = buildList {
                            val tagsArray = item.getJSONArray("tags")
                            for (j in 0 until tagsArray.length()) {
                                add(tagsArray.getString(j))
                            }
                        }
                    )
                )
            }
        }
    }
}
