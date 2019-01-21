package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.android.internal.telephony.RILConstants;

public class RadioAccessFamily implements Parcelable {
    private static final int CDMA = 112;
    public static final Creator<RadioAccessFamily> CREATOR = new Creator<RadioAccessFamily>() {
        public RadioAccessFamily createFromParcel(Parcel in) {
            return new RadioAccessFamily(in.readInt(), in.readInt());
        }

        public RadioAccessFamily[] newArray(int size) {
            return new RadioAccessFamily[size];
        }
    };
    private static final int EVDO = 12672;
    private static final int GSM = 65542;
    private static final int HS = 36352;
    private static final int LTE = 540672;
    public static final int RAF_1xRTT = 64;
    public static final int RAF_EDGE = 4;
    public static final int RAF_EHRPD = 8192;
    public static final int RAF_EVDO_0 = 128;
    public static final int RAF_EVDO_A = 256;
    public static final int RAF_EVDO_B = 4096;
    public static final int RAF_GPRS = 2;
    public static final int RAF_GSM = 65536;
    public static final int RAF_HSDPA = 512;
    public static final int RAF_HSPA = 2048;
    public static final int RAF_HSPAP = 32768;
    public static final int RAF_HSUPA = 1024;
    public static final int RAF_IS95A = 16;
    public static final int RAF_IS95B = 32;
    public static final int RAF_LTE = 16384;
    public static final int RAF_LTE_CA = 524288;
    public static final int RAF_TD_SCDMA = 131072;
    public static final int RAF_UMTS = 8;
    public static final int RAF_UNKNOWN = 1;
    private static final int WCDMA = 36360;
    private int mPhoneId;
    private int mRadioAccessFamily;

    public RadioAccessFamily(int phoneId, int radioAccessFamily) {
        this.mPhoneId = phoneId;
        this.mRadioAccessFamily = radioAccessFamily;
    }

    public int getPhoneId() {
        return this.mPhoneId;
    }

    public int getRadioAccessFamily() {
        return this.mRadioAccessFamily;
    }

    public String toString() {
        String ret = new StringBuilder();
        ret.append("{ mPhoneId = ");
        ret.append(this.mPhoneId);
        ret.append(", mRadioAccessFamily = ");
        ret.append(this.mRadioAccessFamily);
        ret.append("}");
        return ret.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel outParcel, int flags) {
        outParcel.writeInt(this.mPhoneId);
        outParcel.writeInt(this.mRadioAccessFamily);
    }

    public static int getRafFromNetworkType(int type) {
        switch (type) {
            case 0:
                return 101902;
            case 1:
                return GSM;
            case 2:
                return WCDMA;
            case 3:
                return 101902;
            case 4:
                return 12784;
            case 5:
                return 112;
            case 6:
                return EVDO;
            case 7:
                return 114686;
            case 8:
                return 553456;
            case 9:
                return 642574;
            case 10:
                return 655358;
            case 11:
                return LTE;
            case 12:
                return 577032;
            case 13:
                return 131072;
            case 14:
                return 167432;
            case 15:
                return 671744;
            case 16:
                return 196614;
            case 17:
                return 737286;
            case 18:
                return 232974;
            case 19:
                return 708104;
            case 20:
                return 773646;
            case 21:
                return 245758;
            case 22:
                return 786430;
            default:
                return 1;
        }
    }

    private static int getAdjustedRaf(int raf) {
        raf = (GSM & raf) > 0 ? GSM | raf : raf;
        raf = (WCDMA & raf) > 0 ? WCDMA | raf : raf;
        raf = (112 & raf) > 0 ? 112 | raf : raf;
        raf = (EVDO & raf) > 0 ? EVDO | raf : raf;
        return (LTE & raf) > 0 ? LTE | raf : raf;
    }

    public static int getHighestRafCapability(int raf) {
        if ((LTE & raf) > 0) {
            return 3;
        }
        if ((49024 | (WCDMA & raf)) > 0) {
            return 2;
        }
        if ((GSM | (112 & raf)) > 0) {
            return 1;
        }
        return 0;
    }

    public static int getNetworkTypeFromRaf(int raf) {
        switch (getAdjustedRaf(raf)) {
            case 112:
                return 5;
            case EVDO /*12672*/:
                return 6;
            case 12784:
                return 4;
            case WCDMA /*36360*/:
                return 2;
            case GSM /*65542*/:
                return 1;
            case 101902:
                return 0;
            case 114686:
                return 7;
            case 131072:
                return 13;
            case 167432:
                return 14;
            case 196614:
                return 16;
            case 232974:
                return 18;
            case 245758:
                return 21;
            case LTE /*540672*/:
                return 11;
            case 553456:
                return 8;
            case 577032:
                return 12;
            case 642574:
                return 9;
            case 655358:
                return 10;
            case 671744:
                return 15;
            case 708104:
                return 19;
            case 737286:
                return 17;
            case 773646:
                return 20;
            case 786430:
                return 22;
            default:
                return RILConstants.PREFERRED_NETWORK_MODE;
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int singleRafTypeFromString(String rafString) {
        int i;
        switch (rafString.hashCode()) {
            case -2039427040:
                if (rafString.equals("LTE_CA")) {
                    i = 21;
                    break;
                }
            case -908593671:
                if (rafString.equals("TD_SCDMA")) {
                    i = 16;
                    break;
                }
            case 2315:
                if (rafString.equals("HS")) {
                    i = 17;
                    break;
                }
            case 70881:
                if (rafString.equals("GSM")) {
                    i = 15;
                    break;
                }
            case 75709:
                if (rafString.equals("LTE")) {
                    i = 13;
                    break;
                }
            case 2063797:
                if (rafString.equals("CDMA")) {
                    i = 18;
                    break;
                }
            case 2123197:
                if (rafString.equals("EDGE")) {
                    i = 1;
                    break;
                }
            case 2140412:
                if (rafString.equals("EVDO")) {
                    i = 19;
                    break;
                }
            case 2194666:
                if (rafString.equals("GPRS")) {
                    i = 0;
                    break;
                }
            case 2227260:
                if (rafString.equals("HSPA")) {
                    i = 10;
                    break;
                }
            case 2608919:
                if (rafString.equals("UMTS")) {
                    i = 2;
                    break;
                }
            case 47955627:
                if (rafString.equals("1XRTT")) {
                    i = 5;
                    break;
                }
            case 65949251:
                if (rafString.equals("EHRPD")) {
                    i = 12;
                    break;
                }
            case 69034058:
                if (rafString.equals("HSDPA")) {
                    i = 8;
                    break;
                }
            case 69045140:
                if (rafString.equals("HSPAP")) {
                    i = 14;
                    break;
                }
            case 69050395:
                if (rafString.equals("HSUPA")) {
                    i = 9;
                    break;
                }
            case 69946171:
                if (rafString.equals("IS95A")) {
                    i = 3;
                    break;
                }
            case 69946172:
                if (rafString.equals("IS95B")) {
                    i = 4;
                    break;
                }
            case 82410124:
                if (rafString.equals("WCDMA")) {
                    i = 20;
                    break;
                }
            case 2056938925:
                if (rafString.equals("EVDO_0")) {
                    i = 6;
                    break;
                }
            case 2056938942:
                if (rafString.equals("EVDO_A")) {
                    i = 7;
                    break;
                }
            case 2056938943:
                if (rafString.equals("EVDO_B")) {
                    i = 11;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                return 2;
            case 1:
                return 4;
            case 2:
                return 8;
            case 3:
                return 16;
            case 4:
                return 32;
            case 5:
                return 64;
            case 6:
                return 128;
            case 7:
                return 256;
            case 8:
                return 512;
            case 9:
                return 1024;
            case 10:
                return 2048;
            case 11:
                return 4096;
            case 12:
                return 8192;
            case 13:
                return 16384;
            case 14:
                return 32768;
            case 15:
                return 65536;
            case 16:
                return 131072;
            case 17:
                return HS;
            case 18:
                return 112;
            case 19:
                return EVDO;
            case 20:
                return WCDMA;
            case 21:
                return 524288;
            default:
                return 1;
        }
    }

    public static int rafTypeFromString(String rafList) {
        int result = 0;
        for (String raf : rafList.toUpperCase().split("\\|")) {
            int rafType = singleRafTypeFromString(raf.trim());
            if (rafType == 1) {
                return rafType;
            }
            result |= rafType;
        }
        return result;
    }
}
