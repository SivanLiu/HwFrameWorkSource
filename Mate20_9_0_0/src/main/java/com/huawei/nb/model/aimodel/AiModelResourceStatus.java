package com.huawei.nb.model.aimodel;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class AiModelResourceStatus extends AManagedObject {
    public static final Creator<AiModelResourceStatus> CREATOR = new Creator<AiModelResourceStatus>() {
        public AiModelResourceStatus createFromParcel(Parcel in) {
            return new AiModelResourceStatus(in);
        }

        public AiModelResourceStatus[] newArray(int size) {
            return new AiModelResourceStatus[size];
        }
    };
    private String abTest;
    private String chipset;
    private String chipsetVendor;
    private String decryptedKey;
    private String district;
    private String emuiFamily;
    private Long id;
    private String interfaceVersion;
    private String param1;
    private String param2;
    private String product;
    private String productFamily;
    private String productModel;
    private String res_name;
    private String resid;
    private Long status;
    private String supprtAppVerson;
    private String teams;
    private Boolean type;
    private Boolean update;
    private String url;
    private Long version = Long.valueOf(0);
    private String xpu;
    private String zipSha256;

    public AiModelResourceStatus(Cursor cursor) {
        Boolean bool;
        boolean z = true;
        Boolean bool2 = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.resid = cursor.getString(2);
        this.status = cursor.isNull(3) ? null : Long.valueOf(cursor.getLong(3));
        this.version = cursor.isNull(4) ? null : Long.valueOf(cursor.getLong(4));
        this.url = cursor.getString(5);
        this.teams = cursor.getString(6);
        this.zipSha256 = cursor.getString(7);
        this.decryptedKey = cursor.getString(8);
        if (cursor.isNull(9)) {
            bool = null;
        } else {
            bool = Boolean.valueOf(cursor.getInt(9) != 0);
        }
        this.type = bool;
        if (!cursor.isNull(10)) {
            if (cursor.getInt(10) == 0) {
                z = false;
            }
            bool2 = Boolean.valueOf(z);
        }
        this.update = bool2;
        this.xpu = cursor.getString(11);
        this.emuiFamily = cursor.getString(12);
        this.productFamily = cursor.getString(13);
        this.chipsetVendor = cursor.getString(14);
        this.chipset = cursor.getString(15);
        this.product = cursor.getString(16);
        this.productModel = cursor.getString(17);
        this.district = cursor.getString(18);
        this.abTest = cursor.getString(19);
        this.supprtAppVerson = cursor.getString(20);
        this.interfaceVersion = cursor.getString(21);
        this.param1 = cursor.getString(22);
        this.param2 = cursor.getString(23);
        this.res_name = cursor.getString(24);
    }

    public AiModelResourceStatus(Parcel in) {
        Boolean bool;
        boolean z = true;
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.resid = in.readByte() == (byte) 0 ? null : in.readString();
        this.status = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.version = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.url = in.readByte() == (byte) 0 ? null : in.readString();
        this.teams = in.readByte() == (byte) 0 ? null : in.readString();
        this.zipSha256 = in.readByte() == (byte) 0 ? null : in.readString();
        this.decryptedKey = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() == (byte) 0) {
            bool = null;
        } else {
            bool = Boolean.valueOf(in.readByte() != (byte) 0);
        }
        this.type = bool;
        if (in.readByte() == (byte) 0) {
            bool = null;
        } else {
            if (in.readByte() == (byte) 0) {
                z = false;
            }
            bool = Boolean.valueOf(z);
        }
        this.update = bool;
        this.xpu = in.readByte() == (byte) 0 ? null : in.readString();
        this.emuiFamily = in.readByte() == (byte) 0 ? null : in.readString();
        this.productFamily = in.readByte() == (byte) 0 ? null : in.readString();
        this.chipsetVendor = in.readByte() == (byte) 0 ? null : in.readString();
        this.chipset = in.readByte() == (byte) 0 ? null : in.readString();
        this.product = in.readByte() == (byte) 0 ? null : in.readString();
        this.productModel = in.readByte() == (byte) 0 ? null : in.readString();
        this.district = in.readByte() == (byte) 0 ? null : in.readString();
        this.abTest = in.readByte() == (byte) 0 ? null : in.readString();
        this.supprtAppVerson = in.readByte() == (byte) 0 ? null : in.readString();
        this.interfaceVersion = in.readByte() == (byte) 0 ? null : in.readString();
        this.param1 = in.readByte() == (byte) 0 ? null : in.readString();
        this.param2 = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.res_name = str;
    }

    private AiModelResourceStatus(Long id, String resid, Long status, Long version, String url, String teams, String zipSha256, String decryptedKey, Boolean type, Boolean update, String xpu, String emuiFamily, String productFamily, String chipsetVendor, String chipset, String product, String productModel, String district, String abTest, String supprtAppVerson, String interfaceVersion, String param1, String param2, String res_name) {
        this.id = id;
        this.resid = resid;
        this.status = status;
        this.version = version;
        this.url = url;
        this.teams = teams;
        this.zipSha256 = zipSha256;
        this.decryptedKey = decryptedKey;
        this.type = type;
        this.update = update;
        this.xpu = xpu;
        this.emuiFamily = emuiFamily;
        this.productFamily = productFamily;
        this.chipsetVendor = chipsetVendor;
        this.chipset = chipset;
        this.product = product;
        this.productModel = productModel;
        this.district = district;
        this.abTest = abTest;
        this.supprtAppVerson = supprtAppVerson;
        this.interfaceVersion = interfaceVersion;
        this.param1 = param1;
        this.param2 = param2;
        this.res_name = res_name;
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

    public String getResid() {
        return this.resid;
    }

    public void setResid(String resid) {
        this.resid = resid;
        setValue();
    }

    public Long getStatus() {
        return this.status;
    }

    public void setStatus(Long status) {
        this.status = status;
        setValue();
    }

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
        this.version = version;
        setValue();
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
        setValue();
    }

    public String getTeams() {
        return this.teams;
    }

    public void setTeams(String teams) {
        this.teams = teams;
        setValue();
    }

    public String getZipSha256() {
        return this.zipSha256;
    }

    public void setZipSha256(String zipSha256) {
        this.zipSha256 = zipSha256;
        setValue();
    }

    public String getDecryptedKey() {
        return this.decryptedKey;
    }

    public void setDecryptedKey(String decryptedKey) {
        this.decryptedKey = decryptedKey;
        setValue();
    }

    public Boolean getType() {
        return this.type;
    }

    public void setType(Boolean type) {
        this.type = type;
        setValue();
    }

    public Boolean getUpdate() {
        return this.update;
    }

    public void setUpdate(Boolean update) {
        this.update = update;
        setValue();
    }

    public String getXpu() {
        return this.xpu;
    }

    public void setXpu(String xpu) {
        this.xpu = xpu;
        setValue();
    }

    public String getEmuiFamily() {
        return this.emuiFamily;
    }

    public void setEmuiFamily(String emuiFamily) {
        this.emuiFamily = emuiFamily;
        setValue();
    }

    public String getProductFamily() {
        return this.productFamily;
    }

    public void setProductFamily(String productFamily) {
        this.productFamily = productFamily;
        setValue();
    }

    public String getChipsetVendor() {
        return this.chipsetVendor;
    }

    public void setChipsetVendor(String chipsetVendor) {
        this.chipsetVendor = chipsetVendor;
        setValue();
    }

    public String getChipset() {
        return this.chipset;
    }

    public void setChipset(String chipset) {
        this.chipset = chipset;
        setValue();
    }

    public String getProduct() {
        return this.product;
    }

    public void setProduct(String product) {
        this.product = product;
        setValue();
    }

    public String getProductModel() {
        return this.productModel;
    }

    public void setProductModel(String productModel) {
        this.productModel = productModel;
        setValue();
    }

    public String getDistrict() {
        return this.district;
    }

    public void setDistrict(String district) {
        this.district = district;
        setValue();
    }

    public String getAbTest() {
        return this.abTest;
    }

    public void setAbTest(String abTest) {
        this.abTest = abTest;
        setValue();
    }

    public String getSupprtAppVerson() {
        return this.supprtAppVerson;
    }

    public void setSupprtAppVerson(String supprtAppVerson) {
        this.supprtAppVerson = supprtAppVerson;
        setValue();
    }

    public String getInterfaceVersion() {
        return this.interfaceVersion;
    }

    public void setInterfaceVersion(String interfaceVersion) {
        this.interfaceVersion = interfaceVersion;
        setValue();
    }

    public String getParam1() {
        return this.param1;
    }

    public void setParam1(String param1) {
        this.param1 = param1;
        setValue();
    }

    public String getParam2() {
        return this.param2;
    }

    public void setParam2(String param2) {
        this.param2 = param2;
        setValue();
    }

    public String getRes_name() {
        return this.res_name;
    }

    public void setRes_name(String res_name) {
        this.res_name = res_name;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        byte b;
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.id.longValue());
        } else {
            out.writeByte((byte) 0);
            out.writeLong(1);
        }
        if (this.resid != null) {
            out.writeByte((byte) 1);
            out.writeString(this.resid);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.status != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.status.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.version != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.version.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.url != null) {
            out.writeByte((byte) 1);
            out.writeString(this.url);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.teams != null) {
            out.writeByte((byte) 1);
            out.writeString(this.teams);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.zipSha256 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.zipSha256);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.decryptedKey != null) {
            out.writeByte((byte) 1);
            out.writeString(this.decryptedKey);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.type != null) {
            out.writeByte((byte) 1);
            if (this.type.booleanValue()) {
                b = (byte) 1;
            } else {
                b = (byte) 0;
            }
            out.writeByte(b);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.update != null) {
            out.writeByte((byte) 1);
            if (this.update.booleanValue()) {
                b = (byte) 1;
            } else {
                b = (byte) 0;
            }
            out.writeByte(b);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.xpu != null) {
            out.writeByte((byte) 1);
            out.writeString(this.xpu);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.emuiFamily != null) {
            out.writeByte((byte) 1);
            out.writeString(this.emuiFamily);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.productFamily != null) {
            out.writeByte((byte) 1);
            out.writeString(this.productFamily);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.chipsetVendor != null) {
            out.writeByte((byte) 1);
            out.writeString(this.chipsetVendor);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.chipset != null) {
            out.writeByte((byte) 1);
            out.writeString(this.chipset);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.product != null) {
            out.writeByte((byte) 1);
            out.writeString(this.product);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.productModel != null) {
            out.writeByte((byte) 1);
            out.writeString(this.productModel);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.district != null) {
            out.writeByte((byte) 1);
            out.writeString(this.district);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.abTest != null) {
            out.writeByte((byte) 1);
            out.writeString(this.abTest);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.supprtAppVerson != null) {
            out.writeByte((byte) 1);
            out.writeString(this.supprtAppVerson);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.interfaceVersion != null) {
            out.writeByte((byte) 1);
            out.writeString(this.interfaceVersion);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.param1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.param1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.param2 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.param2);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.res_name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.res_name);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<AiModelResourceStatus> getHelper() {
        return AiModelResourceStatusHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.aimodel.AiModelResourceStatus";
    }

    public String getDatabaseName() {
        return "dsAiModel";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("AiModelResourceStatus { id: ").append(this.id);
        sb.append(", resid: ").append(this.resid);
        sb.append(", status: ").append(this.status);
        sb.append(", version: ").append(this.version);
        sb.append(", url: ").append(this.url);
        sb.append(", teams: ").append(this.teams);
        sb.append(", zipSha256: ").append(this.zipSha256);
        sb.append(", decryptedKey: ").append(this.decryptedKey);
        sb.append(", type: ").append(this.type);
        sb.append(", update: ").append(this.update);
        sb.append(", xpu: ").append(this.xpu);
        sb.append(", emuiFamily: ").append(this.emuiFamily);
        sb.append(", productFamily: ").append(this.productFamily);
        sb.append(", chipsetVendor: ").append(this.chipsetVendor);
        sb.append(", chipset: ").append(this.chipset);
        sb.append(", product: ").append(this.product);
        sb.append(", productModel: ").append(this.productModel);
        sb.append(", district: ").append(this.district);
        sb.append(", abTest: ").append(this.abTest);
        sb.append(", supprtAppVerson: ").append(this.supprtAppVerson);
        sb.append(", interfaceVersion: ").append(this.interfaceVersion);
        sb.append(", param1: ").append(this.param1);
        sb.append(", param2: ").append(this.param2);
        sb.append(", res_name: ").append(this.res_name);
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
        return "0.0.8";
    }

    public int getDatabaseVersionCode() {
        return 8;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
