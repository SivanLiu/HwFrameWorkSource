package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable.Creator;

public class ClientTransactionItemImpl extends ClientTransactionItem {
    public static final Creator<ClientTransactionItemImpl> CREATOR = new Creator<ClientTransactionItemImpl>() {
        public ClientTransactionItemImpl createFromParcel(Parcel in) {
            return new ClientTransactionItemImpl(in, null);
        }

        public ClientTransactionItemImpl[] newArray(int size) {
            return new ClientTransactionItemImpl[size];
        }
    };
    private IClientTransactionItem mFLConfigurationChangeItem;

    /* synthetic */ ClientTransactionItemImpl(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public ClientTransactionItemImpl(IClientTransactionItem flConfigurationChangeItem) {
        this.mFLConfigurationChangeItem = flConfigurationChangeItem;
    }

    public void execute(ClientTransactionHandler client, IBinder token, PendingTransactionActions pendingActions) {
        if (this.mFLConfigurationChangeItem != null) {
            this.mFLConfigurationChangeItem.execute(client, token, pendingActions);
        }
    }

    public void recycle() {
    }

    public void writeToParcel(Parcel dest, int flags) {
    }

    private ClientTransactionItemImpl(Parcel in) {
        this.mFLConfigurationChangeItem = null;
    }

    public boolean equals(Object o) {
        boolean z = false;
        if (!(o instanceof ClientTransactionItemImpl)) {
            return false;
        }
        if (this.mFLConfigurationChangeItem == ((ClientTransactionItemImpl) o).mFLConfigurationChangeItem) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return this.mFLConfigurationChangeItem != null ? this.mFLConfigurationChangeItem.hashCode() : 0;
    }

    public String toString() {
        return "ClientTransactionItemImpl{}";
    }
}
