package com.nafaq.expenses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nafaq.expenses.ui.navigation.NafaqNavHost
import com.nafaq.expenses.ui.theme.NafaqTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NafaqTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NafaqNavHost()
                }
            }
        }
    }
}
