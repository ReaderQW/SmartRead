package com.example.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * 最终调试版 vivo TTS 管理类
 */
class VivoTtsManager(private val appKey: String) {
    private val TAG = "VivoTtsManager"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private var audioTrack: AudioTrack? = null

    private val audioQueue = LinkedBlockingQueue<ByteArray>()
    private var playerThread: Thread? = null
    
    private var currentText: String = ""
    private var currentVcn: String = "M24"
    
    enum class PlayState { IDLE, PLAYING, PAUSED }
    @Volatile private var currentState = PlayState.IDLE

    var onPlayStateChanged: ((Boolean) -> Unit)? = null

    init {
        startPlayerThread()
    }

    private fun startPlayerThread() {
        playerThread = thread(start = true, name = "TTS-Player") {
            Log.d(TAG, "Player thread started")
            try {
                while (!Thread.currentThread().isInterrupted) {
                    if (currentState == PlayState.PLAYING) {
                        val pcmData = audioQueue.poll(50, TimeUnit.MILLISECONDS)
                        if (pcmData != null) {
                            audioTrack?.let { track ->
                                if (track.state == AudioTrack.STATE_INITIALIZED) {
                                    val written = track.write(pcmData, 0, pcmData.size)
                                    if (written < 0) Log.e(TAG, "AudioTrack write error: $written")
                                    if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                                        track.play()
                                    }
                                }
                            }
                        }
                    } else {
                        Thread.sleep(50)
                    }
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Player thread interrupted")
            }
        }
    }

    fun speak(text: String, vcn: String, volume: Int) {
        Log.d(TAG, "speak() called with text length: ${text.length}, vcn: $vcn")
        
        if (text != currentText || vcn != currentVcn) {
            Log.d(TAG, "Content or VCN changed, stopping current playback")
            stop()
            currentText = text
            currentVcn = vcn
        }

        if (currentState == PlayState.PAUSED) {
            Log.d(TAG, "Resuming from PAUSED state")
            resume()
            return
        }

        if (currentState == PlayState.PLAYING) {
            Log.d(TAG, "Already playing, ignoring request")
            return
        }

        currentState = PlayState.PLAYING
        onPlayStateChanged?.invoke(true)
        
        initAudioTrack()
        val chunks = splitText(text, 600)
        connectAndPlay(chunks, volume)
    }

    private fun initAudioTrack() {
        if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
            Log.d(TAG, "Initializing AudioTrack")
            try {
                audioTrack?.release()
                val minBufferSize = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(24000)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(minBufferSize * 4)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "AudioTrack init failed: ${e.message}")
            }
        }
        audioTrack?.play()
    }

    private fun connectAndPlay(chunks: List<String>, volume: Int) {
        val requestId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString().replace("-", "").take(32)
        
        val urlBuilder = "https://api-ai.vivo.com.cn/tts".toHttpUrl().newBuilder()
            .addEncodedQueryParameter("engineid", "tts_humanoid_lam")
            .addEncodedQueryParameter("system_time", (System.currentTimeMillis() / 1000).toString())
            .addEncodedQueryParameter("user_id", userId)
            .addEncodedQueryParameter("requestId", requestId)
            .addEncodedQueryParameter("model", "unknown")
            .addEncodedQueryParameter("product", "unknown")
            .addEncodedQueryParameter("package", "unknown")
            .addEncodedQueryParameter("client_version", "unknown")
            .addEncodedQueryParameter("system_version", "unknown")
            .addEncodedQueryParameter("sdk_version", "unknown")
            .addEncodedQueryParameter("android_version", "unknown")

        val url = urlBuilder.build().toString().replace("https://", "wss://")
        Log.d(TAG, "Connecting to TTS WSS: $url")

        val request = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $appKey")
            .addHeader("X-AI-GATEWAY-SIGNATURE", "developers-aigc").build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "TTS WebSocket Opened. Sending ${chunks.size} chunks")
                chunks.forEachIndexed { index, chunk ->
                    val base64Text = Base64.encodeToString(chunk.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    val json = JSONObject().apply {
                        put("aue", 0)
                        put("vcn", currentVcn)
                        put("text", base64Text)
                        put("reqId", (System.currentTimeMillis() + index).toString())
                        put("volume", volume)
                        put("speed", 50)
                        put("encoding", "utf8")
                    }
                    webSocket.send(json.toString())
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                val errCode = json.optInt("error_code")
                if (errCode == 0) {
                    val audioBase64 = json.optJSONObject("data")?.optString("audio")
                    if (!audioBase64.isNullOrEmpty()) {
                        val pcm = Base64.decode(audioBase64, Base64.DEFAULT)
                        Log.d(TAG, "Received audio chunk: ${pcm.size} bytes")
                        audioQueue.put(pcm)
                    }
                    if (json.optJSONObject("data")?.optInt("status") == 2) {
                        Log.d(TAG, "Server signaled end of synthesis")
                    }
                } else {
                    Log.e(TAG, "TTS Server Error: $errCode - ${json.optString("error_msg")}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "TTS WebSocket Failure: ${t.message}. Response: $response")
                if (currentState != PlayState.PAUSED) {
                    currentState = PlayState.IDLE
                    onPlayStateChanged?.invoke(false)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "TTS WebSocket Closing: $code / $reason")
            }
        })
    }

    fun pause() {
        if (currentState == PlayState.PLAYING) {
            Log.d(TAG, "Pausing playback")
            currentState = PlayState.PAUSED
            audioTrack?.pause()
            onPlayStateChanged?.invoke(false)
        }
    }

    private fun resume() {
        if (currentState == PlayState.PAUSED) {
            Log.d(TAG, "Resuming playback")
            currentState = PlayState.PLAYING
            audioTrack?.play()
            onPlayStateChanged?.invoke(true)
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping TTS")
        currentState = PlayState.IDLE
        webSocket?.close(1000, "User Stop")
        webSocket = null
        audioQueue.clear()
        audioTrack?.apply {
            if (state == AudioTrack.STATE_INITIALIZED) {
                try {
                    pause()
                    flush()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping AudioTrack: ${e.message}")
                }
            }
        }
        onPlayStateChanged?.invoke(false)
    }

    private fun splitText(text: String, limit: Int): List<String> {
        return text.chunked(limit)
    }

    fun release() {
        stop()
        playerThread?.interrupt()
        audioTrack?.release()
        audioTrack = null
    }
}
