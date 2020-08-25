package com.huawei.nb.kv;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.huawei.nb.utils.JsonUtils;
import com.huawei.nb.utils.logger.DSLog;

public class VJson implements Value {
    public static final Parcelable.Creator<VJson> CREATOR = new Parcelable.Creator<VJson>() {
        /* class com.huawei.nb.kv.VJson.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public VJson createFromParcel(Parcel in) {
            return new VJson(in);
        }

        @Override // android.os.Parcelable.Creator
        public VJson[] newArray(int size) {
            return new VJson[size];
        }
    };
    private static final String EMPTY_JSON = "{}";
    private static final String SEPARATOR = "/";
    private String jsonVal;

    public VJson(String val) {
        this.jsonVal = val;
    }

    @Override // com.huawei.nb.kv.Value
    public Integer dType() {
        return 4;
    }

    public boolean equals(Object aValue) {
        if (aValue == null || !(aValue instanceof VJson)) {
            return false;
        }
        VJson otherVal = (VJson) aValue;
        if (this.jsonVal != null) {
            return this.jsonVal.equals(otherVal.getValue());
        }
        if (otherVal.getValue() == null) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        if (this.jsonVal != null) {
            return this.jsonVal.hashCode();
        }
        return 0;
    }

    @Override // com.huawei.nb.kv.Value
    public boolean verify() {
        boolean bRet = false;
        try {
            JsonObject obj = new JsonParser().parse(JsonUtils.sanitize(this.jsonVal)).getAsJsonObject();
            if (obj != null) {
                if (EMPTY_JSON.equalsIgnoreCase(obj.toString())) {
                    return false;
                }
                bRet = true;
            }
        } catch (Exception e) {
            DSLog.e("Invalid VJson value [" + (this.jsonVal == null ? " " : this.jsonVal) + "]: " + e.getMessage(), new Object[0]);
        }
        return bRet;
    }

    public boolean verify(String secondKey) {
        if (secondKey == null || secondKey.length() == 0) {
            return true;
        }
        try {
            JsonObject obj = new JsonParser().parse(JsonUtils.sanitize(this.jsonVal)).getAsJsonObject();
            if (obj == null) {
                return false;
            }
            String[] keys = secondKey.split(SEPARATOR);
            return obj.has(keys[keys.length - 1]);
        } catch (Exception e) {
            DSLog.e("verify exception: " + e.getMessage(), new Object[0]);
            return false;
        }
    }

    public String getValue() {
        return this.jsonVal;
    }

    public void setValue(String jsonVal2) {
        this.jsonVal = jsonVal2;
    }

    protected VJson(Parcel in) {
        this.jsonVal = in.readString();
        if (this.jsonVal.equals("")) {
            this.jsonVal = null;
        }
    }

    @Override // com.huawei.nb.kv.Value
    public String toString() {
        return this.jsonVal == null ? "" : this.jsonVal;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.jsonVal == null ? "" : this.jsonVal);
    }
}
