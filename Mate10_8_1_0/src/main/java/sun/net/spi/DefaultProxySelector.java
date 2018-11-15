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
    static final String[][] props;

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

    static {
        r0 = new String[5][];
        r0[0] = new String[]{"http", "http.proxy", "proxy", "socksProxy"};
        r0[1] = new String[]{"https", "https.proxy", "proxy", "socksProxy"};
        r0[2] = new String[]{"ftp", "ftp.proxy", "ftpProxy", "proxy", "socksProxy"};
        r0[3] = new String[]{"gopher", "gopherProxy", "socksProxy"};
        r0[4] = new String[]{"socket", "socksProxy"};
        props = r0;
    }

    public List<Proxy> select(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("URI can't be null.");
        }
        final String protocol = uri.getScheme();
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
            throw new IllegalArgumentException("protocol = " + protocol + " host = " + host);
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
        String proto = protocol;
        final NonProxyInfo nprop = pinfo;
        final String urlhost = host.toLowerCase();
        proxyl.add((Proxy) AccessController.doPrivileged(new PrivilegedAction<Proxy>() {
            public Proxy run() {
                String phost = null;
                int i = 0;
                while (i < DefaultProxySelector.props.length) {
                    if (DefaultProxySelector.props[i][0].equalsIgnoreCase(protocol)) {
                        int j = 1;
                        while (j < DefaultProxySelector.props[i].length) {
                            phost = NetProperties.get(DefaultProxySelector.props[i][j] + "Host");
                            if (phost != null && phost.length() != 0) {
                                break;
                            }
                            j++;
                        }
                        if (phost == null || phost.length() == 0) {
                            return Proxy.NO_PROXY;
                        }
                        if (nprop != null) {
                            String nphosts = NetProperties.get(nprop.property);
                            synchronized (nprop) {
                                if (nphosts == null) {
                                    if (nprop.defaultVal != null) {
                                        nphosts = nprop.defaultVal;
                                    } else {
                                        nprop.hostsSource = null;
                                        nprop.pattern = null;
                                    }
                                } else if (nphosts.length() != 0) {
                                    nphosts = nphosts + "|localhost|127.*|[::1]|0.0.0.0|[::0]";
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
                        int pport = NetProperties.getInteger(DefaultProxySelector.props[i][j] + "Port", 0).intValue();
                        if (pport == 0 && j < DefaultProxySelector.props[i].length - 1) {
                            for (int k = 1; k < DefaultProxySelector.props[i].length - 1; k++) {
                                if (k != j && pport == 0) {
                                    pport = NetProperties.getInteger(DefaultProxySelector.props[i][k] + "Port", 0).intValue();
                                }
                            }
                        }
                        if (pport == 0) {
                            if (j == DefaultProxySelector.props[i].length - 1) {
                                pport = DefaultProxySelector.this.defaultPort("socket");
                            } else {
                                pport = DefaultProxySelector.this.defaultPort(protocol);
                            }
                        }
                        InetSocketAddress saddr = InetSocketAddress.createUnresolved(phost, pport);
                        if (j == DefaultProxySelector.props[i].length - 1) {
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
            return ".*" + Pattern.quote(disjunct.substring(1));
        }
        if (disjunct.endsWith("*")) {
            return Pattern.quote(disjunct.substring(0, disjunct.length() - 1)) + ".*";
        }
        return Pattern.quote(disjunct);
    }
}
