package android.media.session;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.SystemClock;
import android.text.TextUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public final class PlaybackState implements Parcelable {
    public static final long ACTION_FAST_FORWARD = 64;
    public static final long ACTION_PAUSE = 2;
    public static final long ACTION_PLAY = 4;
    public static final long ACTION_PLAY_FROM_MEDIA_ID = 1024;
    public static final long ACTION_PLAY_FROM_SEARCH = 2048;
    public static final long ACTION_PLAY_FROM_URI = 8192;
    public static final long ACTION_PLAY_PAUSE = 512;
    public static final long ACTION_PREPARE = 16384;
    public static final long ACTION_PREPARE_FROM_MEDIA_ID = 32768;
    public static final long ACTION_PREPARE_FROM_SEARCH = 65536;
    public static final long ACTION_PREPARE_FROM_URI = 131072;
    public static final long ACTION_REWIND = 8;
    public static final long ACTION_SEEK_TO = 256;
    public static final long ACTION_SET_RATING = 128;
    public static final long ACTION_SKIP_TO_NEXT = 32;
    public static final long ACTION_SKIP_TO_PREVIOUS = 16;
    public static final long ACTION_SKIP_TO_QUEUE_ITEM = 4096;
    public static final long ACTION_STOP = 1;
    public static final Creator<PlaybackState> CREATOR = new Creator<PlaybackState>() {
        public PlaybackState createFromParcel(Parcel in) {
            return new PlaybackState(in, null);
        }

        public PlaybackState[] newArray(int size) {
            return new PlaybackState[size];
        }
    };
    public static final long PLAYBACK_POSITION_UNKNOWN = -1;
    public static final int STATE_BUFFERING = 6;
    public static final int STATE_CONNECTING = 8;
    public static final int STATE_ERROR = 7;
    public static final int STATE_FAST_FORWARDING = 4;
    public static final int STATE_NONE = 0;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_REWINDING = 5;
    public static final int STATE_SKIPPING_TO_NEXT = 10;
    public static final int STATE_SKIPPING_TO_PREVIOUS = 9;
    public static final int STATE_SKIPPING_TO_QUEUE_ITEM = 11;
    public static final int STATE_STOPPED = 1;
    private static final String TAG = "PlaybackState";
    private final long mActions;
    private final long mActiveItemId;
    private final long mBufferedPosition;
    private List<CustomAction> mCustomActions;
    private final CharSequence mErrorMessage;
    private final Bundle mExtras;
    private final long mPosition;
    private final float mSpeed;
    private final int mState;
    private final long mUpdateTime;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Actions {
    }

    public static final class Builder {
        private long mActions;
        private long mActiveItemId = -1;
        private long mBufferedPosition;
        private final List<CustomAction> mCustomActions = new ArrayList();
        private CharSequence mErrorMessage;
        private Bundle mExtras;
        private long mPosition;
        private float mSpeed;
        private int mState;
        private long mUpdateTime;

        public Builder(PlaybackState from) {
            if (from != null) {
                this.mState = from.mState;
                this.mPosition = from.mPosition;
                this.mBufferedPosition = from.mBufferedPosition;
                this.mSpeed = from.mSpeed;
                this.mActions = from.mActions;
                if (from.mCustomActions != null) {
                    this.mCustomActions.addAll(from.mCustomActions);
                }
                this.mErrorMessage = from.mErrorMessage;
                this.mUpdateTime = from.mUpdateTime;
                this.mActiveItemId = from.mActiveItemId;
                this.mExtras = from.mExtras;
            }
        }

        public Builder setState(int state, long position, float playbackSpeed, long updateTime) {
            this.mState = state;
            this.mPosition = position;
            this.mUpdateTime = updateTime;
            this.mSpeed = playbackSpeed;
            return this;
        }

        public Builder setState(int state, long position, float playbackSpeed) {
            return setState(state, position, playbackSpeed, SystemClock.elapsedRealtime());
        }

        public Builder setActions(long actions) {
            this.mActions = actions;
            return this;
        }

        public Builder addCustomAction(String action, String name, int icon) {
            return addCustomAction(new CustomAction(action, name, icon, null, null));
        }

        public Builder addCustomAction(CustomAction customAction) {
            if (customAction != null) {
                this.mCustomActions.add(customAction);
                return this;
            }
            throw new IllegalArgumentException("You may not add a null CustomAction to PlaybackState.");
        }

        public Builder setBufferedPosition(long bufferedPosition) {
            this.mBufferedPosition = bufferedPosition;
            return this;
        }

        public Builder setActiveQueueItemId(long id) {
            this.mActiveItemId = id;
            return this;
        }

        public Builder setErrorMessage(CharSequence error) {
            this.mErrorMessage = error;
            return this;
        }

        public Builder setExtras(Bundle extras) {
            this.mExtras = extras;
            return this;
        }

        public PlaybackState build() {
            return new PlaybackState(this.mState, this.mPosition, this.mUpdateTime, this.mSpeed, this.mBufferedPosition, this.mActions, this.mCustomActions, this.mActiveItemId, this.mErrorMessage, this.mExtras, null);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    public static final class CustomAction implements Parcelable {
        public static final Creator<CustomAction> CREATOR = new Creator<CustomAction>() {
            public CustomAction createFromParcel(Parcel p) {
                return new CustomAction(p, null);
            }

            public CustomAction[] newArray(int size) {
                return new CustomAction[size];
            }
        };
        private final String mAction;
        private final Bundle mExtras;
        private final int mIcon;
        private final CharSequence mName;

        public static final class Builder {
            private final String mAction;
            private Bundle mExtras;
            private final int mIcon;
            private final CharSequence mName;

            public Builder(String action, CharSequence name, int icon) {
                if (TextUtils.isEmpty(action)) {
                    throw new IllegalArgumentException("You must specify an action to build a CustomAction.");
                } else if (TextUtils.isEmpty(name)) {
                    throw new IllegalArgumentException("You must specify a name to build a CustomAction.");
                } else if (icon != 0) {
                    this.mAction = action;
                    this.mName = name;
                    this.mIcon = icon;
                } else {
                    throw new IllegalArgumentException("You must specify an icon resource id to build a CustomAction.");
                }
            }

            public Builder setExtras(Bundle extras) {
                this.mExtras = extras;
                return this;
            }

            public CustomAction build() {
                return new CustomAction(this.mAction, this.mName, this.mIcon, this.mExtras, null);
            }
        }

        /* synthetic */ CustomAction(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        /* synthetic */ CustomAction(String x0, CharSequence x1, int x2, Bundle x3, AnonymousClass1 x4) {
            this(x0, x1, x2, x3);
        }

        private CustomAction(String action, CharSequence name, int icon, Bundle extras) {
            this.mAction = action;
            this.mName = name;
            this.mIcon = icon;
            this.mExtras = extras;
        }

        private CustomAction(Parcel in) {
            this.mAction = in.readString();
            this.mName = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            this.mIcon = in.readInt();
            this.mExtras = in.readBundle();
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.mAction);
            TextUtils.writeToParcel(this.mName, dest, flags);
            dest.writeInt(this.mIcon);
            dest.writeBundle(this.mExtras);
        }

        public int describeContents() {
            return 0;
        }

        public String getAction() {
            return this.mAction;
        }

        public CharSequence getName() {
            return this.mName;
        }

        public int getIcon() {
            return this.mIcon;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Action:mName='");
            stringBuilder.append(this.mName);
            stringBuilder.append(", mIcon=");
            stringBuilder.append(this.mIcon);
            stringBuilder.append(", mExtras=");
            stringBuilder.append(this.mExtras);
            return stringBuilder.toString();
        }
    }

    /* synthetic */ PlaybackState(int x0, long x1, long x2, float x3, long x4, long x5, List x6, long x7, CharSequence x8, Bundle x9, AnonymousClass1 x10) {
        this(x0, x1, x2, x3, x4, x5, x6, x7, x8, x9);
    }

    /* synthetic */ PlaybackState(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    private PlaybackState(int state, long position, long updateTime, float speed, long bufferedPosition, long transportControls, List<CustomAction> customActions, long activeItemId, CharSequence error, Bundle extras) {
        this.mState = state;
        this.mPosition = position;
        this.mSpeed = speed;
        this.mUpdateTime = updateTime;
        this.mBufferedPosition = bufferedPosition;
        this.mActions = transportControls;
        this.mCustomActions = new ArrayList(customActions);
        this.mActiveItemId = activeItemId;
        this.mErrorMessage = error;
        this.mExtras = extras;
    }

    private PlaybackState(Parcel in) {
        this.mState = in.readInt();
        this.mPosition = in.readLong();
        this.mSpeed = in.readFloat();
        this.mUpdateTime = in.readLong();
        this.mBufferedPosition = in.readLong();
        this.mActions = in.readLong();
        this.mCustomActions = in.createTypedArrayList(CustomAction.CREATOR);
        this.mActiveItemId = in.readLong();
        this.mErrorMessage = in.readCharSequence();
        this.mExtras = in.readBundle();
    }

    public String toString() {
        StringBuilder bob = new StringBuilder("PlaybackState {");
        bob.append("state=");
        bob.append(this.mState);
        bob.append(", position=");
        bob.append(this.mPosition);
        bob.append(", buffered position=");
        bob.append(this.mBufferedPosition);
        bob.append(", speed=");
        bob.append(this.mSpeed);
        bob.append(", updated=");
        bob.append(this.mUpdateTime);
        bob.append(", actions=");
        bob.append(this.mActions);
        bob.append(", custom actions=");
        bob.append(this.mCustomActions);
        bob.append(", active item id=");
        bob.append(this.mActiveItemId);
        bob.append(", error=");
        bob.append(this.mErrorMessage);
        bob.append("}");
        return bob.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mState);
        dest.writeLong(this.mPosition);
        dest.writeFloat(this.mSpeed);
        dest.writeLong(this.mUpdateTime);
        dest.writeLong(this.mBufferedPosition);
        dest.writeLong(this.mActions);
        dest.writeTypedList(this.mCustomActions);
        dest.writeLong(this.mActiveItemId);
        dest.writeCharSequence(this.mErrorMessage);
        dest.writeBundle(this.mExtras);
    }

    public int getState() {
        return this.mState;
    }

    public long getPosition() {
        return this.mPosition;
    }

    public long getBufferedPosition() {
        return this.mBufferedPosition;
    }

    public float getPlaybackSpeed() {
        return this.mSpeed;
    }

    public long getActions() {
        return this.mActions;
    }

    public List<CustomAction> getCustomActions() {
        return this.mCustomActions;
    }

    public CharSequence getErrorMessage() {
        return this.mErrorMessage;
    }

    public long getLastPositionUpdateTime() {
        return this.mUpdateTime;
    }

    public long getActiveQueueItemId() {
        return this.mActiveItemId;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    public static int getStateFromRccState(int rccState) {
        switch (rccState) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 10;
            case 7:
                return 9;
            case 8:
                return 6;
            case 9:
                return 7;
            default:
                return -1;
        }
    }

    public static int getRccStateFromState(int state) {
        switch (state) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 8;
            case 7:
                return 9;
            case 9:
                return 7;
            case 10:
                return 6;
            default:
                return -1;
        }
    }

    public static long getActionsFromRccControlFlags(int rccFlags) {
        long actions = 0;
        for (long flag = 1; flag <= ((long) rccFlags); flag <<= 1) {
            if ((((long) rccFlags) & flag) != 0) {
                actions |= getActionForRccFlag((int) flag);
            }
        }
        return actions;
    }

    public static int getRccControlFlagsFromActions(long actions) {
        int rccFlags = 0;
        long action = 1;
        while (action <= actions && action < 2147483647L) {
            if ((action & actions) != 0) {
                rccFlags |= getRccFlagForAction(action);
            }
            action <<= 1;
        }
        return rccFlags;
    }

    private static long getActionForRccFlag(int flag) {
        if (flag == 4) {
            return 4;
        }
        if (flag == 8) {
            return 512;
        }
        if (flag == 16) {
            return 2;
        }
        if (flag == 32) {
            return 1;
        }
        if (flag == 64) {
            return 64;
        }
        if (flag == 128) {
            return 32;
        }
        if (flag == 256) {
            return 256;
        }
        if (flag == 512) {
            return 128;
        }
        switch (flag) {
            case 1:
                return 16;
            case 2:
                return 8;
            default:
                return 0;
        }
    }

    private static int getRccFlagForAction(long action) {
        int testAction = action < 2147483647L ? (int) action : 0;
        if (testAction == 4) {
            return 4;
        }
        if (testAction == 8) {
            return 2;
        }
        if (testAction == 16) {
            return 1;
        }
        if (testAction == 32) {
            return 128;
        }
        if (testAction == 64) {
            return 64;
        }
        if (testAction == 128) {
            return 512;
        }
        if (testAction == 256) {
            return 256;
        }
        if (testAction == 512) {
            return 8;
        }
        switch (testAction) {
            case 1:
                return 32;
            case 2:
                return 16;
            default:
                return 0;
        }
    }
}
