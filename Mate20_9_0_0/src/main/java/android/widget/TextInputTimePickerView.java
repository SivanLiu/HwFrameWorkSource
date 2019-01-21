package android.widget;

import android.content.Context;
import android.os.LocaleList;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.view.View;
import android.widget.AdapterView.OnItemSelectedListener;

public class TextInputTimePickerView extends RelativeLayout {
    private static final int AM = 0;
    public static final int AMPM = 2;
    public static final int HOURS = 0;
    public static final int MINUTES = 1;
    private static final int PM = 1;
    private final Spinner mAmPmSpinner;
    private final TextView mErrorLabel;
    private boolean mErrorShowing;
    private final EditText mHourEditText;
    private boolean mHourFormatStartsAtZero;
    private final TextView mHourLabel;
    private final TextView mInputSeparatorView;
    private boolean mIs24Hour;
    private OnValueTypedListener mListener;
    private final EditText mMinuteEditText;
    private final TextView mMinuteLabel;

    interface OnValueTypedListener {
        void onValueChanged(int i, int i2);
    }

    public TextInputTimePickerView(Context context) {
        this(context, null);
    }

    public TextInputTimePickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextInputTimePickerView(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    public TextInputTimePickerView(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
        View.inflate(context, 17367320, this);
        this.mHourEditText = (EditText) findViewById(16908999);
        this.mMinuteEditText = (EditText) findViewById(16909000);
        this.mInputSeparatorView = (TextView) findViewById(16909002);
        this.mErrorLabel = (TextView) findViewById(16909028);
        this.mHourLabel = (TextView) findViewById(16909029);
        this.mMinuteLabel = (TextView) findViewById(16909030);
        this.mHourEditText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                TextInputTimePickerView.this.parseAndSetHourInternal(editable.toString());
            }
        });
        this.mMinuteEditText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                TextInputTimePickerView.this.parseAndSetMinuteInternal(editable.toString());
            }
        });
        this.mAmPmSpinner = (Spinner) findViewById(16908729);
        String[] amPmStrings = TimePicker.getAmPmStrings(context);
        SpinnerAdapter adapter = new ArrayAdapter(context, 17367049);
        adapter.add(TimePickerClockDelegate.obtainVerbatim(amPmStrings[0]));
        adapter.add(TimePickerClockDelegate.obtainVerbatim(amPmStrings[1]));
        this.mAmPmSpinner.setAdapter(adapter);
        this.mAmPmSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (position == 0) {
                    TextInputTimePickerView.this.mListener.onValueChanged(2, 0);
                } else {
                    TextInputTimePickerView.this.mListener.onValueChanged(2, 1);
                }
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    void setListener(OnValueTypedListener listener) {
        this.mListener = listener;
    }

    void setHourFormat(int maxCharLength) {
        this.mHourEditText.setFilters(new InputFilter[]{new LengthFilter(maxCharLength)});
        this.mMinuteEditText.setFilters(new InputFilter[]{new LengthFilter(maxCharLength)});
        LocaleList locales = this.mContext.getResources().getConfiguration().getLocales();
        this.mHourEditText.setImeHintLocales(locales);
        this.mMinuteEditText.setImeHintLocales(locales);
    }

    boolean validateInput() {
        boolean z = false;
        boolean inputValid = parseAndSetHourInternal(this.mHourEditText.getText().toString()) && parseAndSetMinuteInternal(this.mMinuteEditText.getText().toString());
        if (!inputValid) {
            z = true;
        }
        setError(z);
        return inputValid;
    }

    void updateSeparator(String separatorText) {
        this.mInputSeparatorView.setText((CharSequence) separatorText);
    }

    private void setError(boolean enabled) {
        this.mErrorShowing = enabled;
        int i = 4;
        this.mErrorLabel.setVisibility(enabled ? 0 : 4);
        this.mHourLabel.setVisibility(enabled ? 4 : 0);
        TextView textView = this.mMinuteLabel;
        if (!enabled) {
            i = 0;
        }
        textView.setVisibility(i);
    }

    void updateTextInputValues(int localizedHour, int minute, int amOrPm, boolean is24Hour, boolean hourFormatStartsAtZero) {
        String hourFormat = "%d";
        String minuteFormat = "%02d";
        this.mIs24Hour = is24Hour;
        this.mHourFormatStartsAtZero = hourFormatStartsAtZero;
        this.mAmPmSpinner.setVisibility(is24Hour ? 4 : 0);
        if (amOrPm == 0) {
            this.mAmPmSpinner.setSelection(0);
        } else {
            this.mAmPmSpinner.setSelection(1);
        }
        this.mHourEditText.setText((CharSequence) String.format("%d", new Object[]{Integer.valueOf(localizedHour)}));
        this.mMinuteEditText.setText((CharSequence) String.format("%02d", new Object[]{Integer.valueOf(minute)}));
        if (this.mErrorShowing) {
            validateInput();
        }
    }

    private boolean parseAndSetHourInternal(String input) {
        try {
            int hour = Integer.parseInt(input);
            if (isValidLocalizedHour(hour)) {
                this.mListener.onValueChanged(0, getHourOfDayFromLocalizedHour(hour));
                return true;
            }
            int minHour = this.mHourFormatStartsAtZero ^ 1;
            this.mListener.onValueChanged(0, getHourOfDayFromLocalizedHour(MathUtils.constrain(hour, minHour, this.mIs24Hour ? 23 : 11 + minHour)));
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean parseAndSetMinuteInternal(String input) {
        try {
            int minutes = Integer.parseInt(input);
            if (minutes >= 0) {
                if (minutes <= 59) {
                    this.mListener.onValueChanged(1, minutes);
                    return true;
                }
            }
            this.mListener.onValueChanged(1, MathUtils.constrain(minutes, 0, 59));
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidLocalizedHour(int localizedHour) {
        int minHour = this.mHourFormatStartsAtZero ^ 1;
        int maxHour = (this.mIs24Hour ? 23 : 11) + minHour;
        if (localizedHour < minHour || localizedHour > maxHour) {
            return false;
        }
        return true;
    }

    private int getHourOfDayFromLocalizedHour(int localizedHour) {
        int hourOfDay = localizedHour;
        if (!this.mIs24Hour) {
            if (!this.mHourFormatStartsAtZero && localizedHour == 12) {
                hourOfDay = 0;
            }
            if (this.mAmPmSpinner.getSelectedItemPosition() == 1) {
                return hourOfDay + 12;
            }
            return hourOfDay;
        } else if (this.mHourFormatStartsAtZero || localizedHour != 24) {
            return hourOfDay;
        } else {
            return 0;
        }
    }
}
