package android.hardware.wifi.supplicant.V1_0;

import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork.getIdCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork.getInterfaceNameCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork.getTypeCallback;
import android.hidl.base.V1_0.DebugInfo;
import android.hidl.base.V1_0.IBase;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwBinder.DeathRecipient;
import android.os.IHwInterface;
import android.os.RemoteException;
import com.android.server.wifi.HalDeviceManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface ISupplicantP2pNetwork extends ISupplicantNetwork {
    public static final String kInterfaceName = "android.hardware.wifi.supplicant@1.0::ISupplicantP2pNetwork";

    @FunctionalInterface
    public interface getBssidCallback {
        void onValues(SupplicantStatus supplicantStatus, byte[] bArr);
    }

    @FunctionalInterface
    public interface getClientListCallback {
        void onValues(SupplicantStatus supplicantStatus, ArrayList<byte[]> arrayList);
    }

    @FunctionalInterface
    public interface getSsidCallback {
        void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    public interface isCurrentCallback {
        void onValues(SupplicantStatus supplicantStatus, boolean z);
    }

    @FunctionalInterface
    public interface isGoCallback {
        void onValues(SupplicantStatus supplicantStatus, boolean z);
    }

    @FunctionalInterface
    public interface isPersistentCallback {
        void onValues(SupplicantStatus supplicantStatus, boolean z);
    }

    public static final class Proxy implements ISupplicantP2pNetwork {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(interfaceDescriptor());
                stringBuilder.append("@Proxy");
                return stringBuilder.toString();
            } catch (RemoteException e) {
                return "[class or subclass of android.hardware.wifi.supplicant@1.0::ISupplicantP2pNetwork]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        public void getId(getIdCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getInterfaceName(getInterfaceNameCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getType(getTypeCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus registerCallback(ISupplicantP2pNetworkCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getSsid(getSsidCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getBssid(getBssidCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                byte[] _hidl_out_bssid = new byte[6];
                _hidl_reply.readBuffer(6).copyToInt8Array(0, _hidl_out_bssid, 6);
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_bssid);
            } finally {
                _hidl_reply.release();
            }
        }

        public void isCurrent(isCurrentCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readBool());
            } finally {
                _hidl_reply.release();
            }
        }

        public void isPersistent(isPersistentCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readBool());
            } finally {
                _hidl_reply.release();
            }
        }

        public void isGo(isGoCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readBool());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setClientList(ArrayList<byte[]> clients) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(16);
            int _hidl_vec_size = clients.size();
            _hidl_blob.putInt32(8, _hidl_vec_size);
            _hidl_blob.putBool(12, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 6);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                childBlob.putInt8Array((long) (_hidl_index_0 * 6), (byte[]) clients.get(_hidl_index_0));
            }
            _hidl_blob.putBlob(0, childBlob);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getClientList(getClientListCallback _hidl_cb) throws RemoteException {
            Throwable th;
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                int _hidl_index_0 = 0;
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                ArrayList<byte[]> _hidl_out_clients = new ArrayList();
                HwBlob _hidl_blob = _hidl_reply.readBuffer(16);
                int _hidl_vec_size = _hidl_blob.getInt32(8);
                HwBlob childBlob = _hidl_reply.readEmbeddedBuffer((long) (_hidl_vec_size * 6), _hidl_blob.handle(), 0, true);
                _hidl_out_clients.clear();
                while (true) {
                    int _hidl_index_02 = _hidl_index_0;
                    if (_hidl_index_02 < _hidl_vec_size) {
                        byte[] _hidl_vec_element = new byte[6];
                        childBlob.copyToInt8Array((long) (_hidl_index_02 * 6), _hidl_vec_element, 6);
                        _hidl_out_clients.add(_hidl_vec_element);
                        _hidl_index_0 = _hidl_index_02 + 1;
                    } else {
                        try {
                            _hidl_cb.onValues(_hidl_out_status, _hidl_out_clients);
                            _hidl_reply.release();
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                            _hidl_reply.release();
                            throw th;
                        }
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                getClientListCallback getclientlistcallback = _hidl_cb;
                _hidl_reply.release();
                throw th;
            }
        }

        public ArrayList<String> interfaceChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256067662, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<String> _hidl_out_descriptors = _hidl_reply.readStringVector();
                return _hidl_out_descriptors;
            } finally {
                _hidl_reply.release();
            }
        }

        public String interfaceDescriptor() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256136003, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                String _hidl_out_descriptor = _hidl_reply.readString();
                return _hidl_out_descriptor;
            } finally {
                _hidl_reply.release();
            }
        }

        public ArrayList<byte[]> getHashChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                int _hidl_index_0 = 0;
                this.mRemote.transact(256398152, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<byte[]> _hidl_out_hashchain = new ArrayList();
                HwBlob _hidl_blob = _hidl_reply.readBuffer(16);
                int _hidl_vec_size = _hidl_blob.getInt32(8);
                HwBlob childBlob = _hidl_reply.readEmbeddedBuffer((long) (_hidl_vec_size * 32), _hidl_blob.handle(), 0, true);
                _hidl_out_hashchain.clear();
                while (true) {
                    int _hidl_index_02 = _hidl_index_0;
                    if (_hidl_index_02 >= _hidl_vec_size) {
                        break;
                    }
                    byte[] _hidl_vec_element = new byte[32];
                    childBlob.copyToInt8Array((long) (_hidl_index_02 * 32), _hidl_vec_element, 32);
                    _hidl_out_hashchain.add(_hidl_vec_element);
                    _hidl_index_0 = _hidl_index_02 + 1;
                }
                return _hidl_out_hashchain;
            } finally {
                _hidl_reply.release();
            }
        }

        public void setHALInstrumentation() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256462420, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public boolean linkToDeath(DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        public void ping() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256921159, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public DebugInfo getDebugInfo() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257049926, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                DebugInfo _hidl_out_info = new DebugInfo();
                _hidl_out_info.readFromParcel(_hidl_reply);
                return _hidl_out_info;
            } finally {
                _hidl_reply.release();
            }
        }

        public void notifySyspropsChanged() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257120595, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public boolean unlinkToDeath(DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    public static abstract class Stub extends HwBinder implements ISupplicantP2pNetwork {
        public IHwBinder asBinder() {
            return this;
        }

        public final ArrayList<String> interfaceChain() {
            return new ArrayList(Arrays.asList(new String[]{ISupplicantP2pNetwork.kInterfaceName, ISupplicantNetwork.kInterfaceName, IBase.kInterfaceName}));
        }

        public final String interfaceDescriptor() {
            return ISupplicantP2pNetwork.kInterfaceName;
        }

        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList(Arrays.asList(new byte[][]{new byte[]{(byte) 86, (byte) 18, (byte) -113, (byte) 116, (byte) 86, (byte) 5, (byte) 113, (byte) -74, (byte) 119, (byte) 125, (byte) 89, (byte) 69, (byte) 63, (byte) 53, (byte) -58, (byte) -77, (byte) 86, (byte) -109, (byte) -18, (byte) 55, (byte) 126, (byte) 42, (byte) 35, (byte) -56, (byte) 7, (byte) 112, (byte) -119, (byte) 6, (byte) -110, (byte) -113, (byte) 9, (byte) -34}, new byte[]{(byte) -51, (byte) -96, (byte) 16, (byte) 8, (byte) -64, (byte) 105, (byte) 34, (byte) -6, (byte) 55, (byte) -63, (byte) 33, (byte) 62, (byte) -101, (byte) -72, (byte) 49, (byte) -95, (byte) 9, (byte) -77, (byte) 23, (byte) 69, (byte) 50, Byte.MIN_VALUE, (byte) 86, (byte) 22, (byte) -5, (byte) 113, (byte) 97, (byte) -19, (byte) -60, (byte) 3, (byte) -122, (byte) 111}, new byte[]{(byte) -67, (byte) -38, (byte) -74, (byte) 24, (byte) 77, (byte) 122, (byte) 52, (byte) 109, (byte) -90, (byte) -96, (byte) 125, (byte) -64, (byte) -126, (byte) -116, (byte) -15, (byte) -102, (byte) 105, (byte) 111, (byte) 76, (byte) -86, (byte) 54, (byte) 17, (byte) -59, (byte) 31, (byte) 46, (byte) 20, (byte) 86, (byte) 90, (byte) 20, (byte) -76, (byte) 15, (byte) -39}}));
        }

        public final void setHALInstrumentation() {
        }

        public final boolean linkToDeath(DeathRecipient recipient, long cookie) {
            return true;
        }

        public final void ping() {
        }

        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0;
            info.arch = 0;
            return info;
        }

        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        public final boolean unlinkToDeath(DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (ISupplicantP2pNetwork.kInterfaceName.equals(descriptor)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String serviceName) throws RemoteException {
            registerService(serviceName);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(interfaceDescriptor());
            stringBuilder.append("@Stub");
            return stringBuilder.toString();
        }

        public void onTransact(int _hidl_code, HwParcel _hidl_request, HwParcel _hidl_reply, int _hidl_flags) throws RemoteException {
            HwParcel hwParcel = _hidl_request;
            final HwParcel hwParcel2 = _hidl_reply;
            boolean _hidl_index_0 = false;
            boolean _hidl_is_oneway = true;
            boolean z;
            int _hidl_vec_size;
            int _hidl_vec_size2;
            HwBlob childBlob;
            int _hidl_index_02;
            switch (_hidl_code) {
                case 1:
                    z = false;
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = z;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantNetwork.kInterfaceName);
                    getId(new getIdCallback() {
                        public void onValues(SupplicantStatus status, int id) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt32(id);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 2:
                    z = false;
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = z;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantNetwork.kInterfaceName);
                    getInterfaceName(new getInterfaceNameCallback() {
                        public void onValues(SupplicantStatus status, String name) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeString(name);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 3:
                    z = false;
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = z;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantNetwork.kInterfaceName);
                    getType(new getTypeCallback() {
                        public void onValues(SupplicantStatus status, int type) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt32(type);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 4:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                    SupplicantStatus _hidl_out_status = registerCallback(ISupplicantP2pNetworkCallback.asInterface(_hidl_request.readStrongBinder()));
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 5:
                    if ((_hidl_flags & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                    getSsid(new getSsidCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Byte> ssid) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt8Vector((ArrayList) ssid);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 6:
                    if ((_hidl_flags & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                    getBssid(new getBssidCallback() {
                        public void onValues(SupplicantStatus status, byte[] bssid) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            HwBlob _hidl_blob = new HwBlob(6);
                            _hidl_blob.putInt8Array(0, bssid);
                            hwParcel2.writeBuffer(_hidl_blob);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 7:
                    if ((_hidl_flags & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                    isCurrent(new isCurrentCallback() {
                        public void onValues(SupplicantStatus status, boolean isCurrent) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeBool(isCurrent);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 8:
                    if ((_hidl_flags & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                    isPersistent(new isPersistentCallback() {
                        public void onValues(SupplicantStatus status, boolean isPersistent) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeBool(isPersistent);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 9:
                    if ((_hidl_flags & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                    isGo(new isGoCallback() {
                        public void onValues(SupplicantStatus status, boolean isGo) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeBool(isGo);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 10:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                    ArrayList<byte[]> clients = new ArrayList();
                    HwBlob _hidl_blob = hwParcel.readBuffer(16);
                    _hidl_vec_size = _hidl_blob.getInt32(8);
                    _hidl_vec_size2 = _hidl_vec_size;
                    childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size * 6), _hidl_blob.handle(), 0, 1);
                    clients.clear();
                    for (_hidl_index_02 = 0; _hidl_index_02 < _hidl_vec_size2; _hidl_index_02++) {
                        byte[] _hidl_vec_element = new byte[6];
                        childBlob.copyToInt8Array((long) (_hidl_index_02 * 6), _hidl_vec_element, 6);
                        clients.add(_hidl_vec_element);
                    }
                    SupplicantStatus _hidl_out_status2 = setClientList(clients);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 11:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                    getClientList(new getClientListCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<byte[]> clients) {
                            int _hidl_index_0 = 0;
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            HwBlob _hidl_blob = new HwBlob(16);
                            int _hidl_vec_size = clients.size();
                            _hidl_blob.putInt32(8, _hidl_vec_size);
                            _hidl_blob.putBool(12, false);
                            HwBlob childBlob = new HwBlob(_hidl_vec_size * 6);
                            while (_hidl_index_0 < _hidl_vec_size) {
                                childBlob.putInt8Array((long) (_hidl_index_0 * 6), (byte[]) clients.get(_hidl_index_0));
                                _hidl_index_0++;
                            }
                            _hidl_blob.putBlob(0, childBlob);
                            hwParcel2.writeBuffer(_hidl_blob);
                            hwParcel2.send();
                        }
                    });
                    return;
                default:
                    switch (_hidl_code) {
                        case 256067662:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway = false;
                            }
                            if (_hidl_is_oneway) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            ArrayList _hidl_out_descriptors = interfaceChain();
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeStringVector(_hidl_out_descriptors);
                            _hidl_reply.send();
                            return;
                        case 256131655:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway = false;
                            }
                            if (_hidl_is_oneway) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            hwParcel2.writeStatus(0);
                            _hidl_reply.send();
                            return;
                        case 256136003:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway = false;
                            }
                            if (_hidl_is_oneway) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            String _hidl_out_descriptor = interfaceDescriptor();
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeString(_hidl_out_descriptor);
                            _hidl_reply.send();
                            return;
                        case 256398152:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway = false;
                            }
                            if (_hidl_is_oneway) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            ArrayList<byte[]> _hidl_out_hashchain = getHashChain();
                            hwParcel2.writeStatus(0);
                            HwBlob _hidl_blob2 = new HwBlob(16);
                            _hidl_vec_size = _hidl_out_hashchain.size();
                            _hidl_blob2.putInt32(8, _hidl_vec_size);
                            _hidl_blob2.putBool(12, false);
                            childBlob = new HwBlob(_hidl_vec_size * 32);
                            while (true) {
                                _hidl_index_02 = _hidl_vec_size2;
                                if (_hidl_index_02 < _hidl_vec_size) {
                                    childBlob.putInt8Array((long) (_hidl_index_02 * 32), (byte[]) _hidl_out_hashchain.get(_hidl_index_02));
                                    _hidl_vec_size2 = _hidl_index_02 + 1;
                                } else {
                                    _hidl_blob2.putBlob(0, childBlob);
                                    hwParcel2.writeBuffer(_hidl_blob2);
                                    _hidl_reply.send();
                                    return;
                                }
                            }
                        case 256462420:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_index_0 = true;
                            }
                            if (!_hidl_index_0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            setHALInstrumentation();
                            return;
                        case 256660548:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_index_0 = true;
                            }
                            if (_hidl_index_0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            return;
                        case 256921159:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway = false;
                            }
                            if (_hidl_is_oneway) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            ping();
                            hwParcel2.writeStatus(0);
                            _hidl_reply.send();
                            return;
                        case 257049926:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway = false;
                            }
                            if (_hidl_is_oneway) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            DebugInfo _hidl_out_info = getDebugInfo();
                            hwParcel2.writeStatus(0);
                            _hidl_out_info.writeToParcel(hwParcel2);
                            _hidl_reply.send();
                            return;
                        case 257120595:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_index_0 = true;
                            }
                            if (!_hidl_index_0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            notifySyspropsChanged();
                            return;
                        case 257250372:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_index_0 = true;
                            }
                            if (_hidl_index_0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            return;
                        default:
                            return;
                    }
            }
        }
    }

    IHwBinder asBinder();

    void getBssid(getBssidCallback getbssidcallback) throws RemoteException;

    void getClientList(getClientListCallback getclientlistcallback) throws RemoteException;

    DebugInfo getDebugInfo() throws RemoteException;

    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getSsid(getSsidCallback getssidcallback) throws RemoteException;

    ArrayList<String> interfaceChain() throws RemoteException;

    String interfaceDescriptor() throws RemoteException;

    void isCurrent(isCurrentCallback iscurrentcallback) throws RemoteException;

    void isGo(isGoCallback isgocallback) throws RemoteException;

    void isPersistent(isPersistentCallback ispersistentcallback) throws RemoteException;

    boolean linkToDeath(DeathRecipient deathRecipient, long j) throws RemoteException;

    void notifySyspropsChanged() throws RemoteException;

    void ping() throws RemoteException;

    SupplicantStatus registerCallback(ISupplicantP2pNetworkCallback iSupplicantP2pNetworkCallback) throws RemoteException;

    SupplicantStatus setClientList(ArrayList<byte[]> arrayList) throws RemoteException;

    void setHALInstrumentation() throws RemoteException;

    boolean unlinkToDeath(DeathRecipient deathRecipient) throws RemoteException;

    static ISupplicantP2pNetwork asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof ISupplicantP2pNetwork)) {
            return (ISupplicantP2pNetwork) iface;
        }
        ISupplicantP2pNetwork proxy = new Proxy(binder);
        try {
            Iterator it = proxy.interfaceChain().iterator();
            while (it.hasNext()) {
                if (((String) it.next()).equals(kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    static ISupplicantP2pNetwork castFrom(IHwInterface iface) {
        return iface == null ? null : asInterface(iface.asBinder());
    }

    static ISupplicantP2pNetwork getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static ISupplicantP2pNetwork getService(boolean retry) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, retry);
    }

    static ISupplicantP2pNetwork getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static ISupplicantP2pNetwork getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }
}
