package com.android.internal.telephony;

import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;

public class DriverCall implements Comparable<DriverCall> {
    public static final int AUDIO_QUALITY_AMR = 1;
    public static final int AUDIO_QUALITY_AMR_WB = 2;
    public static final int AUDIO_QUALITY_EVRC = 6;
    public static final int AUDIO_QUALITY_EVRC_B = 7;
    public static final int AUDIO_QUALITY_EVRC_NW = 9;
    public static final int AUDIO_QUALITY_EVRC_WB = 8;
    public static final int AUDIO_QUALITY_GSM_EFR = 3;
    public static final int AUDIO_QUALITY_GSM_FR = 4;
    public static final int AUDIO_QUALITY_GSM_HR = 5;
    public static final int AUDIO_QUALITY_UNSPECIFIED = 0;
    static final String LOG_TAG = "DriverCall";
    public int TOA;
    public int als;
    public int audioQuality = 0;
    public int index;
    public boolean isMT;
    public boolean isMpty;
    public boolean isVoice;
    public boolean isVoicePrivacy;
    public String name;
    public int namePresentation;
    public String number;
    public int numberPresentation;
    public String redirectNumber;
    public int redirectNumberPresentation;
    public int redirectNumberTOA;
    public State state;
    public UUSInfo uusInfo;

    public enum State {
        ACTIVE,
        HOLDING,
        DIALING,
        ALERTING,
        INCOMING,
        WAITING
    }

    static DriverCall fromCLCCLine(String line) {
        DriverCall ret = new DriverCall();
        ATResponseParser p = new ATResponseParser(line);
        try {
            ret.index = p.nextInt();
            ret.isMT = p.nextBoolean();
            ret.state = stateFromCLCC(p.nextInt());
            ret.isVoice = p.nextInt() == 0;
            ret.isMpty = p.nextBoolean();
            ret.numberPresentation = 1;
            if (p.hasMore()) {
                ret.number = PhoneNumberUtils.extractNetworkPortionAlt(p.nextString());
                if (ret.number.length() == 0) {
                    ret.number = null;
                }
                ret.TOA = p.nextInt();
                ret.number = PhoneNumberUtils.stringFromStringAndTOA(ret.number, ret.TOA);
            }
            return ret;
        } catch (ATParseEx e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid CLCC line: '");
            stringBuilder.append(line);
            stringBuilder.append("'");
            Rlog.e(str, stringBuilder.toString());
            return null;
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("id=");
        stringBuilder.append(this.index);
        stringBuilder.append(",");
        stringBuilder.append(this.state);
        stringBuilder.append(",toa=");
        stringBuilder.append(this.TOA);
        stringBuilder.append(",");
        stringBuilder.append(this.isMpty ? "conf" : "norm");
        stringBuilder.append(",");
        stringBuilder.append(this.isMT ? "mt" : "mo");
        stringBuilder.append(",");
        stringBuilder.append(this.als);
        stringBuilder.append(",");
        stringBuilder.append(this.isVoice ? "voc" : "nonvoc");
        stringBuilder.append(",");
        stringBuilder.append(this.isVoicePrivacy ? "evp" : "noevp");
        stringBuilder.append(",,cli=");
        stringBuilder.append(this.numberPresentation);
        stringBuilder.append(",,");
        stringBuilder.append(this.namePresentation);
        stringBuilder.append(",audioQuality=");
        stringBuilder.append(this.audioQuality);
        return stringBuilder.toString();
    }

    public static State stateFromCLCC(int state) throws ATParseEx {
        switch (state) {
            case 0:
                return State.ACTIVE;
            case 1:
                return State.HOLDING;
            case 2:
                return State.DIALING;
            case 3:
                return State.ALERTING;
            case 4:
                return State.INCOMING;
            case 5:
                return State.WAITING;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("illegal call state ");
                stringBuilder.append(state);
                throw new ATParseEx(stringBuilder.toString());
        }
    }

    public static int presentationFromCLIP(int cli) throws ATParseEx {
        switch (cli) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("illegal presentation ");
                stringBuilder.append(cli);
                throw new ATParseEx(stringBuilder.toString());
        }
    }

    public int compareTo(DriverCall dc) {
        if (this.index < dc.index) {
            return -1;
        }
        if (this.index == dc.index) {
            return 0;
        }
        return 1;
    }
}
