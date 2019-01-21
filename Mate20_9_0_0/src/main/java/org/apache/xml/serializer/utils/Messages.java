package org.apache.xml.serializer.utils;

import java.text.MessageFormat;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Messages {
    private final Locale m_locale = Locale.getDefault();
    private ListResourceBundle m_resourceBundle;
    private String m_resourceBundleName;

    Messages(String resourceBundle) {
        this.m_resourceBundleName = resourceBundle;
    }

    private Locale getLocale() {
        return this.m_locale;
    }

    private ListResourceBundle getResourceBundle() {
        return this.m_resourceBundle;
    }

    public final String createMessage(String msgKey, Object[] args) {
        if (this.m_resourceBundle == null) {
            this.m_resourceBundle = loadResourceBundle(this.m_resourceBundleName);
        }
        if (this.m_resourceBundle != null) {
            return createMsg(this.m_resourceBundle, msgKey, args);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not load the resource bundles: ");
        stringBuilder.append(this.m_resourceBundleName);
        return stringBuilder.toString();
    }

    private final String createMsg(ListResourceBundle fResourceBundle, String msgKey, Object[] args) {
        StringBuilder stringBuilder;
        String fmsg = null;
        boolean throwex = false;
        String msg = null;
        if (msgKey != null) {
            msg = fResourceBundle.getString(msgKey);
        } else {
            msgKey = "";
        }
        if (msg == null) {
            throwex = true;
            try {
                msg = MessageFormat.format(MsgKey.BAD_MSGKEY, new Object[]{msgKey, this.m_resourceBundleName});
            } catch (Exception e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("The message key '");
                stringBuilder.append(msgKey);
                stringBuilder.append("' is not in the message class '");
                stringBuilder.append(this.m_resourceBundleName);
                stringBuilder.append("'");
                msg = stringBuilder.toString();
            }
        } else if (args != null) {
            try {
                int n = args.length;
                for (int i = 0; i < n; i++) {
                    if (args[i] == null) {
                        args[i] = "";
                    }
                }
                fmsg = MessageFormat.format(msg, args);
            } catch (Exception e2) {
                throwex = true;
                try {
                    fmsg = MessageFormat.format(MsgKey.BAD_MSGFORMAT, new Object[]{msgKey, this.m_resourceBundleName});
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(fmsg);
                    stringBuilder2.append(" ");
                    stringBuilder2.append(msg);
                    fmsg = stringBuilder2.toString();
                } catch (Exception e3) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("The format of message '");
                    stringBuilder.append(msgKey);
                    stringBuilder.append("' in message class '");
                    stringBuilder.append(this.m_resourceBundleName);
                    stringBuilder.append("' failed.");
                    fmsg = stringBuilder.toString();
                }
            }
        } else {
            fmsg = msg;
        }
        if (!throwex) {
            return fmsg;
        }
        throw new RuntimeException(fmsg);
    }

    private ListResourceBundle loadResourceBundle(String resourceBundle) throws MissingResourceException {
        MissingResourceException e;
        this.m_resourceBundleName = resourceBundle;
        try {
            e = (ListResourceBundle) ResourceBundle.getBundle(this.m_resourceBundleName, getLocale());
        } catch (MissingResourceException e2) {
            try {
                e = (ListResourceBundle) ResourceBundle.getBundle(this.m_resourceBundleName, new Locale("en", "US"));
            } catch (MissingResourceException e3) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Could not load any resource bundles.");
                stringBuilder.append(this.m_resourceBundleName);
                throw new MissingResourceException(stringBuilder.toString(), this.m_resourceBundleName, "");
            }
        }
        this.m_resourceBundle = e;
        return e;
    }

    private static String getResourceSuffix(Locale locale) {
        String suffix = new StringBuilder();
        suffix.append("_");
        suffix.append(locale.getLanguage());
        suffix = suffix.toString();
        String country = locale.getCountry();
        if (!country.equals("TW")) {
            return suffix;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(suffix);
        stringBuilder.append("_");
        stringBuilder.append(country);
        return stringBuilder.toString();
    }
}
