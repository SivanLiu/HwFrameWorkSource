package com.huawei.android.pushagent.utils.tools;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;

public class c {
    private static boolean ak = false;
    private static boolean al = false;

    /* JADX WARNING: Removed duplicated region for block: B:29:0x0089  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x008e  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005e  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0063  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0096  */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x009b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void cs(String str) {
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "ctrlScoket registerPackage " + str);
        if (!TextUtils.isEmpty(str)) {
            Parcel obtain;
            try {
                IBinder service = ServiceManager.getService("connectivity");
                if (service == null) {
                    com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "get connectivity service failed ");
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
                    com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "registerPackage error:" + e.getMessage());
                    if (obtain != null) {
                    }
                    if (parcel != null) {
                    }
                } catch (Exception e4) {
                    e2 = e4;
                    try {
                        com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "registerPackage error:", e2);
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
                com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "registerPackage error:" + e.getMessage());
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (Exception e6) {
                e2 = e6;
                obtain = null;
                com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "registerPackage error:", e2);
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

    /* JADX WARNING: Removed duplicated region for block: B:28:0x0080  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0085  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0055  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x008d  */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x0092  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void cu(String str) {
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "ctrlScoket deregisterPackage " + str);
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
                        com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "deregisterPackage error:" + e.getMessage());
                        if (obtain != null) {
                            obtain.recycle();
                        }
                        if (parcel != null) {
                            parcel.recycle();
                        }
                    } catch (Exception e4) {
                        e2 = e4;
                        try {
                            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "deregisterPackage error:", e2);
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
                com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "deregisterPackage error:" + e.getMessage());
                if (obtain != null) {
                }
                if (parcel != null) {
                }
            } catch (Exception e6) {
                e2 = e6;
                obtain = null;
                com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "deregisterPackage error:", e2);
                if (obtain != null) {
                }
                if (parcel != null) {
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

    /* JADX WARNING: Removed duplicated region for block: B:33:0x00ad  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00b2  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x00a0  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00a5  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0075  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x007a  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void ct(int i, int i2) {
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "ctrlSocket cmd is " + i + ", param is " + i2);
        Parcel obtain;
        try {
            IBinder service = ServiceManager.getService("connectivity");
            if (service == null) {
                com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "get connectivity service failed ");
                return;
            }
            obtain = Parcel.obtain();
            try {
                obtain.writeInt(Process.myPid());
                obtain.writeInt(i);
                obtain.writeInt(i2);
                parcel = Parcel.obtain();
                service.transact(1003, obtain, parcel, 0);
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "ctrlSocket success");
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (RemoteException e3) {
                e = e3;
                com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "ctrlSocket error:" + e.getMessage());
                if (obtain != null) {
                }
                if (parcel != null) {
                }
            } catch (Exception e4) {
                e2 = e4;
                try {
                    com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "ctrlSocket error:", e2);
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
            com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "ctrlSocket error:" + e.getMessage());
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
        } catch (Exception e6) {
            e2 = e6;
            obtain = null;
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "ctrlSocket error:", e2);
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

    /* JADX WARNING: Removed duplicated region for block: B:35:0x0094  */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x0099  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x005b  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0060  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static String cv() {
        String readString;
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        String str = "";
        Parcel obtain;
        try {
            IBinder service = ServiceManager.getService("connectivity");
            if (service == null) {
                com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "get connectivity service failed ");
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
                    com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "getCtrlSocketVersion error:", e2);
                    if (obtain != null) {
                    }
                    if (parcel != null) {
                    }
                    readString = str;
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "ctrlSocket version is:" + readString);
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
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "ctrlSocket version is:" + readString);
            return readString;
        } catch (RemoteException e5) {
            e = e5;
            obtain = null;
            com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "getCtrlSocketVersion error:" + e.getMessage());
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            readString = str;
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "ctrlSocket version is:" + readString);
            return readString;
        } catch (Exception e6) {
            e2 = e6;
            obtain = null;
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "getCtrlSocketVersion error:", e2);
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            readString = str;
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "ctrlSocket version is:" + readString);
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

    public static boolean cr() {
        String str = "v2";
        if (!ak) {
            ak = true;
            al = str.equals(cv());
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "enter isSupportCtrlSocketV2, mHasCheckCtrlSocketVersion:" + ak + ",mIsSupportCtrlSokceV2:" + al);
        return al;
    }
}
