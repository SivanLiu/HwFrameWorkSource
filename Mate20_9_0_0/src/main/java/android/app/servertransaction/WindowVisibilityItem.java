package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.os.Trace;

public class WindowVisibilityItem extends ClientTransactionItem {
    public static final Creator<WindowVisibilityItem> CREATOR = new Creator<WindowVisibilityItem>() {
        public WindowVisibilityItem createFromParcel(Parcel in) {
            return new WindowVisibilityItem(in, null);
        }

        public WindowVisibilityItem[] newArray(int size) {
            return new WindowVisibilityItem[size];
        }
    };
    private boolean mShowWindow;

    /* synthetic */ WindowVisibilityItem(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public void execute(ClientTransactionHandler client, IBinder token, PendingTransactionActions pendingActions) {
        Trace.traceBegin(64, "activityShowWindow");
        client.handleWindowVisibility(token, this.mShowWindow);
        Trace.traceEnd(64);
    }

    private WindowVisibilityItem() {
    }

    public static WindowVisibilityItem obtain(boolean showWindow) {
        WindowVisibilityItem instance = (WindowVisibilityItem) ObjectPool.obtain(WindowVisibilityItem.class);
        if (instance == null) {
            instance = new WindowVisibilityItem();
        }
        instance.mShowWindow = showWindow;
        return instance;
    }

    public void recycle() {
        this.mShowWindow = false;
        ObjectPool.recycle(this);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBoolean(this.mShowWindow);
    }

    private WindowVisibilityItem(Parcel in) {
        this.mShowWindow = in.readBoolean();
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (this.mShowWindow != ((WindowVisibilityItem) o).mShowWindow) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return 17 + (31 * this.mShowWindow);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WindowVisibilityItem{showWindow=");
        stringBuilder.append(this.mShowWindow);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
