package com.bellatrixos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
