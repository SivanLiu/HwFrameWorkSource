package huawei.android.widget;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View.BaseSavedState;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import huawei.android.widget.loader.ResLoader;
import huawei.android.widget.loader.ResLoaderUtil;

public class DownLoadWidget extends FrameLayout {
    public static final int STATE_DOWNLOAD = 1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_PAUSE = 2;
    private static final String TAG = "DownLoadWidget";
    private ProgressBar mDownLoadProgress;
    private int mDownloadDrawableId;
    private int mDownloadTextColorId;
    private String mPauseText;
    private TextView mPercentage;
    private int mState;

    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int mProgress;
        int mSaveState;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            if (parcel == null) {
                Log.w(DownLoadWidget.TAG, "SavedState, parcel is null");
                return;
            }
            this.mProgress = parcel.readInt();
            this.mSaveState = parcel.readInt();
        }

        public void writeToParcel(Parcel parcel, int flags) {
            super.writeToParcel(parcel, flags);
            if (parcel == null) {
                Log.w(DownLoadWidget.TAG, "writeToParcel, parcel is null");
                return;
            }
            parcel.writeInt(this.mProgress);
            parcel.writeInt(this.mSaveState);
        }
    }

    public DownLoadWidget(Context context) {
        super(context);
        this.mDownLoadProgress = null;
        this.mPercentage = null;
        this.mState = 0;
        init();
    }

    public DownLoadWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DownLoadWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mDownLoadProgress = null;
        this.mPercentage = null;
        this.mState = 0;
        init();
    }

    private void init() {
        ((LayoutInflater) getContext().getSystemService("layout_inflater")).inflate(ResLoaderUtil.getLayoutId(getContext(), "download_progress"), this, true);
        this.mDownLoadProgress = (ProgressBar) findViewById(ResLoaderUtil.getViewId(getContext(), "downloadProgress"));
        this.mPercentage = (TextView) findViewById(ResLoaderUtil.getViewId(getContext(), "percentage"));
        this.mDownloadDrawableId = ResLoaderUtil.getDrawableId(getContext(), "download_widget_progress_layer");
        this.mDownloadTextColorId = ResLoader.getInstance().getIdentifier(getContext(), ResLoaderUtil.COLOR, "button_text_normal_emui");
        this.mPauseText = "";
    }

    public void setIdleText(String idleText) {
        if (idleText == null) {
            Log.w(TAG, "setIdleText, idleText is null");
        } else if (this.mState != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setIdleText, mState = ");
            stringBuilder.append(this.mState);
            Log.w(str, stringBuilder.toString());
        } else {
            this.mPercentage.setText(idleText);
        }
    }

    public void setPauseText(String pauseText) {
        if (pauseText == null) {
            Log.w(TAG, "setPauseText, pauseText is null");
        } else {
            this.mPauseText = pauseText;
        }
    }

    public int getState() {
        return this.mState;
    }

    public void incrementProgressBy(int progress) {
        if (this.mDownLoadProgress == null) {
            Log.w(TAG, "incrementProgressBy, mDownLoadProgress is null");
            return;
        }
        if (1 != this.mState) {
            this.mState = 1;
            this.mDownLoadProgress.setBackground(null);
            this.mDownLoadProgress.setProgressDrawable(getResources().getDrawable(this.mDownloadDrawableId));
            this.mPercentage.setTextColor(getResources().getColorStateList(this.mDownloadTextColorId, getContext().getTheme()));
        }
        this.mDownLoadProgress.incrementProgressBy(progress);
        setPercentage(this.mDownLoadProgress.getProgress());
    }

    private void setPercentage(int percent) {
        if (this.mPercentage == null) {
            Log.w(TAG, "setPercentage, mPercentage is null");
            return;
        }
        if (2 == this.mState) {
            this.mPercentage.setText(this.mPauseText);
        } else {
            TextView textView = this.mPercentage;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(String.format("%2d", new Object[]{Integer.valueOf(percent)}));
            stringBuilder.append("%");
            textView.setText(stringBuilder.toString());
        }
    }

    public void stop() {
        this.mState = 2;
        setPercentage(this.mDownLoadProgress.getProgress());
    }

    public Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState(super.onSaveInstanceState());
        ss.mProgress = this.mDownLoadProgress.getProgress();
        ss.mSaveState = this.mState;
        return ss;
    }

    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss.getSuperState());
            this.mState = ss.mSaveState;
            if (this.mState == 0 || this.mDownLoadProgress == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onRestoreInstanceState mState = ");
                stringBuilder.append(this.mState);
                stringBuilder.append(" , mDownLoadProgress = ");
                stringBuilder.append(this.mDownLoadProgress);
                Log.w(str, stringBuilder.toString());
                return;
            }
            this.mDownLoadProgress.setProgressDrawable(getResources().getDrawable(33751740));
            this.mDownLoadProgress.setProgress(ss.mProgress);
            setPercentage(ss.mProgress);
            return;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("onRestoreInstanceState, state = ");
        stringBuilder2.append(state);
        Log.w(str2, stringBuilder2.toString());
    }
}
