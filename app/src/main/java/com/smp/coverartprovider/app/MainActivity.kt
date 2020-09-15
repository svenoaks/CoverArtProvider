package com.smp.coverartprovider.app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.smp.coverartprovider.app.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun startProvider() {
        with (Intent(Intent.ACTION_OPEN_DOCUMENT)) {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(this, 1234)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun launchPicker(view: View) {
        startProvider()
    }
}