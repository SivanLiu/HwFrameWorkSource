package com.huawei.nb.searchmanager.client.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.nb.utils.logger.DSLog;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class IndexData implements Parcelable {
    public static final Creator<IndexData> CREATOR = new Creator<IndexData>() {
        /* class com.huawei.nb.searchmanager.client.model.IndexData.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public IndexData createFromParcel(Parcel in) {
            return new IndexData(in);
        }

        @Override // android.os.Parcelable.Creator
        public IndexData[] newArray(int size) {
            return new IndexData[size];
        }
    };
    private static final String TAG = "IndexData";
    private Map<String, Object> values = new HashMap();

    public IndexData() {
    }

    protected IndexData(Parcel in) {
        readFromParcel(in);
    }

    public Map<String, Object> getValues() {
        return this.values;
    }

    public void setValues(Map<String, Object> values2) {
        this.values = values2;
    }

    public void put(String key, String value) {
        if (key == null || value == null) {
            DSLog.et(TAG, "null String value cannot put in IndexData", new Object[0]);
        } else {
            this.values.put(key, value);
        }
    }

    public void put(String key, Integer value) {
        if (key == null || value == null) {
            DSLog.et(TAG, "null Integer value cannot put in IndexData", new Object[0]);
        } else {
            this.values.put(key, value);
        }
    }

    public void put(String key, Float value) {
        if (key == null || value == null) {
            DSLog.et(TAG, "null Float value cannot put in IndexData", new Object[0]);
        } else {
            this.values.put(key, value);
        }
    }

    public void put(String key, Double value) {
        if (key == null || value == null) {
            DSLog.et(TAG, "null Double value cannot put in IndexData", new Object[0]);
        } else {
            this.values.put(key, value);
        }
    }

    public void put(String key, Long value) {
        if (key == null || value == null) {
            DSLog.et(TAG, "put Long key and value cannot be null", new Object[0]);
        } else {
            this.values.put(key, value);
        }
    }

    public Object get(String key) {
        return this.values.get(key);
    }

    public String getAsString(String key) {
        Object value = this.values.get(key);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    public Integer getAsInteger(String key) {
        Integer num;
        Object value = this.values.get(key);
        if (value != null) {
            try {
                num = Integer.valueOf(((Number) value).intValue());
            } catch (ClassCastException e) {
                if (value instanceof CharSequence) {
                    try {
                        return Integer.valueOf(value.toString());
                    } catch (NumberFormatException e2) {
                        DSLog.et(TAG, "Cannot parse value to Integer", new Object[0]);
                        return null;
                    }
                } else {
                    DSLog.et(TAG, "Cannot cast value to Integer", new Object[0]);
                    return null;
                }
            }
        } else {
            num = null;
        }
        return num;
    }

    public Float getAsFloat(String key) {
        Float f;
        Object value = this.values.get(key);
        if (value != null) {
            try {
                f = Float.valueOf(((Number) value).floatValue());
            } catch (ClassCastException e) {
                if (value instanceof CharSequence) {
                    try {
                        return Float.valueOf(value.toString());
                    } catch (NumberFormatException e2) {
                        DSLog.et(TAG, "Cannot parse value to Float", new Object[0]);
                        return null;
                    }
                } else {
                    DSLog.et(TAG, "Cannot cast value to Float", new Object[0]);
                    return null;
                }
            }
        } else {
            f = null;
        }
        return f;
    }

    public Double getAsDouble(String key) {
        Double d;
        Object value = this.values.get(key);
        if (value != null) {
            try {
                d = Double.valueOf(((Number) value).doubleValue());
            } catch (ClassCastException e) {
                if (value instanceof CharSequence) {
                    try {
                        return Double.valueOf(value.toString());
                    } catch (NumberFormatException e2) {
                        DSLog.et(TAG, "Cannot parse value to Double", new Object[0]);
                        return null;
                    }
                } else {
                    DSLog.et(TAG, "Cannot cast value to Double", new Object[0]);
                    return null;
                }
            }
        } else {
            d = null;
        }
        return d;
    }

    public Long getAsLong(String key) {
        Long l;
        Object value = this.values.get(key);
        if (value != null) {
            try {
                l = Long.valueOf(((Number) value).longValue());
            } catch (ClassCastException e) {
                if (value instanceof CharSequence) {
                    try {
                        return Long.valueOf(value.toString());
                    } catch (NumberFormatException e2) {
                        DSLog.et(TAG, "Cannot parse value to Long", new Object[0]);
                        return null;
                    }
                } else {
                    DSLog.et(TAG, "Cannot cast value to Long", new Object[0]);
                    return null;
                }
            }
        } else {
            l = null;
        }
        return l;
    }

    public int size() {
        return this.values.size();
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    public void remove(String key) {
        this.values.remove(key);
    }

    public void clear() {
        this.values.clear();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeMap(this.values);
    }

    private void readFromParcel(Parcel parcel) {
        parcel.readMap(this.values, null);
    }

    public String toJson() {
        return new JSONObject(this.values).toString();
    }

    public IndexData fromJson(String jsonValue) {
        if (jsonValue != null && !jsonValue.trim().equals("")) {
            try {
                JSONObject jsonObject = new JSONObject(jsonValue);
                Iterator<String> it = jsonObject.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    if (key == null) {
                        DSLog.et(TAG, "key is null, skip", new Object[0]);
                    } else {
                        Object object = jsonObject.get(key);
                        if (object == null) {
                            DSLog.et(TAG, "value is null, skip", new Object[0]);
                        } else {
                            this.values.put(key, object);
                        }
                    }
                }
            } catch (JSONException e) {
                DSLog.et(TAG, "throw JSONException: " + e.getMessage(), new Object[0]);
            }
        }
        return this;
    }
}
