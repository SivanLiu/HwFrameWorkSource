package com.huawei.nb.model.ips;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import com.huawei.odmf.utils.BindUtils;
import java.sql.Blob;

public class IndoorTrack extends AManagedObject {
    public static final Creator<IndoorTrack> CREATOR = new Creator<IndoorTrack>() {
        public IndoorTrack createFromParcel(Parcel in) {
            return new IndoorTrack(in);
        }

        public IndoorTrack[] newArray(int size) {
            return new IndoorTrack[size];
        }
    };
    private String areaCode;
    private Blob dataAdd;
    private Blob dataDel;
    private Blob dataUpdate;
    private Short floorNum;
    private Integer id;
    private Double latitude;
    private Double longitude;
    private String reserved;
    private Long timestamp;
    private Integer type;

    public IndoorTrack(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.areaCode = cursor.getString(2);
        this.type = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.floorNum = cursor.isNull(4) ? null : Short.valueOf(cursor.getShort(4));
        this.longitude = cursor.isNull(5) ? null : Double.valueOf(cursor.getDouble(5));
        this.latitude = cursor.isNull(6) ? null : Double.valueOf(cursor.getDouble(6));
        this.dataAdd = cursor.isNull(7) ? null : new com.huawei.odmf.data.Blob(cursor.getBlob(7));
        this.dataDel = cursor.isNull(8) ? null : new com.huawei.odmf.data.Blob(cursor.getBlob(8));
        this.dataUpdate = cursor.isNull(9) ? null : new com.huawei.odmf.data.Blob(cursor.getBlob(9));
        if (!cursor.isNull(10)) {
            l = Long.valueOf(cursor.getLong(10));
        }
        this.timestamp = l;
        this.reserved = cursor.getString(11);
    }

    public IndoorTrack(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.areaCode = in.readByte() == (byte) 0 ? null : in.readString();
        this.type = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.floorNum = in.readByte() == (byte) 0 ? null : Short.valueOf((short) in.readInt());
        this.longitude = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.latitude = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.dataAdd = in.readByte() == (byte) 0 ? null : (Blob) com.huawei.odmf.data.Blob.CREATOR.createFromParcel(in);
        this.dataDel = in.readByte() == (byte) 0 ? null : (Blob) com.huawei.odmf.data.Blob.CREATOR.createFromParcel(in);
        this.dataUpdate = in.readByte() == (byte) 0 ? null : (Blob) com.huawei.odmf.data.Blob.CREATOR.createFromParcel(in);
        this.timestamp = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.reserved = str;
    }

    private IndoorTrack(Integer id, String areaCode, Integer type, Short floorNum, Double longitude, Double latitude, Blob dataAdd, Blob dataDel, Blob dataUpdate, Long timestamp, String reserved) {
        this.id = id;
        this.areaCode = areaCode;
        this.type = type;
        this.floorNum = floorNum;
        this.longitude = longitude;
        this.latitude = latitude;
        this.dataAdd = dataAdd;
        this.dataDel = dataDel;
        this.dataUpdate = dataUpdate;
        this.timestamp = timestamp;
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

    public String getAreaCode() {
        return this.areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
        setValue();
    }

    public Integer getType() {
        return this.type;
    }

    public void setType(Integer type) {
        this.type = type;
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

    public Blob getDataAdd() {
        return this.dataAdd;
    }

    public void setDataAdd(Blob dataAdd) {
        this.dataAdd = dataAdd;
        setValue();
    }

    public Blob getDataDel() {
        return this.dataDel;
    }

    public void setDataDel(Blob dataDel) {
        this.dataDel = dataDel;
        setValue();
    }

    public Blob getDataUpdate() {
        return this.dataUpdate;
    }

    public void setDataUpdate(Blob dataUpdate) {
        this.dataUpdate = dataUpdate;
        setValue();
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
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
        if (this.areaCode != null) {
            out.writeByte((byte) 1);
            out.writeString(this.areaCode);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.type != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.type.intValue());
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
        if (this.dataAdd != null) {
            out.writeByte((byte) 1);
            if (this.dataAdd instanceof com.huawei.odmf.data.Blob) {
                ((com.huawei.odmf.data.Blob) this.dataAdd).writeToParcel(out, 0);
            } else {
                out.writeByteArray(BindUtils.bindBlob(this.dataAdd));
            }
        } else {
            out.writeByte((byte) 0);
        }
        if (this.dataDel != null) {
            out.writeByte((byte) 1);
            if (this.dataDel instanceof com.huawei.odmf.data.Blob) {
                ((com.huawei.odmf.data.Blob) this.dataDel).writeToParcel(out, 0);
            } else {
                out.writeByteArray(BindUtils.bindBlob(this.dataDel));
            }
        } else {
            out.writeByte((byte) 0);
        }
        if (this.dataUpdate != null) {
            out.writeByte((byte) 1);
            if (this.dataUpdate instanceof com.huawei.odmf.data.Blob) {
                ((com.huawei.odmf.data.Blob) this.dataUpdate).writeToParcel(out, 0);
            } else {
                out.writeByteArray(BindUtils.bindBlob(this.dataUpdate));
            }
        } else {
            out.writeByte((byte) 0);
        }
        if (this.timestamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.timestamp.longValue());
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

    public AEntityHelper<IndoorTrack> getHelper() {
        return IndoorTrackHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.ips.IndoorTrack";
    }

    public String getDatabaseName() {
        return "dsIndoorFingerprint";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("IndoorTrack { id: ").append(this.id);
        sb.append(", areaCode: ").append(this.areaCode);
        sb.append(", type: ").append(this.type);
        sb.append(", floorNum: ").append(this.floorNum);
        sb.append(", longitude: ").append(this.longitude);
        sb.append(", latitude: ").append(this.latitude);
        sb.append(", dataAdd: ").append(this.dataAdd);
        sb.append(", dataDel: ").append(this.dataDel);
        sb.append(", dataUpdate: ").append(this.dataUpdate);
        sb.append(", timestamp: ").append(this.timestamp);
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
