package com.huawei.android.pushagent.utils.tools;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.b.a;

public class b {
    private static boolean gc = false;
    private static boolean gd = false;

    /* JADX WARNING: Unknown top exception splitter block from list: {B:18:0x0053=Splitter:B:18:0x0053, B:26:0x0069=Splitter:B:26:0x0069} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0089  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x008e  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005e  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0063  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0096  */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x009b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void sj(String str) {
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        a.sv("PushLog3414", "ctrlScoket registerPackage " + str);
        if (!TextUtils.isEmpty(str)) {
            Parcel obtain;
            try {
                IBinder service = ServiceManager.getService("connectivity");
                if (service == null) {
                    a.su("PushLog3414", "get connectivity service failed ");
                    return;
                }
                obtain = Parcel.obtain();
                try {
                    obtain.writeString(str);
                    parcel = Parcel.obtain();
                    service.transact(1001, obtain, parcel, 0);
                    if (obtain != null) {
                        obtain.recycle();
                    }
                    if (parcel != null) {
                        parcel.recycle();
                    }
                } catch (RemoteException e3) {
                    e = e3;
                    a.su("PushLog3414", "registerPackage error:" + e.getMessage());
                    if (obtain != null) {
                    }
                    if (parcel != null) {
                    }
                } catch (Exception e4) {
                    e2 = e4;
                    try {
                        a.sw("PushLog3414", "registerPackage error:", e2);
                        if (obtain != null) {
                        }
                        if (parcel != null) {
                        }
                    } catch (Throwable th) {
                        e2 = th;
                        if (obtain != null) {
                        }
                        if (parcel != null) {
                        }
                        throw e2;
                    }
                }
            } catch (RemoteException e5) {
                e = e5;
                obtain = null;
                a.su("PushLog3414", "registerPackage error:" + e.getMessage());
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (Exception e6) {
                e2 = e6;
                obtain = null;
                a.sw("PushLog3414", "registerPackage error:", e2);
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (Throwable th2) {
                e2 = th2;
                obtain = null;
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
                throw e2;
            }
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:25:0x0060=Splitter:B:25:0x0060, B:17:0x004a=Splitter:B:17:0x004a} */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x008d  */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0080  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0085  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0055  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x005a  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void sh(String str) {
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        a.sv("PushLog3414", "ctrlScoket deregisterPackage " + str);
        if (!TextUtils.isEmpty(str)) {
            Parcel obtain;
            try {
                IBinder service = ServiceManager.getService("connectivity");
                if (service != null) {
                    obtain = Parcel.obtain();
                    try {
                        obtain.writeString(str);
                        parcel = Parcel.obtain();
                        service.transact(1002, obtain, parcel, 0);
                        if (obtain != null) {
                            obtain.recycle();
                        }
                        if (parcel != null) {
                            parcel.recycle();
                        }
                    } catch (RemoteException e3) {
                        e = e3;
                        a.su("PushLog3414", "deregisterPackage error:" + e.getMessage());
                        if (obtain != null) {
                        }
                        if (parcel != null) {
                        }
                    } catch (Exception e4) {
                        e2 = e4;
                        try {
                            a.sw("PushLog3414", "deregisterPackage error:", e2);
                            if (obtain != null) {
                            }
                            if (parcel != null) {
                            }
                        } catch (Throwable th) {
                            e2 = th;
                            if (obtain != null) {
                                obtain.recycle();
                            }
                            if (parcel != null) {
                                parcel.recycle();
                            }
                            throw e2;
                        }
                    }
                }
            } catch (RemoteException e5) {
                e = e5;
                obtain = null;
                a.su("PushLog3414", "deregisterPackage error:" + e.getMessage());
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (Exception e6) {
                e2 = e6;
                obtain = null;
                a.sw("PushLog3414", "deregisterPackage error:", e2);
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (Throwable th2) {
                e2 = th2;
                obtain = null;
                if (obtain != null) {
                }
                if (parcel != null) {
                }
                throw e2;
            }
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:24:0x0080=Splitter:B:24:0x0080, B:16:0x006a=Splitter:B:16:0x006a} */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x00ad  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00b2  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x00a0  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00a5  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0075  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x007a  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void sg(int i, int i2) {
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        a.sv("PushLog3414", "ctrlSocket cmd is " + i + ", param is " + i2);
        Parcel obtain;
        try {
            IBinder service = ServiceManager.getService("connectivity");
            if (service == null) {
                a.sx("PushLog3414", "get connectivity service failed ");
                return;
            }
            obtain = Parcel.obtain();
            try {
                obtain.writeInt(Process.myPid());
                obtain.writeInt(i);
                obtain.writeInt(i2);
                parcel = Parcel.obtain();
                service.transact(1003, obtain, parcel, 0);
                a.sv("PushLog3414", "ctrlSocket success");
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (RemoteException e3) {
                e = e3;
                a.su("PushLog3414", "ctrlSocket error:" + e.getMessage());
                if (obtain != null) {
                }
                if (parcel != null) {
                }
            } catch (Exception e4) {
                e2 = e4;
                try {
                    a.sw("PushLog3414", "ctrlSocket error:", e2);
                    if (obtain != null) {
                    }
                    if (parcel != null) {
                    }
                } catch (Throwable th) {
                    e2 = th;
                    if (obtain != null) {
                        obtain.recycle();
                    }
                    if (parcel != null) {
                        parcel.recycle();
                    }
                    throw e2;
                }
            }
        } catch (RemoteException e5) {
            e = e5;
            obtain = null;
            a.su("PushLog3414", "ctrlSocket error:" + e.getMessage());
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
        } catch (Exception e6) {
            e2 = e6;
            obtain = null;
            a.sw("PushLog3414", "ctrlSocket error:", e2);
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
        } catch (Throwable th2) {
            e2 = th2;
            obtain = null;
            if (obtain != null) {
            }
            if (parcel != null) {
            }
            throw e2;
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:17:0x0050=Splitter:B:17:0x0050, B:26:0x0067=Splitter:B:26:0x0067} */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x005b  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0060  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0094  */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x0099  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static String si() {
        String readString;
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        String str = "";
        Parcel obtain;
        try {
            IBinder service = ServiceManager.getService("connectivity");
            if (service == null) {
                a.sx("PushLog3414", "get connectivity service failed ");
                return str;
            }
            obtain = Parcel.obtain();
            try {
                parcel = Parcel.obtain();
                service.transact(1006, obtain, parcel, 0);
                readString = parcel.readString();
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (RemoteException e3) {
                e = e3;
            } catch (Exception e4) {
                e2 = e4;
                try {
                    a.sw("PushLog3414", "getCtrlSocketVersion error:", e2);
                    if (obtain != null) {
                    }
                    if (parcel != null) {
                    }
                    readString = str;
                    a.sv("PushLog3414", "ctrlSocket version is:" + readString);
                    return readString;
                } catch (Throwable th) {
                    e2 = th;
                    if (obtain != null) {
                    }
                    if (parcel != null) {
                    }
                    throw e2;
                }
            }
            a.sv("PushLog3414", "ctrlSocket version is:" + readString);
            return readString;
        } catch (RemoteException e5) {
            e = e5;
            obtain = null;
            a.su("PushLog3414", "getCtrlSocketVersion error:" + e.getMessage());
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            readString = str;
            a.sv("PushLog3414", "ctrlSocket version is:" + readString);
            return readString;
        } catch (Exception e6) {
            e2 = e6;
            obtain = null;
            a.sw("PushLog3414", "getCtrlSocketVersion error:", e2);
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            readString = str;
            a.sv("PushLog3414", "ctrlSocket version is:" + readString);
            return readString;
        } catch (Throwable th2) {
            e2 = th2;
            obtain = null;
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            throw e2;
        }
    }

    public static boolean sf() {
        String str = "v2";
        if (!gc) {
            gc = true;
            gd = str.equals(si());
        }
        a.sv("PushLog3414", "enter isSupportCtrlSocketV2, mHasCheckCtrlSocketVersion:" + gc + ",mIsSupportCtrlSokceV2:" + gd);
        return gd;
    }
}
