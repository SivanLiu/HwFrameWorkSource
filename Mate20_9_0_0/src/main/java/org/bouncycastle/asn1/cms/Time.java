package org.bouncycastle.asn1.cms;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1UTCTime;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERUTCTime;

public class Time extends ASN1Object implements ASN1Choice {
    ASN1Primitive time;

    public Time(Date date) {
        SimpleTimeZone simpleTimeZone = new SimpleTimeZone(0, "Z");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        simpleDateFormat.setTimeZone(simpleTimeZone);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(simpleDateFormat.format(date));
        stringBuilder.append("Z");
        String stringBuilder2 = stringBuilder.toString();
        int parseInt = Integer.parseInt(stringBuilder2.substring(0, 4));
        ASN1Primitive dERGeneralizedTime = (parseInt < 1950 || parseInt > 2049) ? new DERGeneralizedTime(stringBuilder2) : new DERUTCTime(stringBuilder2.substring(2));
        this.time = dERGeneralizedTime;
    }

    public Time(Date date, Locale locale) {
        SimpleTimeZone simpleTimeZone = new SimpleTimeZone(0, "Z");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", locale);
        simpleDateFormat.setTimeZone(simpleTimeZone);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(simpleDateFormat.format(date));
        stringBuilder.append("Z");
        String stringBuilder2 = stringBuilder.toString();
        int parseInt = Integer.parseInt(stringBuilder2.substring(0, 4));
        ASN1Primitive dERGeneralizedTime = (parseInt < 1950 || parseInt > 2049) ? new DERGeneralizedTime(stringBuilder2) : new DERUTCTime(stringBuilder2.substring(2));
        this.time = dERGeneralizedTime;
    }

    public Time(ASN1Primitive aSN1Primitive) {
        if ((aSN1Primitive instanceof ASN1UTCTime) || (aSN1Primitive instanceof ASN1GeneralizedTime)) {
            this.time = aSN1Primitive;
            return;
        }
        throw new IllegalArgumentException("unknown object passed to Time");
    }

    public static Time getInstance(Object obj) {
        if (obj == null || (obj instanceof Time)) {
            return (Time) obj;
        }
        if (obj instanceof ASN1UTCTime) {
            return new Time((ASN1UTCTime) obj);
        }
        if (obj instanceof ASN1GeneralizedTime) {
            return new Time((ASN1GeneralizedTime) obj);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unknown object in factory: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static Time getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(aSN1TaggedObject.getObject());
    }

    public Date getDate() {
        try {
            return this.time instanceof ASN1UTCTime ? ((ASN1UTCTime) this.time).getAdjustedDate() : ((ASN1GeneralizedTime) this.time).getDate();
        } catch (ParseException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid date string: ");
            stringBuilder.append(e.getMessage());
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    public String getTime() {
        return this.time instanceof ASN1UTCTime ? ((ASN1UTCTime) this.time).getAdjustedTime() : ((ASN1GeneralizedTime) this.time).getTime();
    }

    public ASN1Primitive toASN1Primitive() {
        return this.time;
    }
}
