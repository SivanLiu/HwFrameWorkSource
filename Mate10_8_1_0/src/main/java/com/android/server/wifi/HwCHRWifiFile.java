package com.android.server.wifi;

import android.util.Log;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class HwCHRWifiFile {
    public static byte[] getDevFileResult(String fileName) {
        Throwable th;
        FileInputStream fileInputStream = null;
        byte[] buffer = new byte[4];
        try {
            FileInputStream fin = new FileInputStream(fileName);
            try {
                int length = fin.read(buffer);
                fin.close();
                if (length != 4) {
                    Log.e("HwCHRWifiFile", "getDevFileResult read length is not right");
                    buffer = null;
                }
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (Exception e) {
                        Log.e("HwCHRWifiFile", "getDevFileResult throw close exception");
                    }
                }
                fileInputStream = fin;
            } catch (FileNotFoundException e2) {
                fileInputStream = fin;
                Log.e("HwCHRWifiFile", "getDevFileResult throw FileNotFoundException");
                buffer = null;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (Exception e3) {
                        Log.e("HwCHRWifiFile", "getDevFileResult throw close exception");
                    }
                }
                return buffer;
            } catch (IOException e4) {
                fileInputStream = fin;
                try {
                    Log.e("HwCHRWifiFile", "getDevFileResult throw IOException");
                    buffer = null;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Exception e5) {
                            Log.e("HwCHRWifiFile", "getDevFileResult throw close exception");
                        }
                    }
                    return buffer;
                } catch (Throwable th2) {
                    th = th2;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Exception e6) {
                            Log.e("HwCHRWifiFile", "getDevFileResult throw close exception");
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                fileInputStream = fin;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw th;
            }
        } catch (FileNotFoundException e7) {
            Log.e("HwCHRWifiFile", "getDevFileResult throw FileNotFoundException");
            buffer = null;
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return buffer;
        } catch (IOException e8) {
            Log.e("HwCHRWifiFile", "getDevFileResult throw IOException");
            buffer = null;
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return buffer;
        }
        return buffer;
    }

    public static List<String> getFileResult(String fileName) {
        BufferedReader dr;
        FileNotFoundException e;
        IOException e2;
        Throwable th;
        List<String> result = new ArrayList();
        FileInputStream fileInputStream = null;
        BufferedReader bufferedReader = null;
        try {
            FileInputStream f = new FileInputStream(fileName);
            try {
                dr = new BufferedReader(new InputStreamReader(f, "US-ASCII"));
            } catch (FileNotFoundException e3) {
                e = e3;
                fileInputStream = f;
                Log.e("HwCHRWifiFile", "getFileResult throw exception" + e);
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (Exception e4) {
                    }
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                return result;
            } catch (IOException e5) {
                e2 = e5;
                fileInputStream = f;
                try {
                    Log.e("HwCHRWifiFile", "getFileResult throw exception" + e2);
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (Exception e6) {
                        }
                    }
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    return result;
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (Exception e7) {
                            throw th;
                        }
                    }
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                fileInputStream = f;
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw th;
            }
            try {
                for (String line = dr.readLine(); line != null; line = dr.readLine()) {
                    line = line.trim();
                    if (!line.equals("")) {
                        result.add(line);
                    }
                }
                dr.close();
                f.close();
                if (dr != null) {
                    try {
                        dr.close();
                    } catch (Exception e8) {
                    }
                }
                if (f != null) {
                    f.close();
                }
                fileInputStream = f;
            } catch (FileNotFoundException e9) {
                e = e9;
                bufferedReader = dr;
                fileInputStream = f;
                Log.e("HwCHRWifiFile", "getFileResult throw exception" + e);
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                return result;
            } catch (IOException e10) {
                e2 = e10;
                bufferedReader = dr;
                fileInputStream = f;
                Log.e("HwCHRWifiFile", "getFileResult throw exception" + e2);
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                return result;
            } catch (Throwable th4) {
                th = th4;
                bufferedReader = dr;
                fileInputStream = f;
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw th;
            }
        } catch (FileNotFoundException e11) {
            e = e11;
            Log.e("HwCHRWifiFile", "getFileResult throw exception" + e);
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return result;
        } catch (IOException e12) {
            e2 = e12;
            Log.e("HwCHRWifiFile", "getFileResult throw exception" + e2);
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return result;
        }
        return result;
    }
}
