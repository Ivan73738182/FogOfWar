package com.ivangames.fogofwar

import android.os.Bundle
import android.widget.TextView
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this).apply {
            text = "FogOfWar"
            textSize = 40f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            gravity = android.view.Gravity.CENTER
        }
        
        setContentView(textView)
    }
}
