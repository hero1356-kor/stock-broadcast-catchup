package com.yeongung.stockbroadcastcatchup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yeongung.stockbroadcastcatchup.ui.StockBroadcastCatchupApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StockBroadcastCatchupApp()
        }
    }
}
