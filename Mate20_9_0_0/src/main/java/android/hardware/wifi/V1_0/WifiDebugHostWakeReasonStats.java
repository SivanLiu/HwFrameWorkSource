package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class WifiDebugHostWakeReasonStats {
    public final ArrayList<Integer> cmdEventWakeCntPerType = new ArrayList();
    public final ArrayList<Integer> driverFwLocalWakeCntPerType = new ArrayList();
    public final WifiDebugHostWakeReasonRxIcmpPacketDetails rxIcmpPkWakeDetails = new WifiDebugHostWakeReasonRxIcmpPacketDetails();
    public final WifiDebugHostWakeReasonRxMulticastPacketDetails rxMulticastPkWakeDetails = new WifiDebugHostWakeReasonRxMulticastPacketDetails();
    public final WifiDebugHostWakeReasonRxPacketDetails rxPktWakeDetails = new WifiDebugHostWakeReasonRxPacketDetails();
    public int totalCmdEventWakeCnt;
    public int totalDriverFwLocalWakeCnt;
    public int totalRxPacketWakeCnt;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != WifiDebugHostWakeReasonStats.class) {
            return false;
        }
        WifiDebugHostWakeReasonStats other = (WifiDebugHostWakeReasonStats) otherObject;
        if (this.totalCmdEventWakeCnt == other.totalCmdEventWakeCnt && HidlSupport.deepEquals(this.cmdEventWakeCntPerType, other.cmdEventWakeCntPerType) && this.totalDriverFwLocalWakeCnt == other.totalDriverFwLocalWakeCnt && HidlSupport.deepEquals(this.driverFwLocalWakeCntPerType, other.driverFwLocalWakeCntPerType) && this.totalRxPacketWakeCnt == other.totalRxPacketWakeCnt && HidlSupport.deepEquals(this.rxPktWakeDetails, other.rxPktWakeDetails) && HidlSupport.deepEquals(this.rxMulticastPkWakeDetails, other.rxMulticastPkWakeDetails) && HidlSupport.deepEquals(this.rxIcmpPkWakeDetails, other.rxIcmpPkWakeDetails)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.totalCmdEventWakeCnt))), Integer.valueOf(HidlSupport.deepHashCode(this.cmdEventWakeCntPerType)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.totalDriverFwLocalWakeCnt))), Integer.valueOf(HidlSupport.deepHashCode(this.driverFwLocalWakeCntPerType)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.totalRxPacketWakeCnt))), Integer.valueOf(HidlSupport.deepHashCode(this.rxPktWakeDetails)), Integer.valueOf(HidlSupport.deepHashCode(this.rxMulticastPkWakeDetails)), Integer.valueOf(HidlSupport.deepHashCode(this.rxIcmpPkWakeDetails))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".totalCmdEventWakeCnt = ");
        builder.append(this.totalCmdEventWakeCnt);
        builder.append(", .cmdEventWakeCntPerType = ");
        builder.append(this.cmdEventWakeCntPerType);
        builder.append(", .totalDriverFwLocalWakeCnt = ");
        builder.append(this.totalDriverFwLocalWakeCnt);
        builder.append(", .driverFwLocalWakeCntPerType = ");
        builder.append(this.driverFwLocalWakeCntPerType);
        builder.append(", .totalRxPacketWakeCnt = ");
        builder.append(this.totalRxPacketWakeCnt);
        builder.append(", .rxPktWakeDetails = ");
        builder.append(this.rxPktWakeDetails);
        builder.append(", .rxMulticastPkWakeDetails = ");
        builder.append(this.rxMulticastPkWakeDetails);
        builder.append(", .rxIcmpPkWakeDetails = ");
        builder.append(this.rxIcmpPkWakeDetails);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(96), 0);
    }

    public static final ArrayList<WifiDebugHostWakeReasonStats> readVectorFromParcel(HwParcel parcel) {
        ArrayList<WifiDebugHostWakeReasonStats> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 96), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            WifiDebugHostWakeReasonStats _hidl_vec_element = new WifiDebugHostWakeReasonStats();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 96));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        HwParcel hwParcel = parcel;
        HwBlob hwBlob = _hidl_blob;
        this.totalCmdEventWakeCnt = hwBlob.getInt32(_hidl_offset + 0);
        int _hidl_vec_size = hwBlob.getInt32((_hidl_offset + 8) + 8);
        int _hidl_vec_size2 = _hidl_vec_size;
        HwBlob childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size * 4), _hidl_blob.handle(), (_hidl_offset + 8) + 0, 1);
        this.cmdEventWakeCntPerType.clear();
        int _hidl_index_02 = 0;
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            this.cmdEventWakeCntPerType.add(Integer.valueOf(childBlob.getInt32((long) (_hidl_index_0 * 4))));
        }
        this.totalDriverFwLocalWakeCnt = hwBlob.getInt32(_hidl_offset + 24);
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 32) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 4), _hidl_blob.handle(), 0 + (_hidl_offset + 32), true);
        this.driverFwLocalWakeCntPerType.clear();
        while (true) {
            _hidl_index_0 = _hidl_index_02;
            if (_hidl_index_0 < _hidl_vec_size2) {
                this.driverFwLocalWakeCntPerType.add(Integer.valueOf(childBlob.getInt32((long) (_hidl_index_0 * 4))));
                _hidl_index_02 = _hidl_index_0 + 1;
            } else {
                this.totalRxPacketWakeCnt = hwBlob.getInt32(_hidl_offset + 48);
                this.rxPktWakeDetails.readEmbeddedFromParcel(hwParcel, hwBlob, _hidl_offset + 52);
                this.rxMulticastPkWakeDetails.readEmbeddedFromParcel(hwParcel, hwBlob, _hidl_offset + 64);
                this.rxIcmpPkWakeDetails.readEmbeddedFromParcel(hwParcel, hwBlob, _hidl_offset + 76);
                return;
            }
        }
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(96);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<WifiDebugHostWakeReasonStats> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        int _hidl_index_0 = 0;
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 96);
        while (_hidl_index_0 < _hidl_vec_size) {
            ((WifiDebugHostWakeReasonStats) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 96));
            _hidl_index_0++;
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        HwBlob hwBlob = _hidl_blob;
        hwBlob.putInt32(_hidl_offset + 0, this.totalCmdEventWakeCnt);
        int _hidl_vec_size = this.cmdEventWakeCntPerType.size();
        hwBlob.putInt32((_hidl_offset + 8) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 8) + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 4);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            childBlob.putInt32((long) (_hidl_index_0 * 4), ((Integer) this.cmdEventWakeCntPerType.get(_hidl_index_0)).intValue());
        }
        hwBlob.putBlob((_hidl_offset + 8) + 0, childBlob);
        hwBlob.putInt32(_hidl_offset + 24, this.totalDriverFwLocalWakeCnt);
        _hidl_vec_size = this.driverFwLocalWakeCntPerType.size();
        hwBlob.putInt32((_hidl_offset + 32) + 8, _hidl_vec_size);
        int _hidl_index_02 = 0;
        hwBlob.putBool((_hidl_offset + 32) + 12, false);
        HwBlob childBlob2 = new HwBlob(_hidl_vec_size * 4);
        while (true) {
            int _hidl_index_03 = _hidl_index_02;
            if (_hidl_index_03 < _hidl_vec_size) {
                childBlob2.putInt32((long) (_hidl_index_03 * 4), ((Integer) this.driverFwLocalWakeCntPerType.get(_hidl_index_03)).intValue());
                _hidl_index_02 = _hidl_index_03 + 1;
            } else {
                hwBlob.putBlob((_hidl_offset + 32) + 0, childBlob2);
                hwBlob.putInt32(_hidl_offset + 48, this.totalRxPacketWakeCnt);
                this.rxPktWakeDetails.writeEmbeddedToBlob(hwBlob, _hidl_offset + 52);
                this.rxMulticastPkWakeDetails.writeEmbeddedToBlob(hwBlob, _hidl_offset + 64);
                this.rxIcmpPkWakeDetails.writeEmbeddedToBlob(hwBlob, _hidl_offset + 76);
                return;
            }
        }
    }
}
