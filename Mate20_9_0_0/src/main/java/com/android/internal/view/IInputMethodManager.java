package com.android.internal.view;

import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.text.style.SuggestionSpan;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.inputmethod.IInputContentUriToken;
import java.util.ArrayList;
import java.util.List;

public interface IInputMethodManager extends IInterface {

    public static abstract class Stub extends Binder implements IInputMethodManager {
        private static final String DESCRIPTOR = "com.android.internal.view.IInputMethodManager";
        static final int TRANSACTION_addClient = 7;
        static final int TRANSACTION_clearLastInputMethodWindowForTransition = 31;
        static final int TRANSACTION_createInputContentUriToken = 32;
        static final int TRANSACTION_finishInput = 9;
        static final int TRANSACTION_getCurrentInputMethodSubtype = 24;
        static final int TRANSACTION_getEnabledInputMethodList = 3;
        static final int TRANSACTION_getEnabledInputMethodSubtypeList = 4;
        static final int TRANSACTION_getHwInnerService = 35;
        static final int TRANSACTION_getInputMethodList = 1;
        static final int TRANSACTION_getInputMethodWindowVisibleHeight = 30;
        static final int TRANSACTION_getLastInputMethodSubtype = 5;
        static final int TRANSACTION_getShortcutInputMethodsAndSubtypes = 6;
        static final int TRANSACTION_getVrInputMethodList = 2;
        static final int TRANSACTION_hideMySoftInput = 18;
        static final int TRANSACTION_hideSoftInput = 11;
        static final int TRANSACTION_isInputMethodPickerShownForTest = 15;
        static final int TRANSACTION_notifySuggestionPicked = 23;
        static final int TRANSACTION_notifyUserAction = 34;
        static final int TRANSACTION_registerSuggestionSpansForNotification = 22;
        static final int TRANSACTION_removeClient = 8;
        static final int TRANSACTION_reportFullscreenMode = 33;
        static final int TRANSACTION_setAdditionalInputMethodSubtypes = 29;
        static final int TRANSACTION_setCurrentInputMethodSubtype = 25;
        static final int TRANSACTION_setImeWindowStatus = 21;
        static final int TRANSACTION_setInputMethod = 16;
        static final int TRANSACTION_setInputMethodAndSubtype = 17;
        static final int TRANSACTION_shouldOfferSwitchingToNextInputMethod = 28;
        static final int TRANSACTION_showInputMethodAndSubtypeEnablerFromClient = 14;
        static final int TRANSACTION_showInputMethodPickerFromClient = 13;
        static final int TRANSACTION_showMySoftInput = 19;
        static final int TRANSACTION_showSoftInput = 10;
        static final int TRANSACTION_startInputOrWindowGainedFocus = 12;
        static final int TRANSACTION_switchToNextInputMethod = 27;
        static final int TRANSACTION_switchToPreviousInputMethod = 26;
        static final int TRANSACTION_updateStatusIcon = 20;

        private static class Proxy implements IInputMethodManager {
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

            public List<InputMethodInfo> getInputMethodList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    List<InputMethodInfo> _result = _reply.createTypedArrayList(InputMethodInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<InputMethodInfo> getVrInputMethodList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    List<InputMethodInfo> _result = _reply.createTypedArrayList(InputMethodInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<InputMethodInfo> getEnabledInputMethodList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    List<InputMethodInfo> _result = _reply.createTypedArrayList(InputMethodInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<InputMethodSubtype> getEnabledInputMethodSubtypeList(String imiId, boolean allowsImplicitlySelectedSubtypes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imiId);
                    _data.writeInt(allowsImplicitlySelectedSubtypes);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    List<InputMethodSubtype> _result = _reply.createTypedArrayList(InputMethodSubtype.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public InputMethodSubtype getLastInputMethodSubtype() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    InputMethodSubtype _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (InputMethodSubtype) InputMethodSubtype.CREATOR.createFromParcel(_reply);
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

            public List getShortcutInputMethodsAndSubtypes() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    ArrayList _result = _reply.readArrayList(getClass().getClassLoader());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addClient(IInputMethodClient client, IInputContext inputContext, int uid, int pid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    IBinder iBinder = null;
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    if (inputContext != null) {
                        iBinder = inputContext.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    _data.writeInt(uid);
                    _data.writeInt(pid);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeClient(IInputMethodClient client) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void finishInput(IInputMethodClient client) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean showSoftInput(IInputMethodClient client, int flags, ResultReceiver resultReceiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeInt(flags);
                    boolean _result = true;
                    if (resultReceiver != null) {
                        _data.writeInt(1);
                        resultReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean hideSoftInput(IInputMethodClient client, int flags, ResultReceiver resultReceiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeInt(flags);
                    boolean _result = true;
                    if (resultReceiver != null) {
                        _data.writeInt(1);
                        resultReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public InputBindResult startInputOrWindowGainedFocus(int startInputReason, IInputMethodClient client, IBinder windowToken, int controlFlags, int softInputMode, int windowFlags, EditorInfo attribute, IInputContext inputContext, int missingMethodFlags, int unverifiedTargetSdkVersion) throws RemoteException {
                Throwable th;
                IBinder iBinder;
                int i;
                int i2;
                int i3;
                int i4;
                int i5;
                EditorInfo editorInfo = attribute;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeInt(startInputReason);
                        _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    } catch (Throwable th2) {
                        th = th2;
                        iBinder = windowToken;
                        i = controlFlags;
                        i2 = softInputMode;
                        i3 = windowFlags;
                        i4 = missingMethodFlags;
                        i5 = unverifiedTargetSdkVersion;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeStrongBinder(windowToken);
                        try {
                            _data.writeInt(controlFlags);
                            try {
                                _data.writeInt(softInputMode);
                            } catch (Throwable th3) {
                                th = th3;
                                i3 = windowFlags;
                                i4 = missingMethodFlags;
                                i5 = unverifiedTargetSdkVersion;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            i2 = softInputMode;
                            i3 = windowFlags;
                            i4 = missingMethodFlags;
                            i5 = unverifiedTargetSdkVersion;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        i = controlFlags;
                        i2 = softInputMode;
                        i3 = windowFlags;
                        i4 = missingMethodFlags;
                        i5 = unverifiedTargetSdkVersion;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(windowFlags);
                        if (editorInfo != null) {
                            _data.writeInt(1);
                            editorInfo.writeToParcel(_data, 0);
                        } else {
                            _data.writeInt(0);
                        }
                        _data.writeStrongBinder(inputContext != null ? inputContext.asBinder() : null);
                        try {
                            _data.writeInt(missingMethodFlags);
                        } catch (Throwable th6) {
                            th = th6;
                            i5 = unverifiedTargetSdkVersion;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeInt(unverifiedTargetSdkVersion);
                            try {
                                InputBindResult _result;
                                this.mRemote.transact(12, _data, _reply, 0);
                                _reply.readException();
                                if (_reply.readInt() != 0) {
                                    _result = (InputBindResult) InputBindResult.CREATOR.createFromParcel(_reply);
                                } else {
                                    _result = null;
                                }
                                InputBindResult _result2 = _result;
                                _reply.recycle();
                                _data.recycle();
                                return _result2;
                            } catch (Throwable th7) {
                                th = th7;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th8) {
                            th = th8;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th9) {
                        th = th9;
                        i4 = missingMethodFlags;
                        i5 = unverifiedTargetSdkVersion;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th10) {
                    th = th10;
                    int i6 = startInputReason;
                    iBinder = windowToken;
                    i = controlFlags;
                    i2 = softInputMode;
                    i3 = windowFlags;
                    i4 = missingMethodFlags;
                    i5 = unverifiedTargetSdkVersion;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public void showInputMethodPickerFromClient(IInputMethodClient client, int auxiliarySubtypeMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeInt(auxiliarySubtypeMode);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void showInputMethodAndSubtypeEnablerFromClient(IInputMethodClient client, String topId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeString(topId);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isInputMethodPickerShownForTest() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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

            public void setInputMethod(IBinder token, String id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(id);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setInputMethodAndSubtype(IBinder token, String id, InputMethodSubtype subtype) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(id);
                    if (subtype != null) {
                        _data.writeInt(1);
                        subtype.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void hideMySoftInput(IBinder token, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(flags);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void showMySoftInput(IBinder token, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(flags);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateStatusIcon(IBinder token, String packageName, int iconId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(packageName);
                    _data.writeInt(iconId);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setImeWindowStatus(IBinder token, IBinder startInputToken, int vis, int backDisposition) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeStrongBinder(startInputToken);
                    _data.writeInt(vis);
                    _data.writeInt(backDisposition);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void registerSuggestionSpansForNotification(SuggestionSpan[] spans) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedArray(spans, 0);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean notifySuggestionPicked(SuggestionSpan span, String originalString, int index) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (span != null) {
                        _data.writeInt(1);
                        span.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(originalString);
                    _data.writeInt(index);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public InputMethodSubtype getCurrentInputMethodSubtype() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    InputMethodSubtype _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (InputMethodSubtype) InputMethodSubtype.CREATOR.createFromParcel(_reply);
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

            public boolean setCurrentInputMethodSubtype(InputMethodSubtype subtype) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (subtype != null) {
                        _data.writeInt(1);
                        subtype.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean switchToPreviousInputMethod(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    boolean z = false;
                    this.mRemote.transact(26, _data, _reply, 0);
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

            public boolean switchToNextInputMethod(IBinder token, boolean onlyCurrentIme) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(onlyCurrentIme);
                    boolean z = false;
                    this.mRemote.transact(27, _data, _reply, 0);
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

            public boolean shouldOfferSwitchingToNextInputMethod(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    boolean z = false;
                    this.mRemote.transact(28, _data, _reply, 0);
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

            public void setAdditionalInputMethodSubtypes(String id, InputMethodSubtype[] subtypes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(id);
                    _data.writeTypedArray(subtypes, 0);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getInputMethodWindowVisibleHeight() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearLastInputMethodWindowForTransition(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IInputContentUriToken createInputContentUriToken(IBinder token, Uri contentUri, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (contentUri != null) {
                        _data.writeInt(1);
                        contentUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    IInputContentUriToken _result = com.android.internal.inputmethod.IInputContentUriToken.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportFullscreenMode(IBinder token, boolean fullscreen) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(fullscreen);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyUserAction(int sequenceNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sequenceNumber);
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public IBinder getHwInnerService() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IInputMethodManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IInputMethodManager)) {
                return new Proxy(obj);
            }
            return (IInputMethodManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            boolean z;
            if (i != 1598968902) {
                boolean z2 = false;
                ResultReceiver _arg2 = null;
                List<InputMethodInfo> _result;
                boolean _arg1;
                InputMethodSubtype _result2;
                IInputMethodClient _arg0;
                int _arg12;
                boolean _result3;
                IBinder _arg02;
                IBinder _arg03;
                switch (i) {
                    case 1:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = getInputMethodList();
                        reply.writeNoException();
                        parcel2.writeTypedList(_result);
                        return z;
                    case 2:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = getVrInputMethodList();
                        reply.writeNoException();
                        parcel2.writeTypedList(_result);
                        return z;
                    case 3:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = getEnabledInputMethodList();
                        reply.writeNoException();
                        parcel2.writeTypedList(_result);
                        return z;
                    case 4:
                        _arg1 = false;
                        z = true;
                        parcel.enforceInterface(descriptor);
                        String _arg04 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = z;
                        }
                        List<InputMethodSubtype> _result4 = getEnabledInputMethodSubtypeList(_arg04, _arg1);
                        reply.writeNoException();
                        parcel2.writeTypedList(_result4);
                        return z;
                    case 5:
                        i = 1;
                        parcel.enforceInterface(descriptor);
                        _result2 = getLastInputMethodSubtype();
                        reply.writeNoException();
                        if (_result2 != null) {
                            parcel2.writeInt(i);
                            _result2.writeToParcel(parcel2, i);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return i;
                    case 6:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        List _result5 = getShortcutInputMethodsAndSubtypes();
                        reply.writeNoException();
                        parcel2.writeList(_result5);
                        return z;
                    case 7:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        addClient(com.android.internal.view.IInputMethodClient.Stub.asInterface(data.readStrongBinder()), com.android.internal.view.IInputContext.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return z;
                    case 8:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        removeClient(com.android.internal.view.IInputMethodClient.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return z;
                    case 9:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        finishInput(com.android.internal.view.IInputMethodClient.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return z;
                    case 10:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _arg0 = com.android.internal.view.IInputMethodClient.Stub.asInterface(data.readStrongBinder());
                        _arg12 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg2 = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = showSoftInput(_arg0, _arg12, _arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return z;
                    case 11:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _arg0 = com.android.internal.view.IInputMethodClient.Stub.asInterface(data.readStrongBinder());
                        _arg12 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg2 = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = hideSoftInput(_arg0, _arg12, _arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return z;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        int _arg05 = data.readInt();
                        IInputMethodClient _arg13 = com.android.internal.view.IInputMethodClient.Stub.asInterface(data.readStrongBinder());
                        IBinder _arg22 = data.readStrongBinder();
                        int _arg3 = data.readInt();
                        int _arg4 = data.readInt();
                        int _arg5 = data.readInt();
                        if (data.readInt() != 0) {
                            _result2 = (EditorInfo) EditorInfo.CREATOR.createFromParcel(parcel);
                        }
                        InputMethodSubtype _arg6 = _result2;
                        i = 0;
                        i = 1;
                        InputBindResult _result6 = startInputOrWindowGainedFocus(_arg05, _arg13, _arg22, _arg3, _arg4, _arg5, _arg6, com.android.internal.view.IInputContext.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readInt());
                        reply.writeNoException();
                        if (_result6 != null) {
                            parcel2.writeInt(i);
                            _result6.writeToParcel(parcel2, i);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return i;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        showInputMethodPickerFromClient(com.android.internal.view.IInputMethodClient.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        showInputMethodAndSubtypeEnablerFromClient(com.android.internal.view.IInputMethodClient.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        boolean _result7 = isInputMethodPickerShownForTest();
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        setInputMethod(data.readStrongBinder(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readStrongBinder();
                        String _arg14 = data.readString();
                        if (data.readInt() != 0) {
                            _result2 = (InputMethodSubtype) InputMethodSubtype.CREATOR.createFromParcel(parcel);
                        }
                        setInputMethodAndSubtype(_arg02, _arg14, _result2);
                        reply.writeNoException();
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        hideMySoftInput(data.readStrongBinder(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        showMySoftInput(data.readStrongBinder(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        updateStatusIcon(data.readStrongBinder(), data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        setImeWindowStatus(data.readStrongBinder(), data.readStrongBinder(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        registerSuggestionSpansForNotification((SuggestionSpan[]) parcel.createTypedArray(SuggestionSpan.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 23:
                        SuggestionSpan _arg06;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg06 = (SuggestionSpan) SuggestionSpan.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = notifySuggestionPicked(_arg06, data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        _result2 = getCurrentInputMethodSubtype();
                        reply.writeNoException();
                        if (_result2 != null) {
                            parcel2.writeInt(1);
                            _result2.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result2 = (InputMethodSubtype) InputMethodSubtype.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = setCurrentInputMethodSubtype(_result2);
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        _arg1 = switchToPreviousInputMethod(data.readStrongBinder());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            z2 = true;
                        }
                        boolean _result8 = switchToNextInputMethod(_arg03, z2);
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        _arg1 = shouldOfferSwitchingToNextInputMethod(data.readStrongBinder());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        setAdditionalInputMethodSubtypes(data.readString(), (InputMethodSubtype[]) parcel.createTypedArray(InputMethodSubtype.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        int _result9 = getInputMethodWindowVisibleHeight();
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        clearLastInputMethodWindowForTransition(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 32:
                        Uri _arg15;
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg15 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg15 = null;
                        }
                        IInputContentUriToken _result10 = createInputContentUriToken(_arg02, _arg15, data.readString());
                        reply.writeNoException();
                        if (_result10 != null) {
                            _arg03 = _result10.asBinder();
                        }
                        parcel2.writeStrongBinder(_arg03);
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            z2 = true;
                        }
                        reportFullscreenMode(_arg03, z2);
                        reply.writeNoException();
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        notifyUserAction(data.readInt());
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        _arg03 = getHwInnerService();
                        reply.writeNoException();
                        parcel2.writeStrongBinder(_arg03);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            z = true;
            parcel2.writeString(descriptor);
            return z;
        }
    }

    void addClient(IInputMethodClient iInputMethodClient, IInputContext iInputContext, int i, int i2) throws RemoteException;

    void clearLastInputMethodWindowForTransition(IBinder iBinder) throws RemoteException;

    IInputContentUriToken createInputContentUriToken(IBinder iBinder, Uri uri, String str) throws RemoteException;

    void finishInput(IInputMethodClient iInputMethodClient) throws RemoteException;

    InputMethodSubtype getCurrentInputMethodSubtype() throws RemoteException;

    List<InputMethodInfo> getEnabledInputMethodList() throws RemoteException;

    List<InputMethodSubtype> getEnabledInputMethodSubtypeList(String str, boolean z) throws RemoteException;

    IBinder getHwInnerService() throws RemoteException;

    List<InputMethodInfo> getInputMethodList() throws RemoteException;

    int getInputMethodWindowVisibleHeight() throws RemoteException;

    InputMethodSubtype getLastInputMethodSubtype() throws RemoteException;

    List getShortcutInputMethodsAndSubtypes() throws RemoteException;

    List<InputMethodInfo> getVrInputMethodList() throws RemoteException;

    void hideMySoftInput(IBinder iBinder, int i) throws RemoteException;

    boolean hideSoftInput(IInputMethodClient iInputMethodClient, int i, ResultReceiver resultReceiver) throws RemoteException;

    boolean isInputMethodPickerShownForTest() throws RemoteException;

    boolean notifySuggestionPicked(SuggestionSpan suggestionSpan, String str, int i) throws RemoteException;

    void notifyUserAction(int i) throws RemoteException;

    void registerSuggestionSpansForNotification(SuggestionSpan[] suggestionSpanArr) throws RemoteException;

    void removeClient(IInputMethodClient iInputMethodClient) throws RemoteException;

    void reportFullscreenMode(IBinder iBinder, boolean z) throws RemoteException;

    void setAdditionalInputMethodSubtypes(String str, InputMethodSubtype[] inputMethodSubtypeArr) throws RemoteException;

    boolean setCurrentInputMethodSubtype(InputMethodSubtype inputMethodSubtype) throws RemoteException;

    void setImeWindowStatus(IBinder iBinder, IBinder iBinder2, int i, int i2) throws RemoteException;

    void setInputMethod(IBinder iBinder, String str) throws RemoteException;

    void setInputMethodAndSubtype(IBinder iBinder, String str, InputMethodSubtype inputMethodSubtype) throws RemoteException;

    boolean shouldOfferSwitchingToNextInputMethod(IBinder iBinder) throws RemoteException;

    void showInputMethodAndSubtypeEnablerFromClient(IInputMethodClient iInputMethodClient, String str) throws RemoteException;

    void showInputMethodPickerFromClient(IInputMethodClient iInputMethodClient, int i) throws RemoteException;

    void showMySoftInput(IBinder iBinder, int i) throws RemoteException;

    boolean showSoftInput(IInputMethodClient iInputMethodClient, int i, ResultReceiver resultReceiver) throws RemoteException;

    InputBindResult startInputOrWindowGainedFocus(int i, IInputMethodClient iInputMethodClient, IBinder iBinder, int i2, int i3, int i4, EditorInfo editorInfo, IInputContext iInputContext, int i5, int i6) throws RemoteException;

    boolean switchToNextInputMethod(IBinder iBinder, boolean z) throws RemoteException;

    boolean switchToPreviousInputMethod(IBinder iBinder) throws RemoteException;

    void updateStatusIcon(IBinder iBinder, String str, int i) throws RemoteException;
}
