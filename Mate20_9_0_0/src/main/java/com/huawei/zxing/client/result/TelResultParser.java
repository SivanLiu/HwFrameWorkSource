package com.huawei.zxing.client.result;

import com.huawei.zxing.Result;

public final class TelResultParser extends ResultParser {
    public TelParsedResult parse(Result result) {
        String rawText = ResultParser.getMassagedText(result);
        if (!rawText.startsWith("tel:") && !rawText.startsWith("TEL:")) {
            return null;
        }
        String telURI;
        if (rawText.startsWith("TEL:")) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("tel:");
            stringBuilder.append(rawText.substring(4));
            telURI = stringBuilder.toString();
        } else {
            telURI = rawText;
        }
        int queryStart = rawText.indexOf(63, 4);
        return new TelParsedResult(queryStart < 0 ? rawText.substring(4) : rawText.substring(4, queryStart), telURI, null);
    }
}
