package com.int4074.wordduo.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.int4074.wordduo.data.LibraryState
import com.int4074.wordduo.data.PracticeMode
import com.int4074.wordduo.data.SessionState

@Composable
fun HomeScreen(library: LibraryState, onStartMode: (PracticeMode) -> Unit) {
    DuolingoBackdrop {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding().displayCutoutPadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("词光", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D241E))
                    Text("把背词做成一件轻盈、舒服、可持续的事。", color = Color(0xFF92857C), fontSize = 15.sp)
                }
            }
            item {
                FloatingCard {
                    Text("今日状态", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricOrb("XP", library.stats.xp.toString(), Icons.Default.Bolt, Color(0xFFFFC25F), Modifier.weight(1f))
                        MetricOrb("连胜", "${library.stats.streakDays}天", Icons.Default.Star, Color(0xFFFFB6A1), Modifier.weight(1f))
                        MetricOrb("目标", "${library.stats.studiedToday}/${library.stats.dailyGoal}", Icons.Default.CheckCircle, Color(0xFF9AD9B0), Modifier.weight(1f))
                    }
                    GlossyProgressBar(
                        progress = library.stats.studiedToday / library.stats.dailyGoal.toFloat().coerceAtLeast(1f),
                        color = Color(0xFF9A8CFF)
                    )
                    Text(
                        if (library.stats.studiedToday >= library.stats.dailyGoal) "今日目标已经达成，继续刷一组也很轻松。" else "还有一点点就能完成今日进度。",
                        color = Color(0xFF8F8378)
                    )
                }
            }
            item {
                Text("练习入口", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
            }
            items(PracticeMode.entries.toList()) { mode ->
                ModeCard(mode = mode) { onStartMode(mode) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PracticeScreen(
    library: LibraryState,
    session: SessionState,
    onModeSelected: (PracticeMode) -> Unit,
    onReciteMark: (Boolean) -> Unit,
    onTypedInput: (String) -> Unit,
    onSubmitTyped: () -> Unit,
    onReveal: () -> Unit,
    onRecognition: (String) -> Unit,
    onSubmitPronunciation: () -> Unit,
    onNextBatch: () -> Unit,
    onBackHome: () -> Unit
) {
    DuolingoBackdrop {
        if (session.queue.isEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Text("开始训练", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D241E)) }
                item { Text("当前词库 ${library.words.size} 词，错词 ${library.weakWords.size} 个。", color = Color(0xFF8F8378)) }
                items(PracticeMode.entries.toList()) { mode -> ModeCard(mode = mode) { onModeSelected(mode) } }
            }
            return@DuolingoBackdrop
        }
        if (session.completed || session.current == null) {
            CompletionPane(session = session, onNextBatch = onNextBatch, onBackHome = onBackHome)
            return@DuolingoBackdrop
        }

        val question = session.current!!
        val questionScroll = rememberScrollState()
        val primaryTag = question.word.tags.firstOrNull { it != "听写" }
        val displayExample = exampleTextFor(question.word)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding().displayCutoutPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            FilledTonalButton(onClick = onBackHome, shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("返回首页")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = { ((session.index + 1).toFloat() / session.queue.size.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(14.dp),
                    color = Color(0xFF9A8CFF),
                    trackColor = Color(0xFFF0E5D9)
                )
                Text("${session.index + 1}/${session.queue.size}", fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(questionScroll),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF8)),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(question.mode.title, color = Color(0xFF8F8378), fontWeight = FontWeight.SemiBold)
                        if (primaryTag != null) {
                            Text("词性：$primaryTag", color = Color(0xFF9A8D83), fontSize = 14.sp)
                        }
                        when (question.mode) {
                            PracticeMode.Meaning -> {
                                Text("请根据中文写出英文", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D241E))
                                Text(question.word.meaning, color = Color(0xFF2D241E), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            PracticeMode.Spelling -> {
                                Text("听音后完成拼写", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D241E))
                                Text(question.word.meaning, color = Color(0xFF2D241E), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            else -> {
                                Text(question.word.word, fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D241E))
                                if (question.word.phonetic.isNotBlank()) {
                                    Text(question.word.phonetic, color = Color(0xFF3399C5), fontSize = 18.sp)
                                }
                                Text(question.word.meaning, color = Color(0xFF2D241E), fontSize = 18.sp)
                            }
                        }
                        if (session.revealAnswer) {
                            Text("答案：${question.word.word}", color = Color(0xFFFFA399), fontWeight = FontWeight.Bold)
                        }
                        if (session.revealAnswer && displayExample.isNotBlank()) {
                            Text("例句：$displayExample", color = Color(0xFF8F8378))
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            question.word.tags.forEach { tag ->
                                AssistChip(onClick = {}, label = { Text(tag) })
                            }
                        }
                    }
                }
            }
            when (question.mode) {
                PracticeMode.Recite -> ReciteActions(onReciteMark, onReveal, displayExample.isNotBlank())
                PracticeMode.Pronunciation -> PronunciationActions(
                    targetWord = question.word.word,
                    onRecognized = onRecognition,
                    onSubmit = onSubmitPronunciation,
                    lastRecognition = session.lastRecognition,
                    revealAnswer = session.revealAnswer
                )
                else -> TypedAnswerPane(
                    mode = question.mode,
                    input = session.input,
                    answer = question.word.word,
                    revealAnswer = session.revealAnswer,
                    onInput = onTypedInput,
                    onSubmit = onSubmitTyped,
                    onReveal = onReveal
                )
            }
        }
    }
}

@Composable
private fun ReciteActions(onReciteMark: (Boolean) -> Unit, onReveal: () -> Unit, hasExample: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(onClick = onReveal, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(if (hasExample) "展开例句与提示" else "展开提示")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onReciteMark(false) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA399)),
                shape = RoundedCornerShape(18.dp)
            ) { Text("没记住") }
            Button(
                onClick = { onReciteMark(true) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A8CFF)),
                shape = RoundedCornerShape(18.dp)
            ) { Text("记住了") }
        }
    }
}

@Composable
private fun TypedAnswerPane(
    mode: PracticeMode,
    input: String,
    answer: String,
    revealAnswer: Boolean,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit,
    onReveal: () -> Unit
) {
    val speechPlayer = rememberSpeechPlayer()
    var playbackStatus by rememberSaveable { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (mode == PracticeMode.Spelling || mode == PracticeMode.MistakeReview) {
            FilledTonalButton(
                onClick = {
                    val started = speechPlayer.speak(answer)
                    playbackStatus = if (started) null else "当前设备未提供可用的英文语音引擎"
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("播放单词")
            }
            playbackStatus?.let {
                Text(it, color = Color(0xFFFF8D72))
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = onInput,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    when (mode) {
                        PracticeMode.Meaning -> "输入英文单词"
                        PracticeMode.Spelling -> "输入你听到的拼写"
                        PracticeMode.MistakeReview -> "纠正这个错词"
                        else -> "输入答案"
                    }
                )
            },
            shape = RoundedCornerShape(18.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onReveal, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) {
                Text(if (revealAnswer) "已显示答案" else "看答案")
            }
            Button(
                onClick = onSubmit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A8CFF)),
                shape = RoundedCornerShape(18.dp)
            ) { Text(if (revealAnswer) "下一题" else "提交") }
        }
    }
}

@Composable
private fun PronunciationActions(
    targetWord: String,
    onRecognized: (String) -> Unit,
    onSubmit: () -> Unit,
    lastRecognition: String,
    revealAnswer: Boolean
) {
    val context = LocalContext.current
    val preview = LocalInspectionMode.current
    val recognitionAvailable = !preview && isSpeechRecognitionAvailable(context)
    var listening by remember { mutableStateOf(false) }
    var speechStatus by rememberSaveable { mutableStateOf<String?>(null) }
    val speechPlayer = rememberSpeechPlayer()
    val recognizer = rememberSpeechRecognizer(
        enabled = recognitionAvailable,
        onRecognized = {
            listening = false
            if (it.isNotBlank()) {
                speechStatus = null
                onRecognized(it)
            } else {
                speechStatus = "未识别到语音，请再试一次"
            }
        },
        onListeningChanged = { listening = it },
        onErrorMessage = {
            listening = false
            speechStatus = it
        }
    )
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val started = recognizer.start()
            speechStatus = if (started) null else "当前设备未提供语音识别服务"
        } else {
            speechStatus = "需要麦克风权限才能跟读"
        }
    }

    LaunchedEffect(recognitionAvailable) {
        if (!recognitionAvailable) {
            speechStatus = "当前设备未提供语音识别服务，建议使用真机或安装系统语音服务"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val started = speechPlayer.speak(targetWord)
                    speechStatus = if (started) null else "当前设备未提供可用的英文语音引擎"
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("播放标准音")
            }
            FilledTonalButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        val started = recognizer.start()
                        speechStatus = if (started) null else "当前设备未提供语音识别服务"
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                enabled = recognitionAvailable
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (listening) "识别中" else if (recognitionAvailable) "开始跟读" else "设备不支持")
            }
        }
        if (lastRecognition.isNotBlank()) {
            Text("识别结果：$lastRecognition", color = Color(0xFF8F8378))
        }
        speechStatus?.let {
            FloatingCard(modifier = Modifier.fillMaxWidth()) {
                Text("语音提示", color = Color(0xFF2D241E), fontWeight = FontWeight.Bold)
                Text(it, color = Color(0xFFFF8D72))
                Text("请使用正常语速读出英文单词；部分国产系统需要先启用系统语音识别服务。", color = Color(0xFF8F8378))
            }
        }
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A8CFF)),
            shape = RoundedCornerShape(18.dp)
        ) { Text(if (revealAnswer) "下一题" else "检查发音") }
    }
}

@Composable
fun CompletionPane(session: SessionState, onNextBatch: () -> Unit, onBackHome: () -> Unit) {
    DuolingoBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingCard(modifier = Modifier.fillMaxWidth()) {
                Text("本轮完成", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D241E))
                Text("${session.mode.title} 获得 ${session.sessionXp} XP", color = Color(0xFF8F8378))
                Text("可以直接开始下一组，也可以回到首页切换训练模式。", textAlign = TextAlign.Center, color = Color(0xFF8F8378))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = onBackHome,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text("返回首页") }
                    Button(
                        onClick = onNextBatch,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A8CFF)),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text("下一组训练") }
                }
            }
        }
    }
}






