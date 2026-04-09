package com.int4074.wordduo.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.random.Random

class BattleRepository(context: Context) {
    private val prefs = context.getSharedPreferences("battle_prefs", Context.MODE_PRIVATE)
    private val playerId = prefs.getString("battle_player_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("battle_player_id", it).apply()
    }

    private val executor = Executors.newCachedThreadPool()
    private val random = Random(System.currentTimeMillis())
    private val port = 40740

    private var wordsById: Map<String, WordEntry> = emptyMap()
    private var selectedWordIds: List<String> = emptyList()
    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var hostSubmission: Submission? = null
    private var guestSubmission: Submission? = null
    private var localName: String = "玩家 1"
    private var remoteName: String = "玩家 2"

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<BattleState> = _state.asStateFlow()

    fun updateJoinCode(code: String) {
        _state.value = _state.value.copy(joinCodeInput = code.trim())
    }

    fun updateAnswerInput(text: String) {
        _state.value = _state.value.copy(answerInput = text)
    }

    fun hostLanRoom(playerName: String, words: List<WordEntry>, size: Int = 5) {
        disconnect()
        if (words.isEmpty()) {
            _state.value = initialState().copy(statusMessage = "当前词库为空，无法开始对战")
            return
        }
        localName = playerName.ifBlank { "玩家 1" }
        remoteName = "等待加入"
        wordsById = words.associateBy { it.id }
        selectedWordIds = words.shuffled().take(size).map { it.id }
        val ip = findLocalIpAddress().ifBlank { "未获取到局域网 IP" }
        _state.value = BattleState(
            mode = BattleMode.Lan,
            phase = BattlePhase.Waiting,
            roomCode = if (ip.contains(':')) ip else "$ip:$port",
            localAddress = ip,
            localPlayerId = playerId,
            players = listOf(BattlePlayerState(playerId, localName)),
            totalRounds = selectedWordIds.size,
            isHost = true,
            statusMessage = if (ip == "未获取到局域网 IP") "请连接同一 Wi‑Fi 后再建房。" else "房间已创建，请让对方输入 $ip:$port 加入",
            turnStartedAtMs = System.currentTimeMillis()
        )
        executor.execute {
            runCatching {
                serverSocket = ServerSocket(port)
                val accepted = serverSocket?.accept() ?: return@runCatching
                setupSocket(accepted)
            }.onFailure {
                postStatus("创建局域网房间失败：${it.message ?: "未知错误"}")
            }
        }
    }

    fun joinLanRoom(playerName: String, words: List<WordEntry>) {
        disconnect()
        val target = _state.value.joinCodeInput.ifBlank { _state.value.joinCodeInput }
        if (target.isBlank()) {
            _state.value = initialState().copy(joinCodeInput = _state.value.joinCodeInput, statusMessage = "请输入房主提供的 IP:端口")
            return
        }
        val host = target.substringBefore(':').trim()
        val targetPort = target.substringAfter(':', port.toString()).toIntOrNull() ?: port
        localName = playerName.ifBlank { "玩家 2" }
        wordsById = words.associateBy { it.id }
        _state.value = BattleState(
            mode = BattleMode.Lan,
            phase = BattlePhase.Waiting,
            joinCodeInput = target,
            roomCode = target,
            localPlayerId = playerId,
            players = listOf(BattlePlayerState(playerId, localName)),
            isHost = false,
            statusMessage = "正在连接房主…",
            turnStartedAtMs = System.currentTimeMillis()
        )
        executor.execute {
            runCatching {
                val connected = Socket(host, targetPort)
                setupSocket(connected)
                sendJson(JSONObject().apply {
                    put("type", "hello")
                    put("name", localName)
                })
            }.onFailure {
                postStatus("连接失败：${it.message ?: "请确认两台手机在同一局域网"}")
            }
        }
    }

    fun startAiBattle(playerName: String, words: List<WordEntry>, size: Int = 5) {
        disconnect()
        if (words.isEmpty()) {
            _state.value = initialState().copy(statusMessage = "当前词库为空，无法开始人机对战")
            return
        }
        localName = playerName.ifBlank { "我" }
        remoteName = "词光 AI"
        wordsById = words.associateBy { it.id }
        selectedWordIds = words.shuffled().take(size).map { it.id }
        _state.value = BattleState(
            mode = BattleMode.Ai,
            phase = BattlePhase.Playing,
            localPlayerId = playerId,
            players = listOf(
                BattlePlayerState(playerId, localName),
                BattlePlayerState("ai", remoteName)
            ),
            totalRounds = selectedWordIds.size,
            currentWord = wordsById[selectedWordIds.firstOrNull()],
            feedback = "人机对战已开始",
            turnStartedAtMs = System.currentTimeMillis()
        )
    }

    fun submitAnswer() {
        val state = _state.value
        val word = state.currentWord ?: return
        when (state.mode) {
            BattleMode.Ai -> submitAiAnswer(state, word)
            BattleMode.Lan -> submitLanAnswer(state, word)
        }
    }

    fun disconnect() {
        runCatching { reader?.close() }
        runCatching { writer?.close() }
        runCatching { socket?.close() }
        runCatching { serverSocket?.close() }
        reader = null
        writer = null
        socket = null
        serverSocket = null
        hostSubmission = null
        guestSubmission = null
        selectedWordIds = emptyList()
        _state.value = initialState().copy(joinCodeInput = _state.value.joinCodeInput)
    }

    private fun submitAiAnswer(state: BattleState, word: WordEntry) {
        if (state.phase == BattlePhase.RoundReview) {
            advanceAiRound(state)
            return
        }
        if (state.phase != BattlePhase.Playing) return

        val elapsed = elapsedSinceTurn(state)
        val userCorrect = normalize(state.answerInput) == normalize(word.word)
        val botElapsed = random.nextLong(1400L, 4800L)
        val botCorrect = random.nextFloat() > 0.32f
        val botAnswer = if (botCorrect) word.word else mutateWord(word.word)
        val updatedPlayers = state.players.map { player ->
            when (player.id) {
                playerId -> player.copy(
                    correctCount = player.correctCount + if (userCorrect) 1 else 0,
                    totalDurationMs = player.totalDurationMs + elapsed
                )
                "ai" -> player.copy(
                    correctCount = player.correctCount + if (botCorrect) 1 else 0,
                    totalDurationMs = player.totalDurationMs + botElapsed
                )
                else -> player
            }
        }
        val isLastRound = state.roundIndex + 1 >= state.totalRounds
        _state.value = state.copy(
            players = updatedPlayers,
            phase = if (isLastRound) BattlePhase.Finished else BattlePhase.RoundReview,
            submitted = true,
            feedback = "你${if (userCorrect) "答对" else "答错"}，AI 回答：$botAnswer，用时 ${String.format(Locale.US, "%.1f", botElapsed / 1000f)} 秒",
            winnerLabel = if (isLastRound) winnerLabel(updatedPlayers) else "",
            turnStartedAtMs = System.currentTimeMillis()
        )
    }

    private fun advanceAiRound(state: BattleState) {
        val nextRound = state.roundIndex + 1
        if (nextRound >= selectedWordIds.size) {
            _state.value = state.copy(
                phase = BattlePhase.Finished,
                winnerLabel = winnerLabel(state.players),
                submitted = true
            )
            return
        }
        _state.value = state.copy(
            phase = BattlePhase.Playing,
            roundIndex = nextRound,
            currentWord = wordsById[selectedWordIds[nextRound]],
            answerInput = "",
            submitted = false,
            feedback = "第 ${nextRound + 1} 题开始",
            turnStartedAtMs = System.currentTimeMillis()
        )
    }

    private fun submitLanAnswer(state: BattleState, word: WordEntry) {
        if (state.phase != BattlePhase.Playing || state.submitted) return
        val submission = Submission(answer = state.answerInput.trim(), elapsedMs = elapsedSinceTurn(state))
        if (state.isHost) {
            hostSubmission = submission
            _state.value = state.copy(submitted = true, feedback = "已提交，等待对手…")
            if (guestSubmission != null) {
                resolveHostRound(word)
            }
        } else {
            sendJson(JSONObject().apply {
                put("type", "submit")
                put("answer", submission.answer)
                put("elapsedMs", submission.elapsedMs)
            })
            _state.value = state.copy(submitted = true, feedback = "已提交，等待房主结算…")
        }
    }

    private fun setupSocket(connected: Socket) {
        socket = connected
        reader = BufferedReader(InputStreamReader(connected.getInputStream()))
        writer = BufferedWriter(OutputStreamWriter(connected.getOutputStream()))
        executor.execute {
            runCatching {
                while (true) {
                    val line = reader?.readLine() ?: break
                    handleMessage(JSONObject(line))
                }
            }.onFailure {
                postStatus("对战连接已断开：${it.message ?: "网络中断"}")
            }
        }
    }

    private fun handleMessage(json: JSONObject) {
        when (json.optString("type")) {
            "hello" -> handleHello(json)
            "start" -> handleStart(json)
            "submit" -> handleGuestSubmit(json)
            "result" -> handleRoundResult(json)
        }
    }

    private fun handleHello(json: JSONObject) {
        if (!_state.value.isHost) return
        remoteName = json.optString("name").ifBlank { "玩家 2" }
        sendJson(JSONObject().apply {
            put("type", "start")
            put("hostName", localName)
            put("guestName", remoteName)
            put("wordIds", JSONArray(selectedWordIds))
        })
        _state.value = _state.value.copy(
            phase = BattlePhase.Playing,
            players = listOf(
                BattlePlayerState(playerId, localName),
                BattlePlayerState("guest", remoteName)
            ),
            totalRounds = selectedWordIds.size,
            currentWord = wordsById[selectedWordIds.firstOrNull()],
            submitted = false,
            feedback = "双方已连接，第 1 题开始",
            turnStartedAtMs = System.currentTimeMillis()
        )
    }

    private fun handleStart(json: JSONObject) {
        if (_state.value.isHost) return
        localName = json.optString("guestName").ifBlank { localName }
        remoteName = json.optString("hostName").ifBlank { "房主" }
        selectedWordIds = buildList {
            val array = json.optJSONArray("wordIds") ?: JSONArray()
            for (i in 0 until array.length()) add(array.optString(i))
        }
        _state.value = _state.value.copy(
            phase = BattlePhase.Playing,
            players = listOf(
                BattlePlayerState("host", remoteName),
                BattlePlayerState(playerId, localName)
            ),
            totalRounds = selectedWordIds.size,
            currentWord = wordsById[selectedWordIds.firstOrNull()],
            submitted = false,
            feedback = "房主已发题，第 1 题开始",
            turnStartedAtMs = System.currentTimeMillis()
        )
    }

    private fun handleGuestSubmit(json: JSONObject) {
        if (!_state.value.isHost) return
        guestSubmission = Submission(
            answer = json.optString("answer"),
            elapsedMs = json.optLong("elapsedMs", 0L)
        )
        val word = _state.value.currentWord ?: return
        if (hostSubmission != null) {
            resolveHostRound(word)
        } else {
            _state.value = _state.value.copy(feedback = "对手已提交，等待你作答")
        }
    }

    private fun resolveHostRound(word: WordEntry) {
        val state = _state.value
        val host = hostSubmission ?: return
        val guest = guestSubmission ?: return
        val hostCorrect = normalize(host.answer) == normalize(word.word)
        val guestCorrect = normalize(guest.answer) == normalize(word.word)
        val currentPlayers = if (state.players.size >= 2) state.players else listOf(
            BattlePlayerState(playerId, localName),
            BattlePlayerState("guest", remoteName)
        )
        val updatedPlayers = currentPlayers.map { player ->
            when (player.id) {
                playerId -> player.copy(
                    correctCount = player.correctCount + if (hostCorrect) 1 else 0,
                    totalDurationMs = player.totalDurationMs + host.elapsedMs
                )
                "guest" -> player.copy(
                    correctCount = player.correctCount + if (guestCorrect) 1 else 0,
                    totalDurationMs = player.totalDurationMs + guest.elapsedMs
                )
                else -> player
            }
        }
        val nextRound = state.roundIndex + 1
        val finished = nextRound >= selectedWordIds.size
        val payload = JSONObject().apply {
            put("type", "result")
            put("roundIndex", if (finished) state.roundIndex else nextRound)
            put("hostCorrectCount", updatedPlayers[0].correctCount)
            put("guestCorrectCount", updatedPlayers[1].correctCount)
            put("hostDurationMs", updatedPlayers[0].totalDurationMs)
            put("guestDurationMs", updatedPlayers[1].totalDurationMs)
            put("status", if (finished) "finished" else "playing")
            put("message", "本题结算：房主${if (hostCorrect) "正确" else "错误"}，客方${if (guestCorrect) "正确" else "错误"}")
            put("winnerLabel", if (finished) winnerLabel(updatedPlayers) else "")
            put("currentWordId", if (finished) "" else selectedWordIds[nextRound])
            put("hostName", updatedPlayers[0].name)
            put("guestName", updatedPlayers[1].name)
        }
        sendJson(payload)
        hostSubmission = null
        guestSubmission = null
        _state.value = if (finished) {
            state.copy(
                players = updatedPlayers,
                phase = BattlePhase.Finished,
                submitted = true,
                feedback = payload.optString("message"),
                winnerLabel = payload.optString("winnerLabel")
            )
        } else {
            state.copy(
                players = updatedPlayers,
                phase = BattlePhase.Playing,
                roundIndex = nextRound,
                currentWord = wordsById[selectedWordIds[nextRound]],
                answerInput = "",
                submitted = false,
                feedback = payload.optString("message"),
                turnStartedAtMs = System.currentTimeMillis()
            )
        }
    }

    private fun handleRoundResult(json: JSONObject) {
        if (_state.value.isHost) return
        val currentPlayers = if (_state.value.players.size >= 2) _state.value.players else listOf(
            BattlePlayerState("host", json.optString("hostName", remoteName)),
            BattlePlayerState(playerId, json.optString("guestName", localName))
        )
        val updatedPlayers = listOf(
            currentPlayers[0].copy(
                name = json.optString("hostName", currentPlayers[0].name),
                correctCount = json.optInt("hostCorrectCount"),
                totalDurationMs = json.optLong("hostDurationMs")
            ),
            currentPlayers[1].copy(
                name = json.optString("guestName", currentPlayers[1].name),
                correctCount = json.optInt("guestCorrectCount"),
                totalDurationMs = json.optLong("guestDurationMs")
            )
        )
        val status = json.optString("status")
        val nextWordId = json.optString("currentWordId")
        _state.value = _state.value.copy(
            players = updatedPlayers,
            phase = if (status == "finished") BattlePhase.Finished else BattlePhase.Playing,
            roundIndex = json.optInt("roundIndex"),
            currentWord = if (status == "finished") null else wordsById[nextWordId],
            answerInput = "",
            submitted = false,
            feedback = json.optString("message"),
            winnerLabel = json.optString("winnerLabel"),
            turnStartedAtMs = System.currentTimeMillis()
        )
    }

    private fun sendJson(json: JSONObject) {
        executor.execute {
            runCatching {
                val out = writer ?: return@runCatching
                synchronized(out) {
                    out.write(json.toString())
                    out.newLine()
                    out.flush()
                }
            }.onFailure {
                postStatus("发送对战数据失败：${it.message ?: "网络异常"}")
            }
        }
    }

    private fun postStatus(message: String) {
        _state.value = _state.value.copy(statusMessage = message)
    }

    private fun elapsedSinceTurn(state: BattleState): Long {
        return (System.currentTimeMillis() - state.turnStartedAtMs).coerceAtLeast(500L)
    }

    private fun winnerLabel(players: List<BattlePlayerState>): String {
        if (players.size < 2) return ""
        val first = players[0]
        val second = players[1]
        return when {
            first.correctCount > second.correctCount -> "${first.name} 获胜"
            second.correctCount > first.correctCount -> "${second.name} 获胜"
            first.totalDurationMs < second.totalDurationMs -> "${first.name} 获胜"
            second.totalDurationMs < first.totalDurationMs -> "${second.name} 获胜"
            else -> "平局"
        }
    }

    private fun mutateWord(word: String): String {
        if (word.length <= 3) return word.reversed()
        val dropIndex = random.nextInt(1, word.length - 1)
        return word.removeRange(dropIndex, dropIndex + 1)
    }

    private fun initialState(): BattleState {
        return BattleState(
            phase = BattlePhase.Idle,
            localPlayerId = playerId,
            localAddress = findLocalIpAddress()
        )
    }

    private fun findLocalIpAddress(): String {
        return runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { iface -> iface.inetAddresses.toList() }
                .firstOrNull { address -> !address.isLoopbackAddress && address is Inet4Address }
                ?.hostAddress.orEmpty()
        }.getOrElse {
            ""
        }
    }

    private fun normalize(text: String): String = text.trim().lowercase(Locale.US).replace("[^a-z ]".toRegex(), "")

    private data class Submission(val answer: String, val elapsedMs: Long)
}

private fun <T> java.util.Enumeration<T>.toList(): List<T> {
    val result = mutableListOf<T>()
    while (hasMoreElements()) result += nextElement()
    return result
}
