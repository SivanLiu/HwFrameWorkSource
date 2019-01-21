package android.view;

import android.content.ClipData;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.MergedConfiguration;
import android.view.DisplayCutout.ParcelableWrapper;
import android.view.WindowManager.LayoutParams;

public interface IWindowSession extends IInterface {

    public static abstract class Stub extends Binder implements IWindowSession {
        private static final String DESCRIPTOR = "android.view.IWindowSession";
        static final int TRANSACTION_add = 1;
        static final int TRANSACTION_addToDisplay = 2;
        static final int TRANSACTION_addToDisplayWithoutInputChannel = 4;
        static final int TRANSACTION_addWithoutInputChannel = 3;
        static final int TRANSACTION_cancelDragAndDrop = 18;
        static final int TRANSACTION_dragRecipientEntered = 19;
        static final int TRANSACTION_dragRecipientExited = 20;
        static final int TRANSACTION_finishDrawing = 12;
        static final int TRANSACTION_getDisplayFrame = 11;
        static final int TRANSACTION_getInTouchMode = 14;
        static final int TRANSACTION_getWindowId = 27;
        static final int TRANSACTION_onRectangleOnScreenRequested = 26;
        static final int TRANSACTION_outOfMemory = 8;
        static final int TRANSACTION_performDrag = 16;
        static final int TRANSACTION_performHapticFeedback = 15;
        static final int TRANSACTION_pokeDrawLock = 28;
        static final int TRANSACTION_prepareToReplaceWindows = 7;
        static final int TRANSACTION_relayout = 6;
        static final int TRANSACTION_remove = 5;
        static final int TRANSACTION_reportDropResult = 17;
        static final int TRANSACTION_sendWallpaperCommand = 24;
        static final int TRANSACTION_setInTouchMode = 13;
        static final int TRANSACTION_setInsets = 10;
        static final int TRANSACTION_setTransparentRegion = 9;
        static final int TRANSACTION_setWallpaperDisplayOffset = 23;
        static final int TRANSACTION_setWallpaperPosition = 21;
        static final int TRANSACTION_startMovingTask = 29;
        static final int TRANSACTION_updatePointerIcon = 30;
        static final int TRANSACTION_updateTapExcludeRegion = 31;
        static final int TRANSACTION_wallpaperCommandComplete = 25;
        static final int TRANSACTION_wallpaperOffsetsComplete = 22;

        private static class Proxy implements IWindowSession {
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

            public int add(IWindow window, int seq, LayoutParams attrs, int viewVisibility, Rect outContentInsets, Rect outStableInsets, InputChannel outInputChannel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    _data.writeInt(seq);
                    if (attrs != null) {
                        _data.writeInt(1);
                        attrs.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(viewVisibility);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        outContentInsets.readFromParcel(_reply);
                    }
                    if (_reply.readInt() != 0) {
                        outStableInsets.readFromParcel(_reply);
                    }
                    if (_reply.readInt() != 0) {
                        outInputChannel.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int addToDisplay(IWindow window, int seq, LayoutParams attrs, int viewVisibility, int layerStackId, Rect outFrame, Rect outContentInsets, Rect outStableInsets, Rect outOutsets, ParcelableWrapper displayCutout, InputChannel outInputChannel) throws RemoteException {
                Throwable th;
                int i;
                int i2;
                Rect rect;
                Rect rect2;
                Rect rect3;
                Rect rect4;
                ParcelableWrapper parcelableWrapper;
                InputChannel inputChannel;
                LayoutParams layoutParams = attrs;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    try {
                        _data.writeInt(seq);
                        if (layoutParams != null) {
                            _data.writeInt(1);
                            layoutParams.writeToParcel(_data, 0);
                        } else {
                            _data.writeInt(0);
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        i = viewVisibility;
                        i2 = layerStackId;
                        rect = outFrame;
                        rect2 = outContentInsets;
                        rect3 = outStableInsets;
                        rect4 = outOutsets;
                        parcelableWrapper = displayCutout;
                        inputChannel = outInputChannel;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(viewVisibility);
                        try {
                            _data.writeInt(layerStackId);
                            try {
                                this.mRemote.transact(2, _data, _reply, 0);
                                _reply.readException();
                                int _result = _reply.readInt();
                                if (_reply.readInt() != 0) {
                                    try {
                                        outFrame.readFromParcel(_reply);
                                    } catch (Throwable th3) {
                                        th = th3;
                                        rect2 = outContentInsets;
                                        rect3 = outStableInsets;
                                        rect4 = outOutsets;
                                        parcelableWrapper = displayCutout;
                                        inputChannel = outInputChannel;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                }
                                rect = outFrame;
                                if (_reply.readInt() != 0) {
                                    try {
                                        outContentInsets.readFromParcel(_reply);
                                    } catch (Throwable th4) {
                                        th = th4;
                                        rect3 = outStableInsets;
                                        rect4 = outOutsets;
                                        parcelableWrapper = displayCutout;
                                        inputChannel = outInputChannel;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                }
                                rect2 = outContentInsets;
                                if (_reply.readInt() != 0) {
                                    try {
                                        outStableInsets.readFromParcel(_reply);
                                    } catch (Throwable th5) {
                                        th = th5;
                                        rect4 = outOutsets;
                                        parcelableWrapper = displayCutout;
                                        inputChannel = outInputChannel;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                }
                                rect3 = outStableInsets;
                                if (_reply.readInt() != 0) {
                                    try {
                                        outOutsets.readFromParcel(_reply);
                                    } catch (Throwable th6) {
                                        th = th6;
                                        parcelableWrapper = displayCutout;
                                        inputChannel = outInputChannel;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                }
                                rect4 = outOutsets;
                                if (_reply.readInt() != 0) {
                                    try {
                                        displayCutout.readFromParcel(_reply);
                                    } catch (Throwable th7) {
                                        th = th7;
                                        inputChannel = outInputChannel;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                }
                                parcelableWrapper = displayCutout;
                                if (_reply.readInt() != 0) {
                                    try {
                                        outInputChannel.readFromParcel(_reply);
                                    } catch (Throwable th8) {
                                        th = th8;
                                    }
                                } else {
                                    inputChannel = outInputChannel;
                                }
                                _reply.recycle();
                                _data.recycle();
                                return _result;
                            } catch (Throwable th9) {
                                th = th9;
                                rect = outFrame;
                                rect2 = outContentInsets;
                                rect3 = outStableInsets;
                                rect4 = outOutsets;
                                parcelableWrapper = displayCutout;
                                inputChannel = outInputChannel;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            rect = outFrame;
                            rect2 = outContentInsets;
                            rect3 = outStableInsets;
                            rect4 = outOutsets;
                            parcelableWrapper = displayCutout;
                            inputChannel = outInputChannel;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        i2 = layerStackId;
                        rect = outFrame;
                        rect2 = outContentInsets;
                        rect3 = outStableInsets;
                        rect4 = outOutsets;
                        parcelableWrapper = displayCutout;
                        inputChannel = outInputChannel;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th12) {
                    th = th12;
                    int i3 = seq;
                    i = viewVisibility;
                    i2 = layerStackId;
                    rect = outFrame;
                    rect2 = outContentInsets;
                    rect3 = outStableInsets;
                    rect4 = outOutsets;
                    parcelableWrapper = displayCutout;
                    inputChannel = outInputChannel;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public int addWithoutInputChannel(IWindow window, int seq, LayoutParams attrs, int viewVisibility, Rect outContentInsets, Rect outStableInsets) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    _data.writeInt(seq);
                    if (attrs != null) {
                        _data.writeInt(1);
                        attrs.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(viewVisibility);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        outContentInsets.readFromParcel(_reply);
                    }
                    if (_reply.readInt() != 0) {
                        outStableInsets.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int addToDisplayWithoutInputChannel(IWindow window, int seq, LayoutParams attrs, int viewVisibility, int layerStackId, Rect outContentInsets, Rect outStableInsets) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    _data.writeInt(seq);
                    if (attrs != null) {
                        _data.writeInt(1);
                        attrs.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(viewVisibility);
                    _data.writeInt(layerStackId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        outContentInsets.readFromParcel(_reply);
                    }
                    if (_reply.readInt() != 0) {
                        outStableInsets.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void remove(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int relayout(IWindow window, int seq, LayoutParams attrs, int requestedWidth, int requestedHeight, int viewVisibility, int flags, long frameNumber, Rect outFrame, Rect outOverscanInsets, Rect outContentInsets, Rect outVisibleInsets, Rect outStableInsets, Rect outOutsets, Rect outBackdropFrame, ParcelableWrapper displayCutout, MergedConfiguration outMergedConfiguration, Surface outSurface) throws RemoteException {
                Throwable th;
                int i;
                int i2;
                int i3;
                int i4;
                long j;
                Rect rect;
                Rect rect2;
                Rect rect3;
                Surface surface;
                LayoutParams layoutParams = attrs;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    try {
                        _data.writeInt(seq);
                        if (layoutParams != null) {
                            _data.writeInt(1);
                            layoutParams.writeToParcel(_data, 0);
                        } else {
                            _data.writeInt(0);
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        i = requestedWidth;
                        i2 = requestedHeight;
                        i3 = viewVisibility;
                        i4 = flags;
                        j = frameNumber;
                        rect = outFrame;
                        rect2 = outOverscanInsets;
                        rect3 = outContentInsets;
                        surface = outSurface;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(requestedWidth);
                        try {
                            _data.writeInt(requestedHeight);
                            try {
                                _data.writeInt(viewVisibility);
                                try {
                                    _data.writeInt(flags);
                                } catch (Throwable th3) {
                                    th = th3;
                                    j = frameNumber;
                                    rect = outFrame;
                                    rect2 = outOverscanInsets;
                                    rect3 = outContentInsets;
                                    surface = outSurface;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                i4 = flags;
                                j = frameNumber;
                                rect = outFrame;
                                rect2 = outOverscanInsets;
                                rect3 = outContentInsets;
                                surface = outSurface;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            i3 = viewVisibility;
                            i4 = flags;
                            j = frameNumber;
                            rect = outFrame;
                            rect2 = outOverscanInsets;
                            rect3 = outContentInsets;
                            surface = outSurface;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeLong(frameNumber);
                            try {
                                Rect rect4;
                                this.mRemote.transact(6, _data, _reply, 0);
                                _reply.readException();
                                int _result = _reply.readInt();
                                if (_reply.readInt() != 0) {
                                    try {
                                        outFrame.readFromParcel(_reply);
                                    } catch (Throwable th6) {
                                        th = th6;
                                        rect2 = outOverscanInsets;
                                        rect3 = outContentInsets;
                                        surface = outSurface;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                }
                                rect = outFrame;
                                if (_reply.readInt() != 0) {
                                    try {
                                        outOverscanInsets.readFromParcel(_reply);
                                    } catch (Throwable th7) {
                                        th = th7;
                                        rect3 = outContentInsets;
                                        surface = outSurface;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                }
                                rect2 = outOverscanInsets;
                                if (_reply.readInt() != 0) {
                                    try {
                                        outContentInsets.readFromParcel(_reply);
                                    } catch (Throwable th8) {
                                        th = th8;
                                        surface = outSurface;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                }
                                rect3 = outContentInsets;
                                if (_reply.readInt() != 0) {
                                    outVisibleInsets.readFromParcel(_reply);
                                } else {
                                    rect4 = outVisibleInsets;
                                }
                                if (_reply.readInt() != 0) {
                                    outStableInsets.readFromParcel(_reply);
                                } else {
                                    rect4 = outStableInsets;
                                }
                                if (_reply.readInt() != 0) {
                                    outOutsets.readFromParcel(_reply);
                                } else {
                                    rect4 = outOutsets;
                                }
                                if (_reply.readInt() != 0) {
                                    outBackdropFrame.readFromParcel(_reply);
                                } else {
                                    rect4 = outBackdropFrame;
                                }
                                if (_reply.readInt() != 0) {
                                    displayCutout.readFromParcel(_reply);
                                } else {
                                    ParcelableWrapper parcelableWrapper = displayCutout;
                                }
                                if (_reply.readInt() != 0) {
                                    outMergedConfiguration.readFromParcel(_reply);
                                } else {
                                    MergedConfiguration mergedConfiguration = outMergedConfiguration;
                                }
                                if (_reply.readInt() != 0) {
                                    try {
                                        outSurface.readFromParcel(_reply);
                                    } catch (Throwable th9) {
                                        th = th9;
                                    }
                                } else {
                                    surface = outSurface;
                                }
                                _reply.recycle();
                                _data.recycle();
                                return _result;
                            } catch (Throwable th10) {
                                th = th10;
                                rect = outFrame;
                                rect2 = outOverscanInsets;
                                rect3 = outContentInsets;
                                surface = outSurface;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th11) {
                            th = th11;
                            rect = outFrame;
                            rect2 = outOverscanInsets;
                            rect3 = outContentInsets;
                            surface = outSurface;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th12) {
                        th = th12;
                        i2 = requestedHeight;
                        i3 = viewVisibility;
                        i4 = flags;
                        j = frameNumber;
                        rect = outFrame;
                        rect2 = outOverscanInsets;
                        rect3 = outContentInsets;
                        surface = outSurface;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th13) {
                    th = th13;
                    int i5 = seq;
                    i = requestedWidth;
                    i2 = requestedHeight;
                    i3 = viewVisibility;
                    i4 = flags;
                    j = frameNumber;
                    rect = outFrame;
                    rect2 = outOverscanInsets;
                    rect3 = outContentInsets;
                    surface = outSurface;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public void prepareToReplaceWindows(IBinder appToken, boolean childrenOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(appToken);
                    _data.writeInt(childrenOnly);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean outOfMemory(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setTransparentRegion(IWindow window, Region region) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    if (region != null) {
                        _data.writeInt(1);
                        region.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setInsets(IWindow window, int touchableInsets, Rect contentInsets, Rect visibleInsets, Region touchableRegion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    _data.writeInt(touchableInsets);
                    if (contentInsets != null) {
                        _data.writeInt(1);
                        contentInsets.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (visibleInsets != null) {
                        _data.writeInt(1);
                        visibleInsets.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (touchableRegion != null) {
                        _data.writeInt(1);
                        touchableRegion.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void getDisplayFrame(IWindow window, Rect outDisplayFrame) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        outDisplayFrame.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void finishDrawing(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setInTouchMode(boolean showFocus) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(showFocus);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getInTouchMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean performHapticFeedback(IWindow window, int effectId, boolean always) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    _data.writeInt(effectId);
                    _data.writeInt(always);
                    boolean z = false;
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder performDrag(IWindow window, int flags, SurfaceControl surface, int touchSource, float touchX, float touchY, float thumbCenterX, float thumbCenterY, ClipData data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    _data.writeInt(flags);
                    if (surface != null) {
                        _data.writeInt(1);
                        surface.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(touchSource);
                    _data.writeFloat(touchX);
                    _data.writeFloat(touchY);
                    _data.writeFloat(thumbCenterX);
                    _data.writeFloat(thumbCenterY);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportDropResult(IWindow window, boolean consumed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    _data.writeInt(consumed);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void cancelDragAndDrop(IBinder dragToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(dragToken);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void dragRecipientEntered(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void dragRecipientExited(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setWallpaperPosition(IBinder windowToken, float x, float y, float xstep, float ystep) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(windowToken);
                    _data.writeFloat(x);
                    _data.writeFloat(y);
                    _data.writeFloat(xstep);
                    _data.writeFloat(ystep);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void wallpaperOffsetsComplete(IBinder window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setWallpaperDisplayOffset(IBinder windowToken, int x, int y) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(windowToken);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Bundle sendWallpaperCommand(IBinder window, String action, int x, int y, int z, Bundle extras, boolean sync) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Bundle _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window);
                    _data.writeString(action);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    _data.writeInt(z);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(sync);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Bundle) Bundle.CREATOR.createFromParcel(_reply);
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

            public void wallpaperCommandComplete(IBinder window, Bundle result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onRectangleOnScreenRequested(IBinder token, Rect rectangle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (rectangle != null) {
                        _data.writeInt(1);
                        rectangle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IWindowId getWindowId(IBinder window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    IWindowId _result = android.view.IWindowId.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void pokeDrawLock(IBinder window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean startMovingTask(IWindow window, float startX, float startY) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    _data.writeFloat(startX);
                    _data.writeFloat(startY);
                    boolean z = false;
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updatePointerIcon(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateTapExcludeRegion(IWindow window, int regionId, int left, int top, int width, int height) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window != null ? window.asBinder() : null);
                    _data.writeInt(regionId);
                    _data.writeInt(left);
                    _data.writeInt(top);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    this.mRemote.transact(31, _data, _reply, 0);
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

        public static IWindowSession asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IWindowSession)) {
                return new Proxy(obj);
            }
            return (IWindowSession) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            Parcel parcel3;
            boolean z;
            if (i != 1598968902) {
                boolean z2 = false;
                LayoutParams _arg1 = null;
                int i2;
                IWindow _arg0;
                int _arg12;
                int _arg3;
                Rect _arg5;
                int _result;
                String descriptor2;
                IWindow _arg02;
                int _arg13;
                Region _arg14;
                Region _arg2;
                int _arg32;
                int _arg4;
                Rect _arg52;
                Rect _arg6;
                Rect rect;
                Rect _arg53;
                Rect _arg132;
                IBinder _arg03;
                Bundle _arg15;
                IWindow _arg04;
                boolean _result2;
                IBinder _arg05;
                switch (i) {
                    case 1:
                        parcel3 = parcel;
                        i2 = 1;
                        parcel3.enforceInterface(descriptor);
                        _arg0 = android.view.IWindow.Stub.asInterface(data.readStrongBinder());
                        _arg12 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (LayoutParams) LayoutParams.CREATOR.createFromParcel(parcel3);
                        }
                        LayoutParams _arg22 = _arg1;
                        _arg3 = data.readInt();
                        Rect descriptor3 = new Rect();
                        _arg5 = new Rect();
                        InputChannel _arg62 = new InputChannel();
                        Rect _arg54 = _arg5;
                        _result = add(_arg0, _arg12, _arg22, _arg3, descriptor3, _arg5, _arg62);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        parcel2.writeInt(i2);
                        descriptor3.writeToParcel(parcel2, i2);
                        parcel2.writeInt(i2);
                        _arg54.writeToParcel(parcel2, i2);
                        parcel2.writeInt(i2);
                        _arg62.writeToParcel(parcel2, i2);
                        return i2;
                    case 2:
                        parcel3 = parcel;
                        i2 = 1;
                        descriptor2 = descriptor;
                        parcel3.enforceInterface(descriptor2);
                        _arg02 = android.view.IWindow.Stub.asInterface(data.readStrongBinder());
                        _arg13 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg14 = (LayoutParams) LayoutParams.CREATOR.createFromParcel(parcel3);
                        }
                        _arg2 = _arg14;
                        _arg32 = data.readInt();
                        _arg4 = data.readInt();
                        _arg52 = new Rect();
                        _arg6 = new Rect();
                        rect = new Rect();
                        _arg5 = new Rect();
                        ParcelableWrapper _arg9 = new ParcelableWrapper();
                        InputChannel _arg10 = new InputChannel();
                        ParcelableWrapper _arg92 = _arg9;
                        Rect _arg8 = _arg5;
                        Rect _arg7 = rect;
                        Rect _arg63 = _arg6;
                        Rect _arg55 = _arg52;
                        _result = addToDisplay(_arg02, _arg13, _arg2, _arg32, _arg4, _arg52, _arg6, rect, _arg8, _arg92, _arg10);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        parcel2.writeInt(i2);
                        _arg55.writeToParcel(parcel2, i2);
                        parcel2.writeInt(i2);
                        _arg63.writeToParcel(parcel2, i2);
                        parcel2.writeInt(i2);
                        _arg7.writeToParcel(parcel2, i2);
                        parcel2.writeInt(i2);
                        _arg8.writeToParcel(parcel2, i2);
                        parcel2.writeInt(i2);
                        _arg92.writeToParcel(parcel2, i2);
                        parcel2.writeInt(i2);
                        _arg10.writeToParcel(parcel2, i2);
                        return i2;
                    case 3:
                        parcel3 = parcel;
                        i2 = 1;
                        descriptor2 = descriptor;
                        parcel3.enforceInterface(descriptor2);
                        IWindow _arg06 = android.view.IWindow.Stub.asInterface(data.readStrongBinder());
                        int _arg16 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg14 = (LayoutParams) LayoutParams.CREATOR.createFromParcel(parcel3);
                        }
                        _arg2 = _arg14;
                        _arg12 = data.readInt();
                        _arg52 = new Rect();
                        _arg53 = new Rect();
                        Rect _arg56 = _arg53;
                        _result = addWithoutInputChannel(_arg06, _arg16, _arg2, _arg12, _arg52, _arg53);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        parcel2.writeInt(i2);
                        _arg52.writeToParcel(parcel2, i2);
                        parcel2.writeInt(i2);
                        _arg56.writeToParcel(parcel2, i2);
                        return i2;
                    case 4:
                        parcel3 = parcel;
                        i2 = 1;
                        descriptor2 = descriptor;
                        parcel3.enforceInterface(descriptor2);
                        _arg0 = android.view.IWindow.Stub.asInterface(data.readStrongBinder());
                        _arg12 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg14 = (LayoutParams) LayoutParams.CREATOR.createFromParcel(parcel3);
                        }
                        _arg2 = _arg14;
                        _arg3 = data.readInt();
                        int _arg42 = data.readInt();
                        _arg5 = new Rect();
                        Rect _arg64 = new Rect();
                        Rect _arg57 = _arg5;
                        _result = addToDisplayWithoutInputChannel(_arg0, _arg12, _arg2, _arg3, _arg42, _arg5, _arg64);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        parcel2.writeInt(i2);
                        _arg57.writeToParcel(parcel2, i2);
                        parcel2.writeInt(i2);
                        _arg64.writeToParcel(parcel2, i2);
                        return i2;
                    case 5:
                        z = true;
                        data.enforceInterface(descriptor);
                        remove(android.view.IWindow.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return z;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        IWindow _arg07 = android.view.IWindow.Stub.asInterface(data.readStrongBinder());
                        int _arg17 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg14 = (LayoutParams) LayoutParams.CREATOR.createFromParcel(parcel);
                        }
                        _arg2 = _arg14;
                        int _arg33 = data.readInt();
                        int _arg43 = data.readInt();
                        int _arg58 = data.readInt();
                        int _arg65 = data.readInt();
                        long _arg72 = data.readLong();
                        rect = new Rect();
                        _arg6 = new Rect();
                        _arg5 = new Rect();
                        _arg53 = new Rect();
                        Rect _arg122 = new Rect();
                        _arg132 = new Rect();
                        Rect _arg142 = new Rect();
                        ParcelableWrapper _arg152 = new ParcelableWrapper();
                        MergedConfiguration _arg162 = new MergedConfiguration();
                        ParcelableWrapper _arg153 = _arg152;
                        Rect _arg143 = _arg142;
                        Rect _arg133 = _arg132;
                        Rect _arg123 = _arg122;
                        Rect _arg11 = _arg53;
                        Rect _arg102 = _arg5;
                        Rect _arg82 = rect;
                        Rect _arg93 = _arg6;
                        Surface _arg172 = new Surface();
                        _result = relayout(_arg07, _arg17, _arg2, _arg33, _arg43, _arg58, _arg65, _arg72, _arg82, _arg93, _arg102, _arg11, _arg123, _arg133, _arg143, _arg153, _arg162, _arg172);
                        reply.writeNoException();
                        parcel2 = reply;
                        parcel2.writeInt(_result);
                        parcel2.writeInt(1);
                        _arg82.writeToParcel(parcel2, 1);
                        parcel2.writeInt(1);
                        _arg93.writeToParcel(parcel2, 1);
                        parcel2.writeInt(1);
                        _arg102.writeToParcel(parcel2, 1);
                        parcel2.writeInt(1);
                        _arg11.writeToParcel(parcel2, 1);
                        parcel2.writeInt(1);
                        _arg123.writeToParcel(parcel2, 1);
                        parcel2.writeInt(1);
                        _arg133.writeToParcel(parcel2, 1);
                        parcel2.writeInt(1);
                        _arg143.writeToParcel(parcel2, 1);
                        parcel2.writeInt(1);
                        _arg153.writeToParcel(parcel2, 1);
                        parcel2.writeInt(1);
                        _arg162.writeToParcel(parcel2, 1);
                        parcel2.writeInt(1);
                        _arg172.writeToParcel(parcel2, 1);
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            z2 = true;
                        }
                        prepareToReplaceWindows(_arg03, z2);
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        boolean _result3 = outOfMemory(android.view.IWindow.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        IWindow _arg08 = android.view.IWindow.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg14 = (Region) Region.CREATOR.createFromParcel(parcel);
                        }
                        setTransparentRegion(_arg08, _arg14);
                        reply.writeNoException();
                        return true;
                    case 10:
                        Rect _arg23;
                        parcel.enforceInterface(descriptor);
                        IWindow _arg09 = android.view.IWindow.Stub.asInterface(data.readStrongBinder());
                        int _arg18 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg23 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg23 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg132 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg132 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg15 = (Region) Region.CREATOR.createFromParcel(parcel);
                        }
                        setInsets(_arg09, _arg18, _arg23, _arg132, _arg15);
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _arg04 = android.view.IWindow.Stub.asInterface(data.readStrongBinder());
                        Rect _arg19 = new Rect();
                        getDisplayFrame(_arg04, _arg19);
                        reply.writeNoException();
                        parcel2.writeInt(1);
                        _arg19.writeToParcel(parcel2, 1);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        finishDrawing(android.view.IWindow.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            z2 = true;
                        }
                        setInTouchMode(z2);
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        boolean _result4 = getInTouchMode();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        _arg04 = android.view.IWindow.Stub.asInterface(data.readStrongBinder());
                        int _arg110 = data.readInt();
                        if (data.readInt() != 0) {
                            z2 = true;
                        }
                        _result2 = performHapticFeedback(_arg04, _arg110, z2);
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 16:
                        SurfaceControl _arg24;
                        parcel.enforceInterface(descriptor);
                        _arg02 = android.view.IWindow.Stub.asInterface(data.readStrongBinder());
                        _arg13 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg24 = (SurfaceControl) SurfaceControl.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg24 = null;
                        }
                        _arg32 = data.readInt();
                        float _arg44 = data.readFloat();
                        float _arg59 = data.readFloat();
                        float _arg66 = data.readFloat();
                        float _arg73 = data.readFloat();
                        if (data.readInt() != 0) {
                            _arg15 = (ClipData) ClipData.CREATOR.createFromParcel(parcel);
                        }
                        _arg03 = performDrag(_arg02, _arg13, _arg24, _arg32, _arg44, _arg59, _arg66, _arg73, _arg15);
                        reply.writeNoException();
                        parcel2.writeStrongBinder(_arg03);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        _arg04 = android.view.IWindow.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            z2 = true;
                        }
                        reportDropResult(_arg04, z2);
                        reply.writeNoException();
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        cancelDragAndDrop(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        dragRecipientEntered(android.view.IWindow.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        dragRecipientExited(android.view.IWindow.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        setWallpaperPosition(data.readStrongBinder(), data.readFloat(), data.readFloat(), data.readFloat(), data.readFloat());
                        reply.writeNoException();
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        wallpaperOffsetsComplete(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        setWallpaperDisplayOffset(data.readStrongBinder(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        IBinder _arg010 = data.readStrongBinder();
                        String _arg111 = data.readString();
                        _arg13 = data.readInt();
                        _arg32 = data.readInt();
                        _arg4 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg15 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        _arg15 = sendWallpaperCommand(_arg010, _arg111, _arg13, _arg32, _arg4, _arg15, data.readInt() != 0);
                        reply.writeNoException();
                        if (_arg15 != null) {
                            parcel2.writeInt(1);
                            _arg15.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        _arg05 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg15 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        wallpaperCommandComplete(_arg05, _arg15);
                        reply.writeNoException();
                        return true;
                    case 26:
                        Rect _arg112;
                        parcel.enforceInterface(descriptor);
                        _arg05 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg112 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        }
                        onRectangleOnScreenRequested(_arg05, _arg112);
                        reply.writeNoException();
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        IWindowId _result5 = getWindowId(data.readStrongBinder());
                        reply.writeNoException();
                        if (_result5 != null) {
                            _arg03 = _result5.asBinder();
                        }
                        parcel2.writeStrongBinder(_arg03);
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        pokeDrawLock(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        _result2 = startMovingTask(android.view.IWindow.Stub.asInterface(data.readStrongBinder()), data.readFloat(), data.readFloat());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        updatePointerIcon(android.view.IWindow.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        updateTapExcludeRegion(android.view.IWindow.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel3 = parcel;
            z = true;
            parcel2.writeString(descriptor);
            return z;
        }
    }

    int add(IWindow iWindow, int i, LayoutParams layoutParams, int i2, Rect rect, Rect rect2, InputChannel inputChannel) throws RemoteException;

    int addToDisplay(IWindow iWindow, int i, LayoutParams layoutParams, int i2, int i3, Rect rect, Rect rect2, Rect rect3, Rect rect4, ParcelableWrapper parcelableWrapper, InputChannel inputChannel) throws RemoteException;

    int addToDisplayWithoutInputChannel(IWindow iWindow, int i, LayoutParams layoutParams, int i2, int i3, Rect rect, Rect rect2) throws RemoteException;

    int addWithoutInputChannel(IWindow iWindow, int i, LayoutParams layoutParams, int i2, Rect rect, Rect rect2) throws RemoteException;

    void cancelDragAndDrop(IBinder iBinder) throws RemoteException;

    void dragRecipientEntered(IWindow iWindow) throws RemoteException;

    void dragRecipientExited(IWindow iWindow) throws RemoteException;

    void finishDrawing(IWindow iWindow) throws RemoteException;

    void getDisplayFrame(IWindow iWindow, Rect rect) throws RemoteException;

    boolean getInTouchMode() throws RemoteException;

    IWindowId getWindowId(IBinder iBinder) throws RemoteException;

    void onRectangleOnScreenRequested(IBinder iBinder, Rect rect) throws RemoteException;

    boolean outOfMemory(IWindow iWindow) throws RemoteException;

    IBinder performDrag(IWindow iWindow, int i, SurfaceControl surfaceControl, int i2, float f, float f2, float f3, float f4, ClipData clipData) throws RemoteException;

    boolean performHapticFeedback(IWindow iWindow, int i, boolean z) throws RemoteException;

    void pokeDrawLock(IBinder iBinder) throws RemoteException;

    void prepareToReplaceWindows(IBinder iBinder, boolean z) throws RemoteException;

    int relayout(IWindow iWindow, int i, LayoutParams layoutParams, int i2, int i3, int i4, int i5, long j, Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, Rect rect6, Rect rect7, ParcelableWrapper parcelableWrapper, MergedConfiguration mergedConfiguration, Surface surface) throws RemoteException;

    void remove(IWindow iWindow) throws RemoteException;

    void reportDropResult(IWindow iWindow, boolean z) throws RemoteException;

    Bundle sendWallpaperCommand(IBinder iBinder, String str, int i, int i2, int i3, Bundle bundle, boolean z) throws RemoteException;

    void setInTouchMode(boolean z) throws RemoteException;

    void setInsets(IWindow iWindow, int i, Rect rect, Rect rect2, Region region) throws RemoteException;

    void setTransparentRegion(IWindow iWindow, Region region) throws RemoteException;

    void setWallpaperDisplayOffset(IBinder iBinder, int i, int i2) throws RemoteException;

    void setWallpaperPosition(IBinder iBinder, float f, float f2, float f3, float f4) throws RemoteException;

    boolean startMovingTask(IWindow iWindow, float f, float f2) throws RemoteException;

    void updatePointerIcon(IWindow iWindow) throws RemoteException;

    void updateTapExcludeRegion(IWindow iWindow, int i, int i2, int i3, int i4, int i5) throws RemoteException;

    void wallpaperCommandComplete(IBinder iBinder, Bundle bundle) throws RemoteException;

    void wallpaperOffsetsComplete(IBinder iBinder) throws RemoteException;
}
