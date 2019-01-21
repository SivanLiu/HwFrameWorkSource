package android.widget;

import android.content.Context;
import android.media.SessionToken2;
import android.media.session.MediaController;
import android.media.update.MediaControlView2Provider;
import android.media.update.ViewGroupHelper;
import android.util.AttributeSet;
import android.view.View;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class MediaControlView2 extends ViewGroupHelper<MediaControlView2Provider> {
    public static final int BUTTON_ASPECT_RATIO = 10;
    public static final int BUTTON_FFWD = 2;
    public static final int BUTTON_FULL_SCREEN = 7;
    public static final int BUTTON_MUTE = 9;
    public static final int BUTTON_NEXT = 4;
    public static final int BUTTON_OVERFLOW = 8;
    public static final int BUTTON_PLAY_PAUSE = 1;
    public static final int BUTTON_PREV = 5;
    public static final int BUTTON_REW = 3;
    public static final int BUTTON_SETTINGS = 11;
    public static final int BUTTON_SUBTITLE = 6;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Button {
    }

    public interface OnFullScreenListener {
        void onFullScreen(View view, boolean z);
    }

    public MediaControlView2(Context context) {
        this(context, null);
    }

    public MediaControlView2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaControlView2(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MediaControlView2(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(new -$$Lambda$MediaControlView2$RI38ILmx2NwSJumbm0C4a0I-utM(attrs, defStyleAttr, defStyleRes), context, attrs, defStyleAttr, defStyleRes);
        ((MediaControlView2Provider) this.mProvider).initialize(attrs, defStyleAttr, defStyleRes);
    }

    public void setMediaSessionToken(SessionToken2 token) {
        ((MediaControlView2Provider) this.mProvider).setMediaSessionToken_impl(token);
    }

    public void setOnFullScreenListener(OnFullScreenListener l) {
        ((MediaControlView2Provider) this.mProvider).setOnFullScreenListener_impl(l);
    }

    public void setController(MediaController controller) {
        ((MediaControlView2Provider) this.mProvider).setController_impl(controller);
    }

    public void setButtonVisibility(int button, int visibility) {
        ((MediaControlView2Provider) this.mProvider).setButtonVisibility_impl(button, visibility);
    }

    public void requestPlayButtonFocus() {
        ((MediaControlView2Provider) this.mProvider).requestPlayButtonFocus_impl();
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        ((MediaControlView2Provider) this.mProvider).onLayout_impl(changed, l, t, r, b);
    }
}
