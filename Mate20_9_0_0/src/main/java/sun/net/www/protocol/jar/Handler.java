package sun.net.www.protocol.jar;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import sun.net.www.ParseUtil;

public class Handler extends URLStreamHandler {
    private static final String separator = "!/";

    protected URLConnection openConnection(URL u) throws IOException {
        return new JarURLConnection(u, this);
    }

    private static int indexOfBangSlash(String spec) {
        int indexOfBang = spec.length();
        while (true) {
            int lastIndexOf = spec.lastIndexOf(33, indexOfBang);
            indexOfBang = lastIndexOf;
            if (lastIndexOf == -1) {
                return -1;
            }
            if (indexOfBang != spec.length() - 1 && spec.charAt(indexOfBang + 1) == '/') {
                return indexOfBang + 1;
            }
            indexOfBang--;
        }
    }

    protected boolean sameFile(URL u1, URL u2) {
        if (!u1.getProtocol().equals("jar") || !u2.getProtocol().equals("jar")) {
            return false;
        }
        String file1 = u1.getFile();
        String file2 = u2.getFile();
        int sep1 = file1.indexOf(separator);
        int sep2 = file2.indexOf(separator);
        if (sep1 == -1 || sep2 == -1) {
            return super.sameFile(u1, u2);
        }
        if (!file1.substring(sep1 + 2).equals(file2.substring(sep2 + 2))) {
            return false;
        }
        try {
            if (super.sameFile(new URL(file1.substring(0, sep1)), new URL(file2.substring(0, sep2)))) {
                return true;
            }
            return false;
        } catch (MalformedURLException e) {
            return super.sameFile(u1, u2);
        }
    }

    protected int hashCode(URL u) {
        int h = 0;
        String protocol = u.getProtocol();
        if (protocol != null) {
            h = 0 + protocol.hashCode();
        }
        String file = u.getFile();
        int sep = file.indexOf(separator);
        if (sep == -1) {
            return file.hashCode() + h;
        }
        String fileWithoutEntry = file.substring(null, sep);
        try {
            h += new URL(fileWithoutEntry).hashCode();
        } catch (MalformedURLException e) {
            h += fileWithoutEntry.hashCode();
        }
        return h + file.substring(sep + 2).hashCode();
    }

    protected void parseURL(URL url, String spec, int start, int limit) {
        URL url2;
        String file;
        String str = spec;
        String file2 = null;
        String ref = null;
        int refPos = str.indexOf(35, limit);
        boolean refOnly = refPos == start;
        if (refPos > -1) {
            ref = str.substring(refPos + 1, spec.length());
            if (refOnly) {
                file2 = url.getFile();
            }
        }
        String ref2 = ref;
        boolean absoluteSpec = false;
        if (spec.length() >= 4) {
            absoluteSpec = str.substring(0, 4).equalsIgnoreCase("jar:");
        }
        boolean absoluteSpec2 = absoluteSpec;
        String spec2 = spec.substring(start, limit);
        if (absoluteSpec2) {
            str = parseAbsoluteSpec(spec2);
            url2 = url;
        } else if (refOnly) {
            url2 = url;
            file = file2;
            setURL(url2, "jar", "", -1, file, ref2);
        } else {
            url2 = url;
            str = parseContextSpec(url2, spec2);
            file2 = indexOfBangSlash(str);
            ref = str.substring(0, file2);
            String afterBangSlash = new ParseUtil().canonizeString(str.substring(file2));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(ref);
            stringBuilder.append(afterBangSlash);
            str = stringBuilder.toString();
        }
        file = str;
        setURL(url2, "jar", "", -1, file, ref2);
    }

    private String parseAbsoluteSpec(String spec) {
        int indexOfBangSlash = indexOfBangSlash(spec);
        int index = indexOfBangSlash;
        if (indexOfBangSlash != -1) {
            try {
                URL url = new URL(spec.substring(null, index - 1));
                return spec;
            } catch (MalformedURLException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invalid url: ");
                stringBuilder.append(spec);
                stringBuilder.append(" (");
                stringBuilder.append(e);
                stringBuilder.append(")");
                throw new NullPointerException(stringBuilder.toString());
            }
        }
        throw new NullPointerException("no !/ in spec");
    }

    private String parseContextSpec(URL url, String spec) {
        int bangSlash;
        StringBuilder stringBuilder;
        String ctxFile = url.getFile();
        if (spec.startsWith("/")) {
            bangSlash = indexOfBangSlash(ctxFile);
            if (bangSlash != -1) {
                ctxFile = ctxFile.substring(0, bangSlash);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("malformed context url:");
                stringBuilder.append((Object) url);
                stringBuilder.append(": no !/");
                throw new NullPointerException(stringBuilder.toString());
            }
        }
        if (!(ctxFile.endsWith("/") || spec.startsWith("/"))) {
            bangSlash = ctxFile.lastIndexOf(47);
            if (bangSlash != -1) {
                ctxFile = ctxFile.substring(0, bangSlash + 1);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("malformed context url:");
                stringBuilder.append((Object) url);
                throw new NullPointerException(stringBuilder.toString());
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(ctxFile);
        stringBuilder2.append(spec);
        return stringBuilder2.toString();
    }
}
