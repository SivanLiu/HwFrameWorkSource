package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class NanDiscoveryCommonConfig {
    public int configRangingIndications;
    public boolean disableDiscoveryTerminationIndication;
    public boolean disableFollowupReceivedIndication;
    public boolean disableMatchExpirationIndication;
    public byte discoveryCount;
    public int discoveryMatchIndicator;
    public short discoveryWindowPeriod;
    public short distanceEgressCm;
    public short distanceIngressCm;
    public final ArrayList<Byte> extendedServiceSpecificInfo = new ArrayList();
    public int rangingIntervalMsec;
    public boolean rangingRequired;
    public final ArrayList<Byte> rxMatchFilter = new ArrayList();
    public final NanDataPathSecurityConfig securityConfig = new NanDataPathSecurityConfig();
    public final ArrayList<Byte> serviceName = new ArrayList();
    public final ArrayList<Byte> serviceSpecificInfo = new ArrayList();
    public byte sessionId;
    public short ttlSec;
    public final ArrayList<Byte> txMatchFilter = new ArrayList();
    public boolean useRssiThreshold;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != NanDiscoveryCommonConfig.class) {
            return false;
        }
        NanDiscoveryCommonConfig other = (NanDiscoveryCommonConfig) otherObject;
        if (this.sessionId == other.sessionId && this.ttlSec == other.ttlSec && this.discoveryWindowPeriod == other.discoveryWindowPeriod && this.discoveryCount == other.discoveryCount && HidlSupport.deepEquals(this.serviceName, other.serviceName) && this.discoveryMatchIndicator == other.discoveryMatchIndicator && HidlSupport.deepEquals(this.serviceSpecificInfo, other.serviceSpecificInfo) && HidlSupport.deepEquals(this.extendedServiceSpecificInfo, other.extendedServiceSpecificInfo) && HidlSupport.deepEquals(this.rxMatchFilter, other.rxMatchFilter) && HidlSupport.deepEquals(this.txMatchFilter, other.txMatchFilter) && this.useRssiThreshold == other.useRssiThreshold && this.disableDiscoveryTerminationIndication == other.disableDiscoveryTerminationIndication && this.disableMatchExpirationIndication == other.disableMatchExpirationIndication && this.disableFollowupReceivedIndication == other.disableFollowupReceivedIndication && HidlSupport.deepEquals(this.securityConfig, other.securityConfig) && this.rangingRequired == other.rangingRequired && this.rangingIntervalMsec == other.rangingIntervalMsec && HidlSupport.deepEquals(Integer.valueOf(this.configRangingIndications), Integer.valueOf(other.configRangingIndications)) && this.distanceIngressCm == other.distanceIngressCm && this.distanceEgressCm == other.distanceEgressCm) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.sessionId))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.ttlSec))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.discoveryWindowPeriod))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.discoveryCount))), Integer.valueOf(HidlSupport.deepHashCode(this.serviceName)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.discoveryMatchIndicator))), Integer.valueOf(HidlSupport.deepHashCode(this.serviceSpecificInfo)), Integer.valueOf(HidlSupport.deepHashCode(this.extendedServiceSpecificInfo)), Integer.valueOf(HidlSupport.deepHashCode(this.rxMatchFilter)), Integer.valueOf(HidlSupport.deepHashCode(this.txMatchFilter)), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.useRssiThreshold))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.disableDiscoveryTerminationIndication))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.disableMatchExpirationIndication))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.disableFollowupReceivedIndication))), Integer.valueOf(HidlSupport.deepHashCode(this.securityConfig)), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.rangingRequired))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.rangingIntervalMsec))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.configRangingIndications))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.distanceIngressCm))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.distanceEgressCm)))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".sessionId = ");
        builder.append(this.sessionId);
        builder.append(", .ttlSec = ");
        builder.append(this.ttlSec);
        builder.append(", .discoveryWindowPeriod = ");
        builder.append(this.discoveryWindowPeriod);
        builder.append(", .discoveryCount = ");
        builder.append(this.discoveryCount);
        builder.append(", .serviceName = ");
        builder.append(this.serviceName);
        builder.append(", .discoveryMatchIndicator = ");
        builder.append(NanMatchAlg.toString(this.discoveryMatchIndicator));
        builder.append(", .serviceSpecificInfo = ");
        builder.append(this.serviceSpecificInfo);
        builder.append(", .extendedServiceSpecificInfo = ");
        builder.append(this.extendedServiceSpecificInfo);
        builder.append(", .rxMatchFilter = ");
        builder.append(this.rxMatchFilter);
        builder.append(", .txMatchFilter = ");
        builder.append(this.txMatchFilter);
        builder.append(", .useRssiThreshold = ");
        builder.append(this.useRssiThreshold);
        builder.append(", .disableDiscoveryTerminationIndication = ");
        builder.append(this.disableDiscoveryTerminationIndication);
        builder.append(", .disableMatchExpirationIndication = ");
        builder.append(this.disableMatchExpirationIndication);
        builder.append(", .disableFollowupReceivedIndication = ");
        builder.append(this.disableFollowupReceivedIndication);
        builder.append(", .securityConfig = ");
        builder.append(this.securityConfig);
        builder.append(", .rangingRequired = ");
        builder.append(this.rangingRequired);
        builder.append(", .rangingIntervalMsec = ");
        builder.append(this.rangingIntervalMsec);
        builder.append(", .configRangingIndications = ");
        builder.append(NanRangingIndication.dumpBitfield(this.configRangingIndications));
        builder.append(", .distanceIngressCm = ");
        builder.append(this.distanceIngressCm);
        builder.append(", .distanceEgressCm = ");
        builder.append(this.distanceEgressCm);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(176), 0);
    }

    public static final ArrayList<NanDiscoveryCommonConfig> readVectorFromParcel(HwParcel parcel) {
        ArrayList<NanDiscoveryCommonConfig> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 176), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            NanDiscoveryCommonConfig _hidl_vec_element = new NanDiscoveryCommonConfig();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 176));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        int _hidl_index_02;
        HwBlob hwBlob = _hidl_blob;
        this.sessionId = hwBlob.getInt8(_hidl_offset + 0);
        this.ttlSec = hwBlob.getInt16(_hidl_offset + 2);
        this.discoveryWindowPeriod = hwBlob.getInt16(_hidl_offset + 4);
        this.discoveryCount = hwBlob.getInt8(_hidl_offset + 6);
        int _hidl_vec_size = hwBlob.getInt32((_hidl_offset + 8) + 8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 1), _hidl_blob.handle(), (_hidl_offset + 8) + 0, true);
        this.serviceName.clear();
        int _hidl_index_03 = 0;
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.serviceName.add(Byte.valueOf(childBlob.getInt8((long) (_hidl_index_0 * 1))));
        }
        this.discoveryMatchIndicator = hwBlob.getInt32(_hidl_offset + 24);
        _hidl_index_0 = hwBlob.getInt32((_hidl_offset + 32) + 8);
        HwBlob childBlob2 = parcel.readEmbeddedBuffer((long) (_hidl_index_0 * 1), _hidl_blob.handle(), (_hidl_offset + 32) + 0, true);
        this.serviceSpecificInfo.clear();
        for (_hidl_index_02 = 0; _hidl_index_02 < _hidl_index_0; _hidl_index_02++) {
            this.serviceSpecificInfo.add(Byte.valueOf(childBlob2.getInt8((long) (_hidl_index_02 * 1))));
        }
        _hidl_index_0 = hwBlob.getInt32((_hidl_offset + 48) + 8);
        childBlob2 = parcel.readEmbeddedBuffer((long) (_hidl_index_0 * 1), _hidl_blob.handle(), (_hidl_offset + 48) + 0, true);
        this.extendedServiceSpecificInfo.clear();
        for (_hidl_index_02 = 0; _hidl_index_02 < _hidl_index_0; _hidl_index_02++) {
            this.extendedServiceSpecificInfo.add(Byte.valueOf(childBlob2.getInt8((long) (_hidl_index_02 * 1))));
        }
        _hidl_index_0 = hwBlob.getInt32((_hidl_offset + 64) + 8);
        childBlob2 = parcel.readEmbeddedBuffer((long) (_hidl_index_0 * 1), _hidl_blob.handle(), (_hidl_offset + 64) + 0, true);
        this.rxMatchFilter.clear();
        for (_hidl_index_02 = 0; _hidl_index_02 < _hidl_index_0; _hidl_index_02++) {
            this.rxMatchFilter.add(Byte.valueOf(childBlob2.getInt8((long) (_hidl_index_02 * 1))));
        }
        int _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 80) + 8);
        HwBlob childBlob3 = parcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 1), _hidl_blob.handle(), (_hidl_offset + 80) + 0, true);
        this.txMatchFilter.clear();
        while (true) {
            int _hidl_index_04 = _hidl_index_03;
            if (_hidl_index_04 < _hidl_vec_size2) {
                this.txMatchFilter.add(Byte.valueOf(childBlob3.getInt8((long) (_hidl_index_04 * 1))));
                _hidl_index_03 = _hidl_index_04 + 1;
            } else {
                this.useRssiThreshold = hwBlob.getBool(_hidl_offset + 96);
                this.disableDiscoveryTerminationIndication = hwBlob.getBool(_hidl_offset + 97);
                this.disableMatchExpirationIndication = hwBlob.getBool(_hidl_offset + 98);
                this.disableFollowupReceivedIndication = hwBlob.getBool(_hidl_offset + 99);
                this.securityConfig.readEmbeddedFromParcel(parcel, hwBlob, _hidl_offset + 104);
                this.rangingRequired = hwBlob.getBool(_hidl_offset + 160);
                this.rangingIntervalMsec = hwBlob.getInt32(_hidl_offset + 164);
                this.configRangingIndications = hwBlob.getInt32(_hidl_offset + 168);
                this.distanceIngressCm = hwBlob.getInt16(_hidl_offset + 172);
                this.distanceEgressCm = hwBlob.getInt16(_hidl_offset + 174);
                return;
            }
        }
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(176);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<NanDiscoveryCommonConfig> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        int _hidl_index_0 = 0;
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 176);
        while (_hidl_index_0 < _hidl_vec_size) {
            ((NanDiscoveryCommonConfig) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 176));
            _hidl_index_0++;
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        HwBlob hwBlob = _hidl_blob;
        hwBlob.putInt8(_hidl_offset + 0, this.sessionId);
        hwBlob.putInt16(_hidl_offset + 2, this.ttlSec);
        hwBlob.putInt16(_hidl_offset + 4, this.discoveryWindowPeriod);
        hwBlob.putInt8(_hidl_offset + 6, this.discoveryCount);
        int _hidl_vec_size = this.serviceName.size();
        hwBlob.putInt32((_hidl_offset + 8) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 8) + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 1);
        for (int _hidl_index_02 = 0; _hidl_index_02 < _hidl_vec_size; _hidl_index_02++) {
            childBlob.putInt8((long) (_hidl_index_02 * 1), ((Byte) this.serviceName.get(_hidl_index_02)).byteValue());
        }
        hwBlob.putBlob((_hidl_offset + 8) + 0, childBlob);
        hwBlob.putInt32(_hidl_offset + 24, this.discoveryMatchIndicator);
        _hidl_vec_size = this.serviceSpecificInfo.size();
        hwBlob.putInt32((_hidl_offset + 32) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 32) + 12, false);
        HwBlob childBlob2 = new HwBlob(_hidl_vec_size * 1);
        for (int _hidl_index_03 = 0; _hidl_index_03 < _hidl_vec_size; _hidl_index_03++) {
            childBlob2.putInt8((long) (_hidl_index_03 * 1), ((Byte) this.serviceSpecificInfo.get(_hidl_index_03)).byteValue());
        }
        hwBlob.putBlob((_hidl_offset + 32) + 0, childBlob2);
        _hidl_vec_size = this.extendedServiceSpecificInfo.size();
        hwBlob.putInt32((_hidl_offset + 48) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 48) + 12, false);
        HwBlob childBlob3 = new HwBlob(_hidl_vec_size * 1);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            childBlob3.putInt8((long) (_hidl_index_0 * 1), ((Byte) this.extendedServiceSpecificInfo.get(_hidl_index_0)).byteValue());
        }
        hwBlob.putBlob((_hidl_offset + 48) + 0, childBlob3);
        _hidl_vec_size = this.rxMatchFilter.size();
        hwBlob.putInt32((_hidl_offset + 64) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 64) + 12, false);
        childBlob3 = new HwBlob(_hidl_vec_size * 1);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            childBlob3.putInt8((long) (_hidl_index_0 * 1), ((Byte) this.rxMatchFilter.get(_hidl_index_0)).byteValue());
        }
        hwBlob.putBlob((_hidl_offset + 64) + 0, childBlob3);
        _hidl_vec_size = this.txMatchFilter.size();
        hwBlob.putInt32((_hidl_offset + 80) + 8, _hidl_vec_size);
        int _hidl_index_04 = 0;
        hwBlob.putBool((_hidl_offset + 80) + 12, false);
        HwBlob childBlob4 = new HwBlob(_hidl_vec_size * 1);
        while (true) {
            int _hidl_index_05 = _hidl_index_04;
            if (_hidl_index_05 < _hidl_vec_size) {
                childBlob4.putInt8((long) (_hidl_index_05 * 1), ((Byte) this.txMatchFilter.get(_hidl_index_05)).byteValue());
                _hidl_index_04 = _hidl_index_05 + 1;
            } else {
                hwBlob.putBlob((_hidl_offset + 80) + 0, childBlob4);
                hwBlob.putBool(_hidl_offset + 96, this.useRssiThreshold);
                hwBlob.putBool(_hidl_offset + 97, this.disableDiscoveryTerminationIndication);
                hwBlob.putBool(_hidl_offset + 98, this.disableMatchExpirationIndication);
                hwBlob.putBool(_hidl_offset + 99, this.disableFollowupReceivedIndication);
                this.securityConfig.writeEmbeddedToBlob(hwBlob, _hidl_offset + 104);
                hwBlob.putBool(_hidl_offset + 160, this.rangingRequired);
                hwBlob.putInt32(_hidl_offset + 164, this.rangingIntervalMsec);
                hwBlob.putInt32(_hidl_offset + 168, this.configRangingIndications);
                hwBlob.putInt16(_hidl_offset + 172, this.distanceIngressCm);
                hwBlob.putInt16(_hidl_offset + 174, this.distanceEgressCm);
                return;
            }
        }
    }
}
