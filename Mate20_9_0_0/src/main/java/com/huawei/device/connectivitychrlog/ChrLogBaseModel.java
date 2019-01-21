package com.huawei.device.connectivitychrlog;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ChrLogBaseModel {
    protected static final int ENCRYPT_LENGTH = 2;
    private static final String GET_LENGTH = "getLength";
    private static final String SET_BYTE_VALUE = "setByByteArray";
    private static final String TO_BYTE_ARRAY = "toByteArray";
    private final String LOG_TAG;
    Map<String, Object> fieldMap = new LinkedHashMap();
    Map<String, Integer> lengthMap = new LinkedHashMap();

    public ChrLogBaseModel() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ChrLogBaseModel_");
        stringBuilder.append(getClass().getSimpleName());
        this.LOG_TAG = stringBuilder.toString();
    }

    public byte[] toByteArray() {
        ByteBuffer bytebuf = ByteBuffer.wrap(new byte[getTotalBytes()]);
        Iterator<Entry<String, Object>> iterator = this.fieldMap.entrySet().iterator();
        Iterator<Entry<String, Integer>> lenthIterator = this.lengthMap.entrySet().iterator();
        while (iterator.hasNext() && lenthIterator.hasNext()) {
            Entry lengthEntry = (Entry) lenthIterator.next();
            Object key = ((Entry) iterator.next()).getValue();
            int lengthKey = ((Integer) lengthEntry.getValue()).intValue();
            if (key == null) {
                if (lengthKey == 1) {
                    try {
                        bytebuf.put((byte) -1);
                    } catch (Exception e) {
                        String str = this.LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("toByteArray exception");
                        stringBuilder.append(e);
                        ChrLog.chrLogE(str, stringBuilder.toString());
                    }
                } else if (lengthKey == 2) {
                    bytebuf.put(ByteConvert.shortToBytes((short) -1));
                } else {
                    String str2 = this.LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("toByteArray exception, invalid class key length  = ");
                    stringBuilder2.append(lengthKey);
                    ChrLog.chrLogE(str2, stringBuilder2.toString());
                }
            } else if (key instanceof ArrayList) {
                if (((ArrayList) key).size() > 0) {
                    bytebuf.put(ByteConvert.shortToBytes((short) ((ArrayList) key).size()));
                } else {
                    bytebuf.put(ByteConvert.shortToBytes((short) -1));
                }
                Iterator it = ((ArrayList) key).iterator();
                while (it.hasNext()) {
                    Object item = it.next();
                    bytebuf.put((byte[]) item.getClass().getMethod(TO_BYTE_ARRAY, new Class[0]).invoke(item, new Object[0]));
                }
            } else {
                bytebuf.put((byte[]) key.getClass().getMethod(TO_BYTE_ARRAY, new Class[0]).invoke(key, new Object[0]));
            }
        }
        return bytebuf.array();
    }

    public void setByByteArray(byte[] src, int len, boolean bIsLittleEndian) {
        int i = 0;
        int totalLen = getLength();
        if (totalLen != len) {
            String str = this.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setByByteArray totalLen = ");
            stringBuilder.append(totalLen);
            stringBuilder.append(", len = ");
            stringBuilder.append(len);
            ChrLog.chrLogW(str, stringBuilder.toString());
        }
        if (totalLen <= len) {
            for (Entry entry : this.fieldMap.entrySet()) {
                ArrayList<ChrLogBaseModel> key = entry.getValue();
                if (i >= len) {
                    String str2 = this.LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setByByteArray error, please check the xml and the vcom data struct , i = ");
                    stringBuilder2.append(i);
                    stringBuilder2.append(", len = ");
                    stringBuilder2.append(len);
                    ChrLog.chrLogE(str2, stringBuilder2.toString());
                    return;
                } else if (key == null) {
                    ChrLog.chrLogD(this.LOG_TAG, "setByByteArray handle class");
                    try {
                        Class<?> clazz = getObjectClassByHashMapKey((String) entry.getKey());
                        if (clazz == null) {
                            ChrLog.chrLogE(this.LOG_TAG, "setByByteArray failed , can not find class when key is null");
                            return;
                        }
                        ChrLogBaseModel model = (ChrLogBaseModel) clazz.newInstance();
                        i += setObjectByteValue(src, model, i);
                        setSubClass((String) entry.getKey(), model);
                    } catch (IllegalAccessException e) {
                        ChrLog.chrLogE(this.LOG_TAG, "setByByteArray IllegalAccessException");
                    } catch (InstantiationException e2) {
                        ChrLog.chrLogE(this.LOG_TAG, "setByByteArray InstantiationException");
                    }
                } else if ("enSubEventId".equals(entry.getKey())) {
                    ChrLog.chrLogD(this.LOG_TAG, "setByByteArray ingnore enSubEventId");
                } else if (key instanceof ArrayList) {
                    ChrLog.chrLogD(this.LOG_TAG, "setByByteArray handle ArrayList");
                    int listSize = getArrayListLength(key);
                    if (listSize > 0) {
                        try {
                            Class<?> clazz2 = getListElementClassByHashMapKey((String) entry.getKey());
                            if (clazz2 == null) {
                                ChrLog.chrLogE(this.LOG_TAG, "setByByteArray failed , can not find class when key is ArrayList");
                                return;
                            }
                            for (int k = 0; k < listSize; k++) {
                                ChrLogBaseModel model2 = (ChrLogBaseModel) clazz2.newInstance();
                                i += setObjectByteValue(src, model2, i);
                                setSubClass((String) entry.getKey(), model2);
                            }
                            String str3 = this.LOG_TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("setByByteArray handle ArrayList clazz = ");
                            stringBuilder3.append(clazz2);
                            stringBuilder3.append(", listSize = ");
                            stringBuilder3.append(listSize);
                            ChrLog.chrLogI(str3, stringBuilder3.toString());
                        } catch (InstantiationException e3) {
                            ChrLog.chrLogE(this.LOG_TAG, "setByByteArray InstantiationException");
                        } catch (IllegalAccessException e4) {
                            ChrLog.chrLogE(this.LOG_TAG, "setByByteArray IllegalAccessException");
                        }
                    } else {
                        ChrLog.chrLogE(this.LOG_TAG, "setByByteArray handle null ArrayList, this should not happen, please check the xml");
                        return;
                    }
                } else {
                    ChrLog.chrLogD(this.LOG_TAG, "setByByteArray handle base element");
                    i += setObjectByteValue(src, key, i);
                }
            }
        }
    }

    private int setObjectByteValue(byte[] src, Object key, int start) {
        int len = 0;
        try {
            len = ((Integer) key.getClass().getMethod(GET_LENGTH, new Class[0]).invoke(key, new Object[0])).intValue();
            if (len > 0) {
                byte[] b = new byte[len];
                Method methodSetByteValue = key.getClass().getMethod(SET_BYTE_VALUE, new Class[]{byte[].class, Integer.TYPE, Boolean.TYPE});
                System.arraycopy(src, start, b, 0, len);
                methodSetByteValue.invoke(key, new Object[]{b, Integer.valueOf(len), Boolean.valueOf(true)});
            } else {
                String str = this.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setObjectByteValue failed len = ");
                stringBuilder.append(len);
                stringBuilder.append(", class = ");
                stringBuilder.append(key.getClass());
                ChrLog.chrLogE(str, stringBuilder.toString());
            }
        } catch (NoSuchMethodException e) {
            ChrLog.chrLogE(this.LOG_TAG, "setObjectByteValue NoSuchMethodException");
        } catch (IllegalAccessException e2) {
            ChrLog.chrLogE(this.LOG_TAG, "setObjectByteValue IllegalAccessException");
        } catch (InvocationTargetException e3) {
            ChrLog.chrLogE(this.LOG_TAG, "setObjectByteValue InvocationTargetException");
        }
        return len;
    }

    public int getLength() {
        int totalLen = 0;
        for (Entry entry : this.fieldMap.entrySet()) {
            ArrayList<? extends ChrLogBaseModel> key = entry.getValue();
            if (key == null) {
                try {
                    Class<?> clazz = getObjectClassByHashMapKey((String) entry.getKey());
                    if (clazz == null) {
                        ChrLog.chrLogE(this.LOG_TAG, "getLength failed clazz is null");
                        return totalLen;
                    }
                    totalLen += ((Integer) clazz.getMethod(GET_LENGTH, new Class[0]).invoke((ChrLogBaseModel) clazz.newInstance(), new Object[0])).intValue();
                } catch (NoSuchMethodException e) {
                    ChrLog.chrLogE(this.LOG_TAG, "getLength NoSuchMethodException");
                } catch (IllegalAccessException e2) {
                    ChrLog.chrLogE(this.LOG_TAG, "getLength IllegalAccessException");
                } catch (InstantiationException e3) {
                    ChrLog.chrLogE(this.LOG_TAG, "getLength InstantiationException");
                } catch (InvocationTargetException e4) {
                    ChrLog.chrLogE(this.LOG_TAG, "getLength InvocationTargetException");
                }
            } else if (!"enSubEventId".equals(entry.getKey())) {
                if (key instanceof ArrayList) {
                    int listSize = getArrayListLength(key);
                    if (listSize > 0) {
                        try {
                            Class<?> clazz2 = getListElementClassByHashMapKey((String) entry.getKey());
                            totalLen += ((Integer) clazz2.getMethod(GET_LENGTH, new Class[0]).invoke((ChrLogBaseModel) clazz2.newInstance(), new Object[0])).intValue() * listSize;
                        } catch (NoSuchMethodException e5) {
                            ChrLog.chrLogE(this.LOG_TAG, "getLength NoSuchMethodException");
                        } catch (IllegalAccessException e6) {
                            ChrLog.chrLogE(this.LOG_TAG, "getLength IllegalAccessException");
                        } catch (InvocationTargetException e7) {
                            ChrLog.chrLogE(this.LOG_TAG, "getLength InvocationTargetException");
                        } catch (InstantiationException e8) {
                            ChrLog.chrLogE(this.LOG_TAG, "getLength InstantiationException");
                        }
                    }
                } else {
                    try {
                        totalLen += ((Integer) key.getClass().getMethod(GET_LENGTH, new Class[0]).invoke(key, new Object[0])).intValue();
                    } catch (NoSuchMethodException e9) {
                        ChrLog.chrLogE(this.LOG_TAG, "getLength NoSuchMethodException");
                    } catch (IllegalAccessException e10) {
                        ChrLog.chrLogE(this.LOG_TAG, "getLength IllegalAccessException");
                    } catch (InvocationTargetException e11) {
                        ChrLog.chrLogE(this.LOG_TAG, "getLength InvocationTargetException");
                    }
                }
            }
        }
        return totalLen;
    }

    int getTotalBytes() {
        int totalBytes = 0;
        for (Entry entry : this.lengthMap.entrySet()) {
            totalBytes += ((Integer) entry.getValue()).intValue();
        }
        return totalBytes;
    }

    private Class<?> getObjectClassByHashMapKey(String key) {
        String packageName = ChrLogBaseModel.class.getPackage().getName();
        String classFullName = new StringBuilder();
        classFullName.append(packageName);
        classFullName.append(".CSub");
        classFullName.append(key.substring(1));
        try {
            return Class.forName(classFullName.toString());
        } catch (ClassNotFoundException e) {
            ChrLog.chrLogE(this.LOG_TAG, "getObjectClassByHashMapKey ClassNotFoundException");
            ChrLog.chrLogE(this.LOG_TAG, "getObjectClassByHashMapKey failed , class is null");
            return null;
        }
    }

    private Class<?> getListElementClassByHashMapKey(String key) {
        String packageName = ChrLogBaseModel.class.getPackage().getName();
        String classFullName = new StringBuilder();
        classFullName.append(packageName);
        classFullName.append(".CSub");
        classFullName.append(key.substring(1, key.length() - 4));
        try {
            return Class.forName(classFullName.toString());
        } catch (ClassNotFoundException e) {
            ChrLog.chrLogE(this.LOG_TAG, "getListElementClassByHashMapKey ClassNotFoundException");
            ChrLog.chrLogE(this.LOG_TAG, "getListElementClassByHashMapKey failed , class is null");
            return null;
        }
    }

    private int getArrayListLength(ArrayList list) {
        if (list != null) {
            try {
                Field f = ArrayList.class.getDeclaredField("array");
                f.setAccessible(true);
                return ((Object[]) f.get(list)).length;
            } catch (IllegalAccessException e) {
                ChrLog.chrLogE(this.LOG_TAG, "getArrayListLength IllegalAccessException");
            } catch (NoSuchFieldException e2) {
                ChrLog.chrLogE(this.LOG_TAG, "getArrayListLength NoSuchFieldException");
            }
        }
        ChrLog.chrLogE(this.LOG_TAG, "getArrayListLength failed , please check the xml");
        return 0;
    }

    private void setSubClass(String key, ChrLogBaseModel model) {
        try {
            String funcName = new StringBuilder();
            funcName.append("setCSub");
            funcName.append(key.substring(1));
            funcName = funcName.toString();
            getClass().getMethod(funcName, new Class[]{model.getClass()}).invoke(this, new Object[]{model});
        } catch (NoSuchMethodException e) {
            ChrLog.chrLogE(this.LOG_TAG, "setSubClass NoSuchMethodException");
        } catch (IllegalAccessException e2) {
            ChrLog.chrLogE(this.LOG_TAG, "setSubClass IllegalAccessException");
        } catch (InvocationTargetException e3) {
            ChrLog.chrLogE(this.LOG_TAG, "setSubClass InvocationTargetException");
        }
    }

    public Map getfieldMap() {
        return this.fieldMap;
    }
}
