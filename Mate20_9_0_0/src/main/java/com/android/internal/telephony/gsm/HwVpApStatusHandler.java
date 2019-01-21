package com.android.internal.telephony.gsm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings.Global;
import android.util.Log;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwQualcommRIL;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class HwVpApStatusHandler extends Handler {
    private static final int AGPS_APP_STOPPED = 0;
    private static final boolean DBG = true;
    private static final String HWNV_CLASS = "com.huawei.android.hwnv.HWNVFuncation";
    private static final int MSG_AP_STATUS_CHANGED = 12;
    private static final int MSG_AP_STATUS_SEND_DONE = 13;
    private static final String TAG_STATIC = "HwVpApStatusHandler";
    private static final int VP_AGPS = 2;
    private static final int VP_BT_TETHER = 64;
    private static final int VP_ENABLE = 256;
    private static final int VP_MASK = 118;
    private static final int VP_MMS = 4;
    private static final int VP_MOBILE = 1;
    private static final int VP_SCREEN_ON = 128;
    private static final int VP_TETHER_MASK = 112;
    private static final int VP_USB_TETHER = 16;
    private static final int VP_WIFI_TETHER = 32;
    private static final boolean mIsSurpportNvFunc = HwModemCapability.isCapabilitySupport(13);
    private String TAG = TAG_STATIC;
    private int VP_Flag = 0;
    private AgpsAppObserver mAgpsAppObserver;
    private GsmCdmaPhone mPhone;
    private EventReceiver mReceiver;
    private Method mSetVPEvent = null;

    private final class AgpsAppObserver extends ContentObserver {
        public AgpsAppObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            int agpsAppStatus = Global.getInt(HwVpApStatusHandler.this.mPhone.getContext().getContentResolver(), "agps_app_started_navigation", -1);
            String access$100 = HwVpApStatusHandler.this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AgpsAppObserver onChange(),agps App Status: ");
            stringBuilder.append(agpsAppStatus);
            Log.d(access$100, stringBuilder.toString());
            if (agpsAppStatus == 0) {
                HwVpApStatusHandler.access$072(HwVpApStatusHandler.this, -3);
            } else {
                HwVpApStatusHandler.access$076(HwVpApStatusHandler.this, 2);
            }
            HwVpApStatusHandler.this.sendMessage(HwVpApStatusHandler.this.obtainMessage(12, HwVpApStatusHandler.this.VP_Flag | HwVpApStatusHandler.VP_ENABLE, 0));
        }
    }

    public class EventReceiver extends BroadcastReceiver {
        public static final String ACTION_TETHER_STATE_CHANGED = "android.net.conn.TETHER_STATE_CHANGED";
        public static final String EXTRA_ACTIVE_TETHER = "tetherArray";
        public static final int MOBILE_TYPE_NONE = -1;

        /* JADX WARNING: Removed duplicated region for block: B:72:0x01df  */
        /* JADX WARNING: Removed duplicated region for block: B:71:0x01d5  */
        /* JADX WARNING: Removed duplicated region for block: B:76:0x01f8  */
        /* JADX WARNING: Removed duplicated region for block: B:75:0x01ef  */
        /* JADX WARNING: Removed duplicated region for block: B:80:0x0210  */
        /* JADX WARNING: Removed duplicated region for block: B:79:0x0207  */
        /* JADX WARNING: Removed duplicated region for block: B:87:0x0238  */
        /* JADX WARNING: Removed duplicated region for block: B:89:0x024b  */
        /* JADX WARNING: Removed duplicated region for block: B:91:0x025e  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            Context context2 = context;
            Intent intent2 = intent;
            String action = intent.getAction();
            int old_VP_Flag = HwVpApStatusHandler.this.VP_Flag;
            String access$100 = HwVpApStatusHandler.this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("receiver:");
            stringBuilder.append(action);
            Log.d(access$100, stringBuilder.toString());
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                String stat_inf;
                StringBuilder stringBuilder2;
                ConnectivityManager mConnMgr = (ConnectivityManager) context2.getSystemService("connectivity");
                int type = intent2.getIntExtra("networkType", -1);
                String extra_info = intent2.getStringExtra("extraInfo");
                String access$1002 = HwVpApStatusHandler.this.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("extra_info:");
                stringBuilder3.append(extra_info != null ? extra_info : "none");
                Log.i(access$1002, stringBuilder3.toString());
                NetworkInfo netinfo = mConnMgr.getNetworkInfo(type);
                String ni_info = "unknown connection ";
                if (netinfo != null) {
                    State istate = netinfo.getState();
                    String access$1003 = HwVpApStatusHandler.this.TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("net-state:");
                    stringBuilder4.append(istate);
                    stringBuilder4.append(",type=");
                    stringBuilder4.append(type);
                    Log.i(access$1003, stringBuilder4.toString());
                    if (type != 0) {
                        if (type != 2) {
                            switch (type) {
                                case 4:
                                    if (istate != State.CONNECTED) {
                                        HwVpApStatusHandler.access$072(HwVpApStatusHandler.this, -17);
                                        ni_info = "-MOBILE_DUN,USB_tether disconnected";
                                        break;
                                    }
                                    HwVpApStatusHandler.access$076(HwVpApStatusHandler.this, 16);
                                    ni_info = "+MOBILE_DUN,USB_tether connected";
                                    break;
                                case 5:
                                    if (istate != State.CONNECTED) {
                                        HwVpApStatusHandler.access$072(HwVpApStatusHandler.this, -17);
                                        ni_info = "-MOBILE_HIPRI,USB_tether disconnected";
                                        break;
                                    }
                                    HwVpApStatusHandler.access$076(HwVpApStatusHandler.this, 16);
                                    ni_info = "+MOBILE_HIPRI,USB_tether connected";
                                    break;
                                default:
                                    stat_inf = istate == State.CONNECTED ? ",connected" : ",disconnected";
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append(ni_info);
                                    stringBuilder2.append(",type:");
                                    stringBuilder2.append(type);
                                    stringBuilder2.append(stat_inf);
                                    ni_info = stringBuilder2.toString();
                                    break;
                            }
                        } else if (istate == State.CONNECTED) {
                            HwVpApStatusHandler.access$076(HwVpApStatusHandler.this, 4);
                            ni_info = "+MOBILE_MMS, MMS connected!";
                        } else {
                            HwVpApStatusHandler.access$072(HwVpApStatusHandler.this, -5);
                            ni_info = "-MOBILE_MMS, MMS disconnected";
                        }
                    } else if (istate == State.CONNECTED) {
                        ni_info = "+MOBILE connected";
                    } else {
                        HwVpApStatusHandler.access$072(HwVpApStatusHandler.this, -2);
                        ni_info = "-MOBILE disconnected";
                    }
                }
                stat_inf = HwVpApStatusHandler.this.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("action:");
                stringBuilder2.append(ni_info);
                Log.e(stat_inf, stringBuilder2.toString());
            } else if (ACTION_TETHER_STATE_CHANGED.equals(action)) {
                boolean usbTethered;
                String s;
                String access$1004;
                StringBuilder stringBuilder5;
                boolean wifiTethered = false;
                boolean btTethered = false;
                ArrayList<String> active = intent2.getStringArrayListExtra(EXTRA_ACTIVE_TETHER);
                if (active != null) {
                    int length = active.size();
                    if (length > 0) {
                        int length2;
                        String[] tethered = (String[]) active.toArray(new String[length]);
                        ConnectivityManager mConnMgr2 = (ConnectivityManager) context2.getSystemService("connectivity");
                        String[] mUsbRegexs = mConnMgr2.getTetherableUsbRegexs();
                        String[] mWifiRegexs = mConnMgr2.getTetherableWifiRegexs();
                        String[] mBtRegexs = mConnMgr2.getTetherableBluetoothRegexs();
                        boolean length3 = tethered.length;
                        usbTethered = false;
                        boolean usbTethered2 = false;
                        while (usbTethered2 < length3) {
                            s = tethered[usbTethered2];
                            boolean length4 = mUsbRegexs.length;
                            boolean z = length3;
                            length3 = false;
                            while (length3 < length4) {
                                boolean z2 = length4;
                                if (s.matches(mUsbRegexs[length3])) {
                                    usbTethered = true;
                                    break;
                                } else {
                                    length3++;
                                    length4 = z2;
                                }
                            }
                            usbTethered2++;
                            length3 = z;
                            context2 = context;
                            intent2 = intent;
                        }
                        int length5 = tethered.length;
                        int i = 0;
                        while (i < length5) {
                            usbTethered2 = tethered[i];
                            length2 = mWifiRegexs.length;
                            int i2 = length5;
                            length5 = 0;
                            while (length5 < length2) {
                                int i3 = length2;
                                if (usbTethered2.matches(mWifiRegexs[length5])) {
                                    wifiTethered = true;
                                    break;
                                } else {
                                    length5++;
                                    length2 = i3;
                                }
                            }
                            i++;
                            length5 = i2;
                        }
                        length5 = tethered.length;
                        i = 0;
                        while (i < length5) {
                            usbTethered2 = tethered[i];
                            length2 = mBtRegexs.length;
                            int i4 = length5;
                            length5 = 0;
                            while (length5 < length2) {
                                int i5 = length2;
                                if (usbTethered2.matches(mBtRegexs[length5])) {
                                    btTethered = true;
                                    break;
                                } else {
                                    length5++;
                                    length2 = i5;
                                }
                            }
                            i++;
                            length5 = i4;
                        }
                        HwVpApStatusHandler.this.VP_Flag = usbTethered ? HwVpApStatusHandler.this.VP_Flag | 16 : HwVpApStatusHandler.this.VP_Flag & -17;
                        HwVpApStatusHandler.this.VP_Flag = wifiTethered ? HwVpApStatusHandler.this.VP_Flag | 32 : HwVpApStatusHandler.this.VP_Flag & -33;
                        HwVpApStatusHandler.this.VP_Flag = btTethered ? HwVpApStatusHandler.this.VP_Flag | 64 : HwVpApStatusHandler.this.VP_Flag & -65;
                        s = "Tethering";
                        StringBuilder stringBuilder6;
                        if (!usbTethered || wifiTethered || btTethered) {
                            if (usbTethered) {
                                stringBuilder6 = new StringBuilder();
                                stringBuilder6.append(s);
                                stringBuilder6.append(":USB");
                                s = stringBuilder6.toString();
                            }
                            if (wifiTethered) {
                                stringBuilder6 = new StringBuilder();
                                stringBuilder6.append(s);
                                stringBuilder6.append(":Wifi");
                                s = stringBuilder6.toString();
                            }
                            if (btTethered) {
                                stringBuilder6 = new StringBuilder();
                                stringBuilder6.append(s);
                                stringBuilder6.append(":BT");
                                s = stringBuilder6.toString();
                            }
                        } else {
                            stringBuilder6 = new StringBuilder();
                            stringBuilder6.append(s);
                            stringBuilder6.append(":Nothing");
                            s = stringBuilder6.toString();
                        }
                        access$1004 = HwVpApStatusHandler.this.TAG;
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("tether :");
                        stringBuilder5.append(s);
                        Log.e(access$1004, stringBuilder5.toString());
                    }
                }
                usbTethered = false;
                if (usbTethered) {
                }
                HwVpApStatusHandler.this.VP_Flag = usbTethered ? HwVpApStatusHandler.this.VP_Flag | 16 : HwVpApStatusHandler.this.VP_Flag & -17;
                if (wifiTethered) {
                }
                HwVpApStatusHandler.this.VP_Flag = wifiTethered ? HwVpApStatusHandler.this.VP_Flag | 32 : HwVpApStatusHandler.this.VP_Flag & -33;
                if (btTethered) {
                }
                HwVpApStatusHandler.this.VP_Flag = btTethered ? HwVpApStatusHandler.this.VP_Flag | 64 : HwVpApStatusHandler.this.VP_Flag & -65;
                s = "Tethering";
                if (usbTethered) {
                }
                if (usbTethered) {
                }
                if (wifiTethered) {
                }
                if (btTethered) {
                }
                access$1004 = HwVpApStatusHandler.this.TAG;
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("tether :");
                stringBuilder5.append(s);
                Log.e(access$1004, stringBuilder5.toString());
            } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                HwVpApStatusHandler.access$076(HwVpApStatusHandler.this, HwVpApStatusHandler.VP_SCREEN_ON);
                Log.e(HwVpApStatusHandler.this.TAG, "Screen on!");
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                HwVpApStatusHandler.access$072(HwVpApStatusHandler.this, -129);
                Log.e(HwVpApStatusHandler.this.TAG, "Screen off!");
            }
            if (HwVpApStatusHandler.this.VP_Flag != old_VP_Flag) {
                HwVpApStatusHandler.this.sendMessage(HwVpApStatusHandler.this.obtainMessage(12, HwVpApStatusHandler.this.VP_Flag | HwVpApStatusHandler.VP_ENABLE, 0));
            }
        }
    }

    static /* synthetic */ int access$072(HwVpApStatusHandler x0, int x1) {
        int i = x0.VP_Flag & x1;
        x0.VP_Flag = i;
        return i;
    }

    static /* synthetic */ int access$076(HwVpApStatusHandler x0, int x1) {
        int i = x0.VP_Flag | x1;
        x0.VP_Flag = i;
        return i;
    }

    protected HwVpApStatusHandler(GsmCdmaPhone phone) {
        if (phone != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.TAG);
            stringBuilder.append("[SUB");
            stringBuilder.append(phone.getPhoneId());
            stringBuilder.append("]");
            this.TAG = stringBuilder.toString();
        }
        Log.i(this.TAG, "HwVpApStatusHandler.constructor!");
        if (phone != null) {
            this.mPhone = phone;
            this.mReceiver = new EventReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            filter.addAction(EventReceiver.ACTION_TETHER_STATE_CHANGED);
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.SCREEN_OFF");
            this.mPhone.getContext().registerReceiver(this.mReceiver, filter);
            this.mAgpsAppObserver = new AgpsAppObserver(this);
            this.mPhone.getContext().getContentResolver().registerContentObserver(Global.getUriFor("agps_app_started_navigation"), false, this.mAgpsAppObserver);
            this.VP_Flag = ((PowerManager) this.mPhone.getContext().getSystemService("power")).isScreenOn() ? this.VP_Flag | VP_SCREEN_ON : this.VP_Flag & -129;
            this.VP_Flag |= VP_ENABLE;
            sendMessage(obtainMessage(12, this.VP_Flag, 0));
        }
    }

    public void dispose() {
        Log.i(this.TAG, "HwVpApStatusHandler.dispose!");
        if (this.mPhone != null) {
            this.mPhone.getContext().unregisterReceiver(this.mReceiver);
            this.mPhone.getContext().getContentResolver().unregisterContentObserver(this.mAgpsAppObserver);
        }
    }

    public void handleMessage(Message msg) {
        String str = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleMessage = ");
        stringBuilder.append(msg.what);
        Log.i(str, stringBuilder.toString());
        String str2;
        StringBuilder stringBuilder2;
        switch (msg.what) {
            case 12:
                int vp_mask = msg.arg1;
                str2 = this.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("VP-bitMask=");
                stringBuilder2.append(vp_mask);
                Log.i(str2, stringBuilder2.toString());
                setVpMask(vp_mask);
                return;
            case 13:
                Log.i(this.TAG, "MSG_AP_STATUS_SEND_DONE");
                AsyncResult ar = msg.obj;
                if (ar == null) {
                    Log.i(this.TAG, "MSG_AP_STATUS_SEND_DONE: null pointer ");
                    return;
                } else if (ar.exception != null) {
                    str2 = this.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("MSG_AP_STATUS_SEND_DONE: failed ");
                    stringBuilder2.append(ar.exception);
                    Log.i(str2, stringBuilder2.toString());
                    return;
                } else {
                    return;
                }
            default:
                return;
        }
    }

    private void setVpMask(int vp_mask) {
        if (mIsSurpportNvFunc) {
            setVpMaskByNvFunction(vp_mask);
        } else {
            this.mPhone.mCi.setVpMask(vp_mask, obtainMessage(13));
        }
    }

    private void setVpMaskByNvFunction(int vp_mask) {
        if (this.mSetVPEvent == null) {
            initVPFunc();
        }
        if (this.mSetVPEvent != null) {
            String str;
            StringBuilder stringBuilder;
            try {
                int ret = ((Integer) this.mSetVPEvent.invoke(null, new Object[]{Integer.valueOf(vp_mask)})).intValue();
                str = this.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("call com.huawei.android.hwnv.HWNVFuncation.setVPEvent()  return:");
                stringBuilder.append(ret);
                Log.i(str, stringBuilder.toString());
                return;
            } catch (IllegalArgumentException e) {
                str = this.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(": setVpMaskByNvFunction IllegalArgumentException is ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return;
            } catch (IllegalAccessException e2) {
                str = this.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(": setVpMaskByNvFunction IllegalAccessException is ");
                stringBuilder.append(e2);
                Log.e(str, stringBuilder.toString());
                return;
            } catch (InvocationTargetException e3) {
                str = this.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(": setVpMaskByNvFunction InvocationTargetException is ");
                stringBuilder.append(e3);
                Log.e(str, stringBuilder.toString());
                return;
            }
        }
        Log.e(this.TAG, "com.huawei.android.hwnv.HWNVFuncation.setVPEvent() not found");
    }

    private void initVPFunc() {
        Log.i(this.TAG, "initVPFunc()");
        try {
            Class classType = HwQualcommRIL.getHWNV();
            if (classType != null) {
                Log.i(this.TAG, "found com.huawei.android.hwnv.HWNVFuncation");
                this.mSetVPEvent = classType.getMethod("setVPEvent", new Class[]{Integer.TYPE});
            } else {
                Log.e(this.TAG, "No found com.huawei.android.hwnv.HWNVFuncation");
            }
            if (this.mSetVPEvent != null) {
                Log.i(this.TAG, "found setVPEvent() interface");
            } else {
                Log.e(this.TAG, "No found setVPEvent() interface");
            }
        } catch (NoSuchMethodException e) {
            String str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(": initVPFunc NoSuchMethodException is ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }
}
