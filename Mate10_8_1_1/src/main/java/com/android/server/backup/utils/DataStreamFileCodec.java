package com.android.server.backup.utils;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class DataStreamFileCodec<T> {
    private final DataStreamCodec<T> mCodec;
    private final File mFile;

    public DataStreamFileCodec(File file, DataStreamCodec<T> codec) {
        this.mFile = file;
        this.mCodec = codec;
    }

    public T deserialize() throws IOException {
        Throwable th;
        Throwable th2;
        Throwable th3 = null;
        FileInputStream fileInputStream = null;
        DataInputStream dataInputStream = null;
        try {
            FileInputStream fileInputStream2 = new FileInputStream(this.mFile);
            try {
                DataInputStream dataInputStream2 = new DataInputStream(fileInputStream2);
                try {
                    T deserialize = this.mCodec.deserialize(dataInputStream2);
                    if (dataInputStream2 != null) {
                        try {
                            dataInputStream2.close();
                        } catch (Throwable th4) {
                            th3 = th4;
                        }
                    }
                    if (fileInputStream2 != null) {
                        try {
                            fileInputStream2.close();
                        } catch (Throwable th5) {
                            th = th5;
                            if (th3 != null) {
                                if (th3 != th) {
                                    th3.addSuppressed(th);
                                    th = th3;
                                }
                            }
                        }
                    }
                    th = th3;
                    if (th == null) {
                        return deserialize;
                    }
                    throw th;
                } catch (Throwable th6) {
                    th = th6;
                    dataInputStream = dataInputStream2;
                    fileInputStream = fileInputStream2;
                    if (dataInputStream != null) {
                        try {
                            dataInputStream.close();
                        } catch (Throwable th7) {
                            th2 = th7;
                            if (th3 != null) {
                                if (th3 != th2) {
                                    th3.addSuppressed(th2);
                                    th2 = th3;
                                }
                            }
                        }
                    }
                    th2 = th3;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Throwable th8) {
                            th3 = th8;
                            if (th2 != null) {
                                if (th2 != th3) {
                                    th2.addSuppressed(th3);
                                    th3 = th2;
                                }
                            }
                        }
                    }
                    th3 = th2;
                    if (th3 != null) {
                        throw th;
                    }
                    throw th3;
                }
            } catch (Throwable th9) {
                th = th9;
                fileInputStream = fileInputStream2;
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
                th2 = th3;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                th3 = th2;
                if (th3 != null) {
                    throw th3;
                }
                throw th;
            }
        } catch (Throwable th10) {
            th = th10;
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            th2 = th3;
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            th3 = th2;
            if (th3 != null) {
                throw th3;
            }
            throw th;
        }
    }

    public void serialize(T t) throws IOException {
        BufferedOutputStream bufferedOutputStream;
        Throwable th;
        Throwable th2 = null;
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream2 = null;
        DataOutputStream dataOutputStream = null;
        Throwable th3;
        try {
            FileOutputStream fileOutputStream2 = new FileOutputStream(this.mFile);
            try {
                bufferedOutputStream = new BufferedOutputStream(fileOutputStream2);
            } catch (Throwable th4) {
                th3 = th4;
                fileOutputStream = fileOutputStream2;
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (Throwable th5) {
                        if (th2 == null) {
                            th2 = th5;
                        } else if (th2 != th5) {
                            th2.addSuppressed(th5);
                        }
                    }
                }
                if (bufferedOutputStream2 != null) {
                    try {
                        bufferedOutputStream2.close();
                    } catch (Throwable th6) {
                        th5 = th6;
                        if (th2 != null) {
                            if (th2 != th5) {
                                th2.addSuppressed(th5);
                                th5 = th2;
                            }
                        }
                    }
                }
                th5 = th2;
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (Throwable th7) {
                        th2 = th7;
                        if (th5 != null) {
                            if (th5 != th2) {
                                th5.addSuppressed(th2);
                                th2 = th5;
                            }
                        }
                    }
                }
                th2 = th5;
                if (th2 == null) {
                    throw th3;
                }
                throw th2;
            }
            try {
                DataOutputStream dataOutputStream2 = new DataOutputStream(bufferedOutputStream);
                try {
                    this.mCodec.serialize(t, dataOutputStream2);
                    dataOutputStream2.flush();
                    if (dataOutputStream2 != null) {
                        try {
                            dataOutputStream2.close();
                        } catch (Throwable th8) {
                            th3 = th8;
                        }
                    }
                    th3 = null;
                    if (bufferedOutputStream != null) {
                        try {
                            bufferedOutputStream.close();
                        } catch (Throwable th9) {
                            th2 = th9;
                            if (th3 != null) {
                                if (th3 != th2) {
                                    th3.addSuppressed(th2);
                                    th2 = th3;
                                }
                            }
                        }
                    }
                    th2 = th3;
                    if (fileOutputStream2 != null) {
                        try {
                            fileOutputStream2.close();
                        } catch (Throwable th10) {
                            th3 = th10;
                            if (th2 != null) {
                                if (th2 != th3) {
                                    th2.addSuppressed(th3);
                                    th3 = th2;
                                }
                            }
                        }
                    }
                    th3 = th2;
                    if (th3 != null) {
                        throw th3;
                    }
                } catch (Throwable th11) {
                    th3 = th11;
                    dataOutputStream = dataOutputStream2;
                    bufferedOutputStream2 = bufferedOutputStream;
                    fileOutputStream = fileOutputStream2;
                    if (dataOutputStream != null) {
                        dataOutputStream.close();
                    }
                    if (bufferedOutputStream2 != null) {
                        bufferedOutputStream2.close();
                    }
                    th5 = th2;
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    th2 = th5;
                    if (th2 == null) {
                        throw th3;
                    }
                    throw th2;
                }
            } catch (Throwable th12) {
                th3 = th12;
                bufferedOutputStream2 = bufferedOutputStream;
                fileOutputStream = fileOutputStream2;
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (bufferedOutputStream2 != null) {
                    bufferedOutputStream2.close();
                }
                th5 = th2;
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                th2 = th5;
                if (th2 == null) {
                    throw th2;
                }
                throw th3;
            }
        } catch (Throwable th13) {
            th3 = th13;
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
            if (bufferedOutputStream2 != null) {
                bufferedOutputStream2.close();
            }
            th5 = th2;
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            th2 = th5;
            if (th2 == null) {
                throw th2;
            }
            throw th3;
        }
    }
}
