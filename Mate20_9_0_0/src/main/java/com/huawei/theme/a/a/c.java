package com.huawei.theme.a.a;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.json.JSONException;
import org.json.JSONObject;

public final class c {
    public static SharedPreferences a(Context context, String str) {
        return context.getSharedPreferences("hianalytics_" + str + "_" + context.getPackageName(), 0);
    }

    /* JADX WARNING: Removed duplicated region for block: B:8:0x0020 A:{ExcHandler: FileNotFoundException (e java.io.FileNotFoundException), Splitter:B:1:0x0006} */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x002c A:{ExcHandler: IOException (e java.io.IOException), Splitter:B:1:0x0006} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x003e A:{SYNTHETIC, Splitter:B:23:0x003e} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:9:0x0021, code skipped:
            if (r0 != null) goto L_0x0023;
     */
    /* JADX WARNING: Missing block: B:11:?, code skipped:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:12:0x0027, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:13:0x0028, code skipped:
            r0.printStackTrace();
     */
    /* JADX WARNING: Missing block: B:15:0x002d, code skipped:
            if (r0 != null) goto L_0x002f;
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:18:0x0033, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:19:0x0034, code skipped:
            r0.printStackTrace();
     */
    /* JADX WARNING: Missing block: B:20:0x0038, code skipped:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:21:0x0039, code skipped:
            r3 = r1;
            r1 = r0;
            r0 = r3;
     */
    /* JADX WARNING: Missing block: B:24:?, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:26:0x0042, code skipped:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:27:0x0043, code skipped:
            r1.printStackTrace();
     */
    /* JADX WARNING: Missing block: B:34:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:35:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:36:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:37:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:39:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:40:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void a(Context context, JSONObject jSONObject, String str) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = context.openFileOutput(d(context, str), 0);
            fileOutputStream.write(jSONObject.toString().getBytes("UTF-8"));
            fileOutputStream.flush();
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e2) {
        } catch (IOException e3) {
        } catch (Throwable th) {
            Throwable th2 = th;
            FileOutputStream fileOutputStream2 = fileOutputStream;
            Throwable th3 = th2;
            if (fileOutputStream2 != null) {
            }
            throw th3;
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:75:0x00ad=Splitter:B:75:0x00ad, B:61:0x008f=Splitter:B:61:0x008f} */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x0079 A:{SYNTHETIC, Splitter:B:50:0x0079} */
    /* JADX WARNING: Removed duplicated region for block: B:123:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x007e A:{SYNTHETIC, Splitter:B:53:0x007e} */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x0097 A:{SYNTHETIC, Splitter:B:64:0x0097} */
    /* JADX WARNING: Removed duplicated region for block: B:125:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x009c A:{SYNTHETIC, Splitter:B:67:0x009c} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x00b2 A:{SYNTHETIC, Splitter:B:78:0x00b2} */
    /* JADX WARNING: Removed duplicated region for block: B:127:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x00b7 A:{SYNTHETIC, Splitter:B:81:0x00b7} */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x00cd A:{SYNTHETIC, Splitter:B:90:0x00cd} */
    /* JADX WARNING: Removed duplicated region for block: B:93:0x00d2 A:{SYNTHETIC, Splitter:B:93:0x00d2} */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x003a A:{SYNTHETIC, Splitter:B:22:0x003a} */
    /* JADX WARNING: Removed duplicated region for block: B:119:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x003f A:{SYNTHETIC, Splitter:B:25:0x003f} */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x0079 A:{SYNTHETIC, Splitter:B:50:0x0079} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x007e A:{SYNTHETIC, Splitter:B:53:0x007e} */
    /* JADX WARNING: Removed duplicated region for block: B:123:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x0097 A:{SYNTHETIC, Splitter:B:64:0x0097} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x009c A:{SYNTHETIC, Splitter:B:67:0x009c} */
    /* JADX WARNING: Removed duplicated region for block: B:125:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x00b2 A:{SYNTHETIC, Splitter:B:78:0x00b2} */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x00b7 A:{SYNTHETIC, Splitter:B:81:0x00b7} */
    /* JADX WARNING: Removed duplicated region for block: B:127:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x00cd A:{SYNTHETIC, Splitter:B:90:0x00cd} */
    /* JADX WARNING: Removed duplicated region for block: B:93:0x00d2 A:{SYNTHETIC, Splitter:B:93:0x00d2} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static JSONObject b(Context context, String str) {
        BufferedReader bufferedReader;
        FileInputStream fileInputStream;
        JSONException e;
        Throwable th;
        Exception e2;
        FileInputStream openFileInput;
        BufferedReader bufferedReader2;
        try {
            openFileInput = context.openFileInput(d(context, str));
            try {
                bufferedReader2 = new BufferedReader(new InputStreamReader(openFileInput, "UTF-8"));
                try {
                    StringBuffer stringBuffer = new StringBuffer("");
                    while (true) {
                        String readLine = bufferedReader2.readLine();
                        if (readLine == null) {
                            break;
                        }
                        stringBuffer.append(readLine);
                    }
                    if (stringBuffer.length() == 0) {
                        try {
                            bufferedReader2.close();
                        } catch (IOException e3) {
                            e3.printStackTrace();
                        }
                        if (openFileInput == null) {
                            return null;
                        }
                        try {
                            openFileInput.close();
                            return null;
                        } catch (IOException e32) {
                            e32.printStackTrace();
                            return null;
                        }
                    }
                    JSONObject jSONObject = new JSONObject(stringBuffer.toString());
                    try {
                        bufferedReader2.close();
                    } catch (IOException e4) {
                        e4.printStackTrace();
                    }
                    if (openFileInput != null) {
                        try {
                            openFileInput.close();
                        } catch (IOException e42) {
                            e42.printStackTrace();
                        }
                    }
                    return jSONObject;
                } catch (FileNotFoundException e5) {
                    bufferedReader = bufferedReader2;
                    fileInputStream = openFileInput;
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e322) {
                            e322.printStackTrace();
                        }
                    }
                    if (fileInputStream != null) {
                        return null;
                    }
                    try {
                        fileInputStream.close();
                        return null;
                    } catch (IOException e3222) {
                        e3222.printStackTrace();
                        return null;
                    }
                } catch (IOException e6) {
                    if (bufferedReader2 != null) {
                        try {
                            bufferedReader2.close();
                        } catch (IOException e32222) {
                            e32222.printStackTrace();
                        }
                    }
                    if (openFileInput == null) {
                        return null;
                    }
                    try {
                        openFileInput.close();
                        return null;
                    } catch (IOException e322222) {
                        e322222.printStackTrace();
                        return null;
                    }
                } catch (JSONException e7) {
                    e = e7;
                    try {
                        e.printStackTrace();
                        c(context, str);
                        if (bufferedReader2 != null) {
                            try {
                                bufferedReader2.close();
                            } catch (IOException e3222222) {
                                e3222222.printStackTrace();
                            }
                        }
                        if (openFileInput == null) {
                            return null;
                        }
                        try {
                            openFileInput.close();
                            return null;
                        } catch (IOException e32222222) {
                            e32222222.printStackTrace();
                            return null;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (bufferedReader2 != null) {
                            try {
                                bufferedReader2.close();
                            } catch (IOException e322222222) {
                                e322222222.printStackTrace();
                            }
                        }
                        if (openFileInput != null) {
                            try {
                                openFileInput.close();
                            } catch (IOException e3222222222) {
                                e3222222222.printStackTrace();
                            }
                        }
                        throw th;
                    }
                } catch (Exception e8) {
                    e2 = e8;
                    e2.printStackTrace();
                    if (bufferedReader2 != null) {
                        try {
                            bufferedReader2.close();
                        } catch (IOException e32222222222) {
                            e32222222222.printStackTrace();
                        }
                    }
                    if (openFileInput == null) {
                        return null;
                    }
                    try {
                        openFileInput.close();
                        return null;
                    } catch (IOException e322222222222) {
                        e322222222222.printStackTrace();
                        return null;
                    }
                }
            } catch (FileNotFoundException e9) {
                bufferedReader = null;
                fileInputStream = openFileInput;
                if (bufferedReader != null) {
                }
                if (fileInputStream != null) {
                }
            } catch (IOException e10) {
                bufferedReader2 = null;
                if (bufferedReader2 != null) {
                }
                if (openFileInput == null) {
                }
            } catch (JSONException e11) {
                e = e11;
                bufferedReader2 = null;
                e.printStackTrace();
                c(context, str);
                if (bufferedReader2 != null) {
                }
                if (openFileInput == null) {
                }
            } catch (Exception e12) {
                e2 = e12;
                bufferedReader2 = null;
                e2.printStackTrace();
                if (bufferedReader2 != null) {
                }
                if (openFileInput == null) {
                }
            } catch (Throwable th3) {
                bufferedReader2 = null;
                th = th3;
                if (bufferedReader2 != null) {
                }
                if (openFileInput != null) {
                }
                throw th;
            }
        } catch (FileNotFoundException e13) {
            bufferedReader = null;
            fileInputStream = null;
        } catch (IOException e14) {
            bufferedReader2 = null;
            openFileInput = null;
            if (bufferedReader2 != null) {
            }
            if (openFileInput == null) {
            }
        } catch (JSONException e15) {
            e = e15;
            bufferedReader2 = null;
            openFileInput = null;
            e.printStackTrace();
            c(context, str);
            if (bufferedReader2 != null) {
            }
            if (openFileInput == null) {
            }
        } catch (Exception e16) {
            e2 = e16;
            bufferedReader2 = null;
            openFileInput = null;
            e2.printStackTrace();
            if (bufferedReader2 != null) {
            }
            if (openFileInput == null) {
            }
        } catch (Throwable th32) {
            bufferedReader2 = null;
            openFileInput = null;
            th = th32;
            if (bufferedReader2 != null) {
            }
            if (openFileInput != null) {
            }
            throw th;
        }
    }

    public static void c(Context context, String str) {
        context.deleteFile(d(context, str));
    }

    private static String d(Context context, String str) {
        return "hianalytics_" + str + "_" + context.getPackageName();
    }
}
