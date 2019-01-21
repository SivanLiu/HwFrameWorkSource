package huawei.cust;

import android.common.HwCfgKey;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import java.util.List;
import java.util.Map;

public class HwGetCfgFile implements IHwGetCfgFileConfig {
    public static final String LOG_TAG = "HwGetCfgFile";
    private static final int SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    private static HwGetCfgFile sInstance = null;
    private Map[] cfgFileData = new Map[2];

    public static synchronized HwGetCfgFile getDefault() {
        HwGetCfgFile hwGetCfgFile;
        synchronized (HwGetCfgFile.class) {
            synchronized (HwGetCfgFile.class) {
                if (sInstance == null) {
                    sInstance = new HwGetCfgFile();
                }
                hwGetCfgFile = sInstance;
            }
        }
        return hwGetCfgFile;
    }

    public Map getCfgFileMap(int slotId) {
        if (this.cfgFileData[slotId] != null) {
            return this.cfgFileData[slotId];
        }
        return null;
    }

    private boolean isAvail(String str) {
        return (str == null || "".equals(str)) ? false : true;
    }

    public <T> T getCfgFileData(HwCfgKey keyCollection, Class<T> clazz) {
        if (keyCollection == null || clazz == null) {
            Rlog.e(LOG_TAG, "getCfgFileData param invalid ");
            return null;
        }
        int slotid = keyCollection.slotid;
        if (!isValidSlot(slotid)) {
            Rlog.e(LOG_TAG, "getCfgFileData Error slotId ");
            return null;
        } else if (!isAvail(keyCollection.itkey) && isAvail(keyCollection.iskey) && isAvail(keyCollection.ifkey)) {
            return getCfgFileData(keyCollection.key, keyCollection.ifkey, keyCollection.iskey, keyCollection.rkey, keyCollection.fvalue, keyCollection.svalue, slotid, clazz);
        } else if (isAvail(keyCollection.itkey) || isAvail(keyCollection.iskey) || !isAvail(keyCollection.ifkey)) {
            if (this.cfgFileData[keyCollection.slotid] != null) {
                List<Map> datalist = (List) this.cfgFileData[keyCollection.slotid].get(keyCollection.key);
                if (datalist != null) {
                    for (Map data : datalist) {
                        String fdata = (String) data.get(keyCollection.ifkey);
                        String sdata = (String) data.get(keyCollection.iskey);
                        String tdata = (String) data.get(keyCollection.itkey);
                        if (isAvail(fdata) && isAvail(sdata) && isAvail(tdata) && fdata.equals(keyCollection.fvalue) && sdata.equals(keyCollection.svalue) && tdata.equals(keyCollection.tvalue)) {
                            return data.get(keyCollection.rkey);
                        }
                        if (isAvail(fdata) && isAvail(sdata) && !isAvail(tdata) && fdata.equals(keyCollection.fvalue) && sdata.equals(keyCollection.svalue)) {
                            return data.get(keyCollection.rkey);
                        }
                    }
                }
            }
            return null;
        } else {
            return getCfgFileData(keyCollection.key, keyCollection.ifkey, keyCollection.rkey, keyCollection.fvalue, slotid, clazz);
        }
    }

    private <T> T getCfgFileData(String key, String ifkey, String iskey, String rkey, String fvalue, String svalue, int slotid, Class<T> clazz) {
        if (!isAvail(key) || clazz == null || !isAvail(rkey)) {
            Rlog.e(LOG_TAG, "getCfgFileData param invalid ");
            return null;
        } else if (!isValidSlot(slotid)) {
            Rlog.e(LOG_TAG, "getCfgFileData Error slotId ");
            return null;
        } else if (!isAvail(iskey) && isAvail(ifkey)) {
            return getCfgFileData(key, ifkey, rkey, fvalue, slotid, clazz);
        } else {
            if (this.cfgFileData[slotid] != null) {
                List<Map> datalist = (List) this.cfgFileData[slotid].get(key);
                if (datalist != null) {
                    for (Map data : datalist) {
                        String fdata = (String) data.get(ifkey);
                        String sdata = (String) data.get(iskey);
                        if (isAvail(fdata) && isAvail(sdata) && fdata.equals(fvalue) && sdata.equals(svalue)) {
                            return data.get(rkey);
                        }
                        if (isAvail(fdata) && !isAvail(sdata) && fdata.equals(fvalue)) {
                            return data.get(rkey);
                        }
                    }
                }
            }
            return null;
        }
    }

    private <T> T getCfgFileData(String key, String ikey, String rkey, String ivalue, int slotid, Class<T> clazz) {
        if (!isAvail(key) || clazz == null || !isAvail(rkey)) {
            Rlog.e(LOG_TAG, "getCfgFileData param invalid ");
            return null;
        } else if (isValidSlot(slotid)) {
            if (this.cfgFileData[slotid] != null) {
                List<Map> datalist = (List) this.cfgFileData[slotid].get(key);
                if (datalist != null) {
                    for (Map data : datalist) {
                        String idata = (String) data.get(ikey);
                        if (!isAvail(idata)) {
                            return data.get(rkey);
                        }
                        if (isAvail(idata) && idata.equals(ivalue)) {
                            return data.get(rkey);
                        }
                    }
                }
            }
            return null;
        } else {
            Rlog.e(LOG_TAG, "getCfgFileData Error slotId ");
            return null;
        }
    }

    private static boolean isValidSlot(int slotId) {
        return slotId >= 0 && slotId < SIM_NUM;
    }

    public void clearCfgFileConfig(int slotId) {
        if (isValidSlot(slotId)) {
            if (this.cfgFileData[slotId] != null) {
                this.cfgFileData[slotId] = null;
            }
            return;
        }
        Rlog.e(LOG_TAG, "ClearCfgFileConfig Error slotId ");
    }

    public void readCfgFileConfig(String fileName, int slotId) {
        if (isValidSlot(slotId)) {
            try {
                Map data = HwCfgFilePolicy.getFileConfig(fileName, slotId);
                if (data == null || this.cfgFileData[slotId] == null) {
                    this.cfgFileData[slotId] = data;
                } else {
                    this.cfgFileData[slotId].putAll(data);
                }
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "Exception: read CfgFileConfig error ");
            }
            return;
        }
        Rlog.e(LOG_TAG, "ClearCfgFileConfig Error slotId ");
    }
}
