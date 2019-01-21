package org.apache.xalan.xslt;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.xalan.templates.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

public class EnvironmentCheck {
    public static final String CLASS_NOTPRESENT = "not-present";
    public static final String CLASS_PRESENT = "present-unknown-version";
    public static final String ERROR = "ERROR.";
    public static final String ERROR_FOUND = "At least one error was found!";
    public static final String FOUNDCLASSES = "foundclasses.";
    public static final String VERSION = "version.";
    public static final String WARNING = "WARNING.";
    private static Hashtable jarVersions = new Hashtable();
    public String[] jarNames = new String[]{"xalan.jar", "xalansamples.jar", "xalanj1compat.jar", "xalanservlet.jar", "serializer.jar", "xerces.jar", "xercesImpl.jar", "testxsl.jar", "crimson.jar", "lotusxsl.jar", "jaxp.jar", "parser.jar", "dom.jar", "sax.jar", "xml.jar", "xml-apis.jar", "xsltc.jar"};
    protected PrintWriter outWriter = new PrintWriter(System.out, true);

    public EnvironmentCheck() {
    }

    public static void main(String[] args) {
        PrintWriter sendOutputTo = new PrintWriter(System.out, true);
        int i = 0;
        while (i < args.length) {
            if ("-out".equalsIgnoreCase(args[i])) {
                i++;
                if (i < args.length) {
                    try {
                        sendOutputTo = new PrintWriter(new FileWriter(args[i], true));
                    } catch (Exception e) {
                        PrintStream printStream = System.err;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("# WARNING: -out ");
                        stringBuilder.append(args[i]);
                        stringBuilder.append(" threw ");
                        stringBuilder.append(e.toString());
                        printStream.println(stringBuilder.toString());
                    }
                } else {
                    System.err.println("# WARNING: -out argument should have a filename, output sent to console");
                }
            }
            i++;
        }
        new EnvironmentCheck().checkEnvironment(sendOutputTo);
    }

    public boolean checkEnvironment(PrintWriter pw) {
        if (pw != null) {
            this.outWriter = pw;
        }
        if (writeEnvironmentReport(getEnvironmentHash())) {
            logMsg("# WARNING: Potential problems found in your environment!");
            logMsg("#    Check any 'ERROR' items above against the Xalan FAQs");
            logMsg("#    to correct potential problems with your classes/jars");
            logMsg("#    http://xml.apache.org/xalan-j/faq.html");
            if (this.outWriter != null) {
                this.outWriter.flush();
            }
            return false;
        }
        logMsg("# YAHOO! Your environment seems to be OK.");
        if (this.outWriter != null) {
            this.outWriter.flush();
        }
        return true;
    }

    public Hashtable getEnvironmentHash() {
        Hashtable hash = new Hashtable();
        checkJAXPVersion(hash);
        checkProcessorVersion(hash);
        checkParserVersion(hash);
        checkAntVersion(hash);
        checkDOMVersion(hash);
        checkSAXVersion(hash);
        checkSystemProperties(hash);
        return hash;
    }

    protected boolean writeEnvironmentReport(Hashtable h) {
        if (h == null) {
            logMsg("# ERROR: writeEnvironmentReport called with null Hashtable");
            return false;
        }
        boolean errors = false;
        logMsg("#---- BEGIN writeEnvironmentReport($Revision: 468646 $): Useful stuff found: ----");
        Enumeration keys = h.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            String keyStr = key;
            try {
                if (keyStr.startsWith(FOUNDCLASSES)) {
                    errors |= logFoundJars((Vector) h.get(keyStr), keyStr);
                } else {
                    if (keyStr.startsWith(ERROR)) {
                        errors = true;
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(keyStr);
                    stringBuilder.append("=");
                    stringBuilder.append(h.get(keyStr));
                    logMsg(stringBuilder.toString());
                }
            } catch (Exception e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Reading-");
                stringBuilder2.append(key);
                stringBuilder2.append("= threw: ");
                stringBuilder2.append(e.toString());
                logMsg(stringBuilder2.toString());
            }
        }
        logMsg("#----- END writeEnvironmentReport: Useful properties found: -----");
        return errors;
    }

    protected boolean logFoundJars(Vector v, String desc) {
        int i = 0;
        if (v == null || v.size() < 1) {
            return false;
        }
        boolean errors = false;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("#---- BEGIN Listing XML-related jars in: ");
        stringBuilder.append(desc);
        stringBuilder.append(" ----");
        logMsg(stringBuilder.toString());
        while (i < v.size()) {
            Hashtable subhash = (Hashtable) v.elementAt(i);
            Enumeration keys = subhash.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String keyStr = key;
                try {
                    if (keyStr.startsWith(ERROR)) {
                        errors = true;
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(keyStr);
                    stringBuilder2.append("=");
                    stringBuilder2.append(subhash.get(keyStr));
                    logMsg(stringBuilder2.toString());
                } catch (Exception e) {
                    errors = true;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Reading-");
                    stringBuilder3.append(key);
                    stringBuilder3.append("= threw: ");
                    stringBuilder3.append(e.toString());
                    logMsg(stringBuilder3.toString());
                }
            }
            i++;
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("#----- END Listing XML-related jars in: ");
        stringBuilder4.append(desc);
        stringBuilder4.append(" -----");
        logMsg(stringBuilder4.toString());
        return errors;
    }

    public void appendEnvironmentReport(Node container, Document factory, Hashtable h) {
        if (container != null && factory != null) {
            try {
                Element envCheckNode = factory.createElement("EnvironmentCheck");
                envCheckNode.setAttribute("version", "$Revision: 468646 $");
                container.appendChild(envCheckNode);
                if (h == null) {
                    Element statusNode = factory.createElement("status");
                    statusNode.setAttribute(Constants.EXSLT_ELEMNAME_FUNCRESULT_STRING, "ERROR");
                    statusNode.appendChild(factory.createTextNode("appendEnvironmentReport called with null Hashtable!"));
                    envCheckNode.appendChild(statusNode);
                    return;
                }
                boolean errors = false;
                Element hashNode = factory.createElement("environment");
                envCheckNode.appendChild(hashNode);
                Enumeration keys = h.keys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    String keyStr = key;
                    try {
                        if (keyStr.startsWith(FOUNDCLASSES)) {
                            errors |= appendFoundJars(hashNode, factory, (Vector) h.get(keyStr), keyStr);
                        } else {
                            if (keyStr.startsWith(ERROR)) {
                                errors = true;
                            }
                            Element node = factory.createElement("item");
                            node.setAttribute("key", keyStr);
                            node.appendChild(factory.createTextNode((String) h.get(keyStr)));
                            hashNode.appendChild(node);
                        }
                    } catch (Exception e) {
                        errors = true;
                        Element node2 = factory.createElement("item");
                        node2.setAttribute("key", keyStr);
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ERROR. Reading ");
                        stringBuilder.append(key);
                        stringBuilder.append(" threw: ");
                        stringBuilder.append(e.toString());
                        node2.appendChild(factory.createTextNode(stringBuilder.toString()));
                        hashNode.appendChild(node2);
                    }
                }
                Element statusNode2 = factory.createElement("status");
                statusNode2.setAttribute(Constants.EXSLT_ELEMNAME_FUNCRESULT_STRING, errors ? "ERROR" : "OK");
                envCheckNode.appendChild(statusNode2);
            } catch (Exception e2) {
                PrintStream printStream = System.err;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("appendEnvironmentReport threw: ");
                stringBuilder2.append(e2.toString());
                printStream.println(stringBuilder2.toString());
                e2.printStackTrace();
            }
        }
    }

    protected boolean appendFoundJars(Node container, Document factory, Vector v, String desc) {
        if (v == null || v.size() < 1) {
            return false;
        }
        boolean errors = false;
        for (int i = 0; i < v.size(); i++) {
            Hashtable subhash = (Hashtable) v.elementAt(i);
            Enumeration keys = subhash.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                Element node;
                try {
                    String keyStr = key;
                    if (keyStr.startsWith(ERROR)) {
                        errors = true;
                    }
                    node = factory.createElement("foundJar");
                    node.setAttribute("name", keyStr.substring(0, keyStr.indexOf("-")));
                    node.setAttribute("desc", keyStr.substring(keyStr.indexOf("-") + 1));
                    node.appendChild(factory.createTextNode((String) subhash.get(keyStr)));
                    container.appendChild(node);
                } catch (Exception e) {
                    errors = true;
                    node = factory.createElement("foundJar");
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ERROR. Reading ");
                    stringBuilder.append(key);
                    stringBuilder.append(" threw: ");
                    stringBuilder.append(e.toString());
                    node.appendChild(factory.createTextNode(stringBuilder.toString()));
                    container.appendChild(node);
                }
            }
        }
        return errors;
    }

    protected void checkSystemProperties(Hashtable h) {
        if (h == null) {
            h = new Hashtable();
        }
        try {
            h.put("java.version", System.getProperty("java.version"));
        } catch (SecurityException e) {
            h.put("java.version", "WARNING: SecurityException thrown accessing system version properties");
        }
        try {
            String cp = System.getProperty("java.class.path");
            h.put("java.class.path", cp);
            Vector classpathJars = checkPathForJars(cp, this.jarNames);
            if (classpathJars != null) {
                h.put("foundclasses.java.class.path", classpathJars);
            }
            String othercp = System.getProperty("sun.boot.class.path");
            if (othercp != null) {
                h.put("sun.boot.class.path", othercp);
                classpathJars = checkPathForJars(othercp, this.jarNames);
                if (classpathJars != null) {
                    h.put("foundclasses.sun.boot.class.path", classpathJars);
                }
            }
            othercp = System.getProperty("java.ext.dirs");
            if (othercp != null) {
                h.put("java.ext.dirs", othercp);
                classpathJars = checkPathForJars(othercp, this.jarNames);
                if (classpathJars != null) {
                    h.put("foundclasses.java.ext.dirs", classpathJars);
                }
            }
        } catch (SecurityException e2) {
            h.put("java.class.path", "WARNING: SecurityException thrown accessing system classpath properties");
        }
    }

    protected Vector checkPathForJars(String cp, String[] jars) {
        if (cp == null || jars == null || cp.length() == 0 || jars.length == 0) {
            return null;
        }
        Vector v = new Vector();
        StringTokenizer st = new StringTokenizer(cp, File.pathSeparator);
        while (st.hasMoreTokens()) {
            String filename = st.nextToken();
            for (int i = 0; i < jars.length; i++) {
                if (filename.indexOf(jars[i]) > -1) {
                    File f = new File(filename);
                    Hashtable h;
                    StringBuilder stringBuilder;
                    if (f.exists()) {
                        try {
                            h = new Hashtable(2);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(jars[i]);
                            stringBuilder.append("-path");
                            h.put(stringBuilder.toString(), f.getAbsolutePath());
                            if (!"xalan.jar".equalsIgnoreCase(jars[i])) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(jars[i]);
                                stringBuilder.append("-apparent.version");
                                h.put(stringBuilder.toString(), getApparentVersion(jars[i], f.length()));
                            }
                            v.addElement(h);
                        } catch (Exception e) {
                        }
                    } else {
                        h = new Hashtable(2);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(jars[i]);
                        stringBuilder.append("-path");
                        String stringBuilder2 = stringBuilder.toString();
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("WARNING. Classpath entry: ");
                        stringBuilder3.append(filename);
                        stringBuilder3.append(" does not exist");
                        h.put(stringBuilder2, stringBuilder3.toString());
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(jars[i]);
                        stringBuilder.append("-apparent.version");
                        h.put(stringBuilder.toString(), CLASS_NOTPRESENT);
                        v.addElement(h);
                    }
                }
            }
        }
        return v;
    }

    protected String getApparentVersion(String jarName, long jarSize) {
        String foundSize = (String) jarVersions.get(new Long(jarSize));
        if (foundSize != null && foundSize.startsWith(jarName)) {
            return foundSize;
        }
        StringBuilder stringBuilder;
        if ("xerces.jar".equalsIgnoreCase(jarName) || "xercesImpl.jar".equalsIgnoreCase(jarName)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(jarName);
            stringBuilder.append(" ");
            stringBuilder.append(WARNING);
            stringBuilder.append(CLASS_PRESENT);
            return stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(jarName);
        stringBuilder.append(" ");
        stringBuilder.append(CLASS_PRESENT);
        return stringBuilder.toString();
    }

    protected void checkJAXPVersion(Hashtable h) {
        if (h == null) {
            h = new Hashtable();
        }
        Class[] noArgs = new Class[null];
        Class clazz = null;
        try {
            String JAXP1_CLASS = "javax.xml.parsers.DocumentBuilder";
            String JAXP11_METHOD = "getDOMImplementation";
            clazz = ObjectFactory.findProviderClass("javax.xml.parsers.DocumentBuilder", ObjectFactory.findClassLoader(), true);
            Method method = clazz.getMethod("getDOMImplementation", noArgs);
            h.put("version.JAXP", "1.1 or higher");
        } catch (Exception e) {
            if (clazz != null) {
                h.put("ERROR.version.JAXP", "1.0.1");
                h.put(ERROR, ERROR_FOUND);
                return;
            }
            h.put("ERROR.version.JAXP", CLASS_NOTPRESENT);
            h.put(ERROR, ERROR_FOUND);
        }
    }

    protected void checkProcessorVersion(Hashtable h) {
        String XALAN1_VERSION_CLASS;
        Class clazz;
        StringBuffer buf;
        if (h == null) {
            h = new Hashtable();
        }
        try {
            XALAN1_VERSION_CLASS = "org.apache.xalan.xslt.XSLProcessorVersion";
            clazz = ObjectFactory.findProviderClass("org.apache.xalan.xslt.XSLProcessorVersion", ObjectFactory.findClassLoader(), true);
            buf = new StringBuffer();
            buf.append(clazz.getField("PRODUCT").get(null));
            buf.append(';');
            buf.append(clazz.getField("LANGUAGE").get(null));
            buf.append(';');
            buf.append(clazz.getField("S_VERSION").get(null));
            buf.append(';');
            h.put("version.xalan1", buf.toString());
        } catch (Exception e) {
            h.put("version.xalan1", CLASS_NOTPRESENT);
        }
        try {
            XALAN1_VERSION_CLASS = "org.apache.xalan.processor.XSLProcessorVersion";
            clazz = ObjectFactory.findProviderClass("org.apache.xalan.processor.XSLProcessorVersion", ObjectFactory.findClassLoader(), true);
            buf = new StringBuffer();
            buf.append(clazz.getField("S_VERSION").get(null));
            h.put("version.xalan2x", buf.toString());
        } catch (Exception e2) {
            h.put("version.xalan2x", CLASS_NOTPRESENT);
        }
        try {
            XALAN1_VERSION_CLASS = "org.apache.xalan.Version";
            String XALAN2_2_VERSION_METHOD = "getVersion";
            h.put("version.xalan2_2", (String) ObjectFactory.findProviderClass("org.apache.xalan.Version", ObjectFactory.findClassLoader(), true).getMethod("getVersion", new Class[0]).invoke(null, new Object[0]));
        } catch (Exception e3) {
            h.put("version.xalan2_2", CLASS_NOTPRESENT);
        }
    }

    protected void checkParserVersion(Hashtable h) {
        String XERCES1_VERSION_CLASS;
        if (h == null) {
            h = new Hashtable();
        }
        try {
            XERCES1_VERSION_CLASS = "org.apache.xerces.framework.Version";
            h.put("version.xerces1", (String) ObjectFactory.findProviderClass("org.apache.xerces.framework.Version", ObjectFactory.findClassLoader(), true).getField("fVersion").get(null));
        } catch (Exception e) {
            h.put("version.xerces1", CLASS_NOTPRESENT);
        }
        try {
            XERCES1_VERSION_CLASS = "org.apache.xerces.impl.Version";
            h.put("version.xerces2", (String) ObjectFactory.findProviderClass("org.apache.xerces.impl.Version", ObjectFactory.findClassLoader(), true).getField("fVersion").get(null));
        } catch (Exception e2) {
            h.put("version.xerces2", CLASS_NOTPRESENT);
        }
        try {
            String CRIMSON_CLASS = "org.apache.crimson.parser.Parser2";
            Class clazz = ObjectFactory.findProviderClass("org.apache.crimson.parser.Parser2", ObjectFactory.findClassLoader(), true);
            h.put("version.crimson", CLASS_PRESENT);
        } catch (Exception e3) {
            h.put("version.crimson", CLASS_NOTPRESENT);
        }
    }

    protected void checkAntVersion(Hashtable h) {
        if (h == null) {
            h = new Hashtable();
        }
        try {
            String ANT_VERSION_CLASS = "org.apache.tools.ant.Main";
            String ANT_VERSION_METHOD = "getAntVersion";
            h.put("version.ant", (String) ObjectFactory.findProviderClass("org.apache.tools.ant.Main", ObjectFactory.findClassLoader(), true).getMethod("getAntVersion", new Class[0]).invoke(null, new Object[0]));
        } catch (Exception e) {
            h.put("version.ant", CLASS_NOTPRESENT);
        }
    }

    protected void checkDOMVersion(Hashtable h) {
        if (h == null) {
            h = new Hashtable();
        }
        String DOM_LEVEL2_CLASS = "org.w3c.dom.Document";
        String DOM_LEVEL2_METHOD = "createElementNS";
        String DOM_LEVEL2WD_CLASS = "org.w3c.dom.Node";
        String DOM_LEVEL2WD_METHOD = "supported";
        String DOM_LEVEL2FD_CLASS = "org.w3c.dom.Node";
        String DOM_LEVEL2FD_METHOD = "isSupported";
        Class[] twoStringArgs = new Class[]{String.class, String.class};
        try {
            Method method = ObjectFactory.findProviderClass("org.w3c.dom.Document", ObjectFactory.findClassLoader(), true).getMethod("createElementNS", twoStringArgs);
            h.put("version.DOM", "2.0");
            try {
                method = ObjectFactory.findProviderClass("org.w3c.dom.Node", ObjectFactory.findClassLoader(), true).getMethod("supported", twoStringArgs);
                h.put("ERROR.version.DOM.draftlevel", "2.0wd");
                h.put(ERROR, ERROR_FOUND);
            } catch (Exception e) {
                try {
                    method = ObjectFactory.findProviderClass("org.w3c.dom.Node", ObjectFactory.findClassLoader(), true).getMethod("isSupported", twoStringArgs);
                    h.put("version.DOM.draftlevel", "2.0fd");
                } catch (Exception e2) {
                    h.put("ERROR.version.DOM.draftlevel", "2.0unknown");
                    h.put(ERROR, ERROR_FOUND);
                }
            }
        } catch (Exception e3) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ERROR attempting to load DOM level 2 class: ");
            stringBuilder.append(e3.toString());
            h.put("ERROR.version.DOM", stringBuilder.toString());
            h.put(ERROR, ERROR_FOUND);
        }
    }

    protected void checkSAXVersion(Hashtable h) {
        if (h == null) {
            h = new Hashtable();
        }
        String SAX_VERSION1_CLASS = "org.xml.sax.Parser";
        String SAX_VERSION1_METHOD = "parse";
        String SAX_VERSION2_CLASS = "org.xml.sax.XMLReader";
        String SAX_VERSION2_METHOD = "parse";
        String SAX_VERSION2BETA_CLASSNF = "org.xml.sax.helpers.AttributesImpl";
        String SAX_VERSION2BETA_METHODNF = "setAttributes";
        Class[] oneStringArg = new Class[]{String.class};
        try {
            Method method = ObjectFactory.findProviderClass("org.xml.sax.helpers.AttributesImpl", ObjectFactory.findClassLoader(), true).getMethod("setAttributes", new Class[]{Attributes.class});
            h.put("version.SAX", "2.0");
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ERROR attempting to load SAX version 2 class: ");
            stringBuilder.append(e.toString());
            h.put("ERROR.version.SAX", stringBuilder.toString());
            h.put(ERROR, ERROR_FOUND);
            Method method2;
            try {
                method2 = ObjectFactory.findProviderClass("org.xml.sax.XMLReader", ObjectFactory.findClassLoader(), true).getMethod("parse", oneStringArg);
                h.put("version.SAX-backlevel", "2.0beta2-or-earlier");
            } catch (Exception e2) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ERROR attempting to load SAX version 2 class: ");
                stringBuilder2.append(e.toString());
                h.put("ERROR.version.SAX", stringBuilder2.toString());
                h.put(ERROR, ERROR_FOUND);
                try {
                    method2 = ObjectFactory.findProviderClass("org.xml.sax.Parser", ObjectFactory.findClassLoader(), true).getMethod("parse", oneStringArg);
                    h.put("version.SAX-backlevel", SerializerConstants.XMLVERSION10);
                } catch (Exception e3) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ERROR attempting to load SAX version 1 class: ");
                    stringBuilder2.append(e3.toString());
                    h.put("ERROR.version.SAX-backlevel", stringBuilder2.toString());
                }
            }
        }
    }

    static {
        jarVersions.put(new Long(857192), "xalan.jar from xalan-j_1_1");
        jarVersions.put(new Long(440237), "xalan.jar from xalan-j_1_2");
        jarVersions.put(new Long(436094), "xalan.jar from xalan-j_1_2_1");
        jarVersions.put(new Long(426249), "xalan.jar from xalan-j_1_2_2");
        jarVersions.put(new Long(702536), "xalan.jar from xalan-j_2_0_0");
        jarVersions.put(new Long(720930), "xalan.jar from xalan-j_2_0_1");
        jarVersions.put(new Long(732330), "xalan.jar from xalan-j_2_1_0");
        jarVersions.put(new Long(872241), "xalan.jar from xalan-j_2_2_D10");
        jarVersions.put(new Long(882739), "xalan.jar from xalan-j_2_2_D11");
        jarVersions.put(new Long(923866), "xalan.jar from xalan-j_2_2_0");
        jarVersions.put(new Long(905872), "xalan.jar from xalan-j_2_3_D1");
        jarVersions.put(new Long(906122), "xalan.jar from xalan-j_2_3_0");
        jarVersions.put(new Long(906248), "xalan.jar from xalan-j_2_3_1");
        jarVersions.put(new Long(983377), "xalan.jar from xalan-j_2_4_D1");
        jarVersions.put(new Long(997276), "xalan.jar from xalan-j_2_4_0");
        jarVersions.put(new Long(1031036), "xalan.jar from xalan-j_2_4_1");
        jarVersions.put(new Long(596540), "xsltc.jar from xalan-j_2_2_0");
        jarVersions.put(new Long(590247), "xsltc.jar from xalan-j_2_3_D1");
        jarVersions.put(new Long(589914), "xsltc.jar from xalan-j_2_3_0");
        jarVersions.put(new Long(589915), "xsltc.jar from xalan-j_2_3_1");
        jarVersions.put(new Long(1306667), "xsltc.jar from xalan-j_2_4_D1");
        jarVersions.put(new Long(1328227), "xsltc.jar from xalan-j_2_4_0");
        jarVersions.put(new Long(1344009), "xsltc.jar from xalan-j_2_4_1");
        jarVersions.put(new Long(1348361), "xsltc.jar from xalan-j_2_5_D1");
        jarVersions.put(new Long(1268634), "xsltc.jar-bundled from xalan-j_2_3_0");
        jarVersions.put(new Long(100196), "xml-apis.jar from xalan-j_2_2_0 or xalan-j_2_3_D1");
        jarVersions.put(new Long(108484), "xml-apis.jar from xalan-j_2_3_0, or xalan-j_2_3_1 from xml-commons-1.0.b2");
        jarVersions.put(new Long(109049), "xml-apis.jar from xalan-j_2_4_0 from xml-commons RIVERCOURT1 branch");
        jarVersions.put(new Long(113749), "xml-apis.jar from xalan-j_2_4_1 from factoryfinder-build of xml-commons RIVERCOURT1");
        jarVersions.put(new Long(124704), "xml-apis.jar from tck-jaxp-1_2_0 branch of xml-commons");
        jarVersions.put(new Long(124724), "xml-apis.jar from tck-jaxp-1_2_0 branch of xml-commons, tag: xml-commons-external_1_2_01");
        jarVersions.put(new Long(194205), "xml-apis.jar from head branch of xml-commons, tag: xml-commons-external_1_3_02");
        jarVersions.put(new Long(424490), "xalan.jar from Xerces Tools releases - ERROR:DO NOT USE!");
        jarVersions.put(new Long(1591855), "xerces.jar from xalan-j_1_1 from xerces-1...");
        jarVersions.put(new Long(1498679), "xerces.jar from xalan-j_1_2 from xerces-1_2_0.bin");
        jarVersions.put(new Long(1484896), "xerces.jar from xalan-j_1_2_1 from xerces-1_2_1.bin");
        jarVersions.put(new Long(804460), "xerces.jar from xalan-j_1_2_2 from xerces-1_2_2.bin");
        jarVersions.put(new Long(1499244), "xerces.jar from xalan-j_2_0_0 from xerces-1_2_3.bin");
        jarVersions.put(new Long(1605266), "xerces.jar from xalan-j_2_0_1 from xerces-1_3_0.bin");
        jarVersions.put(new Long(904030), "xerces.jar from xalan-j_2_1_0 from xerces-1_4.bin");
        jarVersions.put(new Long(904030), "xerces.jar from xerces-1_4_0.bin");
        jarVersions.put(new Long(1802885), "xerces.jar from xerces-1_4_2.bin");
        jarVersions.put(new Long(1734594), "xerces.jar from Xerces-J-bin.2.0.0.beta3");
        jarVersions.put(new Long(1808883), "xerces.jar from xalan-j_2_2_D10,D11,D12 or xerces-1_4_3.bin");
        jarVersions.put(new Long(1812019), "xerces.jar from xalan-j_2_2_0");
        jarVersions.put(new Long(1720292), "xercesImpl.jar from xalan-j_2_3_D1");
        jarVersions.put(new Long(1730053), "xercesImpl.jar from xalan-j_2_3_0 or xalan-j_2_3_1 from xerces-2_0_0");
        jarVersions.put(new Long(1728861), "xercesImpl.jar from xalan-j_2_4_D1 from xerces-2_0_1");
        jarVersions.put(new Long(972027), "xercesImpl.jar from xalan-j_2_4_0 from xerces-2_1");
        jarVersions.put(new Long(831587), "xercesImpl.jar from xalan-j_2_4_1 from xerces-2_2");
        jarVersions.put(new Long(891817), "xercesImpl.jar from xalan-j_2_5_D1 from xerces-2_3");
        jarVersions.put(new Long(895924), "xercesImpl.jar from xerces-2_4");
        jarVersions.put(new Long(1010806), "xercesImpl.jar from Xerces-J-bin.2.6.2");
        jarVersions.put(new Long(1203860), "xercesImpl.jar from Xerces-J-bin.2.7.1");
        jarVersions.put(new Long(37485), "xalanj1compat.jar from xalan-j_2_0_0");
        jarVersions.put(new Long(38100), "xalanj1compat.jar from xalan-j_2_0_1");
        jarVersions.put(new Long(18779), "xalanservlet.jar from xalan-j_2_0_0");
        jarVersions.put(new Long(21453), "xalanservlet.jar from xalan-j_2_0_1");
        jarVersions.put(new Long(24826), "xalanservlet.jar from xalan-j_2_3_1 or xalan-j_2_4_1");
        jarVersions.put(new Long(24831), "xalanservlet.jar from xalan-j_2_4_1");
        jarVersions.put(new Long(5618), "jaxp.jar from jaxp1.0.1");
        jarVersions.put(new Long(136133), "parser.jar from jaxp1.0.1");
        jarVersions.put(new Long(28404), "jaxp.jar from jaxp-1.1");
        jarVersions.put(new Long(187162), "crimson.jar from jaxp-1.1");
        jarVersions.put(new Long(801714), "xalan.jar from jaxp-1.1");
        jarVersions.put(new Long(196399), "crimson.jar from crimson-1.1.1");
        jarVersions.put(new Long(33323), "jaxp.jar from crimson-1.1.1 or jakarta-ant-1.4.1b1");
        jarVersions.put(new Long(152717), "crimson.jar from crimson-1.1.2beta2");
        jarVersions.put(new Long(88143), "xml-apis.jar from crimson-1.1.2beta2");
        jarVersions.put(new Long(206384), "crimson.jar from crimson-1.1.3 or jakarta-ant-1.4.1b1");
        jarVersions.put(new Long(136198), "parser.jar from jakarta-ant-1.3 or 1.2");
        jarVersions.put(new Long(5537), "jaxp.jar from jakarta-ant-1.3 or 1.2");
    }

    protected void logMsg(String s) {
        this.outWriter.println(s);
    }
}
