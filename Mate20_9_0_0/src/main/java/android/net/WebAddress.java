package android.net;

import android.annotation.SystemApi;
import android.content.IntentFilter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SystemApi
public class WebAddress {
    static final int MATCH_GROUP_AUTHORITY = 2;
    static final int MATCH_GROUP_HOST = 3;
    static final int MATCH_GROUP_PATH = 5;
    static final int MATCH_GROUP_PORT = 4;
    static final int MATCH_GROUP_SCHEME = 1;
    static Pattern sAddressPattern = Pattern.compile("(?:(http|https|file)\\:\\/\\/)?(?:([-A-Za-z0-9$_.+!*'(),;?&=]+(?:\\:[-A-Za-z0-9$_.+!*'(),;?&=]+)?)@)?([a-zA-Z0-9 -퟿豈-﷏ﷰ-￯%_-][a-zA-Z0-9 -퟿豈-﷏ﷰ-￯%_\\.-]*|\\[[0-9a-fA-F:\\.]+\\])?(?:\\:([0-9]*))?(\\/?[^#]*)?.*", 2);
    private String mAuthInfo;
    private String mHost;
    private String mPath;
    private int mPort;
    private String mScheme;

    public WebAddress(String address) throws ParseException {
        if (address != null) {
            this.mScheme = "";
            this.mHost = "";
            this.mPort = -1;
            this.mPath = "/";
            this.mAuthInfo = "";
            Matcher m = sAddressPattern.matcher(address);
            if (m.matches()) {
                String t = m.group(1);
                if (t != null) {
                    this.mScheme = t.toLowerCase(Locale.ROOT);
                }
                t = m.group(2);
                if (t != null) {
                    this.mAuthInfo = t;
                }
                t = m.group(3);
                if (t != null) {
                    this.mHost = t;
                }
                t = m.group(4);
                if (t != null && t.length() > 0) {
                    try {
                        this.mPort = Integer.parseInt(t);
                    } catch (NumberFormatException e) {
                        throw new ParseException("Bad port");
                    }
                }
                t = m.group(5);
                if (t != null && t.length() > 0) {
                    if (t.charAt(0) == '/') {
                        this.mPath = t;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("/");
                        stringBuilder.append(t);
                        this.mPath = stringBuilder.toString();
                    }
                }
                if (this.mPort == 443 && this.mScheme.equals("")) {
                    this.mScheme = IntentFilter.SCHEME_HTTPS;
                } else if (this.mPort == -1) {
                    if (this.mScheme.equals(IntentFilter.SCHEME_HTTPS)) {
                        this.mPort = 443;
                    } else {
                        this.mPort = 80;
                    }
                }
                if (this.mScheme.equals("")) {
                    this.mScheme = IntentFilter.SCHEME_HTTP;
                    return;
                }
                return;
            }
            throw new ParseException("Bad address");
        }
        throw new NullPointerException();
    }

    public String toString() {
        StringBuilder stringBuilder;
        String port = "";
        if ((this.mPort != 443 && this.mScheme.equals(IntentFilter.SCHEME_HTTPS)) || (this.mPort != 80 && this.mScheme.equals(IntentFilter.SCHEME_HTTP))) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(":");
            stringBuilder2.append(Integer.toString(this.mPort));
            port = stringBuilder2.toString();
        }
        String authInfo = "";
        if (this.mAuthInfo.length() > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.mAuthInfo);
            stringBuilder.append("@");
            authInfo = stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(this.mScheme);
        stringBuilder.append("://");
        stringBuilder.append(authInfo);
        stringBuilder.append(this.mHost);
        stringBuilder.append(port);
        stringBuilder.append(this.mPath);
        return stringBuilder.toString();
    }

    public void setScheme(String scheme) {
        this.mScheme = scheme;
    }

    public String getScheme() {
        return this.mScheme;
    }

    public void setHost(String host) {
        this.mHost = host;
    }

    public String getHost() {
        return this.mHost;
    }

    public void setPort(int port) {
        this.mPort = port;
    }

    public int getPort() {
        return this.mPort;
    }

    public void setPath(String path) {
        this.mPath = path;
    }

    public String getPath() {
        return this.mPath;
    }

    public void setAuthInfo(String authInfo) {
        this.mAuthInfo = authInfo;
    }

    public String getAuthInfo() {
        return this.mAuthInfo;
    }
}
