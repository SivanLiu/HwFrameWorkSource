package gov.nist.javax.sip.header;

import gov.nist.core.GenericObject;
import gov.nist.core.GenericObjectList;
import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import java.io.PrintStream;
import java.lang.reflect.Field;

public abstract class SIPObject extends GenericObject {
    public abstract String encode();

    protected SIPObject() {
    }

    public void dbgPrint() {
        super.dbgPrint();
    }

    public StringBuffer encode(StringBuffer buffer) {
        buffer.append(encode());
        return buffer;
    }

    /* JADX WARNING: Missing block: B:90:0x017c, code skipped:
            if (r5.equals(gov.nist.javax.sip.header.SIPObject.class) == false) goto L_0x0181;
     */
    /* JADX WARNING: Missing block: B:92:0x0180, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean equals(Object other) {
        Exception ex1;
        PrintStream printStream;
        StringBuilder stringBuilder;
        PrintStream printStream2;
        SIPObject sIPObject;
        Object obj = this;
        boolean z = false;
        if (!getClass().equals(other.getClass())) {
            return false;
        }
        SIPObject that = (SIPObject) other;
        Class myclass = getClass();
        Class hisclass = other.getClass();
        Class myclass2 = myclass;
        while (true) {
            Field[] fields = myclass2.getDeclaredFields();
            if (hisclass.equals(myclass2)) {
                Field[] hisfields = hisclass.getDeclaredFields();
                int i = z;
                while (true) {
                    int i2 = i;
                    if (i2 >= fields.length) {
                        break;
                    }
                    Field f = fields[i2];
                    Field g = hisfields[i2];
                    int modifier = f.getModifiers();
                    if ((modifier & 2) != 2) {
                        Class fieldType = f.getType();
                        String fieldName = f.getName();
                        if (!(fieldName.compareTo("stringRepresentation") == 0 || fieldName.compareTo("indentation") == 0)) {
                            try {
                                if (fieldType.isPrimitive()) {
                                    String fname = fieldType.toString();
                                    if (fname.compareTo("int") == 0) {
                                        try {
                                            if (f.getInt(obj) != g.getInt(that)) {
                                                return false;
                                            }
                                        } catch (IllegalAccessException e) {
                                            ex1 = e;
                                            printStream = System.out;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("accessed field ");
                                            stringBuilder.append(fieldName);
                                            printStream.println(stringBuilder.toString());
                                            printStream2 = System.out;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("modifier  ");
                                            stringBuilder.append(modifier);
                                            printStream2.println(stringBuilder.toString());
                                            System.out.println("modifier.private  2");
                                            InternalErrorHandler.handleException(ex1);
                                            i = i2 + 1;
                                            sIPObject = this;
                                        }
                                    } else if (fname.compareTo("short") == 0) {
                                        if (f.getShort(obj) != g.getShort(that)) {
                                            return false;
                                        }
                                    } else if (fname.compareTo("char") == 0) {
                                        if (f.getChar(obj) != g.getChar(that)) {
                                            return false;
                                        }
                                    } else if (fname.compareTo("long") == 0) {
                                        if (f.getLong(obj) != g.getLong(that)) {
                                            return false;
                                        }
                                    } else if (fname.compareTo("boolean") == 0) {
                                        if (f.getBoolean(obj) != g.getBoolean(that)) {
                                            return false;
                                        }
                                    } else if (fname.compareTo("double") == 0) {
                                        if (f.getDouble(obj) != g.getDouble(that)) {
                                            return false;
                                        }
                                    } else if (fname.compareTo("float") == 0 && f.getFloat(obj) != g.getFloat(that)) {
                                        return false;
                                    }
                                } else if (g.get(that) != f.get(obj)) {
                                    if (f.get(obj) == null && g.get(that) != null) {
                                        return false;
                                    }
                                    if (g.get(that) == null && f.get(obj) != null) {
                                        return false;
                                    }
                                    if (!f.get(obj).equals(g.get(that))) {
                                        return false;
                                    }
                                }
                            } catch (IllegalAccessException e2) {
                                ex1 = e2;
                                printStream = System.out;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("accessed field ");
                                stringBuilder.append(fieldName);
                                printStream.println(stringBuilder.toString());
                                printStream2 = System.out;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("modifier  ");
                                stringBuilder.append(modifier);
                                printStream2.println(stringBuilder.toString());
                                System.out.println("modifier.private  2");
                                InternalErrorHandler.handleException(ex1);
                                i = i2 + 1;
                                sIPObject = this;
                            }
                        }
                    }
                    i = i2 + 1;
                    sIPObject = this;
                }
            } else {
                return z;
            }
            myclass2 = myclass2.getSuperclass();
            hisclass = hisclass.getSuperclass();
            sIPObject = this;
            z = false;
        }
    }

    /* JADX WARNING: Missing block: B:122:0x01c2, code skipped:
            r1 = r3;
     */
    /* JADX WARNING: Missing block: B:123:0x01c9, code skipped:
            if (r6.equals(gov.nist.javax.sip.header.SIPObject.class) == false) goto L_0x01ce;
     */
    /* JADX WARNING: Missing block: B:125:0x01cd, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean match(Object other) {
        Exception ex1;
        boolean z;
        SIPObject sIPObject;
        Object obj = this;
        if (other == null) {
            return true;
        }
        boolean z2 = false;
        if (!getClass().equals(other.getClass())) {
            return false;
        }
        GenericObject that = (GenericObject) other;
        Class myclass = getClass();
        Class hisclass = other.getClass();
        Class myclass2 = myclass;
        while (true) {
            Field[] fields = myclass2.getDeclaredFields();
            Field[] hisfields = hisclass.getDeclaredFields();
            int i = z2;
            while (true) {
                int i2 = i;
                if (i2 >= fields.length) {
                    break;
                }
                Field f = fields[i2];
                Field g = hisfields[i2];
                if ((f.getModifiers() & 2) != 2) {
                    Class fieldType = f.getType();
                    String fieldName = f.getName();
                    if (!(fieldName.compareTo("stringRepresentation") == 0 || fieldName.compareTo("indentation") == 0)) {
                        try {
                            if (fieldType.isPrimitive()) {
                                String fname = fieldType.toString();
                                if (fname.compareTo("int") == 0) {
                                    try {
                                        if (f.getInt(obj) != g.getInt(that)) {
                                            return false;
                                        }
                                    } catch (IllegalAccessException e) {
                                        ex1 = e;
                                        z = false;
                                        InternalErrorHandler.handleException(ex1);
                                        i = i2 + 1;
                                        z2 = z;
                                        sIPObject = this;
                                    }
                                } else if (fname.compareTo("short") == 0) {
                                    if (f.getShort(obj) != g.getShort(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("char") == 0) {
                                    if (f.getChar(obj) != g.getChar(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("long") == 0) {
                                    if (f.getLong(obj) != g.getLong(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("boolean") == 0) {
                                    if (f.getBoolean(obj) != g.getBoolean(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("double") == 0) {
                                    if (f.getDouble(obj) != g.getDouble(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("float") != 0) {
                                    InternalErrorHandler.handleException("unknown type");
                                } else if (f.getFloat(obj) != g.getFloat(that)) {
                                    return false;
                                }
                            }
                            Object myObj = f.get(obj);
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
                                    } else if (hisObj != null && GenericObject.isMySubclass(myObj.getClass()) && GenericObject.isMySubclass(hisObj.getClass()) && myObj.getClass().equals(hisObj.getClass()) && ((GenericObject) hisObj).getMatcher() != null) {
                                        if (!((GenericObject) hisObj).getMatcher().match(((GenericObject) myObj).encode())) {
                                            return false;
                                        }
                                    } else if (GenericObject.isMySubclass(myObj.getClass()) && !((GenericObject) myObj).match(hisObj)) {
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
                            sIPObject = this;
                            z = false;
                        } catch (IllegalAccessException e2) {
                            ex1 = e2;
                            z = z2;
                            InternalErrorHandler.handleException(ex1);
                            i = i2 + 1;
                            z2 = z;
                            sIPObject = this;
                        }
                        i = i2 + 1;
                        z2 = z;
                        sIPObject = this;
                    }
                }
                z = z2;
                i = i2 + 1;
                z2 = z;
                sIPObject = this;
            }
            myclass2 = myclass2.getSuperclass();
            hisclass = hisclass.getSuperclass();
            z2 = z;
            sIPObject = this;
        }
    }

    public String debugDump() {
        this.stringRepresentation = "";
        Class myclass = getClass();
        sprint(myclass.getName());
        sprint("{");
        Field[] fields = myclass.getDeclaredFields();
        for (Field f : fields) {
            if ((f.getModifiers() & 2) != 2) {
                Class fieldType = f.getType();
                String fieldName = f.getName();
                if (!(fieldName.compareTo("stringRepresentation") == 0 || fieldName.compareTo("indentation") == 0)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(fieldName);
                    stringBuilder.append(Separators.COLON);
                    sprint(stringBuilder.toString());
                    try {
                        if (fieldType.isPrimitive()) {
                            String fname = fieldType.toString();
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(fname);
                            stringBuilder2.append(Separators.COLON);
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
                                stringBuilder.append(Separators.COLON);
                                sprint(stringBuilder.toString());
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(fieldType.getName());
                                stringBuilder.append(Separators.COLON);
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
                    }
                }
            }
        }
        sprint("}");
        return this.stringRepresentation;
    }

    public String debugDump(int indent) {
        int save = this.indentation;
        this.indentation = indent;
        String retval = debugDump();
        this.indentation = save;
        return retval;
    }

    public String toString() {
        return encode();
    }
}
