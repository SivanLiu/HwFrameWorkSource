package huawei.cust;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import huawei.cust.aidl.IHwCarrierConfigService;
import huawei.cust.aidl.IHwCarrierConfigService.Stub;
import huawei.cust.aidl.SimFileInfo;
import huawei.cust.aidl.SimMatchRule;
import java.util.Map;

public class HwCarrierConfigPolicy implements IHwCarrierConfigPolicy {
    private static final String HW_CARRIER_CONFIG_SERVICE = "hwCarrierConfig";
    private static final String LOG_TAG = "HwCarrierConfigPolicy";
    private static final String OPKEY_PROP = "persist.sys.opkey";
    private static final int SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    private static final int SLOT_0 = 0;
    private static final int SLOT_1 = 1;
    private static final boolean isMultiSimEnabled = TelephonyManager.getDefault().isMultiSimEnabled();
    private static HwCarrierConfigPolicy sInstance;

    public static HwCarrierConfigPolicy getDefault() {
        HwCarrierConfigPolicy hwCarrierConfigPolicy;
        synchronized (HwCarrierConfigPolicy.class) {
            if (sInstance == null) {
                sInstance = new HwCarrierConfigPolicy();
            }
            hwCarrierConfigPolicy = sInstance;
        }
        return hwCarrierConfigPolicy;
    }

    private HwCarrierConfigPolicy() {
    }

    public String getOpKey() {
        return getOpKey(getDefaultSlotId());
    }

    public String getOpKey(int slotId) {
        StringBuilder stringBuilder;
        if (isValidSlot(slotId)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(OPKEY_PROP);
            stringBuilder.append(slotId);
            String opKey = SystemProperties.get(stringBuilder.toString(), null);
            if (opKey == null || "".equals(opKey.trim())) {
                return null;
            }
            return opKey;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Error slotId ");
        stringBuilder.append(slotId);
        loge(stringBuilder.toString());
        return null;
    }

    public <T> T getValue(String key, Class<T> clazz) {
        return getValue(key, getDefaultSlotId(), clazz);
    }

    public <T> T getValue(String key, int slotId, Class<T> clazz) {
        if (key == null || clazz == null) {
            loge("getValue param invalid");
            return null;
        }
        try {
            IHwCarrierConfigService mHwCarrierConfigService = getHwCarrierConfigService(slotId);
            if (mHwCarrierConfigService == null) {
                loge("getConfigForSlotId Error mHwCarrierConfigService is null ");
                return null;
            }
            Map data = mHwCarrierConfigService.getConfigForSlotId(key, slotId);
            if (data == null) {
                return null;
            }
            Object value = data.get(key);
            if (value == null) {
                return null;
            }
            if (value.getClass().getName().equals(clazz.getName())) {
                return value;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getValue type error :key = ");
            stringBuilder.append(key);
            stringBuilder.append(" clazz =");
            stringBuilder.append(clazz.getName());
            stringBuilder.append(" slotId = ");
            stringBuilder.append(slotId);
            loge(stringBuilder.toString());
            return null;
        } catch (RemoteException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error getValue for slotId ");
            stringBuilder2.append(slotId);
            stringBuilder2.append(": ");
            stringBuilder2.append(e.toString());
            loge(stringBuilder2.toString());
            return null;
        }
    }

    public Map getFileConfig(String fileName) {
        return getFileConfig(fileName, getDefaultSlotId());
    }

    public Map getFileConfig(String fileName, int slotId) {
        if (isValidSlot(slotId)) {
            return HwCarrierConfigXmlParse.parse(fileName, slotId);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Error slotId ");
        stringBuilder.append(slotId);
        loge(stringBuilder.toString());
        return null;
    }

    public SimMatchRule querySimMatchRule(String mccmnc, String iccid, String imsi) {
        return querySimMatchRule(mccmnc, iccid, imsi, getDefaultSlotId());
    }

    public SimMatchRule querySimMatchRule(String mccmnc, String iccid, String imsi, int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("querySimMatchRule mccmnc = ");
        stringBuilder.append(mccmnc);
        stringBuilder.append(" ,slotId = ");
        stringBuilder.append(slotId);
        log(stringBuilder.toString());
        try {
            IHwCarrierConfigService mHwCarrierConfigService = getHwCarrierConfigService(slotId);
            if (mHwCarrierConfigService != null) {
                return mHwCarrierConfigService.querySimMatchRule(mccmnc, iccid, imsi, slotId);
            }
            loge("Error HwCarrierConfigService is null");
            return null;
        } catch (RemoteException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error querySimRule for slotId ");
            stringBuilder2.append(slotId);
            stringBuilder2.append(": ");
            stringBuilder2.append(e.toString());
            loge(stringBuilder2.toString());
            return null;
        }
    }

    public void updateSimFileInfo(SimFileInfo simFileInfo) {
        updateSimFileInfo(simFileInfo, getDefaultSlotId());
    }

    public void updateSimFileInfo(SimFileInfo simFileInfo, int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateSimFileInfo slotId = ");
        stringBuilder.append(slotId);
        log(stringBuilder.toString());
        try {
            IHwCarrierConfigService mHwCarrierConfigService = getHwCarrierConfigService(slotId);
            if (mHwCarrierConfigService == null) {
                loge("Error HwCarrierConfigService is null");
            } else {
                mHwCarrierConfigService.updateSimFileInfo(simFileInfo, slotId);
            }
        } catch (RemoteException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error updateSimFileInfo for slotId ");
            stringBuilder2.append(slotId);
            stringBuilder2.append(": ");
            stringBuilder2.append(e.toString());
            loge(stringBuilder2.toString());
        }
    }

    private IHwCarrierConfigService getHwCarrierConfigService(int slotId) {
        if (isValidSlot(slotId)) {
            try {
                IBinder binder = ServiceManager.checkService(HW_CARRIER_CONFIG_SERVICE);
                if (binder != null) {
                    return Stub.asInterface(binder);
                }
                loge("checkService hwCarrierConfig is Error");
                return null;
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getHwCarrierConfigService hwCarrierConfig is Error: ");
                stringBuilder.append(e.toString());
                loge(stringBuilder.toString());
                return null;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Error slotId ");
        stringBuilder2.append(slotId);
        loge(stringBuilder2.toString());
        return null;
    }

    private boolean isValidSlot(int slotId) {
        return slotId >= 0 && slotId < SIM_NUM;
    }

    private int getDefaultSlotId() {
        if (isMultiSimEnabled) {
            String opKey = getOpKey(0);
            if (opKey != null && !"".equals(opKey.trim())) {
                return 0;
            }
            opKey = getOpKey(1);
            if (opKey == null || "".equals(opKey.trim())) {
                return 0;
            }
            return 1;
        }
        return 0;
    }

    private static void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private static void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
