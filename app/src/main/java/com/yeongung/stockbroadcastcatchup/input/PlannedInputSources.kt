package com.yeongung.stockbroadcastcatchup.input

class MicInputSource : BroadcastInputSource {
    override val type: BroadcastInputSourceType = BroadcastInputSourceType.MICROPHONE

    override fun currentSnapshot(): BroadcastInputSnapshot =
        BroadcastInputSnapshot.pending("Microphone")
}

class AndroidPlaybackCaptureSource : BroadcastInputSource {
    override val type: BroadcastInputSourceType = BroadcastInputSourceType.ANDROID_PLAYBACK_CAPTURE

    override fun currentSnapshot(): BroadcastInputSnapshot =
        BroadcastInputSnapshot.pending("Android playback capture")
}

class TextInputSource : BroadcastInputSource {
    override val type: BroadcastInputSourceType = BroadcastInputSourceType.TEXT

    override fun currentSnapshot(): BroadcastInputSnapshot =
        BroadcastInputSnapshot.pending("Text")
}
