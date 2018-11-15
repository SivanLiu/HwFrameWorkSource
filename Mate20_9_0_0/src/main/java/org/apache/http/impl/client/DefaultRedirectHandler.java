package org.apache.http.impl.client;

import android.net.http.Headers;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

@Deprecated
public class DefaultRedirectHandler implements RedirectHandler {
    private static final String REDIRECT_LOCATIONS = "http.protocol.redirect-locations";
    private final Log log = LogFactory.getLog(getClass());

    public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
        if (response != null) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_TEMPORARY_REDIRECT) {
                switch (statusCode) {
                    case HttpStatus.SC_MOVED_PERMANENTLY /*301*/:
                    case HttpStatus.SC_MOVED_TEMPORARILY /*302*/:
                    case HttpStatus.SC_SEE_OTHER /*303*/:
                        break;
                    default:
                        return false;
                }
            }
            return true;
        }
        throw new IllegalArgumentException("HTTP response may not be null");
    }

    public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {
        if (response != null) {
            Header locationHeader = response.getFirstHeader(Headers.LOCATION);
            if (locationHeader != null) {
                String location = locationHeader.getValue();
                if (this.log.isDebugEnabled()) {
                    Log log = this.log;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Redirect requested to location '");
                    stringBuilder.append(location);
                    stringBuilder.append("'");
                    log.debug(stringBuilder.toString());
                }
                try {
                    URI uri = new URI(location);
                    HttpParams params = response.getParams();
                    if (!uri.isAbsolute()) {
                        if (params.isParameterTrue(ClientPNames.REJECT_RELATIVE_REDIRECT)) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Relative redirect location '");
                            stringBuilder2.append(uri);
                            stringBuilder2.append("' not allowed");
                            throw new ProtocolException(stringBuilder2.toString());
                        }
                        HttpHost target = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                        if (target != null) {
                            try {
                                uri = URIUtils.resolve(URIUtils.rewriteURI(new URI(((HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST)).getRequestLine().getUri()), target, true), uri);
                            } catch (URISyntaxException ex) {
                                throw new ProtocolException(ex.getMessage(), ex);
                            }
                        }
                        throw new IllegalStateException("Target host not available in the HTTP context");
                    }
                    if (params.isParameterFalse(ClientPNames.ALLOW_CIRCULAR_REDIRECTS)) {
                        URI redirectURI;
                        RedirectLocations redirectLocations = (RedirectLocations) context.getAttribute(REDIRECT_LOCATIONS);
                        if (redirectLocations == null) {
                            redirectLocations = new RedirectLocations();
                            context.setAttribute(REDIRECT_LOCATIONS, redirectLocations);
                        }
                        if (uri.getFragment() != null) {
                            try {
                                redirectURI = URIUtils.rewriteURI(uri, new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()), true);
                            } catch (URISyntaxException ex2) {
                                throw new ProtocolException(ex2.getMessage(), ex2);
                            }
                        }
                        redirectURI = uri;
                        if (redirectLocations.contains(redirectURI)) {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Circular redirect to '");
                            stringBuilder3.append(redirectURI);
                            stringBuilder3.append("'");
                            throw new CircularRedirectException(stringBuilder3.toString());
                        }
                        redirectLocations.add(redirectURI);
                    }
                    return uri;
                } catch (URISyntaxException ex3) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Invalid redirect URI: ");
                    stringBuilder4.append(location);
                    throw new ProtocolException(stringBuilder4.toString(), ex3);
                }
            }
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("Received redirect response ");
            stringBuilder5.append(response.getStatusLine());
            stringBuilder5.append(" but no location header");
            throw new ProtocolException(stringBuilder5.toString());
        }
        throw new IllegalArgumentException("HTTP response may not be null");
    }
}
