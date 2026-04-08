package com.int4074.wordduo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.int4074.wordduo.data.PracticeMode
import com.int4074.wordduo.data.WordEntry

@Composable
fun DuolingoBackdrop(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFCF6),
                        Color(0xFFFFF7ED),
                        Color(0xFFFFFAF2)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x14CDBDFF), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x18FFC7A3), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        content()
    }
}

@Composable
fun FloatingCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.shadow(20.dp, RoundedCornerShape(32.dp), clip = false),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF8))
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
fun GlossyIconBubble(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    selected: Boolean,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(if (selected) 14.dp else 8.dp, CircleShape, clip = false)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White,
                        accent.copy(alpha = if (selected) 0.34f else 0.18f),
                        accent.copy(alpha = if (selected) 0.68f else 0.36f)
                    )
                ),
                shape = CircleShape
            )
            .border(1.dp, Color.White.copy(alpha = 0.95f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accent.copy(alpha = 0.96f) else Color(0xFF8C8076),
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
fun MetricOrb(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(16.dp, CircleShape, clip = false)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        tint.copy(alpha = 0.20f),
                        Color(0xFFF9F1E7)
                    )
                ),
                shape = CircleShape
            )
            .border(1.dp, Color.White.copy(alpha = 0.92f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlossyIconBubble(icon = icon, accent = tint, selected = true, size = 40.dp)
            Text(title, color = Color(0xFF9A8D83), fontSize = 12.sp)
            Text(value, color = Color(0xFF2B221E), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun GlossyProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .shadow(8.dp, RoundedCornerShape(999.dp), clip = false)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF0E6DA))
                ),
                shape = RoundedCornerShape(999.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.95f), RoundedCornerShape(999.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(color.copy(alpha = 0.84f), color, color.copy(alpha = 0.70f))
                    ),
                    shape = RoundedCornerShape(999.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
                    .background(Color.White.copy(alpha = 0.34f), RoundedCornerShape(999.dp))
            )
        }
    }
}

private fun modeAccent(mode: PracticeMode): Color = when (mode) {
    PracticeMode.Recite -> Color(0xFF8FCF9D)
    PracticeMode.Pronunciation -> Color(0xFF8BC5FF)
    PracticeMode.Meaning -> Color(0xFFFFC97A)
    PracticeMode.Spelling -> Color(0xFFFF9E8E)
    PracticeMode.MistakeReview -> Color(0xFF9A8CFF)
}

private fun modeIcon(mode: PracticeMode): androidx.compose.ui.graphics.vector.ImageVector = when (mode) {
    PracticeMode.Recite -> Icons.AutoMirrored.Filled.MenuBook
    PracticeMode.Pronunciation -> Icons.Default.Mic
    PracticeMode.Meaning -> Icons.Default.CheckCircle
    PracticeMode.Spelling -> Icons.Default.GraphicEq
    PracticeMode.MistakeReview -> Icons.Default.Warning
}

@Composable
fun ModeCard(mode: PracticeMode, onClick: () -> Unit) {
    val accent = modeAccent(mode)
    FloatingCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlossyIconBubble(icon = modeIcon(mode), accent = accent, selected = true, size = 66.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(mode.title, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color(0xFF2D241E))
                Text(mode.subtitle, color = Color(0xFF8F8378))
                Text(mode.title, color = accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("开始")
            }
        }
    }
}

fun exampleTextFor(word: WordEntry): String {
    if (word.example.isNotBlank()) return word.example.removePrefix("备注：").trim()
    val lower = word.word.lowercase()
    return when {
        "动词" in word.tags && "短语" in word.tags -> "Please $lower before you leave the room."
        "动词" in word.tags -> "We need to $lower this task before Friday."
        "形容词" in word.tags -> "The new plan looks $lower and practical."
        "副词" in word.tags -> "Please check the details $lower before you submit the form."
        "名词" in word.tags && word.word.contains(" ") -> "The $lower was mentioned in today's class discussion."
        "名词" in word.tags -> "The $lower is important in this topic."
        else -> "We discussed $lower in class today."
    }
}

@Composable
fun TinyStatChip(text: String, tint: Color) {
    Surface(
        color = tint.copy(alpha = 0.16f),
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color(0xFF5F544D),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}


