package org.ccil.cowan.tagsoup;

import java.util.HashMap;
import java.util.Locale;

public abstract class Schema {
    public static final int F_CDATA = 2;
    public static final int F_NOFORCE = 4;
    public static final int F_RESTART = 1;
    public static final int M_ANY = -1;
    public static final int M_EMPTY = 0;
    public static final int M_PCDATA = 1073741824;
    public static final int M_ROOT = Integer.MIN_VALUE;
    private HashMap theElementTypes = new HashMap();
    private HashMap theEntities = new HashMap();
    private String thePrefix = "";
    private ElementType theRoot = null;
    private String theURI = "";

    public void elementType(String name, int model, int memberOf, int flags) {
        ElementType e = new ElementType(name, model, memberOf, flags, this);
        this.theElementTypes.put(name.toLowerCase(Locale.ROOT), e);
        if (memberOf == M_ROOT) {
            this.theRoot = e;
        }
    }

    public ElementType rootElementType() {
        return this.theRoot;
    }

    public void attribute(String elemName, String attrName, String type, String value) {
        ElementType e = getElementType(elemName);
        if (e != null) {
            e.setAttribute(attrName, type, value);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attribute ");
        stringBuilder.append(attrName);
        stringBuilder.append(" specified for unknown element type ");
        stringBuilder.append(elemName);
        throw new Error(stringBuilder.toString());
    }

    public void parent(String name, String parentName) {
        ElementType child = getElementType(name);
        ElementType parent = getElementType(parentName);
        StringBuilder stringBuilder;
        if (child == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No child ");
            stringBuilder.append(name);
            stringBuilder.append(" for parent ");
            stringBuilder.append(parentName);
            throw new Error(stringBuilder.toString());
        } else if (parent != null) {
            child.setParent(parent);
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No parent ");
            stringBuilder.append(parentName);
            stringBuilder.append(" for child ");
            stringBuilder.append(name);
            throw new Error(stringBuilder.toString());
        }
    }

    public void entity(String name, int value) {
        this.theEntities.put(name, new Integer(value));
    }

    public ElementType getElementType(String name) {
        return (ElementType) this.theElementTypes.get(name.toLowerCase(Locale.ROOT));
    }

    public int getEntity(String name) {
        Integer ch = (Integer) this.theEntities.get(name);
        if (ch == null) {
            return 0;
        }
        return ch.intValue();
    }

    public String getURI() {
        return this.theURI;
    }

    public String getPrefix() {
        return this.thePrefix;
    }

    public void setURI(String uri) {
        this.theURI = uri;
    }

    public void setPrefix(String prefix) {
        this.thePrefix = prefix;
    }
}
