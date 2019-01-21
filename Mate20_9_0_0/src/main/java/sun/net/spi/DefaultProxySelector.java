package sun.net.spi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import sun.net.NetProperties;
import sun.net.SocksProxy;

public class DefaultProxySelector extends ProxySelector {
    private static final String SOCKS_PROXY_VERSION = "socksProxyVersion";
    private static boolean hasSystemProxies = false;
    static final String[][] props = new String[][]{new String[]{"http", "http.proxy", "proxy", "socksProxy"}, new String[]{"https", "https.proxy", "proxy", "socksProxy"}, new String[]{"ftp", "ftp.proxy", "ftpProxy", "proxy", "socksProxy"}, new String[]{"gopher", "gopherProxy", "socksProxy"}, new String[]{"socket", "socksProxy"}};

    static class NonProxyInfo {
        static final String defStringVal = "localhost|127.*|[::1]|0.0.0.0|[::0]";
        static NonProxyInfo ftpNonProxyInfo = new NonProxyInfo("ftp.nonProxyHosts", null, null, defStringVal);
        static NonProxyInfo httpNonProxyInfo = new NonProxyInfo("http.nonProxyHosts", null, null, defStringVal);
        static NonProxyInfo httpsNonProxyInfo = new NonProxyInfo("https.nonProxyHosts", null, null, defStringVal);
        static NonProxyInfo socksNonProxyInfo = new NonProxyInfo("socksNonProxyHosts", null, null, defStringVal);
        final String defaultVal;
        String hostsSource;
        Pattern pattern;
        final String property;

        NonProxyInfo(String p, String s, Pattern pattern, String d) {
            this.property = p;
            this.hostsSource = s;
            this.pattern = pattern;
            this.defaultVal = d;
        }
    }

    public List<Proxy> select(URI uri) {
        if (uri != null) {
            String protocol = uri.getScheme();
            String host = uri.getHost();
            if (host == null) {
                String auth = uri.getAuthority();
                if (auth != null) {
                    int i = auth.indexOf(64);
                    if (i >= 0) {
                        auth = auth.substring(i + 1);
                    }
                    i = auth.lastIndexOf(58);
                    if (i >= 0) {
                        auth = auth.substring(0, i);
                    }
                    host = auth;
                }
            }
            if (protocol == null || host == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("protocol = ");
                stringBuilder.append(protocol);
                stringBuilder.append(" host = ");
                stringBuilder.append(host);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            List<Proxy> proxyl = new ArrayList(1);
            NonProxyInfo pinfo = null;
            if ("http".equalsIgnoreCase(protocol)) {
                pinfo = NonProxyInfo.httpNonProxyInfo;
            } else if ("https".equalsIgnoreCase(protocol)) {
                pinfo = NonProxyInfo.httpsNonProxyInfo;
            } else if ("ftp".equalsIgnoreCase(protocol)) {
                pinfo = NonProxyInfo.ftpNonProxyInfo;
            } else if ("socket".equalsIgnoreCase(protocol)) {
                pinfo = NonProxyInfo.socksNonProxyInfo;
            }
            final String proto = protocol;
            final NonProxyInfo nprop = pinfo;
            final String urlhost = host.toLowerCase();
            proxyl.add((Proxy) AccessController.doPrivileged(new PrivilegedAction<Proxy>() {
                public Proxy run() {
                    int i = 0;
                    while (i < DefaultProxySelector.props.length) {
                        if (DefaultProxySelector.props[i][0].equalsIgnoreCase(proto)) {
                            StringBuilder stringBuilder;
                            String phost = null;
                            String phost2 = 1;
                            while (phost2 < DefaultProxySelector.props[i].length) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(DefaultProxySelector.props[i][phost2]);
                                stringBuilder.append("Host");
                                phost = NetProperties.get(stringBuilder.toString());
                                if (phost != null && phost.length() != 0) {
                                    break;
                                }
                                phost2++;
                            }
                            if (phost == null || phost.length() == 0) {
                                return Proxy.NO_PROXY;
                            }
                            StringBuilder stringBuilder2;
                            if (nprop != null) {
                                String nphosts = NetProperties.get(nprop.property);
                                synchronized (nprop) {
                                    if (nphosts == null) {
                                        try {
                                            if (nprop.defaultVal != null) {
                                                nphosts = nprop.defaultVal;
                                            } else {
                                                nprop.hostsSource = null;
                                                nprop.pattern = null;
                                            }
                                        } finally {
                                        }
                                    } else if (nphosts.length() != 0) {
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append(nphosts);
                                        stringBuilder2.append("|localhost|127.*|[::1]|0.0.0.0|[::0]");
                                        nphosts = stringBuilder2.toString();
                                    }
                                    if (!(nphosts == null || nphosts.equals(nprop.hostsSource))) {
                                        nprop.pattern = DefaultProxySelector.toPattern(nphosts);
                                        nprop.hostsSource = nphosts;
                                    }
                                    if (DefaultProxySelector.shouldNotUseProxyFor(nprop.pattern, urlhost)) {
                                        Proxy proxy = Proxy.NO_PROXY;
                                        return proxy;
                                    }
                                }
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(DefaultProxySelector.props[i][phost2]);
                            stringBuilder.append("Port");
                            int pport = NetProperties.getInteger(stringBuilder.toString(), 0).intValue();
                            if (pport == 0 && phost2 < DefaultProxySelector.props[i].length - 1) {
                                int pport2 = pport;
                                for (pport = 1; pport < DefaultProxySelector.props[i].length - 1; pport++) {
                                    if (pport != phost2 && pport2 == 0) {
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append(DefaultProxySelector.props[i][pport]);
                                        stringBuilder2.append("Port");
                                        pport2 = NetProperties.getInteger(stringBuilder2.toString(), 0).intValue();
                                    }
                                }
                                pport = pport2;
                            }
                            if (pport == 0) {
                                if (phost2 == DefaultProxySelector.props[i].length - 1) {
                                    pport = DefaultProxySelector.this.defaultPort("socket");
                                } else {
                                    pport = DefaultProxySelector.this.defaultPort(proto);
                                }
                            }
                            InetSocketAddress saddr = InetSocketAddress.createUnresolved(phost, pport);
                            if (phost2 == DefaultProxySelector.props[i].length - 1) {
                                return SocksProxy.create(saddr, NetProperties.getInteger(DefaultProxySelector.SOCKS_PROXY_VERSION, 5).intValue());
                            }
                            return new Proxy(Type.HTTP, saddr);
                        }
                        i++;
                    }
                    return Proxy.NO_PROXY;
                }
            }));
            return proxyl;
        }
        throw new IllegalArgumentException("URI can't be null.");
    }

    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        if (uri == null || sa == null || ioe == null) {
            throw new IllegalArgumentException("Arguments can't be null.");
        }
    }

    private int defaultPort(String protocol) {
        if ("http".equalsIgnoreCase(protocol)) {
            return 80;
        }
        if ("https".equalsIgnoreCase(protocol)) {
            return 443;
        }
        if ("ftp".equalsIgnoreCase(protocol)) {
            return 80;
        }
        if ("socket".equalsIgnoreCase(protocol)) {
            return SocksConsts.DEFAULT_PORT;
        }
        if ("gopher".equalsIgnoreCase(protocol)) {
            return 80;
        }
        return -1;
    }

    static boolean shouldNotUseProxyFor(Pattern pattern, String urlhost) {
        if (pattern == null || urlhost.isEmpty()) {
            return false;
        }
        return pattern.matcher(urlhost).matches();
    }

    static Pattern toPattern(String mask) {
        boolean disjunctionEmpty = true;
        StringJoiner joiner = new StringJoiner("|");
        for (String disjunct : mask.split("\\|")) {
            if (!disjunct.isEmpty()) {
                disjunctionEmpty = false;
                joiner.add(disjunctToRegex(disjunct.toLowerCase()));
            }
        }
        return disjunctionEmpty ? null : Pattern.compile(joiner.toString());
    }

    static String disjunctToRegex(String disjunct) {
        if (disjunct.startsWith("*")) {
            String regex = new StringBuilder();
            regex.append(".*");
            regex.append(Pattern.quote(disjunct.substring(1)));
            return regex.toString();
        } else if (!disjunct.endsWith("*")) {
            return Pattern.quote(disjunct);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Pattern.quote(disjunct.substring(0, disjunct.length() - 1)));
            stringBuilder.append(".*");
            return stringBuilder.toString();
        }
    }
}
