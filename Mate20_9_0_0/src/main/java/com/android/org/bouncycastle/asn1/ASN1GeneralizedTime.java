package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class ASN1GeneralizedTime extends ASN1Primitive {
    private byte[] time;

    public static ASN1GeneralizedTime getInstance(Object obj) {
        if (obj == null || (obj instanceof ASN1GeneralizedTime)) {
            return (ASN1GeneralizedTime) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (ASN1GeneralizedTime) ASN1Primitive.fromByteArray((byte[]) obj);
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("encoding error in getInstance: ");
                stringBuilder.append(e.toString());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("illegal object in getInstance: ");
        stringBuilder2.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    public static ASN1GeneralizedTime getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        if (explicit || (o instanceof ASN1GeneralizedTime)) {
            return getInstance(o);
        }
        return new ASN1GeneralizedTime(((ASN1OctetString) o).getOctets());
    }

    public ASN1GeneralizedTime(String time) {
        this.time = Strings.toByteArray(time);
        try {
            getDate();
        } catch (ParseException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid date string: ");
            stringBuilder.append(e.getMessage());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public ASN1GeneralizedTime(Date time) {
        SimpleDateFormat dateF = new SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.US);
        dateF.setTimeZone(new SimpleTimeZone(0, "Z"));
        this.time = Strings.toByteArray(dateF.format(time));
    }

    public ASN1GeneralizedTime(Date time, Locale locale) {
        SimpleDateFormat dateF = new SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.US);
        dateF.setCalendar(Calendar.getInstance(Locale.US));
        dateF.setTimeZone(new SimpleTimeZone(0, "Z"));
        this.time = Strings.toByteArray(dateF.format(time));
    }

    ASN1GeneralizedTime(byte[] bytes) {
        this.time = bytes;
    }

    public String getTimeString() {
        return Strings.fromByteArray(this.time);
    }

    public String getTime() {
        String stime = Strings.fromByteArray(this.time);
        if (stime.charAt(stime.length() - 1) == 'Z') {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(stime.substring(0, stime.length() - 1));
            stringBuilder.append("GMT+00:00");
            return stringBuilder.toString();
        }
        int signPos = stime.length() - 5;
        char sign = stime.charAt(signPos);
        if (sign == '-' || sign == '+') {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(stime.substring(0, signPos));
            stringBuilder2.append("GMT");
            stringBuilder2.append(stime.substring(signPos, signPos + 3));
            stringBuilder2.append(":");
            stringBuilder2.append(stime.substring(signPos + 3));
            return stringBuilder2.toString();
        }
        int signPos2 = stime.length() - 3;
        char sign2 = stime.charAt(signPos2);
        if (sign2 == '-' || sign2 == '+') {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(stime.substring(0, signPos2));
            stringBuilder3.append("GMT");
            stringBuilder3.append(stime.substring(signPos2));
            stringBuilder3.append(":00");
            return stringBuilder3.toString();
        }
        signPos = new StringBuilder();
        signPos.append(stime);
        signPos.append(calculateGMTOffset());
        return signPos.toString();
    }

    private String calculateGMTOffset() {
        String sign = "+";
        TimeZone timeZone = TimeZone.getDefault();
        int offset = timeZone.getRawOffset();
        if (offset < 0) {
            sign = "-";
            offset = -offset;
        }
        int hours = offset / 3600000;
        int minutes = (offset - (((hours * 60) * 60) * 1000)) / 60000;
        try {
            if (timeZone.useDaylightTime() && timeZone.inDaylightTime(getDate())) {
                hours += sign.equals("+") ? 1 : -1;
            }
        } catch (ParseException e) {
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("GMT");
        stringBuilder.append(sign);
        stringBuilder.append(convert(hours));
        stringBuilder.append(":");
        stringBuilder.append(convert(minutes));
        return stringBuilder.toString();
    }

    private String convert(int time) {
        if (time >= 10) {
            return Integer.toString(time);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0");
        stringBuilder.append(time);
        return stringBuilder.toString();
    }

    public Date getDate() throws ParseException {
        SimpleDateFormat dateF;
        String stime = Strings.fromByteArray(this.time);
        String d = stime;
        if (stime.endsWith("Z")) {
            if (hasFractionalSeconds()) {
                dateF = new SimpleDateFormat("yyyyMMddHHmmss.SSS'Z'", Locale.US);
            } else {
                dateF = new SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.US);
            }
            dateF.setTimeZone(new SimpleTimeZone(0, "Z"));
        } else if (stime.indexOf(45) > 0 || stime.indexOf(43) > 0) {
            d = getTime();
            if (hasFractionalSeconds()) {
                dateF = new SimpleDateFormat("yyyyMMddHHmmss.SSSz", Locale.US);
            } else {
                dateF = new SimpleDateFormat("yyyyMMddHHmmssz", Locale.US);
            }
            dateF.setTimeZone(new SimpleTimeZone(0, "Z"));
        } else {
            if (hasFractionalSeconds()) {
                dateF = new SimpleDateFormat("yyyyMMddHHmmss.SSS", Locale.US);
            } else {
                dateF = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            }
            dateF.setTimeZone(new SimpleTimeZone(0, TimeZone.getDefault().getID()));
        }
        if (hasFractionalSeconds()) {
            String frac = d.substring(14);
            int index = 1;
            while (index < frac.length()) {
                char ch = frac.charAt(index);
                if ('0' > ch || ch > '9') {
                    break;
                }
                index++;
            }
            StringBuilder stringBuilder;
            if (index - 1 > 3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(frac.substring(0, 4));
                stringBuilder.append(frac.substring(index));
                frac = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(d.substring(0, 14));
                stringBuilder.append(frac);
                d = stringBuilder.toString();
            } else if (index - 1 == 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(frac.substring(0, index));
                stringBuilder.append("00");
                stringBuilder.append(frac.substring(index));
                frac = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(d.substring(0, 14));
                stringBuilder.append(frac);
                d = stringBuilder.toString();
            } else if (index - 1 == 2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(frac.substring(0, index));
                stringBuilder.append("0");
                stringBuilder.append(frac.substring(index));
                frac = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(d.substring(0, 14));
                stringBuilder.append(frac);
                d = stringBuilder.toString();
            }
        }
        return dateF.parse(d);
    }

    private boolean hasFractionalSeconds() {
        int i = 0;
        while (i != this.time.length) {
            if (this.time[i] == (byte) 46 && i == 14) {
                return true;
            }
            i++;
        }
        return false;
    }

    boolean isConstructed() {
        return false;
    }

    int encodedLength() {
        int length = this.time.length;
        return (1 + StreamUtil.calculateBodyLength(length)) + length;
    }

    void encode(ASN1OutputStream out) throws IOException {
        out.writeEncoded(24, this.time);
    }

    boolean asn1Equals(ASN1Primitive o) {
        if (o instanceof ASN1GeneralizedTime) {
            return Arrays.areEqual(this.time, ((ASN1GeneralizedTime) o).time);
        }
        return false;
    }

    public int hashCode() {
        return Arrays.hashCode(this.time);
    }
}
