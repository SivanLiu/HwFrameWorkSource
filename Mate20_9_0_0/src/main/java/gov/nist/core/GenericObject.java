package gov.nist.core;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class GenericObject implements Serializable, Cloneable {
    protected static final String AND = "&";
    protected static final String AT = "@";
    protected static final String COLON = ":";
    protected static final String COMMA = ",";
    protected static final String DOT = ".";
    protected static final String DOUBLE_QUOTE = "\"";
    protected static final String EQUALS = "=";
    protected static final String GREATER_THAN = ">";
    protected static final String HT = "\t";
    protected static final String LESS_THAN = "<";
    protected static final String LPAREN = "(";
    protected static final String NEWLINE = "\r\n";
    protected static final String PERCENT = "%";
    protected static final String POUND = "#";
    protected static final String QUESTION = "?";
    protected static final String QUOTE = "'";
    protected static final String RETURN = "\n";
    protected static final String RPAREN = ")";
    protected static final String SEMICOLON = ";";
    protected static final String SLASH = "/";
    protected static final String SP = " ";
    protected static final String STAR = "*";
    static final String[] immutableClassNames = new String[]{"String", "Character", "Boolean", "Byte", "Short", "Integer", "Long", "Float", "Double"};
    protected static final Set<Class<?>> immutableClasses = new HashSet(10);
    protected int indentation = 0;
    protected Match matchExpression;
    protected String stringRepresentation = "";

    public abstract String encode();

    static {
        int i = 0;
        while (i < immutableClassNames.length) {
            try {
                Set set = immutableClasses;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("java.lang.");
                stringBuilder.append(immutableClassNames[i]);
                set.add(Class.forName(stringBuilder.toString()));
                i++;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Internal error", e);
            }
        }
    }

    public void setMatcher(Match matchExpression) {
        if (matchExpression != null) {
            this.matchExpression = matchExpression;
            return;
        }
        throw new IllegalArgumentException("null arg!");
    }

    public Match getMatcher() {
        return this.matchExpression;
    }

    public static Class<?> getClassFromName(String className) {
        try {
            return Class.forName(className);
        } catch (Exception ex) {
            InternalErrorHandler.handleException(ex);
            return null;
        }
    }

    public static boolean isMySubclass(Class<?> other) {
        return GenericObject.class.isAssignableFrom(other);
    }

    public static Object makeClone(Object obj) {
        if (obj != null) {
            Class<?> c = obj.getClass();
            Object clone_obj = obj;
            if (immutableClasses.contains(c)) {
                return obj;
            }
            if (c.isArray()) {
                Class<?> ec = c.getComponentType();
                if (ec.isPrimitive()) {
                    if (ec == Character.TYPE) {
                        clone_obj = ((char[]) obj).clone();
                    } else if (ec == Boolean.TYPE) {
                        clone_obj = ((boolean[]) obj).clone();
                    }
                    if (ec == Byte.TYPE) {
                        clone_obj = ((byte[]) obj).clone();
                    } else if (ec == Short.TYPE) {
                        clone_obj = ((short[]) obj).clone();
                    } else if (ec == Integer.TYPE) {
                        clone_obj = ((int[]) obj).clone();
                    } else if (ec == Long.TYPE) {
                        clone_obj = ((long[]) obj).clone();
                    } else if (ec == Float.TYPE) {
                        clone_obj = ((float[]) obj).clone();
                    } else if (ec == Double.TYPE) {
                        clone_obj = ((double[]) obj).clone();
                    }
                } else {
                    clone_obj = ((Object[]) obj).clone();
                }
            } else if (GenericObject.class.isAssignableFrom(c)) {
                clone_obj = ((GenericObject) obj).clone();
            } else if (GenericObjectList.class.isAssignableFrom(c)) {
                clone_obj = ((GenericObjectList) obj).clone();
            } else if (Cloneable.class.isAssignableFrom(c)) {
                try {
                    clone_obj = c.getMethod("clone", (Class[]) null).invoke(obj, (Object[]) null);
                } catch (IllegalArgumentException ex) {
                    InternalErrorHandler.handleException(ex);
                } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
                }
            }
            return clone_obj;
        }
        throw new NullPointerException("null obj!");
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Internal error");
        }
    }

    public void merge(Object mergeObject) {
        if (mergeObject != null) {
            if (mergeObject.getClass().equals(getClass())) {
                Class<?> myclass = getClass();
                while (true) {
                    Field[] fields = myclass.getDeclaredFields();
                    for (Field f : fields) {
                        int modifier = f.getModifiers();
                        if (!(Modifier.isPrivate(modifier) || Modifier.isStatic(modifier) || Modifier.isInterface(modifier))) {
                            Class<?> fieldType = f.getType();
                            String fname = fieldType.toString();
                            try {
                                if (!fieldType.isPrimitive()) {
                                    GenericObject obj = f.get(this);
                                    Object mobj = f.get(mergeObject);
                                    if (mobj != null) {
                                        if (obj == null) {
                                            f.set(this, mobj);
                                        } else if (obj instanceof GenericObject) {
                                            obj.merge(mobj);
                                        } else {
                                            f.set(this, mobj);
                                        }
                                    }
                                } else if (fname.compareTo("int") == 0) {
                                    f.setInt(this, f.getInt(mergeObject));
                                } else if (fname.compareTo("short") == 0) {
                                    f.setShort(this, f.getShort(mergeObject));
                                } else if (fname.compareTo("char") == 0) {
                                    f.setChar(this, f.getChar(mergeObject));
                                } else if (fname.compareTo("long") == 0) {
                                    f.setLong(this, f.getLong(mergeObject));
                                } else if (fname.compareTo("boolean") == 0) {
                                    f.setBoolean(this, f.getBoolean(mergeObject));
                                } else if (fname.compareTo("double") == 0) {
                                    f.setDouble(this, f.getDouble(mergeObject));
                                } else if (fname.compareTo("float") == 0) {
                                    f.setFloat(this, f.getFloat(mergeObject));
                                }
                            } catch (IllegalAccessException ex1) {
                                ex1.printStackTrace();
                            }
                        }
                    }
                    myclass = myclass.getSuperclass();
                    if (myclass.equals(GenericObject.class)) {
                        return;
                    }
                }
            } else {
                throw new IllegalArgumentException("Bad override object");
            }
        }
    }

    protected GenericObject() {
    }

    protected String getIndentation() {
        char[] chars = new char[this.indentation];
        Arrays.fill(chars, ' ');
        return new String(chars);
    }

    protected void sprint(String a) {
        StringBuilder stringBuilder;
        if (a == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.stringRepresentation);
            stringBuilder.append(getIndentation());
            this.stringRepresentation = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.stringRepresentation);
            stringBuilder.append("<null>\n");
            this.stringRepresentation = stringBuilder.toString();
            return;
        }
        if (a.compareTo("}") == 0 || a.compareTo("]") == 0) {
            this.indentation--;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(this.stringRepresentation);
        stringBuilder.append(getIndentation());
        this.stringRepresentation = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(this.stringRepresentation);
        stringBuilder.append(a);
        this.stringRepresentation = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(this.stringRepresentation);
        stringBuilder.append("\n");
        this.stringRepresentation = stringBuilder.toString();
        if (a.compareTo("{") == 0 || a.compareTo("[") == 0) {
            this.indentation++;
        }
    }

    protected void sprint(Object o) {
        sprint(o.toString());
    }

    protected void sprint(int intField) {
        sprint(String.valueOf(intField));
    }

    protected void sprint(short shortField) {
        sprint(String.valueOf(shortField));
    }

    protected void sprint(char charField) {
        sprint(String.valueOf(charField));
    }

    protected void sprint(long longField) {
        sprint(String.valueOf(longField));
    }

    protected void sprint(boolean booleanField) {
        sprint(String.valueOf(booleanField));
    }

    protected void sprint(double doubleField) {
        sprint(String.valueOf(doubleField));
    }

    protected void sprint(float floatField) {
        sprint(String.valueOf(floatField));
    }

    protected void dbgPrint() {
        Debug.println(debugDump());
    }

    protected void dbgPrint(String s) {
        Debug.println(s);
    }

    /* JADX WARNING: Missing block: B:76:0x0133, code skipped:
            if (r5.equals(gov.nist.core.GenericObject.class) == false) goto L_0x0137;
     */
    /* JADX WARNING: Missing block: B:77:0x0136, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean equals(Object that) {
        Object obj = that;
        if (obj == null || !getClass().equals(that.getClass())) {
            return false;
        }
        Class<?> myclass = getClass();
        Class<?> hisclass = that.getClass();
        Class<?> myclass2 = myclass;
        while (true) {
            Field[] fields = myclass2.getDeclaredFields();
            Field[] hisfields = hisclass.getDeclaredFields();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= fields.length) {
                    break;
                }
                Field f = fields[i2];
                Field g = hisfields[i2];
                if ((f.getModifiers() & 2) != 2) {
                    Class<?> fieldType = f.getType();
                    String fieldName = f.getName();
                    if (!(fieldName.compareTo("stringRepresentation") == 0 || fieldName.compareTo("indentation") == 0)) {
                        try {
                            if (fieldType.isPrimitive()) {
                                String fname = fieldType.toString();
                                if (fname.compareTo("int") == 0) {
                                    if (f.getInt(this) != g.getInt(obj)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("short") == 0) {
                                    if (f.getShort(this) != g.getShort(obj)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("char") == 0) {
                                    if (f.getChar(this) != g.getChar(obj)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("long") == 0) {
                                    if (f.getLong(this) != g.getLong(obj)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("boolean") == 0) {
                                    if (f.getBoolean(this) != g.getBoolean(obj)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("double") == 0) {
                                    if (f.getDouble(this) != g.getDouble(obj)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("float") == 0 && f.getFloat(this) != g.getFloat(obj)) {
                                    return false;
                                }
                            } else if (g.get(obj) == f.get(this)) {
                                return true;
                            } else {
                                if (f.get(this) == null || g.get(obj) == null) {
                                    return false;
                                }
                                if ((g.get(obj) == null && f.get(this) != null) || !f.get(this).equals(g.get(obj))) {
                                    return false;
                                }
                            }
                        } catch (IllegalAccessException ex1) {
                            InternalErrorHandler.handleException(ex1);
                        }
                    }
                }
                i = i2 + 1;
            }
            myclass2 = myclass2.getSuperclass();
            hisclass = hisclass.getSuperclass();
        }
    }

    public boolean match(Object other) {
        Exception ex1;
        boolean z;
        GenericObject genericObject;
        Object genericObject2 = this;
        if (other == null) {
            return true;
        }
        boolean z2 = false;
        if (!getClass().equals(other.getClass())) {
            return false;
        }
        GenericObject that = (GenericObject) other;
        Field[] fields = getClass().getDeclaredFields();
        Field[] hisfields = other.getClass().getDeclaredFields();
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= fields.length) {
                return true;
            }
            Field f = fields[i2];
            Field g = hisfields[i2];
            if ((f.getModifiers() & 2) != 2) {
                Class<?> fieldType = f.getType();
                String fieldName = f.getName();
                if (!(fieldName.compareTo("stringRepresentation") == 0 || fieldName.compareTo("indentation") == 0)) {
                    try {
                        if (fieldType.isPrimitive()) {
                            String fname = fieldType.toString();
                            if (fname.compareTo("int") == 0) {
                                try {
                                    if (f.getInt(genericObject2) != g.getInt(that)) {
                                        return false;
                                    }
                                } catch (IllegalAccessException e) {
                                    ex1 = e;
                                    z = false;
                                    InternalErrorHandler.handleException(ex1);
                                    i = i2 + 1;
                                    z2 = z;
                                    genericObject2 = this;
                                }
                            } else if (fname.compareTo("short") == 0) {
                                if (f.getShort(genericObject2) != g.getShort(that)) {
                                    return false;
                                }
                            } else if (fname.compareTo("char") == 0) {
                                if (f.getChar(genericObject2) != g.getChar(that)) {
                                    return false;
                                }
                            } else if (fname.compareTo("long") == 0) {
                                if (f.getLong(genericObject2) != g.getLong(that)) {
                                    return false;
                                }
                            } else if (fname.compareTo("boolean") == 0) {
                                if (f.getBoolean(genericObject2) != g.getBoolean(that)) {
                                    return false;
                                }
                            } else if (fname.compareTo("double") == 0) {
                                if (f.getDouble(genericObject2) != g.getDouble(that)) {
                                    return false;
                                }
                            } else if (fname.compareTo("float") == 0 && f.getFloat(genericObject2) != g.getFloat(that)) {
                                return false;
                            }
                        }
                        Object myObj = f.get(genericObject2);
                        Object hisObj = g.get(that);
                        if (hisObj != null && myObj == null) {
                            return false;
                        }
                        if (hisObj != null || myObj == null) {
                            if (hisObj != null || myObj != null) {
                                if ((hisObj instanceof String) && (myObj instanceof String)) {
                                    if (!((String) hisObj).trim().equals("")) {
                                        if (((String) myObj).compareToIgnoreCase((String) hisObj) != 0) {
                                            return false;
                                        }
                                    }
                                } else if (isMySubclass(myObj.getClass()) && !((GenericObject) myObj).match(hisObj)) {
                                    return false;
                                } else {
                                    if (GenericObjectList.isMySubclass(myObj.getClass()) && !((GenericObjectList) myObj).match(hisObj)) {
                                        return false;
                                    }
                                }
                            }
                        }
                        z = false;
                        i = i2 + 1;
                        z2 = z;
                        genericObject2 = this;
                        z = false;
                    } catch (IllegalAccessException e2) {
                        ex1 = e2;
                        z = z2;
                        InternalErrorHandler.handleException(ex1);
                        i = i2 + 1;
                        z2 = z;
                        genericObject2 = this;
                    }
                    i = i2 + 1;
                    z2 = z;
                    genericObject2 = this;
                }
            }
            z = z2;
            i = i2 + 1;
            z2 = z;
            genericObject2 = this;
        }
    }

    public String debugDump() {
        this.stringRepresentation = "";
        Class<?> myclass = getClass();
        sprint(myclass.getName());
        sprint("{");
        Field[] fields = myclass.getDeclaredFields();
        for (Field f : fields) {
            if ((f.getModifiers() & 2) != 2) {
                Class<?> fieldType = f.getType();
                String fieldName = f.getName();
                if (!(fieldName.compareTo("stringRepresentation") == 0 || fieldName.compareTo("indentation") == 0)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(fieldName);
                    stringBuilder.append(":");
                    sprint(stringBuilder.toString());
                    try {
                        if (fieldType.isPrimitive()) {
                            String fname = fieldType.toString();
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(fname);
                            stringBuilder2.append(":");
                            sprint(stringBuilder2.toString());
                            if (fname.compareTo("int") == 0) {
                                sprint(f.getInt(this));
                            } else if (fname.compareTo("short") == 0) {
                                sprint(f.getShort(this));
                            } else if (fname.compareTo("char") == 0) {
                                sprint(f.getChar(this));
                            } else if (fname.compareTo("long") == 0) {
                                sprint(f.getLong(this));
                            } else if (fname.compareTo("boolean") == 0) {
                                sprint(f.getBoolean(this));
                            } else if (fname.compareTo("double") == 0) {
                                sprint(f.getDouble(this));
                            } else if (fname.compareTo("float") == 0) {
                                sprint(f.getFloat(this));
                            }
                        } else if (GenericObject.class.isAssignableFrom(fieldType)) {
                            if (f.get(this) != null) {
                                sprint(((GenericObject) f.get(this)).debugDump(this.indentation + 1));
                            } else {
                                sprint("<null>");
                            }
                        } else if (!GenericObjectList.class.isAssignableFrom(fieldType)) {
                            if (f.get(this) != null) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(f.get(this).getClass().getName());
                                stringBuilder.append(":");
                                sprint(stringBuilder.toString());
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(fieldType.getName());
                                stringBuilder.append(":");
                                sprint(stringBuilder.toString());
                            }
                            sprint("{");
                            if (f.get(this) != null) {
                                sprint(f.get(this).toString());
                            } else {
                                sprint("<null>");
                            }
                            sprint("}");
                        } else if (f.get(this) != null) {
                            sprint(((GenericObjectList) f.get(this)).debugDump(this.indentation + 1));
                        } else {
                            sprint("<null>");
                        }
                    } catch (IllegalAccessException e) {
                    } catch (Exception ex) {
                        InternalErrorHandler.handleException(ex);
                    }
                }
            }
        }
        sprint("}");
        return this.stringRepresentation;
    }

    public String debugDump(int indent) {
        this.indentation = indent;
        String retval = debugDump();
        this.indentation = 0;
        return retval;
    }

    public StringBuffer encode(StringBuffer buffer) {
        buffer.append(encode());
        return buffer;
    }
}
