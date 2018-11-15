package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class DSExpress extends AManagedObject {
    public static final Creator<DSExpress> CREATOR = new Creator<DSExpress>() {
        public DSExpress createFromParcel(Parcel in) {
            return new DSExpress(in);
        }

        public DSExpress[] newArray(int size) {
            return new DSExpress[size];
        }
    };
    private String appName;
    private String appPackage;
    private String cabinetCompany;
    private String cabinetLocation;
    private String code;
    private String companyCode;
    private String courierName;
    private String courierPhone;
    private Long createTime = Long.valueOf(0);
    private String dataSource;
    private String detail;
    private String expand;
    private String expressCompany;
    private Integer expressFlow = Integer.valueOf(-1);
    private String expressNumber;
    private String extras;
    private Integer id;
    private Long lastUpdateTime = Long.valueOf(0);
    private String latitude;
    private String longitude;
    private Integer mState = Integer.valueOf(-1);
    private Long newestTime = Long.valueOf(0);
    private Integer oldState = Integer.valueOf(-1);
    private String reserved0;
    private String reserved1;
    private String reserved2;
    private String reserved3;
    private String reserved4;
    private String reserved5;
    private Long sendTime = Long.valueOf(0);
    private String signPerson;
    private Long signTime = Long.valueOf(0);
    private Integer source = Integer.valueOf(0);
    private Integer subWithImei = Integer.valueOf(1);
    private Integer subscribeState = Integer.valueOf(0);
    private Long updateTime = Long.valueOf(0);

    public DSExpress(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.expressNumber = cursor.getString(2);
        this.expressCompany = cursor.getString(3);
        this.companyCode = cursor.getString(4);
        this.cabinetCompany = cursor.getString(5);
        this.mState = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        this.oldState = cursor.isNull(7) ? null : Integer.valueOf(cursor.getInt(7));
        this.detail = cursor.getString(8);
        this.updateTime = cursor.isNull(9) ? null : Long.valueOf(cursor.getLong(9));
        this.lastUpdateTime = cursor.isNull(10) ? null : Long.valueOf(cursor.getLong(10));
        this.newestTime = cursor.isNull(11) ? null : Long.valueOf(cursor.getLong(11));
        this.code = cursor.getString(12);
        this.cabinetLocation = cursor.getString(13);
        this.latitude = cursor.getString(14);
        this.longitude = cursor.getString(15);
        this.appName = cursor.getString(16);
        this.appPackage = cursor.getString(17);
        this.source = cursor.isNull(18) ? null : Integer.valueOf(cursor.getInt(18));
        this.dataSource = cursor.getString(19);
        this.courierName = cursor.getString(20);
        this.courierPhone = cursor.getString(21);
        this.subscribeState = cursor.isNull(22) ? null : Integer.valueOf(cursor.getInt(22));
        this.sendTime = cursor.isNull(23) ? null : Long.valueOf(cursor.getLong(23));
        this.signTime = cursor.isNull(24) ? null : Long.valueOf(cursor.getLong(24));
        this.signPerson = cursor.getString(25);
        this.expressFlow = cursor.isNull(26) ? null : Integer.valueOf(cursor.getInt(26));
        this.extras = cursor.getString(27);
        this.createTime = cursor.isNull(28) ? null : Long.valueOf(cursor.getLong(28));
        this.expand = cursor.getString(29);
        if (!cursor.isNull(30)) {
            num = Integer.valueOf(cursor.getInt(30));
        }
        this.subWithImei = num;
        this.reserved0 = cursor.getString(31);
        this.reserved1 = cursor.getString(32);
        this.reserved2 = cursor.getString(33);
        this.reserved3 = cursor.getString(34);
        this.reserved4 = cursor.getString(35);
        this.reserved5 = cursor.getString(36);
    }

    public DSExpress(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.expressNumber = in.readByte() == (byte) 0 ? null : in.readString();
        this.expressCompany = in.readByte() == (byte) 0 ? null : in.readString();
        this.companyCode = in.readByte() == (byte) 0 ? null : in.readString();
        this.cabinetCompany = in.readByte() == (byte) 0 ? null : in.readString();
        this.mState = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.oldState = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.detail = in.readByte() == (byte) 0 ? null : in.readString();
        this.updateTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.lastUpdateTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.newestTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.code = in.readByte() == (byte) 0 ? null : in.readString();
        this.cabinetLocation = in.readByte() == (byte) 0 ? null : in.readString();
        this.latitude = in.readByte() == (byte) 0 ? null : in.readString();
        this.longitude = in.readByte() == (byte) 0 ? null : in.readString();
        this.appName = in.readByte() == (byte) 0 ? null : in.readString();
        this.appPackage = in.readByte() == (byte) 0 ? null : in.readString();
        this.source = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.dataSource = in.readByte() == (byte) 0 ? null : in.readString();
        this.courierName = in.readByte() == (byte) 0 ? null : in.readString();
        this.courierPhone = in.readByte() == (byte) 0 ? null : in.readString();
        this.subscribeState = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.sendTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.signTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.signPerson = in.readByte() == (byte) 0 ? null : in.readString();
        this.expressFlow = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.extras = in.readByte() == (byte) 0 ? null : in.readString();
        this.createTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.expand = in.readByte() == (byte) 0 ? null : in.readString();
        this.subWithImei = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.reserved0 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved1 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved2 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved3 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved4 = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.reserved5 = str;
    }

    private DSExpress(Integer id, String expressNumber, String expressCompany, String companyCode, String cabinetCompany, Integer mState, Integer oldState, String detail, Long updateTime, Long lastUpdateTime, Long newestTime, String code, String cabinetLocation, String latitude, String longitude, String appName, String appPackage, Integer source, String dataSource, String courierName, String courierPhone, Integer subscribeState, Long sendTime, Long signTime, String signPerson, Integer expressFlow, String extras, Long createTime, String expand, Integer subWithImei, String reserved0, String reserved1, String reserved2, String reserved3, String reserved4, String reserved5) {
        this.id = id;
        this.expressNumber = expressNumber;
        this.expressCompany = expressCompany;
        this.companyCode = companyCode;
        this.cabinetCompany = cabinetCompany;
        this.mState = mState;
        this.oldState = oldState;
        this.detail = detail;
        this.updateTime = updateTime;
        this.lastUpdateTime = lastUpdateTime;
        this.newestTime = newestTime;
        this.code = code;
        this.cabinetLocation = cabinetLocation;
        this.latitude = latitude;
        this.longitude = longitude;
        this.appName = appName;
        this.appPackage = appPackage;
        this.source = source;
        this.dataSource = dataSource;
        this.courierName = courierName;
        this.courierPhone = courierPhone;
        this.subscribeState = subscribeState;
        this.sendTime = sendTime;
        this.signTime = signTime;
        this.signPerson = signPerson;
        this.expressFlow = expressFlow;
        this.extras = extras;
        this.createTime = createTime;
        this.expand = expand;
        this.subWithImei = subWithImei;
        this.reserved0 = reserved0;
        this.reserved1 = reserved1;
        this.reserved2 = reserved2;
        this.reserved3 = reserved3;
        this.reserved4 = reserved4;
        this.reserved5 = reserved5;
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

    public String getExpressNumber() {
        return this.expressNumber;
    }

    public void setExpressNumber(String expressNumber) {
        this.expressNumber = expressNumber;
        setValue();
    }

    public String getExpressCompany() {
        return this.expressCompany;
    }

    public void setExpressCompany(String expressCompany) {
        this.expressCompany = expressCompany;
        setValue();
    }

    public String getCompanyCode() {
        return this.companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
        setValue();
    }

    public String getCabinetCompany() {
        return this.cabinetCompany;
    }

    public void setCabinetCompany(String cabinetCompany) {
        this.cabinetCompany = cabinetCompany;
        setValue();
    }

    public Integer getMState() {
        return this.mState;
    }

    public void setMState(Integer mState) {
        this.mState = mState;
        setValue();
    }

    public Integer getOldState() {
        return this.oldState;
    }

    public void setOldState(Integer oldState) {
        this.oldState = oldState;
        setValue();
    }

    public String getDetail() {
        return this.detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
        setValue();
    }

    public Long getUpdateTime() {
        return this.updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
        setValue();
    }

    public Long getLastUpdateTime() {
        return this.lastUpdateTime;
    }

    public void setLastUpdateTime(Long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
        setValue();
    }

    public Long getNewestTime() {
        return this.newestTime;
    }

    public void setNewestTime(Long newestTime) {
        this.newestTime = newestTime;
        setValue();
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
        setValue();
    }

    public String getCabinetLocation() {
        return this.cabinetLocation;
    }

    public void setCabinetLocation(String cabinetLocation) {
        this.cabinetLocation = cabinetLocation;
        setValue();
    }

    public String getLatitude() {
        return this.latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
        setValue();
    }

    public String getLongitude() {
        return this.longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
        setValue();
    }

    public String getAppName() {
        return this.appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
        setValue();
    }

    public String getAppPackage() {
        return this.appPackage;
    }

    public void setAppPackage(String appPackage) {
        this.appPackage = appPackage;
        setValue();
    }

    public Integer getSource() {
        return this.source;
    }

    public void setSource(Integer source) {
        this.source = source;
        setValue();
    }

    public String getDataSource() {
        return this.dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
        setValue();
    }

    public String getCourierName() {
        return this.courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
        setValue();
    }

    public String getCourierPhone() {
        return this.courierPhone;
    }

    public void setCourierPhone(String courierPhone) {
        this.courierPhone = courierPhone;
        setValue();
    }

    public Integer getSubscribeState() {
        return this.subscribeState;
    }

    public void setSubscribeState(Integer subscribeState) {
        this.subscribeState = subscribeState;
        setValue();
    }

    public Long getSendTime() {
        return this.sendTime;
    }

    public void setSendTime(Long sendTime) {
        this.sendTime = sendTime;
        setValue();
    }

    public Long getSignTime() {
        return this.signTime;
    }

    public void setSignTime(Long signTime) {
        this.signTime = signTime;
        setValue();
    }

    public String getSignPerson() {
        return this.signPerson;
    }

    public void setSignPerson(String signPerson) {
        this.signPerson = signPerson;
        setValue();
    }

    public Integer getExpressFlow() {
        return this.expressFlow;
    }

    public void setExpressFlow(Integer expressFlow) {
        this.expressFlow = expressFlow;
        setValue();
    }

    public String getExtras() {
        return this.extras;
    }

    public void setExtras(String extras) {
        this.extras = extras;
        setValue();
    }

    public Long getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
        setValue();
    }

    public String getExpand() {
        return this.expand;
    }

    public void setExpand(String expand) {
        this.expand = expand;
        setValue();
    }

    public Integer getSubWithImei() {
        return this.subWithImei;
    }

    public void setSubWithImei(Integer subWithImei) {
        this.subWithImei = subWithImei;
        setValue();
    }

    public String getReserved0() {
        return this.reserved0;
    }

    public void setReserved0(String reserved0) {
        this.reserved0 = reserved0;
        setValue();
    }

    public String getReserved1() {
        return this.reserved1;
    }

    public void setReserved1(String reserved1) {
        this.reserved1 = reserved1;
        setValue();
    }

    public String getReserved2() {
        return this.reserved2;
    }

    public void setReserved2(String reserved2) {
        this.reserved2 = reserved2;
        setValue();
    }

    public String getReserved3() {
        return this.reserved3;
    }

    public void setReserved3(String reserved3) {
        this.reserved3 = reserved3;
        setValue();
    }

    public String getReserved4() {
        return this.reserved4;
    }

    public void setReserved4(String reserved4) {
        this.reserved4 = reserved4;
        setValue();
    }

    public String getReserved5() {
        return this.reserved5;
    }

    public void setReserved5(String reserved5) {
        this.reserved5 = reserved5;
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
        if (this.expressNumber != null) {
            out.writeByte((byte) 1);
            out.writeString(this.expressNumber);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.expressCompany != null) {
            out.writeByte((byte) 1);
            out.writeString(this.expressCompany);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.companyCode != null) {
            out.writeByte((byte) 1);
            out.writeString(this.companyCode);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.cabinetCompany != null) {
            out.writeByte((byte) 1);
            out.writeString(this.cabinetCompany);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mState != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mState.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.oldState != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.oldState.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.detail != null) {
            out.writeByte((byte) 1);
            out.writeString(this.detail);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.updateTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.updateTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.lastUpdateTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.lastUpdateTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.newestTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.newestTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.code != null) {
            out.writeByte((byte) 1);
            out.writeString(this.code);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.cabinetLocation != null) {
            out.writeByte((byte) 1);
            out.writeString(this.cabinetLocation);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.latitude != null) {
            out.writeByte((byte) 1);
            out.writeString(this.latitude);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.longitude != null) {
            out.writeByte((byte) 1);
            out.writeString(this.longitude);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.appName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.appName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.appPackage != null) {
            out.writeByte((byte) 1);
            out.writeString(this.appPackage);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.source != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.source.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.dataSource != null) {
            out.writeByte((byte) 1);
            out.writeString(this.dataSource);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.courierName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.courierName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.courierPhone != null) {
            out.writeByte((byte) 1);
            out.writeString(this.courierPhone);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.subscribeState != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.subscribeState.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.sendTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.sendTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.signTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.signTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.signPerson != null) {
            out.writeByte((byte) 1);
            out.writeString(this.signPerson);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.expressFlow != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.expressFlow.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.extras != null) {
            out.writeByte((byte) 1);
            out.writeString(this.extras);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.createTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.createTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.expand != null) {
            out.writeByte((byte) 1);
            out.writeString(this.expand);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.subWithImei != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.subWithImei.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved0 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved0);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved2 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved2);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved3 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved3);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved4 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved4);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved5 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved5);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<DSExpress> getHelper() {
        return DSExpressHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.DSExpress";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DSExpress { id: ").append(this.id);
        sb.append(", expressNumber: ").append(this.expressNumber);
        sb.append(", expressCompany: ").append(this.expressCompany);
        sb.append(", companyCode: ").append(this.companyCode);
        sb.append(", cabinetCompany: ").append(this.cabinetCompany);
        sb.append(", mState: ").append(this.mState);
        sb.append(", oldState: ").append(this.oldState);
        sb.append(", detail: ").append(this.detail);
        sb.append(", updateTime: ").append(this.updateTime);
        sb.append(", lastUpdateTime: ").append(this.lastUpdateTime);
        sb.append(", newestTime: ").append(this.newestTime);
        sb.append(", code: ").append(this.code);
        sb.append(", cabinetLocation: ").append(this.cabinetLocation);
        sb.append(", latitude: ").append(this.latitude);
        sb.append(", longitude: ").append(this.longitude);
        sb.append(", appName: ").append(this.appName);
        sb.append(", appPackage: ").append(this.appPackage);
        sb.append(", source: ").append(this.source);
        sb.append(", dataSource: ").append(this.dataSource);
        sb.append(", courierName: ").append(this.courierName);
        sb.append(", courierPhone: ").append(this.courierPhone);
        sb.append(", subscribeState: ").append(this.subscribeState);
        sb.append(", sendTime: ").append(this.sendTime);
        sb.append(", signTime: ").append(this.signTime);
        sb.append(", signPerson: ").append(this.signPerson);
        sb.append(", expressFlow: ").append(this.expressFlow);
        sb.append(", extras: ").append(this.extras);
        sb.append(", createTime: ").append(this.createTime);
        sb.append(", expand: ").append(this.expand);
        sb.append(", subWithImei: ").append(this.subWithImei);
        sb.append(", reserved0: ").append(this.reserved0);
        sb.append(", reserved1: ").append(this.reserved1);
        sb.append(", reserved2: ").append(this.reserved2);
        sb.append(", reserved3: ").append(this.reserved3);
        sb.append(", reserved4: ").append(this.reserved4);
        sb.append(", reserved5: ").append(this.reserved5);
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
        return "0.0.10";
    }

    public int getDatabaseVersionCode() {
        return 10;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
