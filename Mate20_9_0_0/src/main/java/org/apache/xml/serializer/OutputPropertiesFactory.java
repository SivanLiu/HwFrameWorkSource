package org.apache.xml.serializer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.xml.serializer.utils.MsgKey;
import org.apache.xml.serializer.utils.Utils;
import org.apache.xml.serializer.utils.WrappedRuntimeException;

public final class OutputPropertiesFactory {
    private static final Class ACCESS_CONTROLLER_CLASS = findAccessControllerClass();
    private static final String PROP_DIR;
    private static final String PROP_FILE_HTML = "output_html.properties";
    private static final String PROP_FILE_TEXT = "output_text.properties";
    private static final String PROP_FILE_UNKNOWN = "output_unknown.properties";
    private static final String PROP_FILE_XML = "output_xml.properties";
    public static final String S_BUILTIN_EXTENSIONS_UNIVERSAL = "{http://xml.apache.org/xalan}";
    private static final String S_BUILTIN_EXTENSIONS_URL = "http://xml.apache.org/xalan";
    public static final String S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL = "{http://xml.apache.org/xslt}";
    public static final int S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL_LEN = S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL.length();
    private static final String S_BUILTIN_OLD_EXTENSIONS_URL = "http://xml.apache.org/xslt";
    public static final String S_KEY_CONTENT_HANDLER = "{http://xml.apache.org/xalan}content-handler";
    public static final String S_KEY_ENTITIES = "{http://xml.apache.org/xalan}entities";
    public static final String S_KEY_INDENT_AMOUNT = "{http://xml.apache.org/xalan}indent-amount";
    public static final String S_KEY_LINE_SEPARATOR = "{http://xml.apache.org/xalan}line-separator";
    public static final String S_OMIT_META_TAG = "{http://xml.apache.org/xalan}omit-meta-tag";
    public static final String S_USE_URL_ESCAPING = "{http://xml.apache.org/xalan}use-url-escaping";
    private static final String S_XALAN_PREFIX = "org.apache.xslt.";
    private static final int S_XALAN_PREFIX_LEN = S_XALAN_PREFIX.length();
    private static final String S_XSLT_PREFIX = "xslt.output.";
    private static final int S_XSLT_PREFIX_LEN = S_XSLT_PREFIX.length();
    private static Properties m_html_properties = null;
    private static Integer m_synch_object = new Integer(1);
    private static Properties m_text_properties = null;
    private static Properties m_unknown_properties = null;
    private static Properties m_xml_properties = null;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(SerializerBase.PKG_PATH);
        stringBuilder.append('/');
        PROP_DIR = stringBuilder.toString();
    }

    private static Class findAccessControllerClass() {
        try {
            return Class.forName("java.security.AccessController");
        } catch (Exception e) {
            return null;
        }
    }

    public static final Properties getDefaultMethodProperties(String method) {
        String fileName = null;
        Properties defaultProperties = null;
        try {
            Properties defaultProperties2;
            synchronized (m_synch_object) {
                if (m_xml_properties == null) {
                    fileName = PROP_FILE_XML;
                    m_xml_properties = loadPropertiesFile(fileName, null);
                }
            }
            if (method.equals("xml")) {
                defaultProperties2 = m_xml_properties;
            } else if (method.equals("html")) {
                if (m_html_properties == null) {
                    m_html_properties = loadPropertiesFile(PROP_FILE_HTML, m_xml_properties);
                }
                defaultProperties2 = m_html_properties;
            } else if (method.equals("text")) {
                if (m_text_properties == null) {
                    m_text_properties = loadPropertiesFile(PROP_FILE_TEXT, m_xml_properties);
                    if (m_text_properties.getProperty("encoding") == null) {
                        m_text_properties.put("encoding", Encodings.getMimeEncoding(null));
                    }
                }
                defaultProperties2 = m_text_properties;
            } else if (method.equals("")) {
                if (m_unknown_properties == null) {
                    m_unknown_properties = loadPropertiesFile(PROP_FILE_UNKNOWN, m_xml_properties);
                }
                defaultProperties2 = m_unknown_properties;
            } else {
                defaultProperties2 = m_xml_properties;
            }
            return new Properties(defaultProperties2);
        } catch (IOException ioe) {
            throw new WrappedRuntimeException(Utils.messages.createMessage(MsgKey.ER_COULD_NOT_LOAD_METHOD_PROPERTY, new Object[]{fileName, method}), ioe);
        }
    }

    private static Properties loadPropertiesFile(final String resourceName, Properties defaults) throws IOException {
        Properties props = new Properties(defaults);
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            if (ACCESS_CONTROLLER_CLASS != null) {
                is = (InputStream) AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        return OutputPropertiesFactory.class.getResourceAsStream(resourceName);
                    }
                });
            } else {
                is = OutputPropertiesFactory.class.getResourceAsStream(resourceName);
            }
            bis = new BufferedInputStream(is);
            props.load(bis);
            bis.close();
            if (is != null) {
                is.close();
            }
            Enumeration keys = ((Properties) props.clone()).keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String value = null;
                try {
                    value = System.getProperty(key);
                } catch (SecurityException e) {
                }
                if (value == null) {
                    value = (String) props.get(key);
                }
                String newKey = fixupPropertyString(key, true);
                String newValue = null;
                try {
                    newValue = System.getProperty(newKey);
                } catch (SecurityException e2) {
                }
                if (newValue == null) {
                    newValue = fixupPropertyString(value, false);
                } else {
                    newValue = fixupPropertyString(newValue, false);
                }
                if (key != newKey || value != newValue) {
                    props.remove(key);
                    props.put(newKey, newValue);
                }
            }
            return props;
        } catch (IOException ioe) {
            if (defaults == null) {
                throw ioe;
            }
            throw new WrappedRuntimeException(Utils.messages.createMessage("ER_COULD_NOT_LOAD_RESOURCE", new Object[]{resourceName}), ioe);
        } catch (SecurityException se) {
            if (defaults == null) {
                throw se;
            }
            throw new WrappedRuntimeException(Utils.messages.createMessage("ER_COULD_NOT_LOAD_RESOURCE", new Object[]{resourceName}), se);
        } catch (Throwable th) {
            if (bis != null) {
                bis.close();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    private static String fixupPropertyString(String s, boolean doClipping) {
        if (doClipping && s.startsWith(S_XSLT_PREFIX)) {
            s = s.substring(S_XSLT_PREFIX_LEN);
        }
        if (s.startsWith(S_XALAN_PREFIX)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(S_BUILTIN_EXTENSIONS_UNIVERSAL);
            stringBuilder.append(s.substring(S_XALAN_PREFIX_LEN));
            s = stringBuilder.toString();
        }
        int indexOf = s.indexOf("\\u003a");
        int index = indexOf;
        if (indexOf <= 0) {
            return s;
        }
        String temp = s.substring(index + 6);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(s.substring(0, index));
        stringBuilder2.append(":");
        stringBuilder2.append(temp);
        return stringBuilder2.toString();
    }
}
