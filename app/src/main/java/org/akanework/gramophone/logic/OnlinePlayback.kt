package org.akanework.gramophone.logic

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.akanework.gramophone.utils.AudioQuality
import org.akanework.gramophone.utils.InnerTubeXPlayer

/** Holds the HTTP headers required by the currently playing online stream. */
object OnlineStreamHeaders {
    @Volatile
    var current: Map<String, String>? = null
}

/**
 * Fetches a playable stream for a YouTube (Music) video id and starts
 * playback through the existing Media3 controller, just like a local song.
 */
suspend fun playYouTubeStream(
    context: Context,
    controller: MediaController?,
    videoId: String,
): String? = withContext(Dispatchers.IO) {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val result = InnerTubeXPlayer.playerResponseForPlayback(videoId, null, AudioQuality.AUTO, cm)
    result.getOrNull()?.let { data ->
        OnlineStreamHeaders.current = data.streamHeaders
        val durationMs = data.videoDetails?.lengthSeconds?.toLongOrNull()?.times(1000L)
        val metadata = MediaMetadata.Builder()
            .setTitle(data.videoDetails?.title ?: "")
            .setArtist(data.videoDetails?.author ?: "")
            .setArtworkUri(data.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url?.toUri())
            .setDurationMs(durationMs ?: C.TIME_UNSET)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()
        val item = MediaItem.Builder()
            .setMediaId(videoId)
            .setMediaMetadata(metadata)
            .setUri(Uri.parse(data.streamUrl))
            .setMimeType(
                data.format.mimeType.substringBefore(";").takeIf { it.isNotBlank() } ?: "audio/mp4"
            )
            .build()
        withContext(Dispatchers.Main) {
            controller?.apply {
                setMediaItems(listOf(item), 0, C.TIME_UNSET)
                prepare()
                play()
            }
        }
        null
    } ?: (result.exceptionOrNull()?.let {
        timber.log.Timber.e(it, "Stream failed")
        it.message ?: it.javaClass.simpleName
    } ?: "Unknown error")
}
