package com.huawei.servicehost.normal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.Surface;
import com.huawei.servicehost.ImageDescriptor;

public interface IIPRequest4CreatePipeline extends IInterface {

    public static abstract class Stub extends Binder implements IIPRequest4CreatePipeline {
        private static final String DESCRIPTOR = "com.huawei.servicehost.normal.IIPRequest4CreatePipeline";
        static final int TRANSACTION_getCamera1CapImageConsumer = 21;
        static final int TRANSACTION_getCamera1CapSurface = 9;
        static final int TRANSACTION_getCamera1ImageConsumer = 17;
        static final int TRANSACTION_getCamera1Surface = 3;
        static final int TRANSACTION_getCamera1VideoImageConsumer = 31;
        static final int TRANSACTION_getCamera1VideoSurface = 27;
        static final int TRANSACTION_getCamera2CapImageConsumer = 22;
        static final int TRANSACTION_getCamera2CapSurface = 10;
        static final int TRANSACTION_getCamera2ImageConsumer = 18;
        static final int TRANSACTION_getCamera2Surface = 4;
        static final int TRANSACTION_getCamera2VideoImageConsumer = 32;
        static final int TRANSACTION_getCamera2VideoSurface = 28;
        static final int TRANSACTION_getDmapImageConsumer = 20;
        static final int TRANSACTION_getDmapSurface = 8;
        static final int TRANSACTION_getLayout = 1;
        static final int TRANSACTION_getMetadataImageConsumer = 19;
        static final int TRANSACTION_getMetadataSurface = 7;
        static final int TRANSACTION_setCamera1CapImageConsumer = 15;
        static final int TRANSACTION_setCamera1ImageConsumer = 11;
        static final int TRANSACTION_setCamera1VideoImageConsumer = 29;
        static final int TRANSACTION_setCamera1VideoSurface = 25;
        static final int TRANSACTION_setCamera2CapImageConsumer = 16;
        static final int TRANSACTION_setCamera2ImageConsumer = 12;
        static final int TRANSACTION_setCamera2VideoImageConsumer = 30;
        static final int TRANSACTION_setCamera2VideoSurface = 26;
        static final int TRANSACTION_setCaptureFormat = 24;
        static final int TRANSACTION_setCaptureFormatLine2 = 35;
        static final int TRANSACTION_setDmapImageConsumer = 14;
        static final int TRANSACTION_setLayout = 2;
        static final int TRANSACTION_setMetadataImageConsumer = 13;
        static final int TRANSACTION_setPreview1Surface = 5;
        static final int TRANSACTION_setPreview2Surface = 6;
        static final int TRANSACTION_setPreviewFormat = 23;
        static final int TRANSACTION_setPreviewFormatLine2 = 34;
        static final int TRANSACTION_setVideoFormat = 33;
        static final int TRANSACTION_setVideoFormatLine2 = 36;

        private static class Proxy implements IIPRequest4CreatePipeline {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public String getLayout() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setLayout(String val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(val);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Surface getCamera1Surface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Surface _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Surface) Surface.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Surface getCamera2Surface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Surface _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Surface) Surface.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPreview1Surface(Surface val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (val != null) {
                        _data.writeInt(1);
                        val.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPreview2Surface(Surface val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (val != null) {
                        _data.writeInt(1);
                        val.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Surface getMetadataSurface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Surface _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Surface) Surface.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Surface getDmapSurface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Surface _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Surface) Surface.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Surface getCamera1CapSurface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Surface _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getCamera1CapSurface, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Surface) Surface.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Surface getCamera2CapSurface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Surface _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Surface) Surface.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCamera1ImageConsumer(IBinder val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(val);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCamera2ImageConsumer(IBinder val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(val);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setMetadataImageConsumer(IBinder val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(val);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDmapImageConsumer(IBinder val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(val);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCamera1CapImageConsumer(IBinder val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(val);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCamera2CapImageConsumer(IBinder val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(val);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder getCamera1ImageConsumer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder getCamera2ImageConsumer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder getMetadataImageConsumer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder getDmapImageConsumer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder getCamera1CapImageConsumer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder getCamera2CapImageConsumer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPreviewFormat(ImageDescriptor val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (val != null) {
                        _data.writeInt(1);
                        val.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_setPreviewFormat, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCaptureFormat(ImageDescriptor val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (val != null) {
                        _data.writeInt(1);
                        val.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_setCaptureFormat, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCamera1VideoSurface(Surface val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (val != null) {
                        _data.writeInt(1);
                        val.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_setCamera1VideoSurface, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCamera2VideoSurface(Surface val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (val != null) {
                        _data.writeInt(1);
                        val.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_setCamera2VideoSurface, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Surface getCamera1VideoSurface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Surface _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getCamera1VideoSurface, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Surface) Surface.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Surface getCamera2VideoSurface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Surface _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getCamera2VideoSurface, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Surface) Surface.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCamera1VideoImageConsumer(IBinder val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(val);
                    this.mRemote.transact(Stub.TRANSACTION_setCamera1VideoImageConsumer, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCamera2VideoImageConsumer(IBinder val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(val);
                    this.mRemote.transact(Stub.TRANSACTION_setCamera2VideoImageConsumer, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder getCamera1VideoImageConsumer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getCamera1VideoImageConsumer, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder getCamera2VideoImageConsumer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setVideoFormat(ImageDescriptor val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (val != null) {
                        _data.writeInt(1);
                        val.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPreviewFormatLine2(ImageDescriptor val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (val != null) {
                        _data.writeInt(1);
                        val.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCaptureFormatLine2(ImageDescriptor val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (val != null) {
                        _data.writeInt(1);
                        val.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_setCaptureFormatLine2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setVideoFormatLine2(ImageDescriptor val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (val != null) {
                        _data.writeInt(1);
                        val.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_setVideoFormatLine2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IIPRequest4CreatePipeline asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IIPRequest4CreatePipeline)) {
                return new Proxy(obj);
            }
            return (IIPRequest4CreatePipeline) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                Surface _arg0 = null;
                IBinder _result;
                ImageDescriptor _arg02;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        String _result2 = getLayout();
                        reply.writeNoException();
                        reply.writeString(_result2);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        setLayout(data.readString());
                        reply.writeNoException();
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        _arg0 = getCamera1Surface();
                        reply.writeNoException();
                        if (_arg0 != null) {
                            reply.writeInt(1);
                            _arg0.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        _arg0 = getCamera2Surface();
                        reply.writeNoException();
                        if (_arg0 != null) {
                            reply.writeInt(1);
                            _arg0.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (Surface) Surface.CREATOR.createFromParcel(data);
                        }
                        setPreview1Surface(_arg0);
                        reply.writeNoException();
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (Surface) Surface.CREATOR.createFromParcel(data);
                        }
                        setPreview2Surface(_arg0);
                        reply.writeNoException();
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        _arg0 = getMetadataSurface();
                        reply.writeNoException();
                        if (_arg0 != null) {
                            reply.writeInt(1);
                            _arg0.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        _arg0 = getDmapSurface();
                        reply.writeNoException();
                        if (_arg0 != null) {
                            reply.writeInt(1);
                            _arg0.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case TRANSACTION_getCamera1CapSurface /*9*/:
                        data.enforceInterface(descriptor);
                        _arg0 = getCamera1CapSurface();
                        reply.writeNoException();
                        if (_arg0 != null) {
                            reply.writeInt(1);
                            _arg0.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        _arg0 = getCamera2CapSurface();
                        reply.writeNoException();
                        if (_arg0 != null) {
                            reply.writeInt(1);
                            _arg0.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 11:
                        data.enforceInterface(descriptor);
                        setCamera1ImageConsumer(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        setCamera2ImageConsumer(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 13:
                        data.enforceInterface(descriptor);
                        setMetadataImageConsumer(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 14:
                        data.enforceInterface(descriptor);
                        setDmapImageConsumer(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 15:
                        data.enforceInterface(descriptor);
                        setCamera1CapImageConsumer(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 16:
                        data.enforceInterface(descriptor);
                        setCamera2CapImageConsumer(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 17:
                        data.enforceInterface(descriptor);
                        _result = getCamera1ImageConsumer();
                        reply.writeNoException();
                        reply.writeStrongBinder(_result);
                        return true;
                    case 18:
                        data.enforceInterface(descriptor);
                        _result = getCamera2ImageConsumer();
                        reply.writeNoException();
                        reply.writeStrongBinder(_result);
                        return true;
                    case 19:
                        data.enforceInterface(descriptor);
                        _result = getMetadataImageConsumer();
                        reply.writeNoException();
                        reply.writeStrongBinder(_result);
                        return true;
                    case 20:
                        data.enforceInterface(descriptor);
                        _result = getDmapImageConsumer();
                        reply.writeNoException();
                        reply.writeStrongBinder(_result);
                        return true;
                    case 21:
                        data.enforceInterface(descriptor);
                        _result = getCamera1CapImageConsumer();
                        reply.writeNoException();
                        reply.writeStrongBinder(_result);
                        return true;
                    case 22:
                        data.enforceInterface(descriptor);
                        _result = getCamera2CapImageConsumer();
                        reply.writeNoException();
                        reply.writeStrongBinder(_result);
                        return true;
                    case TRANSACTION_setPreviewFormat /*23*/:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ImageDescriptor) ImageDescriptor.CREATOR.createFromParcel(data);
                        }
                        setPreviewFormat(_arg02);
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_setCaptureFormat /*24*/:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ImageDescriptor) ImageDescriptor.CREATOR.createFromParcel(data);
                        }
                        setCaptureFormat(_arg02);
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_setCamera1VideoSurface /*25*/:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (Surface) Surface.CREATOR.createFromParcel(data);
                        }
                        setCamera1VideoSurface(_arg0);
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_setCamera2VideoSurface /*26*/:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (Surface) Surface.CREATOR.createFromParcel(data);
                        }
                        setCamera2VideoSurface(_arg0);
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_getCamera1VideoSurface /*27*/:
                        data.enforceInterface(descriptor);
                        _arg0 = getCamera1VideoSurface();
                        reply.writeNoException();
                        if (_arg0 != null) {
                            reply.writeInt(1);
                            _arg0.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case TRANSACTION_getCamera2VideoSurface /*28*/:
                        data.enforceInterface(descriptor);
                        _arg0 = getCamera2VideoSurface();
                        reply.writeNoException();
                        if (_arg0 != null) {
                            reply.writeInt(1);
                            _arg0.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case TRANSACTION_setCamera1VideoImageConsumer /*29*/:
                        data.enforceInterface(descriptor);
                        setCamera1VideoImageConsumer(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_setCamera2VideoImageConsumer /*30*/:
                        data.enforceInterface(descriptor);
                        setCamera2VideoImageConsumer(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_getCamera1VideoImageConsumer /*31*/:
                        data.enforceInterface(descriptor);
                        _result = getCamera1VideoImageConsumer();
                        reply.writeNoException();
                        reply.writeStrongBinder(_result);
                        return true;
                    case 32:
                        data.enforceInterface(descriptor);
                        _result = getCamera2VideoImageConsumer();
                        reply.writeNoException();
                        reply.writeStrongBinder(_result);
                        return true;
                    case 33:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ImageDescriptor) ImageDescriptor.CREATOR.createFromParcel(data);
                        }
                        setVideoFormat(_arg02);
                        reply.writeNoException();
                        return true;
                    case 34:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ImageDescriptor) ImageDescriptor.CREATOR.createFromParcel(data);
                        }
                        setPreviewFormatLine2(_arg02);
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_setCaptureFormatLine2 /*35*/:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ImageDescriptor) ImageDescriptor.CREATOR.createFromParcel(data);
                        }
                        setCaptureFormatLine2(_arg02);
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_setVideoFormatLine2 /*36*/:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ImageDescriptor) ImageDescriptor.CREATOR.createFromParcel(data);
                        }
                        setVideoFormatLine2(_arg02);
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    IBinder getCamera1CapImageConsumer() throws RemoteException;

    Surface getCamera1CapSurface() throws RemoteException;

    IBinder getCamera1ImageConsumer() throws RemoteException;

    Surface getCamera1Surface() throws RemoteException;

    IBinder getCamera1VideoImageConsumer() throws RemoteException;

    Surface getCamera1VideoSurface() throws RemoteException;

    IBinder getCamera2CapImageConsumer() throws RemoteException;

    Surface getCamera2CapSurface() throws RemoteException;

    IBinder getCamera2ImageConsumer() throws RemoteException;

    Surface getCamera2Surface() throws RemoteException;

    IBinder getCamera2VideoImageConsumer() throws RemoteException;

    Surface getCamera2VideoSurface() throws RemoteException;

    IBinder getDmapImageConsumer() throws RemoteException;

    Surface getDmapSurface() throws RemoteException;

    String getLayout() throws RemoteException;

    IBinder getMetadataImageConsumer() throws RemoteException;

    Surface getMetadataSurface() throws RemoteException;

    void setCamera1CapImageConsumer(IBinder iBinder) throws RemoteException;

    void setCamera1ImageConsumer(IBinder iBinder) throws RemoteException;

    void setCamera1VideoImageConsumer(IBinder iBinder) throws RemoteException;

    void setCamera1VideoSurface(Surface surface) throws RemoteException;

    void setCamera2CapImageConsumer(IBinder iBinder) throws RemoteException;

    void setCamera2ImageConsumer(IBinder iBinder) throws RemoteException;

    void setCamera2VideoImageConsumer(IBinder iBinder) throws RemoteException;

    void setCamera2VideoSurface(Surface surface) throws RemoteException;

    void setCaptureFormat(ImageDescriptor imageDescriptor) throws RemoteException;

    void setCaptureFormatLine2(ImageDescriptor imageDescriptor) throws RemoteException;

    void setDmapImageConsumer(IBinder iBinder) throws RemoteException;

    void setLayout(String str) throws RemoteException;

    void setMetadataImageConsumer(IBinder iBinder) throws RemoteException;

    void setPreview1Surface(Surface surface) throws RemoteException;

    void setPreview2Surface(Surface surface) throws RemoteException;

    void setPreviewFormat(ImageDescriptor imageDescriptor) throws RemoteException;

    void setPreviewFormatLine2(ImageDescriptor imageDescriptor) throws RemoteException;

    void setVideoFormat(ImageDescriptor imageDescriptor) throws RemoteException;

    void setVideoFormatLine2(ImageDescriptor imageDescriptor) throws RemoteException;
}
