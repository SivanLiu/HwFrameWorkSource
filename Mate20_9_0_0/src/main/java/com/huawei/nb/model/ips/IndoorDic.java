package com.huawei.nb.model.ips;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import com.huawei.odmf.utils.BindUtils;
import java.sql.Blob;

public class IndoorDic extends AManagedObject {
    public static final Creator<IndoorDic> CREATOR = new Creator<IndoorDic>() {
        public IndoorDic createFromParcel(Parcel in) {
            return new IndoorDic(in);
        }

        public IndoorDic[] newArray(int size) {
            return new IndoorDic[size];
        }
    };
    private Integer id;
    private Blob key;
    private String reserved;
    private Integer type;
    private Short value;
    private String venueId;

    public IndoorDic(Cursor cursor) {
        Short sh = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.venueId = cursor.getString(2);
        this.type = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.key = cursor.isNull(4) ? null : new com.huawei.odmf.data.Blob(cursor.getBlob(4));
        if (!cursor.isNull(5)) {
            sh = Short.valueOf(cursor.getShort(5));
        }
        this.value = sh;
        this.reserved = cursor.getString(6);
    }

    public IndoorDic(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.venueId = in.readByte() == (byte) 0 ? null : in.readString();
        this.type = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.key = in.readByte() == (byte) 0 ? null : (Blob) com.huawei.odmf.data.Blob.CREATOR.createFromParcel(in);
        this.value = in.readByte() == (byte) 0 ? null : Short.valueOf((short) in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.reserved = str;
    }

    private IndoorDic(Integer id, String venueId, Integer type, Blob key, Short value, String reserved) {
        this.id = id;
        this.venueId = venueId;
        this.type = type;
        this.key = key;
        this.value = value;
        this.reserved = reserved;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
        setValue();
    }

    public String getVenueId() {
        return this.venueId;
    }

    public void setVenueId(String venueId) {
        this.venueId = venueId;
        setValue();
    }

    public Integer getType() {
        return this.type;
    }

    public void setType(Integer type) {
        this.type = type;
        setValue();
    }

    public Blob getKey() {
        return this.key;
    }

    public void setKey(Blob key) {
        this.key = key;
        setValue();
    }

    public Short getValue() {
        return this.value;
    }

    public void setValue(Short value) {
        this.value = value;
        setValue();
    }

    public String getReserved() {
        return this.reserved;
    }

    public void setReserved(String reserved) {
        this.reserved = reserved;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.id.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.venueId != null) {
            out.writeByte((byte) 1);
            out.writeString(this.venueId);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.type != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.type.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.key != null) {
            out.writeByte((byte) 1);
            if (this.key instanceof com.huawei.odmf.data.Blob) {
                ((com.huawei.odmf.data.Blob) this.key).writeToParcel(out, 0);
            } else {
                out.writeByteArray(BindUtils.bindBlob(this.key));
            }
        } else {
            out.writeByte((byte) 0);
        }
        if (this.value != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.value.shortValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<IndoorDic> getHelper() {
        return IndoorDicHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.ips.IndoorDic";
    }

    public String getDatabaseName() {
        return "dsIndoorFingerprint";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("IndoorDic { id: ").append(this.id);
        sb.append(", venueId: ").append(this.venueId);
        sb.append(", type: ").append(this.type);
        sb.append(", key: ").append(this.key);
        sb.append(", value: ").append(this.value);
        sb.append(", reserved: ").append(this.reserved);
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
        return "0.0.4";
    }

    public int getDatabaseVersionCode() {
        return 4;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
