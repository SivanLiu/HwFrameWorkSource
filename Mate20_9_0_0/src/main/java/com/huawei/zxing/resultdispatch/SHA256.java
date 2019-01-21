package com.huawei.zxing.resultdispatch;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256 {
    public static String Encrypt(String strSrc, String encName) {
        MessageDigest md;
        byte[] bt = strSrc.getBytes();
        if (encName != null) {
            try {
                if (encName.equals("")) {
                }
                md = MessageDigest.getInstance(encName);
                md.update(bt);
                return bytes2Hex(md.digest());
            } catch (NoSuchAlgorithmException e) {
                return null;
            }
        }
        encName = "SHA-256";
        md = MessageDigest.getInstance(encName);
        md.update(bt);
        return bytes2Hex(md.digest());
    }

    public static String bytes2Hex(byte[] bts) {
        String des = "";
        for (byte b : bts) {
            StringBuilder stringBuilder;
            String tmp = Integer.toHexString(b & 255);
            if (tmp.length() == 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(des);
                stringBuilder.append("0");
                des = stringBuilder.toString();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(des);
            stringBuilder.append(tmp);
            des = stringBuilder.toString();
        }
        return des;
    }
}
