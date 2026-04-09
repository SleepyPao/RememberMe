package com.int4074.wordduo.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.int4074.wordduo.data.BattleRepository
import com.int4074.wordduo.data.BattleState
import com.int4074.wordduo.data.EssayReviewState
import com.int4074.wordduo.data.PracticeMode
import com.int4074.wordduo.data.SessionState
import com.int4074.wordduo.data.StudyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max

class AppViewModel(
    private val repository: StudyRepository,
    private val battleRepository: BattleRepository
) : ViewModel() {
    val library = repository.state
    val authState = repository.authState
    val battle: StateFlow<BattleState> = battleRepository.state

    private val _session = MutableStateFlow(SessionState())
    val session: StateFlow<SessionState> = _session

    private val _essayReview = MutableStateFlow(EssayReviewState())
    val essayReview: StateFlow<EssayReviewState> = _essayReview

    fun startSession(mode: PracticeMode) {
        _session.value = repository.buildSession(mode)
    }

    fun hostLanBattle() {
        battleRepository.hostLanRoom(authState.value.currentUser.ifBlank { "玩家 1" }, library.value.words)
    }

    fun joinLanBattle() {
        battleRepository.joinLanRoom(authState.value.currentUser.ifBlank { "玩家 2" }, library.value.words)
    }

    fun startAiBattle() {
        battleRepository.startAiBattle(authState.value.currentUser.ifBlank { "我" }, library.value.words)
    }

    fun updateBattleJoinCode(code: String) {
        battleRepository.updateJoinCode(code)
    }

    fun updateBattleInput(text: String) {
        battleRepository.updateAnswerInput(text)
    }

    fun submitBattleAnswer() {
        battleRepository.submitAnswer()
    }

    fun leaveBattle() {
        battleRepository.disconnect()
    }

    fun updateEssayText(text: String) {
        _essayReview.value = _essayReview.value.copy(
            inputText = text,
            statusMessage = null,
            result = if (text != _essayReview.value.inputText) null else _essayReview.value.result,
            isAnalyzing = false
        )
    }

    fun importEssayText(text: String, sourceLabel: String) {
        _essayReview.value = _essayReview.value.copy(
            inputText = text,
            imageSourceLabel = sourceLabel,
            statusMessage = if (text.isBlank()) "图片里没有识别到有效文字" else "已从图片导入 ${text.length} 个字符",
            result = null,
            isAnalyzing = false
        )
    }

    fun setEssayStatus(message: String?) {
        _essayReview.value = _essayReview.value.copy(statusMessage = message, isAnalyzing = false)
    }

    fun clearEssayReview() {
        _essayReview.value = EssayReviewState()
    }

    fun analyzeEssay() {
        val text = _essayReview.value.inputText.trim()
        if (text.isBlank()) {
            _essayReview.value = _essayReview.value.copy(statusMessage = "请先输入文章或导入图片文字")
            return
        }

        _essayReview.value = _essayReview.value.copy(
            statusMessage = "正在调用 AI 评分，请稍候…",
            result = null,
            isAnalyzing = true
        )

        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    Triple(AiEssayService.review(text), false, null as String?)
                }.getOrElse { error ->
                    Triple(EssayReviewEngine.review(text), true, error.message ?: error.javaClass.simpleName)
                }
            }

            val (result, usedFallback, errorMessage) = outcome
            _essayReview.value = _essayReview.value.copy(
                result = result,
                statusMessage = if (usedFallback) {
                    val detail = errorMessage?.replace("\n", " ")?.take(180).orEmpty()
                    if (detail.isBlank()) {
                        "AI 接口暂时不可用，已切换为本地评估，共发现 ${result.issues.size} 处可优化点"
                    } else {
                        "AI 接口暂时不可用：$detail。已切换为本地评估，共发现 ${result.issues.size} 处可优化点"
                    }
                } else {
                    "AI 评估已完成，共发现 ${result.issues.size} 处可优化点"
                },
                isAnalyzing = false
            )
        }
    }

    fun login(account: String, password: String) {
        repository.login(account, password)
    }

    fun register(account: String, password: String, confirmPassword: String) {
        repository.register(account, password, confirmPassword)
    }

    fun updateAvatar(uri: String) {
        repository.updateAvatarUri(uri)
    }

    fun logout() {
        repository.logout()
        battleRepository.disconnect()
        _session.value = SessionState()
        _essayReview.value = EssayReviewState()
    }

    fun clearAuthMessage() {
        repository.clearAuthMessage()
    }

    fun updateInput(text: String) {
        _session.value = _session.value.copy(input = text)
    }

    fun revealAnswer() {
        _session.value = _session.value.copy(revealAnswer = true)
    }

    fun setRecognition(text: String) {
        _session.value = _session.value.copy(lastRecognition = text)
    }

    fun markRecite(known: Boolean) {
        val current = _session.value.current ?: return
        val xpDelta = if (known) 10 else 3
        repository.submitResult(current.word.id, PracticeMode.Recite, known, xpDelta)
        advance(if (known) "已掌握" else "已计入错词趋势", xpDelta)
    }

    fun submitTypedAnswer() {
        val state = _session.value
        val current = state.current ?: return

        if (state.revealAnswer) {
            advance("进入下一题")
            return
        }

        val answer = normalize(state.input)
        val target = normalize(current.word.word)
        val ok = answer == target
        val xpDelta = if (ok) 12 else 4
        repository.submitResult(current.word.id, current.mode, ok, xpDelta)

        if (ok) {
            advance("回答正确", xpDelta)
        } else {
            _session.value = state.copy(
                revealAnswer = true,
                feedback = "正确答案：${current.word.word}",
                sessionXp = state.sessionXp + xpDelta
            )
        }
    }

    fun submitPronunciationResult() {
        val state = _session.value
        val current = state.current ?: return

        if (state.revealAnswer) {
            advance("进入下一题")
            return
        }

        val heard = normalize(state.lastRecognition)
        val target = normalize(current.word.word)
        val ok = heard == target || heard.contains(target) || similarity(heard, target) >= 0.72f
        val xpDelta = if (ok) 14 else 5
        repository.submitResult(current.word.id, PracticeMode.Pronunciation, ok, xpDelta)
        _session.value = state.copy(
            revealAnswer = true,
            feedback = if (ok) "发音通过" else "识别结果：${state.lastRecognition.ifBlank { "未识别" }}",
            sessionXp = state.sessionXp + xpDelta
        )
    }

    fun updateGoal(goal: Int) {
        repository.updateDailyGoal(goal)
    }

    private fun advance(feedback: String, xpDelta: Int = 0) {
        val state = _session.value
        val nextIndex = state.index + 1
        _session.value = state.copy(
            index = nextIndex,
            input = "",
            revealAnswer = false,
            feedback = feedback,
            sessionXp = state.sessionXp + xpDelta,
            completed = nextIndex >= state.queue.size,
            lastRecognition = ""
        )
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(
                    StudyRepository(context.applicationContext),
                    BattleRepository(context.applicationContext)
                ) as T
            }
        }

        private fun normalize(text: String): String {
            return text.trim().lowercase(Locale.US).replace("[^a-z ]".toRegex(), "")
        }

        private fun similarity(a: String, b: String): Float {
            if (a.isBlank() || b.isBlank()) return 0f
            val distance = levenshtein(a, b)
            return 1f - distance.toFloat() / max(a.length, b.length)
        }

        private fun levenshtein(a: String, b: String): Int {
            val dp = Array(a.length + 1) { IntArray(b.length + 1) }
            for (i in a.indices) dp[i + 1][0] = i + 1
            for (j in b.indices) dp[0][j + 1] = j + 1
            for (i in a.indices) {
                for (j in b.indices) {
                    val cost = if (a[i] == b[j]) 0 else 1
                    dp[i + 1][j + 1] = minOf(dp[i][j + 1] + 1, dp[i + 1][j] + 1, dp[i][j] + cost)
                }
            }
            return dp[a.length][b.length]
        }
    }
}


