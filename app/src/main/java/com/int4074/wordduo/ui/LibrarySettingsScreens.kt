package com.int4074.wordduo.ui

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.int4074.wordduo.data.LibraryState
import com.int4074.wordduo.data.UserStats
import com.int4074.wordduo.data.WordEntry
import com.int4074.wordduo.data.WordProgress
import java.io.File

@Composable
fun MistakesScreen(library: LibraryState, onReview: () -> Unit, onBack: () -> Unit) {
    DuolingoBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FilledTonalButton(onClick = onBack, shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("返回我的")
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("错误词复习", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D241E))
                        Text("把薄弱词集中练透，再回到主训练。", color = Color(0xFF91857B))
                    }
                    Button(
                        onClick = onReview,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A8CFF))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("开始")
                    }
                }
            }
            if (library.weakWords.isEmpty()) {
                item {
                    FloatingCard {
                        Text("当前没有明显错词。", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                        Text("先去做几轮拼写挑战或释义回忆，系统会自动更新弱项。", color = Color(0xFF8F8378))
                    }
                }
            } else {
                items(library.weakWords) { word ->
                    WordRow(word = word, progress = library.progress[word.id])
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(library: LibraryState, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(library.words, query) {
        library.words.filter { it.word.contains(query, true) || it.meaning.contains(query, true) }
    }
    DuolingoBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FilledTonalButton(onClick = onBack, shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("返回我的")
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("词库", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D241E))
                    Text("按单词或中文释义快速检索。", color = Color(0xFF91857B))
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    leadingIcon = {
                        GlossyIconBubble(
                            icon = Icons.Default.Search,
                            accent = Color(0xFF9A8CFF),
                            selected = true,
                            size = 34.dp
                        )
                    },
                    label = { Text("搜索单词或释义") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFFFCF8),
                        unfocusedContainerColor = Color(0xFFFFFCF8),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(0xFF2D241E),
                        unfocusedTextColor = Color(0xFF2D241E),
                        focusedLabelColor = Color(0xFF9A8CFF),
                        unfocusedLabelColor = Color(0xFF9A8D83)
                    )
                )
            }
            items(filtered) { word ->
                WordRow(word = word, progress = library.progress[word.id])
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    stats: UserStats,
    onGoalChanged: (Int) -> Unit,
    onLogout: () -> Unit,
    currentUser: String,
    avatarUri: String,
    onAvatarSelected: (String) -> Unit,
    onOpenMistakes: () -> Unit,
    onOpenLibrary: () -> Unit,
    onBackHome: () -> Unit
) {
    var goalText by rememberSaveable(stats.dailyGoal) { mutableStateOf(stats.dailyGoal.toString()) }
    val context = LocalContext.current
    val profileName = currentUser.ifBlank { "WordLight User" }
    val profileInitial = profileName.firstOrNull()?.uppercaseChar()?.toString() ?: "词"
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val savedAvatarUri = copyAvatarToInternalStorage(context, uri)
            if (savedAvatarUri != null) {
                onAvatarSelected(savedAvatarUri)
            }
        }
    }

    DuolingoBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FilledTonalButton(onClick = onBackHome, shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("返回首页")
                }
            }
            item {
                TopAppBar(title = { Text("我的", color = Color(0xFF2D241E), fontWeight = FontWeight.Bold) })
            }
            item {
                FloatingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(
                            avatarUri = avatarUri,
                            profileInitial = profileInitial,
                            onClick = { avatarPicker.launch("image/*") }
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(profileName, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D241E))
                            Text("本地学习档案", color = Color(0xFF8F8378), fontWeight = FontWeight.SemiBold)
                            Text("点击头像即可选择新的头像，选中的图片会复制到应用本地，不会再卡在文档页面。", color = Color(0xFF9A8D83))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TinyStatChip("XP ${stats.xp}", Color(0xFFFFC25F))
                        TinyStatChip("连胜 ${stats.streakDays}天", Color(0xFFFFB6A1))
                        TinyStatChip("今日 ${stats.studiedToday}/${stats.dailyGoal}", Color(0xFF9AD9B0))
                    }
                }
            }
            item {
                FloatingCard {
                    Text("快捷入口", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                    Text("错词复习和词库查询改成二级入口，底栏不会再把个人页挤掉。", color = Color(0xFF8F8378))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onOpenMistakes,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA399))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("错词本")
                        }
                        Button(
                            onClick = onOpenLibrary,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC46A))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("词库")
                        }
                    }
                }
            }
            item {
                FloatingCard {
                    Text("每日目标", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it.filter(Char::isDigit) },
                        label = { Text("建议 10-30") },
                        shape = RoundedCornerShape(26.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFFF7F0),
                            unfocusedContainerColor = Color(0xFFFFF7F0),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF2D241E),
                            unfocusedTextColor = Color(0xFF2D241E)
                        )
                    )
                    Text("当前已保存：${stats.dailyGoal}", color = Color(0xFF8F8378))
                    Button(
                        onClick = {
                            val nextGoal = (goalText.toIntOrNull() ?: stats.dailyGoal).coerceIn(1, 200)
                            goalText = nextGoal.toString()
                            onGoalChanged(nextGoal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A8CFF)),
                        shape = RoundedCornerShape(20.dp)
                    ) { Text("保存目标") }
                }
            }
            item {
                FloatingCard {
                    Text("说明与账户", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                    Text("1. 发音检查需要麦克风权限、系统语音识别服务和可用的英文语音引擎。", color = Color(0xFF8F8378))
                    Text("2. 头像会从系统图片选择器导入，并复制到应用本地，重启后仍会保留。", color = Color(0xFF8F8378))
                    Text("3. 每日目标支持 1-200，自定义较大的目标后会立即保存。", color = Color(0xFF8F8378))
                    Text("4. 正式词库已从题库 PDF 导入，错词本会随错误次数自动更新。", color = Color(0xFF8F8378))
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9B83)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("退出登录")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(avatarUri: String, profileInitial: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White,
                        Color(0x339DB2FF),
                        Color(0x669A8CFF)
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUri.isNotBlank()) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { imageView ->
                    imageView.setImageURI(Uri.parse(avatarUri))
                }
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF5D68D8),
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = profileInitial,
                color = Color(0xFF2D241E),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
            )
        }
    }
}

private fun copyAvatarToInternalStorage(context: Context, uri: Uri): String? {
    return runCatching {
        val avatarFile = File(context.filesDir, "profile_avatar.jpg")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "avatar input stream is null" }
            avatarFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(avatarFile).toString()
    }.getOrNull()
}

@Composable
private fun WordRow(word: WordEntry, progress: WordProgress?) {
    val mastery = progress?.mastery ?: 0f
    val masteryColor by animateColorAsState(
        if (mastery > 0.7f) Color(0xFF83C99A) else if (mastery > 0.35f) Color(0xFFFFBE68) else Color(0xFFFF8E84),
        label = "masteryColor"
    )
    FloatingCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlossyIconBubble(icon = Icons.AutoMirrored.Filled.MenuBook, accent = masteryColor, selected = true, size = 54.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(word.word, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2D241E))
                Text(word.meaning, color = Color(0xFF675C55), fontWeight = FontWeight.SemiBold)
                if (word.phonetic.isNotBlank()) {
                    Text(word.phonetic, color = Color(0xFF9A8D83), fontSize = 13.sp)
                }
            }
            TinyStatChip(word.level, masteryColor)
        }
        GlossyProgressBar(progress = mastery, color = masteryColor)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TinyStatChip("熟练度 ${(mastery * 100).toInt()}%", masteryColor)
            TinyStatChip("正确 ${progress?.knownCount ?: 0}", Color(0xFF8FCF9D))
            TinyStatChip("错误 ${progress?.wrongCount ?: 0}", Color(0xFFFFA399))
        }
    }
}






