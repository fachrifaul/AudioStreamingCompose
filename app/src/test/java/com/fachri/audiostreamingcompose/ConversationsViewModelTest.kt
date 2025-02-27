import android.media.MediaPlayer
import com.fachri.audiostreamingcompose.network.model.VoiceOption
import com.fachri.audiostreamingcompose.network.service.API
import com.fachri.audiostreamingcompose.page.ConversationsViewModel
import io.mockk.coEvery
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsViewModelTest {

    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()

    private lateinit var api: API
    private lateinit var mediaPlayerMock: MediaPlayer
    private lateinit var viewModel: ConversationsViewModel

    private val voiceOption = VoiceOption(voiceId = 1, sampleId = 1, name = "Meadow")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mediaPlayerMock = mockk<MediaPlayer>(relaxed = true)
        api = mockk(relaxed = true)
        viewModel = spyk(ConversationsViewModel(api, voiceOption, mediaPlayerMock))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `fetch updates text state on success`() = runTest {
        // Given
        val transcription: String = "Test Transcription"
        coEvery {
            api.fetchTranscription(any(), any())
        } returns Result.success(transcription)

        // When
        viewModel.fetch(5)

        // Then
        assertEquals(transcription, viewModel.text.value)
        verify { mediaPlayerMock.setDataSource(any<String>()) }
        verify { mediaPlayerMock.prepare() }
        verify { mediaPlayerMock.start() }
    }

    @Test
    fun `fetch updates errorMessage state on failure`() = runTest {
        // Given
        val errorMessage = "Network Error"
        coEvery { api.fetchTranscription(any(), any()) } returns Result.failure(
            Exception(
                errorMessage
            )
        )

        // When
        viewModel.fetch(5)

        // Then
        assertEquals(errorMessage, viewModel.errorMessage.value)
        assertNull(viewModel.text.value)
    }

    @Test
    fun `stopAudio releases MediaPlayer`() = runTest {
        // When
        viewModel.stopAudio()

        // Then
        verify { mediaPlayerMock.stop() }
        verify { mediaPlayerMock.release() }
    }

}
