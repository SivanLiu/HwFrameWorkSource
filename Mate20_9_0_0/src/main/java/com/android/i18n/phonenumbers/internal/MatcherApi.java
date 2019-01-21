package com.android.i18n.phonenumbers.internal;

import com.android.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;

public interface MatcherApi {
    boolean matchNationalNumber(CharSequence charSequence, PhoneNumberDesc phoneNumberDesc, boolean z);
}
