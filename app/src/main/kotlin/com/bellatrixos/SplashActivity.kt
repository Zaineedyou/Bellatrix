package com.bellatrixos

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(null)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
