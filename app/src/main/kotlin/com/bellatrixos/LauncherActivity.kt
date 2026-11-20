package com.bellatrixos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bellatrixos.storage.LauncherData

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        val home = if (LauncherData.useOwlHome(this)) OwlHomeActivity::class.java else MainActivity::class.java
        startActivity(Intent(this, home))
        finish()
    }
}
