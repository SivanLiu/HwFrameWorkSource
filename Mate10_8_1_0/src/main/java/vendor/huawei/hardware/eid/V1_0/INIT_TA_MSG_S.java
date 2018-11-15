package vendor.huawei.hardware.eid.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class INIT_TA_MSG_S {
    public final byte[] eid_aid = new byte[256];
    public int eid_aid_len;
    public final byte[] eid_logo = new byte[BUFF_LEN_E.MAX_LOGO_SIZE];
    public final byte[] hw_aid = new byte[256];
    public int hw_aid_len;
    public int logo_size;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != INIT_TA_MSG_S.class) {
            return false;
        }
        INIT_TA_MSG_S other = (INIT_TA_MSG_S) otherObject;
        return HidlSupport.deepEquals(this.hw_aid, other.hw_aid) && this.hw_aid_len == other.hw_aid_len && HidlSupport.deepEquals(this.eid_aid, other.eid_aid) && this.eid_aid_len == other.eid_aid_len && HidlSupport.deepEquals(this.eid_logo, other.eid_logo) && this.logo_size == other.logo_size;
    }

    public final int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(HidlSupport.deepHashCode(this.hw_aid)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.hw_aid_len))), Integer.valueOf(HidlSupport.deepHashCode(this.eid_aid)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.eid_aid_len))), Integer.valueOf(HidlSupport.deepHashCode(this.eid_logo)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.logo_size)))});
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".hw_aid = ");
        builder.append(Arrays.toString(this.hw_aid));
        builder.append(", .hw_aid_len = ");
        builder.append(this.hw_aid_len);
        builder.append(", .eid_aid = ");
        builder.append(Arrays.toString(this.eid_aid));
        builder.append(", .eid_aid_len = ");
        builder.append(this.eid_aid_len);
        builder.append(", .eid_logo = ");
        builder.append(Arrays.toString(this.eid_logo));
        builder.append(", .logo_size = ");
        builder.append(this.logo_size);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(HwParcel parcel) {
        readEmbeddedFromParcel(parcel, parcel.readBuffer(25100), 0);
    }

    public static final ArrayList<INIT_TA_MSG_S> readVectorFromParcel(HwParcel parcel) {
        ArrayList<INIT_TA_MSG_S> _hidl_vec = new ArrayList();
        HwBlob _hidl_blob = parcel.readBuffer(16);
        int _hidl_vec_size = _hidl_blob.getInt32(8);
        HwBlob childBlob = parcel.readEmbeddedBuffer((long) (_hidl_vec_size * 25100), _hidl_blob.handle(), 0, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            INIT_TA_MSG_S _hidl_vec_element = new INIT_TA_MSG_S();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, (long) (_hidl_index_0 * 25100));
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0_0;
        long _hidl_array_offset_0 = _hidl_offset + 0;
        for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 256; _hidl_index_0_0++) {
            this.hw_aid[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
            _hidl_array_offset_0++;
        }
        this.hw_aid_len = _hidl_blob.getInt32(256 + _hidl_offset);
        _hidl_array_offset_0 = _hidl_offset + 260;
        for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 256; _hidl_index_0_0++) {
            this.eid_aid[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
            _hidl_array_offset_0++;
        }
        this.eid_aid_len = _hidl_blob.getInt32(516 + _hidl_offset);
        _hidl_array_offset_0 = _hidl_offset + 520;
        for (_hidl_index_0_0 = 0; _hidl_index_0_0 < BUFF_LEN_E.MAX_LOGO_SIZE; _hidl_index_0_0++) {
            this.eid_logo[_hidl_index_0_0] = _hidl_blob.getInt8(_hidl_array_offset_0);
            _hidl_array_offset_0++;
        }
        this.logo_size = _hidl_blob.getInt32(25096 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(25100);
        writeEmbeddedToBlob(_hidl_blob, 0);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<INIT_TA_MSG_S> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8, _hidl_vec_size);
        _hidl_blob.putBool(12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 25100);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ((INIT_TA_MSG_S) _hidl_vec.get(_hidl_index_0)).writeEmbeddedToBlob(childBlob, (long) (_hidl_index_0 * 25100));
        }
        _hidl_blob.putBlob(0, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_index_0_0;
        long _hidl_array_offset_0 = _hidl_offset + 0;
        for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 256; _hidl_index_0_0++) {
            _hidl_blob.putInt8(_hidl_array_offset_0, this.hw_aid[_hidl_index_0_0]);
            _hidl_array_offset_0++;
        }
        _hidl_blob.putInt32(256 + _hidl_offset, this.hw_aid_len);
        _hidl_array_offset_0 = _hidl_offset + 260;
        for (_hidl_index_0_0 = 0; _hidl_index_0_0 < 256; _hidl_index_0_0++) {
            _hidl_blob.putInt8(_hidl_array_offset_0, this.eid_aid[_hidl_index_0_0]);
            _hidl_array_offset_0++;
        }
        _hidl_blob.putInt32(516 + _hidl_offset, this.eid_aid_len);
        _hidl_array_offset_0 = _hidl_offset + 520;
        for (_hidl_index_0_0 = 0; _hidl_index_0_0 < BUFF_LEN_E.MAX_LOGO_SIZE; _hidl_index_0_0++) {
            _hidl_blob.putInt8(_hidl_array_offset_0, this.eid_logo[_hidl_index_0_0]);
            _hidl_array_offset_0++;
        }
        _hidl_blob.putInt32(25096 + _hidl_offset, this.logo_size);
    }
}
