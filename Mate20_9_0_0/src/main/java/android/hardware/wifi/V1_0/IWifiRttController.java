package android.hardware.wifi.V1_0;

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

public interface IWifiRttController extends IBase {
    public static final String kInterfaceName = "android.hardware.wifi@1.0::IWifiRttController";

    @FunctionalInterface
    public interface getBoundIfaceCallback {
        void onValues(WifiStatus wifiStatus, IWifiIface iWifiIface);
    }

    @FunctionalInterface
    public interface getCapabilitiesCallback {
        void onValues(WifiStatus wifiStatus, RttCapabilities rttCapabilities);
    }

    @FunctionalInterface
    public interface getResponderInfoCallback {
        void onValues(WifiStatus wifiStatus, RttResponder rttResponder);
    }

    public static final class Proxy implements IWifiRttController {
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
                return "[class or subclass of android.hardware.wifi@1.0::IWifiRttController]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        public void getBoundIface(getBoundIfaceCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiRttController.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, IWifiIface.asInterface(_hidl_reply.readStrongBinder()));
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus registerEventCallback(IWifiRttControllerEventCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiRttController.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus rangeRequest(int cmdId, ArrayList<RttConfig> rttConfigs) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiRttController.kInterfaceName);
            _hidl_request.writeInt32(cmdId);
            RttConfig.writeVectorToParcel(_hidl_request, rttConfigs);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus rangeCancel(int cmdId, ArrayList<byte[]> addrs) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiRttController.kInterfaceName);
            _hidl_request.writeInt32(cmdId);
            HwBlob _hidl_blob = new HwBlob(16);
            int _hidl_vec_size = addrs.size();
            _hidl_blob.putInt32(8, _hidl_vec_size);
            _hidl_blob.putBool(12, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 6);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                childBlob.putInt8Array((long) (_hidl_index_0 * 6), (byte[]) addrs.get(_hidl_index_0));
            }
            _hidl_blob.putBlob(0, childBlob);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getCapabilities(getCapabilitiesCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiRttController.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                RttCapabilities _hidl_out_capabilities = new RttCapabilities();
                _hidl_out_capabilities.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_capabilities);
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus setLci(int cmdId, RttLciInformation lci) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiRttController.kInterfaceName);
            _hidl_request.writeInt32(cmdId);
            lci.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus setLcr(int cmdId, RttLcrInformation lcr) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiRttController.kInterfaceName);
            _hidl_request.writeInt32(cmdId);
            lcr.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getResponderInfo(getResponderInfoCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiRttController.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                RttResponder _hidl_out_info = new RttResponder();
                _hidl_out_info.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_info);
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus enableResponder(int cmdId, WifiChannelInfo channelHint, int maxDurationSeconds, RttResponder info) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiRttController.kInterfaceName);
            _hidl_request.writeInt32(cmdId);
            channelHint.writeToParcel(_hidl_request);
            _hidl_request.writeInt32(maxDurationSeconds);
            info.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus disableResponder(int cmdId) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiRttController.kInterfaceName);
            _hidl_request.writeInt32(cmdId);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
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

    public static abstract class Stub extends HwBinder implements IWifiRttController {
        public IHwBinder asBinder() {
            return this;
        }

        public final ArrayList<String> interfaceChain() {
            return new ArrayList(Arrays.asList(new String[]{IWifiRttController.kInterfaceName, IBase.kInterfaceName}));
        }

        public final String interfaceDescriptor() {
            return IWifiRttController.kInterfaceName;
        }

        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList(Arrays.asList(new byte[][]{new byte[]{(byte) 118, (byte) 110, (byte) -105, (byte) 101, (byte) -11, (byte) -55, (byte) -57, (byte) 89, (byte) -78, (byte) -89, (byte) 99, (byte) -62, (byte) 40, (byte) -125, (byte) 83, (byte) -5, (byte) 93, (byte) -17, (byte) -13, (byte) 56, (byte) -100, (byte) 44, (byte) -62, (byte) -113, (byte) -127, (byte) -41, (byte) -100, (byte) -109, (byte) -105, (byte) 4, (byte) -50, (byte) -117}, new byte[]{(byte) -67, (byte) -38, (byte) -74, (byte) 24, (byte) 77, (byte) 122, (byte) 52, (byte) 109, (byte) -90, (byte) -96, (byte) 125, (byte) -64, (byte) -126, (byte) -116, (byte) -15, (byte) -102, (byte) 105, (byte) 111, (byte) 76, (byte) -86, (byte) 54, (byte) 17, (byte) -59, (byte) 31, (byte) 46, (byte) 20, (byte) 86, (byte) 90, (byte) 20, (byte) -76, (byte) 15, (byte) -39}}));
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
            if (IWifiRttController.kInterfaceName.equals(descriptor)) {
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
            WifiStatus _hidl_out_status;
            int _hidl_vec_size;
            HwBlob childBlob;
            int _hidl_index_02;
            WifiStatus _hidl_out_status2;
            switch (_hidl_code) {
                case 1:
                    boolean z = false;
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = z;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                    getBoundIface(new getBoundIfaceCallback() {
                        public void onValues(WifiStatus status, IWifiIface boundIface) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeStrongBinder(boundIface == null ? null : boundIface.asBinder());
                            hwParcel2.send();
                        }
                    });
                    return;
                case 2:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                    WifiStatus _hidl_out_status3 = registerEventCallback(IWifiRttControllerEventCallback.asInterface(_hidl_request.readStrongBinder()));
                    hwParcel2.writeStatus(0);
                    _hidl_out_status3.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 3:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                    _hidl_out_status = rangeRequest(_hidl_request.readInt32(), RttConfig.readVectorFromParcel(_hidl_request));
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
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
                    hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                    int cmdId = _hidl_request.readInt32();
                    ArrayList<byte[]> addrs = new ArrayList();
                    HwBlob _hidl_blob = hwParcel.readBuffer(16);
                    int _hidl_vec_size2 = _hidl_blob.getInt32(8);
                    _hidl_vec_size = _hidl_vec_size2;
                    ArrayList<byte[]> addrs2 = addrs;
                    childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 6), _hidl_blob.handle(), 0, 1);
                    addrs2.clear();
                    for (_hidl_index_02 = 0; _hidl_index_02 < _hidl_vec_size; _hidl_index_02++) {
                        byte[] _hidl_vec_element = new byte[6];
                        childBlob.copyToInt8Array((long) (_hidl_index_02 * 6), _hidl_vec_element, 6);
                        addrs2.add(_hidl_vec_element);
                    }
                    WifiStatus _hidl_out_status4 = rangeCancel(cmdId, addrs2);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status4.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 5:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                    getCapabilities(new getCapabilitiesCallback() {
                        public void onValues(WifiStatus status, RttCapabilities capabilities) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            capabilities.writeToParcel(hwParcel2);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 6:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                    _hidl_index_02 = _hidl_request.readInt32();
                    RttLciInformation lci = new RttLciInformation();
                    lci.readFromParcel(hwParcel);
                    _hidl_out_status2 = setLci(_hidl_index_02, lci);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 7:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                    _hidl_index_02 = _hidl_request.readInt32();
                    RttLcrInformation lcr = new RttLcrInformation();
                    lcr.readFromParcel(hwParcel);
                    _hidl_out_status2 = setLcr(_hidl_index_02, lcr);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 8:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                    getResponderInfo(new getResponderInfoCallback() {
                        public void onValues(WifiStatus status, RttResponder info) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            info.writeToParcel(hwParcel2);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 9:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                    _hidl_index_02 = _hidl_request.readInt32();
                    WifiChannelInfo channelHint = new WifiChannelInfo();
                    channelHint.readFromParcel(hwParcel);
                    int maxDurationSeconds = _hidl_request.readInt32();
                    RttResponder info = new RttResponder();
                    info.readFromParcel(hwParcel);
                    WifiStatus _hidl_out_status5 = enableResponder(_hidl_index_02, channelHint, maxDurationSeconds, info);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status5.writeToParcel(hwParcel2);
                    _hidl_reply.send();
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
                    hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                    _hidl_out_status = disableResponder(_hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
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
                            int _hidl_vec_size3 = _hidl_out_hashchain.size();
                            _hidl_blob2.putInt32(8, _hidl_vec_size3);
                            _hidl_blob2.putBool(12, false);
                            childBlob = new HwBlob(_hidl_vec_size3 * 32);
                            while (true) {
                                _hidl_index_02 = _hidl_vec_size;
                                if (_hidl_index_02 < _hidl_vec_size3) {
                                    childBlob.putInt8Array((long) (_hidl_index_02 * 32), (byte[]) _hidl_out_hashchain.get(_hidl_index_02));
                                    _hidl_vec_size = _hidl_index_02 + 1;
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

    WifiStatus disableResponder(int i) throws RemoteException;

    WifiStatus enableResponder(int i, WifiChannelInfo wifiChannelInfo, int i2, RttResponder rttResponder) throws RemoteException;

    void getBoundIface(getBoundIfaceCallback getboundifacecallback) throws RemoteException;

    void getCapabilities(getCapabilitiesCallback getcapabilitiescallback) throws RemoteException;

    DebugInfo getDebugInfo() throws RemoteException;

    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getResponderInfo(getResponderInfoCallback getresponderinfocallback) throws RemoteException;

    ArrayList<String> interfaceChain() throws RemoteException;

    String interfaceDescriptor() throws RemoteException;

    boolean linkToDeath(DeathRecipient deathRecipient, long j) throws RemoteException;

    void notifySyspropsChanged() throws RemoteException;

    void ping() throws RemoteException;

    WifiStatus rangeCancel(int i, ArrayList<byte[]> arrayList) throws RemoteException;

    WifiStatus rangeRequest(int i, ArrayList<RttConfig> arrayList) throws RemoteException;

    WifiStatus registerEventCallback(IWifiRttControllerEventCallback iWifiRttControllerEventCallback) throws RemoteException;

    void setHALInstrumentation() throws RemoteException;

    WifiStatus setLci(int i, RttLciInformation rttLciInformation) throws RemoteException;

    WifiStatus setLcr(int i, RttLcrInformation rttLcrInformation) throws RemoteException;

    boolean unlinkToDeath(DeathRecipient deathRecipient) throws RemoteException;

    static IWifiRttController asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IWifiRttController)) {
            return (IWifiRttController) iface;
        }
        IWifiRttController proxy = new Proxy(binder);
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

    static IWifiRttController castFrom(IHwInterface iface) {
        return iface == null ? null : asInterface(iface.asBinder());
    }

    static IWifiRttController getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IWifiRttController getService(boolean retry) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, retry);
    }

    static IWifiRttController getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static IWifiRttController getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }
}
