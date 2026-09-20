package com.tick.magna

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity

/**
 * A FragmentActivity rather than a ComponentActivity, and only for one reason: `BiometricPrompt`
 * requires one. FragmentActivity extends ComponentActivity, so nothing else about this changes.
 */
class MainActivity: FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}
