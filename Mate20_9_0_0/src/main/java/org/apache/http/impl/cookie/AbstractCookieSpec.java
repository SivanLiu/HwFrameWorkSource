package org.apache.http.impl.cookie;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieSpec;

@Deprecated
public abstract class AbstractCookieSpec implements CookieSpec {
    private final Map<String, CookieAttributeHandler> attribHandlerMap = new HashMap(10);

    public void registerAttribHandler(String name, CookieAttributeHandler handler) {
        if (name == null) {
            throw new IllegalArgumentException("Attribute name may not be null");
        } else if (handler != null) {
            this.attribHandlerMap.put(name, handler);
        } else {
            throw new IllegalArgumentException("Attribute handler may not be null");
        }
    }

    protected CookieAttributeHandler findAttribHandler(String name) {
        return (CookieAttributeHandler) this.attribHandlerMap.get(name);
    }

    protected CookieAttributeHandler getAttribHandler(String name) {
        CookieAttributeHandler handler = findAttribHandler(name);
        if (handler != null) {
            return handler;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Handler not registered for ");
        stringBuilder.append(name);
        stringBuilder.append(" attribute.");
        throw new IllegalStateException(stringBuilder.toString());
    }

    protected Collection<CookieAttributeHandler> getAttribHandlers() {
        return this.attribHandlerMap.values();
    }
}
