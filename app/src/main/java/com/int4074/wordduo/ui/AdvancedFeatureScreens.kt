@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.int4074.wordduo.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.int4074.wordduo.data.BattleMode
import com.int4074.wordduo.data.BattlePhase
import com.int4074.wordduo.data.BattleState
import com.int4074.wordduo.data.EssayReviewState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

@Composable
fun FeatureLaunchCard(
    title: String,
    subtitle: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonLabel: String,
    onClick: () -> Unit
) {
    FloatingCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlossyIconBubble(icon = icon, accent = accent, selected = true, size = 64.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                Text(subtitle, color = Color(0xFF8F8378))
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
fun OnlineBattleScreen(
    battle: BattleState,
    onHostLan: () -> Unit,
    onJoinCodeChange: (String) -> Unit,
    onJoinLan: () -> Unit,
    onStartAi: () -> Unit,
    onInputChange: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onLeave: () -> Unit
) {
    val speechPlayer = rememberSpeechPlayer()
    var playbackStatus by rememberSaveable { mutableStateOf<String?>(null) }

    DuolingoBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                FilledTonalButton(onClick = onLeave, shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (battle.phase == BattlePhase.Idle) "返回首页" else "结束对战")
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("对战模式", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D241E))
                    Text("支持局域网双机对战，也支持立即开始的人机对战。", color = Color(0xFF8F8378))
                }
            }
            if (battle.phase == BattlePhase.Idle) {
                item {
                    FeatureLaunchCard(
                        title = "局域网建房",
                        subtitle = "房主创建房间，另一台手机输入 IP:端口 加入。",
                        accent = Color(0xFFFF9E7A),
                        icon = Icons.Default.Groups,
                        buttonLabel = "创建",
                        onClick = onHostLan
                    )
                }
                item {
                    FloatingCard {
                        Text("加入局域网房间", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2D241E))
                        Text(if (battle.localAddress.isBlank()) "请先确保两台手机连接同一个 Wi‑Fi。" else "本机当前 IP：${battle.localAddress}", color = Color(0xFF8F8378))
                        OutlinedTextField(
                            value = battle.joinCodeInput,
                            onValueChange = onJoinCodeChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("输入房主 IP:端口，例如 192.168.1.5:40740") },
                            shape = RoundedCornerShape(20.dp)
                        )
                        Button(
                            onClick = onJoinLan,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F86FF)),
                            shape = RoundedCornerShape(18.dp)
                        ) { Text("加入对战") }
                    }
                }
                item {
                    FeatureLaunchCard(
                        title = "人机对手",
                        subtitle = "立即开始 5 题人机拼写赛，AI 会按正确率和速度与你比拼。",
                        accent = Color(0xFF8F86FF),
                        icon = Icons.Default.AutoAwesome,
                        buttonLabel = "开始",
                        onClick = onStartAi
                    )
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        TinyStatChip(if (battle.mode == BattleMode.Ai) "人机对战" else "局域网对战", Color(0xFF8F86FF))
                        TinyStatChip("第 ${battle.roundIndex + 1}/${battle.totalRounds} 题", Color(0xFFFFA37C))
                    }
                }
                if (battle.mode == BattleMode.Lan) {
                    item {
                        FloatingCard {
                            Text("连接信息", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2D241E))
                            if (battle.isHost) {
                                Text("请让另一台手机加入：${battle.roomCode}", color = Color(0xFF8F8378))
                            } else {
                                Text("已连接房间：${battle.roomCode}", color = Color(0xFF8F8378))
                            }
                            battle.statusMessage?.let { Text(it, color = Color(0xFFFF8D72)) }
                        }
                    }
                }
                item {
                    FloatingCard {
                        Text("当前比分", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2D241E))
                        battle.players.forEach { player ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(player.name, fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                                Text(
                                    "正确 ${player.correctCount}  ·  ${String.format(Locale.US, "%.1f", player.totalDurationMs / 1000f)} 秒",
                                    color = Color(0xFF8F8378)
                                )
                            }
                        }
                        battle.feedback?.let { Text(it, color = Color(0xFF8F8378)) }
                    }
                }
                if ((battle.phase == BattlePhase.Playing || battle.phase == BattlePhase.RoundReview) && battle.currentWord != null) {
                    item {
                        FloatingCard {
                            Text("释义", color = Color(0xFF8F8378), fontWeight = FontWeight.SemiBold)
                            Text(battle.currentWord.meaning, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D241E))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                battle.currentWord.tags.take(2).forEach { tag -> TinyStatChip(tag, Color(0xFF9A8CFF)) }
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                val started = speechPlayer.speak(battle.currentWord.word)
                                playbackStatus = if (started) null else "当前设备未提供可用的英文语音引擎"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F86FF)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("播放发音")
                        }
                    }
                    playbackStatus?.let { status ->
                        item { Text(status, color = Color(0xFFFF8D72)) }
                    }
                    item {
                        OutlinedTextField(
                            value = battle.answerInput,
                            onValueChange = onInputChange,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = battle.mode == BattleMode.Ai || !battle.submitted,
                            label = {
                                Text(
                                    when {
                                        battle.mode == BattleMode.Ai && battle.phase == BattlePhase.RoundReview -> "点击下一题继续"
                                        battle.submitted -> "已提交，等待对手"
                                        else -> "输入你的拼写"
                                    }
                                )
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                    item {
                        Button(
                            onClick = onSubmitAnswer,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = battle.mode == BattleMode.Ai || !battle.submitted,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8D72)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                when {
                                    battle.mode == BattleMode.Ai && battle.phase == BattlePhase.RoundReview -> "下一题"
                                    battle.submitted -> "已提交"
                                    else -> "提交本题"
                                }
                            )
                        }
                    }
                }
                if (battle.phase == BattlePhase.Finished) {
                    item {
                        FloatingCard {
                            Text("本局结束", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D241E))
                            Text(battle.winnerLabel.ifBlank { "本局已结束" }, color = Color(0xFF8F86FF), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(if (battle.mode == BattleMode.Ai) "可以再开一局继续挑战 AI。" else "可以退出后重新建房，再来一局。", color = Color(0xFF8F8378))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EssayReviewScreen(
    essayState: EssayReviewState,
    onTextChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onClear: () -> Unit,
    onImportText: (String, String) -> Unit,
    onStatus: (String?) -> Unit,
    onBackHome: () -> Unit
) {
    val context = LocalContext.current
    val result = essayState.result
    var issueFilter by rememberSaveable(result?.issues?.hashCode()) { mutableStateOf("全部") }
    val filteredIssues = remember(result?.issues, issueFilter) {
        result?.issues?.filter { issueMatchesFilter(it.category, issueFilter) }.orEmpty()
    }
    var ocrBusy by rememberSaveable { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        ocrBusy = true
        onStatus("正在识别图片中的文字…")
        recognizeEssayText(
            context = context,
            uri = uri,
            onSuccess = { text ->
                ocrBusy = false
                onImportText(text, uri.lastPathSegment ?: "图片导入")
            },
            onError = { message ->
                ocrBusy = false
                onStatus(message)
            }
        )
    }

    DuolingoBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                FilledTonalButton(onClick = onBackHome, shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("返回首页")
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("AI 写作检测", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D241E))
                    Text("支持直接粘贴文章，也可以先导入照片识别文字，再做评分与纠错。", color = Color(0xFF8F8378))
                }
            }
            item {
                FloatingCard {
                    Text("输入文章", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2D241E))
                    OutlinedTextField(
                        value = essayState.inputText,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 8,
                        label = { Text("粘贴英文作文，或先导入照片") },
                        shape = RoundedCornerShape(20.dp)
                    )
                    if (essayState.imageSourceLabel.isNotBlank()) {
                        Text("最近导入：${essayState.imageSourceLabel}", color = Color(0xFF8F8378))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            onClick = { pickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("导入照片")
                        }
                        Button(
                            onClick = onAnalyze,
                            modifier = Modifier.weight(1f),
                            enabled = !essayState.isAnalyzing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F86FF)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            if (essayState.isAnalyzing) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("分析中")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("开始评估")
                            }
                        }
                    }
                    FilledTonalButton(
                        onClick = onClear,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("清空内容")
                    }
                }
            }
            if (ocrBusy || essayState.isAnalyzing) {
                item {
                    FloatingCard {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(24.dp), color = Color(0xFF8F86FF))
                            Text(
                                if (essayState.isAnalyzing) "正在调用 AI 评分并生成批改结果…" else "正在从图片提取文字，请稍候…",
                                color = Color(0xFF8F8378)
                            )
                        }
                    }
                }
            }
            essayState.statusMessage?.let { message ->
                item {
                    FloatingCard {
                        Text("状态", fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                        Text(message, color = Color(0xFF8F8378))
                    }
                }
            }
            result?.let { review ->
                item {
                    FloatingCard {
                        Text("综合评分", fontWeight = FontWeight.Bold, color = Color(0xFF2D241E), fontSize = 20.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${review.score} / 100", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color(0xFF8F86FF))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${review.wordCount} words", color = Color(0xFF8F8378))
                                Text("${review.sentenceCount} sentences", color = Color(0xFF8F8378))
                            }
                        }
                        Text(review.summary, color = Color(0xFF675C55))
                    }
                }
                item {
                    FloatingCard {
                        Text("分类筛选", fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("全部", "语法", "用词", "搭配").forEach { filter ->
                                val selected = issueFilter == filter
                                Button(
                                    onClick = { issueFilter = filter },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) Color(0xFF8F86FF) else Color(0xFFF1E8DA),
                                        contentColor = if (selected) Color.White else Color(0xFF6F635A)
                                    )
                                ) {
                                    Text(filter)
                                }
                            }
                        }
                        Text("当前显示 ${filteredIssues.size} 条问题", color = Color(0xFF8F8378))
                    }
                }
                if (essayState.inputText.isNotBlank()) {
                    item {
                        Text("原文高亮", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                    }
                    item {
                        FloatingCard {
                            HighlightedEssayText(
                                sourceText = essayState.inputText,
                                excerpts = filteredIssues.map { it.excerpt }
                            )
                        }
                    }
                }
                item {
                    Text("问题定位", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                }
                if (filteredIssues.isEmpty()) {
                    item {
                        FloatingCard {
                            Text(
                                if (review.issues.isEmpty()) "这篇文章没有检测到明显的基础错误。" else "当前分类下没有问题，切换其他分类看看。",
                                color = Color(0xFF675C55)
                            )
                        }
                    }
                } else {
                    filteredIssues.forEach { issue ->
                        item {
                            FloatingCard {
                                Text(issue.category, color = Color(0xFF8F86FF), fontWeight = FontWeight.Bold)
                                Text("原句片段：${issue.excerpt}", color = Color(0xFF2D241E), fontWeight = FontWeight.SemiBold)
                                Text(issue.diagnosis, color = Color(0xFF675C55))
                                Text("建议：${issue.suggestion}", color = Color(0xFFFF8D72), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                item {
                    Text("润色建议", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                }
                item {
                    FloatingCard {
                        Text(review.improvedText, color = Color(0xFF2D241E), lineHeight = 24.sp)
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

private fun issueMatchesFilter(category: String, filter: String): Boolean {
    if (filter == "全部") return true
    val bucket = when {
        category.contains("搭配") -> "搭配"
        category.contains("用词") || category.contains("拼写") || category.contains("表达") -> "用词"
        category.contains("语法") || category.contains("主谓") || category.contains("句式") || category.contains("标点") || category.contains("大小写") || category.contains("可数性") -> "语法"
        else -> "其他"
    }
    return bucket == filter
}

@Composable
private fun HighlightedEssayText(sourceText: String, excerpts: List<String>) {
    val annotated = remember(sourceText, excerpts) {
        buildAnnotatedString {
            append(sourceText)
            excerpts
                .filter { it.isNotBlank() && it != "全文长度" && it != "结尾标点" }
                .distinct()
                .forEach { excerpt ->
                    val pattern = Regex(Regex.escape(excerpt), RegexOption.IGNORE_CASE)
                    pattern.findAll(sourceText).forEach { match ->
                        addStyle(
                            SpanStyle(
                                background = Color(0x33FFB38F),
                                color = Color(0xFF2D241E)
                            ),
                            match.range.first,
                            match.range.last + 1
                        )
                    }
                }
        }
    }
    Text(annotated, color = Color(0xFF2D241E), lineHeight = 24.sp)
}

private fun recognizeEssayText(
    context: Context,
    uri: Uri,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    runCatching { InputImage.fromFilePath(context, uri) }
        .onFailure { onError("无法读取这张图片，请重试或换一张更清晰的照片") }
        .onSuccess { image ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    recognizer.close()
                    if (result.text.isBlank()) {
                        onError("图片里没有识别到有效英文文字")
                    } else {
                        onSuccess(result.text)
                    }
                }
                .addOnFailureListener {
                    recognizer.close()
                    onError("图片文字识别失败，请尽量使用正向、清晰、光线稳定的照片")
                }
        }
}





