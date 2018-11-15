package com.android.server.media;

import android.media.AudioPlaybackConfiguration;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaSessionService$za_9dlUSlnaiZw6eCdPVEZq0XLw implements OnAudioPlayerActiveStateChangedListener {
    private final /* synthetic */ MediaSessionService f$0;

    public /* synthetic */ -$$Lambda$MediaSessionService$za_9dlUSlnaiZw6eCdPVEZq0XLw(MediaSessionService mediaSessionService) {
        this.f$0 = mediaSessionService;
    }

    public final void onAudioPlayerActiveStateChanged(AudioPlaybackConfiguration audioPlaybackConfiguration, boolean z) {
        MediaSessionService.lambda$onStart$0(this.f$0, audioPlaybackConfiguration, z);
    }
}
