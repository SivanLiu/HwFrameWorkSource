package vendor.huawei.hardware.wifi.supplicant.V2_0;

import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.addNetworkCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.getNameCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.getNetworkCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.getTypeCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.listNetworksCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.FreqRange;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.connectCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.createNfcHandoverRequestMessageCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.createNfcHandoverSelectMessageCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.getDeviceAddressCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.getGroupCapabilityCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.getSsidCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.requestServiceDiscoveryCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.startWpsPinDisplayCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIfaceCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.ReasonCode;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.StatusCode;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
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

public interface ISupplicantP2pIface extends android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface {
    public static final String kInterfaceName = "vendor.huawei.hardware.wifi.supplicant@2.0::ISupplicantP2pIface";

    @FunctionalInterface
    public interface getP2pLinkspeedCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    public static final class Proxy implements ISupplicantP2pIface {
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
                return "[class or subclass of vendor.huawei.hardware.wifi.supplicant@2.0::ISupplicantP2pIface]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        public void getName(getNameCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
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
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void addNetwork(addNetworkCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, ISupplicantNetwork.asInterface(_hidl_reply.readStrongBinder()));
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus removeNetwork(int id) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeInt32(id);
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

        public void getNetwork(int id, getNetworkCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeInt32(id);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, ISupplicantNetwork.asInterface(_hidl_reply.readStrongBinder()));
            } finally {
                _hidl_reply.release();
            }
        }

        public void listNetworks(listNetworksCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsDeviceName(String name) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(name);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsDeviceType(byte[] type) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(8);
            _hidl_blob.putInt8Array(0, type);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsManufacturer(String manufacturer) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(manufacturer);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsModelName(String modelName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(modelName);
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

        public SupplicantStatus setWpsModelNumber(String modelNumber) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(modelNumber);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsSerialNumber(String serialNumber) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeString(serialNumber);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWpsConfigMethods(short configMethods) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            _hidl_request.writeInt16(configMethods);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus registerCallback(ISupplicantP2pIfaceCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(14, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getDeviceAddress(getDeviceAddressCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(15, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                byte[] _hidl_out_deviceAddress = new byte[6];
                _hidl_reply.readBuffer(6).copyToInt8Array(0, _hidl_out_deviceAddress, 6);
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_deviceAddress);
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setSsidPostfix(ArrayList<Byte> postfix) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt8Vector((ArrayList) postfix);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(16, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setGroupIdle(String groupIfName, int timeoutInSec) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            _hidl_request.writeInt32(timeoutInSec);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(17, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setPowerSave(String groupIfName, boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(18, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus find(int timeoutInSec) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt32(timeoutInSec);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(19, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus stopFind() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(20, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus flush() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(21, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void connect(byte[] peerAddress, int provisionMethod, String preSelectedPin, boolean joinExistingGroup, boolean persistent, int goIntent, connectCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, peerAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeInt32(provisionMethod);
            _hidl_request.writeString(preSelectedPin);
            _hidl_request.writeBool(joinExistingGroup);
            _hidl_request.writeBool(persistent);
            _hidl_request.writeInt32(goIntent);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(22, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus cancelConnect() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(23, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus provisionDiscovery(byte[] peerAddress, int provisionMethod) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, peerAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeInt32(provisionMethod);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(24, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus addGroup(boolean persistent, int persistentNetworkId) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeBool(persistent);
            _hidl_request.writeInt32(persistentNetworkId);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(25, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus removeGroup(String groupIfName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(26, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus reject(byte[] peerAddress) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, peerAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(27, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus invite(String groupIfName, byte[] goDeviceAddress, byte[] peerAddress) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            HwBlob _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, goDeviceAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, peerAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(28, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus reinvoke(int persistentNetworkId, byte[] peerAddress) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt32(persistentNetworkId);
            HwBlob _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, peerAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(29, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus configureExtListen(int periodInMillis, int intervalInMillis) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt32(periodInMillis);
            _hidl_request.writeInt32(intervalInMillis);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(30, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setListenChannel(int channel, int operatingClass) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt32(channel);
            _hidl_request.writeInt32(operatingClass);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(31, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setDisallowedFrequencies(ArrayList<FreqRange> ranges) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            FreqRange.writeVectorToParcel(_hidl_request, ranges);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(32, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getSsid(byte[] peerAddress, getSsidCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, peerAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(33, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getGroupCapability(byte[] peerAddress, getGroupCapabilityCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, peerAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(34, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus addBonjourService(ArrayList<Byte> query, ArrayList<Byte> response) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt8Vector((ArrayList) query);
            _hidl_request.writeInt8Vector((ArrayList) response);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(35, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus removeBonjourService(ArrayList<Byte> query) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt8Vector((ArrayList) query);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(36, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus addUpnpService(int version, String serviceName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt32(version);
            _hidl_request.writeString(serviceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(37, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus removeUpnpService(int version, String serviceName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt32(version);
            _hidl_request.writeString(serviceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(38, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus flushServices() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(39, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void requestServiceDiscovery(byte[] peerAddress, ArrayList<Byte> query, requestServiceDiscoveryCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, peerAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeInt8Vector((ArrayList) query);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(40, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt64());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus cancelServiceDiscovery(long identifier) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt64(identifier);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(41, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setMiracastMode(byte mode) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt8(mode);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(42, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus startWpsPbc(String groupIfName, byte[] bssid) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            HwBlob _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, bssid);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(43, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus startWpsPinKeypad(String groupIfName, String pin) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            _hidl_request.writeString(pin);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(44, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void startWpsPinDisplay(String groupIfName, byte[] bssid, startWpsPinDisplayCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            HwBlob _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, bssid);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(45, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus cancelWps(String groupIfName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(groupIfName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(46, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus enableWfd(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(47, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus setWfdDeviceInfo(byte[] info) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, info);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(48, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void createNfcHandoverRequestMessage(createNfcHandoverRequestMessageCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(49, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public void createNfcHandoverSelectMessage(createNfcHandoverSelectMessageCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(50, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus reportNfcHandoverResponse(ArrayList<Byte> request) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt8Vector((ArrayList) request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(51, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus reportNfcHandoverInitiation(ArrayList<Byte> select) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeInt8Vector((ArrayList) select);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(52, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus saveConfig() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(53, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus hwP2pRegisterCallback(ISupplicantP2pIfaceCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(54, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus addGroupWithFreq(boolean persistent, int persistentNetworkId, String s_freq) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeBool(persistent);
            _hidl_request.writeInt32(persistentNetworkId);
            _hidl_request.writeString(s_freq);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(55, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus magiclinkConnect(String cmd) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(cmd);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(56, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public SupplicantStatus rptP2pAddGroup(String param) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(param);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(57, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getP2pLinkspeed(String ifaceName, getP2pLinkspeedCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISupplicantP2pIface.kInterfaceName);
            _hidl_request.writeString(ifaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(58, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                SupplicantStatus _hidl_out_status = new SupplicantStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
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

    public static abstract class Stub extends HwBinder implements ISupplicantP2pIface {
        public IHwBinder asBinder() {
            return this;
        }

        public final ArrayList<String> interfaceChain() {
            return new ArrayList(Arrays.asList(new String[]{ISupplicantP2pIface.kInterfaceName, android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName, ISupplicantIface.kInterfaceName, IBase.kInterfaceName}));
        }

        public final String interfaceDescriptor() {
            return ISupplicantP2pIface.kInterfaceName;
        }

        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList(Arrays.asList(new byte[][]{new byte[]{(byte) 71, (byte) 24, (byte) 106, (byte) -85, (byte) -19, (byte) 1, (byte) 120, (byte) -43, (byte) 76, (byte) -21, (byte) -87, (byte) 73, (byte) -76, (byte) -29, (byte) 71, (byte) -68, (byte) 121, (byte) -91, (byte) -99, (byte) 116, (byte) -46, (byte) -118, (byte) 13, (byte) 60, (byte) 53, (byte) 0, (byte) -71, (byte) -116, (byte) 80, (byte) 83, (byte) 69, (byte) -25}, new byte[]{(byte) 73, (byte) 7, (byte) 65, (byte) 3, (byte) 56, (byte) -59, (byte) -24, (byte) -37, (byte) -18, (byte) -60, (byte) -75, (byte) -19, (byte) -62, (byte) 96, (byte) -114, (byte) -93, (byte) 35, (byte) -11, (byte) 86, (byte) 25, (byte) 69, (byte) -8, (byte) -127, (byte) 10, (byte) -8, (byte) 24, (byte) 16, (byte) -60, (byte) 123, (byte) 1, (byte) -111, (byte) -124}, new byte[]{(byte) 53, (byte) -70, (byte) 123, (byte) -51, (byte) -15, (byte) -113, (byte) 36, (byte) -88, (byte) 102, (byte) -89, (byte) -27, (byte) 66, (byte) -107, (byte) 72, (byte) -16, (byte) 103, (byte) 104, (byte) -69, (byte) 32, (byte) -94, (byte) 87, (byte) -9, (byte) 91, (byte) 16, (byte) -93, (byte) -105, (byte) -60, (byte) -40, (byte) 37, (byte) -17, (byte) -124, (byte) 56}, new byte[]{(byte) -67, (byte) -38, (byte) -74, (byte) 24, (byte) 77, (byte) 122, (byte) 52, (byte) 109, (byte) -90, (byte) -96, (byte) 125, (byte) -64, (byte) -126, (byte) -116, (byte) -15, (byte) -102, (byte) 105, (byte) 111, (byte) 76, (byte) -86, (byte) 54, (byte) 17, (byte) -59, (byte) 31, (byte) 46, (byte) 20, (byte) 86, (byte) 90, (byte) 20, (byte) -76, (byte) 15, (byte) -39}}));
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
            if (ISupplicantP2pIface.kInterfaceName.equals(descriptor)) {
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
            boolean z = true;
            SupplicantStatus _hidl_out_status;
            SupplicantStatus _hidl_out_status2;
            SupplicantStatus _hidl_out_status3;
            byte[] peerAddress;
            String groupIfName;
            byte[] goDeviceAddress;
            int persistentNetworkId;
            switch (_hidl_code) {
                case 1:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    getName(new getNameCallback() {
                        public void onValues(SupplicantStatus status, String name) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeString(name);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 2:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    getType(new getTypeCallback() {
                        public void onValues(SupplicantStatus status, int type) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt32(type);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 3:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    addNetwork(new addNetworkCallback() {
                        public void onValues(SupplicantStatus status, ISupplicantNetwork network) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeStrongBinder(network == null ? null : network.asBinder());
                            hwParcel2.send();
                        }
                    });
                    return;
                case 4:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = removeNetwork(_hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
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
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    getNetwork(_hidl_request.readInt32(), new getNetworkCallback() {
                        public void onValues(SupplicantStatus status, ISupplicantNetwork network) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeStrongBinder(network == null ? null : network.asBinder());
                            hwParcel2.send();
                        }
                    });
                    return;
                case 6:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    listNetworks(new listNetworksCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Integer> networkIds) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt32Vector((ArrayList) networkIds);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 7:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = setWpsDeviceName(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 8:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    byte[] type = new byte[8];
                    hwParcel.readBuffer(8).copyToInt8Array(0, type, 8);
                    SupplicantStatus _hidl_out_status4 = setWpsDeviceType(type);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status4.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 9:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = setWpsManufacturer(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 10:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = setWpsModelName(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 11:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = setWpsModelNumber(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 12:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = setWpsSerialNumber(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 13:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                    _hidl_out_status = setWpsConfigMethods(_hidl_request.readInt16());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 14:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = registerCallback(ISupplicantP2pIfaceCallback.asInterface(_hidl_request.readStrongBinder()));
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 15:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    getDeviceAddress(new getDeviceAddressCallback() {
                        public void onValues(SupplicantStatus status, byte[] deviceAddress) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            HwBlob _hidl_blob = new HwBlob(6);
                            _hidl_blob.putInt8Array(0, deviceAddress);
                            hwParcel2.writeBuffer(_hidl_blob);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 16:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = setSsidPostfix(_hidl_request.readInt8Vector());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 17:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status2 = setGroupIdle(_hidl_request.readString(), _hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 18:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status2 = setPowerSave(_hidl_request.readString(), _hidl_request.readBool());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 19:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = find(_hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 20:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status3 = stopFind();
                    hwParcel2.writeStatus(0);
                    _hidl_out_status3.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case ReasonCode.UNSUPPORTED_RSN_IE_VERSION /*21*/:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status3 = flush();
                    hwParcel2.writeStatus(0);
                    _hidl_out_status3.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 22:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    byte[] peerAddress2 = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress2, 6);
                    int provisionMethod = _hidl_request.readInt32();
                    String preSelectedPin = _hidl_request.readString();
                    boolean joinExistingGroup = _hidl_request.readBool();
                    boolean persistent = _hidl_request.readBool();
                    connect(peerAddress2, provisionMethod, preSelectedPin, joinExistingGroup, persistent, _hidl_request.readInt32(), new connectCallback() {
                        public void onValues(SupplicantStatus status, String generatedPin) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeString(generatedPin);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 23:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status3 = cancelConnect();
                    hwParcel2.writeStatus(0);
                    _hidl_out_status3.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 24:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    peerAddress = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress, 6);
                    _hidl_out_status2 = provisionDiscovery(peerAddress, _hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 25:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status2 = addGroup(_hidl_request.readBool(), _hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case ReasonCode.TDLS_TEARDOWN_UNSPECIFIED /*26*/:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = removeGroup(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 27:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    peerAddress = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress, 6);
                    _hidl_out_status = reject(peerAddress);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 28:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    groupIfName = _hidl_request.readString();
                    goDeviceAddress = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, goDeviceAddress, 6);
                    byte[] peerAddress3 = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress3, 6);
                    _hidl_out_status = invite(groupIfName, goDeviceAddress, peerAddress3);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 29:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    persistentNetworkId = _hidl_request.readInt32();
                    goDeviceAddress = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, goDeviceAddress, 6);
                    _hidl_out_status = reinvoke(persistentNetworkId, goDeviceAddress);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 30:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status2 = configureExtListen(_hidl_request.readInt32(), _hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 31:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status2 = setListenChannel(_hidl_request.readInt32(), _hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 32:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = setDisallowedFrequencies(FreqRange.readVectorFromParcel(_hidl_request));
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 33:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    peerAddress = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress, 6);
                    getSsid(peerAddress, new getSsidCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Byte> ssid) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt8Vector((ArrayList) ssid);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 34:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    peerAddress = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress, 6);
                    getGroupCapability(peerAddress, new getGroupCapabilityCallback() {
                        public void onValues(SupplicantStatus status, int capabilities) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt32(capabilities);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 35:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status2 = addBonjourService(_hidl_request.readInt8Vector(), _hidl_request.readInt8Vector());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case ReasonCode.STA_LEAVING /*36*/:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = removeBonjourService(_hidl_request.readInt8Vector());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 37:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status2 = addUpnpService(_hidl_request.readInt32(), _hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 38:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status2 = removeUpnpService(_hidl_request.readInt32(), _hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 39:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status3 = flushServices();
                    hwParcel2.writeStatus(0);
                    _hidl_out_status3.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case StatusCode.INVALID_IE /*40*/:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    peerAddress = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress, 6);
                    requestServiceDiscovery(peerAddress, _hidl_request.readInt8Vector(), new requestServiceDiscoveryCallback() {
                        public void onValues(SupplicantStatus status, long identifier) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt64(identifier);
                            hwParcel2.send();
                        }
                    });
                    return;
                case StatusCode.GROUP_CIPHER_NOT_VALID /*41*/:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status2 = cancelServiceDiscovery(_hidl_request.readInt64());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 42:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = setMiracastMode(_hidl_request.readInt8());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 43:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    groupIfName = _hidl_request.readString();
                    goDeviceAddress = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, goDeviceAddress, 6);
                    _hidl_out_status = startWpsPbc(groupIfName, goDeviceAddress);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case StatusCode.UNSUPPORTED_RSN_IE_VERSION /*44*/:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status2 = startWpsPinKeypad(_hidl_request.readString(), _hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 45:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    groupIfName = _hidl_request.readString();
                    goDeviceAddress = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, goDeviceAddress, 6);
                    startWpsPinDisplay(groupIfName, goDeviceAddress, new startWpsPinDisplayCallback() {
                        public void onValues(SupplicantStatus status, String generatedPin) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeString(generatedPin);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 46:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = cancelWps(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 47:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = enableWfd(_hidl_request.readBool());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 48:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    peerAddress = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, peerAddress, 6);
                    _hidl_out_status = setWfdDeviceInfo(peerAddress);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 49:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    createNfcHandoverRequestMessage(new createNfcHandoverRequestMessageCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Byte> request) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt8Vector((ArrayList) request);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 50:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    createNfcHandoverSelectMessage(new createNfcHandoverSelectMessageCallback() {
                        public void onValues(SupplicantStatus status, ArrayList<Byte> select) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt8Vector((ArrayList) select);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 51:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = reportNfcHandoverResponse(_hidl_request.readInt8Vector());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 52:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = reportNfcHandoverInitiation(_hidl_request.readInt8Vector());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 53:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status3 = saveConfig();
                    hwParcel2.writeStatus(0);
                    _hidl_out_status3.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 54:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = hwP2pRegisterCallback(ISupplicantP2pIfaceCallback.asInterface(_hidl_request.readStrongBinder()));
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 55:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pIface.kInterfaceName);
                    SupplicantStatus _hidl_out_status5 = addGroupWithFreq(_hidl_request.readBool(), _hidl_request.readInt32(), _hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status5.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 56:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = magiclinkConnect(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 57:
                    if ((_hidl_flags & 1) == 0) {
                        z = false;
                    }
                    if (z) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pIface.kInterfaceName);
                    _hidl_out_status = rptP2pAddGroup(_hidl_request.readString());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 58:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(ISupplicantP2pIface.kInterfaceName);
                    getP2pLinkspeed(_hidl_request.readString(), new getP2pLinkspeedCallback() {
                        public void onValues(SupplicantStatus status, int rate) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt32(rate);
                            hwParcel2.send();
                        }
                    });
                    return;
                default:
                    switch (_hidl_code) {
                        case 256067662:
                            if ((_hidl_flags & 1) == 0) {
                                z = false;
                            }
                            if (z) {
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
                                z = false;
                            }
                            if (z) {
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
                                z = false;
                            }
                            if (z) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            groupIfName = interfaceDescriptor();
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeString(groupIfName);
                            _hidl_reply.send();
                            return;
                        case 256398152:
                            if ((_hidl_flags & 1) == 0) {
                                z = false;
                            }
                            if (z) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            hwParcel.enforceInterface(IBase.kInterfaceName);
                            ArrayList<byte[]> _hidl_out_hashchain = getHashChain();
                            hwParcel2.writeStatus(0);
                            HwBlob _hidl_blob = new HwBlob(16);
                            int _hidl_vec_size = _hidl_out_hashchain.size();
                            _hidl_blob.putInt32(8, _hidl_vec_size);
                            _hidl_blob.putBool(12, false);
                            HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
                            while (true) {
                                int _hidl_index_02;
                                persistentNetworkId = _hidl_index_02;
                                if (persistentNetworkId < _hidl_vec_size) {
                                    childBlob.putInt8Array((long) (persistentNetworkId * 32), (byte[]) _hidl_out_hashchain.get(persistentNetworkId));
                                    _hidl_index_02 = persistentNetworkId + 1;
                                } else {
                                    _hidl_blob.putBlob(0, childBlob);
                                    hwParcel2.writeBuffer(_hidl_blob);
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
                                z = false;
                            }
                            if (z) {
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
                                z = false;
                            }
                            if (z) {
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

    SupplicantStatus addGroupWithFreq(boolean z, int i, String str) throws RemoteException;

    IHwBinder asBinder();

    DebugInfo getDebugInfo() throws RemoteException;

    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getP2pLinkspeed(String str, getP2pLinkspeedCallback getp2plinkspeedcallback) throws RemoteException;

    SupplicantStatus hwP2pRegisterCallback(ISupplicantP2pIfaceCallback iSupplicantP2pIfaceCallback) throws RemoteException;

    ArrayList<String> interfaceChain() throws RemoteException;

    String interfaceDescriptor() throws RemoteException;

    boolean linkToDeath(DeathRecipient deathRecipient, long j) throws RemoteException;

    SupplicantStatus magiclinkConnect(String str) throws RemoteException;

    void notifySyspropsChanged() throws RemoteException;

    void ping() throws RemoteException;

    SupplicantStatus rptP2pAddGroup(String str) throws RemoteException;

    void setHALInstrumentation() throws RemoteException;

    boolean unlinkToDeath(DeathRecipient deathRecipient) throws RemoteException;

    static ISupplicantP2pIface asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof ISupplicantP2pIface)) {
            return (ISupplicantP2pIface) iface;
        }
        ISupplicantP2pIface proxy = new Proxy(binder);
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

    static ISupplicantP2pIface castFrom(IHwInterface iface) {
        return iface == null ? null : asInterface(iface.asBinder());
    }

    static ISupplicantP2pIface getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static ISupplicantP2pIface getService(boolean retry) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, retry);
    }

    static ISupplicantP2pIface getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static ISupplicantP2pIface getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }
}
