package com.android.server.intellicom.networkslice.model;

import java.util.Objects;

public class OsAppId {
    private static final int NUMBER_OF_APPID_PARTS = 2;
    private static final String SEPARATOR_FOR_APPID = "#";
    private String mAppId;
    private String mOsId;

    public String getAppId() {
        return this.mAppId;
    }

    public static OsAppId create(String osAppId) {
        String[] values;
        if (osAppId == null || osAppId.indexOf("#") == -1 || (values = osAppId.split("#")) == null || values.length != 2) {
            return null;
        }
        OsAppId id = new OsAppId();
        id.mOsId = values[0];
        id.mAppId = values[1];
        return id;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OsAppId osAppId = (OsAppId) o;
        if (!Objects.equals(this.mOsId, osAppId.mOsId) || !Objects.equals(this.mAppId, osAppId.mAppId)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return Objects.hash(this.mOsId, this.mAppId);
    }
}
