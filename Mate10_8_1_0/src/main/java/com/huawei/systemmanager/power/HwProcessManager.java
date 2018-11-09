package com.huawei.systemmanager.power;

public class HwProcessManager {

    public enum UIDTYPE {
        ROOTUID,
        SYSTEMUID,
        MEDIAUID,
        SHELLUID,
        NORMALUID
    }

    public static UIDTYPE getKindOfUid(int uid) {
        switch (uid) {
            case 0:
                return UIDTYPE.ROOTUID;
            case 1000:
                return UIDTYPE.SYSTEMUID;
            case 1013:
                return UIDTYPE.MEDIAUID;
            case 2000:
                return UIDTYPE.SHELLUID;
            default:
                return UIDTYPE.NORMALUID;
        }
    }
}
