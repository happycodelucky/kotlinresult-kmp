/*
 * kotlinresult — Android sample entry point.
 *
 * Minimal Compose screen that builds a `:kotlinresult` `Result` and shows it —
 * the same API as `kotlin.Result`.
 */
package com.happycodelucky.kotlinresult.example.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.happycodelucky.kotlinresult.Result
import com.happycodelucky.kotlinresult.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GreetingScreen()
                }
            }
        }
    }
}

@Composable
private fun GreetingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = Result.success("KotlinResult").map { "Hello from $it on Android" }.getOrNull() ?: "failed")
    }
}
