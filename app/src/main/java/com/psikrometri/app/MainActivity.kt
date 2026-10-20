package com.psikrometri.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // klavye pencereyi küçültmesin: ekran aynı kalır, klavye üstüne biner
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { com.psikrometri.app.ui.AnaEkran() }
    }
}
