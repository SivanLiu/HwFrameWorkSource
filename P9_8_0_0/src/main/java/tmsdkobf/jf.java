package tmsdkobf;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import java.util.Map.Entry;

public class jf extends ContentProvider {
    private Context mContext;

    public jf(Context context) {
        this.mContext = context;
    }

    private String a(Uri uri) {
        return uri.getPathSegments().size() <= 0 ? null : (String) uri.getPathSegments().get(0);
    }

    private String b(Object obj) {
        if (obj instanceof String) {
            return "String";
        }
        if (obj instanceof Integer) {
            return "Int";
        }
        if (obj instanceof Boolean) {
            return "Boolean";
        }
        if (obj instanceof Float) {
            return "Float";
        }
        if (obj instanceof Long) {
            return "Long";
        }
        throw new RuntimeException("cannot parse type def!");
    }

    public int delete(Uri uri, String str, String[] strArr) {
        Object -l_4_R = a(uri);
        if (-l_4_R != null) {
            Object -l_6_R = this.mContext.getSharedPreferences(-l_4_R, 0).edit();
            if (str != null) {
                -l_6_R.remove(str);
            } else {
                -l_6_R.clear();
            }
            return -l_6_R.commit() == 0 ? 0 : 1;
        } else {
            throw new RuntimeException("[delete] sharedPreferences failed:file name should not be null(uri=" + uri + ")");
        }
    }

    public String getType(Uri uri) {
        return null;
    }

    public Uri insert(Uri uri, ContentValues contentValues) {
        Object -l_3_R = a(uri);
        if (-l_3_R == null || contentValues == null) {
            throw new RuntimeException(new StringBuilder().append("[insert] sharedPreferences failed:").append(-l_3_R).toString() != null ? "valuesshould not be null(uri=" + uri + ")" : "file name ");
        }
        Object -l_5_R = this.mContext.getSharedPreferences(-l_3_R, 0).edit();
        for (Entry -l_7_R : contentValues.valueSet()) {
            String -l_8_R = (String) -l_7_R.getKey();
            Object -l_9_R = -l_7_R.getValue();
            if (-l_9_R == null) {
                -l_5_R.remove(-l_8_R);
            } else if (-l_9_R instanceof String) {
                -l_5_R.putString(-l_8_R, -l_9_R.toString());
            } else if (-l_9_R instanceof Integer) {
                -l_5_R.putInt(-l_8_R, Integer.parseInt(-l_9_R.toString()));
            } else if (-l_9_R instanceof Boolean) {
                -l_5_R.putBoolean(-l_8_R, Boolean.parseBoolean(-l_9_R.toString()));
            } else if (-l_9_R instanceof Float) {
                -l_5_R.putFloat(-l_8_R, Float.parseFloat(-l_9_R.toString()));
            } else if (-l_9_R instanceof Long) {
                -l_5_R.putLong(-l_8_R, Long.parseLong(-l_9_R.toString()));
            } else {
                throw new IllegalArgumentException("not supported type.");
            }
        }
        return !-l_5_R.commit() ? null : uri;
    }

    public boolean onCreate() {
        return true;
    }

    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String -l_9_R) {
        Object -l_6_R = a(uri);
        if (-l_6_R != null) {
            SharedPreferences -l_7_R = this.mContext.getSharedPreferences(-l_6_R, 0);
            Object -l_8_R = null;
            MatrixCursor -l_8_R2;
            Object -l_11_R;
            if (-l_9_R != null) {
                -l_8_R2 = new MatrixCursor(new String[]{"value"}, 1);
                String -l_10_R = (String) uri.getPathSegments().get(1);
                if (-l_7_R.contains(-l_9_R)) {
                    if ("String".equals(-l_10_R)) {
                        -l_11_R = -l_7_R.getString(-l_9_R, null);
                        -l_8_R2.addRow(new String[]{-l_11_R});
                    } else if ("Boolean".equals(-l_10_R)) {
                        Integer[] numArr = new Integer[1];
                        numArr[0] = Integer.valueOf(-l_7_R.getBoolean(-l_9_R, false) == 0 ? 0 : 1);
                        -l_8_R2.addRow(numArr);
                    } else if ("Int".equals(-l_10_R)) {
                        int -l_11_I = -l_7_R.getInt(-l_9_R, 0);
                        -l_8_R2.addRow(new Integer[]{Integer.valueOf(-l_11_I)});
                    } else if ("Float".equals(-l_10_R)) {
                        float -l_11_F = -l_7_R.getFloat(-l_9_R, 0.0f);
                        -l_8_R2.addRow(new Float[]{Float.valueOf(-l_11_F)});
                    } else if ("Long".equals(-l_10_R)) {
                        long -l_11_J = -l_7_R.getLong(-l_9_R, 0);
                        -l_8_R2.addRow(new Long[]{Long.valueOf(-l_11_J)});
                    } else {
                        throw new RuntimeException("cannot parse type def!");
                    }
                }
                return -l_8_R2;
            }
            Object -l_10_R2 = -l_7_R.getAll();
            if (-l_10_R2 != null) {
                -l_8_R2 = new MatrixCursor(new String[]{"key", "value", "typedef"}, -l_10_R2.size());
                for (Entry -l_12_R : -l_10_R2.entrySet()) {
                    String -l_13_R = (String) -l_12_R.getKey();
                    Object -l_14_R = -l_12_R.getValue();
                    Object -l_15_R = b(-l_14_R);
                    if (-l_15_R != null) {
                        if ("Boolean".equals(-l_15_R)) {
                            Boolean -l_16_R = (Boolean) -l_14_R;
                            Object[] objArr = new Object[3];
                            objArr[0] = -l_13_R;
                            objArr[1] = Integer.valueOf(!-l_16_R.booleanValue() ? 0 : 1);
                            objArr[2] = -l_15_R;
                            -l_8_R2.addRow(objArr);
                        } else {
                            -l_8_R2.addRow(new Object[]{-l_13_R, -l_14_R, -l_15_R});
                        }
                    }
                }
            }
            return -l_8_R;
        }
        throw new RuntimeException(new StringBuilder().append("[query] sharedPreferences failed:").append(-l_6_R).toString() != null ? "selectionshould not be null(uri=" + uri + ")" : "file name ");
    }

    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        throw new UnsupportedOperationException("[update] not implement");
    }
}
