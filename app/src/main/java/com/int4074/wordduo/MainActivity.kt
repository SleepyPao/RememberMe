package com.int4074.wordduo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.int4074.wordduo.ui.WordDuoApp
import com.int4074.wordduo.ui.theme.WordDuoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WordDuoTheme {
                WordDuoApp()
            }
        }
    }
}
