package android.filterfw.core;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map.Entry;

public class KeyValueMap extends HashMap<String, Object> {
    public void setKeyValues(Object... keyValues) {
        if (keyValues.length % 2 == 0) {
            int i = 0;
            while (i < keyValues.length) {
                if (keyValues[i] instanceof String) {
                    put(keyValues[i], keyValues[i + 1]);
                    i += 2;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Key-value argument ");
                    stringBuilder.append(i);
                    stringBuilder.append(" must be a key of type String, but found an object of type ");
                    stringBuilder.append(keyValues[i].getClass());
                    stringBuilder.append("!");
                    throw new RuntimeException(stringBuilder.toString());
                }
            }
            return;
        }
        throw new RuntimeException("Key-Value arguments passed into setKeyValues must be an alternating list of keys and values!");
    }

    public static KeyValueMap fromKeyValues(Object... keyValues) {
        KeyValueMap result = new KeyValueMap();
        result.setKeyValues(keyValues);
        return result;
    }

    public String getString(String key) {
        Object result = get(key);
        return result != null ? (String) result : null;
    }

    public int getInt(String key) {
        Object result = get(key);
        return (result != null ? (Integer) result : null).intValue();
    }

    public float getFloat(String key) {
        Object result = get(key);
        return (result != null ? (Float) result : null).floatValue();
    }

    public String toString() {
        StringWriter writer = new StringWriter();
        for (Entry<String, Object> entry : entrySet()) {
            String valueString;
            Object value = entry.getValue();
            if (value instanceof String) {
                valueString = new StringBuilder();
                valueString.append("\"");
                valueString.append(value);
                valueString.append("\"");
                valueString = valueString.toString();
            } else {
                valueString = value.toString();
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append((String) entry.getKey());
            stringBuilder.append(" = ");
            stringBuilder.append(valueString);
            stringBuilder.append(";\n");
            writer.write(stringBuilder.toString());
        }
        return writer.toString();
    }
}
