package com.huawei.nb.model.policy;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class HwPolicyServiceAbility extends AManagedObject {
    public static final Creator<HwPolicyServiceAbility> CREATOR = new Creator<HwPolicyServiceAbility>() {
        public HwPolicyServiceAbility createFromParcel(Parcel in) {
            return new HwPolicyServiceAbility(in);
        }

        public HwPolicyServiceAbility[] newArray(int size) {
            return new HwPolicyServiceAbility[size];
        }
    };
    private Long id;
    private String name;
    private String reserve;
    private String type;
    private Long versionCode;
    private String versionName;

    public HwPolicyServiceAbility(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.name = cursor.getString(2);
        this.type = cursor.getString(3);
        if (!cursor.isNull(4)) {
            l = Long.valueOf(cursor.getLong(4));
        }
        this.versionCode = l;
        this.versionName = cursor.getString(5);
        this.reserve = cursor.getString(6);
    }

    public HwPolicyServiceAbility(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.name = in.readByte() == (byte) 0 ? null : in.readString();
        this.type = in.readByte() == (byte) 0 ? null : in.readString();
        this.versionCode = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.versionName = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.reserve = str;
    }

    private HwPolicyServiceAbility(Long id, String name, String type, Long versionCode, String versionName, String reserve) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.reserve = reserve;
    }

    public int describeContents() {
        return 0;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
        setValue();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        setValue();
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
        setValue();
    }

    public Long getVersionCode() {
        return this.versionCode;
    }

    public void setVersionCode(Long versionCode) {
        this.versionCode = versionCode;
        setValue();
    }

    public String getVersionName() {
        return this.versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
        setValue();
    }

    public String getReserve() {
        return this.reserve;
    }

    public void setReserve(String reserve) {
        this.reserve = reserve;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.id.longValue());
        } else {
            out.writeByte((byte) 0);
            out.writeLong(1);
        }
        if (this.name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.name);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.type != null) {
            out.writeByte((byte) 1);
            out.writeString(this.type);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.versionCode != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.versionCode.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.versionName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.versionName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserve != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserve);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<HwPolicyServiceAbility> getHelper() {
        return HwPolicyServiceAbilityHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.policy.HwPolicyServiceAbility";
    }

    public String getDatabaseName() {
        return "dsServiceMetaData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("HwPolicyServiceAbility { id: ").append(this.id);
        sb.append(", name: ").append(this.name);
        sb.append(", type: ").append(this.type);
        sb.append(", versionCode: ").append(this.versionCode);
        sb.append(", versionName: ").append(this.versionName);
        sb.append(", reserve: ").append(this.reserve);
        sb.append(" }");
        return sb.toString();
    }

    public boolean equals(Object o) {
        return super.equals(o);
    }

    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.11";
    }

    public int getDatabaseVersionCode() {
        return 11;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
