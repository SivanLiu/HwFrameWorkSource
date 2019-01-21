package android.os;

import android.system.OsConstants;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Locale;

class CommonTimeUtils {
    public static final int ERROR = -1;
    public static final int ERROR_BAD_VALUE = -4;
    public static final int ERROR_DEAD_OBJECT = -7;
    public static final int SUCCESS = 0;
    private String mInterfaceDesc;
    private IBinder mRemote;

    public CommonTimeUtils(IBinder remote, String interfaceDesc) {
        this.mRemote = remote;
        this.mInterfaceDesc = interfaceDesc;
    }

    public int transactGetInt(int method_code, int error_ret_val) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            this.mRemote.transact(method_code, data, reply, 0);
            int ret_val = reply.readInt() == 0 ? reply.readInt() : error_ret_val;
            reply.recycle();
            data.recycle();
            return ret_val;
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
        }
    }

    public int transactSetInt(int method_code, int val) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        int i;
        int e;
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            data.writeInt(val);
            i = 0;
            this.mRemote.transact(method_code, data, reply, i);
            e = reply.readInt();
            return e;
        } catch (RemoteException e2) {
            e = e2;
            i = -7;
            return i;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public long transactGetLong(int method_code, long error_ret_val) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            this.mRemote.transact(method_code, data, reply, 0);
            long ret_val = reply.readInt() == 0 ? reply.readLong() : error_ret_val;
            reply.recycle();
            data.recycle();
            return ret_val;
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
        }
    }

    public int transactSetLong(int method_code, long val) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        int i;
        int e;
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            data.writeLong(val);
            i = 0;
            this.mRemote.transact(method_code, data, reply, i);
            e = reply.readInt();
            return e;
        } catch (RemoteException e2) {
            e = e2;
            i = -7;
            return i;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public String transactGetString(int method_code, String error_ret_val) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            this.mRemote.transact(method_code, data, reply, 0);
            String ret_val = reply.readInt() == 0 ? reply.readString() : error_ret_val;
            reply.recycle();
            data.recycle();
            return ret_val;
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
        }
    }

    public int transactSetString(int method_code, String val) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        int i;
        int e;
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            data.writeString(val);
            i = 0;
            this.mRemote.transact(method_code, data, reply, i);
            e = reply.readInt();
            return e;
        } catch (RemoteException e2) {
            e = e2;
            i = -7;
            return i;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public InetSocketAddress transactGetSockaddr(int method_code) throws RemoteException {
        Throwable th;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        InetSocketAddress ret_val = null;
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            try {
                this.mRemote.transact(method_code, data, reply, 0);
                int res = reply.readInt();
                if (res == 0) {
                    int port = 0;
                    String addrStr = null;
                    int type = reply.readInt();
                    int addr;
                    if (OsConstants.AF_INET == type) {
                        addr = reply.readInt();
                        port = reply.readInt();
                        addrStr = String.format(Locale.US, "%d.%d.%d.%d", new Object[]{Integer.valueOf((addr >> 24) & 255), Integer.valueOf((addr >> 16) & 255), Integer.valueOf((addr >> 8) & 255), Integer.valueOf(addr & 255)});
                        int i = res;
                    } else if (OsConstants.AF_INET6 == type) {
                        addr = reply.readInt();
                        int addr2 = reply.readInt();
                        int addr3 = reply.readInt();
                        int addr4 = reply.readInt();
                        port = reply.readInt();
                        int flowinfo = reply.readInt();
                        int scope_id = reply.readInt();
                        r5 = new Object[8];
                        r5[0] = Integer.valueOf((addr >> 16) & 65535);
                        r5[1] = Integer.valueOf(addr & 65535);
                        r5[2] = Integer.valueOf((addr2 >> 16) & 65535);
                        r5[3] = Integer.valueOf(addr2 & 65535);
                        r5[4] = Integer.valueOf((addr3 >> 16) & 65535);
                        r5[5] = Integer.valueOf(addr3 & 65535);
                        r5[6] = Integer.valueOf((addr4 >> 16) & 65535);
                        r5[7] = Integer.valueOf(addr4 & 65535);
                        addrStr = String.format(Locale.US, "[%04X:%04X:%04X:%04X:%04X:%04X:%04X:%04X]", r5);
                    }
                    if (addrStr != null) {
                        ret_val = new InetSocketAddress(addrStr, port);
                    }
                }
                reply.recycle();
                data.recycle();
                return ret_val;
            } catch (Throwable th2) {
                th = th2;
                reply.recycle();
                data.recycle();
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            int i2 = method_code;
            reply.recycle();
            data.recycle();
            throw th;
        }
    }

    public int transactSetSockaddr(int method_code, InetSocketAddress addr) {
        int ret_val;
        Throwable th;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        int ret_val2 = -1;
        int i;
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            if (addr == null) {
                data.writeInt(0);
            } else {
                data.writeInt(1);
                InetAddress a = addr.getAddress();
                byte[] b = a.getAddress();
                int p = addr.getPort();
                if (a instanceof Inet4Address) {
                    int v4addr = ((((b[1] & 255) << 16) | ((b[0] & 255) << 24)) | ((b[2] & 255) << 8)) | (b[3] & 255);
                    data.writeInt(OsConstants.AF_INET);
                    data.writeInt(v4addr);
                    data.writeInt(p);
                } else if (a instanceof Inet6Address) {
                    Inet6Address v6 = (Inet6Address) a;
                    data.writeInt(OsConstants.AF_INET6);
                    for (int i2 = 0; i2 < 4; i2++) {
                        data.writeInt(((((b[(i2 * 4) + 0] & 255) << 24) | ((b[(i2 * 4) + 1] & 255) << 16)) | ((b[(i2 * 4) + 2] & 255) << 8)) | (b[(i2 * 4) + 3] & 255));
                    }
                    data.writeInt(p);
                    data.writeInt(0);
                    data.writeInt(v6.getScopeId());
                } else {
                    i = method_code;
                    reply.recycle();
                    data.recycle();
                    return -4;
                }
            }
            try {
                this.mRemote.transact(method_code, data, reply, 0);
                ret_val = reply.readInt();
            } catch (RemoteException e) {
            } catch (Throwable th2) {
                th = th2;
                reply.recycle();
                data.recycle();
                throw th;
            }
        } catch (RemoteException e2) {
            i = method_code;
            ret_val = -7;
            reply.recycle();
            data.recycle();
            return ret_val;
        } catch (Throwable th3) {
            th = th3;
            i = method_code;
            reply.recycle();
            data.recycle();
            throw th;
        }
        reply.recycle();
        data.recycle();
        return ret_val;
    }
}
