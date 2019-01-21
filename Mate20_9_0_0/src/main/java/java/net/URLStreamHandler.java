package java.net;

import java.io.IOException;
import java.util.Objects;
import sun.net.util.IPAddressUtil;

public abstract class URLStreamHandler {
    protected abstract URLConnection openConnection(URL url) throws IOException;

    protected URLConnection openConnection(URL u, Proxy p) throws IOException {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    /* JADX WARNING: Removed duplicated region for block: B:94:0x022f  */
    /* JADX WARNING: Removed duplicated region for block: B:93:0x022a  */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x0233  */
    /* JADX WARNING: Removed duplicated region for block: B:115:0x029d  */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x02c3 A:{SYNTHETIC, EDGE_INSN: B:156:0x02c3->B:119:0x02c3 ?: BREAK  , EDGE_INSN: B:156:0x02c3->B:119:0x02c3 ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x02a8 A:{LOOP_END, LOOP:1: B:116:0x029f->B:118:0x02a8} */
    /* JADX WARNING: Removed duplicated region for block: B:158:0x030b A:{SYNTHETIC, EDGE_INSN: B:158:0x030b->B:131:0x030b ?: BREAK  , EDGE_INSN: B:158:0x030b->B:131:0x030b ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:122:0x02cd  */
    /* JADX WARNING: Removed duplicated region for block: B:133:0x0313  */
    /* JADX WARNING: Removed duplicated region for block: B:144:0x0357  */
    /* JADX WARNING: Removed duplicated region for block: B:143:0x034b  */
    /* JADX WARNING: Removed duplicated region for block: B:148:0x036d  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x0361  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void parseURL(URL u, String spec, int start, int limit) {
        String authority;
        String userInfo;
        int port;
        String path;
        int start2;
        String userInfo2;
        String userInfo3;
        String authority2;
        int start3;
        String host;
        StringBuilder stringBuilder;
        String path2;
        String spec2 = spec;
        int i = start;
        int limit2 = limit;
        String protocol = u.getProtocol();
        String authority3 = u.getAuthority();
        String userInfo4 = u.getUserInfo();
        String host2 = u.getHost();
        int port2 = u.getPort();
        String path3 = u.getPath();
        String query = u.getQuery();
        String ref = u.getRef();
        boolean isRelPath = false;
        String spec3 = null;
        boolean queryOnly = false;
        if (i < limit2) {
            int queryStart = spec2.indexOf(63);
            spec3 = queryStart == i ? true : null;
            if (queryStart != -1 && queryStart < limit2) {
                query = spec2.substring(queryStart + 1, limit2);
                if (limit2 > queryStart) {
                    limit2 = queryStart;
                }
                spec2 = spec2.substring(0, queryStart);
                queryOnly = true;
            }
        }
        boolean querySet = queryOnly;
        queryOnly = spec3;
        spec3 = spec2;
        int i2 = 0;
        if (false || i > limit2 - 2) {
            authority = authority3;
            userInfo = userInfo4;
            port = port2;
            path = path3;
        } else {
            authority = authority3;
            if (spec3.charAt(i) == '/' && spec3.charAt(i + 1) == '/') {
                start2 = i + 2;
                i = start2;
                while (i < limit2) {
                    authority3 = spec3.charAt(i);
                    userInfo = userInfo4;
                    if (authority3 == 35 || authority3 == 47 || authority3 == 63 || authority3 == 92) {
                        break;
                    }
                    i++;
                    userInfo4 = userInfo;
                }
                authority3 = spec3.substring(start2, i);
                userInfo4 = authority3;
                start2 = userInfo4.indexOf(64);
                if (start2 == -1) {
                    host2 = null;
                } else if (start2 != userInfo4.lastIndexOf(64)) {
                    host2 = null;
                    authority3 = null;
                } else {
                    userInfo2 = userInfo4.substring(0, start2);
                    authority3 = userInfo4.substring(start2 + 1);
                    host2 = userInfo2;
                }
                if (authority3 != null) {
                    if (authority3.length() > 0) {
                        userInfo3 = host2;
                        if (authority3.charAt(0) == 91) {
                            start2 = authority3.indexOf(93);
                            host2 = start2;
                            StringBuilder stringBuilder2;
                            if (start2 > 2) {
                                spec2 = authority3;
                                authority3 = spec2.substring(null, host2 + 1);
                                if (IPAddressUtil.isIPv6LiteralAddress(authority3.substring(1, host2))) {
                                    int port3 = -1;
                                    if (spec2.length() > host2 + 1) {
                                        if (spec2.charAt(host2 + 1) == ':') {
                                            host2++;
                                            if (spec2.length() > host2 + 1) {
                                                port2 = Integer.parseInt(spec2.substring(host2 + 1));
                                                spec2 = host2;
                                                host2 = authority3;
                                                authority2 = userInfo4;
                                            }
                                        } else {
                                            StringBuilder stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("Invalid authority field: ");
                                            stringBuilder3.append(userInfo4);
                                            throw new IllegalArgumentException(stringBuilder3.toString());
                                        }
                                    }
                                    spec2 = host2;
                                    port2 = port3;
                                    host2 = authority3;
                                    authority2 = userInfo4;
                                } else {
                                    String str = spec2;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Invalid host: ");
                                    stringBuilder2.append(authority3);
                                    throw new IllegalArgumentException(stringBuilder2.toString());
                                }
                            }
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Invalid authority field: ");
                            stringBuilder2.append(userInfo4);
                            throw new IllegalArgumentException(stringBuilder2.toString());
                        }
                        path = path3;
                    } else {
                        userInfo3 = host2;
                        port = port2;
                        path = path3;
                    }
                    start2 = authority3.indexOf(58);
                    port2 = -1;
                    if (start2 >= 0) {
                        if (authority3.length() > start2 + 1) {
                            char firstPortChar = authority3.charAt(start2 + 1);
                            if (firstPortChar < '0' || firstPortChar > '9') {
                                authority2 = userInfo4;
                                userInfo4 = new StringBuilder();
                                userInfo4.append("invalid port: ");
                                userInfo4.append(authority3.substring(start2 + 1));
                                throw new IllegalArgumentException(userInfo4.toString());
                            }
                            port2 = Integer.parseInt(authority3.substring(start2 + 1));
                            authority2 = userInfo4;
                        } else {
                            authority2 = userInfo4;
                        }
                        host2 = authority3.substring(null, start2);
                    } else {
                        authority2 = userInfo4;
                        host2 = authority3;
                    }
                } else {
                    authority2 = userInfo4;
                    userInfo3 = host2;
                    port = port2;
                    path = path3;
                    host2 = "";
                }
                if (port2 >= -1) {
                    authority3 = i;
                    path3 = null;
                    if (querySet) {
                        port = port2;
                        userInfo2 = query;
                        start3 = authority3;
                    } else {
                        userInfo2 = 0;
                        start3 = authority3;
                        port = port2;
                    }
                    if (host2 != null) {
                        host = "";
                    } else {
                        host = host2;
                    }
                    if (start3 < limit2) {
                        if (spec3.charAt(start3) == '/' || spec3.charAt(start3) == '\\') {
                            path3 = spec3.substring(start3, limit2);
                        } else if (path3 == null || path3.length() <= 0) {
                            spec2 = authority2 != null ? "/" : "";
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append(spec2);
                            stringBuilder4.append(spec3.substring(start3, limit2));
                            path3 = stringBuilder4.toString();
                        } else {
                            isRelPath = true;
                            int ind = path3.lastIndexOf(47);
                            spec2 = "";
                            if (ind == -1 && authority2 != null) {
                                spec2 = "/";
                            }
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append(path3.substring(0, ind + 1));
                            stringBuilder5.append(spec2);
                            stringBuilder5.append(spec3.substring(start3, limit2));
                            path3 = stringBuilder5.toString();
                        }
                    }
                    if (path3 == null) {
                        path3 = "";
                    }
                    while (true) {
                        start2 = path3.indexOf("/./");
                        i = start2;
                        if (start2 >= 0) {
                            break;
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(path3.substring(0, i));
                        stringBuilder.append(path3.substring(i + 2));
                        path3 = stringBuilder.toString();
                    }
                    start2 = 0;
                    while (true) {
                        i = path3.indexOf("/../", start2);
                        start2 = i;
                        if (i >= 0) {
                            break;
                        } else if (start2 == 0) {
                            path3 = path3.substring(start2 + 3);
                            start2 = 0;
                        } else {
                            if (start2 > 0) {
                                i = path3.lastIndexOf(47, start2 - 1);
                                limit2 = i;
                                if (i >= 0 && path3.indexOf("/../", limit2) != 0) {
                                    StringBuilder stringBuilder6 = new StringBuilder();
                                    stringBuilder6.append(path3.substring(0, limit2));
                                    stringBuilder6.append(path3.substring(start2 + 3));
                                    path3 = stringBuilder6.toString();
                                    start2 = 0;
                                }
                            }
                            start2 += 3;
                        }
                    }
                    while (path3.endsWith("/..")) {
                        start2 = path3.indexOf("/..");
                        i = path3.lastIndexOf(47, start2 - 1);
                        limit2 = i;
                        if (i < 0) {
                            break;
                        }
                        path3 = path3.substring(0, limit2 + 1);
                    }
                    if (path3.startsWith("./") && path3.length() > 2) {
                        path3 = path3.substring(2);
                    }
                    if (path3.endsWith("/.")) {
                        i = 1;
                        limit2 = 0;
                    } else {
                        i = 1;
                        limit2 = 0;
                        path3 = path3.substring(0, path3.length() - 1);
                    }
                    if (path3.endsWith("?")) {
                        path2 = path3;
                    } else {
                        path2 = path3.substring(limit2, path3.length() - i);
                    }
                    setURL(u, protocol, host, port, authority2, userInfo3, path2, userInfo2, ref);
                }
                userInfo4 = new StringBuilder();
                userInfo4.append("Invalid port number :");
                userInfo4.append(port2);
                throw new IllegalArgumentException(userInfo4.toString());
            }
            userInfo = userInfo4;
            port = port2;
            path = path3;
        }
        userInfo2 = query;
        authority2 = authority;
        userInfo3 = userInfo;
        path3 = path;
        start3 = i;
        i = i2;
        if (host2 != null) {
        }
        if (start3 < limit2) {
        }
        if (path3 == null) {
        }
        while (true) {
            start2 = path3.indexOf("/./");
            i = start2;
            if (start2 >= 0) {
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(path3.substring(0, i));
            stringBuilder.append(path3.substring(i + 2));
            path3 = stringBuilder.toString();
        }
        start2 = 0;
        while (true) {
            i = path3.indexOf("/../", start2);
            start2 = i;
            if (i >= 0) {
            }
        }
        while (path3.endsWith("/..")) {
        }
        path3 = path3.substring(2);
        if (path3.endsWith("/.")) {
        }
        if (path3.endsWith("?")) {
        }
        setURL(u, protocol, host, port, authority2, userInfo3, path2, userInfo2, ref);
    }

    protected int getDefaultPort() {
        return -1;
    }

    protected boolean equals(URL u1, URL u2) {
        return Objects.equals(u1.getRef(), u2.getRef()) && Objects.equals(u1.getQuery(), u2.getQuery()) && sameFile(u1, u2);
    }

    protected int hashCode(URL u) {
        return Objects.hash(u.getRef(), u.getQuery(), u.getProtocol(), u.getFile(), u.getHost(), Integer.valueOf(u.getPort()));
    }

    protected boolean sameFile(URL u1, URL u2) {
        if (u1.getProtocol() != u2.getProtocol() && (u1.getProtocol() == null || !u1.getProtocol().equalsIgnoreCase(u2.getProtocol()))) {
            return false;
        }
        if (u1.getFile() != u2.getFile() && (u1.getFile() == null || !u1.getFile().equals(u2.getFile()))) {
            return false;
        }
        if ((u1.getPort() != -1 ? u1.getPort() : u1.handler.getDefaultPort()) == (u2.getPort() != -1 ? u2.getPort() : u2.handler.getDefaultPort()) && hostsEqual(u1, u2)) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:25:0x002b, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected synchronized InetAddress getHostAddress(URL u) {
        if (u.hostAddress != null) {
            return u.hostAddress;
        }
        String host = u.getHost();
        if (host != null && !host.equals("")) {
            try {
                u.hostAddress = InetAddress.getByName(host);
                return u.hostAddress;
            } catch (UnknownHostException e) {
                return null;
            } catch (SecurityException e2) {
                return null;
            }
        }
    }

    protected boolean hostsEqual(URL u1, URL u2) {
        if (u1.getHost() != null && u2.getHost() != null) {
            return u1.getHost().equalsIgnoreCase(u2.getHost());
        }
        boolean z = u1.getHost() == null && u2.getHost() == null;
        return z;
    }

    protected String toExternalForm(URL u) {
        int len = u.getProtocol().length() + 1;
        if (u.getAuthority() != null && u.getAuthority().length() > 0) {
            len += 2 + u.getAuthority().length();
        }
        if (u.getPath() != null) {
            len += u.getPath().length();
        }
        if (u.getQuery() != null) {
            len += u.getQuery().length() + 1;
        }
        if (u.getRef() != null) {
            len += 1 + u.getRef().length();
        }
        StringBuilder result = new StringBuilder(len);
        result.append(u.getProtocol());
        result.append(":");
        if (u.getAuthority() != null) {
            result.append("//");
            result.append(u.getAuthority());
        }
        String fileAndQuery = u.getFile();
        if (fileAndQuery != null) {
            result.append(fileAndQuery);
        }
        if (u.getRef() != null) {
            result.append("#");
            result.append(u.getRef());
        }
        return result.toString();
    }

    protected void setURL(URL u, String protocol, String host, int port, String authority, String userInfo, String path, String query, String ref) {
        URL url = u;
        if (this == url.handler) {
            url.set(url.getProtocol(), host, port, authority, userInfo, path, query, ref);
            return;
        }
        throw new SecurityException("handler for url different from this handler");
    }

    @Deprecated
    protected void setURL(URL u, String protocol, String host, int port, String file, String ref) {
        String str = host;
        int i = port;
        String str2 = file;
        String authority = null;
        String userInfo = null;
        if (!(str == null || host.length() == 0)) {
            String str3;
            if (i == -1) {
                str3 = str;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(":");
                stringBuilder.append(i);
                str3 = stringBuilder.toString();
            }
            authority = str3;
            int at = str.lastIndexOf(64);
            if (at != -1) {
                userInfo = str.substring(0, at);
                str = str.substring(at + 1);
            }
        }
        String host2 = str;
        String authority2 = authority;
        String userInfo2 = userInfo;
        str = null;
        authority = null;
        if (str2 != null) {
            int q = str2.lastIndexOf(63);
            if (q != -1) {
                authority = str2.substring(q + 1);
                str = str2.substring(0, q);
            } else {
                str = str2;
            }
        }
        setURL(u, protocol, host2, i, authority2, userInfo2, str, authority, ref);
    }
}
