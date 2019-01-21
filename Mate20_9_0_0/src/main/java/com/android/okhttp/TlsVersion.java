package com.android.okhttp;

public enum TlsVersion {
    TLS_1_2("TLSv1.2"),
    TLS_1_1("TLSv1.1"),
    TLS_1_0("TLSv1"),
    SSL_3_0("SSLv3");
    
    final String javaName;

    private TlsVersion(String javaName) {
        this.javaName = javaName;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static TlsVersion forJavaName(String javaName) {
        Object obj;
        switch (javaName.hashCode()) {
            case -503070503:
                if (javaName.equals("TLSv1.1")) {
                    obj = 1;
                    break;
                }
            case -503070502:
                if (javaName.equals("TLSv1.2")) {
                    obj = null;
                    break;
                }
            case 79201641:
                if (javaName.equals("SSLv3")) {
                    obj = 3;
                    break;
                }
            case 79923350:
                if (javaName.equals("TLSv1")) {
                    obj = 2;
                    break;
                }
            default:
                obj = -1;
                break;
        }
        switch (obj) {
            case null:
                return TLS_1_2;
            case 1:
                return TLS_1_1;
            case 2:
                return TLS_1_0;
            case 3:
                return SSL_3_0;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected TLS version: ");
                stringBuilder.append(javaName);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public String javaName() {
        return this.javaName;
    }
}
