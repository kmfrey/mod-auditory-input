package com.example.thesisprototype.utils;

import android.bluetooth.BluetoothClass;
import android.os.Build;

import java.util.Arrays;
import java.util.List;

public class SharedData {

    // Used to determine if an bluetooth device is applicable to our application.
    public static final List<Integer> AUDIO_CLASSES = Arrays.asList(
            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
            BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE,
            BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
            BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER);

    // Cheaper than an enum. Used to keep track of bluetooth device state.
    // int because Integer is not seen as constant.
    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_BONDING = 1;
    public static final int STATUS_BONDED = 2;
    public static final int STATUS_CONNECTING = 3;
    public static final int STATUS_CONNECTED = 4;
    public static final int STATUS_BOND_FAILED = 5;
    public static final int STATUS_CONNECT_FAILED = 6;

    public static final Integer INPUT_IDENTIFIER = 0;
    public static final Integer OUTPUT_IDENTIFIER = 1;
    public static final Integer UNKNOWN_IDENTIFIER = -1;

    public static boolean isSdkOverS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }
}
