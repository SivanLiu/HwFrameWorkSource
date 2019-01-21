package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.os.Trace;
import com.android.internal.content.ReferrerIntent;
import java.util.List;
import java.util.Objects;

public class NewIntentItem extends ClientTransactionItem {
    public static final Creator<NewIntentItem> CREATOR = new Creator<NewIntentItem>() {
        public NewIntentItem createFromParcel(Parcel in) {
            return new NewIntentItem(in, null);
        }

        public NewIntentItem[] newArray(int size) {
            return new NewIntentItem[size];
        }
    };
    private List<ReferrerIntent> mIntents;
    private boolean mPause;

    /* synthetic */ NewIntentItem(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public void execute(ClientTransactionHandler client, IBinder token, PendingTransactionActions pendingActions) {
        Trace.traceBegin(64, "activityNewIntent");
        client.handleNewIntent(token, this.mIntents, this.mPause);
        Trace.traceEnd(64);
    }

    private NewIntentItem() {
    }

    public static NewIntentItem obtain(List<ReferrerIntent> intents, boolean pause) {
        NewIntentItem instance = (NewIntentItem) ObjectPool.obtain(NewIntentItem.class);
        if (instance == null) {
            instance = new NewIntentItem();
        }
        instance.mIntents = intents;
        instance.mPause = pause;
        return instance;
    }

    public void recycle() {
        this.mIntents = null;
        this.mPause = false;
        ObjectPool.recycle(this);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBoolean(this.mPause);
        dest.writeTypedList(this.mIntents, flags);
    }

    private NewIntentItem(Parcel in) {
        this.mPause = in.readBoolean();
        this.mIntents = in.createTypedArrayList(ReferrerIntent.CREATOR);
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NewIntentItem other = (NewIntentItem) o;
        if (!(this.mPause == other.mPause && Objects.equals(this.mIntents, other.mIntents))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * 17) + this.mPause)) + this.mIntents.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NewIntentItem{pause=");
        stringBuilder.append(this.mPause);
        stringBuilder.append(",intents=");
        stringBuilder.append(this.mIntents);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
