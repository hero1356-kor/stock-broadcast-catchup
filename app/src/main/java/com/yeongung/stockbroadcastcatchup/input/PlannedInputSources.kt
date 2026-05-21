package com.yeongung.stockbroadcastcatchup.input

class AndroidPlaybackCaptureSource : BroadcastInputSource {
    override val type: BroadcastInputSourceType = BroadcastInputSourceType.ANDROID_PLAYBACK_CAPTURE

    override fun currentSnapshot(): BroadcastInputSnapshot =
        BroadcastInputSnapshot.pending("Android playback capture")
}
