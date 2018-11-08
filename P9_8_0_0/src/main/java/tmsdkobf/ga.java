package tmsdkobf;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class ga {
    public static long Q() {
        Object -l_4_R;
        Object -l_12_R;
        long -l_0_J = 0;
        Object -l_2_R = new File("/proc/meminfo");
        DataInputStream dataInputStream = null;
        if (-l_2_R.exists()) {
            try {
                DataInputStream -l_3_R = new DataInputStream(new FileInputStream(-l_2_R));
                try {
                    -l_4_R = -l_3_R.readLine();
                    Object -l_5_R = -l_3_R.readLine();
                    Object -l_6_R = -l_3_R.readLine();
                    Object -l_7_R = -l_3_R.readLine();
                    if (-l_4_R == null || -l_5_R == null || -l_6_R == null || -l_7_R == null) {
                        throw new IOException("/proc/meminfo is error!");
                    }
                    -l_4_R = -l_4_R.trim();
                    -l_5_R = -l_5_R.trim();
                    -l_6_R = -l_6_R.trim();
                    -l_7_R = -l_7_R.trim();
                    Object -l_8_R = -l_4_R.split("[\\s]+");
                    Object -l_9_R = -l_5_R.split("[\\s]+");
                    Object -l_10_R = -l_6_R.split("[\\s]+");
                    Object -l_11_R = -l_7_R.split("[\\s]+");
                    if (-l_9_R != null && -l_9_R.length > 1) {
                        -l_0_J = 0 + Long.parseLong(-l_9_R[1]);
                    }
                    if (-l_10_R != null && -l_10_R.length > 1) {
                        -l_0_J += Long.parseLong(-l_10_R[1]);
                    }
                    if (-l_11_R != null && -l_11_R.length > 1) {
                        -l_0_J += Long.parseLong(-l_11_R[1]);
                    }
                    if (-l_3_R == null) {
                        dataInputStream = -l_3_R;
                    } else {
                        try {
                            -l_3_R.close();
                        } catch (Object -l_4_R2) {
                            -l_4_R2.printStackTrace();
                        }
                    }
                } catch (FileNotFoundException e) {
                    -l_4_R2 = e;
                    dataInputStream = -l_3_R;
                    try {
                        -l_4_R2.printStackTrace();
                        if (dataInputStream != null) {
                            try {
                                dataInputStream.close();
                            } catch (Object -l_4_R22) {
                                -l_4_R22.printStackTrace();
                            }
                        }
                        return (-l_0_J <= 0 ? null : 1) == null ? 1 : -l_0_J;
                    } catch (Throwable th) {
                        -l_12_R = th;
                        if (dataInputStream != null) {
                            try {
                                dataInputStream.close();
                            } catch (Object -l_13_R) {
                                -l_13_R.printStackTrace();
                            }
                        }
                        throw -l_12_R;
                    }
                } catch (IOException e2) {
                    -l_4_R22 = e2;
                    dataInputStream = -l_3_R;
                    -l_4_R22.printStackTrace();
                    if (dataInputStream != null) {
                        try {
                            dataInputStream.close();
                        } catch (Object -l_4_R222) {
                            -l_4_R222.printStackTrace();
                        }
                    }
                    if (-l_0_J <= 0) {
                    }
                    if ((-l_0_J <= 0 ? null : 1) == null) {
                    }
                } catch (NumberFormatException e3) {
                    -l_4_R222 = e3;
                    dataInputStream = -l_3_R;
                    -l_4_R222.printStackTrace();
                    if (dataInputStream != null) {
                        try {
                            dataInputStream.close();
                        } catch (Object -l_4_R2222) {
                            -l_4_R2222.printStackTrace();
                        }
                    }
                    if (-l_0_J <= 0) {
                    }
                    if ((-l_0_J <= 0 ? null : 1) == null) {
                    }
                } catch (Throwable th2) {
                    -l_12_R = th2;
                    dataInputStream = -l_3_R;
                    if (dataInputStream != null) {
                        dataInputStream.close();
                    }
                    throw -l_12_R;
                }
            } catch (FileNotFoundException e4) {
                -l_4_R2222 = e4;
                -l_4_R2222.printStackTrace();
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
                if (-l_0_J <= 0) {
                }
                if ((-l_0_J <= 0 ? null : 1) == null) {
                }
            } catch (IOException e5) {
                -l_4_R2222 = e5;
                -l_4_R2222.printStackTrace();
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
                if (-l_0_J <= 0) {
                }
                if ((-l_0_J <= 0 ? null : 1) == null) {
                }
            } catch (NumberFormatException e6) {
                -l_4_R2222 = e6;
                -l_4_R2222.printStackTrace();
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
                if (-l_0_J <= 0) {
                }
                if ((-l_0_J <= 0 ? null : 1) == null) {
                }
            }
        }
        if (-l_0_J <= 0) {
        }
        if ((-l_0_J <= 0 ? null : 1) == null) {
        }
    }
}
