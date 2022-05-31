package com.example.thesisprototype;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class MainViewModel extends ViewModel {

    private MutableLiveData<BluetoothDevice> mInput;
    private MutableLiveData<BluetoothDevice> mOutput;
    private MutableLiveData<Boolean> mInputBond;
    private MutableLiveData<Boolean> mOutputBond;

    public MutableLiveData<BluetoothDevice> getInput() {
        if (mInput == null) mInput = new MutableLiveData<>();
        return mInput;
    }
    public MutableLiveData<BluetoothDevice> getOutput() {
        if (mOutput == null) mOutput = new MutableLiveData<>();
        return mOutput;
    }

    public MutableLiveData<Boolean> getInputBond() {
        if (mInputBond == null) mInputBond = new MutableLiveData<>();
        return mInputBond;
    }

    public MutableLiveData<Boolean> getOutputBond() {
        if (mOutputBond == null) mOutputBond = new MutableLiveData<>();
        return mOutputBond;
    }
}
