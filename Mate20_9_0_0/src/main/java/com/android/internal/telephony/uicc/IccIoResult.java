package com.android.internal.telephony.uicc;

import android.hardware.radio.V1_0.LastCallFailCause;
import android.os.Build;
import com.android.internal.telephony.AbstractPhoneBase;
import com.google.android.mms.pdu.PduHeaders;

public class IccIoResult {
    private static final String UNKNOWN_ERROR = "unknown";
    private int fileId;
    private boolean isValid;
    public byte[] payload;
    public int sw1;
    public int sw2;

    private String getErrorString() {
        int i = this.sw1;
        if (i != 152) {
            switch (i) {
                case 98:
                    i = this.sw2;
                    if (i != 0) {
                        switch (i) {
                            case 129:
                                return "Part of returned data may be corrupted";
                            case 130:
                                return "End of file/record reached before reading Le bytes";
                            case 131:
                                return "Selected file invalidated";
                            case 132:
                                return "Selected file in termination state";
                            default:
                                switch (i) {
                                    case 241:
                                        return "More data available";
                                    case LastCallFailCause.IMSI_UNKNOWN_IN_VLR /*242*/:
                                        return "More data available and proactive command pending";
                                    case 243:
                                        return "Response data available";
                                }
                                break;
                        }
                    }
                    return "No information given, state of non volatile memory unchanged";
                case 99:
                    if ((this.sw2 >> 4) == 12) {
                        return "Command successful but after using an internalupdate retry routine but Verification failed";
                    }
                    switch (this.sw2) {
                        case 241:
                            return "More data expected";
                        case LastCallFailCause.IMSI_UNKNOWN_IN_VLR /*242*/:
                            return "More data expected and proactive command pending";
                    }
                    break;
                case 100:
                    if (this.sw2 == 0) {
                        return "No information given, state of non-volatile memory unchanged";
                    }
                    break;
                case 101:
                    i = this.sw2;
                    if (i == 0) {
                        return "No information given, state of non-volatile memory changed";
                    }
                    if (i == 129) {
                        return "Memory problem";
                    }
                    break;
                default:
                    switch (i) {
                        case 103:
                            if (this.sw2 != 0) {
                                return "The interpretation of this status word is command dependent";
                            }
                            return "incorrect parameter P3";
                        case AbstractPhoneBase.EVENT_ECC_NUM /*104*/:
                            i = this.sw2;
                            if (i == 0) {
                                return "No information given";
                            }
                            switch (i) {
                                case 129:
                                    return "Logical channel not supported";
                                case 130:
                                    return "Secure messaging not supported";
                            }
                            break;
                        case AbstractPhoneBase.EVENT_GET_IMSI_DONE /*105*/:
                            i = this.sw2;
                            if (i == 0) {
                                return "No information given";
                            }
                            if (i == 137) {
                                return "Command not allowed - secure channel - security not satisfied";
                            }
                            switch (i) {
                                case 129:
                                    return "Command incompatible with file structure";
                                case 130:
                                    return "Security status not satisfied";
                                case 131:
                                    return "Authentication/PIN method blocked";
                                case 132:
                                    return "Referenced data invalidated";
                                case 133:
                                    return "Conditions of use not satisfied";
                                case 134:
                                    return "Command not allowed (no EF selected)";
                            }
                            break;
                        case 106:
                            switch (this.sw2) {
                                case 128:
                                    return "Incorrect parameters in the data field";
                                case 129:
                                    return "Function not supported";
                                case 130:
                                    return "File not found";
                                case 131:
                                    return "Record not found";
                                case 132:
                                    return "Not enough memory space";
                                case 134:
                                    return "Incorrect parameters P1 to P2";
                                case 135:
                                    return "Lc inconsistent with P1 to P2";
                                case 136:
                                    return "Referenced data not found";
                            }
                            break;
                        case 107:
                            return "incorrect parameter P1 or P2";
                        default:
                            switch (i) {
                                case 109:
                                    return "unknown instruction code given in the command";
                                case 110:
                                    return "wrong instruction class given in the command";
                                case 111:
                                    if (this.sw2 != 0) {
                                        return "The interpretation of this status word is command dependent";
                                    }
                                    return "technical problem with no diagnostic given";
                                default:
                                    switch (i) {
                                        case 144:
                                            return null;
                                        case 145:
                                            return null;
                                        case 146:
                                            if ((this.sw2 >> 4) == 0) {
                                                return "command successful but after using an internal update retry routine";
                                            }
                                            if (this.sw2 == 64) {
                                                return "memory problem";
                                            }
                                            break;
                                        case 147:
                                            if (this.sw2 == 0) {
                                                return "SIM Application Toolkit is busy. Command cannot be executed at present, further normal commands are allowed.";
                                            }
                                            break;
                                        case 148:
                                            i = this.sw2;
                                            if (i == 0) {
                                                return "no EF selected";
                                            }
                                            if (i == 2) {
                                                return "out f range (invalid address)";
                                            }
                                            if (i == 4) {
                                                return "file ID not found/pattern not found";
                                            }
                                            if (i == 8) {
                                                return "file is inconsistent with the command";
                                            }
                                            break;
                                        default:
                                            switch (i) {
                                                case PduHeaders.REPLY_CHARGING_ID /*158*/:
                                                    return null;
                                                case PduHeaders.REPLY_CHARGING_SIZE /*159*/:
                                                    return null;
                                            }
                                            break;
                                    }
                            }
                    }
            }
        }
        i = this.sw2;
        if (i == 2) {
            return "no CHV initialized";
        }
        if (i == 4) {
            return "access condition not fulfilled/unsuccessful CHV verification, at least one attempt left/unsuccessful UNBLOCK CHV verification, at least one attempt left/authentication failed";
        }
        if (i == 8) {
            return "in contradiction with CHV status";
        }
        if (i == 16) {
            return "in contradiction with invalidation status";
        }
        if (i == 64) {
            return "unsuccessful CHV verification, no attempt left/unsuccessful UNBLOCK CHV verification, no attempt left/CHV blockedUNBLOCK CHV blocked";
        }
        if (i == 80) {
            return "increase cannot be performed, Max value reached";
        }
        if (i == 98) {
            return "authentication error, application specific";
        }
        switch (i) {
            case 100:
                return "authentication error, security context not supported";
            case 101:
                return "key freshness failure";
            case 102:
                return "authentication error, no memory space available";
            case 103:
                return "authentication error, no memory space available in EF_MUK";
        }
        return UNKNOWN_ERROR;
    }

    public IccIoResult(int sw1, int sw2, byte[] payload) {
        this.sw1 = sw1;
        this.sw2 = sw2;
        this.payload = payload;
    }

    public IccIoResult(int sw1, int sw2, String hexString) {
        this(sw1, sw2, IccUtils.hexStringToBytes(hexString));
    }

    public IccIoResult(boolean isValid, int fileId, int sw1, int sw2, byte[] payload) {
        this(sw1, sw2, payload);
        this.isValid = isValid;
        this.fileId = fileId;
    }

    public IccIoResult(boolean isValid, int fileId, int sw1, int sw2, String hexString) {
        this(isValid, fileId, sw1, sw2, IccUtils.hexStringToBytes(hexString));
    }

    public boolean isValidIccioResult() {
        return this.isValid;
    }

    public int getFileId() {
        return this.fileId;
    }

    public String toString() {
        String str;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IccIoResult sw1:0x");
        stringBuilder.append(Integer.toHexString(this.sw1));
        stringBuilder.append(" sw2:0x");
        stringBuilder.append(Integer.toHexString(this.sw2));
        stringBuilder.append(" Payload: ");
        Object obj = (Build.IS_DEBUGGABLE && Build.IS_ENG) ? this.payload : "*******";
        stringBuilder.append(obj);
        if (success()) {
            str = "";
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" Error: ");
            stringBuilder2.append(getErrorString());
            str = stringBuilder2.toString();
        }
        stringBuilder.append(str);
        return stringBuilder.toString();
    }

    public boolean success() {
        return this.sw1 == 144 || this.sw1 == 145 || this.sw1 == PduHeaders.REPLY_CHARGING_ID || this.sw1 == PduHeaders.REPLY_CHARGING_SIZE;
    }

    public IccException getException() {
        if (success()) {
            return null;
        }
        if (this.sw1 != 148) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sw1:");
            stringBuilder.append(this.sw1);
            stringBuilder.append(" sw2:");
            stringBuilder.append(this.sw2);
            return new IccException(stringBuilder.toString());
        } else if (this.sw2 == 8) {
            return new IccFileTypeMismatch();
        } else {
            return new IccFileNotFound();
        }
    }
}
