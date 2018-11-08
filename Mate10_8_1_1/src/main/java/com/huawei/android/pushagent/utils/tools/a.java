package com.huawei.android.pushagent.utils.tools;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.b;

public class a {
    private static boolean n = false;
    private static boolean o = false;

    public static void k(String str) {
        Parcel obtain;
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        b.z("PushLog2976", "ctrlScoket registerPackage " + str);
        if (!TextUtils.isEmpty(str)) {
            try {
                IBinder service = ServiceManager.getService("connectivity");
                if (service == null) {
                    b.y("PushLog2976", "get connectivity service failed ");
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
                    b.y("PushLog2976", "registerPackage error:" + e.getMessage());
                    if (obtain != null) {
                        obtain.recycle();
                    }
                    if (parcel != null) {
                        parcel.recycle();
                    }
                } catch (Exception e4) {
                    e2 = e4;
                    try {
                        b.aa("PushLog2976", "registerPackage error:", e2);
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
                b.y("PushLog2976", "registerPackage error:" + e.getMessage());
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (Exception e6) {
                e2 = e6;
                obtain = null;
                b.aa("PushLog2976", "registerPackage error:", e2);
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

    public static void h(String str) {
        Parcel obtain;
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        b.z("PushLog2976", "ctrlScoket deregisterPackage " + str);
        if (!TextUtils.isEmpty(str)) {
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
                        b.y("PushLog2976", "deregisterPackage error:" + e.getMessage());
                        if (obtain != null) {
                            obtain.recycle();
                        }
                        if (parcel != null) {
                            parcel.recycle();
                        }
                    } catch (Exception e4) {
                        e2 = e4;
                        try {
                            b.aa("PushLog2976", "deregisterPackage error:", e2);
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
                b.y("PushLog2976", "deregisterPackage error:" + e.getMessage());
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (Exception e6) {
                e2 = e6;
                obtain = null;
                b.aa("PushLog2976", "deregisterPackage error:", e2);
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

    public static void g(int i, int i2) {
        Parcel obtain;
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        b.z("PushLog2976", "ctrlSocket cmd is " + i + ", param is " + i2);
        try {
            IBinder service = ServiceManager.getService("connectivity");
            if (service == null) {
                b.ab("PushLog2976", "get connectivity service failed ");
                return;
            }
            obtain = Parcel.obtain();
            try {
                obtain.writeInt(Process.myPid());
                obtain.writeInt(i);
                obtain.writeInt(i2);
                parcel = Parcel.obtain();
                service.transact(1003, obtain, parcel, 0);
                b.z("PushLog2976", "ctrlSocket success");
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (RemoteException e3) {
                e = e3;
                b.y("PushLog2976", "ctrlSocket error:" + e.getMessage());
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
            } catch (Exception e4) {
                e2 = e4;
                try {
                    b.aa("PushLog2976", "ctrlSocket error:", e2);
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
            b.y("PushLog2976", "ctrlSocket error:" + e.getMessage());
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
        } catch (Exception e6) {
            e2 = e6;
            obtain = null;
            b.aa("PushLog2976", "ctrlSocket error:", e2);
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

    private static String i() {
        String readString;
        RemoteException e;
        Throwable e2;
        Parcel parcel = null;
        String str = "";
        Parcel obtain;
        try {
            IBinder service = ServiceManager.getService("connectivity");
            if (service == null) {
                b.ab("PushLog2976", "get connectivity service failed ");
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
                b.y("PushLog2976", "getCtrlSocketVersion error:" + e.getMessage());
                if (obtain != null) {
                    obtain.recycle();
                }
                if (parcel != null) {
                    parcel.recycle();
                }
                readString = str;
                b.z("PushLog2976", "ctrlSocket version is:" + readString);
                return readString;
            } catch (Exception e4) {
                e2 = e4;
                try {
                    b.aa("PushLog2976", "getCtrlSocketVersion error:", e2);
                    if (obtain != null) {
                        obtain.recycle();
                    }
                    if (parcel != null) {
                        parcel.recycle();
                    }
                    readString = str;
                    b.z("PushLog2976", "ctrlSocket version is:" + readString);
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
            b.z("PushLog2976", "ctrlSocket version is:" + readString);
            return readString;
        } catch (RemoteException e5) {
            e = e5;
            obtain = null;
            b.y("PushLog2976", "getCtrlSocketVersion error:" + e.getMessage());
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            readString = str;
            b.z("PushLog2976", "ctrlSocket version is:" + readString);
            return readString;
        } catch (Exception e6) {
            e2 = e6;
            obtain = null;
            b.aa("PushLog2976", "getCtrlSocketVersion error:", e2);
            if (obtain != null) {
                obtain.recycle();
            }
            if (parcel != null) {
                parcel.recycle();
            }
            readString = str;
            b.z("PushLog2976", "ctrlSocket version is:" + readString);
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

    public static boolean j() {
        String str = "v2";
        if (!n) {
            n = true;
            o = str.equals(i());
        }
        b.z("PushLog2976", "enter isSupportCtrlSocketV2, mHasCheckCtrlSocketVersion:" + n + ",mIsSupportCtrlSokceV2:" + o);
        return o;
    }
}
