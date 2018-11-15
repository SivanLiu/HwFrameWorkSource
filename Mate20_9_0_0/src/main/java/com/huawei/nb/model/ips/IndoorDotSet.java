package com.huawei.nb.model.ips;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import com.huawei.odmf.utils.BindUtils;
import java.sql.Blob;

public class IndoorDotSet extends AManagedObject {
    public static final Creator<IndoorDotSet> CREATOR = new Creator<IndoorDotSet>() {
        public IndoorDotSet createFromParcel(Parcel in) {
            return new IndoorDotSet(in);
        }

        public IndoorDotSet[] newArray(int size) {
            return new IndoorDotSet[size];
        }
    };
    private Blob data;
    private Integer dataType;
    private Short floorNum;
    private Integer id;
    private Double latitude;
    private Double longitude;
    private String reserved;
    private String venueId;

    public IndoorDotSet(Cursor cursor) {
        Blob blob = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.venueId = cursor.getString(2);
        this.floorNum = cursor.isNull(3) ? null : Short.valueOf(cursor.getShort(3));
        this.longitude = cursor.isNull(4) ? null : Double.valueOf(cursor.getDouble(4));
        this.latitude = cursor.isNull(5) ? null : Double.valueOf(cursor.getDouble(5));
        this.dataType = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        if (!cursor.isNull(7)) {
            blob = new com.huawei.odmf.data.Blob(cursor.getBlob(7));
        }
        this.data = blob;
        this.reserved = cursor.getString(8);
    }

    public IndoorDotSet(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.venueId = in.readByte() == (byte) 0 ? null : in.readString();
        this.floorNum = in.readByte() == (byte) 0 ? null : Short.valueOf((short) in.readInt());
        this.longitude = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.latitude = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.dataType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.data = in.readByte() == (byte) 0 ? null : (Blob) com.huawei.odmf.data.Blob.CREATOR.createFromParcel(in);
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.reserved = str;
    }

    private IndoorDotSet(Integer id, String venueId, Short floorNum, Double longitude, Double latitude, Integer dataType, Blob data, String reserved) {
        this.id = id;
        this.venueId = venueId;
        this.floorNum = floorNum;
        this.longitude = longitude;
        this.latitude = latitude;
        this.dataType = dataType;
        this.data = data;
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

    public Short getFloorNum() {
        return this.floorNum;
    }

    public void setFloorNum(Short floorNum) {
        this.floorNum = floorNum;
        setValue();
    }

    public Double getLongitude() {
        return this.longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
        setValue();
    }

    public Double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
        setValue();
    }

    public Integer getDataType() {
        return this.dataType;
    }

    public void setDataType(Integer dataType) {
        this.dataType = dataType;
        setValue();
    }

    public Blob getData() {
        return this.data;
    }

    public void setData(Blob data) {
        this.data = data;
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
        if (this.floorNum != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.floorNum.shortValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.longitude != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.longitude.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.latitude != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.latitude.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.dataType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.dataType.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.data != null) {
            out.writeByte((byte) 1);
            if (this.data instanceof com.huawei.odmf.data.Blob) {
                ((com.huawei.odmf.data.Blob) this.data).writeToParcel(out, 0);
            } else {
                out.writeByteArray(BindUtils.bindBlob(this.data));
            }
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

    public AEntityHelper<IndoorDotSet> getHelper() {
        return IndoorDotSetHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.ips.IndoorDotSet";
    }

    public String getDatabaseName() {
        return "dsIndoorFingerprint";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("IndoorDotSet { id: ").append(this.id);
        sb.append(", venueId: ").append(this.venueId);
        sb.append(", floorNum: ").append(this.floorNum);
        sb.append(", longitude: ").append(this.longitude);
        sb.append(", latitude: ").append(this.latitude);
        sb.append(", dataType: ").append(this.dataType);
        sb.append(", data: ").append(this.data);
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
