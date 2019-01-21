package android.view;

import android.graphics.Rect;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Pools.SynchronizedPool;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;

public class WindowInfo implements Parcelable {
    public static final Creator<WindowInfo> CREATOR = new Creator<WindowInfo>() {
        public WindowInfo createFromParcel(Parcel parcel) {
            WindowInfo window = WindowInfo.obtain();
            window.initFromParcel(parcel);
            return window;
        }

        public WindowInfo[] newArray(int size) {
            return new WindowInfo[size];
        }
    };
    private static final int MAX_POOL_SIZE = 10;
    private static final SynchronizedPool<WindowInfo> sPool = new SynchronizedPool(10);
    public long accessibilityIdOfAnchor = AccessibilityNodeInfo.UNDEFINED_NODE_ID;
    public IBinder activityToken;
    public final Rect boundsInScreen = new Rect();
    public List<IBinder> childTokens;
    public boolean focused;
    public boolean inPictureInPicture;
    public int layer;
    public IBinder parentToken;
    public CharSequence title;
    public IBinder token;
    public int type;

    private WindowInfo() {
    }

    public static WindowInfo obtain() {
        WindowInfo window = (WindowInfo) sPool.acquire();
        if (window == null) {
            return new WindowInfo();
        }
        return window;
    }

    public static WindowInfo obtain(WindowInfo other) {
        WindowInfo window = obtain();
        window.type = other.type;
        window.layer = other.layer;
        window.token = other.token;
        window.parentToken = other.parentToken;
        window.activityToken = other.activityToken;
        window.focused = other.focused;
        window.boundsInScreen.set(other.boundsInScreen);
        window.title = other.title;
        window.accessibilityIdOfAnchor = other.accessibilityIdOfAnchor;
        window.inPictureInPicture = other.inPictureInPicture;
        if (!(other.childTokens == null || other.childTokens.isEmpty())) {
            if (window.childTokens == null) {
                window.childTokens = new ArrayList(other.childTokens);
            } else {
                window.childTokens.addAll(other.childTokens);
            }
        }
        return window;
    }

    public void recycle() {
        clear();
        sPool.release(this);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.type);
        parcel.writeInt(this.layer);
        parcel.writeStrongBinder(this.token);
        parcel.writeStrongBinder(this.parentToken);
        parcel.writeStrongBinder(this.activityToken);
        parcel.writeInt(this.focused);
        this.boundsInScreen.writeToParcel(parcel, flags);
        parcel.writeCharSequence(this.title);
        parcel.writeLong(this.accessibilityIdOfAnchor);
        parcel.writeInt(this.inPictureInPicture);
        if (this.childTokens == null || this.childTokens.isEmpty()) {
            parcel.writeInt(0);
            return;
        }
        parcel.writeInt(1);
        parcel.writeBinderList(this.childTokens);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WindowInfo[");
        builder.append("title=");
        builder.append(this.title);
        builder.append(", type=");
        builder.append(this.type);
        builder.append(", layer=");
        builder.append(this.layer);
        builder.append(", token=");
        builder.append(this.token);
        builder.append(", bounds=");
        builder.append(this.boundsInScreen);
        builder.append(", parent=");
        builder.append(this.parentToken);
        builder.append(", focused=");
        builder.append(this.focused);
        builder.append(", children=");
        builder.append(this.childTokens);
        builder.append(", accessibility anchor=");
        builder.append(this.accessibilityIdOfAnchor);
        builder.append(']');
        return builder.toString();
    }

    private void initFromParcel(Parcel parcel) {
        this.type = parcel.readInt();
        this.layer = parcel.readInt();
        this.token = parcel.readStrongBinder();
        this.parentToken = parcel.readStrongBinder();
        this.activityToken = parcel.readStrongBinder();
        boolean z = false;
        this.focused = parcel.readInt() == 1;
        this.boundsInScreen.readFromParcel(parcel);
        this.title = parcel.readCharSequence();
        this.accessibilityIdOfAnchor = parcel.readLong();
        this.inPictureInPicture = parcel.readInt() == 1;
        if (parcel.readInt() == 1) {
            z = true;
        }
        if (z) {
            if (this.childTokens == null) {
                this.childTokens = new ArrayList();
            }
            parcel.readBinderList(this.childTokens);
        }
    }

    private void clear() {
        this.type = 0;
        this.layer = 0;
        this.token = null;
        this.parentToken = null;
        this.activityToken = null;
        this.focused = false;
        this.boundsInScreen.setEmpty();
        if (this.childTokens != null) {
            this.childTokens.clear();
        }
        this.inPictureInPicture = false;
    }
}
