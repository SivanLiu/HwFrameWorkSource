package com.huawei.zxing.client.result;

import com.huawei.zxing.Result;
import java.util.regex.Pattern;

public final class EmailDoCoMoResultParser extends AbstractDoCoMoResultParser {
    private static final Pattern ATEXT_ALPHANUMERIC = Pattern.compile("[a-zA-Z0-9@.!#$%&'*+\\-/=?^_`{|}~]+");

    public EmailAddressParsedResult parse(Result result) {
        String rawText = ResultParser.getMassagedText(result);
        if (!rawText.startsWith("MATMSG:")) {
            return null;
        }
        String[] rawTo = AbstractDoCoMoResultParser.matchDoCoMoPrefixedField("TO:", rawText, true);
        if (rawTo == null) {
            return null;
        }
        String to = rawTo[0];
        if (!isBasicallyValidEmailAddress(to)) {
            return null;
        }
        String subject = AbstractDoCoMoResultParser.matchSingleDoCoMoPrefixedField("SUB:", rawText, false);
        String body = AbstractDoCoMoResultParser.matchSingleDoCoMoPrefixedField("BODY:", rawText, false);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mailto:");
        stringBuilder.append(to);
        return new EmailAddressParsedResult(to, subject, body, stringBuilder.toString());
    }

    static boolean isBasicallyValidEmailAddress(String email) {
        return email != null && ATEXT_ALPHANUMERIC.matcher(email).matches() && email.indexOf(64) >= 0;
    }
}
