package android.icu.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public abstract class URLHandler {
    private static final boolean DEBUG = ICUDebug.enabled("URLHandler");
    public static final String PROPNAME = "urlhandler.props";
    private static final Map<String, Method> handlers;

    public interface URLVisitor {
        void visit(String str);
    }

    private static class FileURLHandler extends URLHandler {
        File file;

        FileURLHandler(URL url) {
            try {
                this.file = new File(url.toURI());
            } catch (URISyntaxException e) {
            }
            if (this.file == null || !this.file.exists()) {
                if (URLHandler.DEBUG) {
                    PrintStream printStream = System.err;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("file does not exist - ");
                    stringBuilder.append(url.toString());
                    printStream.println(stringBuilder.toString());
                }
                throw new IllegalArgumentException();
            }
        }

        public void guide(URLVisitor v, boolean recurse, boolean strip) {
            if (this.file.isDirectory()) {
                process(v, recurse, strip, "/", this.file.listFiles());
                return;
            }
            v.visit(this.file.getName());
        }

        private void process(URLVisitor v, boolean recurse, boolean strip, String path, File[] files) {
            if (files != null) {
                for (File f : files) {
                    StringBuilder stringBuilder;
                    if (!f.isDirectory()) {
                        String name;
                        if (strip) {
                            name = f.getName();
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(path);
                            stringBuilder.append(f.getName());
                            name = stringBuilder.toString();
                        }
                        v.visit(name);
                    } else if (recurse) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(path);
                        stringBuilder.append(f.getName());
                        stringBuilder.append('/');
                        process(v, recurse, strip, stringBuilder.toString(), f.listFiles());
                    }
                }
            }
        }
    }

    private static class JarURLHandler extends URLHandler {
        JarFile jarFile;
        String prefix;

        JarURLHandler(URL url) {
            StringBuilder stringBuilder;
            try {
                this.prefix = url.getPath();
                int ix = this.prefix.lastIndexOf("!/");
                if (ix >= 0) {
                    this.prefix = this.prefix.substring(ix + 2);
                }
                if (!url.getProtocol().equals("jar")) {
                    String urlStr = url.toString();
                    int idx = urlStr.indexOf(":");
                    if (idx != -1) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("jar");
                        stringBuilder2.append(urlStr.substring(idx));
                        url = new URL(stringBuilder2.toString());
                    }
                }
                this.jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
            } catch (Exception e) {
                if (URLHandler.DEBUG) {
                    PrintStream printStream = System.err;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("icurb jar error: ");
                    stringBuilder.append(e);
                    printStream.println(stringBuilder.toString());
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("jar error: ");
                stringBuilder.append(e.getMessage());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public void guide(URLVisitor v, boolean recurse, boolean strip) {
            try {
                Enumeration<JarEntry> entries = this.jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = (JarEntry) entries.nextElement();
                    if (!entry.isDirectory()) {
                        String name = entry.getName();
                        if (name.startsWith(this.prefix)) {
                            name = name.substring(this.prefix.length());
                            int ix = name.lastIndexOf(47);
                            if (ix <= 0 || recurse) {
                                if (strip && ix != -1) {
                                    name = name.substring(ix + 1);
                                }
                                v.visit(name);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (URLHandler.DEBUG) {
                    PrintStream printStream = System.err;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("icurb jar error: ");
                    stringBuilder.append(e);
                    printStream.println(stringBuilder.toString());
                }
            }
        }
    }

    public abstract void guide(URLVisitor uRLVisitor, boolean z, boolean z2);

    static {
        Map<String, Method> h = null;
        BufferedReader br = null;
        try {
            InputStream is = ClassLoaderUtil.getClassLoader(URLHandler.class).getResourceAsStream(PROPNAME);
            if (is != null) {
                Class<?>[] params = new Class[]{URL.class};
                br = new BufferedReader(new InputStreamReader(is));
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    line = line.trim();
                    if (line.length() != 0) {
                        if (line.charAt(0) != '#') {
                            int ix = line.indexOf(61);
                            if (ix == -1) {
                                if (DEBUG) {
                                    PrintStream printStream = System.err;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("bad urlhandler line: '");
                                    stringBuilder.append(line);
                                    stringBuilder.append("'");
                                    printStream.println(stringBuilder.toString());
                                }
                                br.close();
                            } else {
                                String key = line.substring(0, ix).trim();
                                Method m = Class.forName(line.substring(ix + 1).trim()).getDeclaredMethod("get", params);
                                if (h == null) {
                                    h = new HashMap();
                                }
                                h.put(key, m);
                            }
                        }
                    }
                }
                br.close();
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        } catch (ClassNotFoundException e2) {
            if (DEBUG) {
                System.err.println(e2);
            }
        } catch (NoSuchMethodException e3) {
            if (DEBUG) {
                System.err.println(e3);
            }
        } catch (SecurityException e4) {
            if (DEBUG) {
                System.err.println(e4);
            }
        } catch (Throwable t) {
            try {
                if (DEBUG) {
                    System.err.println(t);
                }
                if (br != null) {
                    br.close();
                }
            } catch (Throwable th) {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e5) {
                    }
                }
            }
        }
        handlers = h;
    }

    public static URLHandler get(URL url) {
        if (url == null) {
            return null;
        }
        String protocol = url.getProtocol();
        if (handlers != null) {
            Method m = (Method) handlers.get(protocol);
            if (m != null) {
                try {
                    URLHandler handler = (URLHandler) m.invoke(null, new Object[]{url});
                    if (handler != null) {
                        return handler;
                    }
                } catch (IllegalAccessException e) {
                    if (DEBUG) {
                        System.err.println(e);
                    }
                } catch (IllegalArgumentException e2) {
                    if (DEBUG) {
                        System.err.println(e2);
                    }
                } catch (InvocationTargetException e3) {
                    if (DEBUG) {
                        System.err.println(e3);
                    }
                }
            }
        }
        return getDefault(url);
    }

    protected static URLHandler getDefault(URL url) {
        String protocol = url.getProtocol();
        try {
            if (protocol.equals("file")) {
                return new FileURLHandler(url);
            }
            if (protocol.equals("jar") || protocol.equals("wsjar")) {
                return new JarURLHandler(url);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public void guide(URLVisitor visitor, boolean recurse) {
        guide(visitor, recurse, true);
    }
}
