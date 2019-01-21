package android.media;

import android.content.Context;
import android.media.SubtitleController.Renderer;
import android.os.Handler;

public class SRTRenderer extends Renderer {
    private final Context mContext;
    private final Handler mEventHandler;
    private final boolean mRender;
    private WebVttRenderingWidget mRenderingWidget;

    public SRTRenderer(Context context) {
        this(context, null);
    }

    SRTRenderer(Context mContext, Handler mEventHandler) {
        this.mContext = mContext;
        this.mRender = mEventHandler == null;
        this.mEventHandler = mEventHandler;
    }

    public boolean supports(MediaFormat format) {
        boolean z = false;
        if (!format.containsKey(MediaFormat.KEY_MIME) || !format.getString(MediaFormat.KEY_MIME).equals("application/x-subrip")) {
            return false;
        }
        if (this.mRender == (format.getInteger(MediaFormat.KEY_IS_TIMED_TEXT, 0) == 0)) {
            z = true;
        }
        return z;
    }

    public SubtitleTrack createTrack(MediaFormat format) {
        if (this.mRender && this.mRenderingWidget == null) {
            this.mRenderingWidget = new WebVttRenderingWidget(this.mContext);
        }
        if (this.mRender) {
            return new SRTTrack(this.mRenderingWidget, format);
        }
        return new SRTTrack(this.mEventHandler, format);
    }
}
