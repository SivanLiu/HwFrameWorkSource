package com.android.ims;

import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;
import android.telephony.ims.ImsReasonInfo;
import com.android.ims.AbstractImsUt.ImsUtReference;
import com.android.ims.internal.IImsUt;
import com.android.internal.telephony.HuaweiTelephonyConfigs;

public class HwImsUt implements ImsUtReference {
    private static final int CODE_IS_SUPPORT_CFT = 2001;
    private static final int CODE_IS_UT_ENABLE = 2002;
    private static final int CODE_UPDATE_CALLBARRING_OPT = 2004;
    private static final int CODE_UPDATE_CFU_TIMER = 2003;
    private static final boolean DBG = true;
    private static final String DESCRIPTOR = "com.android.ims.internal.IImsUt";
    private static final String IMS_UT_SERVICE_NAME = "ims_ut";
    private static final String TAG = "HwImsUt";
    private static final boolean isHisiPlateform = HuaweiTelephonyConfigs.isHisiPlatform();
    private ImsUt mImsUt;
    private int mPhoneId = 0;
    private IImsUt miUt;

    public HwImsUt(ImsUt imsUt) {
        this.mImsUt = imsUt;
    }

    public HwImsUt(ImsUt imsUt, int phoneId) {
        this.mImsUt = imsUt;
        this.mPhoneId = phoneId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwImsUt:imsUt = ");
        stringBuilder.append(imsUt);
        stringBuilder.append(", mPhoneId = ");
        stringBuilder.append(this.mPhoneId);
        log(stringBuilder.toString());
    }

    public HwImsUt(IImsUt iUt, ImsUt imsUt, int phoneId) {
        this.miUt = iUt;
        this.mImsUt = imsUt;
        this.mPhoneId = phoneId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwImsUt:miUt=");
        stringBuilder.append(this.miUt);
        stringBuilder.append(",mImsUt = ");
        stringBuilder.append(this.mImsUt);
        stringBuilder.append(", mPhoneId = ");
        stringBuilder.append(this.mPhoneId);
        log(stringBuilder.toString());
    }

    public boolean isSupportCFT() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSupportCFT:isHisiPlateform i ");
        stringBuilder.append(isHisiPlateform);
        log(stringBuilder.toString());
        boolean z = false;
        if (isHisiPlateform) {
            Parcel _data = Parcel.obtain();
            Parcel _reply = Parcel.obtain();
            IBinder b = ServiceManager.getService(IMS_UT_SERVICE_NAME);
            log("isSupportCFT");
            if (b != null) {
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(this.mPhoneId);
                    b.transact(CODE_IS_SUPPORT_CFT, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 1) {
                        z = DBG;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return z;
                } catch (RemoteException localRemoteException) {
                    localRemoteException.printStackTrace();
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            } else {
                log("isSupportCFT - can't get ims_ut service");
                _reply.recycle();
                _data.recycle();
                return false;
            }
        } else if (this.miUt == null) {
            loge("The device is not Hisi plateform,but miUt is null");
            return false;
        } else {
            try {
                return this.miUt.isSupportCFT();
            } catch (RemoteException localRemoteException2) {
                localRemoteException2.printStackTrace();
                return false;
            }
        }
    }

    public boolean isUtEnable() {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService(IMS_UT_SERVICE_NAME);
        log("isUtEnable");
        boolean z = false;
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR);
                _data.writeInt(this.mPhoneId);
                b.transact(CODE_IS_UT_ENABLE, _data, _reply, 0);
                _reply.readException();
                if (_reply.readInt() == 1) {
                    z = DBG;
                }
                _reply.recycle();
                _data.recycle();
                return z;
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } catch (Throwable th) {
                _reply.recycle();
                _data.recycle();
            }
        } else {
            log("isUtEnable - can't get ims_ut service");
            _reply.recycle();
            _data.recycle();
            return false;
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:46:0x00d7=Splitter:B:46:0x00d7, B:41:0x00cd=Splitter:B:41:0x00cd} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00fc A:{Catch:{ all -> 0x0109, all -> 0x011c }} */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00ee A:{Catch:{ all -> 0x0109, all -> 0x011c }} */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00ee A:{Catch:{ all -> 0x0109, all -> 0x011c }} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00fc A:{Catch:{ all -> 0x0109, all -> 0x011c }} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00fc A:{Catch:{ all -> 0x0109, all -> 0x011c }} */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00ee A:{Catch:{ all -> 0x0109, all -> 0x011c }} */
    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:41:0x00cd, B:49:0x00e2] */
    /* JADX WARNING: Missing block: B:60:0x0109, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:70:0x011c, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateCallBarringOption(String password, int cbType, boolean enable, int serviceClass, Message result, String[] barrList) {
        RemoteException localRemoteException;
        String[] strArr;
        IBinder iBinder;
        Throwable th;
        String str;
        int i = cbType;
        boolean z = enable;
        int i2 = serviceClass;
        Message message = result;
        synchronized (this.mImsUt.mLockObj) {
            int id = -1;
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateCallBarringOption:isHisiPlateform i ");
                stringBuilder.append(isHisiPlateform);
                stringBuilder.append(", password= ***, cbType= ");
                stringBuilder.append(i);
                stringBuilder.append(", enable= ");
                stringBuilder.append(z);
                stringBuilder.append(", serviceclass = ");
                stringBuilder.append(i2);
                log(stringBuilder.toString());
                if (isHisiPlateform) {
                    Parcel _data = Parcel.obtain();
                    Parcel _reply = Parcel.obtain();
                    IBinder b = ServiceManager.getService(IMS_UT_SERVICE_NAME);
                    if (b != null) {
                        try {
                            _data.writeInterfaceToken(DESCRIPTOR);
                            _data.writeInt(this.mPhoneId);
                            try {
                                _data.writeString(password);
                                _data.writeInt(i);
                                _data.writeInt(z);
                                _data.writeInt(i2);
                            } catch (RemoteException e) {
                                localRemoteException = e;
                                strArr = barrList;
                                try {
                                    localRemoteException.printStackTrace();
                                    iBinder = b;
                                    this.mImsUt.sendFailureReport(message, new ImsReasonInfo(802, 0));
                                    _reply.recycle();
                                    _data.recycle();
                                    if (id >= 0) {
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    iBinder = b;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                strArr = barrList;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                            try {
                                _data.writeStringArray(barrList);
                                b.transact(CODE_UPDATE_CALLBARRING_OPT, _data, _reply, 0);
                                _reply.readException();
                                id = _reply.readInt();
                            } catch (RemoteException e2) {
                                localRemoteException = e2;
                                localRemoteException.printStackTrace();
                                iBinder = b;
                                this.mImsUt.sendFailureReport(message, new ImsReasonInfo(802, 0));
                                _reply.recycle();
                                _data.recycle();
                                if (id >= 0) {
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (RemoteException e3) {
                            localRemoteException = e3;
                            str = password;
                            strArr = barrList;
                            localRemoteException.printStackTrace();
                            iBinder = b;
                            this.mImsUt.sendFailureReport(message, new ImsReasonInfo(802, 0));
                            _reply.recycle();
                            _data.recycle();
                            if (id >= 0) {
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            str = password;
                            strArr = barrList;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    }
                    str = password;
                    strArr = barrList;
                    log("updateCallBarringOption - can't get ims_ut service");
                    _reply.recycle();
                    _data.recycle();
                } else if (this.miUt == null) {
                    loge("The device is not Hisi plateform,but miUt is null");
                    return;
                } else {
                    id = this.miUt.updateCallBarringOption(password, i, z, i2, barrList);
                    str = password;
                    strArr = barrList;
                }
            } catch (RemoteException localRemoteException2) {
                localRemoteException2.printStackTrace();
                this.mImsUt.sendFailureReport(message, new ImsReasonInfo(802, 0));
            } catch (Throwable th6) {
                th = th6;
                str = password;
                strArr = barrList;
                throw th;
            }
            if (id >= 0) {
                this.mImsUt.sendFailureReport(message, new ImsReasonInfo(802, 0));
                return;
            }
            this.mImsUt.mPendingCmds.put(Integer.valueOf(id), message);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:62:0x0159 A:{Catch:{ all -> 0x016f, all -> 0x0177 }} */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x014c A:{Catch:{ all -> 0x016f, all -> 0x0177 }} */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x014c A:{Catch:{ all -> 0x016f, all -> 0x0177 }} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x0159 A:{Catch:{ all -> 0x016f, all -> 0x0177 }} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x0159 A:{Catch:{ all -> 0x016f, all -> 0x0177 }} */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x014c A:{Catch:{ all -> 0x016f, all -> 0x0177 }} */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x014c A:{Catch:{ all -> 0x016f, all -> 0x0177 }} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x0159 A:{Catch:{ all -> 0x016f, all -> 0x0177 }} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x0114 A:{Splitter:B:34:0x00dd, ExcHandler: all (th java.lang.Throwable), Catch:{ RemoteException -> 0x0132 }} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x0159 A:{Catch:{ all -> 0x016f, all -> 0x0177 }} */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x014c A:{Catch:{ all -> 0x016f, all -> 0x0177 }} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:42:0x0110, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:43:0x0111, code skipped:
            r8 = r26;
     */
    /* JADX WARNING: Missing block: B:44:0x0114, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:45:0x0115, code skipped:
            r8 = r26;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateCallForwardUncondTimer(int startHour, int startMinute, int endHour, int endMinute, int action, int condition, String number, Message result) {
        Message message;
        RemoteException localRemoteException;
        int id;
        Throwable th;
        String str;
        int i = startHour;
        int i2 = startMinute;
        int i3 = endHour;
        int i4 = endMinute;
        int i5 = action;
        int i6 = condition;
        Message message2 = result;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateCallForwardUncondTimer :: , action=");
        stringBuilder.append(i5);
        stringBuilder.append(", condition=");
        stringBuilder.append(i6);
        stringBuilder.append(", startHour=");
        stringBuilder.append(i);
        stringBuilder.append(", startMinute=");
        stringBuilder.append(i2);
        stringBuilder.append(", endHour=");
        stringBuilder.append(i3);
        stringBuilder.append(", endMinute=");
        stringBuilder.append(i4);
        log(stringBuilder.toString());
        Object obj = this.mImsUt.mLockObj;
        synchronized (obj) {
            int id2 = -1;
            Object obj2;
            try {
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateCallForwardUncondTimer:isHisiPlateform i ");
                stringBuilder.append(isHisiPlateform);
                log(stringBuilder.toString());
                if (isHisiPlateform) {
                    int i7 = 0;
                    i3 = 802;
                    obj2 = obj;
                    message = message2;
                    Parcel _data = Parcel.obtain();
                    Parcel _reply = Parcel.obtain();
                    IBinder b = ServiceManager.getService(IMS_UT_SERVICE_NAME);
                    int i8;
                    if (b != null) {
                        try {
                            _data.writeInterfaceToken(DESCRIPTOR);
                            _data.writeInt(this.mPhoneId);
                            _data.writeInt(i);
                            _data.writeInt(i2);
                            i8 = i7;
                            _data.writeInt(endHour);
                            _data.writeInt(i4);
                            _data.writeInt(i5);
                            _data.writeInt(i6);
                            try {
                                _data.writeString(number);
                                b.transact(CODE_UPDATE_CFU_TIMER, _data, _reply, i8);
                                _reply.readException();
                                id2 = _reply.readInt();
                            } catch (RemoteException e) {
                                localRemoteException = e;
                                try {
                                    localRemoteException.printStackTrace();
                                    this.mImsUt.sendFailureReport(message, new ImsReasonInfo(i3, 0));
                                    _reply.recycle();
                                    _data.recycle();
                                    id = id2;
                                    if (id >= 0) {
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            }
                        } catch (RemoteException e2) {
                            localRemoteException = e2;
                            str = number;
                            i8 = i7;
                            i7 = endHour;
                            localRemoteException.printStackTrace();
                            this.mImsUt.sendFailureReport(message, new ImsReasonInfo(i3, 0));
                            _reply.recycle();
                            _data.recycle();
                            id = id2;
                            if (id >= 0) {
                            }
                        } catch (Throwable th3) {
                        }
                    } else {
                        str = number;
                        i8 = i7;
                        i7 = endHour;
                        log("updateCallForwardUncondTimer - can't get ims_ut service");
                    }
                    _reply.recycle();
                    _data.recycle();
                    id = id2;
                } else if (this.miUt == null) {
                    loge("The device is not Hisi plateform,but miUt is null");
                } else {
                    int i9;
                    try {
                        i9 = i3;
                        Object obj3 = null;
                        i3 = 802;
                        obj2 = obj;
                    } catch (RemoteException e3) {
                        localRemoteException = e3;
                        i3 = 802;
                        obj2 = obj;
                        try {
                            localRemoteException.printStackTrace();
                            message = result;
                            this.mImsUt.sendFailureReport(message, new ImsReasonInfo(i3, 0));
                            str = number;
                            id = id2;
                            if (id >= 0) {
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            str = number;
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        obj2 = obj;
                        str = number;
                        message = message2;
                        throw th;
                    }
                    try {
                        id = this.miUt.updateCallForwardUncondTimer(i, i2, i9, i4, i5, i6, number);
                        str = number;
                        message = result;
                    } catch (RemoteException e4) {
                        localRemoteException = e4;
                        localRemoteException.printStackTrace();
                        message = result;
                        this.mImsUt.sendFailureReport(message, new ImsReasonInfo(i3, 0));
                        str = number;
                        id = id2;
                        if (id >= 0) {
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        str = number;
                        message = result;
                        throw th;
                    }
                }
                if (id >= 0) {
                    this.mImsUt.sendFailureReport(message, new ImsReasonInfo(i3, 0));
                    return;
                }
                this.mImsUt.mPendingCmds.put(Integer.valueOf(id), message);
            } catch (Throwable th7) {
                th = th7;
            }
        }
    }

    public Message popUtMessage(int id) {
        Message msg;
        Integer key = Integer.valueOf(id);
        synchronized (this.mImsUt.mLockObj) {
            msg = (Message) this.mImsUt.mPendingCmds.get(key);
            this.mImsUt.mPendingCmds.remove(key);
        }
        return msg;
    }

    private void log(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwImsUt[");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append("]");
        Rlog.d(stringBuilder.toString(), s);
    }

    private void loge(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwImsUt[");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append("]");
        Rlog.e(stringBuilder.toString(), s);
    }
}
