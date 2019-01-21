package huawei.android.app.admin;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionSponsor {
    protected static final boolean HWDBG = false;
    protected static final boolean HWFLOW;
    private static final String TAG = "TransactionSponsor";

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    void transactTo_uninstallPackage(int code, String transactName, ComponentName who, String packageName, boolean keepData, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        String str;
        StringBuilder stringBuilder;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append("to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeString(packageName);
                _data.writeInt(keepData);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
            }
        } catch (RemoteException localRemoteException) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(transactName);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
    }

    boolean transactTo_isFunctionDisabled(int code, String transactName, ComponentName who, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        boolean z = false;
        boolean bDisabled = false;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                if (_reply.readInt() == 1) {
                    z = true;
                }
                bDisabled = z;
            }
        } catch (RemoteException localRemoteException) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(transactName);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return bDisabled;
    }

    boolean transactTo_setFunctionDisabled(int code, String transactName, ComponentName who, boolean disabled, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        boolean success = false;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append("to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeInt(disabled);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
            }
        } catch (RemoteException localRemoteException) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transactTo ");
            stringBuilder2.append(transactName);
            stringBuilder2.append(" failed: ");
            stringBuilder2.append(localRemoteException.getMessage());
            Log.e(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return success;
    }

    void transactTo_execCommand(int code, String transactName, ComponentName who, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        String str;
        StringBuilder stringBuilder;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact: ");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
            }
        } catch (RemoteException localRemoteException) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(transactName);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
    }

    void transactTo_execCommand(int code, String transactName, ComponentName who, String param, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        String str;
        StringBuilder stringBuilder;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact: ");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeString(param);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
            }
        } catch (RemoteException localRemoteException) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(transactName);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
    }

    boolean transactTo_execCommand(int code, String transactName, ComponentName who, List<String> param, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        boolean success = false;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.code is ");
                    stringBuilder.append(code);
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeStringList(param);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
            }
        } catch (RemoteException localRemoteException) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transactTo ");
            stringBuilder2.append(transactName);
            stringBuilder2.append(" failed: ");
            stringBuilder2.append(localRemoteException.getMessage());
            Log.e(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return success;
    }

    void transactTo_execCommand(int code, String transactName, ComponentName who, Map param, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        String str;
        StringBuilder stringBuilder;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.code is ");
                    stringBuilder.append(code);
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeMap(param);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
            }
        } catch (RemoteException localRemoteException) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(transactName);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
    }

    void transactTo_execCommand(int code, String transactName, ComponentName who, Map param1, String param2, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        boolean success = false;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service. code is ");
                    stringBuilder.append(code);
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeMap(param1);
                _data.writeString(param2);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
            }
        } catch (RemoteException localRemoteException) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transactTo ");
            stringBuilder2.append(transactName);
            stringBuilder2.append(" failed: ");
            stringBuilder2.append(localRemoteException.getMessage());
            Log.e(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
    }

    List<String> transactTo_getListFunction(int code, String transactName, ComponentName who, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        List<String> stringList = null;
        String str;
        StringBuilder stringBuilder;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append("to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                stringList = new ArrayList();
                _reply.readStringList(stringList);
            }
        } catch (RemoteException localRemoteException) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(transactName);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return stringList;
    }

    List<String> transactTo_queryApn(int code, String transactName, ComponentName who, Map param, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        List<String> stringList = null;
        String str;
        StringBuilder stringBuilder;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeMap(param);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                stringList = new ArrayList();
                _reply.readStringList(stringList);
            }
        } catch (RemoteException localRemoteException) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(transactName);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return stringList;
    }

    Map<String, String> transactTo_getApnInfo(int code, String transactName, ComponentName who, String param, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        Map<String, String> map = null;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeString(param);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                map = new HashMap();
                _reply.readMap(map, null);
            }
        } catch (RemoteException localRemoteException) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transactTo ");
            stringBuilder2.append(transactName);
            stringBuilder2.append(" failed: ");
            stringBuilder2.append(localRemoteException.getMessage());
            Log.e(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return map;
    }

    void transactTo_configExchangeMail(int code, String transactName, ComponentName who, Bundle para, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        String str;
        StringBuilder stringBuilder;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append("to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                para.writeToParcel(_data, 0);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
            }
        } catch (RemoteException localRemoteException) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(transactName);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
    }

    Bundle transactTo_getMailProviderForDomain(int code, String transactName, ComponentName who, String domain, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        Bundle para = null;
        String str;
        StringBuilder stringBuilder;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append("to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeString(domain);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                if (_reply.readInt() != 0) {
                    para = new Bundle();
                    para.readFromParcel(_reply);
                }
            }
        } catch (RemoteException localRemoteException) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(transactName);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return para;
    }

    void transactTo_setDefaultLauncher(int code, String transactName, ComponentName who, String packageName, String className, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        String str;
        StringBuilder stringBuilder;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append("to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeString(packageName);
                _data.writeString(className);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
            }
        } catch (RemoteException localRemoteException) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(transactName);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
    }

    Bitmap transactTo_captureScreen(int code, String transactName, ComponentName who, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        Bitmap bitmap = null;
        String str;
        StringBuilder stringBuilder;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append("to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                if (_reply.readInt() != 0) {
                    bitmap = (Bitmap) Bitmap.CREATOR.createFromParcel(_reply);
                }
            }
        } catch (RemoteException localRemoteException) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(transactName);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return bitmap;
    }

    int transactTo_getSDCardEncryptionStatus(int code, String transactName, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        int encryptionStatus = 0;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                encryptionStatus = _reply.readInt();
            }
        } catch (RemoteException localRemoteException) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transactTo ");
            stringBuilder2.append(transactName);
            stringBuilder2.append(" failed: ");
            stringBuilder2.append(localRemoteException.getMessage());
            Log.e(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return encryptionStatus;
    }

    boolean transactTo_setPolicy(int code, String policyName, String transactName, ComponentName who, int userId, Bundle policyData, int customType) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        boolean bDisabled = false;
        boolean bDisabled2 = false;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeInt(userId);
                _data.writeString(policyName);
                _data.writeBundle(policyData);
                _data.writeInt(customType);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                if (_reply.readInt() == 1) {
                    bDisabled = true;
                }
                bDisabled2 = bDisabled;
            }
        } catch (RemoteException localRemoteException) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transactTo ");
            stringBuilder2.append(transactName);
            stringBuilder2.append(" failed: ");
            stringBuilder2.append(localRemoteException.getMessage());
            Log.e(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return bDisabled2;
    }

    Bundle transactTo_getPolicy(int code, String policyName, Bundle keyWords, String transactName, ComponentName who, int userId, int customType) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        Bundle policyData = null;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeInt(userId);
                _data.writeString(policyName);
                _data.writeBundle(keyWords);
                _data.writeInt(customType);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                policyData = _reply.readBundle();
            }
        } catch (RemoteException localRemoteException) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(transactName);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return policyData;
    }

    boolean transactTo_removePolicy(int code, String policyName, String transactName, ComponentName who, int userId, int customType) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        boolean bDisabled = false;
        boolean bDisabled2 = false;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeInt(userId);
                _data.writeString(policyName);
                _data.writeInt(customType);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                if (_reply.readInt() == 1) {
                    bDisabled = true;
                }
                bDisabled2 = bDisabled;
            }
        } catch (RemoteException localRemoteException) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transactTo ");
            stringBuilder2.append(transactName);
            stringBuilder2.append(" failed: ");
            stringBuilder2.append(localRemoteException.getMessage());
            Log.e(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return bDisabled2;
    }

    boolean transactTo_hasHwPolicy(int code, String transactName, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        boolean z = false;
        boolean bDisabled = false;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                if (_reply.readInt() == 1) {
                    z = true;
                }
                bDisabled = z;
            }
        } catch (RemoteException localRemoteException) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transactTo ");
            stringBuilder2.append(transactName);
            stringBuilder2.append(" failed: ");
            stringBuilder2.append(localRemoteException.getMessage());
            Log.e(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return bDisabled;
    }

    void transactTo_setAccountDisabled(int code, String transactName, ComponentName who, String accountType, boolean disabled, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        String str;
        StringBuilder stringBuilder;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                int i = 1;
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeString(accountType);
                if (!disabled) {
                    i = 0;
                }
                _data.writeInt(i);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
            }
        } catch (RemoteException localRemoteException) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(transactName);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
    }

    boolean transactTo_isAccountDisabled(int code, String transactName, ComponentName who, String accountType, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        boolean bDisabled = false;
        boolean bDisabled2 = false;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeString(accountType);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                if (_reply.readInt() == 1) {
                    bDisabled = true;
                }
                bDisabled2 = bDisabled;
            }
        } catch (RemoteException localRemoteException) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transactTo ");
            stringBuilder2.append(transactName);
            stringBuilder2.append(" failed: ");
            stringBuilder2.append(localRemoteException.getMessage());
            Log.e(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return bDisabled2;
    }

    boolean transactTo_formatSDCard(int code, String transactName, ComponentName who, String diskId, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        boolean bDisabled = false;
        boolean bDisabled2 = false;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeString(diskId);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                if (_reply.readInt() == 1) {
                    bDisabled = true;
                }
                bDisabled2 = bDisabled;
            }
        } catch (RemoteException localRemoteException) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transactTo ");
            stringBuilder2.append(transactName);
            stringBuilder2.append(" failed: ");
            stringBuilder2.append(localRemoteException.getMessage());
            Log.e(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return bDisabled2;
    }

    boolean transactTo_installCertificateWithType(int code, String transactName, ComponentName who, int type, byte[] certBuffer, String name, String password, int flag, boolean requestAccess, int userId) {
        RemoteException localRemoteException;
        int i;
        String str;
        String str2;
        int i2;
        boolean z;
        int i3;
        String str3;
        StringBuilder stringBuilder;
        Throwable th;
        String str4 = transactName;
        ComponentName componentName = who;
        byte[] bArr = certBuffer;
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        boolean bDisabled = false;
        int i4;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    String str5 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Transact:");
                    stringBuilder2.append(str4);
                    stringBuilder2.append(" to device policy manager service.");
                    Log.i(str5, stringBuilder2.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (componentName != null) {
                    _data.writeInt(1);
                    componentName.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                try {
                    _data.writeInt(type);
                    _data.writeInt(bArr.length);
                    _data.writeByteArray(bArr);
                } catch (RemoteException e) {
                    localRemoteException = e;
                    i = code;
                    str = name;
                    str2 = password;
                    i2 = flag;
                    z = requestAccess;
                    i3 = userId;
                    try {
                        str3 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("transactTo ");
                        stringBuilder.append(str4);
                        stringBuilder.append(" failed: ");
                        stringBuilder.append(localRemoteException.getMessage());
                        Log.e(str3, stringBuilder.toString());
                        _reply.recycle();
                        _data.recycle();
                        return bDisabled;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    i = code;
                    str = name;
                    str2 = password;
                    i2 = flag;
                    z = requestAccess;
                    i3 = userId;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
                try {
                    _data.writeString(name);
                    try {
                        _data.writeString(password);
                    } catch (RemoteException e2) {
                        localRemoteException = e2;
                        i = code;
                        i2 = flag;
                        z = requestAccess;
                        i3 = userId;
                        str3 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("transactTo ");
                        stringBuilder.append(str4);
                        stringBuilder.append(" failed: ");
                        stringBuilder.append(localRemoteException.getMessage());
                        Log.e(str3, stringBuilder.toString());
                        _reply.recycle();
                        _data.recycle();
                        return bDisabled;
                    } catch (Throwable th4) {
                        th = th4;
                        i = code;
                        i2 = flag;
                        z = requestAccess;
                        i3 = userId;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (RemoteException e3) {
                    localRemoteException = e3;
                    i = code;
                    str2 = password;
                    i2 = flag;
                    z = requestAccess;
                    i3 = userId;
                    str3 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("transactTo ");
                    stringBuilder.append(str4);
                    stringBuilder.append(" failed: ");
                    stringBuilder.append(localRemoteException.getMessage());
                    Log.e(str3, stringBuilder.toString());
                    _reply.recycle();
                    _data.recycle();
                    return bDisabled;
                } catch (Throwable th5) {
                    th = th5;
                    i = code;
                    str2 = password;
                    i2 = flag;
                    z = requestAccess;
                    i3 = userId;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
                try {
                    _data.writeInt(flag);
                    try {
                        _data.writeInt(requestAccess ? 1 : 0);
                    } catch (RemoteException e4) {
                        localRemoteException = e4;
                        i = code;
                        i3 = userId;
                        str3 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("transactTo ");
                        stringBuilder.append(str4);
                        stringBuilder.append(" failed: ");
                        stringBuilder.append(localRemoteException.getMessage());
                        Log.e(str3, stringBuilder.toString());
                        _reply.recycle();
                        _data.recycle();
                        return bDisabled;
                    } catch (Throwable th6) {
                        th = th6;
                        i = code;
                        i3 = userId;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (RemoteException e5) {
                    localRemoteException = e5;
                    i = code;
                    z = requestAccess;
                    i3 = userId;
                    str3 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("transactTo ");
                    stringBuilder.append(str4);
                    stringBuilder.append(" failed: ");
                    stringBuilder.append(localRemoteException.getMessage());
                    Log.e(str3, stringBuilder.toString());
                    _reply.recycle();
                    _data.recycle();
                    return bDisabled;
                } catch (Throwable th7) {
                    th = th7;
                    i = code;
                    z = requestAccess;
                    i3 = userId;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
                try {
                    _data.writeInt(userId);
                    try {
                        binder.transact(code, _data, _reply, 0);
                        _reply.readException();
                        bDisabled = _reply.readInt() == 1;
                    } catch (RemoteException e6) {
                        localRemoteException = e6;
                        str3 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("transactTo ");
                        stringBuilder.append(str4);
                        stringBuilder.append(" failed: ");
                        stringBuilder.append(localRemoteException.getMessage());
                        Log.e(str3, stringBuilder.toString());
                        _reply.recycle();
                        _data.recycle();
                        return bDisabled;
                    }
                } catch (RemoteException e7) {
                    localRemoteException = e7;
                    i = code;
                    str3 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("transactTo ");
                    stringBuilder.append(str4);
                    stringBuilder.append(" failed: ");
                    stringBuilder.append(localRemoteException.getMessage());
                    Log.e(str3, stringBuilder.toString());
                    _reply.recycle();
                    _data.recycle();
                    return bDisabled;
                } catch (Throwable th8) {
                    th = th8;
                    i = code;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }
            i = code;
            i4 = type;
            str = name;
            str2 = password;
            i2 = flag;
            z = requestAccess;
            i3 = userId;
        } catch (RemoteException e8) {
            localRemoteException = e8;
            i = code;
            i4 = type;
            str = name;
            str2 = password;
            i2 = flag;
            z = requestAccess;
            i3 = userId;
            str3 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transactTo ");
            stringBuilder.append(str4);
            stringBuilder.append(" failed: ");
            stringBuilder.append(localRemoteException.getMessage());
            Log.e(str3, stringBuilder.toString());
            _reply.recycle();
            _data.recycle();
            return bDisabled;
        } catch (Throwable th9) {
            th = th9;
            i = code;
            i4 = type;
            str = name;
            str2 = password;
            i2 = flag;
            z = requestAccess;
            i3 = userId;
            _reply.recycle();
            _data.recycle();
            throw th;
        }
        _reply.recycle();
        _data.recycle();
        return bDisabled;
    }

    boolean transactTo_setCarrierLockScreenPassword(int code, String transactName, ComponentName who, String password, String phoneNumber, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        boolean bDisabled = false;
        boolean bDisabled2 = false;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeString(password);
                _data.writeString(phoneNumber);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                if (_reply.readInt() == 1) {
                    bDisabled = true;
                }
                bDisabled2 = bDisabled;
            }
        } catch (RemoteException localRemoteException) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transactTo ");
            stringBuilder2.append(transactName);
            stringBuilder2.append(" failed: ");
            stringBuilder2.append(localRemoteException.getMessage());
            Log.e(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return bDisabled2;
    }

    boolean transactTo_clearCarrierLockScreenPassword(int code, String transactName, ComponentName who, String password, int userId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        boolean bDisabled = false;
        boolean bDisabled2 = false;
        try {
            IBinder binder = ServiceManager.getService("device_policy");
            if (binder != null) {
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to device policy manager service.");
                    Log.i(str, stringBuilder.toString());
                }
                _data.writeInterfaceToken(ConstantValue.DESCRIPTOR);
                if (who != null) {
                    _data.writeInt(1);
                    who.writeToParcel(_data, 0);
                } else {
                    _data.writeInt(0);
                }
                _data.writeString(password);
                _data.writeInt(userId);
                binder.transact(code, _data, _reply, 0);
                _reply.readException();
                if (_reply.readInt() == 1) {
                    bDisabled = true;
                }
                bDisabled2 = bDisabled;
            }
        } catch (RemoteException localRemoteException) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transactTo ");
            stringBuilder2.append(transactName);
            stringBuilder2.append(" failed: ");
            stringBuilder2.append(localRemoteException.getMessage());
            Log.e(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
        return bDisabled2;
    }
}
