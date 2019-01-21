package com.huawei.android.telephony;

import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseCallState;
import android.telephony.SubscriptionManager;
import com.huawei.android.util.NoExtAPIException;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class PhoneStateListenerEx extends PhoneStateListener {
    public static final int LISTEN_PRECISE_CALL_STATE = 2048;
    protected int mSubscription = 0;

    public PhoneStateListenerEx(int subscription) {
        this.mSubscription = subscription;
    }

    public PhoneStateListenerEx(int subId, Looper looper) {
        super(Integer.valueOf(subId), looper);
    }

    public static int getSubscription(PhoneStateListener obj) {
        try {
            final Field filed = Class.forName("android.telephony.PhoneStateListener").getDeclaredField("mSubId");
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    filed.setAccessible(true);
                    return null;
                }
            });
            return SubscriptionManager.getSlotIndex(((Integer) filed.get(obj)).intValue());
        } catch (RuntimeException e) {
            throw new NoExtAPIException("method not supported.");
        } catch (Exception e2) {
            throw new NoExtAPIException("method not supported.");
        }
    }

    public static void setSubscription(PhoneStateListener obj, int subscription) {
        try {
            final Field filed = Class.forName("android.telephony.PhoneStateListener").getDeclaredField("mSubId");
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    filed.setAccessible(true);
                    return null;
                }
            });
            filed.set(obj, Integer.valueOf(subscription));
        } catch (RuntimeException e) {
            throw new NoExtAPIException("method not supported.");
        } catch (Exception e2) {
            throw new NoExtAPIException("method not supported.");
        }
    }

    public void onPreciseCallStateChangedCallBack(PreciseCallStateEx callState) {
    }

    public void onPreciseCallStateChanged(PreciseCallState callState) {
        onPreciseCallStateChangedCallBack(new PreciseCallStateEx(callState));
    }

    public static int getListenCarrierNetwoekChange() {
        return 65536;
    }
}
