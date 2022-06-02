package com.example.thesisprototype.models;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.thesisprototype.utils.SharedData;

import java.util.List;

public class MainViewModel extends ViewModel {

    private MutableLiveData<BluetoothDevice> mInput;
    private MutableLiveData<BluetoothDevice> mOutput;
    private MutableLiveData<Integer> mConnectionStatusInput;
    private MutableLiveData<Integer> mConnectionStatusOutput;

    public MutableLiveData<BluetoothDevice> getDevice(Integer id) {
        if (id.equals(SharedData.INPUT_IDENTIFIER)) return getInput();
        else return getOutput();
    }
    private MutableLiveData<BluetoothDevice> getInput() {
        if (mInput == null) mInput = new MutableLiveData<>();
        return mInput;
    }
    private MutableLiveData<BluetoothDevice> getOutput() {
        if (mOutput == null) mOutput = new MutableLiveData<>();
        return mOutput;
    }

    public MutableLiveData<Integer> getConnectionStatus(Integer id) {
        if (id.equals(SharedData.INPUT_IDENTIFIER)) return getConnectionStatusInput();
        else return getConnectionStatusOutput();
    }

    private MutableLiveData<Integer> getConnectionStatusInput() {
        if (mConnectionStatusInput == null) mConnectionStatusInput = new MutableLiveData<>();
        return mConnectionStatusInput;
    }

    private MutableLiveData<Integer> getConnectionStatusOutput() {
        if (mConnectionStatusOutput == null) mConnectionStatusOutput = new MutableLiveData<>();
        return mConnectionStatusOutput;
    }
}
