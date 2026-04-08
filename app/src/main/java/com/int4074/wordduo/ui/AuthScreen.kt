package com.int4074.wordduo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.int4074.wordduo.data.AuthState

@Composable
fun AuthEntryScreen(
    authState: AuthState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit
) {
    var isLogin by rememberSaveable { mutableStateOf(true) }
    var account by rememberSaveable(authState.lastAccount) { mutableStateOf(authState.lastAccount) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    DuolingoBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(0xFFFF8F7C), CircleShape)
                    .align(Alignment.TopEnd)
            )
            FloatingCard(modifier = Modifier.align(Alignment.Center)) {
                Text("词光", color = Color(0xFF2D241E), fontSize = 36.sp, fontWeight = FontWeight.Black)
                Text(
                    if (isLogin) "登录后继续今天的背词节奏" else "注册一个本地账号，保存你的学习进度",
                    color = Color(0xFF8F8378)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = { isLogin = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text("登录") }
                    Button(
                        onClick = { isLogin = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A8CFF)),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text("注册") }
                }
                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("账号") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFFF7F0),
                        unfocusedContainerColor = Color(0xFFFFF7F0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(0xFF2D241E),
                        unfocusedTextColor = Color(0xFF2D241E)
                    )
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("密码") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFFF7F0),
                        unfocusedContainerColor = Color(0xFFFFF7F0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(0xFF2D241E),
                        unfocusedTextColor = Color(0xFF2D241E)
                    )
                )
                if (!isLogin) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("确认密码") },
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFFF7F0),
                            unfocusedContainerColor = Color(0xFFFFF7F0),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF2D241E),
                            unfocusedTextColor = Color(0xFF2D241E)
                        )
                    )
                }
                authState.authMessage?.let {
                    Text(it, color = Color(0xFFE46B62), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        if (isLogin) onLogin(account, password) else onRegister(account, password, confirmPassword)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isLogin) Color(0xFFFFA58D) else Color(0xFF9A8CFF)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(if (isLogin) "进入词光" else "创建账号")
                }
            }
        }
    }
}
