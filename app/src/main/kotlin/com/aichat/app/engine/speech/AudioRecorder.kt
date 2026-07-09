package com.aichat.app.engine.speech

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * 麦克风采集器：以 16kHz / 16bit / 单声道采集 PCM，
 * 后台线程持续读取并缓存，供 [SpeechEngineImpl] 周期性取出转写。
 *
 * [stop] 返回标准 WAV 字节（含 44 字节头），可直接用于 [Whisper] 一次性转写。
 *
 * @param sampleRate 采样率（默认 16000，whisper 要求）
 * @param channelConfig 声道（默认单声道）
 * @param audioFormat 采样格式（默认 16bit PCM）
 */
class AudioRecorder(
    private val sampleRate: Int = SAMPLE_RATE,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {

    @Volatile private var audioRecord: AudioRecord? = null
    private val queue = ArrayDeque<Short>()
    private val lock = Any()
    @Volatile private var recording = false
    private var readerThread: Thread? = null

    /** 开始录音（需 RECORD_AUDIO 权限）。 */
    fun start() {
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        require(minBuf > 0) { "AudioRecord 缓冲区大小无效：可能缺少录音权限或无麦克风" }

        val ar = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBuf * 2)
        require(ar.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord 初始化失败" }
        audioRecord = ar

        recording = true
        ar.startRecording()
        readerThread = thread(name = "AudioRecorder", isDaemon = true) {
            val buffer = ShortArray(minBuf / 2)
            while (recording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    synchronized(lock) {
                        for (i in 0 until read) queue.add(buffer[i])
                    }
                }
            }
        }
    }

    /**
     * 取出并清空已采集的 PCM（16bit），转换为 whisper 需要的 Float（值域约 [-1, 1]）。
     * @return 可读的 FloatBuffer（position=0，remaining=采样数）
     */
    fun consumeFloatBuffer(): FloatBuffer {
        val shorts = synchronized(lock) {
            val arr = queue.toShortArray()
            queue.clear()
            arr
        }
        val floats = FloatArray(shorts.size) { shorts[it] / 32768.0f }
        return FloatBuffer.wrap(floats)
    }

    /** 停止录音并返回 WAV 字节（含 44 字节头 + 16bit PCM 单声道）。 */
    fun stop(): ByteArray {
        recording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        readerThread?.join(1000)
        readerThread = null
        val shorts = synchronized(lock) {
            val arr = queue.toShortArray()
            queue.clear()
            arr
        }
        return encodeWav(shorts)
    }

    /** 编码为标准 16bit PCM 单声道 WAV。 */
    private fun encodeWav(shorts: ShortArray): ByteArray {
        val dataSize = shorts.size * 2
        val out = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        out.put("RIFF".toByteArray(Charsets.US_ASCII))
        out.putInt(36 + dataSize)
        out.put("WAVE".toByteArray(Charsets.US_ASCII))
        out.put("fmt ".toByteArray(Charsets.US_ASCII))
        out.putInt(16)              // fmt 子块大小
        out.putShort(1)             // 音频格式：PCM
        out.putShort(1)             // 声道数：单声道
        out.putInt(sampleRate)
        out.putInt(sampleRate * 2)  // 字节率 = sampleRate * channels * bits/8
        out.putShort(2)             // 块对齐 = channels * bits/8
        out.putShort(16)            // 位深
        out.put("data".toByteArray(Charsets.US_ASCII))
        out.putInt(dataSize)
        for (s in shorts) out.putShort(s)
        return out.array()
    }

    companion object {
        /** whisper 要求的采样率。 */
        const val SAMPLE_RATE = 16000

        /**
         * 将 WAV 字节解析为 whisper 输入 Float（单声道 16bit -> [-1, 1]）。
         * 支持标准 44 字节头；非 WAV 或数据过短则返回空数组。
         */
        fun wavToFloat(wav: ByteArray): FloatArray {
            if (wav.size < 44) return FloatArray(0)
            val header = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
            val dataSize = header.getInt(40)
            val payloadLen = dataSize.coerceAtMost(wav.size - 44)
            if (payloadLen <= 0 || payloadLen % 2 != 0) return FloatArray(0)
            val sbb = ByteBuffer.wrap(wav, 44, payloadLen).order(ByteOrder.LITTLE_ENDIAN)
            val out = FloatArray(payloadLen / 2)
            for (i in out.indices) out[i] = sbb.short / 32768.0f
            return out
        }
    }
}
