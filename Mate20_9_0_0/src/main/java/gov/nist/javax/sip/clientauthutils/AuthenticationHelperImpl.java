package gov.nist.javax.sip.clientauthutils;

import gov.nist.core.StackLogger;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Timer;
import javax.sip.ClientTransaction;
import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.address.Hop;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class AuthenticationHelperImpl implements AuthenticationHelper {
    private Object accountManager = null;
    private CredentialsCache cachedCredentials;
    private HeaderFactory headerFactory;
    private SipStackImpl sipStack;
    Timer timer;

    public AuthenticationHelperImpl(SipStackImpl sipStack, AccountManager accountManager, HeaderFactory headerFactory) {
        this.accountManager = accountManager;
        this.headerFactory = headerFactory;
        this.sipStack = sipStack;
        this.cachedCredentials = new CredentialsCache(sipStack.getTimer());
    }

    public AuthenticationHelperImpl(SipStackImpl sipStack, SecureAccountManager accountManager, HeaderFactory headerFactory) {
        this.accountManager = accountManager;
        this.headerFactory = headerFactory;
        this.sipStack = sipStack;
        this.cachedCredentials = new CredentialsCache(sipStack.getTimer());
    }

    public ClientTransaction handleChallenge(Response challenge, ClientTransaction challengedTransaction, SipProvider transactionCreator, int cacheTime) throws SipException, NullPointerException {
        SipException ex;
        Exception ex2;
        Response response = challenge;
        ClientTransaction clientTransaction = challengedTransaction;
        int i = cacheTime;
        SipProvider sipProvider;
        try {
            Request reoriginatedRequest;
            Request reoriginatedRequest2;
            if (this.sipStack.isLoggingEnabled()) {
                StackLogger stackLogger = this.sipStack.getStackLogger();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleChallenge: ");
                stringBuilder.append(response);
                stackLogger.logDebug(stringBuilder.toString());
            }
            SIPRequest challengedRequest = (SIPRequest) challengedTransaction.getRequest();
            if (challengedRequest.getToTag() == null && challengedTransaction.getDialog() != null) {
                if (challengedTransaction.getDialog().getState() == DialogState.CONFIRMED) {
                    String headerName;
                    reoriginatedRequest = challengedTransaction.getDialog().createRequest(challengedRequest.getMethod());
                    Iterator<String> headerNames = challengedRequest.getHeaderNames();
                    while (headerNames.hasNext()) {
                        headerName = (String) headerNames.next();
                        if (reoriginatedRequest.getHeader(headerName) != null) {
                            ListIterator<Header> iterator = reoriginatedRequest.getHeaders(headerName);
                            while (iterator.hasNext()) {
                                reoriginatedRequest.addHeader((Header) iterator.next());
                            }
                        }
                    }
                    reoriginatedRequest2 = reoriginatedRequest;
                    removeBranchID(reoriginatedRequest2);
                    if (response != null || reoriginatedRequest2 == null) {
                        sipProvider = transactionCreator;
                        throw new NullPointerException("A null argument was passed to handle challenge.");
                    }
                    ListIterator authHeaders;
                    if (challenge.getStatusCode() == Response.UNAUTHORIZED) {
                        authHeaders = response.getHeaders("WWW-Authenticate");
                    } else if (challenge.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED) {
                        authHeaders = response.getHeaders("Proxy-Authenticate");
                    } else {
                        sipProvider = transactionCreator;
                        throw new IllegalArgumentException("Unexpected status code ");
                    }
                    ListIterator authHeaders2 = authHeaders;
                    if (authHeaders2 != null) {
                        reoriginatedRequest2.removeHeader("Authorization");
                        reoriginatedRequest2.removeHeader("Proxy-Authorization");
                        CSeqHeader cSeq = (CSeqHeader) reoriginatedRequest2.getHeader("CSeq");
                        try {
                            cSeq.setSeqNumber(cSeq.getSeqNumber() + 1);
                            if (challengedRequest.getRouteHeaders() == null) {
                                Hop hop = ((SIPClientTransaction) clientTransaction).getNextHop();
                                SipURI sipUri = (SipURI) reoriginatedRequest2.getRequestURI();
                                if (!(hop.getHost().equalsIgnoreCase(sipUri.getHost()) || hop.equals(this.sipStack.getRouter(challengedRequest).getOutboundProxy()))) {
                                    sipUri.setMAddrParam(hop.getHost());
                                }
                                if (hop.getPort() != -1) {
                                    sipUri.setPort(hop.getPort());
                                }
                            }
                            ClientTransaction retryTran = transactionCreator.getNewClientTransaction(reoriginatedRequest2);
                            SipURI requestUri = (SipURI) challengedTransaction.getRequest().getRequestURI();
                            while (true) {
                                SipURI requestUri2 = requestUri;
                                if (authHeaders2.hasNext()) {
                                    String sipDomain;
                                    WWWAuthenticateHeader authHeader;
                                    AuthorizationHeader authorization;
                                    WWWAuthenticateHeader authHeader2 = (WWWAuthenticateHeader) authHeaders2.next();
                                    String realm = authHeader2.getRealm();
                                    if (this.accountManager instanceof SecureAccountManager) {
                                        String str;
                                        UserCredentialHash credHash = ((SecureAccountManager) this.accountManager).getCredentialHash(clientTransaction, realm);
                                        URI uri = reoriginatedRequest2.getRequestURI();
                                        sipDomain = credHash.getSipDomain();
                                        headerName = reoriginatedRequest2.getMethod();
                                        String uri2 = uri.toString();
                                        if (reoriginatedRequest2.getContent() == null) {
                                            str = "";
                                            URI uri3 = uri;
                                        } else {
                                            str = new String(reoriginatedRequest2.getRawContent());
                                        }
                                        authHeader = authHeader2;
                                        authorization = getAuthorization(headerName, uri2, str, authHeader2, credHash);
                                    } else {
                                        authHeader = authHeader2;
                                        UserCredentials userCreds = ((AccountManager) this.accountManager).getCredentials(clientTransaction, realm);
                                        sipDomain = userCreds.getSipDomain();
                                        if (userCreds != null) {
                                            String str2;
                                            headerName = reoriginatedRequest2.getMethod();
                                            String uri4 = reoriginatedRequest2.getRequestURI().toString();
                                            if (reoriginatedRequest2.getContent() == null) {
                                                str2 = "";
                                            } else {
                                                str2 = new String(reoriginatedRequest2.getRawContent());
                                            }
                                            authorization = getAuthorization(headerName, uri4, str2, authHeader, userCreds);
                                        } else {
                                            UserCredentials userCredentials = userCreds;
                                            throw new SipException("Cannot find user creds for the given user name and realm");
                                        }
                                    }
                                    headerName = sipDomain;
                                    if (this.sipStack.isLoggingEnabled()) {
                                        StackLogger stackLogger2 = this.sipStack.getStackLogger();
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Created authorization header: ");
                                        stringBuilder2.append(authorization.toString());
                                        stackLogger2.logDebug(stringBuilder2.toString());
                                    }
                                    if (i != 0) {
                                        this.cachedCredentials.cacheAuthorizationHeader(headerName, authorization, i);
                                    }
                                    reoriginatedRequest2.addHeader(authorization);
                                    requestUri = requestUri2;
                                    WWWAuthenticateHeader wWWAuthenticateHeader = authHeader;
                                    response = challenge;
                                } else {
                                    if (this.sipStack.isLoggingEnabled()) {
                                        StackLogger stackLogger3 = this.sipStack.getStackLogger();
                                        StringBuilder stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("Returning authorization transaction.");
                                        stringBuilder3.append(retryTran);
                                        stackLogger3.logDebug(stringBuilder3.toString());
                                    }
                                    return retryTran;
                                }
                            }
                        } catch (InvalidArgumentException e) {
                            sipProvider = transactionCreator;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Invalid CSeq -- could not increment : ");
                            stringBuilder4.append(cSeq.getSeqNumber());
                            throw new SipException(stringBuilder4.toString());
                        } catch (SipException e2) {
                            ex = e2;
                            throw ex;
                        } catch (Exception e3) {
                            ex2 = e3;
                            this.sipStack.getStackLogger().logError("Unexpected exception ", ex2);
                            throw new SipException("Unexpected exception ", ex2);
                        }
                    }
                    sipProvider = transactionCreator;
                    throw new IllegalArgumentException("Could not find WWWAuthenticate or ProxyAuthenticate headers");
                }
            }
            reoriginatedRequest = (Request) challengedRequest.clone();
            reoriginatedRequest2 = reoriginatedRequest;
            removeBranchID(reoriginatedRequest2);
            if (response != null) {
            }
            sipProvider = transactionCreator;
            throw new NullPointerException("A null argument was passed to handle challenge.");
        } catch (SipException e4) {
            ex = e4;
            sipProvider = transactionCreator;
            throw ex;
        } catch (Exception e5) {
            ex2 = e5;
            sipProvider = transactionCreator;
            this.sipStack.getStackLogger().logError("Unexpected exception ", ex2);
            throw new SipException("Unexpected exception ", ex2);
        }
    }

    private AuthorizationHeader getAuthorization(String method, String uri, String requestBody, WWWAuthenticateHeader authHeader, UserCredentials userCredentials) {
        String str;
        String str2;
        String str3;
        String qop = authHeader.getQop() != null ? "auth" : null;
        String nc_value = "00000001";
        String cnonce = "xyz";
        String cnonce2 = cnonce;
        String nc_value2 = nc_value;
        String response = MessageDigestAlgorithm.calculateResponse(authHeader.getAlgorithm(), userCredentials.getUserName(), authHeader.getRealm(), userCredentials.getPassword(), authHeader.getNonce(), nc_value, cnonce, method, uri, requestBody, qop, this.sipStack.getStackLogger());
        try {
            AuthorizationHeader authorization;
            if (authHeader instanceof ProxyAuthenticateHeader) {
                try {
                    authorization = this.headerFactory.createProxyAuthorizationHeader(authHeader.getScheme());
                } catch (ParseException e) {
                    str = uri;
                    throw new RuntimeException("Failed to create an authorization header!");
                }
            }
            authorization = this.headerFactory.createAuthorizationHeader(authHeader.getScheme());
            AuthorizationHeader authorization2 = authorization;
            authorization2.setUsername(userCredentials.getUserName());
            authorization2.setRealm(authHeader.getRealm());
            authorization2.setNonce(authHeader.getNonce());
            try {
                authorization2.setParameter("uri", uri);
                authorization2.setResponse(response);
                if (authHeader.getAlgorithm() != null) {
                    try {
                        authorization2.setAlgorithm(authHeader.getAlgorithm());
                    } catch (ParseException e2) {
                    }
                }
                if (authHeader.getOpaque() != null) {
                    authorization2.setOpaque(authHeader.getOpaque());
                }
                if (qop != null) {
                    authorization2.setQop(qop);
                    try {
                        authorization2.setCNonce(cnonce2);
                    } catch (ParseException e3) {
                        str2 = nc_value2;
                        throw new RuntimeException("Failed to create an authorization header!");
                    }
                    try {
                        authorization2.setNonceCount(Integer.parseInt(nc_value2));
                    } catch (ParseException e4) {
                        throw new RuntimeException("Failed to create an authorization header!");
                    }
                }
                str2 = nc_value2;
                authorization2.setResponse(response);
                return authorization2;
            } catch (ParseException e5) {
                str3 = cnonce2;
                str2 = nc_value2;
                throw new RuntimeException("Failed to create an authorization header!");
            }
        } catch (ParseException e6) {
            str = uri;
            str3 = cnonce2;
            str2 = nc_value2;
            throw new RuntimeException("Failed to create an authorization header!");
        }
    }

    private AuthorizationHeader getAuthorization(String method, String uri, String requestBody, WWWAuthenticateHeader authHeader, UserCredentialHash userCredentials) {
        String str;
        String str2;
        String qop = authHeader.getQop() != null ? "auth" : null;
        String nc_value = "00000001";
        String cnonce = "xyz";
        String cnonce2 = cnonce;
        String response = MessageDigestAlgorithm.calculateResponse(authHeader.getAlgorithm(), userCredentials.getHashUserDomainPassword(), authHeader.getNonce(), nc_value, cnonce, method, uri, requestBody, qop, this.sipStack.getStackLogger());
        try {
            AuthorizationHeader authorization;
            if (authHeader instanceof ProxyAuthenticateHeader) {
                try {
                    authorization = this.headerFactory.createProxyAuthorizationHeader(authHeader.getScheme());
                } catch (ParseException e) {
                    str = uri;
                    throw new RuntimeException("Failed to create an authorization header!");
                }
            }
            authorization = this.headerFactory.createAuthorizationHeader(authHeader.getScheme());
            AuthorizationHeader authorization2 = authorization;
            authorization2.setUsername(userCredentials.getUserName());
            authorization2.setRealm(authHeader.getRealm());
            authorization2.setNonce(authHeader.getNonce());
            try {
                authorization2.setParameter("uri", uri);
                authorization2.setResponse(response);
                if (authHeader.getAlgorithm() != null) {
                    try {
                        authorization2.setAlgorithm(authHeader.getAlgorithm());
                    } catch (ParseException e2) {
                    }
                }
                if (authHeader.getOpaque() != null) {
                    authorization2.setOpaque(authHeader.getOpaque());
                }
                if (qop != null) {
                    authorization2.setQop(qop);
                    try {
                        authorization2.setCNonce(cnonce2);
                        authorization2.setNonceCount(Integer.parseInt(nc_value));
                    } catch (ParseException e3) {
                        throw new RuntimeException("Failed to create an authorization header!");
                    }
                }
                authorization2.setResponse(response);
                return authorization2;
            } catch (ParseException e4) {
                str2 = cnonce2;
                throw new RuntimeException("Failed to create an authorization header!");
            }
        } catch (ParseException e5) {
            str = uri;
            str2 = cnonce2;
            throw new RuntimeException("Failed to create an authorization header!");
        }
    }

    private void removeBranchID(Request request) {
        ((ViaHeader) request.getHeader("Via")).removeParameter("branch");
    }

    public void setAuthenticationHeaders(Request request) {
        String callId = ((SIPRequest) request).getCallId().getCallId();
        request.removeHeader("Authorization");
        Collection<AuthorizationHeader> authHeaders = this.cachedCredentials.getCachedAuthorizationHeaders(callId);
        if (authHeaders == null) {
            if (this.sipStack.isLoggingEnabled()) {
                StackLogger stackLogger = this.sipStack.getStackLogger();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Could not find authentication headers for ");
                stringBuilder.append(callId);
                stackLogger.logDebug(stringBuilder.toString());
            }
            return;
        }
        for (AuthorizationHeader authHeader : authHeaders) {
            request.addHeader(authHeader);
        }
    }

    public void removeCachedAuthenticationHeaders(String callId) {
        if (callId != null) {
            this.cachedCredentials.removeAuthenticationHeader(callId);
            return;
        }
        throw new NullPointerException("Null callId argument ");
    }
}
