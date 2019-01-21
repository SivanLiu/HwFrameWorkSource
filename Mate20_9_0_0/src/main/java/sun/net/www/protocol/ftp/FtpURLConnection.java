package sun.net.www.protocol.ftp;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketPermission;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.StringTokenizer;
import libcore.net.NetworkSecurityPolicy;
import sun.net.ProgressMonitor;
import sun.net.ProgressSource;
import sun.net.ftp.FtpClient;
import sun.net.ftp.FtpLoginException;
import sun.net.ftp.FtpProtocolException;
import sun.net.www.MessageHeader;
import sun.net.www.MeteredStream;
import sun.net.www.ParseUtil;
import sun.net.www.URLConnection;
import sun.security.action.GetPropertyAction;
import sun.security.util.SecurityConstants;

public class FtpURLConnection extends URLConnection {
    static final int ASCII = 1;
    static final int BIN = 2;
    static final int DIR = 3;
    static final int NONE = 0;
    private int connectTimeout;
    String filename;
    FtpClient ftp;
    String fullpath;
    String host;
    private Proxy instProxy;
    InputStream is;
    OutputStream os;
    String password;
    String pathname;
    Permission permission;
    int port;
    private int readTimeout;
    int type;
    String user;

    protected class FtpInputStream extends FilterInputStream {
        FtpClient ftp;

        FtpInputStream(FtpClient cl, InputStream fd) {
            super(new BufferedInputStream(fd));
            this.ftp = cl;
        }

        public void close() throws IOException {
            super.close();
            if (this.ftp != null) {
                this.ftp.close();
            }
        }
    }

    protected class FtpOutputStream extends FilterOutputStream {
        FtpClient ftp;

        FtpOutputStream(FtpClient cl, OutputStream fd) {
            super(fd);
            this.ftp = cl;
        }

        public void close() throws IOException {
            super.close();
            if (this.ftp != null) {
                this.ftp.close();
            }
        }
    }

    public FtpURLConnection(URL url) throws IOException {
        this(url, null);
    }

    FtpURLConnection(URL url, Proxy p) throws IOException {
        super(url);
        this.is = null;
        this.os = null;
        this.ftp = null;
        this.type = 0;
        this.connectTimeout = -1;
        this.readTimeout = -1;
        this.instProxy = p;
        this.host = url.getHost();
        this.port = url.getPort();
        String userInfo = url.getUserInfo();
        if (!NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted()) {
            String stringBuilder;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Cleartext traffic not permitted: ");
            stringBuilder2.append(url.getProtocol());
            stringBuilder2.append("://");
            stringBuilder2.append(this.host);
            if (url.getPort() >= 0) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(":");
                stringBuilder3.append(url.getPort());
                stringBuilder = stringBuilder3.toString();
            } else {
                stringBuilder = "";
            }
            stringBuilder2.append(stringBuilder);
            throw new IOException(stringBuilder2.toString());
        } else if (userInfo != null) {
            int delimiter = userInfo.indexOf(58);
            if (delimiter == -1) {
                this.user = ParseUtil.decode(userInfo);
                this.password = null;
                return;
            }
            int delimiter2 = delimiter + 1;
            this.user = ParseUtil.decode(userInfo.substring(0, delimiter));
            this.password = ParseUtil.decode(userInfo.substring(delimiter2));
        }
    }

    private void setTimeouts() {
        if (this.ftp != null) {
            if (this.connectTimeout >= 0) {
                this.ftp.setConnectTimeout(this.connectTimeout);
            }
            if (this.readTimeout >= 0) {
                this.ftp.setReadTimeout(this.readTimeout);
            }
        }
    }

    public synchronized void connect() throws IOException {
        if (!this.connected) {
            Proxy p = null;
            if (this.instProxy == null) {
                ProxySelector sel = (ProxySelector) AccessController.doPrivileged(new PrivilegedAction<ProxySelector>() {
                    public ProxySelector run() {
                        return ProxySelector.getDefault();
                    }
                });
                if (sel != null) {
                    URI uri = ParseUtil.toURI(this.url);
                    for (Proxy p2 : sel.select(uri)) {
                        if (p2 == null || p2 == Proxy.NO_PROXY) {
                            break;
                        } else if (p2.type() == Type.SOCKS) {
                            break;
                        } else {
                            if (p2.type() == Type.HTTP) {
                                if (p2.address() instanceof InetSocketAddress) {
                                    sel.connectFailed(uri, p2.address(), new IOException("FTP connections over HTTP proxy not supported"));
                                }
                            }
                            sel.connectFailed(uri, p2.address(), new IOException("Wrong proxy type"));
                        }
                    }
                }
            } else {
                p2 = this.instProxy;
            }
            if (this.user == null) {
                this.user = "anonymous";
                String vers = (String) AccessController.doPrivileged(new GetPropertyAction("java.version"));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Java");
                stringBuilder.append(vers);
                stringBuilder.append("@");
                this.password = (String) AccessController.doPrivileged(new GetPropertyAction("ftp.protocol.user", stringBuilder.toString()));
            }
            try {
                this.ftp = FtpClient.create();
                if (p2 != null) {
                    this.ftp.setProxy(p2);
                }
                setTimeouts();
                if (this.port != -1) {
                    this.ftp.connect(new InetSocketAddress(this.host, this.port));
                } else {
                    this.ftp.connect(new InetSocketAddress(this.host, FtpClient.defaultPort()));
                }
                this.ftp.login(this.user, this.password == null ? null : this.password.toCharArray());
                this.connected = true;
            } catch (UnknownHostException e) {
                throw e;
            } catch (FtpProtocolException fe) {
                throw new IOException(fe);
            } catch (FtpProtocolException e2) {
                this.ftp.close();
                throw new FtpLoginException("Invalid username/password");
            }
        }
    }

    private void decodePath(String path) {
        int i = path.indexOf(";type=");
        if (i >= 0) {
            String s1 = path.substring(i + 6, path.length());
            if ("i".equalsIgnoreCase(s1)) {
                this.type = 2;
            }
            if ("a".equalsIgnoreCase(s1)) {
                this.type = 1;
            }
            if ("d".equalsIgnoreCase(s1)) {
                this.type = 3;
            }
            path = path.substring(0, i);
        }
        if (path != null && path.length() > 1 && path.charAt(0) == '/') {
            path = path.substring(1);
        }
        if (path == null || path.length() == 0) {
            path = "./";
        }
        if (path.endsWith("/")) {
            this.pathname = path.substring(0, path.length() - 1);
            this.filename = null;
        } else {
            i = path.lastIndexOf(47);
            if (i > 0) {
                this.filename = path.substring(i + 1, path.length());
                this.filename = ParseUtil.decode(this.filename);
                this.pathname = path.substring(0, i);
            } else {
                this.filename = ParseUtil.decode(path);
                this.pathname = null;
            }
        }
        if (this.pathname != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.pathname);
            stringBuilder.append("/");
            stringBuilder.append(this.filename != null ? this.filename : "");
            this.fullpath = stringBuilder.toString();
            return;
        }
        this.fullpath = this.filename;
    }

    private void cd(String path) throws FtpProtocolException, IOException {
        if (path != null && !path.isEmpty()) {
            if (path.indexOf(47) == -1) {
                this.ftp.changeDirectory(ParseUtil.decode(path));
                return;
            }
            StringTokenizer token = new StringTokenizer(path, "/");
            while (token.hasMoreTokens()) {
                this.ftp.changeDirectory(ParseUtil.decode(token.nextToken()));
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x0097 A:{Catch:{ Exception -> 0x00bd }} */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x00d2 A:{Catch:{ FileNotFoundException -> 0x00ff, FtpProtocolException -> 0x00f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x00c3 A:{Catch:{ FileNotFoundException -> 0x00ff, FtpProtocolException -> 0x00f8 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public InputStream getInputStream() throws IOException {
        if (!this.connected) {
            connect();
        }
        if (this.os != null) {
            throw new IOException("Already opened for output");
        } else if (this.is != null) {
            return this.is;
        } else {
            MessageHeader msgh = new MessageHeader();
            try {
                long l;
                decodePath(this.url.getPath());
                if (this.filename != null) {
                    if (this.type != 3) {
                        if (this.type == 1) {
                            this.ftp.setAsciiType();
                        } else {
                            this.ftp.setBinaryType();
                        }
                        cd(this.pathname);
                        this.is = new FtpInputStream(this.ftp, this.ftp.getFileStream(this.filename));
                        l = this.ftp.getLastTransferSize();
                        msgh.add("content-length", Long.toString(l));
                        if (l > 0) {
                            ProgressSource pi = null;
                            if (ProgressMonitor.getDefault().shouldMeterInput(this.url, "GET")) {
                                pi = new ProgressSource(this.url, "GET", l);
                                pi.beginTracking();
                            }
                            this.is = new MeteredStream(this.is, pi, l);
                        }
                        if (false) {
                            msgh.add("access-type", "file");
                            String ftype = java.net.URLConnection.guessContentTypeFromName(this.fullpath);
                            if (ftype == null && this.is.markSupported()) {
                                ftype = java.net.URLConnection.guessContentTypeFromStream(this.is);
                            }
                            if (ftype != null) {
                                msgh.add("content-type", ftype);
                            }
                        } else {
                            msgh.add("content-type", "text/plain");
                            msgh.add("access-type", "directory");
                        }
                        setProperties(msgh);
                        return this.is;
                    }
                }
                this.ftp.setAsciiType();
                cd(this.pathname);
                if (this.filename == null) {
                    this.is = new FtpInputStream(this.ftp, this.ftp.list(null));
                } else {
                    this.is = new FtpInputStream(this.ftp, this.ftp.nameList(this.filename));
                }
                try {
                    l = this.ftp.getLastTransferSize();
                    msgh.add("content-length", Long.toString(l));
                    if (l > 0) {
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (false) {
                }
            } catch (FileNotFoundException e2) {
                try {
                    cd(this.fullpath);
                    this.ftp.setAsciiType();
                    this.is = new FtpInputStream(this.ftp, this.ftp.list(null));
                    msgh.add("content-type", "text/plain");
                    msgh.add("access-type", "directory");
                } catch (IOException e3) {
                    throw new FileNotFoundException(this.fullpath);
                } catch (FtpProtocolException e4) {
                    throw new FileNotFoundException(this.fullpath);
                }
            } catch (FtpProtocolException ftpe) {
                throw new IOException(ftpe);
            }
            setProperties(msgh);
            return this.is;
        }
    }

    public OutputStream getOutputStream() throws IOException {
        if (!this.connected) {
            connect();
        }
        if (this.is != null) {
            throw new IOException("Already opened for input");
        } else if (this.os != null) {
            return this.os;
        } else {
            decodePath(this.url.getPath());
            if (this.filename == null || this.filename.length() == 0) {
                throw new IOException("illegal filename for a PUT");
            }
            try {
                if (this.pathname != null) {
                    cd(this.pathname);
                }
                if (this.type == 1) {
                    this.ftp.setAsciiType();
                } else {
                    this.ftp.setBinaryType();
                }
                this.os = new FtpOutputStream(this.ftp, this.ftp.putFileStream(this.filename, false));
                return this.os;
            } catch (FtpProtocolException e) {
                throw new IOException(e);
            }
        }
    }

    String guessContentTypeFromFilename(String fname) {
        return java.net.URLConnection.guessContentTypeFromName(fname);
    }

    public Permission getPermission() {
        if (this.permission == null) {
            int urlport = this.url.getPort();
            urlport = urlport < 0 ? FtpClient.defaultPort() : urlport;
            String urlhost = new StringBuilder();
            urlhost.append(this.host);
            urlhost.append(":");
            urlhost.append(urlport);
            this.permission = new SocketPermission(urlhost.toString(), SecurityConstants.SOCKET_CONNECT_ACTION);
        }
        return this.permission;
    }

    public void setRequestProperty(String key, String value) {
        super.setRequestProperty(key, value);
        if (!"type".equals(key)) {
            return;
        }
        if ("i".equalsIgnoreCase(value)) {
            this.type = 2;
        } else if ("a".equalsIgnoreCase(value)) {
            this.type = 1;
        } else if ("d".equalsIgnoreCase(value)) {
            this.type = 3;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Value of '");
            stringBuilder.append(key);
            stringBuilder.append("' request property was '");
            stringBuilder.append(value);
            stringBuilder.append("' when it must be either 'i', 'a' or 'd'");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public String getRequestProperty(String key) {
        String value = super.getRequestProperty(key);
        if (value != null || !"type".equals(key)) {
            return value;
        }
        String str = this.type == 1 ? "a" : this.type == 3 ? "d" : "i";
        return str;
    }

    public void setConnectTimeout(int timeout) {
        if (timeout >= 0) {
            this.connectTimeout = timeout;
            return;
        }
        throw new IllegalArgumentException("timeouts can't be negative");
    }

    public int getConnectTimeout() {
        return this.connectTimeout < 0 ? 0 : this.connectTimeout;
    }

    public void setReadTimeout(int timeout) {
        if (timeout >= 0) {
            this.readTimeout = timeout;
            return;
        }
        throw new IllegalArgumentException("timeouts can't be negative");
    }

    public int getReadTimeout() {
        return this.readTimeout < 0 ? 0 : this.readTimeout;
    }
}
