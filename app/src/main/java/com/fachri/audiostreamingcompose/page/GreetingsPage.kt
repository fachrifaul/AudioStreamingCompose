package com.fachri.audiostreamingcompose.page

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.fachri.audiostreamingcompose.core.orange
import com.fachri.audiostreamingcompose.network.model.VoiceOption
import com.fachri.audiostreamingcompose.network.model.bgColor
import com.fachri.audiostreamingcompose.network.model.borderColor
import com.fachri.audiostreamingcompose.network.service.API
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GreetingsViewModel(
    private val api: API,
    private var mediaPlayer: MediaPlayer = MediaPlayer()
) : ViewModel() {

    private val _voices = MutableStateFlow(emptyList<VoiceOption>())
    val voices: StateFlow<List<VoiceOption>> = _voices.asStateFlow()

    private val _selectedVoice = MutableStateFlow<VoiceOption?>(null)
    val selectedVoice: StateFlow<VoiceOption?> = _selectedVoice.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun fetchVoices() {
        viewModelScope.launch {
            _voices.value = listOf(
                VoiceOption(voiceId = 1, sampleId = 1, name = "Meadow"),
                VoiceOption(voiceId = 2, sampleId = 1, name = "Cypress"),
                VoiceOption(voiceId = 3, sampleId = 1, name = "Iris"),
                VoiceOption(voiceId = 4, sampleId = 1, name = "Hawke"),
                VoiceOption(voiceId = 5, sampleId = 1, name = "Seren"),
                VoiceOption(voiceId = 6, sampleId = 1, name = "Stone"),
            )
        }
    }

    fun fetch() {
        viewModelScope.launch {
            api.fetchGreetings()
                .onSuccess { voices ->
                    _voices.value = voices
                }
                .onFailure { error ->
                    _errorMessage.value = error.localizedMessage
                }
        }
    }

    fun selectVoice(voice: VoiceOption) {
        _selectedVoice.value = voice
        playSound(voice.soundUrlString)
    }

     fun playSound(urlString: String) {
        viewModelScope.launch {
            mediaPlayer.release()

            mediaPlayer = MediaPlayer()
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
fun GreetingPage(
    navController: NavController,
    api: API = API(context = LocalContext.current),
    viewModel: GreetingsViewModel = remember { GreetingsViewModel(api = api) }
) {
    val voices by viewModel.voices.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val scope = rememberCoroutineScope()

    val composition by rememberLottieComposition(LottieCompositionSpec.Url("https://static.dailyfriend.ai/images/mascot-animation.json"))
    val lottieAnimatable = rememberLottieAnimatable()


    LaunchedEffect(Unit) {
        viewModel.fetchVoices()

        composition?.let {
            lottieAnimatable.animate(composition)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pick my voice", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        LottieAnimation(
            composition,
            progress = {
                lottieAnimatable.progress
            },
            modifier = Modifier.size(150.dp)
        )

        Text("Find the voice that resonates with you", fontSize = 16.sp)

        if (errorMessage != null) {
            Text(errorMessage!!, color = Color.Red)
            Button(onClick = { viewModel.fetchVoices() }) { Text("Retry") }
        } else if (voices.isNotEmpty()) {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth()) {
                items(voices.size) { index ->
                    val voice = voices[index]
                    VoiceButtonView(index, voice, selectedVoice) {
                        scope.launch {
                            viewModel.selectVoice(voice)
                            lottieAnimatable.animate(composition = composition)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.stopAudio()

                    val json = Uri.encode(Gson().toJson(viewModel.selectedVoice.value))
                    navController.navigate("conversation/${json}")
                },
                enabled = selectedVoice != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedVoice != null) Color.orange() else Color(
                        0xFFB0B0B0
                    ), // Darker gray
                    disabledContainerColor = Color(0xFFB0B0B0),
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                Text("Next")
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
fun VoiceButtonView(
    index: Int,
    voice: VoiceOption,
    selectedVoice: VoiceOption?,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(index.bgColor(), RoundedCornerShape(12.dp)) // Add border
            .let {
                if (selectedVoice?.id == voice.id) {
                    it.border(1.dp, index.borderColor(), RoundedCornerShape(12.dp))
                } else it
            }
            .clickable(onClick = onSelect)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(voice.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                if (selectedVoice?.id == voice.id) Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = Color.orange()
            )
        }

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(voice.imageUrlString)
                .decoderFactory(SvgDecoder.Factory()) // Enable SVG decoding
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPagePreview() {
    GreetingPage(navController = rememberNavController()) // Mock NavController
}