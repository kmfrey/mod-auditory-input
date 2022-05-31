package com.example.thesisprototype.fragments.inclass;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class InClassViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public InClassViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is the in-class fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}