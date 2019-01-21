package android.media.update;

import android.media.AudioAttributes;
import android.media.DataSourceDesc;
import android.media.MediaItem2;
import android.media.MediaMetadata2;
import android.media.SessionToken2;
import android.media.session.MediaController;
import android.media.session.PlaybackState.CustomAction;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.MediaControlView2;
import android.widget.VideoView2.OnCustomActionListener;
import android.widget.VideoView2.OnFullScreenRequestListener;
import android.widget.VideoView2.OnViewTypeChangedListener;
import com.android.internal.annotations.VisibleForTesting;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public interface VideoView2Provider extends ViewGroupProvider {
    MediaControlView2 getMediaControlView2_impl();

    MediaController getMediaController_impl();

    MediaMetadata2 getMediaMetadata_impl();

    SessionToken2 getMediaSessionToken_impl();

    int getViewType_impl();

    void initialize(AttributeSet attributeSet, int i, int i2);

    boolean isSubtitleEnabled_impl();

    void setAudioAttributes_impl(AudioAttributes audioAttributes);

    void setAudioFocusRequest_impl(int i);

    void setCustomActions_impl(List<CustomAction> list, Executor executor, OnCustomActionListener onCustomActionListener);

    void setDataSource_impl(DataSourceDesc dataSourceDesc);

    void setFullScreenRequestListener_impl(OnFullScreenRequestListener onFullScreenRequestListener);

    void setMediaControlView2_impl(MediaControlView2 mediaControlView2, long j);

    void setMediaItem_impl(MediaItem2 mediaItem2);

    void setMediaMetadata_impl(MediaMetadata2 mediaMetadata2);

    @VisibleForTesting
    void setOnViewTypeChangedListener_impl(OnViewTypeChangedListener onViewTypeChangedListener);

    void setSpeed_impl(float f);

    void setSubtitleEnabled_impl(boolean z);

    void setVideoPath_impl(String str);

    void setVideoUri_impl(Uri uri);

    void setVideoUri_impl(Uri uri, Map<String, String> map);

    void setViewType_impl(int i);
}
