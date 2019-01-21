package org.apache.xml.utils;

import java.util.Hashtable;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.XMLReader;

public class XMLReaderManager {
    private static final String NAMESPACES_FEATURE = "http://xml.org/sax/features/namespaces";
    private static final String NAMESPACE_PREFIXES_FEATURE = "http://xml.org/sax/features/namespace-prefixes";
    private static SAXParserFactory m_parserFactory;
    private static final XMLReaderManager m_singletonManager = new XMLReaderManager();
    private Hashtable m_inUse;
    private ThreadLocal m_readers;

    private XMLReaderManager() {
    }

    public static XMLReaderManager getInstance() {
        return m_singletonManager;
    }

    /*  JADX ERROR: NullPointerException in pass: RegionMakerVisitor
        java.lang.NullPointerException
        	at java.util.BitSet.and(BitSet.java:916)
        	at jadx.core.utils.BlockUtils.getPathCross(BlockUtils.java:434)
        	at jadx.core.dex.visitors.regions.RegionMaker.processTryCatchBlocks(RegionMaker.java:925)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:52)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public synchronized org.xml.sax.XMLReader getXMLReader() throws org.xml.sax.SAXException {
        /*
        r6 = this;
        monitor-enter(r6);
        r0 = r6.m_readers;	 Catch:{ all -> 0x0099 }
        if (r0 != 0) goto L_0x000c;	 Catch:{ all -> 0x0099 }
    L_0x0005:
        r0 = new java.lang.ThreadLocal;	 Catch:{ all -> 0x0099 }
        r0.<init>();	 Catch:{ all -> 0x0099 }
        r6.m_readers = r0;	 Catch:{ all -> 0x0099 }
    L_0x000c:
        r0 = r6.m_inUse;	 Catch:{ all -> 0x0099 }
        if (r0 != 0) goto L_0x0017;	 Catch:{ all -> 0x0099 }
    L_0x0010:
        r0 = new java.util.Hashtable;	 Catch:{ all -> 0x0099 }
        r0.<init>();	 Catch:{ all -> 0x0099 }
        r6.m_inUse = r0;	 Catch:{ all -> 0x0099 }
    L_0x0017:
        r0 = r6.m_readers;	 Catch:{ all -> 0x0099 }
        r0 = r0.get();	 Catch:{ all -> 0x0099 }
        r0 = (org.xml.sax.XMLReader) r0;	 Catch:{ all -> 0x0099 }
        r1 = 0;	 Catch:{ all -> 0x0099 }
        r2 = 1;	 Catch:{ all -> 0x0099 }
        if (r0 == 0) goto L_0x0025;	 Catch:{ all -> 0x0099 }
    L_0x0023:
        r3 = r2;	 Catch:{ all -> 0x0099 }
        goto L_0x0026;	 Catch:{ all -> 0x0099 }
    L_0x0025:
        r3 = r1;	 Catch:{ all -> 0x0099 }
    L_0x0026:
        if (r3 == 0) goto L_0x003b;	 Catch:{ all -> 0x0099 }
    L_0x0028:
        r4 = r6.m_inUse;	 Catch:{ all -> 0x0099 }
        r4 = r4.get(r0);	 Catch:{ all -> 0x0099 }
        r5 = java.lang.Boolean.TRUE;	 Catch:{ all -> 0x0099 }
        if (r4 != r5) goto L_0x0033;	 Catch:{ all -> 0x0099 }
    L_0x0032:
        goto L_0x003b;	 Catch:{ all -> 0x0099 }
    L_0x0033:
        r1 = r6.m_inUse;	 Catch:{ all -> 0x0099 }
        r2 = java.lang.Boolean.TRUE;	 Catch:{ all -> 0x0099 }
        r1.put(r0, r2);	 Catch:{ all -> 0x0099 }
        goto L_0x0085;
    L_0x003b:
        r4 = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();	 Catch:{ Exception -> 0x0049 }
        r0 = r4;
        goto L_0x0065;
    L_0x0041:
        r1 = move-exception;
        goto L_0x0074;
    L_0x0043:
        r1 = move-exception;
        goto L_0x0075;
    L_0x0045:
        r1 = move-exception;
        goto L_0x0087;
    L_0x0047:
        r1 = move-exception;
        goto L_0x0092;
    L_0x0049:
        r4 = move-exception;
        r5 = m_parserFactory;	 Catch:{ ParserConfigurationException -> 0x0072, FactoryConfigurationError -> 0x0045, NoSuchMethodError -> 0x0043, AbstractMethodError | NoSuchMethodError -> 0x0041 }
        if (r5 != 0) goto L_0x0059;	 Catch:{ ParserConfigurationException -> 0x0072, FactoryConfigurationError -> 0x0045, NoSuchMethodError -> 0x0043, AbstractMethodError | NoSuchMethodError -> 0x0041 }
    L_0x004e:
        r5 = javax.xml.parsers.SAXParserFactory.newInstance();	 Catch:{ ParserConfigurationException -> 0x0072, FactoryConfigurationError -> 0x0045, NoSuchMethodError -> 0x0043, AbstractMethodError | NoSuchMethodError -> 0x0041 }
        m_parserFactory = r5;	 Catch:{ ParserConfigurationException -> 0x0072, FactoryConfigurationError -> 0x0045, NoSuchMethodError -> 0x0043, AbstractMethodError | NoSuchMethodError -> 0x0041 }
        r5 = m_parserFactory;	 Catch:{ ParserConfigurationException -> 0x0072, FactoryConfigurationError -> 0x0045, NoSuchMethodError -> 0x0043, AbstractMethodError | NoSuchMethodError -> 0x0041 }
        r5.setNamespaceAware(r2);	 Catch:{ ParserConfigurationException -> 0x0072, FactoryConfigurationError -> 0x0045, NoSuchMethodError -> 0x0043, AbstractMethodError | NoSuchMethodError -> 0x0041 }
    L_0x0059:
        r5 = m_parserFactory;	 Catch:{ ParserConfigurationException -> 0x0072, FactoryConfigurationError -> 0x0045, NoSuchMethodError -> 0x0043, AbstractMethodError | NoSuchMethodError -> 0x0041 }
        r5 = r5.newSAXParser();	 Catch:{ ParserConfigurationException -> 0x0072, FactoryConfigurationError -> 0x0045, NoSuchMethodError -> 0x0043, AbstractMethodError | NoSuchMethodError -> 0x0041 }
        r5 = r5.getXMLReader();	 Catch:{ ParserConfigurationException -> 0x0072, FactoryConfigurationError -> 0x0045, NoSuchMethodError -> 0x0043, AbstractMethodError | NoSuchMethodError -> 0x0041 }
        r0 = r5;
    L_0x0065:
        r4 = "http://xml.org/sax/features/namespaces";	 Catch:{ SAXException -> 0x0070 }
        r0.setFeature(r4, r2);	 Catch:{ SAXException -> 0x0070 }
        r2 = "http://xml.org/sax/features/namespace-prefixes";	 Catch:{ SAXException -> 0x0070 }
        r0.setFeature(r2, r1);	 Catch:{ SAXException -> 0x0070 }
        goto L_0x0076;
    L_0x0070:
        r1 = move-exception;
        goto L_0x0076;
    L_0x0072:
        r1 = move-exception;
        throw r1;	 Catch:{ ParserConfigurationException -> 0x0047, FactoryConfigurationError -> 0x0045, NoSuchMethodError -> 0x0043, AbstractMethodError | NoSuchMethodError -> 0x0041 }
    L_0x0074:
        goto L_0x0077;
    L_0x0077:
        if (r3 != 0) goto L_0x0085;
    L_0x0079:
        r1 = r6.m_readers;	 Catch:{ all -> 0x0099 }
        r1.set(r0);	 Catch:{ all -> 0x0099 }
        r1 = r6.m_inUse;	 Catch:{ all -> 0x0099 }
        r2 = java.lang.Boolean.TRUE;	 Catch:{ all -> 0x0099 }
        r1.put(r0, r2);	 Catch:{ all -> 0x0099 }
    L_0x0085:
        monitor-exit(r6);
        return r0;
        r2 = new org.xml.sax.SAXException;	 Catch:{ all -> 0x0099 }
        r4 = r1.toString();	 Catch:{ all -> 0x0099 }
        r2.<init>(r4);	 Catch:{ all -> 0x0099 }
        throw r2;	 Catch:{ all -> 0x0099 }
        r2 = new org.xml.sax.SAXException;	 Catch:{ all -> 0x0099 }
        r2.<init>(r1);	 Catch:{ all -> 0x0099 }
        throw r2;	 Catch:{ all -> 0x0099 }
    L_0x0099:
        r0 = move-exception;
        monitor-exit(r6);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.apache.xml.utils.XMLReaderManager.getXMLReader():org.xml.sax.XMLReader");
    }

    public synchronized void releaseXMLReader(XMLReader reader) {
        if (this.m_readers.get() == reader && reader != null) {
            this.m_inUse.remove(reader);
        }
    }
}
