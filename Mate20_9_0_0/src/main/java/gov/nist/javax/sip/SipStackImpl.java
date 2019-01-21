package gov.nist.javax.sip;

import gov.nist.core.Separators;
import gov.nist.core.ServerLogger;
import gov.nist.core.StackLogger;
import gov.nist.core.net.AddressResolver;
import gov.nist.core.net.NetworkLayer;
import gov.nist.core.net.SslNetworkLayer;
import gov.nist.javax.sip.clientauthutils.AccountManager;
import gov.nist.javax.sip.clientauthutils.AuthenticationHelper;
import gov.nist.javax.sip.clientauthutils.AuthenticationHelperImpl;
import gov.nist.javax.sip.clientauthutils.SecureAccountManager;
import gov.nist.javax.sip.parser.StringMsgParser;
import gov.nist.javax.sip.stack.DefaultMessageLogFactory;
import gov.nist.javax.sip.stack.DefaultRouter;
import gov.nist.javax.sip.stack.MessageProcessor;
import gov.nist.javax.sip.stack.SIPTransactionStack;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.ProviderDoesNotExistException;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.Router;
import javax.sip.header.HeaderFactory;
import org.ccil.cowan.tagsoup.HTMLModels;

public class SipStackImpl extends SIPTransactionStack implements SipStack, SipStackExt {
    public static final Integer MAX_DATAGRAM_SIZE = Integer.valueOf(HTMLModels.M_LEGEND);
    private String[] cipherSuites;
    boolean deliverTerminatedEventForAck;
    boolean deliverUnsolicitedNotify;
    private String[] enabledProtocols;
    private EventScanner eventScanner;
    private Hashtable<String, ListeningPointImpl> listeningPoints;
    boolean reEntrantListener;
    SipListener sipListener;
    private LinkedList<SipProviderImpl> sipProviders;
    private Semaphore stackSemaphore;

    protected SipStackImpl() {
        this.deliverTerminatedEventForAck = false;
        this.deliverUnsolicitedNotify = false;
        this.stackSemaphore = new Semaphore(1);
        this.cipherSuites = new String[]{"TLS_RSA_WITH_AES_128_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_DH_anon_WITH_AES_128_CBC_SHA", "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA"};
        this.enabledProtocols = new String[]{"SSLv3", "SSLv2Hello", "TLSv1"};
        super.setMessageFactory(new NistSipMessageFactoryImpl(this));
        this.eventScanner = new EventScanner(this);
        this.listeningPoints = new Hashtable();
        this.sipProviders = new LinkedList();
    }

    private void reInitialize() {
        super.reInit();
        this.eventScanner = new EventScanner(this);
        this.listeningPoints = new Hashtable();
        this.sipProviders = new LinkedList();
        this.sipListener = null;
    }

    boolean isAutomaticDialogSupportEnabled() {
        return this.isAutomaticDialogSupportEnabled;
    }

    /* JADX WARNING: Removed duplicated region for block: B:134:0x0361  */
    /* JADX WARNING: Removed duplicated region for block: B:126:0x0322 A:{SYNTHETIC, Splitter:B:126:0x0322} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x03b0  */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x036e A:{SYNTHETIC, Splitter:B:137:0x036e} */
    /* JADX WARNING: Removed duplicated region for block: B:181:0x044a  */
    /* JADX WARNING: Removed duplicated region for block: B:159:0x03f0 A:{SYNTHETIC, Splitter:B:159:0x03f0} */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x0456  */
    /* JADX WARNING: Removed duplicated region for block: B:195:0x0482 A:{Catch:{ NumberFormatException -> 0x048d }} */
    /* JADX WARNING: Removed duplicated region for block: B:187:0x0468 A:{SYNTHETIC, Splitter:B:187:0x0468} */
    /* JADX WARNING: Removed duplicated region for block: B:223:0x0524  */
    /* JADX WARNING: Removed duplicated region for block: B:210:0x04d6 A:{SYNTHETIC, Splitter:B:210:0x04d6} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0588  */
    /* JADX WARNING: Removed duplicated region for block: B:226:0x0555 A:{SYNTHETIC, Splitter:B:226:0x0555} */
    /* JADX WARNING: Removed duplicated region for block: B:246:0x05ce  */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x05a9  */
    /* JADX WARNING: Removed duplicated region for block: B:250:0x0621  */
    /* JADX WARNING: Removed duplicated region for block: B:249:0x0606  */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x0663  */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x062f  */
    /* JADX WARNING: Removed duplicated region for block: B:116:0x02e1 A:{SYNTHETIC, Splitter:B:116:0x02e1} */
    /* JADX WARNING: Removed duplicated region for block: B:126:0x0322 A:{SYNTHETIC, Splitter:B:126:0x0322} */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x0361  */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x036e A:{SYNTHETIC, Splitter:B:137:0x036e} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x03b0  */
    /* JADX WARNING: Removed duplicated region for block: B:159:0x03f0 A:{SYNTHETIC, Splitter:B:159:0x03f0} */
    /* JADX WARNING: Removed duplicated region for block: B:181:0x044a  */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x0456  */
    /* JADX WARNING: Removed duplicated region for block: B:187:0x0468 A:{SYNTHETIC, Splitter:B:187:0x0468} */
    /* JADX WARNING: Removed duplicated region for block: B:195:0x0482 A:{Catch:{ NumberFormatException -> 0x048d }} */
    /* JADX WARNING: Removed duplicated region for block: B:204:0x04c1  */
    /* JADX WARNING: Removed duplicated region for block: B:210:0x04d6 A:{SYNTHETIC, Splitter:B:210:0x04d6} */
    /* JADX WARNING: Removed duplicated region for block: B:223:0x0524  */
    /* JADX WARNING: Removed duplicated region for block: B:226:0x0555 A:{SYNTHETIC, Splitter:B:226:0x0555} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0588  */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x05a9  */
    /* JADX WARNING: Removed duplicated region for block: B:246:0x05ce  */
    /* JADX WARNING: Removed duplicated region for block: B:249:0x0606  */
    /* JADX WARNING: Removed duplicated region for block: B:250:0x0621  */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x062f  */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x0663  */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x0456  */
    /* JADX WARNING: Removed duplicated region for block: B:195:0x0482 A:{Catch:{ NumberFormatException -> 0x048d }} */
    /* JADX WARNING: Removed duplicated region for block: B:187:0x0468 A:{SYNTHETIC, Splitter:B:187:0x0468} */
    /* JADX WARNING: Removed duplicated region for block: B:204:0x04c1  */
    /* JADX WARNING: Removed duplicated region for block: B:223:0x0524  */
    /* JADX WARNING: Removed duplicated region for block: B:210:0x04d6 A:{SYNTHETIC, Splitter:B:210:0x04d6} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0588  */
    /* JADX WARNING: Removed duplicated region for block: B:226:0x0555 A:{SYNTHETIC, Splitter:B:226:0x0555} */
    /* JADX WARNING: Removed duplicated region for block: B:246:0x05ce  */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x05a9  */
    /* JADX WARNING: Removed duplicated region for block: B:250:0x0621  */
    /* JADX WARNING: Removed duplicated region for block: B:249:0x0606  */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x0663  */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x062f  */
    /* JADX WARNING: Removed duplicated region for block: B:180:0x042f  */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x0456  */
    /* JADX WARNING: Removed duplicated region for block: B:187:0x0468 A:{SYNTHETIC, Splitter:B:187:0x0468} */
    /* JADX WARNING: Removed duplicated region for block: B:195:0x0482 A:{Catch:{ NumberFormatException -> 0x048d }} */
    /* JADX WARNING: Removed duplicated region for block: B:204:0x04c1  */
    /* JADX WARNING: Removed duplicated region for block: B:210:0x04d6 A:{SYNTHETIC, Splitter:B:210:0x04d6} */
    /* JADX WARNING: Removed duplicated region for block: B:223:0x0524  */
    /* JADX WARNING: Removed duplicated region for block: B:226:0x0555 A:{SYNTHETIC, Splitter:B:226:0x0555} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0588  */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x05a9  */
    /* JADX WARNING: Removed duplicated region for block: B:246:0x05ce  */
    /* JADX WARNING: Removed duplicated region for block: B:249:0x0606  */
    /* JADX WARNING: Removed duplicated region for block: B:250:0x0621  */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x062f  */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x0663  */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0588  */
    /* JADX WARNING: Removed duplicated region for block: B:226:0x0555 A:{SYNTHETIC, Splitter:B:226:0x0555} */
    /* JADX WARNING: Removed duplicated region for block: B:246:0x05ce  */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x05a9  */
    /* JADX WARNING: Removed duplicated region for block: B:250:0x0621  */
    /* JADX WARNING: Removed duplicated region for block: B:249:0x0606  */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x0663  */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x062f  */
    /* JADX WARNING: Removed duplicated region for block: B:222:0x0521  */
    /* JADX WARNING: Removed duplicated region for block: B:221:0x04fa  */
    /* JADX WARNING: Removed duplicated region for block: B:226:0x0555 A:{SYNTHETIC, Splitter:B:226:0x0555} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0588  */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x05a9  */
    /* JADX WARNING: Removed duplicated region for block: B:246:0x05ce  */
    /* JADX WARNING: Removed duplicated region for block: B:249:0x0606  */
    /* JADX WARNING: Removed duplicated region for block: B:250:0x0621  */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x062f  */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x0663  */
    /* JADX WARNING: Removed duplicated region for block: B:246:0x05ce  */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x05a9  */
    /* JADX WARNING: Removed duplicated region for block: B:250:0x0621  */
    /* JADX WARNING: Removed duplicated region for block: B:249:0x0606  */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x0663  */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x062f  */
    /* JADX WARNING: Removed duplicated region for block: B:180:0x042f  */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x0456  */
    /* JADX WARNING: Removed duplicated region for block: B:195:0x0482 A:{Catch:{ NumberFormatException -> 0x048d }} */
    /* JADX WARNING: Removed duplicated region for block: B:187:0x0468 A:{SYNTHETIC, Splitter:B:187:0x0468} */
    /* JADX WARNING: Removed duplicated region for block: B:204:0x04c1  */
    /* JADX WARNING: Removed duplicated region for block: B:223:0x0524  */
    /* JADX WARNING: Removed duplicated region for block: B:210:0x04d6 A:{SYNTHETIC, Splitter:B:210:0x04d6} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0588  */
    /* JADX WARNING: Removed duplicated region for block: B:226:0x0555 A:{SYNTHETIC, Splitter:B:226:0x0555} */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x05a9  */
    /* JADX WARNING: Removed duplicated region for block: B:246:0x05ce  */
    /* JADX WARNING: Removed duplicated region for block: B:249:0x0606  */
    /* JADX WARNING: Removed duplicated region for block: B:250:0x0621  */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x062f  */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x0663  */
    /* JADX WARNING: Removed duplicated region for block: B:204:0x04c1  */
    /* JADX WARNING: Removed duplicated region for block: B:210:0x04d6 A:{SYNTHETIC, Splitter:B:210:0x04d6} */
    /* JADX WARNING: Removed duplicated region for block: B:223:0x0524  */
    /* JADX WARNING: Removed duplicated region for block: B:226:0x0555 A:{SYNTHETIC, Splitter:B:226:0x0555} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0588  */
    /* JADX WARNING: Removed duplicated region for block: B:246:0x05ce  */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x05a9  */
    /* JADX WARNING: Removed duplicated region for block: B:250:0x0621  */
    /* JADX WARNING: Removed duplicated region for block: B:249:0x0606  */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x0663  */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x062f  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x04b5  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0494  */
    /* JADX WARNING: Removed duplicated region for block: B:204:0x04c1  */
    /* JADX WARNING: Removed duplicated region for block: B:223:0x0524  */
    /* JADX WARNING: Removed duplicated region for block: B:210:0x04d6 A:{SYNTHETIC, Splitter:B:210:0x04d6} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0588  */
    /* JADX WARNING: Removed duplicated region for block: B:226:0x0555 A:{SYNTHETIC, Splitter:B:226:0x0555} */
    /* JADX WARNING: Removed duplicated region for block: B:241:0x05a9  */
    /* JADX WARNING: Removed duplicated region for block: B:246:0x05ce  */
    /* JADX WARNING: Removed duplicated region for block: B:249:0x0606  */
    /* JADX WARNING: Removed duplicated region for block: B:250:0x0621  */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x062f  */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x0663  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SipStackImpl(Properties configurationProperties) throws PeerUnavailableException {
        Exception e;
        String str;
        StringBuilder stringBuilder;
        StackLogger stackLogger;
        NumberFormatException ex;
        String str2;
        String str3;
        boolean z;
        InputStream in;
        StackLogger stackLogger2;
        StringBuilder stringBuilder2;
        String str4;
        IOException ex2;
        InputStream inputStream;
        Properties properties = configurationProperties;
        this();
        String address = properties.getProperty("javax.sip.IP_ADDRESS");
        if (address != null) {
            try {
                super.setHostAddress(address);
            } catch (UnknownHostException ex3) {
                UnknownHostException unknownHostException = ex3;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("bad address ");
                stringBuilder3.append(address);
                throw new PeerUnavailableException(stringBuilder3.toString());
            }
        }
        String name = properties.getProperty("javax.sip.STACK_NAME");
        String str5;
        if (name != null) {
            super.setStackName(name);
            String stackLoggerClassName = properties.getProperty("gov.nist.javax.sip.STACK_LOGGER");
            if (stackLoggerClassName == null) {
                stackLoggerClassName = "gov.nist.core.LogWriter";
            }
            String stackLoggerClassName2 = stackLoggerClassName;
            try {
                StackLogger stackLogger3 = (StackLogger) Class.forName(stackLoggerClassName2).getConstructor(new Class[0]).newInstance(new Object[0]);
                stackLogger3.setStackProperties(properties);
                super.setStackLogger(stackLogger3);
                stackLoggerClassName = properties.getProperty("gov.nist.javax.sip.SERVER_LOGGER");
                if (stackLoggerClassName == null) {
                    stackLoggerClassName = "gov.nist.javax.sip.stack.ServerLog";
                }
                String serverLoggerClassName = stackLoggerClassName;
                try {
                    this.serverLogger = (ServerLogger) Class.forName(serverLoggerClassName).getConstructor(new Class[0]).newInstance(new Object[0]);
                    this.serverLogger.setSipStack(this);
                    this.serverLogger.setStackProperties(properties);
                    this.outboundProxy = properties.getProperty("javax.sip.OUTBOUND_PROXY");
                    this.defaultRouter = new DefaultRouter(this, this.outboundProxy);
                    stackLoggerClassName = properties.getProperty("javax.sip.ROUTER_PATH");
                    if (stackLoggerClassName == null) {
                        stackLoggerClassName = "gov.nist.javax.sip.stack.DefaultRouter";
                    }
                    try {
                        StringTokenizer st;
                        String em;
                        StringBuilder stringBuilder4;
                        String path;
                        Class<?> clazz;
                        String path2;
                        boolean z2;
                        super.setRouter((Router) Class.forName(stackLoggerClassName).getConstructor(new Class[]{SipStack.class, String.class}).newInstance(new Object[]{this, this.outboundProxy}));
                        String useRouterForAll = properties.getProperty("javax.sip.USE_ROUTER_FOR_ALL_URIS");
                        this.useRouterForAll = true;
                        if (useRouterForAll != null) {
                            this.useRouterForAll = "true".equalsIgnoreCase(useRouterForAll);
                        }
                        String extensionMethods = properties.getProperty("javax.sip.EXTENSION_METHODS");
                        if (extensionMethods != null) {
                            st = new StringTokenizer(extensionMethods);
                            while (st.hasMoreTokens()) {
                                em = st.nextToken(Separators.COLON);
                                if (em.equalsIgnoreCase("BYE") || em.equalsIgnoreCase("INVITE") || em.equalsIgnoreCase("SUBSCRIBE") || em.equalsIgnoreCase("NOTIFY") || em.equalsIgnoreCase("ACK") || em.equalsIgnoreCase("OPTIONS")) {
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("Bad extension method ");
                                    stringBuilder4.append(em);
                                    throw new PeerUnavailableException(stringBuilder4.toString());
                                }
                                addExtensionMethod(em);
                            }
                        }
                        em = properties.getProperty("javax.net.ssl.keyStore");
                        stackLoggerClassName = properties.getProperty("javax.net.ssl.trustStore");
                        if (em != null) {
                            if (stackLoggerClassName == null) {
                                stackLoggerClassName = em;
                            }
                            try {
                                this.networkLayer = new SslNetworkLayer(stackLoggerClassName, em, properties.getProperty("javax.net.ssl.keyStorePassword").toCharArray(), properties.getProperty("javax.net.ssl.keyStoreType"));
                            } catch (Exception stackLoggerClassName3) {
                                getStackLogger().logError("could not instantiate SSL networking", stackLoggerClassName3);
                            }
                        }
                        this.isAutomaticDialogSupportEnabled = properties.getProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", "on").equalsIgnoreCase("on");
                        this.isAutomaticDialogErrorHandlingEnabled = properties.getProperty("gov.nist.javax.sip.AUTOMATIC_DIALOG_ERROR_HANDLING", "true").equals(Boolean.TRUE.toString());
                        if (this.isAutomaticDialogSupportEnabled) {
                            this.isAutomaticDialogErrorHandlingEnabled = true;
                        }
                        if (properties.getProperty("gov.nist.javax.sip.MAX_LISTENER_RESPONSE_TIME") != null) {
                            this.maxListenerResponseTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_LISTENER_RESPONSE_TIME"));
                            if (this.maxListenerResponseTime <= 0) {
                                throw new PeerUnavailableException("Bad configuration parameter gov.nist.javax.sip.MAX_LISTENER_RESPONSE_TIME : should be positive");
                            }
                        }
                        this.maxListenerResponseTime = -1;
                        this.deliverTerminatedEventForAck = properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_ACK", "false").equalsIgnoreCase("true");
                        this.deliverUnsolicitedNotify = properties.getProperty("gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY", "false").equalsIgnoreCase("true");
                        String forkedSubscriptions = properties.getProperty("javax.sip.FORKABLE_EVENTS");
                        if (forkedSubscriptions != null) {
                            st = new StringTokenizer(forkedSubscriptions);
                            while (st.hasMoreTokens()) {
                                this.forkedEvents.add(st.nextToken());
                            }
                        }
                        String NETWORK_LAYER_KEY = "gov.nist.javax.sip.NETWORK_LAYER";
                        if (properties.containsKey("gov.nist.javax.sip.NETWORK_LAYER")) {
                            path = properties.getProperty("gov.nist.javax.sip.NETWORK_LAYER");
                            try {
                                clazz = Class.forName(path);
                                try {
                                    this.networkLayer = (NetworkLayer) clazz.getConstructor(new Class[0]).newInstance(new Object[0]);
                                } catch (Exception e2) {
                                    e = e2;
                                }
                            } catch (Exception e3) {
                                e = e3;
                                str = address;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("can't find or instantiate NetworkLayer implementation: ");
                                stringBuilder4.append(path);
                                throw new PeerUnavailableException(stringBuilder4.toString());
                            }
                        }
                        address = "gov.nist.javax.sip.ADDRESS_RESOLVER";
                        if (properties.containsKey("gov.nist.javax.sip.ADDRESS_RESOLVER")) {
                            path2 = properties.getProperty("gov.nist.javax.sip.ADDRESS_RESOLVER");
                            try {
                                clazz = Class.forName(path2);
                                try {
                                    this.addressResolver = (AddressResolver) clazz.getConstructor(new Class[0]).newInstance(new Object[0]);
                                } catch (Exception e4) {
                                    e = e4;
                                }
                            } catch (Exception e5) {
                                e = e5;
                                String str6 = address;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("can't find or instantiate AddressResolver implementation: ");
                                stringBuilder.append(path2);
                                throw new PeerUnavailableException(stringBuilder.toString());
                            }
                        }
                        address = properties.getProperty("gov.nist.javax.sip.MAX_CONNECTIONS");
                        if (address != null) {
                            try {
                                this.maxConnections = new Integer(address).intValue();
                                String str7 = address;
                            } catch (NumberFormatException ex4) {
                                if (isLoggingEnabled()) {
                                    stackLogger = getStackLogger();
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("max connections - bad value ");
                                    stringBuilder.append(ex4.getMessage());
                                    stackLogger.logError(stringBuilder.toString());
                                }
                            }
                            address = properties.getProperty("gov.nist.javax.sip.THREAD_POOL_SIZE");
                            if (address != null) {
                                try {
                                    this.threadPoolSize = new Integer(address).intValue();
                                    String str8 = address;
                                } catch (NumberFormatException ex42) {
                                    if (isLoggingEnabled()) {
                                        stackLogger = getStackLogger();
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("thread pool size - bad value ");
                                        stringBuilder.append(ex42.getMessage());
                                        stackLogger.logError(stringBuilder.toString());
                                    }
                                }
                                address = properties.getProperty("gov.nist.javax.sip.MAX_SERVER_TRANSACTIONS");
                                if (address != null) {
                                    try {
                                        this.serverTransactionTableHighwaterMark = new Integer(address).intValue();
                                        this.serverTransactionTableLowaterMark = (this.serverTransactionTableHighwaterMark * 80) / 100;
                                        String str9 = address;
                                    } catch (NumberFormatException ex422) {
                                        if (isLoggingEnabled()) {
                                            StackLogger stackLogger4 = getStackLogger();
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("transaction table size - bad value ");
                                            stringBuilder4.append(ex422.getMessage());
                                            stackLogger4.logError(stringBuilder4.toString());
                                        }
                                    }
                                } else {
                                    this.unlimitedServerTransactionTableSize = true;
                                }
                                address = properties.getProperty("gov.nist.javax.sip.MAX_CLIENT_TRANSACTIONS");
                                if (address != null) {
                                    try {
                                        this.clientTransactionTableHiwaterMark = new Integer(address).intValue();
                                        this.clientTransactionTableLowaterMark = (this.clientTransactionTableLowaterMark * 80) / 100;
                                        String str10 = address;
                                    } catch (NumberFormatException ex4222) {
                                        if (isLoggingEnabled()) {
                                            stackLogger = getStackLogger();
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("transaction table size - bad value ");
                                            stringBuilder.append(ex4222.getMessage());
                                            stackLogger.logError(stringBuilder.toString());
                                        }
                                    }
                                    z2 = true;
                                } else {
                                    z2 = true;
                                    this.unlimitedClientTransactionTableSize = true;
                                }
                                this.cacheServerConnections = z2;
                                address = properties.getProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS");
                                if (address != null && "false".equalsIgnoreCase(address.trim())) {
                                    this.cacheServerConnections = false;
                                }
                                this.cacheClientConnections = true;
                                path2 = properties.getProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS");
                                if (path2 != null && "false".equalsIgnoreCase(path2.trim())) {
                                    this.cacheClientConnections = false;
                                }
                                path = properties.getProperty("gov.nist.javax.sip.READ_TIMEOUT");
                                if (path != null) {
                                    try {
                                        int rt = Integer.parseInt(path);
                                        if (rt >= 100) {
                                            try {
                                                this.readTimeout = rt;
                                                str5 = name;
                                            } catch (NumberFormatException e6) {
                                                ex4222 = e6;
                                                str5 = name;
                                                if (isLoggingEnabled()) {
                                                }
                                                address = properties.getProperty("gov.nist.javax.sip.STUN_SERVER");
                                                if (address != null) {
                                                }
                                                name = properties.getProperty("gov.nist.javax.sip.MAX_MESSAGE_SIZE");
                                                if (name == null) {
                                                }
                                                str2 = name;
                                                str3 = forkedSubscriptions;
                                                address = properties.getProperty("gov.nist.javax.sip.REENTRANT_LISTENER");
                                                if (address == null) {
                                                }
                                                this.reEntrantListener = z;
                                                name = properties.getProperty("gov.nist.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS");
                                                if (name == null) {
                                                }
                                                setNon2XXAckPassedToListener(Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "false")).booleanValue());
                                                this.generateTimeStampHeader = Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.AUTO_GENERATE_TIMESTAMP", "false")).booleanValue();
                                                address = properties.getProperty("gov.nist.javax.sip.LOG_FACTORY");
                                                if (address == null) {
                                                }
                                                StringMsgParser.setComputeContentLengthFromMessage(properties.getProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false").equalsIgnoreCase("true"));
                                                serverLoggerClassName = properties.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
                                                if (serverLoggerClassName != null) {
                                                }
                                                this.rfc2543Supported = properties.getProperty("gov.nist.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");
                                                this.cancelClientTransactionChecked = properties.getProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase("true");
                                                this.logStackTraceOnMessageSend = properties.getProperty("gov.nist.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
                                                if (isLoggingEnabled()) {
                                                }
                                                in = getClass().getResourceAsStream("/TIMESTAMP");
                                                if (in != null) {
                                                }
                                                stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                                super.setReceiveUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                                stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                                super.setSendUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                                z = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                                                this.stackDoesCongestionControl = z;
                                                this.isBackToBackUserAgent = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                                                this.checkBranchId = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                                                this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                                                this.maxForkTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                                                return;
                                            }
                                        }
                                        try {
                                            PrintStream printStream = System.err;
                                            StringBuilder stringBuilder5 = new StringBuilder();
                                            try {
                                                stringBuilder5.append("Value too low ");
                                                stringBuilder5.append(path);
                                                printStream.println(stringBuilder5.toString());
                                            } catch (NumberFormatException e7) {
                                                ex4222 = e7;
                                            }
                                        } catch (NumberFormatException e8) {
                                            ex4222 = e8;
                                            str5 = name;
                                            if (isLoggingEnabled()) {
                                            }
                                            address = properties.getProperty("gov.nist.javax.sip.STUN_SERVER");
                                            if (address != null) {
                                            }
                                            name = properties.getProperty("gov.nist.javax.sip.MAX_MESSAGE_SIZE");
                                            if (name == null) {
                                            }
                                            str2 = name;
                                            str3 = forkedSubscriptions;
                                            address = properties.getProperty("gov.nist.javax.sip.REENTRANT_LISTENER");
                                            if (address == null) {
                                            }
                                            this.reEntrantListener = z;
                                            name = properties.getProperty("gov.nist.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS");
                                            if (name == null) {
                                            }
                                            setNon2XXAckPassedToListener(Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "false")).booleanValue());
                                            this.generateTimeStampHeader = Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.AUTO_GENERATE_TIMESTAMP", "false")).booleanValue();
                                            address = properties.getProperty("gov.nist.javax.sip.LOG_FACTORY");
                                            if (address == null) {
                                            }
                                            StringMsgParser.setComputeContentLengthFromMessage(properties.getProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false").equalsIgnoreCase("true"));
                                            serverLoggerClassName = properties.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
                                            if (serverLoggerClassName != null) {
                                            }
                                            this.rfc2543Supported = properties.getProperty("gov.nist.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");
                                            this.cancelClientTransactionChecked = properties.getProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase("true");
                                            this.logStackTraceOnMessageSend = properties.getProperty("gov.nist.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
                                            if (isLoggingEnabled()) {
                                            }
                                            in = getClass().getResourceAsStream("/TIMESTAMP");
                                            if (in != null) {
                                            }
                                            stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                            super.setReceiveUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                            stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                            super.setSendUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                            z = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                                            this.stackDoesCongestionControl = z;
                                            this.isBackToBackUserAgent = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                                            this.checkBranchId = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                                            this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                                            this.maxForkTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                                            return;
                                        }
                                    } catch (NumberFormatException e9) {
                                        ex4222 = e9;
                                        String str11 = address;
                                        str5 = name;
                                        if (isLoggingEnabled()) {
                                            stackLogger2 = getStackLogger();
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Bad read timeout ");
                                            stringBuilder2.append(path);
                                            stackLogger2.logError(stringBuilder2.toString());
                                        }
                                        address = properties.getProperty("gov.nist.javax.sip.STUN_SERVER");
                                        if (address != null) {
                                        }
                                        name = properties.getProperty("gov.nist.javax.sip.MAX_MESSAGE_SIZE");
                                        if (name == null) {
                                        }
                                        str2 = name;
                                        str3 = forkedSubscriptions;
                                        address = properties.getProperty("gov.nist.javax.sip.REENTRANT_LISTENER");
                                        if (address == null) {
                                        }
                                        this.reEntrantListener = z;
                                        name = properties.getProperty("gov.nist.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS");
                                        if (name == null) {
                                        }
                                        setNon2XXAckPassedToListener(Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "false")).booleanValue());
                                        this.generateTimeStampHeader = Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.AUTO_GENERATE_TIMESTAMP", "false")).booleanValue();
                                        address = properties.getProperty("gov.nist.javax.sip.LOG_FACTORY");
                                        if (address == null) {
                                        }
                                        StringMsgParser.setComputeContentLengthFromMessage(properties.getProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false").equalsIgnoreCase("true"));
                                        serverLoggerClassName = properties.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
                                        if (serverLoggerClassName != null) {
                                        }
                                        this.rfc2543Supported = properties.getProperty("gov.nist.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");
                                        this.cancelClientTransactionChecked = properties.getProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase("true");
                                        this.logStackTraceOnMessageSend = properties.getProperty("gov.nist.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
                                        if (isLoggingEnabled()) {
                                        }
                                        in = getClass().getResourceAsStream("/TIMESTAMP");
                                        if (in != null) {
                                        }
                                        stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                        super.setReceiveUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                        stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                        super.setSendUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                        z = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                                        this.stackDoesCongestionControl = z;
                                        this.isBackToBackUserAgent = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                                        this.checkBranchId = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                                        this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                                        this.maxForkTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                                        return;
                                    }
                                }
                                str5 = name;
                                address = properties.getProperty("gov.nist.javax.sip.STUN_SERVER");
                                if (address != null) {
                                    getStackLogger().logWarning("Ignoring obsolete property gov.nist.javax.sip.STUN_SERVER");
                                }
                                name = properties.getProperty("gov.nist.javax.sip.MAX_MESSAGE_SIZE");
                                if (name == null) {
                                    try {
                                        this.maxMessageSize = new Integer(name).intValue();
                                        if (this.maxMessageSize < 4096) {
                                            try {
                                                this.maxMessageSize = 4096;
                                            } catch (NumberFormatException e10) {
                                                ex4222 = e10;
                                                if (isLoggingEnabled()) {
                                                }
                                                address = properties.getProperty("gov.nist.javax.sip.REENTRANT_LISTENER");
                                                if (address == null) {
                                                }
                                                this.reEntrantListener = z;
                                                name = properties.getProperty("gov.nist.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS");
                                                if (name == null) {
                                                }
                                                setNon2XXAckPassedToListener(Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "false")).booleanValue());
                                                this.generateTimeStampHeader = Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.AUTO_GENERATE_TIMESTAMP", "false")).booleanValue();
                                                address = properties.getProperty("gov.nist.javax.sip.LOG_FACTORY");
                                                if (address == null) {
                                                }
                                                StringMsgParser.setComputeContentLengthFromMessage(properties.getProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false").equalsIgnoreCase("true"));
                                                serverLoggerClassName = properties.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
                                                if (serverLoggerClassName != null) {
                                                }
                                                this.rfc2543Supported = properties.getProperty("gov.nist.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");
                                                this.cancelClientTransactionChecked = properties.getProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase("true");
                                                this.logStackTraceOnMessageSend = properties.getProperty("gov.nist.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
                                                if (isLoggingEnabled()) {
                                                }
                                                in = getClass().getResourceAsStream("/TIMESTAMP");
                                                if (in != null) {
                                                }
                                                stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                                super.setReceiveUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                                stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                                super.setSendUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                                z = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                                                this.stackDoesCongestionControl = z;
                                                this.isBackToBackUserAgent = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                                                this.checkBranchId = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                                                this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                                                this.maxForkTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                                                return;
                                            }
                                        }
                                    } catch (NumberFormatException e11) {
                                        ex4222 = e11;
                                        String str12 = address;
                                        if (isLoggingEnabled()) {
                                            stackLogger2 = getStackLogger();
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("maxMessageSize - bad value ");
                                            stringBuilder2.append(ex4222.getMessage());
                                            stackLogger2.logError(stringBuilder2.toString());
                                        } else {
                                            str3 = forkedSubscriptions;
                                        }
                                        address = properties.getProperty("gov.nist.javax.sip.REENTRANT_LISTENER");
                                        if (address == null) {
                                        }
                                        this.reEntrantListener = z;
                                        name = properties.getProperty("gov.nist.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS");
                                        if (name == null) {
                                        }
                                        setNon2XXAckPassedToListener(Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "false")).booleanValue());
                                        this.generateTimeStampHeader = Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.AUTO_GENERATE_TIMESTAMP", "false")).booleanValue();
                                        address = properties.getProperty("gov.nist.javax.sip.LOG_FACTORY");
                                        if (address == null) {
                                        }
                                        StringMsgParser.setComputeContentLengthFromMessage(properties.getProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false").equalsIgnoreCase("true"));
                                        serverLoggerClassName = properties.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
                                        if (serverLoggerClassName != null) {
                                        }
                                        this.rfc2543Supported = properties.getProperty("gov.nist.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");
                                        this.cancelClientTransactionChecked = properties.getProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase("true");
                                        this.logStackTraceOnMessageSend = properties.getProperty("gov.nist.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
                                        if (isLoggingEnabled()) {
                                        }
                                        in = getClass().getResourceAsStream("/TIMESTAMP");
                                        if (in != null) {
                                        }
                                        stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                        super.setReceiveUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                        stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                        super.setSendUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                        z = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                                        this.stackDoesCongestionControl = z;
                                        this.isBackToBackUserAgent = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                                        this.checkBranchId = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                                        this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                                        this.maxForkTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                                        return;
                                    }
                                }
                                this.maxMessageSize = 0;
                                str2 = name;
                                str3 = forkedSubscriptions;
                                address = properties.getProperty("gov.nist.javax.sip.REENTRANT_LISTENER");
                                z = address == null && "true".equalsIgnoreCase(address);
                                this.reEntrantListener = z;
                                name = properties.getProperty("gov.nist.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS");
                                if (name == null) {
                                    try {
                                        try {
                                            getThreadAuditor().setPingIntervalInMillisecs(Long.valueOf(name).longValue() / 2);
                                            String str13 = address;
                                        } catch (NumberFormatException e12) {
                                            ex4222 = e12;
                                            if (isLoggingEnabled()) {
                                            } else {
                                                StackLogger stackLogger5 = getStackLogger();
                                                StringBuilder stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("THREAD_AUDIT_INTERVAL_IN_MILLISECS - bad value [");
                                                stringBuilder6.append(name);
                                                stringBuilder6.append("] ");
                                                stringBuilder6.append(ex4222.getMessage());
                                                stackLogger5.logError(stringBuilder6.toString());
                                            }
                                            setNon2XXAckPassedToListener(Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "false")).booleanValue());
                                            this.generateTimeStampHeader = Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.AUTO_GENERATE_TIMESTAMP", "false")).booleanValue();
                                            address = properties.getProperty("gov.nist.javax.sip.LOG_FACTORY");
                                            if (address == null) {
                                            }
                                            StringMsgParser.setComputeContentLengthFromMessage(properties.getProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false").equalsIgnoreCase("true"));
                                            serverLoggerClassName = properties.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
                                            if (serverLoggerClassName != null) {
                                            }
                                            this.rfc2543Supported = properties.getProperty("gov.nist.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");
                                            this.cancelClientTransactionChecked = properties.getProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase("true");
                                            this.logStackTraceOnMessageSend = properties.getProperty("gov.nist.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
                                            if (isLoggingEnabled()) {
                                            }
                                            in = getClass().getResourceAsStream("/TIMESTAMP");
                                            if (in != null) {
                                            }
                                            stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                            super.setReceiveUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                            stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                            super.setSendUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                            z = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                                            this.stackDoesCongestionControl = z;
                                            this.isBackToBackUserAgent = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                                            this.checkBranchId = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                                            this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                                            this.maxForkTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                                            return;
                                        }
                                    } catch (NumberFormatException e13) {
                                        ex4222 = e13;
                                        str4 = serverLoggerClassName;
                                        if (isLoggingEnabled()) {
                                        }
                                        setNon2XXAckPassedToListener(Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "false")).booleanValue());
                                        this.generateTimeStampHeader = Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.AUTO_GENERATE_TIMESTAMP", "false")).booleanValue();
                                        address = properties.getProperty("gov.nist.javax.sip.LOG_FACTORY");
                                        if (address == null) {
                                        }
                                        StringMsgParser.setComputeContentLengthFromMessage(properties.getProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false").equalsIgnoreCase("true"));
                                        serverLoggerClassName = properties.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
                                        if (serverLoggerClassName != null) {
                                        }
                                        this.rfc2543Supported = properties.getProperty("gov.nist.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");
                                        this.cancelClientTransactionChecked = properties.getProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase("true");
                                        this.logStackTraceOnMessageSend = properties.getProperty("gov.nist.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
                                        if (isLoggingEnabled()) {
                                        }
                                        in = getClass().getResourceAsStream("/TIMESTAMP");
                                        if (in != null) {
                                        }
                                        stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                        super.setReceiveUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                        stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                        super.setSendUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                        z = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                                        this.stackDoesCongestionControl = z;
                                        this.isBackToBackUserAgent = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                                        this.checkBranchId = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                                        this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                                        this.maxForkTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                                        return;
                                    }
                                }
                                str4 = serverLoggerClassName;
                                setNon2XXAckPassedToListener(Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "false")).booleanValue());
                                this.generateTimeStampHeader = Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.AUTO_GENERATE_TIMESTAMP", "false")).booleanValue();
                                address = properties.getProperty("gov.nist.javax.sip.LOG_FACTORY");
                                if (address == null) {
                                    try {
                                        clazz = Class.forName(address);
                                        try {
                                            this.logRecordFactory = (LogRecordFactory) clazz.getConstructor(new Class[0]).newInstance(new Object[0]);
                                        } catch (Exception e14) {
                                        }
                                    } catch (Exception e15) {
                                        if (isLoggingEnabled()) {
                                            getStackLogger().logError("Bad configuration value for LOG_FACTORY -- using default logger");
                                        }
                                        this.logRecordFactory = new DefaultMessageLogFactory();
                                        StringMsgParser.setComputeContentLengthFromMessage(properties.getProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false").equalsIgnoreCase("true"));
                                        serverLoggerClassName = properties.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
                                        if (serverLoggerClassName != null) {
                                        }
                                        this.rfc2543Supported = properties.getProperty("gov.nist.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");
                                        this.cancelClientTransactionChecked = properties.getProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase("true");
                                        this.logStackTraceOnMessageSend = properties.getProperty("gov.nist.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
                                        if (isLoggingEnabled()) {
                                        }
                                        in = getClass().getResourceAsStream("/TIMESTAMP");
                                        if (in != null) {
                                        }
                                        stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                        super.setReceiveUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                        stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                        super.setSendUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                        z = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                                        this.stackDoesCongestionControl = z;
                                        this.isBackToBackUserAgent = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                                        this.checkBranchId = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                                        this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                                        this.maxForkTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                                        return;
                                    }
                                }
                                this.logRecordFactory = new DefaultMessageLogFactory();
                                StringMsgParser.setComputeContentLengthFromMessage(properties.getProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false").equalsIgnoreCase("true"));
                                serverLoggerClassName = properties.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
                                if (serverLoggerClassName != null) {
                                    st = new StringTokenizer(serverLoggerClassName, " ,");
                                    address = new String[st.countTokens()];
                                    int i = 0;
                                    while (true) {
                                        int i2 = i;
                                        if (!st.hasMoreTokens()) {
                                            break;
                                        }
                                        i = i2 + 1;
                                        address[i2] = st.nextToken();
                                    }
                                    this.enabledProtocols = address;
                                }
                                this.rfc2543Supported = properties.getProperty("gov.nist.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");
                                this.cancelClientTransactionChecked = properties.getProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase("true");
                                this.logStackTraceOnMessageSend = properties.getProperty("gov.nist.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
                                if (isLoggingEnabled()) {
                                    StackLogger stackLogger6 = getStackLogger();
                                    StringBuilder stringBuilder7 = new StringBuilder();
                                    stringBuilder7.append("created Sip stack. Properties = ");
                                    stringBuilder7.append(properties);
                                    stackLogger6.logDebug(stringBuilder7.toString());
                                }
                                in = getClass().getResourceAsStream("/TIMESTAMP");
                                if (in != null) {
                                    try {
                                        stackLoggerClassName3 = new BufferedReader(new InputStreamReader(in)).readLine();
                                        if (in != null) {
                                            try {
                                                in.close();
                                            } catch (IOException e16) {
                                                ex2 = e16;
                                                inputStream = in;
                                            }
                                        }
                                        try {
                                            getStackLogger().setBuildTimeStamp(stackLoggerClassName3);
                                        } catch (IOException e17) {
                                            ex2 = e17;
                                        }
                                    } catch (IOException e18) {
                                        ex2 = e18;
                                        inputStream = in;
                                        getStackLogger().logError("Could not open build timestamp.");
                                        stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                        super.setReceiveUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                        stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                        super.setSendUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                        z = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                                        this.stackDoesCongestionControl = z;
                                        this.isBackToBackUserAgent = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                                        this.checkBranchId = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                                        this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                                        this.maxForkTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                                        return;
                                    }
                                }
                                stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                super.setReceiveUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                                super.setSendUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                                z = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                                this.stackDoesCongestionControl = z;
                                this.isBackToBackUserAgent = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                                this.checkBranchId = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                                this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                                this.maxForkTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                                return;
                            }
                            address = properties.getProperty("gov.nist.javax.sip.MAX_SERVER_TRANSACTIONS");
                            if (address != null) {
                            }
                            address = properties.getProperty("gov.nist.javax.sip.MAX_CLIENT_TRANSACTIONS");
                            if (address != null) {
                            }
                            this.cacheServerConnections = z2;
                            address = properties.getProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS");
                            this.cacheServerConnections = false;
                            this.cacheClientConnections = true;
                            path2 = properties.getProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS");
                            this.cacheClientConnections = false;
                            path = properties.getProperty("gov.nist.javax.sip.READ_TIMEOUT");
                            if (path != null) {
                            }
                            address = properties.getProperty("gov.nist.javax.sip.STUN_SERVER");
                            if (address != null) {
                            }
                            name = properties.getProperty("gov.nist.javax.sip.MAX_MESSAGE_SIZE");
                            if (name == null) {
                            }
                            str2 = name;
                            str3 = forkedSubscriptions;
                            address = properties.getProperty("gov.nist.javax.sip.REENTRANT_LISTENER");
                            if (address == null) {
                            }
                            this.reEntrantListener = z;
                            name = properties.getProperty("gov.nist.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS");
                            if (name == null) {
                            }
                            setNon2XXAckPassedToListener(Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "false")).booleanValue());
                            this.generateTimeStampHeader = Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.AUTO_GENERATE_TIMESTAMP", "false")).booleanValue();
                            address = properties.getProperty("gov.nist.javax.sip.LOG_FACTORY");
                            if (address == null) {
                            }
                            StringMsgParser.setComputeContentLengthFromMessage(properties.getProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false").equalsIgnoreCase("true"));
                            serverLoggerClassName = properties.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
                            if (serverLoggerClassName != null) {
                            }
                            this.rfc2543Supported = properties.getProperty("gov.nist.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");
                            this.cancelClientTransactionChecked = properties.getProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase("true");
                            this.logStackTraceOnMessageSend = properties.getProperty("gov.nist.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
                            if (isLoggingEnabled()) {
                            }
                            in = getClass().getResourceAsStream("/TIMESTAMP");
                            if (in != null) {
                            }
                            stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                            super.setReceiveUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                            stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                            super.setSendUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                            z = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                            this.stackDoesCongestionControl = z;
                            this.isBackToBackUserAgent = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                            this.checkBranchId = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                            this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                            this.maxForkTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                            return;
                        }
                        address = properties.getProperty("gov.nist.javax.sip.THREAD_POOL_SIZE");
                        if (address != null) {
                        }
                        address = properties.getProperty("gov.nist.javax.sip.MAX_SERVER_TRANSACTIONS");
                        if (address != null) {
                        }
                        address = properties.getProperty("gov.nist.javax.sip.MAX_CLIENT_TRANSACTIONS");
                        if (address != null) {
                        }
                        this.cacheServerConnections = z2;
                        address = properties.getProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS");
                        this.cacheServerConnections = false;
                        this.cacheClientConnections = true;
                        path2 = properties.getProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS");
                        this.cacheClientConnections = false;
                        path = properties.getProperty("gov.nist.javax.sip.READ_TIMEOUT");
                        if (path != null) {
                        }
                        address = properties.getProperty("gov.nist.javax.sip.STUN_SERVER");
                        if (address != null) {
                        }
                        name = properties.getProperty("gov.nist.javax.sip.MAX_MESSAGE_SIZE");
                        if (name == null) {
                        }
                        str2 = name;
                        str3 = forkedSubscriptions;
                        address = properties.getProperty("gov.nist.javax.sip.REENTRANT_LISTENER");
                        if (address == null) {
                        }
                        this.reEntrantListener = z;
                        name = properties.getProperty("gov.nist.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS");
                        if (name == null) {
                        }
                        setNon2XXAckPassedToListener(Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "false")).booleanValue());
                        this.generateTimeStampHeader = Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.AUTO_GENERATE_TIMESTAMP", "false")).booleanValue();
                        address = properties.getProperty("gov.nist.javax.sip.LOG_FACTORY");
                        if (address == null) {
                        }
                        StringMsgParser.setComputeContentLengthFromMessage(properties.getProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false").equalsIgnoreCase("true"));
                        serverLoggerClassName = properties.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
                        if (serverLoggerClassName != null) {
                        }
                        this.rfc2543Supported = properties.getProperty("gov.nist.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");
                        this.cancelClientTransactionChecked = properties.getProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase("true");
                        this.logStackTraceOnMessageSend = properties.getProperty("gov.nist.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
                        if (isLoggingEnabled()) {
                        }
                        in = getClass().getResourceAsStream("/TIMESTAMP");
                        if (in != null) {
                        }
                        stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                        super.setReceiveUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                        stackLoggerClassName3 = properties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                        super.setSendUdpBufferSize(new Integer(stackLoggerClassName3).intValue());
                        z = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                        this.stackDoesCongestionControl = z;
                        this.isBackToBackUserAgent = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                        this.checkBranchId = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                        this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                        this.maxForkTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                        return;
                    } catch (InvocationTargetException ex1) {
                        str = address;
                        str5 = name;
                        str4 = serverLoggerClassName;
                        getStackLogger().logError("could not instantiate router -- invocation target problem", (Exception) ex1.getCause());
                        throw new PeerUnavailableException("Cound not instantiate router - check constructor", ex1);
                    } catch (Exception e19) {
                        str = address;
                        str5 = name;
                        str4 = serverLoggerClassName;
                        getStackLogger().logError("could not instantiate router", (Exception) e19.getCause());
                        throw new PeerUnavailableException("Could not instantiate router", e19);
                    }
                } catch (InvocationTargetException ex12) {
                    str = address;
                    str5 = name;
                    str4 = serverLoggerClassName;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Cound not instantiate server logger ");
                    stringBuilder2.append(stackLoggerClassName2);
                    stringBuilder2.append("- check that it is present on the classpath and that there is a no-args constructor defined");
                    throw new IllegalArgumentException(stringBuilder2.toString(), ex12);
                } catch (Exception e192) {
                    str = address;
                    str5 = name;
                    str4 = serverLoggerClassName;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Cound not instantiate server logger ");
                    stringBuilder2.append(stackLoggerClassName2);
                    stringBuilder2.append("- check that it is present on the classpath and that there is a no-args constructor defined");
                    throw new IllegalArgumentException(stringBuilder2.toString(), e192);
                }
            } catch (InvocationTargetException ex122) {
                str = address;
                str5 = name;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Cound not instantiate stack logger ");
                stringBuilder2.append(stackLoggerClassName2);
                stringBuilder2.append("- check that it is present on the classpath and that there is a no-args constructor defined");
                throw new IllegalArgumentException(stringBuilder2.toString(), ex122);
            } catch (Exception e1922) {
                str = address;
                str5 = name;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Cound not instantiate stack logger ");
                stringBuilder2.append(stackLoggerClassName2);
                stringBuilder2.append("- check that it is present on the classpath and that there is a no-args constructor defined");
                throw new IllegalArgumentException(stringBuilder2.toString(), e1922);
            }
        }
        str5 = name;
        throw new PeerUnavailableException("stack name is missing");
    }

    public synchronized ListeningPoint createListeningPoint(String address, int port, String transport) throws TransportNotSupportedException, InvalidArgumentException {
        StringBuilder stringBuilder;
        if (isLoggingEnabled()) {
            StackLogger stackLogger = getStackLogger();
            stringBuilder = new StringBuilder();
            stringBuilder.append("createListeningPoint : address = ");
            stringBuilder.append(address);
            stringBuilder.append(" port = ");
            stringBuilder.append(port);
            stringBuilder.append(" transport = ");
            stringBuilder.append(transport);
            stackLogger.logDebug(stringBuilder.toString());
        }
        if (address == null) {
            throw new NullPointerException("Address for listening point is null!");
        } else if (transport == null) {
            throw new NullPointerException("null transport");
        } else if (port > 0) {
            if (!(transport.equalsIgnoreCase(ListeningPoint.UDP) || transport.equalsIgnoreCase(ListeningPoint.TLS) || transport.equalsIgnoreCase(ListeningPoint.TCP))) {
                if (!transport.equalsIgnoreCase(ListeningPoint.SCTP)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("bad transport ");
                    stringBuilder.append(transport);
                    throw new TransportNotSupportedException(stringBuilder.toString());
                }
            }
            if (!isAlive()) {
                this.toExit = false;
                reInitialize();
            }
            String key = ListeningPointImpl.makeKey(address, port, transport);
            ListeningPointImpl lip = (ListeningPointImpl) this.listeningPoints.get(key);
            if (lip != null) {
                return lip;
            }
            try {
                MessageProcessor messageProcessor = createMessageProcessor(InetAddress.getByName(address), port, transport);
                if (isLoggingEnabled()) {
                    StackLogger stackLogger2 = getStackLogger();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Created Message Processor: ");
                    stringBuilder2.append(address);
                    stringBuilder2.append(" port = ");
                    stringBuilder2.append(port);
                    stringBuilder2.append(" transport = ");
                    stringBuilder2.append(transport);
                    stackLogger2.logDebug(stringBuilder2.toString());
                }
                lip = new ListeningPointImpl(this, port, transport);
                lip.messageProcessor = messageProcessor;
                messageProcessor.setListeningPoint(lip);
                this.listeningPoints.put(key, lip);
                messageProcessor.start();
                return lip;
            } catch (IOException ex) {
                if (isLoggingEnabled()) {
                    StackLogger stackLogger3 = getStackLogger();
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Invalid argument address = ");
                    stringBuilder3.append(address);
                    stringBuilder3.append(" port = ");
                    stringBuilder3.append(port);
                    stringBuilder3.append(" transport = ");
                    stringBuilder3.append(transport);
                    stackLogger3.logError(stringBuilder3.toString());
                }
                throw new InvalidArgumentException(ex.getMessage(), ex);
            }
        } else {
            throw new InvalidArgumentException("bad port");
        }
    }

    public SipProvider createSipProvider(ListeningPoint listeningPoint) throws ObjectInUseException {
        if (listeningPoint != null) {
            if (isLoggingEnabled()) {
                StackLogger stackLogger = getStackLogger();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("createSipProvider: ");
                stringBuilder.append(listeningPoint);
                stackLogger.logDebug(stringBuilder.toString());
            }
            ListeningPointImpl listeningPointImpl = (ListeningPointImpl) listeningPoint;
            if (listeningPointImpl.sipProvider == null) {
                SipProviderImpl provider = new SipProviderImpl(this);
                provider.setListeningPoint(listeningPointImpl);
                listeningPointImpl.sipProvider = provider;
                this.sipProviders.add(provider);
                return provider;
            }
            throw new ObjectInUseException("Provider already attached!");
        }
        throw new NullPointerException("null listeningPoint");
    }

    public void deleteListeningPoint(ListeningPoint listeningPoint) throws ObjectInUseException {
        if (listeningPoint != null) {
            ListeningPointImpl lip = (ListeningPointImpl) listeningPoint;
            super.removeMessageProcessor(lip.messageProcessor);
            this.listeningPoints.remove(lip.getKey());
            return;
        }
        throw new NullPointerException("null listeningPoint arg");
    }

    public void deleteSipProvider(SipProvider sipProvider) throws ObjectInUseException {
        if (sipProvider != null) {
            SipProviderImpl sipProviderImpl = (SipProviderImpl) sipProvider;
            if (sipProviderImpl.getSipListener() == null) {
                sipProviderImpl.removeListeningPoints();
                sipProviderImpl.stop();
                this.sipProviders.remove(sipProvider);
                if (this.sipProviders.isEmpty()) {
                    stopStack();
                    return;
                }
                return;
            }
            throw new ObjectInUseException("SipProvider still has an associated SipListener!");
        }
        throw new NullPointerException("null provider arg");
    }

    public String getIPAddress() {
        return super.getHostAddress();
    }

    public Iterator getListeningPoints() {
        return this.listeningPoints.values().iterator();
    }

    public boolean isRetransmissionFilterActive() {
        return true;
    }

    public Iterator<SipProviderImpl> getSipProviders() {
        return this.sipProviders.iterator();
    }

    public String getStackName() {
        return this.stackName;
    }

    protected void finalize() {
        stopStack();
    }

    public ListeningPoint createListeningPoint(int port, String transport) throws TransportNotSupportedException, InvalidArgumentException {
        if (this.stackAddress != null) {
            return createListeningPoint(this.stackAddress, port, transport);
        }
        throw new NullPointerException("Stack does not have a default IP Address!");
    }

    public void stop() {
        if (isLoggingEnabled()) {
            getStackLogger().logDebug("stopStack -- stoppping the stack");
        }
        stopStack();
        this.sipProviders = new LinkedList();
        this.listeningPoints = new Hashtable();
        if (this.eventScanner != null) {
            this.eventScanner.forceStop();
        }
        this.eventScanner = null;
    }

    public void start() throws ProviderDoesNotExistException, SipException {
        if (this.eventScanner == null) {
            this.eventScanner = new EventScanner(this);
        }
    }

    public SipListener getSipListener() {
        return this.sipListener;
    }

    public LogRecordFactory getLogRecordFactory() {
        return this.logRecordFactory;
    }

    @Deprecated
    public EventScanner getEventScanner() {
        return this.eventScanner;
    }

    public AuthenticationHelper getAuthenticationHelper(AccountManager accountManager, HeaderFactory headerFactory) {
        return new AuthenticationHelperImpl(this, accountManager, headerFactory);
    }

    public AuthenticationHelper getSecureAuthenticationHelper(SecureAccountManager accountManager, HeaderFactory headerFactory) {
        return new AuthenticationHelperImpl(this, accountManager, headerFactory);
    }

    public void setEnabledCipherSuites(String[] newCipherSuites) {
        this.cipherSuites = newCipherSuites;
    }

    public String[] getEnabledCipherSuites() {
        return this.cipherSuites;
    }

    public void setEnabledProtocols(String[] newProtocols) {
        this.enabledProtocols = newProtocols;
    }

    public String[] getEnabledProtocols() {
        return this.enabledProtocols;
    }

    public void setIsBackToBackUserAgent(boolean flag) {
        this.isBackToBackUserAgent = flag;
    }

    public boolean isBackToBackUserAgent() {
        return this.isBackToBackUserAgent;
    }

    public boolean isAutomaticDialogErrorHandlingEnabled() {
        return this.isAutomaticDialogErrorHandlingEnabled;
    }

    public boolean acquireSem() {
        try {
            return this.stackSemaphore.tryAcquire(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void releaseSem() {
        this.stackSemaphore.release();
    }
}
