package com.huawei.android.pushagent.utils.tools;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.d.c;

public class d {
    private static boolean fc = false;
    private static boolean fd = false;

    public static void qs(String str) {
        Parcel obtain;
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        c.sh("PushLog2951", "ctrlScoket registerPackage " + str);
        if (!TextUtils.isEmpty(str)) {
            try {
                IBinder service = ServiceManager.getService("connectivity");
                if (service == null) {
                    c.sf("PushLog2951", "get connectivity service failed ");
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
                    c.sf("PushLog2951", "registerPackage error:" + e.getMessage());
                    if (obtain != null) {
                        obtain.recycle();
                    }
                    if (parcel != null) {
                        parcel.recycle();
                    }
                } catch (Exception e4) {
                    e2 = e4;
                    try {
                        c.se("PushLog2951", "registerPackage error:", e2);
                        if (obtain != null) {
                            obtain.recycle();
                        }
                        if (parcel != null) {
                            parcel.recycle();
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
                c.sf("PushLog2951", "registerPackage error:" + e.getMessage());
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (Exception e6) {
                e2 = e6;
                obtain = null;
                c.se("PushLog2951", "registerPackage error:", e2);
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

    public static void qt(String str) {
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        c.sh("PushLog2951", "ctrlScoket deregisterPackage " + str);
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
                        c.sf("PushLog2951", "deregisterPackage error:" + e.getMessage());
                        if (obtain != null) {
                            obtain.recycle();
                        }
                        if (parcel != null) {
                            parcel.recycle();
                        }
                    } catch (Exception e4) {
                        e2 = e4;
                        try {
                            c.se("PushLog2951", "deregisterPackage error:", e2);
                            if (obtain != null) {
                                obtain.recycle();
                            }
                            if (parcel != null) {
                                parcel.recycle();
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
                c.sf("PushLog2951", "deregisterPackage error:" + e.getMessage());
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (Exception e6) {
                e2 = e6;
                obtain = null;
                c.se("PushLog2951", "deregisterPackage error:", e2);
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

    public static void qp(int i, int i2) {
        Parcel obtain;
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        c.sh("PushLog2951", "ctrlSocket cmd is " + i + ", param is " + i2);
        try {
            IBinder service = ServiceManager.getService("connectivity");
            if (service == null) {
                c.sj("PushLog2951", "get connectivity service failed ");
                return;
            }
            obtain = Parcel.obtain();
            try {
                obtain.writeInt(Process.myPid());
                obtain.writeInt(i);
                obtain.writeInt(i2);
                parcel = Parcel.obtain();
                service.transact(1003, obtain, parcel, 0);
                c.sh("PushLog2951", "ctrlSocket success");
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (RemoteException e3) {
                e = e3;
                c.sf("PushLog2951", "ctrlSocket error:" + e.getMessage());
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (Exception e4) {
                e2 = e4;
                try {
                    c.se("PushLog2951", "ctrlSocket error:", e2);
                    if (obtain != null) {
                        obtain.recycle();
                    }
                    if (parcel != null) {
                        parcel.recycle();
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
            c.sf("PushLog2951", "ctrlSocket error:" + e.getMessage());
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
        } catch (Exception e6) {
            e2 = e6;
            obtain = null;
            c.se("PushLog2951", "ctrlSocket error:", e2);
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

    public static String[] qr() {
        Parcel obtain;
        String[] strArr;
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        String[] strArr2 = new String[0];
        try {
            IBinder service = ServiceManager.getService("connectivity");
            if (service == null) {
                c.sj("PushLog2951", "get connectivity service failed ");
                return strArr2;
            }
            obtain = Parcel.obtain();
            try {
                parcel = Parcel.obtain();
                service.transact(1004, obtain, parcel, 0);
                Object readString = parcel.readString();
                c.sh("PushLog2951", "ctrlSocket whitepackages is:" + readString);
                if (TextUtils.isEmpty(readString)) {
                    strArr = strArr2;
                } else {
                    strArr = readString.split("\t");
                }
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (RemoteException e3) {
                e = e3;
                c.sf("PushLog2951", "ctrlSocket error:" + e.getMessage());
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
                strArr = strArr2;
                return strArr;
            } catch (Exception e4) {
                e2 = e4;
                try {
                    c.se("PushLog2951", "ctrlSocket error:", e2);
                    if (obtain != null) {
                        obtain.recycle();
                    }
                    if (parcel != null) {
                        parcel.recycle();
                    }
                    strArr = strArr2;
                    return strArr;
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
            return strArr;
        } catch (RemoteException e5) {
            e = e5;
            obtain = parcel;
            c.sf("PushLog2951", "ctrlSocket error:" + e.getMessage());
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            strArr = strArr2;
            return strArr;
        } catch (Exception e6) {
            e2 = e6;
            obtain = parcel;
            c.se("PushLog2951", "ctrlSocket error:", e2);
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            strArr = strArr2;
            return strArr;
        } catch (Throwable th2) {
            e2 = th2;
            obtain = parcel;
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            throw e2;
        }
    }

    public static int qq() {
        Parcel obtain;
        int readInt;
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        try {
            IBinder service = ServiceManager.getService("connectivity");
            if (service == null) {
                c.sj("PushLog2951", "get connectivity service failed ");
                return -1;
            }
            obtain = Parcel.obtain();
            try {
                parcel = Parcel.obtain();
                service.transact(1005, obtain, parcel, 0);
                readInt = parcel.readInt();
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (RemoteException e3) {
                e = e3;
                c.sf("PushLog2951", "getCtrlSocketModel error:" + e.getMessage());
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
                readInt = -1;
                c.sh("PushLog2951", "ctrlSocket level is:" + readInt);
                return readInt;
            } catch (Exception e4) {
                e2 = e4;
                try {
                    c.se("PushLog2951", "getCtrlSocketModel error:", e2);
                    if (obtain != null) {
                        obtain.recycle();
                    }
                    if (parcel != null) {
                        parcel.recycle();
                    }
                    readInt = -1;
                    c.sh("PushLog2951", "ctrlSocket level is:" + readInt);
                    return readInt;
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
            c.sh("PushLog2951", "ctrlSocket level is:" + readInt);
            return readInt;
        } catch (RemoteException e5) {
            e = e5;
            obtain = null;
            c.sf("PushLog2951", "getCtrlSocketModel error:" + e.getMessage());
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            readInt = -1;
            c.sh("PushLog2951", "ctrlSocket level is:" + readInt);
            return readInt;
        } catch (Exception e6) {
            e2 = e6;
            obtain = null;
            c.se("PushLog2951", "getCtrlSocketModel error:", e2);
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            readInt = -1;
            c.sh("PushLog2951", "ctrlSocket level is:" + readInt);
            return readInt;
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

    private static String qu() {
        String readString;
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        String str = "";
        Parcel obtain;
        try {
            IBinder service = ServiceManager.getService("connectivity");
            if (service == null) {
                c.sj("PushLog2951", "get connectivity service failed ");
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
                c.sf("PushLog2951", "getCtrlSocketVersion error:" + e.getMessage());
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
                readString = str;
                c.sh("PushLog2951", "ctrlSocket version is:" + readString);
                return readString;
            } catch (Exception e4) {
                e2 = e4;
                try {
                    c.se("PushLog2951", "getCtrlSocketVersion error:", e2);
                    if (obtain != null) {
                        obtain.recycle();
                    }
                    if (parcel != null) {
                        parcel.recycle();
                    }
                    readString = str;
                    c.sh("PushLog2951", "ctrlSocket version is:" + readString);
                    return readString;
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
            c.sh("PushLog2951", "ctrlSocket version is:" + readString);
            return readString;
        } catch (RemoteException e5) {
            e = e5;
            obtain = null;
            c.sf("PushLog2951", "getCtrlSocketVersion error:" + e.getMessage());
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            readString = str;
            c.sh("PushLog2951", "ctrlSocket version is:" + readString);
            return readString;
        } catch (Exception e6) {
            e2 = e6;
            obtain = null;
            c.se("PushLog2951", "getCtrlSocketVersion error:", e2);
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            readString = str;
            c.sh("PushLog2951", "ctrlSocket version is:" + readString);
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

    public static boolean qo() {
        String str = "v2";
        c.sh("PushLog2951", "enter isSupportCtrlSocketV2, mHasCheckCtrlSocketVersion:" + fc + ",mIsSupportCtrlSokceV2:" + fd);
        if (!fc) {
            fc = true;
            fd = str.equals(qu());
            c.sh("PushLog2951", "mIsSupportCtrlSokceV2:" + fd);
        }
        return fd;
    }
}
