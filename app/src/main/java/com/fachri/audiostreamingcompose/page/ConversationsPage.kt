package com.fachri.audiostreamingcompose.page

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.fachri.audiostreamingcompose.network.model.VoiceOption
import com.fachri.audiostreamingcompose.network.API
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConversationsViewModel(
    private val api: API,
    private val voiceOption: VoiceOption,
    private var mediaPlayer: MediaPlayer = MediaPlayer()
) : ViewModel() {

    private val _text = MutableStateFlow<String?>(null)
    val text = _text.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun fetch(randomSampleId: Int = (2..20).random()) {
        viewModelScope.launch {
            api.fetchTranscription(voiceOption.voiceId, randomSampleId)
                .onSuccess { transcription ->
                    _text.value = transcription
                    startAudio(
                        API.soundUrlString(
                            voiceId = voiceOption.voiceId,
                            sampleId = randomSampleId
                        )
                    )
                }
                .onFailure { error ->
                    _errorMessage.value = error.localizedMessage
                }
        }
    }

    private fun startAudio(urlString: String) {
        viewModelScope.launch {
            mediaPlayer.setDataSource(urlString)
            mediaPlayer.prepare()
            mediaPlayer.start()
        }
    }

    fun stopAudio() {
        viewModelScope.launch {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
    }
}

@Composable
fun ConversationsPage(
    navController: NavController,
    api: API = API(context = LocalContext.current),
    voiceOption: VoiceOption,
    viewModel: ConversationsViewModel = remember {
        ConversationsViewModel(
            api = api,
            voiceOption = voiceOption
        )
    }
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://static.dailyfriend.ai/images/mascot-animation.json")
    )
    val progress by animateLottieCompositionAsState(composition)

    val text by viewModel.text.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetch()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAudio()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back"
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center

        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(300.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            when {
                errorMessage != null -> {
                    Text(errorMessage ?: "")
                    Button(onClick = { viewModel.fetch() }) { Text("Retry") }
                }

                text != null -> Text(
                    text ?: "",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )

                else -> CircularProgressIndicator()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConversationPagePreview() {
    ConversationsPage(
        navController = rememberNavController(),
        voiceOption = VoiceOption(voiceId = 1, sampleId = 1, name = "Meadow")
    )
}