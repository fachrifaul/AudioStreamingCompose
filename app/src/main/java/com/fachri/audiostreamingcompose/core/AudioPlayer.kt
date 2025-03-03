package com.fachri.audiostreamingcompose.core

import android.content.Context
import android.media.MediaPlayer
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

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