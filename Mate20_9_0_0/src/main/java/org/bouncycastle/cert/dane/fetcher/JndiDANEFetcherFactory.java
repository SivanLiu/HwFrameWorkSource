package org.bouncycastle.cert.dane.fetcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.naming.Binding;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.bouncycastle.cert.dane.DANEEntry;
import org.bouncycastle.cert.dane.DANEEntryFetcher;
import org.bouncycastle.cert.dane.DANEEntryFetcherFactory;
import org.bouncycastle.cert.dane.DANEException;

public class JndiDANEFetcherFactory implements DANEEntryFetcherFactory {
    private static final String DANE_TYPE = "53";
    private List dnsServerList = new ArrayList();
    private boolean isAuthoritative;

    private void addEntries(List list, String str, Attribute attribute) throws NamingException, DANEException {
        for (int i = 0; i != attribute.size(); i++) {
            byte[] bArr = (byte[]) attribute.get(i);
            if (DANEEntry.isValidCertificate(bArr)) {
                try {
                    list.add(new DANEEntry(str, bArr));
                } catch (IOException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception parsing entry: ");
                    stringBuilder.append(e.getMessage());
                    throw new DANEException(stringBuilder.toString(), e);
                }
            }
        }
    }

    public DANEEntryFetcher build(final String str) {
        final Hashtable hashtable = new Hashtable();
        hashtable.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        hashtable.put("java.naming.authoritative", this.isAuthoritative ? "true" : "false");
        if (this.dnsServerList.size() > 0) {
            StringBuffer stringBuffer = new StringBuffer();
            for (Object append : this.dnsServerList) {
                if (stringBuffer.length() > 0) {
                    stringBuffer.append(" ");
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("dns://");
                stringBuilder.append(append);
                stringBuffer.append(stringBuilder.toString());
            }
            hashtable.put("java.naming.provider.url", stringBuffer.toString());
        }
        return new DANEEntryFetcher() {
            public List getEntries() throws DANEException {
                ArrayList arrayList = new ArrayList();
                StringBuilder stringBuilder;
                try {
                    InitialDirContext initialDirContext = new InitialDirContext(hashtable);
                    if (str.indexOf("_smimecert.") > 0) {
                        Attribute attribute = initialDirContext.getAttributes(str, new String[]{JndiDANEFetcherFactory.DANE_TYPE}).get(JndiDANEFetcherFactory.DANE_TYPE);
                        if (attribute != null) {
                            JndiDANEFetcherFactory.this.addEntries(arrayList, str, attribute);
                            return arrayList;
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("_smimecert.");
                    stringBuilder.append(str);
                    NamingEnumeration listBindings = initialDirContext.listBindings(stringBuilder.toString());
                    while (listBindings.hasMore()) {
                        DirContext dirContext = (DirContext) ((Binding) listBindings.next()).getObject();
                        Attribute attribute2 = initialDirContext.getAttributes(dirContext.getNameInNamespace().substring(1, dirContext.getNameInNamespace().length() - 1), new String[]{JndiDANEFetcherFactory.DANE_TYPE}).get(JndiDANEFetcherFactory.DANE_TYPE);
                        if (attribute2 != null) {
                            String nameInNamespace = dirContext.getNameInNamespace();
                            JndiDANEFetcherFactory.this.addEntries(arrayList, nameInNamespace.substring(1, nameInNamespace.length() - 1), attribute2);
                        }
                    }
                    return arrayList;
                } catch (NamingException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception dealing with DNS: ");
                    stringBuilder.append(e.getMessage());
                    throw new DANEException(stringBuilder.toString(), e);
                }
            }
        };
    }

    public JndiDANEFetcherFactory setAuthoritative(boolean z) {
        this.isAuthoritative = z;
        return this;
    }

    public JndiDANEFetcherFactory usingDNSServer(String str) {
        this.dnsServerList.add(str);
        return this;
    }
}
