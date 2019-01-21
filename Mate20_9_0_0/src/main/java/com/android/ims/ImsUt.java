package com.android.ims;

import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RemoteException;
import android.telephony.Rlog;
import android.telephony.ims.ImsCallForwardInfo;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsSsData;
import android.telephony.ims.ImsSsInfo;
import com.android.ims.internal.IImsUt;
import com.android.ims.internal.IImsUtListener.Stub;
import java.util.HashMap;
import java.util.Map.Entry;

public class ImsUt extends AbstractImsUt implements ImsUtInterface {
    public static final String CATEGORY_CB = "CB";
    public static final String CATEGORY_CDIV = "CDIV";
    public static final String CATEGORY_CONF = "CONF";
    public static final String CATEGORY_CW = "CW";
    public static final String CATEGORY_OIP = "OIP";
    public static final String CATEGORY_OIR = "OIR";
    public static final String CATEGORY_TIP = "TIP";
    public static final String CATEGORY_TIR = "TIR";
    private static final boolean DBG = true;
    public static final String KEY_ACTION = "action";
    public static final String KEY_CATEGORY = "category";
    private static final int SERVICE_CLASS_NONE = 0;
    private static final int SERVICE_CLASS_VOICE = 1;
    private static final String TAG = "ImsUt";
    public Object mLockObj = new Object();
    public HashMap<Integer, Message> mPendingCmds = new HashMap();
    private int mPhoneId = 0;
    private Registrant mSsIndicationRegistrant;
    private final IImsUt miUt;

    private class IImsUtListenerProxy extends Stub {
        private IImsUtListenerProxy() {
        }

        public void utConfigurationUpdated(IImsUt ut, int id) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport((Message) ImsUt.this.mPendingCmds.get(key));
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void utConfigurationUpdateFailed(IImsUt ut, int id, ImsReasonInfo error) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendFailureReport((Message) ImsUt.this.mPendingCmds.get(key), error);
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void utConfigurationQueried(IImsUt ut, int id, Bundle ssInfo) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport((Message) ImsUt.this.mPendingCmds.get(key), ssInfo);
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void utConfigurationQueryFailed(IImsUt ut, int id, ImsReasonInfo error) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendFailureReport((Message) ImsUt.this.mPendingCmds.get(key), error);
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void utConfigurationCallBarringQueried(IImsUt ut, int id, ImsSsInfo[] cbInfo) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport((Message) ImsUt.this.mPendingCmds.get(key), cbInfo);
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void utConfigurationCallForwardQueried(IImsUt ut, int id, ImsCallForwardInfo[] cfInfo) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport((Message) ImsUt.this.mPendingCmds.get(key), cfInfo);
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void utConfigurationCallWaitingQueried(IImsUt ut, int id, ImsSsInfo[] cwInfo) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport((Message) ImsUt.this.mPendingCmds.get(key), cwInfo);
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void onSupplementaryServiceIndication(ImsSsData ssData) {
            if (ImsUt.this.mSsIndicationRegistrant != null) {
                ImsUt.this.mSsIndicationRegistrant.notifyResult(ssData);
            }
        }
    }

    public ImsUt(IImsUt iUt) {
        this.miUt = iUt;
        if (this.miUt != null) {
            try {
                this.miUt.setListener(new IImsUtListenerProxy());
            } catch (RemoteException e) {
            }
        }
    }

    public ImsUt(IImsUt iUt, int phoneId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ImsUt :: iUt =");
        stringBuilder.append(iUt);
        stringBuilder.append(", phoneId=");
        stringBuilder.append(phoneId);
        log(stringBuilder.toString());
        this.mPhoneId = phoneId;
        this.miUt = iUt;
        this.mReference = new HwImsUt(this.miUt, this, phoneId);
        if (this.miUt != null) {
            try {
                this.miUt.setListener(new IImsUtListenerProxy());
            } catch (RemoteException e) {
                loge("miUt setListener failed");
            }
        }
    }

    public void close() {
        synchronized (this.mLockObj) {
            if (this.miUt != null) {
                try {
                    this.miUt.close();
                } catch (RemoteException e) {
                }
            }
            if (!this.mPendingCmds.isEmpty()) {
                for (Entry<Integer, Message> entry : (Entry[]) this.mPendingCmds.entrySet().toArray(new Entry[this.mPendingCmds.size()])) {
                    sendFailureReport((Message) entry.getValue(), new ImsReasonInfo(802, 0));
                }
                this.mPendingCmds.clear();
            }
        }
    }

    public void registerForSuppServiceIndication(Handler h, int what, Object obj) {
        this.mSsIndicationRegistrant = new Registrant(h, what, obj);
    }

    public void unregisterForSuppServiceIndication(Handler h) {
        this.mSsIndicationRegistrant.clear();
    }

    public void queryCallBarring(int cbType, Message result) {
        queryCallBarring(cbType, result, 0);
    }

    public void queryCallBarring(int cbType, Message result, int serviceClass) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("queryCallBarring :: Ut=");
        stringBuilder.append(this.miUt);
        stringBuilder.append(", cbType=");
        stringBuilder.append(cbType);
        stringBuilder.append(", serviceClass=");
        stringBuilder.append(serviceClass);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.queryCallBarringForServiceClass(cbType, serviceClass);
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void queryCallForward(int condition, String number, Message result) {
        queryCallForwardForServiceClass(condition, number, 0, result);
    }

    public void queryCallForwardForServiceClass(int condition, String number, int serviceClass, Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("queryCallForward :: Ut=");
        stringBuilder.append(this.miUt);
        stringBuilder.append(", condition=");
        stringBuilder.append(condition);
        stringBuilder.append(", serviceClass:");
        stringBuilder.append(serviceClass);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.queryCallForwardForServiceClass(condition, number, serviceClass);
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void queryCallWaiting(Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("queryCallWaiting :: Ut=");
        stringBuilder.append(this.miUt);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.queryCallWaiting();
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void queryCLIR(Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("queryCLIR :: Ut=");
        stringBuilder.append(this.miUt);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.queryCLIR();
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void queryCLIP(Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("queryCLIP :: Ut=");
        stringBuilder.append(this.miUt);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.queryCLIP();
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void queryCOLR(Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("queryCOLR :: Ut=");
        stringBuilder.append(this.miUt);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.queryCOLR();
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void queryCOLP(Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("queryCOLP :: Ut=");
        stringBuilder.append(this.miUt);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.queryCOLP();
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void updateCallBarring(int cbType, int action, Message result, String[] barrList) {
        updateCallBarring(cbType, action, result, barrList, 0);
    }

    public void updateCallBarring(int cbType, int action, Message result, String[] barrList, int serviceClass) {
        if (barrList != null) {
            String bList = new String();
            for (String append : barrList) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(append);
                stringBuilder.append(" ");
                bList.concat(stringBuilder.toString());
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateCallBarring :: Ut=");
            stringBuilder2.append(this.miUt);
            stringBuilder2.append(", cbType=");
            stringBuilder2.append(cbType);
            stringBuilder2.append(", action=");
            stringBuilder2.append(action);
            stringBuilder2.append(", serviceClass=");
            stringBuilder2.append(serviceClass);
            stringBuilder2.append(", barrList=");
            stringBuilder2.append(bList);
            log(stringBuilder2.toString());
        } else {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("updateCallBarring :: Ut=");
            stringBuilder3.append(this.miUt);
            stringBuilder3.append(", cbType=");
            stringBuilder3.append(cbType);
            stringBuilder3.append(", action=");
            stringBuilder3.append(action);
            stringBuilder3.append(", serviceClass=");
            stringBuilder3.append(serviceClass);
            log(stringBuilder3.toString());
        }
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.updateCallBarringForServiceClass(cbType, action, barrList, serviceClass);
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void updateCallForward(int action, int condition, String number, int serviceClass, int timeSeconds, Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateCallForward :: Ut=");
        stringBuilder.append(this.miUt);
        stringBuilder.append(", action=");
        stringBuilder.append(action);
        stringBuilder.append(", condition=");
        stringBuilder.append(condition);
        stringBuilder.append(", serviceClass=");
        stringBuilder.append(serviceClass);
        stringBuilder.append(", timeSeconds=");
        stringBuilder.append(timeSeconds);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.updateCallForward(action, condition, number, serviceClass, timeSeconds);
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void updateCallWaiting(boolean enable, int serviceClass, Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateCallWaiting :: Ut=");
        stringBuilder.append(this.miUt);
        stringBuilder.append(", enable=");
        stringBuilder.append(enable);
        stringBuilder.append(",serviceClass=");
        stringBuilder.append(serviceClass);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.updateCallWaiting(enable, serviceClass);
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void updateCLIR(int clirMode, Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateCLIR :: Ut=");
        stringBuilder.append(this.miUt);
        stringBuilder.append(", clirMode=");
        stringBuilder.append(clirMode);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.updateCLIR(clirMode);
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void updateCLIP(boolean enable, Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateCLIP :: Ut=");
        stringBuilder.append(this.miUt);
        stringBuilder.append(", enable=");
        stringBuilder.append(enable);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.updateCLIP(enable);
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void updateCOLR(int presentation, Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateCOLR :: Ut=");
        stringBuilder.append(this.miUt);
        stringBuilder.append(", presentation=");
        stringBuilder.append(presentation);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.updateCOLR(presentation);
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void updateCOLP(boolean enable, Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateCallWaiting :: Ut=");
        stringBuilder.append(this.miUt);
        stringBuilder.append(", enable=");
        stringBuilder.append(enable);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.updateCOLP(enable);
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public boolean isBinderAlive() {
        return this.miUt.asBinder().isBinderAlive();
    }

    public void processECT() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processECT :: Ut=");
        stringBuilder.append(this.miUt);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                this.miUt.processECT();
            } catch (RemoteException e) {
                sendFailureReport(null, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void transact(Bundle ssInfo, Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("transact :: Ut=");
        stringBuilder.append(this.miUt);
        stringBuilder.append(", ssInfo=");
        stringBuilder.append(ssInfo);
        log(stringBuilder.toString());
        synchronized (this.mLockObj) {
            try {
                int id = this.miUt.transact(ssInfo);
                if (id < 0) {
                    sendFailureReport(result, new ImsReasonInfo(802, 0));
                    return;
                }
                this.mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
        }
    }

    public void sendFailureReport(Message result, ImsReasonInfo error) {
        if (result != null && error != null) {
            String errorString;
            if (error.mExtraMessage == null) {
                errorString = Resources.getSystem().getString(17040531);
            } else {
                errorString = new String(error.mExtraMessage);
            }
            AsyncResult.forMessage(result, null, new ImsException(errorString, error.mCode));
            result.sendToTarget();
        }
    }

    private void sendSuccessReport(Message result) {
        if (result != null) {
            AsyncResult.forMessage(result, null, null);
            result.sendToTarget();
        }
    }

    private void sendSuccessReport(Message result, Object ssInfo) {
        if (result != null) {
            AsyncResult.forMessage(result, ssInfo, null);
            result.sendToTarget();
        }
    }

    private void log(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ImsUt[");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append("]");
        Rlog.d(stringBuilder.toString(), s);
    }

    private void loge(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ImsUt[");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append("]");
        Rlog.e(stringBuilder.toString(), s);
    }

    private void loge(String s, Throwable t) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ImsUt[");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append("]");
        Rlog.e(stringBuilder.toString(), s, t);
    }

    public String getUtIMPUFromNetwork() {
        String impu = null;
        synchronized (this.mLockObj) {
            try {
                impu = this.miUt.getUtIMPUFromNetwork();
            } catch (RemoteException e) {
                sendFailureReport(null, new ImsReasonInfo(802, 0));
            }
        }
        return impu;
    }
}
