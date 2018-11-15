package com.android.server.pm.auth.processor;

import android.annotation.SuppressLint;
import android.content.pm.PackageParser.Package;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.HwCertification.CertificationData;
import com.android.server.pm.auth.util.HwAuthLogger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;

public class ValidPeriodProcessor extends BaseProcessor {
    /* JADX WARNING: Missing block: B:15:0x0033, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean readCert(String line, CertificationData rawCert) {
        if (line == null || line.isEmpty() || !line.startsWith(HwCertification.KEY_VALID_PERIOD)) {
            return false;
        }
        String key = line.substring(HwCertification.KEY_VALID_PERIOD.length() + 1);
        if (key == null || key.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "VP_RC empty");
            return false;
        }
        rawCert.mPeriodString = key;
        return true;
    }

    @SuppressLint({"AvoidMethodInForLoop"})
    public boolean parserCert(HwCertification rawCert) {
        String period = rawCert.mCertificationData.mPeriodString;
        if (period == null) {
            HwAuthLogger.e("HwCertificationManager", "VP_PC error time is null ");
            return false;
        }
        String dateElem = "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})";
        String dateRegx = new StringBuilder();
        dateRegx.append("from ");
        dateRegx.append(dateElem);
        dateRegx.append(" ");
        dateRegx.append(HwCertification.KEY_DATE_TO);
        dateRegx.append(" ");
        dateRegx.append(dateElem);
        Matcher m = Pattern.compile(dateRegx.toString()).matcher(period);
        int i = 1;
        if (m.matches()) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                rawCert.setFromDate(dateFormat.parse(m.group(1)));
                rawCert.setToDate(dateFormat.parse(m.group(2)));
                return true;
            } catch (ParseException e) {
                HwAuthLogger.e("HwCertificationManager", "VP_PC time parser exception");
                if (HwAuthLogger.getHWFLOW()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("period:");
                    stringBuilder.append(period);
                    stringBuilder.append("m.groupCount():");
                    stringBuilder.append(m.groupCount());
                    HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                    while (i <= m.groupCount()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("m.group");
                        stringBuilder.append(i);
                        stringBuilder.append(":");
                        stringBuilder.append(m.group(i));
                        HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                        i++;
                    }
                }
                return false;
            }
        }
        if (HwAuthLogger.getHWFLOW()) {
            while (true) {
                int i2 = i;
                if (i2 > m.groupCount()) {
                    break;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("m.group");
                stringBuilder2.append(i2);
                stringBuilder2.append(":");
                stringBuilder2.append(m.group(i2));
                HwAuthLogger.i("HwCertificationManager", stringBuilder2.toString());
                i = i2 + 1;
            }
        }
        HwAuthLogger.e("HwCertificationManager", "VP_PC error");
        return false;
    }

    public boolean verifyCert(Package pkg, HwCertification cert) {
        if (HwAuthLogger.getHWFLOW()) {
            HwAuthLogger.i("HwCertificationManager", "VP_VC start");
        }
        Date fromDate = cert.getFromDate();
        Date toDate = cert.getToDate();
        if (fromDate == null || toDate == null) {
            return false;
        }
        StringBuilder stringBuilder;
        if (!toDate.after(fromDate)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("VP_VC date error from time is : ");
            stringBuilder.append(fromDate);
            stringBuilder.append(", toTime is:");
            stringBuilder.append(toDate);
            HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
            return false;
        } else if (!isCurrentDataExpired(toDate)) {
            return true;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("VP_VC date expired ");
            stringBuilder.append(cert.mCertificationData.mPeriodString);
            HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
            return false;
        }
    }

    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        if (!HwCertification.KEY_VALID_PERIOD.equals(tag)) {
            return false;
        }
        cert.mCertificationData.mPeriodString = parser.getAttributeValue(null, "value");
        return true;
    }

    private boolean isCurrentDataExpired(Date toDate) {
        return System.currentTimeMillis() > toDate.getTime();
    }
}
