package vendor.huawei.hardware.eid.V1_0;

import java.util.ArrayList;

public final class BUFF_LEN_E {
    public static final int CERTIFICATE_MAX_LEN = 8192;
    public static final int CERT_REQ_MSG_MAX_LEN = 2048;
    public static final int DE_SKEY_MAX_LEN = 2048;
    public static final int ID_INFO_MAX_LEN = 5120;
    public static final int IMAGE_NV21_SIZE = 460800;
    public static final int INFO_MAX_LEN = 2048;
    public static final int INFO_SIGN_MAX_LEN = 4096;
    public static final int INPUT_MAX_TRANSPOT_LEN = 153600;
    public static final int INPUT_TRANSPOT_TIMES = 3;
    public static final int MAX_AID_LEN = 256;
    public static final int MAX_LOGO_SIZE = 24576;
    public static final int OUTPUT_MAX_TRANSPOT_LEN = 163840;
    public static final int OUTPUT_TRANSPOT_TIMES = 3;
    public static final int SEC_IMAGE_MAX_LEN = 491520;

    public static final String toString(int o) {
        if (o == 8192) {
            return "CERTIFICATE_MAX_LEN";
        }
        if (o == SEC_IMAGE_MAX_LEN) {
            return "SEC_IMAGE_MAX_LEN";
        }
        if (o == 2048) {
            return "DE_SKEY_MAX_LEN";
        }
        if (o == 2048) {
            return "CERT_REQ_MSG_MAX_LEN";
        }
        if (o == 2048) {
            return "INFO_MAX_LEN";
        }
        if (o == 4096) {
            return "INFO_SIGN_MAX_LEN";
        }
        if (o == IMAGE_NV21_SIZE) {
            return "IMAGE_NV21_SIZE";
        }
        if (o == ID_INFO_MAX_LEN) {
            return "ID_INFO_MAX_LEN";
        }
        if (o == OUTPUT_MAX_TRANSPOT_LEN) {
            return "OUTPUT_MAX_TRANSPOT_LEN";
        }
        if (o == INPUT_MAX_TRANSPOT_LEN) {
            return "INPUT_MAX_TRANSPOT_LEN";
        }
        if (o == 3) {
            return "OUTPUT_TRANSPOT_TIMES";
        }
        if (o == 3) {
            return "INPUT_TRANSPOT_TIMES";
        }
        if (o == 256) {
            return "MAX_AID_LEN";
        }
        if (o == MAX_LOGO_SIZE) {
            return "MAX_LOGO_SIZE";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        if ((o & 8192) == 8192) {
            list.add("CERTIFICATE_MAX_LEN");
            flipped = 0 | 8192;
        }
        if ((o & SEC_IMAGE_MAX_LEN) == SEC_IMAGE_MAX_LEN) {
            list.add("SEC_IMAGE_MAX_LEN");
            flipped |= SEC_IMAGE_MAX_LEN;
        }
        if ((o & 2048) == 2048) {
            list.add("DE_SKEY_MAX_LEN");
            flipped |= 2048;
        }
        if ((o & 2048) == 2048) {
            list.add("CERT_REQ_MSG_MAX_LEN");
            flipped |= 2048;
        }
        if ((o & 2048) == 2048) {
            list.add("INFO_MAX_LEN");
            flipped |= 2048;
        }
        if ((o & 4096) == 4096) {
            list.add("INFO_SIGN_MAX_LEN");
            flipped |= 4096;
        }
        if ((o & IMAGE_NV21_SIZE) == IMAGE_NV21_SIZE) {
            list.add("IMAGE_NV21_SIZE");
            flipped |= IMAGE_NV21_SIZE;
        }
        if ((o & ID_INFO_MAX_LEN) == ID_INFO_MAX_LEN) {
            list.add("ID_INFO_MAX_LEN");
            flipped |= ID_INFO_MAX_LEN;
        }
        if ((o & OUTPUT_MAX_TRANSPOT_LEN) == OUTPUT_MAX_TRANSPOT_LEN) {
            list.add("OUTPUT_MAX_TRANSPOT_LEN");
            flipped |= OUTPUT_MAX_TRANSPOT_LEN;
        }
        if ((o & INPUT_MAX_TRANSPOT_LEN) == INPUT_MAX_TRANSPOT_LEN) {
            list.add("INPUT_MAX_TRANSPOT_LEN");
            flipped |= INPUT_MAX_TRANSPOT_LEN;
        }
        if ((o & 3) == 3) {
            list.add("OUTPUT_TRANSPOT_TIMES");
            flipped |= 3;
        }
        if ((o & 3) == 3) {
            list.add("INPUT_TRANSPOT_TIMES");
            flipped |= 3;
        }
        if ((o & 256) == 256) {
            list.add("MAX_AID_LEN");
            flipped |= 256;
        }
        if ((o & MAX_LOGO_SIZE) == MAX_LOGO_SIZE) {
            list.add("MAX_LOGO_SIZE");
            flipped |= MAX_LOGO_SIZE;
        }
        if (o != flipped) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("0x");
            stringBuilder.append(Integer.toHexString((~flipped) & o));
            list.add(stringBuilder.toString());
        }
        return String.join(" | ", list);
    }
}
