package com.example.trivia;

import android.text.Editable;
import android.text.TextWatcher;

public abstract class SimpleTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // אין צורך לעשות כלום כאן
    }

    @Override
    public void afterTextChanged(Editable s) {
        // אין צורך לעשות כלום כאן
    }

    // המחלקה המרחיבה תיישם רק את onTextChanged
}