package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import java.util.Objects;

public class ConfigurationChangeItem extends ClientTransactionItem {
    public static final Creator<ConfigurationChangeItem> CREATOR = new Creator<ConfigurationChangeItem>() {
        public ConfigurationChangeItem createFromParcel(Parcel in) {
            return new ConfigurationChangeItem(in, null);
        }

        public ConfigurationChangeItem[] newArray(int size) {
            return new ConfigurationChangeItem[size];
        }
    };
    private Configuration mConfiguration;

    /* synthetic */ ConfigurationChangeItem(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public void preExecute(ClientTransactionHandler client, IBinder token) {
        client.updatePendingConfiguration(this.mConfiguration);
    }

    public void execute(ClientTransactionHandler client, IBinder token, PendingTransactionActions pendingActions) {
        client.handleConfigurationChanged(this.mConfiguration);
    }

    private ConfigurationChangeItem() {
    }

    public static ConfigurationChangeItem obtain(Configuration config) {
        ConfigurationChangeItem instance = (ConfigurationChangeItem) ObjectPool.obtain(ConfigurationChangeItem.class);
        if (instance == null) {
            instance = new ConfigurationChangeItem();
        }
        instance.mConfiguration = config;
        return instance;
    }

    public void recycle() {
        this.mConfiguration = null;
        ObjectPool.recycle(this);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedObject(this.mConfiguration, flags);
    }

    private ConfigurationChangeItem(Parcel in) {
        this.mConfiguration = (Configuration) in.readTypedObject(Configuration.CREATOR);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equals(this.mConfiguration, ((ConfigurationChangeItem) o).mConfiguration);
    }

    public int hashCode() {
        return this.mConfiguration.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ConfigurationChangeItem{config=");
        stringBuilder.append(this.mConfiguration);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
