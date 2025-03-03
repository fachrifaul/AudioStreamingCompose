package com.fachri.audiostreamingcompose

import com.fachri.audiostreamingcompose.core.AudioPlayerInterface
import com.fachri.audiostreamingcompose.network.API
import com.fachri.audiostreamingcompose.network.model.VoiceOption
import com.fachri.audiostreamingcompose.page.GreetingsViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GreetingsViewModelTest {

    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()

    private lateinit var api: API
    private lateinit var mockAudioPlayer: AudioPlayerInterface
    private lateinit var viewModel: GreetingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockAudioPlayer = mockk<AudioPlayerInterface>(relaxed = true)
        api = mockk(relaxed = true)
        viewModel = spyk(GreetingsViewModel(api, mockAudioPlayer))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `fetchVoices should populate voices list`() = runTest {
        // Given
        val voices = listOf(
            VoiceOption(voiceId = 1, sampleId = 1, name = "Meadow"),
            VoiceOption(voiceId = 2, sampleId = 1, name = "Cypress")
        )

        coEvery { api.fetchGreetings() } returns Result.success(voices)

        // When
        viewModel.fetch()

        // Then
        val currentVoices = viewModel.voices.value
        assertEquals(voices, currentVoices)
    }

    @Test
    fun `selectVoice should update selectedVoice and play sound`() = runTest {
        // Given
        val voice = VoiceOption(voiceId = 1, sampleId = 1, name = "Meadow")

        coEvery { viewModel.playSound(voice.soundUrlString) } just Runs

        // When
        viewModel.selectVoice(voice)

        // Then
        val selectedVoice = viewModel.selectedVoice.value
        assertEquals(voice, selectedVoice)
        verify { viewModel.playSound(voice.soundUrlString) }
    }

    @Test
    fun `stopAudio should stop and release media player`() = runTest {
        // Given
        viewModel = GreetingsViewModel(api = api, audioPlayer = mockAudioPlayer)

        // When
        viewModel.stopAudio()

        // Then
        verify { mockAudioPlayer.stop() }
    }

    @Test
    fun `fetchVoices should handle error correctly`() = runTest {
        // Given
        val errorMessage = "Error fetching voices"

        coEvery { api.fetchGreetings() } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.fetch()

        // Then
        val currentErrorMessage = viewModel.errorMessage.value
        assertNotNull(currentErrorMessage)
        assertEquals(errorMessage, currentErrorMessage)
    }
}