package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.net.Proxy.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.StringTokenizer;
import sun.net.ApplicationProxy;
import sun.net.www.protocol.file.Handler;
import sun.security.util.SecurityConstants;

public final class URL implements Serializable {
    private static final Set<String> BUILTIN_HANDLER_CLASS_NAMES = createBuiltinHandlerClassNames();
    static URLStreamHandlerFactory factory = null;
    static Hashtable<String, URLStreamHandler> handlers = new Hashtable();
    private static final String protocolPathProp = "java.protocol.handler.pkgs";
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[]{new ObjectStreamField("protocol", String.class), new ObjectStreamField("host", String.class), new ObjectStreamField("port", Integer.TYPE), new ObjectStreamField("authority", String.class), new ObjectStreamField("file", String.class), new ObjectStreamField("ref", String.class)};
    static final long serialVersionUID = -7627629688361524110L;
    private static Object streamHandlerLock = new Object();
    private String authority;
    private String file;
    transient URLStreamHandler handler;
    private int hashCode;
    private String host;
    transient InetAddress hostAddress;
    private transient String path;
    private int port;
    private String protocol;
    private transient String query;
    private String ref;
    private transient UrlDeserializedState tempState;
    private transient String userInfo;

    public URL(String protocol, String host, int port, String file) throws MalformedURLException {
        this(protocol, host, port, file, null);
    }

    public URL(String protocol, String host, String file) throws MalformedURLException {
        this(protocol, host, -1, file);
    }

    public URL(String protocol, String host, int port, String file, URLStreamHandler handler) throws MalformedURLException {
        StringBuilder stringBuilder;
        this.port = -1;
        this.hashCode = -1;
        if (handler != null) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                checkSpecifyHandler(sm);
            }
        }
        protocol = protocol.toLowerCase();
        this.protocol = protocol;
        if (host != null) {
            if (host.indexOf(58) >= 0 && !host.startsWith("[")) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[");
                stringBuilder.append(host);
                stringBuilder.append("]");
                host = stringBuilder.toString();
            }
            this.host = host;
            if (port >= -1) {
                String str;
                this.port = port;
                if (port == -1) {
                    str = host;
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(host);
                    stringBuilder2.append(":");
                    stringBuilder2.append(port);
                    str = stringBuilder2.toString();
                }
                this.authority = str;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid port number :");
                stringBuilder.append(port);
                throw new MalformedURLException(stringBuilder.toString());
            }
        }
        Parts parts = new Parts(file, host);
        this.path = parts.getPath();
        this.query = parts.getQuery();
        if (this.query != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.path);
            stringBuilder.append("?");
            stringBuilder.append(this.query);
            this.file = stringBuilder.toString();
        } else {
            this.file = this.path;
        }
        this.ref = parts.getRef();
        if (handler == null) {
            URLStreamHandler uRLStreamHandler = getURLStreamHandler(protocol);
            handler = uRLStreamHandler;
            if (uRLStreamHandler == null) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("unknown protocol: ");
                stringBuilder3.append(protocol);
                throw new MalformedURLException(stringBuilder3.toString());
            }
        }
        this.handler = handler;
    }

    public URL(String spec) throws MalformedURLException {
        this(null, spec);
    }

    public URL(URL context, String spec) throws MalformedURLException {
        this(context, spec, null);
    }

    public URL(URL context, String spec, URLStreamHandler handler) throws MalformedURLException {
        this.port = -1;
        this.hashCode = -1;
        String original = spec;
        int start = 0;
        String newProtocol = null;
        boolean aRef = false;
        boolean isRelative = false;
        if (handler != null) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                checkSpecifyHandler(sm);
            }
        }
        try {
            int limit = spec.length();
            while (limit > 0 && spec.charAt(limit - 1) <= ' ') {
                limit--;
            }
            while (start < limit && spec.charAt(start) <= ' ') {
                start++;
            }
            if (spec.regionMatches(true, start, "url:", 0, 4)) {
                start += 4;
            }
            if (start < spec.length() && spec.charAt(start) == '#') {
                aRef = true;
            }
            int i = start;
            while (!aRef && i < limit) {
                char charAt = spec.charAt(i);
                char c = charAt;
                if (charAt == '/') {
                    break;
                } else if (c == ':') {
                    String s = spec.substring(start, i).toLowerCase();
                    if (isValidProtocol(s)) {
                        newProtocol = s;
                        start = i + 1;
                    }
                } else {
                    i++;
                }
            }
            this.protocol = newProtocol;
            if (context != null && (newProtocol == null || newProtocol.equalsIgnoreCase(context.protocol))) {
                if (handler == null) {
                    handler = context.handler;
                }
                if (context.path != null && context.path.startsWith("/")) {
                    newProtocol = null;
                }
                if (newProtocol == null) {
                    this.protocol = context.protocol;
                    this.authority = context.authority;
                    this.userInfo = context.userInfo;
                    this.host = context.host;
                    this.port = context.port;
                    this.file = context.file;
                    this.path = context.path;
                    isRelative = true;
                }
            }
            StringBuilder stringBuilder;
            if (this.protocol != null) {
                if (handler == null) {
                    URLStreamHandler uRLStreamHandler = getURLStreamHandler(this.protocol);
                    handler = uRLStreamHandler;
                    if (uRLStreamHandler == null) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("unknown protocol: ");
                        stringBuilder.append(this.protocol);
                        throw new MalformedURLException(stringBuilder.toString());
                    }
                }
                this.handler = handler;
                i = spec.indexOf(35, start);
                if (i >= 0) {
                    this.ref = spec.substring(i + 1, limit);
                    limit = i;
                }
                if (isRelative && start == limit) {
                    this.query = context.query;
                    if (this.ref == null) {
                        this.ref = context.ref;
                    }
                }
                handler.parseURL(this, spec, start, limit);
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("no protocol: ");
            stringBuilder.append(original);
            throw new MalformedURLException(stringBuilder.toString());
        } catch (MalformedURLException e) {
            throw e;
        } catch (Exception e2) {
            MalformedURLException exception = new MalformedURLException(e2.getMessage());
            exception.initCause(e2);
            throw exception;
        }
    }

    private boolean isValidProtocol(String protocol) {
        int len = protocol.length();
        if (len < 1) {
            return false;
        }
        char c = protocol.charAt(0);
        if (!Character.isLetter(c)) {
            return false;
        }
        for (int i = 1; i < len; i++) {
            char c2 = protocol.charAt(i);
            if (!Character.isLetterOrDigit(c2) && c2 != '.' && c2 != '+' && c2 != '-') {
                return false;
            }
        }
        return true;
    }

    private void checkSpecifyHandler(SecurityManager sm) {
        sm.checkPermission(SecurityConstants.SPECIFY_HANDLER_PERMISSION);
    }

    void set(String protocol, String host, int port, String file, String ref) {
        synchronized (this) {
            String str;
            this.protocol = protocol;
            this.host = host;
            if (port == -1) {
                str = host;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(host);
                stringBuilder.append(":");
                stringBuilder.append(port);
                str = stringBuilder.toString();
            }
            this.authority = str;
            this.port = port;
            this.file = file;
            this.ref = ref;
            this.hashCode = -1;
            this.hostAddress = null;
            int q = file.lastIndexOf(63);
            if (q != -1) {
                this.query = file.substring(q + 1);
                this.path = file.substring(0, q);
            } else {
                this.path = file;
            }
        }
    }

    void set(String protocol, String host, int port, String authority, String userInfo, String path, String query, String ref) {
        synchronized (this) {
            String stringBuilder;
            this.protocol = protocol;
            this.host = host;
            this.port = port;
            if (query != null) {
                if (!query.isEmpty()) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(path);
                    stringBuilder2.append("?");
                    stringBuilder2.append(query);
                    stringBuilder = stringBuilder2.toString();
                    this.file = stringBuilder;
                    this.userInfo = userInfo;
                    this.path = path;
                    this.ref = ref;
                    this.hashCode = -1;
                    this.hostAddress = null;
                    this.query = query;
                    this.authority = authority;
                }
            }
            stringBuilder = path;
            this.file = stringBuilder;
            this.userInfo = userInfo;
            this.path = path;
            this.ref = ref;
            this.hashCode = -1;
            this.hostAddress = null;
            this.query = query;
            this.authority = authority;
        }
    }

    public String getQuery() {
        return this.query;
    }

    public String getPath() {
        return this.path;
    }

    public String getUserInfo() {
        return this.userInfo;
    }

    public String getAuthority() {
        return this.authority;
    }

    public int getPort() {
        return this.port;
    }

    public int getDefaultPort() {
        return this.handler.getDefaultPort();
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getHost() {
        return this.host;
    }

    public String getFile() {
        return this.file;
    }

    public String getRef() {
        return this.ref;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof URL)) {
            return false;
        }
        return this.handler.equals(this, (URL) obj);
    }

    public synchronized int hashCode() {
        if (this.hashCode != -1) {
            return this.hashCode;
        }
        this.hashCode = this.handler.hashCode(this);
        return this.hashCode;
    }

    public boolean sameFile(URL other) {
        return this.handler.sameFile(this, other);
    }

    public String toString() {
        return toExternalForm();
    }

    public String toExternalForm() {
        return this.handler.toExternalForm(this);
    }

    public URI toURI() throws URISyntaxException {
        return new URI(toString());
    }

    public URLConnection openConnection() throws IOException {
        return this.handler.openConnection(this);
    }

    public URLConnection openConnection(Proxy proxy) throws IOException {
        if (proxy != null) {
            Proxy p = proxy == Proxy.NO_PROXY ? Proxy.NO_PROXY : ApplicationProxy.create(proxy);
            SecurityManager sm = System.getSecurityManager();
            if (!(p.type() == Type.DIRECT || sm == null)) {
                InetSocketAddress epoint = (InetSocketAddress) p.address();
                if (epoint.isUnresolved()) {
                    sm.checkConnect(epoint.getHostName(), epoint.getPort());
                } else {
                    sm.checkConnect(epoint.getAddress().getHostAddress(), epoint.getPort());
                }
            }
            return this.handler.openConnection(this, p);
        }
        throw new IllegalArgumentException("proxy can not be null");
    }

    public final InputStream openStream() throws IOException {
        return openConnection().getInputStream();
    }

    public final Object getContent() throws IOException {
        return openConnection().getContent();
    }

    public final Object getContent(Class[] classes) throws IOException {
        return openConnection().getContent(classes);
    }

    public static void setURLStreamHandlerFactory(URLStreamHandlerFactory fac) {
        synchronized (streamHandlerLock) {
            if (factory == null) {
                SecurityManager security = System.getSecurityManager();
                if (security != null) {
                    security.checkSetFactory();
                }
                handlers.clear();
                factory = fac;
            } else {
                throw new Error("factory already defined");
            }
        }
    }

    static URLStreamHandler getURLStreamHandler(String protocol) {
        URLStreamHandler handler = (URLStreamHandler) handlers.get(protocol);
        if (handler == null) {
            boolean checkedWithFactory = false;
            if (factory != null) {
                handler = factory.createURLStreamHandler(protocol);
                checkedWithFactory = true;
            }
            if (handler == null) {
                StringTokenizer packagePrefixIter = new StringTokenizer(System.getProperty(protocolPathProp, ""), "|");
                while (handler == null && packagePrefixIter.hasMoreTokens()) {
                    String packagePrefix = packagePrefixIter.nextToken().trim();
                    try {
                        String clsName = new StringBuilder();
                        clsName.append(packagePrefix);
                        clsName.append(".");
                        clsName.append(protocol);
                        clsName.append(".Handler");
                        clsName = clsName.toString();
                        Class<?> cls = null;
                        try {
                            cls = Class.forName(clsName, true, ClassLoader.getSystemClassLoader());
                        } catch (ClassNotFoundException e) {
                            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
                            if (contextLoader != null) {
                                cls = Class.forName(clsName, true, contextLoader);
                            }
                        }
                        if (cls != null) {
                            handler = (URLStreamHandler) cls.newInstance();
                        }
                    } catch (ReflectiveOperationException e2) {
                    }
                }
            }
            if (handler == null) {
                try {
                    handler = createBuiltinHandler(protocol);
                } catch (Exception e3) {
                    throw new AssertionError(e3);
                }
            }
            synchronized (streamHandlerLock) {
                URLStreamHandler handler2 = (URLStreamHandler) handlers.get(protocol);
                if (handler2 != null) {
                    return handler2;
                }
                if (!(checkedWithFactory || factory == null)) {
                    handler2 = factory.createURLStreamHandler(protocol);
                }
                if (handler2 != null) {
                    handler = handler2;
                }
                if (handler != null) {
                    handlers.put(protocol, handler);
                }
            }
        }
        return handler;
    }

    private static URLStreamHandler createBuiltinHandler(String protocol) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (protocol.equals("file")) {
            return new Handler();
        }
        if (protocol.equals("ftp")) {
            return new sun.net.www.protocol.ftp.Handler();
        }
        if (protocol.equals("jar")) {
            return new sun.net.www.protocol.jar.Handler();
        }
        if (protocol.equals("http")) {
            return (URLStreamHandler) Class.forName("com.android.okhttp.HttpHandler").newInstance();
        }
        if (protocol.equals("https")) {
            return (URLStreamHandler) Class.forName("com.android.okhttp.HttpsHandler").newInstance();
        }
        return null;
    }

    private static Set<String> createBuiltinHandlerClassNames() {
        Set<String> result = new HashSet();
        result.add("sun.net.www.protocol.file.Handler");
        result.add("sun.net.www.protocol.ftp.Handler");
        result.add("sun.net.www.protocol.jar.Handler");
        result.add("com.android.okhttp.HttpHandler");
        result.add("com.android.okhttp.HttpsHandler");
        return Collections.unmodifiableSet(result);
    }

    private synchronized void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }

    private synchronized void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        synchronized (this) {
            GetField gf = s.readFields();
            String protocol = (String) gf.get("protocol", null);
            if (getURLStreamHandler(protocol) != null) {
                String host = (String) gf.get("host", null);
                int port = gf.get("port", -1);
                String authority = (String) gf.get("authority", null);
                String file = (String) gf.get("file", null);
                String ref = (String) gf.get("ref", null);
                if (authority == null && ((host != null && host.length() > 0) || port != -1)) {
                    String str;
                    if (host == null) {
                        host = "";
                    }
                    if (port == -1) {
                        str = host;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(host);
                        stringBuilder.append(":");
                        stringBuilder.append(port);
                        str = stringBuilder.toString();
                    }
                    authority = str;
                }
                this.tempState = new UrlDeserializedState(protocol, host, port, authority, file, ref, -1);
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("unknown protocol: ");
                stringBuilder2.append(protocol);
                throw new IOException(stringBuilder2.toString());
            }
        }
    }

    private Object readResolve() throws ObjectStreamException {
        URLStreamHandler handler = getURLStreamHandler(this.tempState.getProtocol());
        if (isBuiltinStreamHandler(handler.getClass().getName())) {
            return fabricateNewURL();
        }
        return setDeserializedFields(handler);
    }

    private URL setDeserializedFields(URLStreamHandler handler) {
        String str;
        String userInfo = null;
        String protocol = this.tempState.getProtocol();
        String host = this.tempState.getHost();
        int port = this.tempState.getPort();
        String authority = this.tempState.getAuthority();
        String file = this.tempState.getFile();
        String ref = this.tempState.getRef();
        int hashCode = this.tempState.getHashCode();
        int at;
        if (authority == null && ((host != null && host.length() > 0) || port != -1)) {
            if (host == null) {
                host = "";
            }
            if (port == -1) {
                str = host;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(host);
                stringBuilder.append(":");
                stringBuilder.append(port);
                str = stringBuilder.toString();
            }
            authority = str;
            at = host.lastIndexOf(64);
            if (at != -1) {
                userInfo = host.substring(0, at);
                host = host.substring(at + 1);
            }
        } else if (authority != null) {
            at = authority.indexOf(64);
            if (at != -1) {
                userInfo = authority.substring(0, at);
            }
        }
        String path = null;
        str = null;
        if (file != null) {
            int q = file.lastIndexOf(63);
            if (q != -1) {
                str = file.substring(q + 1);
                path = file.substring(0, q);
            } else {
                path = file;
            }
        }
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.file = file;
        this.authority = authority;
        this.ref = ref;
        this.hashCode = hashCode;
        this.handler = handler;
        this.query = str;
        this.path = path;
        this.userInfo = userInfo;
        return this;
    }

    private URL fabricateNewURL() throws InvalidObjectException {
        String urlString = this.tempState.reconstituteUrlString();
        try {
            URL replacementURL = new URL(urlString);
            replacementURL.setSerializedHashCode(this.tempState.getHashCode());
            resetState();
            return replacementURL;
        } catch (MalformedURLException mEx) {
            resetState();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Malformed URL: ");
            stringBuilder.append(urlString);
            InvalidObjectException invoEx = new InvalidObjectException(stringBuilder.toString());
            invoEx.initCause(mEx);
            throw invoEx;
        }
    }

    private boolean isBuiltinStreamHandler(String handlerClassName) {
        return BUILTIN_HANDLER_CLASS_NAMES.contains(handlerClassName);
    }

    private void resetState() {
        this.protocol = null;
        this.host = null;
        this.port = -1;
        this.file = null;
        this.authority = null;
        this.ref = null;
        this.hashCode = -1;
        this.handler = null;
        this.query = null;
        this.path = null;
        this.userInfo = null;
        this.tempState = null;
    }

    private void setSerializedHashCode(int hc) {
        this.hashCode = hc;
    }
}
