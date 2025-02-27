package com.fachri.audiostreamingcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fachri.audiostreamingcompose.network.model.VoiceOption
import com.fachri.audiostreamingcompose.page.ConversationsPage
import com.fachri.audiostreamingcompose.page.GreetingPage
import com.google.gson.Gson

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "greeting") {
        composable("greeting") { GreetingPage(navController) }
        composable("conversation/{voiceOption}") { backStackEntry ->
            val json = backStackEntry.arguments?.getString("voiceOption")
            val voiceOption = Gson().fromJson(json, VoiceOption::class.java)
            ConversationsPage(navController, voiceOption = voiceOption)
        }
    }
}