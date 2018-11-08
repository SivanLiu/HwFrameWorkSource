package com.hianalytics.android.a.a;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import org.json.JSONException;
import org.json.JSONObject;

public final class c {
    public static SharedPreferences a(Context context, String str) {
        return context.getSharedPreferences("hianalytics_" + str + "_" + context.getPackageName(), 0);
    }

    public static void a(Context context, JSONObject jSONObject, String str) {
        Object -l_3_R = null;
        try {
            -l_3_R = context.openFileOutput(d(context, str), 0);
            -l_3_R.write(jSONObject.toString().getBytes("UTF-8"));
            -l_3_R.flush();
            if (-l_3_R != null) {
                try {
                    -l_3_R.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e2) {
            if (-l_3_R != null) {
                try {
                    -l_3_R.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
        } catch (IOException e4) {
            if (-l_3_R != null) {
                try {
                    -l_3_R.close();
                } catch (IOException e32) {
                    e32.printStackTrace();
                }
            }
        } catch (Throwable th) {
            if (-l_3_R != null) {
                try {
                    -l_3_R.close();
                } catch (IOException e5) {
                    e5.printStackTrace();
                }
            }
        }
    }

    public static JSONObject b(Context context, String str) {
        JSONException e;
        Throwable th;
        Exception e2;
        FileInputStream fileInputStream = null;
        BufferedReader bufferedReader = null;
        try {
            fileInputStream = context.openFileInput(d(context, str));
            BufferedReader -l_3_R = new BufferedReader(new InputStreamReader(fileInputStream, "UTF-8"));
            try {
                Object -l_4_R;
                Object -l_5_R = new StringBuffer("");
                while (true) {
                    -l_4_R = -l_3_R.readLine();
                    if (-l_4_R == null) {
                        break;
                    }
                    -l_5_R.append(-l_4_R);
                }
                if (-l_5_R.length() != 0) {
                    -l_4_R = new JSONObject(-l_5_R.toString());
                    try {
                        -l_3_R.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e32) {
                            e32.printStackTrace();
                        }
                    }
                    return -l_4_R;
                }
                try {
                    -l_3_R.close();
                } catch (IOException e322) {
                    e322.printStackTrace();
                }
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e3222) {
                        e3222.printStackTrace();
                    }
                }
                return null;
            } catch (FileNotFoundException e4) {
                bufferedReader = -l_3_R;
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e32222) {
                        e32222.printStackTrace();
                    }
                }
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e322222) {
                        e322222.printStackTrace();
                    }
                }
                return null;
            } catch (IOException e5) {
                bufferedReader = -l_3_R;
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e3222222) {
                        e3222222.printStackTrace();
                    }
                }
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e32222222) {
                        e32222222.printStackTrace();
                    }
                }
                return null;
            } catch (JSONException e6) {
                e = e6;
                bufferedReader = -l_3_R;
                try {
                    e.printStackTrace();
                    c(context, str);
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e322222222) {
                            e322222222.printStackTrace();
                        }
                    }
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e3222222222) {
                            e3222222222.printStackTrace();
                        }
                    }
                    return null;
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e7) {
                            e7.printStackTrace();
                        }
                    }
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e72) {
                            e72.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Exception e8) {
                e2 = e8;
                bufferedReader = -l_3_R;
                e2.printStackTrace();
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e32222222222) {
                        e32222222222.printStackTrace();
                    }
                }
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e322222222222) {
                        e322222222222.printStackTrace();
                    }
                }
                return null;
            } catch (Throwable th3) {
                th = th3;
                bufferedReader = -l_3_R;
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw th;
            }
        } catch (FileNotFoundException e9) {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return null;
        } catch (IOException e10) {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return null;
        } catch (JSONException e11) {
            e = e11;
            e.printStackTrace();
            c(context, str);
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return null;
        } catch (Exception e12) {
            e2 = e12;
            e2.printStackTrace();
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return null;
        }
    }

    public static void c(Context context, String str) {
        context.deleteFile(d(context, str));
    }

    private static String d(Context context, String str) {
        return "hianalytics_" + str + "_" + context.getPackageName();
    }
}
