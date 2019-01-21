package com.huawei.okhttp3;

import com.huawei.okio.ByteString;
import java.io.UnsupportedEncodingException;

public final class Credentials {
    private Credentials() {
    }

    public static String basic(String userName, String password) {
        try {
            String usernameAndPassword = new StringBuilder();
            usernameAndPassword.append(userName);
            usernameAndPassword.append(":");
            usernameAndPassword.append(password);
            String encoded = ByteString.of(usernameAndPassword.toString().getBytes("ISO-8859-1")).base64();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Basic ");
            stringBuilder.append(encoded);
            return stringBuilder.toString();
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }
    }
}
