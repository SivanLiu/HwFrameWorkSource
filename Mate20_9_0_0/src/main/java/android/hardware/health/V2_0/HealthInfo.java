package android.hardware.health.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class HealthInfo {
    public int batteryCurrentAverage;
    public final ArrayList<DiskStats> diskStats = new ArrayList();
    public final android.hardware.health.V1_0.HealthInfo legacy = new android.hardware.health.V1_0.HealthInfo();
    public final ArrayList<StorageInfo> storageInfos = new ArrayList();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != HealthInfo.class) {
            return false;
        }
        HealthInfo other = (HealthInfo) otherObject;
        if (HidlSupport.deepEquals(this.legacy, other.legacy) && this.batteryCurrentAverage == other.batteryCurrentAverage && HidlSupport.deepEquals(this.diskStats, other.diskStats) && HidlSupport.deepEquals(this.storageInfos, other.storageInfos)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(this.legacy)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.batteryCurrentAverage))), Integer.valueOf(HidlSupport.deepHashCode(this.diskStats)), Integer.valueOf(HidlSupport.deepHashCode(this.storageInfos))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".legacy = ");
        builder.append(this.legacy);
        builder.append(", .batteryCurrentAverage = ");
        builder.append(this.batteryCurrentAverage);
        builder.append(", .diskStats = ");
        builder.append(this.diskStats);
        builder.append(", .storageInfos = ");
        builder.append(this.storageInfos);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(112), 0);
    }

    public static final ArrayList<HealthInfo> readVectorFromParcel(HwParcel parcel) {
        ArrayList<HealthInfo> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 112), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            HealthInfo _hidl_vec_element = new HealthInfo();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 112));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        HwParcel hwParcel = parcel;
        HwBlob hwBlob = _hidl_blob;
        this.legacy.readEmbeddedFromParcel(hwParcel, hwBlob, _hidl_offset + 0);
        this.batteryCurrentAverage = hwBlob.getInt32(_hidl_offset + 72);
        int _hidl_vec_size = hwBlob.getInt32((_hidl_offset + 80) + 8);
        int _hidl_vec_size2 = _hidl_vec_size;
        HwBlob childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size * 112), _hidl_blob.handle(), (_hidl_offset + 80) + 0, 1);
        this.diskStats.clear();
        int _hidl_index_02 = 0;
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size2; _hidl_index_0++) {
            DiskStats _hidl_vec_element = new DiskStats();
            _hidl_vec_element.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 112));
            this.diskStats.add(_hidl_vec_element);
        }
        _hidl_vec_size2 = hwBlob.getInt32((_hidl_offset + 96) + 8);
        childBlob = hwParcel.readEmbeddedBuffer((long) (_hidl_vec_size2 * 48), _hidl_blob.handle(), 0 + (_hidl_offset + 96), true);
        this.storageInfos.clear();
        while (true) {
            _hidl_index_0 = _hidl_index_02;
            if (_hidl_index_0 < _hidl_vec_size2) {
                StorageInfo _hidl_vec_element2 = new StorageInfo();
                _hidl_vec_element2.readEmbeddedFromParcel(hwParcel, childBlob, (long) (_hidl_index_0 * 48));
                this.storageInfos.add(_hidl_vec_element2);
                _hidl_index_02 = _hidl_index_0 + 1;
            } else {
                return;
            }
        }
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(112);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<HealthInfo> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        int _hidl_index_0 = 0;
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 112);
        while (_hidl_index_0 < _hidl_vec_size) {
            ((HealthInfo) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 112));
            _hidl_index_0++;
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0;
        HwBlob hwBlob = _hidl_blob;
        this.legacy.writeEmbeddedToBlob(hwBlob, _hidl_offset + 0);
        hwBlob.putInt32(_hidl_offset + 72, this.batteryCurrentAverage);
        int _hidl_vec_size = this.diskStats.size();
        hwBlob.putInt32((_hidl_offset + 80) + 8, _hidl_vec_size);
        hwBlob.putBool((_hidl_offset + 80) + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 112);
        for (_hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((DiskStats) this.diskStats.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 112));
        }
        hwBlob.putBlob((_hidl_offset + 80) + 0, childBlob);
        _hidl_vec_size = this.storageInfos.size();
        hwBlob.putInt32((_hidl_offset + 96) + 8, _hidl_vec_size);
        int _hidl_index_02 = 0;
        hwBlob.putBool((_hidl_offset + 96) + 12, false);
        childBlob = new HwBlob(_hidl_vec_size * 48);
        while (true) {
            _hidl_index_0 = _hidl_index_02;
            if (_hidl_index_0 < _hidl_vec_size) {
                ((StorageInfo) this.storageInfos.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 48));
                _hidl_index_02 = _hidl_index_0 + 1;
            } else {
                hwBlob.putBlob((_hidl_offset + 96) + 0, childBlob);
                return;
            }
        }
    }
}
