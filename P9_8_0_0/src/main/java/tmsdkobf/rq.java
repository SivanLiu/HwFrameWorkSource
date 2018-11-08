package tmsdkobf;

import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

public class rq {
    public static int a(String str, Pattern pattern) {
        int -l_2_I = 0;
        Object -l_3_R = pattern.matcher(str);
        if (-l_3_R.find() && -l_3_R.groupCount() >= 1) {
            try {
                -l_2_I = Integer.parseInt(-l_3_R.group(1));
            } catch (Exception e) {
            }
        }
        return -l_2_I;
    }

    public static String b(String str, Pattern pattern) {
        Object -l_2_R = pattern.matcher(str);
        return (-l_2_R.find() && -l_2_R.groupCount() >= 1) ? -l_2_R.group(1) : null;
    }

    public static List<String> dp(String str) {
        Object -l_1_R = new ArrayList();
        if (TextUtils.isEmpty(str)) {
            return -l_1_R;
        }
        for (String -l_4_R : rh.jZ()) {
            Object -l_5_R = -l_4_R + "/" + str;
            Object -l_6_R = new File(-l_5_R).list();
            if (-l_6_R != null) {
                Object -l_7_R = -l_6_R;
                for (Object -l_10_R : -l_6_R) {
                    Object -l_11_R = new File(-l_5_R + "/" + -l_10_R);
                    if (-l_11_R.isDirectory()) {
                        -l_1_R.add(-l_11_R.getAbsolutePath());
                    }
                }
            }
        }
        return -l_1_R;
    }

    public static long dq(String str) {
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        long -l_1_J = 0;
        Object -l_3_R = new Stack();
        Object -l_4_R = new Stack();
        -l_3_R.push(str);
        while (!-l_3_R.isEmpty()) {
            Object -l_5_R = new File((String) -l_3_R.pop());
            if (-l_5_R.isDirectory()) {
                -l_4_R.push(-l_5_R.getAbsolutePath());
                Object -l_6_R = -l_5_R.list();
                if (-l_6_R != null) {
                    Object -l_7_R = -l_6_R;
                    for (Object -l_10_R : -l_6_R) {
                        Object -l_11_R = new File(-l_5_R, -l_10_R);
                        if (-l_11_R.isDirectory()) {
                            -l_3_R.push(-l_11_R.getAbsolutePath());
                        } else {
                            -l_1_J += -l_11_R.length();
                        }
                    }
                }
            } else {
                -l_1_J += -l_5_R.length();
            }
        }
        while (!-l_4_R.isEmpty()) {
            -l_1_J += new File((String) -l_4_R.pop()).length();
        }
        return -l_1_J;
    }

    public static List<String> dr(String str) {
        Object -l_5_R;
        Object -l_6_R;
        Object -l_1_R = new ArrayList();
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            FileInputStream -l_2_R = new FileInputStream(str);
            try {
                try {
                    InputStreamReader -l_3_R = new InputStreamReader(-l_2_R, "utf-8");
                    try {
                        try {
                            BufferedReader -l_4_R = new BufferedReader(-l_3_R);
                            while (true) {
                                try {
                                    -l_5_R = -l_4_R.readLine();
                                    if (-l_5_R == null) {
                                        break;
                                    }
                                    -l_1_R.add(-l_5_R);
                                } catch (Exception e) {
                                    -l_5_R = e;
                                    bufferedReader = -l_4_R;
                                    inputStreamReader = -l_3_R;
                                    fileInputStream = -l_2_R;
                                } catch (Throwable th) {
                                    -l_6_R = th;
                                    bufferedReader = -l_4_R;
                                    inputStreamReader = -l_3_R;
                                    fileInputStream = -l_2_R;
                                }
                            }
                            if (-l_4_R != null) {
                                try {
                                    -l_4_R.close();
                                } catch (Object -l_5_R2) {
                                    -l_5_R2.printStackTrace();
                                }
                            }
                            if (-l_3_R != null) {
                                try {
                                    -l_3_R.close();
                                } catch (Object -l_5_R22) {
                                    -l_5_R22.printStackTrace();
                                }
                            }
                            if (-l_2_R != null) {
                                try {
                                    -l_2_R.close();
                                } catch (Object -l_5_R222) {
                                    -l_5_R222.printStackTrace();
                                }
                            }
                            bufferedReader = -l_4_R;
                            inputStreamReader = -l_3_R;
                            fileInputStream = -l_2_R;
                        } catch (Exception e2) {
                            -l_5_R222 = e2;
                            inputStreamReader = -l_3_R;
                            fileInputStream = -l_2_R;
                            try {
                                -l_5_R222.printStackTrace();
                                if (bufferedReader != null) {
                                    try {
                                        bufferedReader.close();
                                    } catch (Object -l_5_R2222) {
                                        -l_5_R2222.printStackTrace();
                                    }
                                }
                                if (inputStreamReader != null) {
                                    try {
                                        inputStreamReader.close();
                                    } catch (Object -l_5_R22222) {
                                        -l_5_R22222.printStackTrace();
                                    }
                                }
                                if (fileInputStream != null) {
                                    try {
                                        fileInputStream.close();
                                    } catch (Object -l_5_R222222) {
                                        -l_5_R222222.printStackTrace();
                                    }
                                }
                                return -l_1_R;
                            } catch (Throwable th2) {
                                -l_6_R = th2;
                                if (bufferedReader != null) {
                                    try {
                                        bufferedReader.close();
                                    } catch (Object -l_7_R) {
                                        -l_7_R.printStackTrace();
                                    }
                                }
                                if (inputStreamReader != null) {
                                    try {
                                        inputStreamReader.close();
                                    } catch (Object -l_7_R2) {
                                        -l_7_R2.printStackTrace();
                                    }
                                }
                                if (fileInputStream != null) {
                                    try {
                                        fileInputStream.close();
                                    } catch (Object -l_7_R22) {
                                        -l_7_R22.printStackTrace();
                                    }
                                }
                                throw -l_6_R;
                            }
                        } catch (Throwable th3) {
                            -l_6_R = th3;
                            inputStreamReader = -l_3_R;
                            fileInputStream = -l_2_R;
                            if (bufferedReader != null) {
                                bufferedReader.close();
                            }
                            if (inputStreamReader != null) {
                                inputStreamReader.close();
                            }
                            if (fileInputStream != null) {
                                fileInputStream.close();
                            }
                            throw -l_6_R;
                        }
                    } catch (Exception e3) {
                        -l_5_R222222 = e3;
                        inputStreamReader = -l_3_R;
                        fileInputStream = -l_2_R;
                        -l_5_R222222.printStackTrace();
                        if (bufferedReader != null) {
                            bufferedReader.close();
                        }
                        if (inputStreamReader != null) {
                            inputStreamReader.close();
                        }
                        if (fileInputStream != null) {
                            fileInputStream.close();
                        }
                        return -l_1_R;
                    } catch (Throwable th4) {
                        -l_6_R = th4;
                        inputStreamReader = -l_3_R;
                        fileInputStream = -l_2_R;
                        if (bufferedReader != null) {
                            bufferedReader.close();
                        }
                        if (inputStreamReader != null) {
                            inputStreamReader.close();
                        }
                        if (fileInputStream != null) {
                            fileInputStream.close();
                        }
                        throw -l_6_R;
                    }
                } catch (Exception e4) {
                    -l_5_R222222 = e4;
                    fileInputStream = -l_2_R;
                    -l_5_R222222.printStackTrace();
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    if (inputStreamReader != null) {
                        inputStreamReader.close();
                    }
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    return -l_1_R;
                } catch (Throwable th5) {
                    -l_6_R = th5;
                    fileInputStream = -l_2_R;
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    if (inputStreamReader != null) {
                        inputStreamReader.close();
                    }
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    throw -l_6_R;
                }
            } catch (Exception e5) {
                -l_5_R222222 = e5;
                fileInputStream = -l_2_R;
                -l_5_R222222.printStackTrace();
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                return -l_1_R;
            } catch (Throwable th6) {
                -l_6_R = th6;
                fileInputStream = -l_2_R;
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw -l_6_R;
            }
        } catch (Exception e6) {
            -l_5_R222222 = e6;
            -l_5_R222222.printStackTrace();
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (inputStreamReader != null) {
                inputStreamReader.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return -l_1_R;
        }
        return -l_1_R;
    }

    public static String ds(String str) {
        Object -l_1_R = new StringBuffer();
        Object -l_3_R = Pattern.compile("\\\\u([\\S]{4})([^\\\\]*)").matcher(str);
        while (-l_3_R.find()) {
            -l_1_R.append((char) Integer.parseInt(-l_3_R.group(1), 16));
            -l_1_R.append(-l_3_R.group(2));
        }
        return -l_1_R.toString();
    }

    public static String dt(String str) {
        Object -l_1_R = mc.bT(str);
        Object -l_2_R = new StringBuilder(-l_1_R.length * 2);
        Object -l_3_R = -l_1_R;
        for (int -l_6_I : -l_1_R) {
            Object -l_7_R = Integer.toHexString(-l_6_I & 255);
            if (-l_7_R.length() == 1) {
                -l_7_R = "0" + -l_7_R;
            }
            -l_2_R.append(-l_7_R);
        }
        return -l_2_R.toString();
    }
}
