package vendor.huawei.hardware.eid.V1_1;

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
import vendor.huawei.hardware.eid.V1_0.CERTIFICATE_REQUEST_MESSAGE_INPUT_INFO_S;
import vendor.huawei.hardware.eid.V1_0.CERTIFICATE_REQUEST_MESSAGE_S;
import vendor.huawei.hardware.eid.V1_0.ENCRYPTION_FACTOR_S;
import vendor.huawei.hardware.eid.V1_0.FACE_CHANGE_INPUT_INFO_S;
import vendor.huawei.hardware.eid.V1_0.FACE_CHANGE_OUTPUT_INFO_S;
import vendor.huawei.hardware.eid.V1_0.IDENTITY_INFORMATION_INPUT_INFO_S;
import vendor.huawei.hardware.eid.V1_0.IDENTITY_INFORMATION_S;
import vendor.huawei.hardware.eid.V1_0.IEid.HWEidGetCertificateRequestMessageCallback;
import vendor.huawei.hardware.eid.V1_0.IEid.HWEidGetFaceIsChangedCallback;
import vendor.huawei.hardware.eid.V1_0.IEid.HWEidGetIdentityInformationCallback;
import vendor.huawei.hardware.eid.V1_0.IEid.HWEidGetImageCallback;
import vendor.huawei.hardware.eid.V1_0.IEid.HWEidGetInfoSignCallback;
import vendor.huawei.hardware.eid.V1_0.IEid.HWEidGetUnsecImageCallback;
import vendor.huawei.hardware.eid.V1_0.IMAGE_CONTAINER_S;
import vendor.huawei.hardware.eid.V1_0.INFO_SIGN_INPUT_INFO_S;
import vendor.huawei.hardware.eid.V1_0.INFO_SIGN_OUTPUT_INFO_S;
import vendor.huawei.hardware.eid.V1_0.INIT_TA_MSG_S;
import vendor.huawei.hardware.eid.V1_0.SEC_IMAGE_S;

public interface IEid extends vendor.huawei.hardware.eid.V1_0.IEid {
    public static final String kInterfaceName = "vendor.huawei.hardware.eid@1.1::IEid";

    @FunctionalInterface
    public interface HWEidGetSecImageZipCallback {
        void onValues(int i, SEC_IMAGE_ZIP_S sec_image_zip_s);
    }

    @FunctionalInterface
    public interface HWEidGetUnsecImageZipCallback {
        void onValues(int i, SEC_IMAGE_ZIP_S sec_image_zip_s);
    }

    @FunctionalInterface
    public interface HWEidGetVersionCallback {
        void onValues(int i, HIDL_VERSION_S hidl_version_s);
    }

    public static final class Proxy implements IEid {
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
                return "[class or subclass of vendor.huawei.hardware.eid@1.1::IEid]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        public int HWEidInitTa(INIT_TA_MSG_S init_msg) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
            init_msg.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        public int HWEidFiniTa() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        public void HWEidGetImage(ENCRYPTION_FACTOR_S factor, HWEidGetImageCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
            factor.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                SEC_IMAGE_S _hidl_out_secImage = new SEC_IMAGE_S();
                _hidl_out_secImage.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_ret, _hidl_out_secImage);
            } finally {
                _hidl_reply.release();
            }
        }

        public void HWEidGetUnsecImage(IMAGE_CONTAINER_S container, ENCRYPTION_FACTOR_S factor, HWEidGetUnsecImageCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
            container.writeToParcel(_hidl_request);
            factor.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                SEC_IMAGE_S _hidl_out_secImage = new SEC_IMAGE_S();
                _hidl_out_secImage.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_ret, _hidl_out_secImage);
            } finally {
                _hidl_reply.release();
            }
        }

        public void HWEidGetCertificateRequestMessage(CERTIFICATE_REQUEST_MESSAGE_INPUT_INFO_S input, HWEidGetCertificateRequestMessageCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
            input.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                CERTIFICATE_REQUEST_MESSAGE_S _hidl_out_certReqMsg = new CERTIFICATE_REQUEST_MESSAGE_S();
                _hidl_out_certReqMsg.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_ret, _hidl_out_certReqMsg);
            } finally {
                _hidl_reply.release();
            }
        }

        public void HWEidGetInfoSign(INFO_SIGN_INPUT_INFO_S input, HWEidGetInfoSignCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
            input.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                INFO_SIGN_OUTPUT_INFO_S _hidl_out_output = new INFO_SIGN_OUTPUT_INFO_S();
                _hidl_out_output.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_ret, _hidl_out_output);
            } finally {
                _hidl_reply.release();
            }
        }

        public void HWEidGetIdentityInformation(IDENTITY_INFORMATION_INPUT_INFO_S input, HWEidGetIdentityInformationCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
            input.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                IDENTITY_INFORMATION_S _hidl_out_idInfo = new IDENTITY_INFORMATION_S();
                _hidl_out_idInfo.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_ret, _hidl_out_idInfo);
            } finally {
                _hidl_reply.release();
            }
        }

        public void HWEidGetFaceIsChanged(FACE_CHANGE_INPUT_INFO_S input, HWEidGetFaceIsChangedCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
            input.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                FACE_CHANGE_OUTPUT_INFO_S _hidl_out_output = new FACE_CHANGE_OUTPUT_INFO_S();
                _hidl_out_output.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_ret, _hidl_out_output);
            } finally {
                _hidl_reply.release();
            }
        }

        public int HWCtidGetImage() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IEid.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        public int HWCtidSetSecMode() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IEid.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        public int HWCtidInitTa(INIT_CTID_TA_MSG_S init_msg) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IEid.kInterfaceName);
            init_msg.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        public void HWEidGetSecImageZip(IMAGE_ZIP_CONTAINER_S container, CUT_COORDINATE_S coordinate, ENCRYPTION_FACTOR_S factor, HWEidGetSecImageZipCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IEid.kInterfaceName);
            container.writeToParcel(_hidl_request);
            coordinate.writeToParcel(_hidl_request);
            factor.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                SEC_IMAGE_ZIP_S _hidl_out_secImage = new SEC_IMAGE_ZIP_S();
                _hidl_out_secImage.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_ret, _hidl_out_secImage);
            } finally {
                _hidl_reply.release();
            }
        }

        public void HWEidGetUnsecImageZip(IMAGE_ZIP_CONTAINER_S container, ENCRYPTION_FACTOR_S factor, HWEidGetUnsecImageZipCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IEid.kInterfaceName);
            container.writeToParcel(_hidl_request);
            factor.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                SEC_IMAGE_ZIP_S _hidl_out_secImage = new SEC_IMAGE_ZIP_S();
                _hidl_out_secImage.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_ret, _hidl_out_secImage);
            } finally {
                _hidl_reply.release();
            }
        }

        public void HWEidGetVersion(HWEidGetVersionCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IEid.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(14, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                HIDL_VERSION_S _hidl_out_version = new HIDL_VERSION_S();
                _hidl_out_version.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_ret, _hidl_out_version);
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

    public static abstract class Stub extends HwBinder implements IEid {
        public IHwBinder asBinder() {
            return this;
        }

        public final ArrayList<String> interfaceChain() {
            return new ArrayList(Arrays.asList(new String[]{IEid.kInterfaceName, vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName, IBase.kInterfaceName}));
        }

        public final String interfaceDescriptor() {
            return IEid.kInterfaceName;
        }

        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList(Arrays.asList(new byte[][]{new byte[]{(byte) -33, (byte) 86, (byte) -60, (byte) 63, (byte) 76, (byte) 2, (byte) 44, (byte) -2, (byte) -3, (byte) -93, (byte) -85, (byte) -96, (byte) -35, (byte) -83, (byte) -82, (byte) -123, (byte) 106, (byte) -2, CurrentPacket.INPUT_MASK, (byte) 93, (byte) -55, (byte) 30, (byte) -29, (byte) -65, (byte) -116, (byte) 104, (byte) 89, (byte) 37, (byte) -75, (byte) -116, (byte) 39, (byte) -118}, new byte[]{(byte) -9, (byte) 116, (byte) 21, (byte) 40, (byte) -67, (byte) -90, (byte) 68, (byte) 123, (byte) 54, (byte) -54, (byte) -100, (byte) -25, (byte) 87, (byte) 47, (byte) -76, (byte) -32, (byte) 40, (byte) 116, (byte) -86, (byte) 99, (byte) 114, (byte) -86, (byte) 27, (byte) -123, (byte) 11, (byte) 63, (byte) -9, (byte) 27, (byte) -89, (byte) 34, (byte) 105, (byte) -105}, new byte[]{(byte) -67, (byte) -38, (byte) -74, (byte) 24, (byte) 77, (byte) 122, (byte) 52, (byte) 109, (byte) -90, (byte) -96, (byte) 125, (byte) -64, (byte) -126, (byte) -116, (byte) -15, (byte) -102, (byte) 105, (byte) 111, (byte) 76, (byte) -86, (byte) 54, (byte) 17, (byte) -59, (byte) 31, (byte) 46, (byte) 20, (byte) 86, (byte) 90, (byte) 20, (byte) -76, CurrentPacket.INPUT_MASK, (byte) -39}}));
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
            if (IEid.kInterfaceName.equals(descriptor)) {
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
            int _hidl_out_ret;
            int _hidl_out_ret2;
            ENCRYPTION_FACTOR_S factor;
            IMAGE_ZIP_CONTAINER_S container;
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
                    _hidl_request.enforceInterface(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
                    INIT_TA_MSG_S init_msg = new INIT_TA_MSG_S();
                    init_msg.readFromParcel(_hidl_request);
                    _hidl_out_ret = HWEidInitTa(init_msg);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_ret);
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
                    _hidl_request.enforceInterface(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
                    _hidl_out_ret2 = HWEidFiniTa();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_ret2);
                    _hidl_reply.send();
                    return;
                case 3:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
                    ENCRYPTION_FACTOR_S factor2 = new ENCRYPTION_FACTOR_S();
                    factor2.readFromParcel(_hidl_request);
                    HWEidGetImage(factor2, new HWEidGetImageCallback() {
                        public void onValues(int ret, SEC_IMAGE_S secImage) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(ret);
                            secImage.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 4:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
                    IMAGE_CONTAINER_S container2 = new IMAGE_CONTAINER_S();
                    container2.readFromParcel(_hidl_request);
                    factor = new ENCRYPTION_FACTOR_S();
                    factor.readFromParcel(_hidl_request);
                    HWEidGetUnsecImage(container2, factor, new HWEidGetUnsecImageCallback() {
                        public void onValues(int ret, SEC_IMAGE_S secImage) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(ret);
                            secImage.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 5:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
                    CERTIFICATE_REQUEST_MESSAGE_INPUT_INFO_S input = new CERTIFICATE_REQUEST_MESSAGE_INPUT_INFO_S();
                    input.readFromParcel(_hidl_request);
                    HWEidGetCertificateRequestMessage(input, new HWEidGetCertificateRequestMessageCallback() {
                        public void onValues(int ret, CERTIFICATE_REQUEST_MESSAGE_S certReqMsg) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(ret);
                            certReqMsg.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 6:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
                    INFO_SIGN_INPUT_INFO_S input2 = new INFO_SIGN_INPUT_INFO_S();
                    input2.readFromParcel(_hidl_request);
                    HWEidGetInfoSign(input2, new HWEidGetInfoSignCallback() {
                        public void onValues(int ret, INFO_SIGN_OUTPUT_INFO_S output) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(ret);
                            output.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
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
                    _hidl_request.enforceInterface(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
                    IDENTITY_INFORMATION_INPUT_INFO_S input3 = new IDENTITY_INFORMATION_INPUT_INFO_S();
                    input3.readFromParcel(_hidl_request);
                    HWEidGetIdentityInformation(input3, new HWEidGetIdentityInformationCallback() {
                        public void onValues(int ret, IDENTITY_INFORMATION_S idInfo) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(ret);
                            idInfo.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 8:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(vendor.huawei.hardware.eid.V1_0.IEid.kInterfaceName);
                    FACE_CHANGE_INPUT_INFO_S input4 = new FACE_CHANGE_INPUT_INFO_S();
                    input4.readFromParcel(_hidl_request);
                    HWEidGetFaceIsChanged(input4, new HWEidGetFaceIsChangedCallback() {
                        public void onValues(int ret, FACE_CHANGE_OUTPUT_INFO_S output) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(ret);
                            output.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 9:
                    if ((_hidl_flags & 1) == 0) {
                        _hidl_is_oneway2 = false;
                    }
                    if (_hidl_is_oneway2) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IEid.kInterfaceName);
                    _hidl_out_ret2 = HWCtidGetImage();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_ret2);
                    _hidl_reply.send();
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
                    _hidl_request.enforceInterface(IEid.kInterfaceName);
                    _hidl_out_ret2 = HWCtidSetSecMode();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_ret2);
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
                    _hidl_request.enforceInterface(IEid.kInterfaceName);
                    INIT_CTID_TA_MSG_S init_msg2 = new INIT_CTID_TA_MSG_S();
                    init_msg2.readFromParcel(_hidl_request);
                    _hidl_out_ret = HWCtidInitTa(init_msg2);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    return;
                case 12:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IEid.kInterfaceName);
                    container = new IMAGE_ZIP_CONTAINER_S();
                    container.readFromParcel(_hidl_request);
                    CUT_COORDINATE_S coordinate = new CUT_COORDINATE_S();
                    coordinate.readFromParcel(_hidl_request);
                    ENCRYPTION_FACTOR_S factor3 = new ENCRYPTION_FACTOR_S();
                    factor3.readFromParcel(_hidl_request);
                    HWEidGetSecImageZip(container, coordinate, factor3, new HWEidGetSecImageZipCallback() {
                        public void onValues(int ret, SEC_IMAGE_ZIP_S secImage) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(ret);
                            secImage.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 13:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IEid.kInterfaceName);
                    container = new IMAGE_ZIP_CONTAINER_S();
                    container.readFromParcel(_hidl_request);
                    factor = new ENCRYPTION_FACTOR_S();
                    factor.readFromParcel(_hidl_request);
                    HWEidGetUnsecImageZip(container, factor, new HWEidGetUnsecImageZipCallback() {
                        public void onValues(int ret, SEC_IMAGE_ZIP_S secImage) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(ret);
                            secImage.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 14:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IEid.kInterfaceName);
                    HWEidGetVersion(new HWEidGetVersionCallback() {
                        public void onValues(int ret, HIDL_VERSION_S version) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(ret);
                            version.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
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

    int HWCtidGetImage() throws RemoteException;

    int HWCtidInitTa(INIT_CTID_TA_MSG_S init_ctid_ta_msg_s) throws RemoteException;

    int HWCtidSetSecMode() throws RemoteException;

    void HWEidGetSecImageZip(IMAGE_ZIP_CONTAINER_S image_zip_container_s, CUT_COORDINATE_S cut_coordinate_s, ENCRYPTION_FACTOR_S encryption_factor_s, HWEidGetSecImageZipCallback hWEidGetSecImageZipCallback) throws RemoteException;

    void HWEidGetUnsecImageZip(IMAGE_ZIP_CONTAINER_S image_zip_container_s, ENCRYPTION_FACTOR_S encryption_factor_s, HWEidGetUnsecImageZipCallback hWEidGetUnsecImageZipCallback) throws RemoteException;

    void HWEidGetVersion(HWEidGetVersionCallback hWEidGetVersionCallback) throws RemoteException;

    IHwBinder asBinder();

    DebugInfo getDebugInfo() throws RemoteException;

    ArrayList<byte[]> getHashChain() throws RemoteException;

    ArrayList<String> interfaceChain() throws RemoteException;

    String interfaceDescriptor() throws RemoteException;

    boolean linkToDeath(DeathRecipient deathRecipient, long j) throws RemoteException;

    void notifySyspropsChanged() throws RemoteException;

    void ping() throws RemoteException;

    void setHALInstrumentation() throws RemoteException;

    boolean unlinkToDeath(DeathRecipient deathRecipient) throws RemoteException;

    static IEid asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IEid)) {
            return (IEid) iface;
        }
        IEid proxy = new Proxy(binder);
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

    static IEid castFrom(IHwInterface iface) {
        return iface == null ? null : asInterface(iface.asBinder());
    }

    static IEid getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IEid getService(boolean retry) throws RemoteException {
        return getService(MemoryConstant.MEM_SCENE_DEFAULT, retry);
    }

    static IEid getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static IEid getService() throws RemoteException {
        return getService(MemoryConstant.MEM_SCENE_DEFAULT);
    }
}
