package org.bouncycastle.i18n;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;

public class MissingEntryException extends RuntimeException {
    private String debugMsg;
    protected final String key;
    protected final ClassLoader loader;
    protected final Locale locale;
    protected final String resource;

    public MissingEntryException(String str, String str2, String str3, Locale locale, ClassLoader classLoader) {
        super(str);
        this.resource = str2;
        this.key = str3;
        this.locale = locale;
        this.loader = classLoader;
    }

    public MissingEntryException(String str, Throwable th, String str2, String str3, Locale locale, ClassLoader classLoader) {
        super(str, th);
        this.resource = str2;
        this.key = str3;
        this.locale = locale;
        this.loader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return this.loader;
    }

    public String getDebugMsg() {
        if (this.debugMsg == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can not find entry ");
            stringBuilder.append(this.key);
            stringBuilder.append(" in resource file ");
            stringBuilder.append(this.resource);
            stringBuilder.append(" for the locale ");
            stringBuilder.append(this.locale);
            stringBuilder.append(".");
            this.debugMsg = stringBuilder.toString();
            if (this.loader instanceof URLClassLoader) {
                URL[] uRLs = ((URLClassLoader) this.loader).getURLs();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.debugMsg);
                stringBuilder2.append(" The following entries in the classpath were searched: ");
                this.debugMsg = stringBuilder2.toString();
                for (int i = 0; i != uRLs.length; i++) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(this.debugMsg);
                    stringBuilder3.append(uRLs[i]);
                    stringBuilder3.append(" ");
                    this.debugMsg = stringBuilder3.toString();
                }
            }
        }
        return this.debugMsg;
    }

    public String getKey() {
        return this.key;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public String getResource() {
        return this.resource;
    }
}
