package vendor.huawei.hardware.dolby.dms.V1_0;

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
import com.android.server.display.HwUibcReceiver.CurrentPacket;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface IDms extends IBase {
    public static final String kInterfaceName = "vendor.huawei.hardware.dolby.dms@1.0::IDms";

    @FunctionalInterface
    public interface getDapParamCallback {
        void onValues(int i, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    public interface getStoredParamsCallback {
        void onValues(boolean z, ArrayList<Integer> arrayList, ArrayList<KeyValue_AudioDevice_DapParamCache> arrayList2);
    }

    public static final class Proxy implements IDms {
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
                return "[class or subclass of vendor.huawei.hardware.dolby.dms@1.0::IDms]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        public void registerClient(IDmsCallbacks callback, int type, int uid) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            _hidl_request.writeInt32(type);
            _hidl_request.writeInt32(uid);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public void unregisterClient(IDmsCallbacks callback, int type, int uid) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            _hidl_request.writeInt32(type);
            _hidl_request.writeInt32(uid);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public boolean hasDecCP() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_retval = _hidl_reply.readBool();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        public boolean isMonoInternalSpeaker() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_retval = _hidl_reply.readBool();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        public boolean getDapEnabled() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_retval = _hidl_reply.readBool();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        public boolean isSharedParam(int param) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            _hidl_request.writeInt32(param);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_retval = _hidl_reply.readBool();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getStoredParams(boolean checkToken, int curTok, getStoredParamsCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            _hidl_request.writeBool(checkToken);
            _hidl_request.writeInt32(curTok);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                _hidl_cb.onValues(_hidl_reply.readBool(), _hidl_reply.readInt32Vector(), KeyValue_AudioDevice_DapParamCache.readVectorFromParcel(_hidl_reply));
            } finally {
                _hidl_reply.release();
            }
        }

        public int setDapParam(ArrayList<Byte> values) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            _hidl_request.writeInt8Vector((ArrayList) values);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getDapParam(ArrayList<Byte> inVal, getDapParamCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            _hidl_request.writeInt8Vector((ArrayList) inVal);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                _hidl_cb.onValues(_hidl_reply.readInt32(), _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public int getAvailableTuningDevicesLen(int port) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            _hidl_request.writeInt32(port);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        public ArrayList<Byte> getAvailableTuningDevices(int port) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            _hidl_request.writeInt32(port);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<Byte> _hidl_out_buf = _hidl_reply.readInt8Vector();
                return _hidl_out_buf;
            } finally {
                _hidl_reply.release();
            }
        }

        public ArrayList<Byte> getSelectedTuningDevice(int port) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            _hidl_request.writeInt32(port);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<Byte> _hidl_out_buf = _hidl_reply.readInt8Vector();
                return _hidl_out_buf;
            } finally {
                _hidl_reply.release();
            }
        }

        public void setSelectedTuningDevice(int port, ArrayList<Byte> buf) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            _hidl_request.writeInt32(port);
            _hidl_request.writeInt8Vector((ArrayList) buf);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public void setActiveDevice(int device) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            _hidl_request.writeInt32(device);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(14, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public int getActiveDevice() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(15, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        public void setChannelCount(int count) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            _hidl_request.writeInt32(count);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(16, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public int getChannelCount() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IDms.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(17, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
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

    public static abstract class Stub extends HwBinder implements IDms {
        public IHwBinder asBinder() {
            return this;
        }

        public final ArrayList<String> interfaceChain() {
            return new ArrayList(Arrays.asList(new String[]{IDms.kInterfaceName, IBase.kInterfaceName}));
        }

        public final String interfaceDescriptor() {
            return IDms.kInterfaceName;
        }

        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList(Arrays.asList(new byte[][]{new byte[]{(byte) 4, (byte) -48, (byte) -117, (byte) -1, (byte) 0, (byte) 78, (byte) 52, (byte) 116, (byte) -115, (byte) 56, (byte) 56, (byte) 16, (byte) -107, (byte) -8, (byte) -70, (byte) 53, (byte) -52, (byte) -121, (byte) 66, (byte) -89, (byte) -52, (byte) -114, (byte) 9, (byte) 74, (byte) 118, (byte) -109, (byte) -38, (byte) 44, (byte) -51, (byte) -45, (byte) 74, (byte) -27}, new byte[]{(byte) -67, (byte) -38, (byte) -74, (byte) 24, (byte) 77, (byte) 122, (byte) 52, (byte) 109, (byte) -90, (byte) -96, (byte) 125, (byte) -64, (byte) -126, (byte) -116, (byte) -15, (byte) -102, (byte) 105, (byte) 111, (byte) 76, (byte) -86, (byte) 54, (byte) 17, (byte) -59, (byte) 31, (byte) 46, (byte) 20, (byte) 86, (byte) 90, (byte) 20, (byte) -76, CurrentPacket.INPUT_MASK, (byte) -39}}));
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
            if (IDms.kInterfaceName.equals(descriptor)) {
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

        public void onTransact(int _hidl_code, HwParcel _hidl_request, final HwParcel _hidl_reply, int _hidl_flags) throws RemoteException {
            boolean _hidl_is_oneway = false;
            boolean _hidl_is_oneway2 = true;
            boolean _hidl_out_retval;
            int _hidl_out_retval2;
            ArrayList _hidl_out_buf;
            int _hidl_out_retval3;
            switch (_hidl_code) {
                case 1:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    registerClient(IDmsCallbacks.asInterface(_hidl_request.readStrongBinder()), _hidl_request.readInt32(), _hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 2:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    unregisterClient(IDmsCallbacks.asInterface(_hidl_request.readStrongBinder()), _hidl_request.readInt32(), _hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 3:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    _hidl_out_retval = hasDecCP();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_retval);
                    _hidl_reply.send();
                    return;
                case 4:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    _hidl_out_retval = isMonoInternalSpeaker();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_retval);
                    _hidl_reply.send();
                    return;
                case 5:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    _hidl_out_retval = getDapEnabled();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_retval);
                    _hidl_reply.send();
                    return;
                case 6:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    boolean _hidl_out_retval4 = isSharedParam(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_retval4);
                    _hidl_reply.send();
                    return;
                case 7:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    getStoredParams(_hidl_request.readBool(), _hidl_request.readInt32(), new getStoredParamsCallback() {
                        public void onValues(boolean retval, ArrayList<Integer> token, ArrayList<KeyValue_AudioDevice_DapParamCache> paramStore) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeBool(retval);
                            _hidl_reply.writeInt32Vector((ArrayList) token);
                            KeyValue_AudioDevice_DapParamCache.writeVectorToParcel(_hidl_reply, paramStore);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 8:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    _hidl_out_retval2 = setDapParam(_hidl_request.readInt8Vector());
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval2);
                    _hidl_reply.send();
                    return;
                case 9:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    getDapParam(_hidl_request.readInt8Vector(), new getDapParamCallback() {
                        public void onValues(int retval, ArrayList<Byte> outVal) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(retval);
                            _hidl_reply.writeInt8Vector((ArrayList) outVal);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 10:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    _hidl_out_retval2 = getAvailableTuningDevicesLen(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval2);
                    _hidl_reply.send();
                    return;
                case 11:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    _hidl_out_buf = getAvailableTuningDevices(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt8Vector(_hidl_out_buf);
                    _hidl_reply.send();
                    return;
                case 12:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    _hidl_out_buf = getSelectedTuningDevice(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt8Vector(_hidl_out_buf);
                    _hidl_reply.send();
                    return;
                case 13:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    setSelectedTuningDevice(_hidl_request.readInt32(), _hidl_request.readInt8Vector());
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 14:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    setActiveDevice(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 15:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    _hidl_out_retval3 = getActiveDevice();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval3);
                    _hidl_reply.send();
                    return;
                case 16:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    setChannelCount(_hidl_request.readInt32());
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 17:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IDms.kInterfaceName);
                    _hidl_out_retval3 = getChannelCount();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval3);
                    _hidl_reply.send();
                    return;
                default:
                    switch (_hidl_code) {
                        case 256067662:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            ArrayList _hidl_out_descriptors = interfaceChain();
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeStringVector(_hidl_out_descriptors);
                            _hidl_reply.send();
                            return;
                        case 256131655:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.send();
                            return;
                        case 256136003:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            String _hidl_out_descriptor = interfaceDescriptor();
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeString(_hidl_out_descriptor);
                            _hidl_reply.send();
                            return;
                        case 256398152:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            ArrayList<byte[]> _hidl_out_hashchain = getHashChain();
                            _hidl_reply.writeStatus(0);
                            HwBlob _hidl_blob = new HwBlob(16);
                            int _hidl_vec_size = _hidl_out_hashchain.size();
                            _hidl_blob.putInt32(8, _hidl_vec_size);
                            _hidl_blob.putBool(12, false);
                            HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
                            int _hidl_index_0;
                            while (_hidl_index_0 < _hidl_vec_size) {
                                childBlob.putInt8Array((long) (_hidl_index_0 * 32), (byte[]) _hidl_out_hashchain.get(_hidl_index_0));
                                _hidl_index_0++;
                            }
                            _hidl_blob.putBlob(0, childBlob);
                            _hidl_reply.writeBuffer(_hidl_blob);
                            _hidl_reply.send();
                            return;
                        case 256462420:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (!_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            setHALInstrumentation();
                            return;
                        case 256660548:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            return;
                        case 256921159:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            ping();
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.send();
                            return;
                        case 257049926:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            DebugInfo _hidl_out_info = getDebugInfo();
                            _hidl_reply.writeStatus(0);
                            _hidl_out_info.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                            return;
                        case 257120595:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (!_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            notifySyspropsChanged();
                            return;
                        case 257250372:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
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

    int getActiveDevice() throws RemoteException;

    ArrayList<Byte> getAvailableTuningDevices(int i) throws RemoteException;

    int getAvailableTuningDevicesLen(int i) throws RemoteException;

    int getChannelCount() throws RemoteException;

    boolean getDapEnabled() throws RemoteException;

    void getDapParam(ArrayList<Byte> arrayList, getDapParamCallback getdapparamcallback) throws RemoteException;

    DebugInfo getDebugInfo() throws RemoteException;

    ArrayList<byte[]> getHashChain() throws RemoteException;

    ArrayList<Byte> getSelectedTuningDevice(int i) throws RemoteException;

    void getStoredParams(boolean z, int i, getStoredParamsCallback getstoredparamscallback) throws RemoteException;

    boolean hasDecCP() throws RemoteException;

    ArrayList<String> interfaceChain() throws RemoteException;

    String interfaceDescriptor() throws RemoteException;

    boolean isMonoInternalSpeaker() throws RemoteException;

    boolean isSharedParam(int i) throws RemoteException;

    boolean linkToDeath(DeathRecipient deathRecipient, long j) throws RemoteException;

    void notifySyspropsChanged() throws RemoteException;

    void ping() throws RemoteException;

    void registerClient(IDmsCallbacks iDmsCallbacks, int i, int i2) throws RemoteException;

    void setActiveDevice(int i) throws RemoteException;

    void setChannelCount(int i) throws RemoteException;

    int setDapParam(ArrayList<Byte> arrayList) throws RemoteException;

    void setHALInstrumentation() throws RemoteException;

    void setSelectedTuningDevice(int i, ArrayList<Byte> arrayList) throws RemoteException;

    boolean unlinkToDeath(DeathRecipient deathRecipient) throws RemoteException;

    void unregisterClient(IDmsCallbacks iDmsCallbacks, int i, int i2) throws RemoteException;

    static IDms asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IDms)) {
            return (IDms) iface;
        }
        IDms proxy = new Proxy(binder);
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

    static IDms castFrom(IHwInterface iface) {
        return iface == null ? null : asInterface(iface.asBinder());
    }

    static IDms getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IDms getService(boolean retry) throws RemoteException {
        return getService(MemoryConstant.MEM_SCENE_DEFAULT, retry);
    }

    static IDms getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static IDms getService() throws RemoteException {
        return getService(MemoryConstant.MEM_SCENE_DEFAULT);
    }
}
