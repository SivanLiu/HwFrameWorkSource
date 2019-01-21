package com.huawei.okhttp3;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public interface Dns {
    public static final Dns SYSTEM = new Dns() {
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            if (hostname != null) {
                try {
                    return Arrays.asList(InetAddress.getAllByName(hostname));
                } catch (NullPointerException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Broken system behaviour for dns lookup of ");
                    stringBuilder.append(hostname);
                    UnknownHostException unknownHostException = new UnknownHostException(stringBuilder.toString());
                    unknownHostException.initCause(e);
                    throw unknownHostException;
                }
            }
            throw new UnknownHostException("hostname == null");
        }
    };

    List<InetAddress> lookup(String str) throws UnknownHostException;
}
