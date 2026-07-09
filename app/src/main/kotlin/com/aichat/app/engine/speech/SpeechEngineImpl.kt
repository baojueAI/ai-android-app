package com.aichat.app.engine.speech

import com.aichat.app.jni.whisper.Whisper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * 语音引擎实现：组合 [Whisper]（端侧 ASR）与 [AudioRecorder]（采集 16kHz 单声道 PCM）。
 *
 * 转写与 LLM 推理共用 [Dispatchers.Default.limitedParallelism] 的单线程调度器，
 * 避免移动端资源争用。
 */
class SpeechEngineImpl(
    private val dispatcher: CoroutineContext = Dispatchers.Default.limitedParallelism(1)
) : SpeechEngine {

    private var whisper: Whisper? = null
    @Volatile private var recorder: AudioRecorder? = null
    private var recordingJob: kotlinx.coroutines.Job? = null

    override suspend fun load(path: String) = withContext(dispatcher) {
        whisper?.close()
        whisper = Whisper.load(path)
    }

    override fun startRecording(onSegment: (String) -> Unit) {
        val rec = AudioRecorder()
        rec.start()
        recorder = rec
        recordingJob = CoroutineScope(dispatcher).launch {
            while (recorder == rec) {
                delay(LIVE_INTERVAL_MS)
                val buf = rec.consumeFloatBuffer()
                if (buf.hasRemaining()) {
                    val samples = FloatArray(buf.remaining()) { buf.get() }
                    val segs = runCatching { whisper?.transcribe(samples) }.getOrNull() ?: emptyList()
                    val text = segs.joinToString("") { it.text }
                    if (text.isNotBlank()) onSegment(text)
                }
            }
        }
    }

    override fun stopRecording(): ByteArray {
        val rec = recorder ?: return ByteArray(0)
        recorder = null
        recordingJob?.cancel()
        recordingJob = null
        return rec.stop()
    }

    override suspend fun transcribe(audio: ByteArray): String = withContext(dispatcher) {
        val floats = AudioRecorder.wavToFloat(audio)
        whisper?.transcribe(floats)?.joinToString("") { it.text } ?: ""
    }

    /** 卸载 ASR 模型。 */
    fun unload() {
        recordingJob?.cancel()
        recordingJob = null
        recorder?.stop()
        recorder = null
        whisper?.close()
        whisper = null
    }

    private companion object {
        const val LIVE_INTERVAL_MS = 2000L
    }
}
