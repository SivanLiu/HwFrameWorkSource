package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.ClassLoaderCreator;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

abstract class BaseParceledListSlice<T> implements Parcelable {
    private static boolean DEBUG = false;
    private static final int MAX_IPC_SIZE = 65536;
    private static String TAG = "ParceledListSlice";
    private int mInlineCountLimit = Integer.MAX_VALUE;
    private final List<T> mList;

    protected abstract Creator<?> readParcelableCreator(Parcel parcel, ClassLoader classLoader);

    protected abstract void writeElement(T t, Parcel parcel, int i);

    protected abstract void writeParcelableCreator(T t, Parcel parcel);

    public BaseParceledListSlice(List<T> list) {
        this.mList = list;
    }

    BaseParceledListSlice(Parcel p, ClassLoader loader) {
        String str;
        ClassLoader classLoader = loader;
        int N = p.readInt();
        this.mList = new ArrayList(N);
        if (DEBUG) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Retrieving ");
            stringBuilder.append(N);
            stringBuilder.append(" items");
            Log.d(str, stringBuilder.toString());
        }
        if (N > 0) {
            StringBuilder stringBuilder2;
            Creator<?> creator = readParcelableCreator(p, loader);
            int i = 0;
            Class<?> listElementClass = null;
            int i2 = 0;
            while (i2 < N && p.readInt() != 0) {
                T parcelable = readCreator(creator, p, classLoader);
                if (listElementClass == null) {
                    listElementClass = parcelable.getClass();
                } else {
                    verifySameType(listElementClass, parcelable.getClass());
                }
                this.mList.add(parcelable);
                if (DEBUG) {
                    String str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Read inline #");
                    stringBuilder2.append(i2);
                    stringBuilder2.append(": ");
                    stringBuilder2.append(this.mList.get(this.mList.size() - 1));
                    Log.d(str2, stringBuilder2.toString());
                }
                i2++;
            }
            Parcel parcel = p;
            if (i2 < N) {
                IBinder retriever = p.readStrongBinder();
                int i3 = i2;
                while (i3 < N) {
                    if (DEBUG) {
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Reading more @");
                        stringBuilder2.append(i3);
                        stringBuilder2.append(" of ");
                        stringBuilder2.append(N);
                        stringBuilder2.append(": retriever=");
                        stringBuilder2.append(retriever);
                        Log.d(str, stringBuilder2.toString());
                    }
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInt(i3);
                    try {
                        retriever.transact(1, data, reply, i);
                        while (i3 < N && reply.readInt() != 0) {
                            T parcelable2 = readCreator(creator, reply, classLoader);
                            verifySameType(listElementClass, parcelable2.getClass());
                            this.mList.add(parcelable2);
                            if (DEBUG) {
                                String str3 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Read extra #");
                                stringBuilder3.append(i3);
                                stringBuilder3.append(": ");
                                stringBuilder3.append(this.mList.get(this.mList.size() - 1));
                                Log.d(str3, stringBuilder3.toString());
                            }
                            i3++;
                        }
                        reply.recycle();
                        data.recycle();
                        i = 0;
                    } catch (RemoteException e) {
                        RemoteException remoteException = e;
                        String str4 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Failure retrieving array; only received ");
                        stringBuilder4.append(i3);
                        stringBuilder4.append(" of ");
                        stringBuilder4.append(N);
                        Log.w(str4, stringBuilder4.toString(), e);
                        return;
                    }
                }
            }
        }
    }

    private T readCreator(Creator<?> creator, Parcel p, ClassLoader loader) {
        if (creator instanceof ClassLoaderCreator) {
            return ((ClassLoaderCreator) creator).createFromParcel(p, loader);
        }
        return creator.createFromParcel(p);
    }

    private static void verifySameType(Class<?> expected, Class<?> actual) {
        if (!actual.equals(expected)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't unparcel type ");
            stringBuilder.append(actual.getName());
            stringBuilder.append(" in list of type ");
            stringBuilder.append(expected.getName());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public List<T> getList() {
        return this.mList;
    }

    public void setInlineCountLimit(int maxCount) {
        this.mInlineCountLimit = maxCount;
    }

    public void writeToParcel(Parcel dest, int flags) {
        final int N = this.mList.size();
        final int callFlags = flags;
        dest.writeInt(N);
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Writing ");
            stringBuilder.append(N);
            stringBuilder.append(" items");
            Log.d(str, stringBuilder.toString());
        }
        if (N > 0) {
            final Class<?> listElementClass = this.mList.get(0).getClass();
            writeParcelableCreator(this.mList.get(0), dest);
            int i = 0;
            while (i < N && i < this.mInlineCountLimit && dest.dataSize() < 65536) {
                dest.writeInt(1);
                T parcelable = this.mList.get(i);
                verifySameType(listElementClass, parcelable.getClass());
                writeElement(parcelable, dest, callFlags);
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Wrote inline #");
                    stringBuilder2.append(i);
                    stringBuilder2.append(": ");
                    stringBuilder2.append(this.mList.get(i));
                    Log.d(str2, stringBuilder2.toString());
                }
                i++;
            }
            if (i < N) {
                dest.writeInt(0);
                Binder retriever = new Binder() {
                    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                        if (code != 1) {
                            return super.onTransact(code, data, reply, flags);
                        }
                        String access$100;
                        StringBuilder stringBuilder;
                        int i = data.readInt();
                        if (BaseParceledListSlice.DEBUG) {
                            access$100 = BaseParceledListSlice.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Writing more @");
                            stringBuilder.append(i);
                            stringBuilder.append(" of ");
                            stringBuilder.append(N);
                            Log.d(access$100, stringBuilder.toString());
                        }
                        while (i < N && reply.dataSize() < 65536) {
                            reply.writeInt(1);
                            T parcelable = BaseParceledListSlice.this.mList.get(i);
                            BaseParceledListSlice.verifySameType(listElementClass, parcelable.getClass());
                            BaseParceledListSlice.this.writeElement(parcelable, reply, callFlags);
                            if (BaseParceledListSlice.DEBUG) {
                                String access$1002 = BaseParceledListSlice.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Wrote extra #");
                                stringBuilder2.append(i);
                                stringBuilder2.append(": ");
                                stringBuilder2.append(BaseParceledListSlice.this.mList.get(i));
                                Log.d(access$1002, stringBuilder2.toString());
                            }
                            i++;
                        }
                        if (i < N) {
                            if (BaseParceledListSlice.DEBUG) {
                                access$100 = BaseParceledListSlice.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Breaking @");
                                stringBuilder.append(i);
                                stringBuilder.append(" of ");
                                stringBuilder.append(N);
                                Log.d(access$100, stringBuilder.toString());
                            }
                            reply.writeInt(0);
                        }
                        return true;
                    }
                };
                if (DEBUG) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Breaking @");
                    stringBuilder3.append(i);
                    stringBuilder3.append(" of ");
                    stringBuilder3.append(N);
                    stringBuilder3.append(": retriever=");
                    stringBuilder3.append(retriever);
                    Log.d(str3, stringBuilder3.toString());
                }
                dest.writeStrongBinder(retriever);
            }
        }
    }
}
