package com.fachri.audiostreamingcompose.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

interface AudioPlayerInterface {

    fun play(urlString: String)

    fun stop()
}

class AudioPlayer : AudioPlayerInterface {

    private var mediaPlayer: MediaPlayer? = null

    override fun play(urlString: String) {

        mediaPlayer = MediaPlayer()
        mediaPlayer?.setDataSource(urlString)
        mediaPlayer?.prepare()
        mediaPlayer?.start()

    }

    override fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }
}

class AudioPlayerStreaming(context: Context) : AudioPlayerInterface {

    private var mediaPlayer = ExoPlayer.Builder(context).build()

    override fun play(urlString: String) {
        val mediaItem = MediaItem.fromUri(urlString)
        mediaPlayer.setMediaItem(mediaItem)
        mediaPlayer.prepare()
        mediaPlayer.play()

    }

    override fun stop() {
        mediaPlayer.stop()
        mediaPlayer.clearMediaItems()
    }
}

class AudioPlayerTrack : AudioPlayerInterface {

    private var audioTrack: AudioTrack? = null
    private var streamingJob: Job? = null

    override fun play(urlString: String) {
        streamingJob = CoroutineScope(Dispatchers.IO).launch {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val inputStream = getAudioStream(urlString) ?: return@launch
            val buffer = ByteArray(minBufferSize)
            audioTrack?.play()

            try {
                var bytesRead = 0
                while (isActive && inputStream.read(buffer).also { bytesRead = it } > 0) {
                    audioTrack?.write(buffer, 0, bytesRead)
                }
            } catch (e: Exception) {
                Log.e("AudioPlayerTrack", "Error in streaming audio", e)
            } finally {
                stop()
            }
        }
    }

    override fun stop() {
        streamingJob?.cancel()
        if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
            audioTrack?.stop()
        }
        audioTrack?.release()
    }

    private fun getAudioStream(urlString: String): InputStream? {
        return try {
            val url = URL("https://www.mauvecloud.net/sounds/pcm1644m.wav")
//            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()
            BufferedInputStream(connection.inputStream)
        } catch (e: Exception) {
            Log.e("AudioPlayerTrack", "Failed to connect to stream", e)
            null
        }
    }
}
