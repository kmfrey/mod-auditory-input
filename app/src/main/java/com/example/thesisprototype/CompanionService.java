package com.example.thesisprototype;

import android.companion.CompanionDeviceService;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.S)
public class CompanionService extends CompanionDeviceService {

    @Override
    public void onDeviceAppeared(@NonNull String s) {
        // set up input transfer (and recording)

        // set up output (mediaplayers in fragment)

    }

    @Override
    public void onDeviceDisappeared(@NonNull String s) {

    }
}
