package android.hardware.wifi.V1_2;

import android.hardware.wifi.V1_0.IWifiIface;
import android.hardware.wifi.V1_0.IWifiIface.getNameCallback;
import android.hardware.wifi.V1_0.IWifiIface.getTypeCallback;
import android.hardware.wifi.V1_0.IWifiStaIface.getApfPacketFilterCapabilitiesCallback;
import android.hardware.wifi.V1_0.IWifiStaIface.getBackgroundScanCapabilitiesCallback;
import android.hardware.wifi.V1_0.IWifiStaIface.getCapabilitiesCallback;
import android.hardware.wifi.V1_0.IWifiStaIface.getDebugRxPacketFatesCallback;
import android.hardware.wifi.V1_0.IWifiStaIface.getDebugTxPacketFatesCallback;
import android.hardware.wifi.V1_0.IWifiStaIface.getLinkLayerStatsCallback;
import android.hardware.wifi.V1_0.IWifiStaIface.getRoamingCapabilitiesCallback;
import android.hardware.wifi.V1_0.IWifiStaIface.getValidFrequenciesForBandCallback;
import android.hardware.wifi.V1_0.IWifiStaIfaceEventCallback;
import android.hardware.wifi.V1_0.StaApfPacketFilterCapabilities;
import android.hardware.wifi.V1_0.StaBackgroundScanCapabilities;
import android.hardware.wifi.V1_0.StaBackgroundScanParameters;
import android.hardware.wifi.V1_0.StaLinkLayerStats;
import android.hardware.wifi.V1_0.StaRoamingCapabilities;
import android.hardware.wifi.V1_0.StaRoamingConfig;
import android.hardware.wifi.V1_0.WifiDebugRxPacketFateReport;
import android.hardware.wifi.V1_0.WifiDebugTxPacketFateReport;
import android.hardware.wifi.V1_0.WifiStatus;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.ReasonCode;
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
import com.android.server.wifi.WifiLoggerHal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface IWifiStaIface extends android.hardware.wifi.V1_0.IWifiStaIface {
    public static final String kInterfaceName = "android.hardware.wifi@1.2::IWifiStaIface";

    @FunctionalInterface
    public interface readApfPacketFilterDataCallback {
        void onValues(WifiStatus wifiStatus, ArrayList<Byte> arrayList);
    }

    public static final class Proxy implements IWifiStaIface {
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
                return "[class or subclass of android.hardware.wifi@1.2::IWifiStaIface]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        public void getType(getTypeCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getName(getNameCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus registerEventCallback(IWifiStaIfaceEventCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
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

        public void getCapabilities(getCapabilitiesCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getApfPacketFilterCapabilities(getApfPacketFilterCapabilitiesCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                StaApfPacketFilterCapabilities _hidl_out_capabilities = new StaApfPacketFilterCapabilities();
                _hidl_out_capabilities.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_capabilities);
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus installApfPacketFilter(int cmdId, ArrayList<Byte> program) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            _hidl_request.writeInt32(cmdId);
            _hidl_request.writeInt8Vector((ArrayList) program);
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

        public void getBackgroundScanCapabilities(getBackgroundScanCapabilitiesCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                StaBackgroundScanCapabilities _hidl_out_capabilities = new StaBackgroundScanCapabilities();
                _hidl_out_capabilities.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_capabilities);
            } finally {
                _hidl_reply.release();
            }
        }

        public void getValidFrequenciesForBand(int band, getValidFrequenciesForBandCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            _hidl_request.writeInt32(band);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt32Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus startBackgroundScan(int cmdId, StaBackgroundScanParameters params) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            _hidl_request.writeInt32(cmdId);
            params.writeToParcel(_hidl_request);
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

        public WifiStatus stopBackgroundScan(int cmdId) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
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

        public WifiStatus enableLinkLayerStatsCollection(boolean debug) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            _hidl_request.writeBool(debug);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus disableLinkLayerStatsCollection() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getLinkLayerStats(getLinkLayerStatsCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                StaLinkLayerStats _hidl_out_stats = new StaLinkLayerStats();
                _hidl_out_stats.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_stats);
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus startRssiMonitoring(int cmdId, int maxRssi, int minRssi) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            _hidl_request.writeInt32(cmdId);
            _hidl_request.writeInt32(maxRssi);
            _hidl_request.writeInt32(minRssi);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(14, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus stopRssiMonitoring(int cmdId) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            _hidl_request.writeInt32(cmdId);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(15, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getRoamingCapabilities(getRoamingCapabilitiesCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(16, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                StaRoamingCapabilities _hidl_out_caps = new StaRoamingCapabilities();
                _hidl_out_caps.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_out_caps);
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus configureRoaming(StaRoamingConfig config) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            config.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(17, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus setRoamingState(byte state) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            _hidl_request.writeInt8(state);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(18, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus enableNdOffload(boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(19, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus startSendingKeepAlivePackets(int cmdId, ArrayList<Byte> ipPacketData, short etherType, byte[] srcAddress, byte[] dstAddress, int periodInMs) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            _hidl_request.writeInt32(cmdId);
            _hidl_request.writeInt8Vector((ArrayList) ipPacketData);
            _hidl_request.writeInt16(etherType);
            HwBlob _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, srcAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, dstAddress);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeInt32(periodInMs);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(20, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus stopSendingKeepAlivePackets(int cmdId) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            _hidl_request.writeInt32(cmdId);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(21, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus setScanningMacOui(byte[] oui) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(3);
            _hidl_blob.putInt8Array(0, oui);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(22, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus startDebugPacketFateMonitoring() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(23, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        public void getDebugTxPacketFates(getDebugTxPacketFatesCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(24, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, WifiDebugTxPacketFateReport.readVectorFromParcel(_hidl_reply));
            } finally {
                _hidl_reply.release();
            }
        }

        public void getDebugRxPacketFates(getDebugRxPacketFatesCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(25, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, WifiDebugRxPacketFateReport.readVectorFromParcel(_hidl_reply));
            } finally {
                _hidl_reply.release();
            }
        }

        public void readApfPacketFilterData(readApfPacketFilterDataCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(26, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                WifiStatus _hidl_out_status = new WifiStatus();
                _hidl_out_status.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_status, _hidl_reply.readInt8Vector());
            } finally {
                _hidl_reply.release();
            }
        }

        public WifiStatus setMacAddress(byte[] mac) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(6);
            _hidl_blob.putInt8Array(0, mac);
            _hidl_request.writeBuffer(_hidl_blob);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(27, _hidl_request, _hidl_reply, 0);
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

    public static abstract class Stub extends HwBinder implements IWifiStaIface {
        public IHwBinder asBinder() {
            return this;
        }

        public final ArrayList<String> interfaceChain() {
            return new ArrayList(Arrays.asList(new String[]{IWifiStaIface.kInterfaceName, android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName, IWifiIface.kInterfaceName, IBase.kInterfaceName}));
        }

        public final String interfaceDescriptor() {
            return IWifiStaIface.kInterfaceName;
        }

        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList(Arrays.asList(new byte[][]{new byte[]{(byte) -11, (byte) 104, (byte) 45, (byte) -65, (byte) 25, (byte) -9, (byte) 18, (byte) -66, (byte) -7, (byte) -52, (byte) 63, (byte) -86, (byte) 95, (byte) -29, (byte) -36, (byte) 103, (byte) 11, (byte) 111, (byte) -5, (byte) -53, (byte) 98, (byte) -95, (byte) 71, (byte) -95, (byte) -40, (byte) 107, (byte) -99, (byte) 67, (byte) 87, (byte) 76, (byte) -40, (byte) 63}, new byte[]{(byte) 59, Byte.MIN_VALUE, (byte) -109, (byte) -45, (byte) -98, (byte) -15, (byte) -31, (byte) 14, (byte) 67, (byte) -59, (byte) 83, (byte) -118, (byte) -5, (byte) -11, (byte) -1, (byte) 110, (byte) 57, (byte) -72, (byte) -40, (byte) 22, (byte) -114, (byte) -69, (byte) -31, (byte) -103, (byte) -115, (byte) -103, (byte) 62, (byte) -119, (byte) -30, (byte) 95, (byte) 20, (byte) -91}, new byte[]{(byte) 107, (byte) -102, (byte) -44, (byte) 58, (byte) 94, (byte) -5, (byte) -26, (byte) -54, (byte) 33, (byte) 79, (byte) 117, (byte) 30, (byte) 34, (byte) -50, (byte) 67, (byte) -49, (byte) 92, (byte) -44, (byte) -43, (byte) -43, (byte) -14, (byte) -53, (byte) -88, (byte) 15, (byte) 36, (byte) -52, (byte) -45, (byte) 117, (byte) 90, (byte) 114, WifiLoggerHal.WIFI_ALERT_REASON_MAX, (byte) 28}, new byte[]{(byte) -67, (byte) -38, (byte) -74, (byte) 24, (byte) 77, (byte) 122, (byte) 52, (byte) 109, (byte) -90, (byte) -96, (byte) 125, (byte) -64, (byte) -126, (byte) -116, (byte) -15, (byte) -102, (byte) 105, (byte) 111, (byte) 76, (byte) -86, (byte) 54, (byte) 17, (byte) -59, (byte) 31, (byte) 46, (byte) 20, (byte) 86, (byte) 90, (byte) 20, (byte) -76, (byte) 15, (byte) -39}}));
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
            if (IWifiStaIface.kInterfaceName.equals(descriptor)) {
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
            WifiStatus _hidl_out_status2;
            WifiStatus _hidl_out_status3;
            WifiStatus _hidl_out_status4;
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
                    hwParcel.enforceInterface(IWifiIface.kInterfaceName);
                    getType(new getTypeCallback() {
                        public void onValues(WifiStatus status, int type) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt32(type);
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
                    hwParcel.enforceInterface(IWifiIface.kInterfaceName);
                    getName(new getNameCallback() {
                        public void onValues(WifiStatus status, String name) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeString(name);
                            hwParcel2.send();
                        }
                    });
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
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    _hidl_out_status = registerEventCallback(IWifiStaIfaceEventCallback.asInterface(_hidl_request.readStrongBinder()));
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 4:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    getCapabilities(new getCapabilitiesCallback() {
                        public void onValues(WifiStatus status, int capabilities) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt32(capabilities);
                            hwParcel2.send();
                        }
                    });
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
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    getApfPacketFilterCapabilities(new getApfPacketFilterCapabilitiesCallback() {
                        public void onValues(WifiStatus status, StaApfPacketFilterCapabilities capabilities) {
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
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    _hidl_out_status2 = installApfPacketFilter(_hidl_request.readInt32(), _hidl_request.readInt8Vector());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 7:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    getBackgroundScanCapabilities(new getBackgroundScanCapabilitiesCallback() {
                        public void onValues(WifiStatus status, StaBackgroundScanCapabilities capabilities) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            capabilities.writeToParcel(hwParcel2);
                            hwParcel2.send();
                        }
                    });
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
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    getValidFrequenciesForBand(_hidl_request.readInt32(), new getValidFrequenciesForBandCallback() {
                        public void onValues(WifiStatus status, ArrayList<Integer> frequencies) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt32Vector((ArrayList) frequencies);
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
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    int cmdId = _hidl_request.readInt32();
                    StaBackgroundScanParameters params = new StaBackgroundScanParameters();
                    params.readFromParcel(hwParcel);
                    _hidl_out_status2 = startBackgroundScan(cmdId, params);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status2.writeToParcel(hwParcel2);
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
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    _hidl_out_status = stopBackgroundScan(_hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 11:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    _hidl_out_status = enableLinkLayerStatsCollection(_hidl_request.readBool());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 12:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    _hidl_out_status3 = disableLinkLayerStatsCollection();
                    hwParcel2.writeStatus(0);
                    _hidl_out_status3.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 13:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    getLinkLayerStats(new getLinkLayerStatsCallback() {
                        public void onValues(WifiStatus status, StaLinkLayerStats stats) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            stats.writeToParcel(hwParcel2);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 14:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    WifiStatus _hidl_out_status5 = startRssiMonitoring(_hidl_request.readInt32(), _hidl_request.readInt32(), _hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status5.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 15:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    _hidl_out_status = stopRssiMonitoring(_hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 16:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    getRoamingCapabilities(new getRoamingCapabilitiesCallback() {
                        public void onValues(WifiStatus status, StaRoamingCapabilities caps) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            caps.writeToParcel(hwParcel2);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 17:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    StaRoamingConfig config = new StaRoamingConfig();
                    config.readFromParcel(hwParcel);
                    _hidl_out_status = configureRoaming(config);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 18:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    _hidl_out_status = setRoamingState(_hidl_request.readInt8());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 19:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    _hidl_out_status = enableNdOffload(_hidl_request.readBool());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 20:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    int cmdId2 = _hidl_request.readInt32();
                    ArrayList<Byte> ipPacketData = _hidl_request.readInt8Vector();
                    short etherType = _hidl_request.readInt16();
                    byte[] srcAddress = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, srcAddress, 6);
                    byte[] bArr = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, bArr, 6);
                    byte[] dstAddress = bArr;
                    _hidl_out_status4 = startSendingKeepAlivePackets(cmdId2, ipPacketData, etherType, srcAddress, bArr, _hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status4.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case ReasonCode.UNSUPPORTED_RSN_IE_VERSION /*21*/:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    _hidl_out_status = stopSendingKeepAlivePackets(_hidl_request.readInt32());
                    hwParcel2.writeStatus(0);
                    _hidl_out_status.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 22:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    byte[] oui = new byte[3];
                    hwParcel.readBuffer(3).copyToInt8Array(0, oui, 3);
                    _hidl_out_status3 = setScanningMacOui(oui);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status3.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 23:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    _hidl_out_status3 = startDebugPacketFateMonitoring();
                    hwParcel2.writeStatus(0);
                    _hidl_out_status3.writeToParcel(hwParcel2);
                    _hidl_reply.send();
                    return;
                case 24:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    getDebugTxPacketFates(new getDebugTxPacketFatesCallback() {
                        public void onValues(WifiStatus status, ArrayList<WifiDebugTxPacketFateReport> fates) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            WifiDebugTxPacketFateReport.writeVectorToParcel(hwParcel2, fates);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 25:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiStaIface.kInterfaceName);
                    getDebugRxPacketFates(new getDebugRxPacketFatesCallback() {
                        public void onValues(WifiStatus status, ArrayList<WifiDebugRxPacketFateReport> fates) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            WifiDebugRxPacketFateReport.writeVectorToParcel(hwParcel2, fates);
                            hwParcel2.send();
                        }
                    });
                    return;
                case ReasonCode.TDLS_TEARDOWN_UNSPECIFIED /*26*/:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_index_0 = true;
                    }
                    if (_hidl_index_0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                    readApfPacketFilterData(new readApfPacketFilterDataCallback() {
                        public void onValues(WifiStatus status, ArrayList<Byte> data) {
                            hwParcel2.writeStatus(0);
                            status.writeToParcel(hwParcel2);
                            hwParcel2.writeInt8Vector((ArrayList) data);
                            hwParcel2.send();
                        }
                    });
                    return;
                case 27:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway = false;
                    }
                    if (_hidl_is_oneway) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                    byte[] mac = new byte[6];
                    hwParcel.readBuffer(6).copyToInt8Array(0, mac, 6);
                    _hidl_out_status4 = setMacAddress(mac);
                    hwParcel2.writeStatus(0);
                    _hidl_out_status4.writeToParcel(hwParcel2);
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
                            HwBlob _hidl_blob = new HwBlob(16);
                            int _hidl_vec_size = _hidl_out_hashchain.size();
                            _hidl_blob.putInt32(8, _hidl_vec_size);
                            _hidl_blob.putBool(12, false);
                            HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
                            while (true) {
                                int _hidl_index_02;
                                int _hidl_index_03 = _hidl_index_02;
                                if (_hidl_index_03 < _hidl_vec_size) {
                                    childBlob.putInt8Array((long) (_hidl_index_03 * 32), (byte[]) _hidl_out_hashchain.get(_hidl_index_03));
                                    _hidl_index_02 = _hidl_index_03 + 1;
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

    DebugInfo getDebugInfo() throws RemoteException;

    ArrayList<byte[]> getHashChain() throws RemoteException;

    ArrayList<String> interfaceChain() throws RemoteException;

    String interfaceDescriptor() throws RemoteException;

    boolean linkToDeath(DeathRecipient deathRecipient, long j) throws RemoteException;

    void notifySyspropsChanged() throws RemoteException;

    void ping() throws RemoteException;

    void readApfPacketFilterData(readApfPacketFilterDataCallback readapfpacketfilterdatacallback) throws RemoteException;

    void setHALInstrumentation() throws RemoteException;

    WifiStatus setMacAddress(byte[] bArr) throws RemoteException;

    boolean unlinkToDeath(DeathRecipient deathRecipient) throws RemoteException;

    static IWifiStaIface asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IWifiStaIface)) {
            return (IWifiStaIface) iface;
        }
        IWifiStaIface proxy = new Proxy(binder);
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

    static IWifiStaIface castFrom(IHwInterface iface) {
        return iface == null ? null : asInterface(iface.asBinder());
    }

    static IWifiStaIface getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IWifiStaIface getService(boolean retry) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, retry);
    }

    static IWifiStaIface getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static IWifiStaIface getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }
}
