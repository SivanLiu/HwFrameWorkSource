package android.content;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.TextUtils;

public class SyncAdapterType implements Parcelable {
    public static final Creator<SyncAdapterType> CREATOR = new Creator<SyncAdapterType>() {
        public SyncAdapterType createFromParcel(Parcel source) {
            return new SyncAdapterType(source);
        }

        public SyncAdapterType[] newArray(int size) {
            return new SyncAdapterType[size];
        }
    };
    public final String accountType;
    private final boolean allowParallelSyncs;
    public final String authority;
    private final boolean isAlwaysSyncable;
    public final boolean isKey;
    private final String packageName;
    private final String settingsActivity;
    private final boolean supportsUploading;
    private final boolean userVisible;

    public SyncAdapterType(String authority, String accountType, boolean userVisible, boolean supportsUploading) {
        StringBuilder stringBuilder;
        if (TextUtils.isEmpty(authority)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("the authority must not be empty: ");
            stringBuilder.append(authority);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (TextUtils.isEmpty(accountType)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("the accountType must not be empty: ");
            stringBuilder.append(accountType);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            this.authority = authority;
            this.accountType = accountType;
            this.userVisible = userVisible;
            this.supportsUploading = supportsUploading;
            this.isAlwaysSyncable = false;
            this.allowParallelSyncs = false;
            this.settingsActivity = null;
            this.isKey = false;
            this.packageName = null;
        }
    }

    public SyncAdapterType(String authority, String accountType, boolean userVisible, boolean supportsUploading, boolean isAlwaysSyncable, boolean allowParallelSyncs, String settingsActivity, String packageName) {
        StringBuilder stringBuilder;
        if (TextUtils.isEmpty(authority)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("the authority must not be empty: ");
            stringBuilder.append(authority);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (TextUtils.isEmpty(accountType)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("the accountType must not be empty: ");
            stringBuilder.append(accountType);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            this.authority = authority;
            this.accountType = accountType;
            this.userVisible = userVisible;
            this.supportsUploading = supportsUploading;
            this.isAlwaysSyncable = isAlwaysSyncable;
            this.allowParallelSyncs = allowParallelSyncs;
            this.settingsActivity = settingsActivity;
            this.isKey = false;
            this.packageName = packageName;
        }
    }

    private SyncAdapterType(String authority, String accountType) {
        StringBuilder stringBuilder;
        if (TextUtils.isEmpty(authority)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("the authority must not be empty: ");
            stringBuilder.append(authority);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (TextUtils.isEmpty(accountType)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("the accountType must not be empty: ");
            stringBuilder.append(accountType);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            this.authority = authority;
            this.accountType = accountType;
            this.userVisible = true;
            this.supportsUploading = true;
            this.isAlwaysSyncable = false;
            this.allowParallelSyncs = false;
            this.settingsActivity = null;
            this.isKey = true;
            this.packageName = null;
        }
    }

    public boolean supportsUploading() {
        if (!this.isKey) {
            return this.supportsUploading;
        }
        throw new IllegalStateException("this method is not allowed to be called when this is a key");
    }

    public boolean isUserVisible() {
        if (!this.isKey) {
            return this.userVisible;
        }
        throw new IllegalStateException("this method is not allowed to be called when this is a key");
    }

    public boolean allowParallelSyncs() {
        if (!this.isKey) {
            return this.allowParallelSyncs;
        }
        throw new IllegalStateException("this method is not allowed to be called when this is a key");
    }

    public boolean isAlwaysSyncable() {
        if (!this.isKey) {
            return this.isAlwaysSyncable;
        }
        throw new IllegalStateException("this method is not allowed to be called when this is a key");
    }

    public String getSettingsActivity() {
        if (!this.isKey) {
            return this.settingsActivity;
        }
        throw new IllegalStateException("this method is not allowed to be called when this is a key");
    }

    public String getPackageName() {
        return this.packageName;
    }

    public static SyncAdapterType newKey(String authority, String accountType) {
        return new SyncAdapterType(authority, accountType);
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (o == this) {
            return true;
        }
        if (!(o instanceof SyncAdapterType)) {
            return false;
        }
        SyncAdapterType other = (SyncAdapterType) o;
        if (!(this.authority.equals(other.authority) && this.accountType.equals(other.accountType))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * 17) + this.authority.hashCode())) + this.accountType.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder;
        if (this.isKey) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("SyncAdapterType Key {name=");
            stringBuilder.append(this.authority);
            stringBuilder.append(", type=");
            stringBuilder.append(this.accountType);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("SyncAdapterType {name=");
        stringBuilder.append(this.authority);
        stringBuilder.append(", type=");
        stringBuilder.append(this.accountType);
        stringBuilder.append(", userVisible=");
        stringBuilder.append(this.userVisible);
        stringBuilder.append(", supportsUploading=");
        stringBuilder.append(this.supportsUploading);
        stringBuilder.append(", isAlwaysSyncable=");
        stringBuilder.append(this.isAlwaysSyncable);
        stringBuilder.append(", allowParallelSyncs=");
        stringBuilder.append(this.allowParallelSyncs);
        stringBuilder.append(", settingsActivity=");
        stringBuilder.append(this.settingsActivity);
        stringBuilder.append(", packageName=");
        stringBuilder.append(this.packageName);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        if (this.isKey) {
            throw new IllegalStateException("keys aren't parcelable");
        }
        dest.writeString(this.authority);
        dest.writeString(this.accountType);
        dest.writeInt(this.userVisible);
        dest.writeInt(this.supportsUploading);
        dest.writeInt(this.isAlwaysSyncable);
        dest.writeInt(this.allowParallelSyncs);
        dest.writeString(this.settingsActivity);
        dest.writeString(this.packageName);
    }

    public SyncAdapterType(Parcel source) {
        this(source.readString(), source.readString(), source.readInt() != 0, source.readInt() != 0, source.readInt() != 0, source.readInt() != 0, source.readString(), source.readString());
    }
}
