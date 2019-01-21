package android.app.mtm;

import android.app.mtm.iaware.HwAppStartupSetting;
import android.app.mtm.iaware.HwAppStartupSettingFilter;
import android.app.mtm.iaware.RSceneData;
import android.app.mtm.iaware.appmng.AppCleanParam;
import android.app.mtm.iaware.appmng.IAppCleanCallback;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.rms.iaware.RPolicyData;
import java.util.List;

public interface IMultiTaskManagerService extends IInterface {

    public static abstract class Stub extends Binder implements IMultiTaskManagerService {
        private static final String DESCRIPTOR = "android.app.mtm.IMultiTaskManagerService";
        static final int TRANSACTION_acquirePolicyData = 10;
        static final int TRANSACTION_forcestopApps = 8;
        static final int TRANSACTION_killProcess = 4;
        static final int TRANSACTION_notifyProcessDiedChange = 7;
        static final int TRANSACTION_notifyProcessGroupChange = 5;
        static final int TRANSACTION_notifyProcessStatusChange = 6;
        static final int TRANSACTION_notifyResourceStatusOverload = 3;
        static final int TRANSACTION_registerObserver = 1;
        static final int TRANSACTION_removeAppStartupSetting = 14;
        static final int TRANSACTION_reportScene = 9;
        static final int TRANSACTION_requestAppCleanWithCallback = 16;
        static final int TRANSACTION_retrieveAppStartupPackages = 12;
        static final int TRANSACTION_retrieveAppStartupSettings = 11;
        static final int TRANSACTION_unregisterObserver = 2;
        static final int TRANSACTION_updateAppStartupSettings = 13;
        static final int TRANSACTION_updateCloudPolicy = 15;

        private static class Proxy implements IMultiTaskManagerService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public void registerObserver(IMultiTaskProcessObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterObserver(IMultiTaskProcessObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyResourceStatusOverload(int resourcetype, String resourceextend, int resourcestatus, Bundle args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(resourcetype);
                    _data.writeString(resourceextend);
                    _data.writeInt(resourcestatus);
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean killProcess(int pid, boolean restartservice) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(pid);
                    _data.writeInt(restartservice);
                    boolean z = false;
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyProcessGroupChange(int pid, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyProcessStatusChange(String pkg, String process, String hostingType, int pid, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeString(process);
                    _data.writeString(hostingType);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyProcessDiedChange(int pid, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean forcestopApps(int pid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(pid);
                    boolean z = false;
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean reportScene(int featureId, RSceneData scene) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(featureId);
                    boolean _result = true;
                    if (scene != null) {
                        _data.writeInt(1);
                        scene.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public RPolicyData acquirePolicyData(int featureId, RSceneData scene) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    RPolicyData _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(featureId);
                    if (scene != null) {
                        _data.writeInt(1);
                        scene.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (RPolicyData) RPolicyData.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<HwAppStartupSetting> retrieveAppStartupSettings(List<String> pkgList, HwAppStartupSettingFilter filter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(pkgList);
                    if (filter != null) {
                        _data.writeInt(1);
                        filter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    List<HwAppStartupSetting> _result = _reply.createTypedArrayList(HwAppStartupSetting.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> retrieveAppStartupPackages(List<String> pkgList, int[] policy, int[] modifier, int[] show) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(pkgList);
                    _data.writeIntArray(policy);
                    _data.writeIntArray(modifier);
                    _data.writeIntArray(show);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean updateAppStartupSettings(List<HwAppStartupSetting> settingList, boolean clearFirst) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(settingList);
                    _data.writeInt(clearFirst);
                    boolean z = false;
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean removeAppStartupSetting(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    boolean z = false;
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean updateCloudPolicy(String filePath) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(filePath);
                    boolean z = false;
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void requestAppCleanWithCallback(AppCleanParam param, IAppCleanCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (param != null) {
                        _data.writeInt(1);
                        param.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMultiTaskManagerService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IMultiTaskManagerService)) {
                return new Proxy(obj);
            }
            return (IMultiTaskManagerService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                boolean _arg1 = false;
                Bundle _arg12 = null;
                int _arg0;
                boolean _result;
                boolean _result2;
                RSceneData _arg13;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        registerObserver(android.app.mtm.IMultiTaskProcessObserver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        unregisterObserver(android.app.mtm.IMultiTaskProcessObserver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        String _arg14 = data.readString();
                        int _arg2 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg12 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        notifyResourceStatusOverload(_arg0, _arg14, _arg2, _arg12);
                        reply.writeNoException();
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        int _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        _result = killProcess(_arg02, _arg1);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        notifyProcessGroupChange(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        notifyProcessStatusChange(data.readString(), data.readString(), data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        notifyProcessDiedChange(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        _result2 = forcestopApps(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg13 = (RSceneData) RSceneData.CREATOR.createFromParcel(parcel);
                        }
                        _result = reportScene(_arg0, _arg13);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        int _arg03 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg13 = (RSceneData) RSceneData.CREATOR.createFromParcel(parcel);
                        }
                        RPolicyData _result3 = acquirePolicyData(_arg03, _arg13);
                        reply.writeNoException();
                        if (_result3 != null) {
                            parcel2.writeInt(1);
                            _result3.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 11:
                        HwAppStartupSettingFilter _arg15;
                        parcel.enforceInterface(descriptor);
                        List<String> _arg04 = data.createStringArrayList();
                        if (data.readInt() != 0) {
                            _arg15 = (HwAppStartupSettingFilter) HwAppStartupSettingFilter.CREATOR.createFromParcel(parcel);
                        }
                        List<HwAppStartupSetting> _result4 = retrieveAppStartupSettings(_arg04, _arg15);
                        reply.writeNoException();
                        parcel2.writeTypedList(_result4);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        List<String> _result5 = retrieveAppStartupPackages(data.createStringArrayList(), data.createIntArray(), data.createIntArray(), data.createIntArray());
                        reply.writeNoException();
                        parcel2.writeStringList(_result5);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        List<HwAppStartupSetting> _arg05 = parcel.createTypedArrayList(HwAppStartupSetting.CREATOR);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        _result = updateAppStartupSettings(_arg05, _arg1);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        _result2 = removeAppStartupSetting(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        _result2 = updateCloudPolicy(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 16:
                        AppCleanParam _arg06;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg06 = (AppCleanParam) AppCleanParam.CREATOR.createFromParcel(parcel);
                        }
                        requestAppCleanWithCallback(_arg06, android.app.mtm.iaware.appmng.IAppCleanCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    RPolicyData acquirePolicyData(int i, RSceneData rSceneData) throws RemoteException;

    boolean forcestopApps(int i) throws RemoteException;

    boolean killProcess(int i, boolean z) throws RemoteException;

    void notifyProcessDiedChange(int i, int i2) throws RemoteException;

    void notifyProcessGroupChange(int i, int i2) throws RemoteException;

    void notifyProcessStatusChange(String str, String str2, String str3, int i, int i2) throws RemoteException;

    void notifyResourceStatusOverload(int i, String str, int i2, Bundle bundle) throws RemoteException;

    void registerObserver(IMultiTaskProcessObserver iMultiTaskProcessObserver) throws RemoteException;

    boolean removeAppStartupSetting(String str) throws RemoteException;

    boolean reportScene(int i, RSceneData rSceneData) throws RemoteException;

    void requestAppCleanWithCallback(AppCleanParam appCleanParam, IAppCleanCallback iAppCleanCallback) throws RemoteException;

    List<String> retrieveAppStartupPackages(List<String> list, int[] iArr, int[] iArr2, int[] iArr3) throws RemoteException;

    List<HwAppStartupSetting> retrieveAppStartupSettings(List<String> list, HwAppStartupSettingFilter hwAppStartupSettingFilter) throws RemoteException;

    void unregisterObserver(IMultiTaskProcessObserver iMultiTaskProcessObserver) throws RemoteException;

    boolean updateAppStartupSettings(List<HwAppStartupSetting> list, boolean z) throws RemoteException;

    boolean updateCloudPolicy(String str) throws RemoteException;
}
