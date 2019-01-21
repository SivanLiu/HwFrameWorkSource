package com.android.internal.telephony;

import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TelephonyDevController extends Handler {
    private static final boolean DBG = true;
    private static final int EVENT_HARDWARE_CONFIG_CHANGED = 1;
    private static final String LOG_TAG = "TDC";
    private static final Object mLock = new Object();
    private static ArrayList<HardwareConfig> mModems = new ArrayList();
    private static ArrayList<HardwareConfig> mSims = new ArrayList();
    private static Message sRilHardwareConfig;
    private static TelephonyDevController sTelephonyDevController;

    private static void logd(String s) {
        Rlog.d(LOG_TAG, s);
    }

    private static void loge(String s) {
        Rlog.e(LOG_TAG, s);
    }

    public static TelephonyDevController create() {
        TelephonyDevController telephonyDevController;
        synchronized (mLock) {
            if (sTelephonyDevController == null) {
                sTelephonyDevController = new TelephonyDevController();
                telephonyDevController = sTelephonyDevController;
            } else {
                throw new RuntimeException("TelephonyDevController already created!?!");
            }
        }
        return telephonyDevController;
    }

    public static TelephonyDevController getInstance() {
        TelephonyDevController telephonyDevController;
        synchronized (mLock) {
            if (sTelephonyDevController != null) {
                telephonyDevController = sTelephonyDevController;
            } else {
                throw new RuntimeException("TelephonyDevController not yet created!?!");
            }
        }
        return telephonyDevController;
    }

    private void initFromResource() {
        String[] hwStrings = Resources.getSystem().getStringArray(17236040);
        if (hwStrings != null) {
            for (String hwString : hwStrings) {
                HardwareConfig hw = new HardwareConfig(hwString);
                if (hw.type == 0) {
                    updateOrInsert(hw, mModems);
                } else if (hw.type == 1) {
                    updateOrInsert(hw, mSims);
                }
            }
        }
    }

    private TelephonyDevController() {
        initFromResource();
        mModems.trimToSize();
        mSims.trimToSize();
    }

    public static void registerRIL(CommandsInterface cmdsIf) {
        cmdsIf.getHardwareConfig(sRilHardwareConfig);
        if (sRilHardwareConfig != null) {
            AsyncResult ar = sRilHardwareConfig.obj;
            if (ar.exception == null) {
                handleGetHardwareConfigChanged(ar);
            }
        }
        cmdsIf.registerForHardwareConfigChanged(sTelephonyDevController, 1, null);
    }

    public static void unregisterRIL(CommandsInterface cmdsIf) {
        cmdsIf.unregisterForHardwareConfigChanged(sTelephonyDevController);
    }

    public void handleMessage(Message msg) {
        if (msg.what != 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleMessage: Unknown Event ");
            stringBuilder.append(msg.what);
            loge(stringBuilder.toString());
            return;
        }
        logd("handleMessage: received EVENT_HARDWARE_CONFIG_CHANGED");
        handleGetHardwareConfigChanged(msg.obj);
    }

    private static void updateOrInsert(HardwareConfig hw, ArrayList<HardwareConfig> list) {
        synchronized (mLock) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                HardwareConfig item = (HardwareConfig) list.get(i);
                if (item.uuid.compareTo(hw.uuid) == 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateOrInsert: removing: ");
                    stringBuilder.append(item);
                    logd(stringBuilder.toString());
                    list.remove(i);
                    break;
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateOrInsert: inserting: ");
            stringBuilder2.append(hw);
            logd(stringBuilder2.toString());
            list.add(hw);
        }
    }

    private static void handleGetHardwareConfigChanged(AsyncResult ar) {
        if (ar.exception != null || ar.result == null) {
            loge("handleGetHardwareConfigChanged - returned an error.");
            return;
        }
        List hwcfg = ar.result;
        for (int i = 0; i < hwcfg.size(); i++) {
            HardwareConfig hw = (HardwareConfig) hwcfg.get(i);
            if (hw != null) {
                if (hw.type == 0) {
                    updateOrInsert(hw, mModems);
                } else if (hw.type == 1) {
                    updateOrInsert(hw, mSims);
                }
            }
        }
    }

    public static int getModemCount() {
        int count;
        synchronized (mLock) {
            count = mModems.size();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getModemCount: ");
            stringBuilder.append(count);
            logd(stringBuilder.toString());
        }
        return count;
    }

    public HardwareConfig getModem(int index) {
        synchronized (mLock) {
            StringBuilder stringBuilder;
            if (mModems.isEmpty()) {
                loge("getModem: no registered modem device?!?");
                return null;
            } else if (index > getModemCount()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("getModem: out-of-bounds access for modem device ");
                stringBuilder.append(index);
                stringBuilder.append(" max: ");
                stringBuilder.append(getModemCount());
                loge(stringBuilder.toString());
                return null;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("getModem: ");
                stringBuilder.append(index);
                logd(stringBuilder.toString());
                HardwareConfig hardwareConfig = (HardwareConfig) mModems.get(index);
                return hardwareConfig;
            }
        }
    }

    public int getSimCount() {
        int count;
        synchronized (mLock) {
            count = mSims.size();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getSimCount: ");
            stringBuilder.append(count);
            logd(stringBuilder.toString());
        }
        return count;
    }

    public HardwareConfig getSim(int index) {
        synchronized (mLock) {
            StringBuilder stringBuilder;
            if (mSims.isEmpty()) {
                loge("getSim: no registered sim device?!?");
                return null;
            } else if (index > getSimCount()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("getSim: out-of-bounds access for sim device ");
                stringBuilder.append(index);
                stringBuilder.append(" max: ");
                stringBuilder.append(getSimCount());
                loge(stringBuilder.toString());
                return null;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("getSim: ");
                stringBuilder.append(index);
                logd(stringBuilder.toString());
                HardwareConfig hardwareConfig = (HardwareConfig) mSims.get(index);
                return hardwareConfig;
            }
        }
    }

    public HardwareConfig getModemForSim(int simIndex) {
        synchronized (mLock) {
            if (!mModems.isEmpty()) {
                if (!mSims.isEmpty()) {
                    StringBuilder stringBuilder;
                    if (simIndex > getSimCount()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("getModemForSim: out-of-bounds access for sim device ");
                        stringBuilder.append(simIndex);
                        stringBuilder.append(" max: ");
                        stringBuilder.append(getSimCount());
                        loge(stringBuilder.toString());
                        return null;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getModemForSim ");
                    stringBuilder.append(simIndex);
                    logd(stringBuilder.toString());
                    HardwareConfig sim = getSim(simIndex);
                    Iterator it = mModems.iterator();
                    while (it.hasNext()) {
                        HardwareConfig modem = (HardwareConfig) it.next();
                        if (modem.uuid.equals(sim.modemUuid)) {
                            return modem;
                        }
                    }
                    return null;
                }
            }
            loge("getModemForSim: no registered modem/sim device?!?");
            return null;
        }
    }

    public ArrayList<HardwareConfig> getAllSimsForModem(int modemIndex) {
        synchronized (mLock) {
            StringBuilder stringBuilder;
            if (mSims.isEmpty()) {
                loge("getAllSimsForModem: no registered sim device?!?");
                return null;
            } else if (modemIndex > getModemCount()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("getAllSimsForModem: out-of-bounds access for modem device ");
                stringBuilder.append(modemIndex);
                stringBuilder.append(" max: ");
                stringBuilder.append(getModemCount());
                loge(stringBuilder.toString());
                return null;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("getAllSimsForModem ");
                stringBuilder.append(modemIndex);
                logd(stringBuilder.toString());
                ArrayList<HardwareConfig> result = new ArrayList();
                HardwareConfig modem = getModem(modemIndex);
                Iterator it = mSims.iterator();
                while (it.hasNext()) {
                    HardwareConfig sim = (HardwareConfig) it.next();
                    if (sim.modemUuid.equals(modem.uuid)) {
                        result.add(sim);
                    }
                }
                return result;
            }
        }
    }

    public ArrayList<HardwareConfig> getAllModems() {
        ArrayList<HardwareConfig> modems;
        synchronized (mLock) {
            modems = new ArrayList();
            if (mModems.isEmpty()) {
                logd("getAllModems: empty list.");
            } else {
                Iterator it = mModems.iterator();
                while (it.hasNext()) {
                    modems.add((HardwareConfig) it.next());
                }
            }
        }
        return modems;
    }

    public ArrayList<HardwareConfig> getAllSims() {
        ArrayList<HardwareConfig> sims;
        synchronized (mLock) {
            sims = new ArrayList();
            if (mSims.isEmpty()) {
                logd("getAllSims: empty list.");
            } else {
                Iterator it = mSims.iterator();
                while (it.hasNext()) {
                    sims.add((HardwareConfig) it.next());
                }
            }
        }
        return sims;
    }
}
