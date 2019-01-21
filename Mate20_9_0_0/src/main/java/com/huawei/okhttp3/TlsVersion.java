package com.huawei.okhttp3;

public enum TlsVersion {
    TLS_1_3("TLSv1.3"),
    TLS_1_2("TLSv1.2"),
    TLS_1_1("TLSv1.1"),
    TLS_1_0("TLSv1"),
    SSL_3_0("SSLv3");
    
    final String javaName;

    private TlsVersion(String javaName) {
        this.javaName = javaName;
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0048  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x006b  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0068  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0065  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0062  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x005f  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0048  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x006b  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0068  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0065  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0062  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x005f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static TlsVersion forJavaName(String javaName) {
        Object obj;
        int hashCode = javaName.hashCode();
        if (hashCode == 79201641) {
            if (javaName.equals("SSLv3")) {
                obj = 4;
                switch (obj) {
                    case null:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode != 79923350) {
            switch (hashCode) {
                case -503070503:
                    if (javaName.equals("TLSv1.1")) {
                        obj = 2;
                        break;
                    }
                case -503070502:
                    if (javaName.equals("TLSv1.2")) {
                        obj = 1;
                        break;
                    }
                case -503070501:
                    if (javaName.equals("TLSv1.3")) {
                        obj = null;
                        break;
                    }
            }
        } else if (javaName.equals("TLSv1")) {
            obj = 3;
            switch (obj) {
                case null:
                    return TLS_1_3;
                case 1:
                    return TLS_1_2;
                case 2:
                    return TLS_1_1;
                case 3:
                    return TLS_1_0;
                case 4:
                    return SSL_3_0;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected TLS version: ");
                    stringBuilder.append(javaName);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        obj = -1;
        switch (obj) {
            case null:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            default:
                break;
        }
    }

    public String javaName() {
        return this.javaName;
    }
}
