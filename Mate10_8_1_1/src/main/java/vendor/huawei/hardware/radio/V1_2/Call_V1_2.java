package vendor.huawei.hardware.radio.V1_2;

import android.hardware.radio.V1_0.Call;
import android.hardware.radio.V1_0.CallPresentation;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class Call_V1_2 {
    public final Call call = new Call();
    public String redirectNumber = new String();
    public int redirectNumberPresentation;
    public int redirectNumberToa;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != Call_V1_2.class) {
            return false;
        }
        Call_V1_2 other = (Call_V1_2) otherObject;
        return HidlSupport.deepEquals(this.call, other.call) && HidlSupport.deepEquals(this.redirectNumber, other.redirectNumber) && this.redirectNumberToa == other.redirectNumberToa && this.redirectNumberPresentation == other.redirectNumberPresentation;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(this.call)), Integer.valueOf(HidlSupport.deepHashCode(this.redirectNumber)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.redirectNumberToa))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.redirectNumberPresentation)))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".call = ");
        builder.append(this.call);
        builder.append(", .redirectNumber = ");
        builder.append(this.redirectNumber);
        builder.append(", .redirectNumberToa = ");
        builder.append(this.redirectNumberToa);
        builder.append(", .redirectNumberPresentation = ");
        builder.append(CallPresentation.toString(this.redirectNumberPresentation));
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(112), 0);
    }

    public static final ArrayList<Call_V1_2> readVectorFromParcel(HwParcel parcel) {
        ArrayList<Call_V1_2> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 112), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            Call_V1_2 _hidl_vec_element = new Call_V1_2();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 112));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.call.readEmbeddedFromParcel(parcel, _hidl_blob, _hidl_offset + 0);
        this.redirectNumber = _hidl_blob.getString(_hidl_offset + 88);
        parcel.readEmbeddedBuffer((long) (this.redirectNumber.getBytes().length + 1), _hidl_blob.handle(), 0 + (_hidl_offset + 88), false);
        this.redirectNumberToa = _hidl_blob.getInt32(104 + _hidl_offset);
        this.redirectNumberPresentation = _hidl_blob.getInt32(108 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(112);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<Call_V1_2> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 112);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((Call_V1_2) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 112));
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        this.call.writeEmbeddedToBlob(_hidl_blob, 0 + _hidl_offset);
        _hidl_blob.putString(88 + _hidl_offset, this.redirectNumber);
        _hidl_blob.putInt32(104 + _hidl_offset, this.redirectNumberToa);
        _hidl_blob.putInt32(108 + _hidl_offset, this.redirectNumberPresentation);
    }
}
