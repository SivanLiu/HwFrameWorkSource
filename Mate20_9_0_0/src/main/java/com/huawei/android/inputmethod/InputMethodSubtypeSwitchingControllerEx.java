package com.huawei.android.inputmethod;

import android.content.Context;
import android.view.inputmethod.InputMethodInfo;
import com.android.internal.inputmethod.InputMethodSubtypeSwitchingController;
import com.android.internal.inputmethod.InputMethodSubtypeSwitchingController.ImeSubtypeListItem;
import com.android.internal.inputmethod.InputMethodUtils.InputMethodSettings;
import com.huawei.android.inputmethod.InputMethodUtilsEx.InputMethodSettingsEx;
import java.util.ArrayList;
import java.util.List;

public class InputMethodSubtypeSwitchingControllerEx {
    InputMethodSubtypeSwitchingController mController;

    public static class ImeSubtypeListItemEx {
        ImeSubtypeListItem mItem;

        public ImeSubtypeListItemEx(ImeSubtypeListItem item) {
            this.mItem = item;
        }

        public InputMethodInfo getImi() {
            if (this.mItem != null) {
                return this.mItem.mImi;
            }
            return null;
        }

        public int getSubtypeId() {
            if (this.mItem != null) {
                return this.mItem.mSubtypeId;
            }
            return -1;
        }

        public CharSequence getImeName() {
            if (this.mItem != null) {
                return this.mItem.mImeName;
            }
            return null;
        }

        public CharSequence getSubtypeName() {
            if (this.mItem != null) {
                return this.mItem.mSubtypeName;
            }
            return null;
        }
    }

    private InputMethodSubtypeSwitchingControllerEx(InputMethodSettings settings, Context context) {
        this.mController = InputMethodSubtypeSwitchingController.createInstanceLocked(settings, context);
    }

    public static InputMethodSubtypeSwitchingControllerEx createInstanceLocked(InputMethodSettingsEx settings, Context context) {
        return new InputMethodSubtypeSwitchingControllerEx(settings.mSettings, context);
    }

    public List<ImeSubtypeListItemEx> getSortedInputMethodAndSubtypeListLocked(boolean includingAuxiliarySubtypes, boolean isScreenLocked) {
        List<ImeSubtypeListItemEx> list = new ArrayList();
        List<ImeSubtypeListItem> imList = new ArrayList();
        if (this.mController != null) {
            imList = this.mController.getSortedInputMethodAndSubtypeListLocked(includingAuxiliarySubtypes, isScreenLocked);
        }
        int N = imList.size();
        for (int i = 0; i < N; i++) {
            list.add(new ImeSubtypeListItemEx((ImeSubtypeListItem) imList.get(i)));
        }
        return list;
    }
}
