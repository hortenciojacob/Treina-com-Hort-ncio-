package com.hortencio.treinacomhortencio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hortencio.treinacomhortencio.ui.TreinaApp
import com.hortencio.treinacomhortencio.ui.theme.TreinaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TreinaTheme { TreinaApp() } }
    }
}
