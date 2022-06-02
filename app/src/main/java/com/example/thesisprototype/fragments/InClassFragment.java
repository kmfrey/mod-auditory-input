package com.example.thesisprototype.fragments;


import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.thesisprototype.models.MainViewModel;
import com.example.thesisprototype.models.InClassViewModel;
import com.example.thesisprototype.R;
import com.example.thesisprototype.databinding.FragmentInclassBinding;
import com.example.thesisprototype.utils.LiveEffectEngine;
import com.example.thesisprototype.utils.SharedData;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class InClassFragment extends Fragment {

    private InClassViewModel inClassViewModel;
    private MainViewModel mainViewModel;
    private FragmentInclassBinding binding;
    private String TAG = "InClassFragment";
    private MediaPlayer background;
    private int audioId = -1;
    private AudioManager audioManager;

    private int maxVol;
    private int backgroundVol;
    private SeekBar backgroundVolControl;
    private FloatingActionButton playButton;

    private boolean isPlaying = false;
    private boolean inputConnected = false;
    private boolean outputConnected = false;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        inClassViewModel =
                new ViewModelProvider(this).get(InClassViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding = FragmentInclassBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textDashboard;
        inClassViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);

        backgroundVolControl = root.findViewById(R.id.backgroundVolume);
        playButton = root.findViewById(R.id.playButton);
        // Should be disabled until both input & output are assigned
        playButton.setEnabled(false);
        playButton.setOnClickListener(view -> manageLectureStream());

        // associate the btDevice with the AudioManager, check if able to start.
        mainViewModel.getDevice(SharedData.INPUT_IDENTIFIER).observe(getViewLifecycleOwner(), btDevice -> {
            if (btDevice != null) {
                // try to connect the input stream
                assignDeviceToEngine(btDevice);
                inputConnected = true;
            }
            else inputConnected = false;
            playButton.setEnabled(inputConnected && outputConnected);
        });
        mainViewModel.getConnectionStatus(SharedData.INPUT_IDENTIFIER).observe(getViewLifecycleOwner(), status -> {
            if (status == SharedData.STATUS_BONDED || status == SharedData.STATUS_CONNECTED) {
                inputConnected = true;
                BluetoothDevice d;
                if ((d = mainViewModel.getDevice(SharedData.INPUT_IDENTIFIER).getValue()) != null)
                    assignDeviceToEngine(d);
            }
        });

        new Thread(() -> LiveEffectEngine.setDefaultStreamValues(requireContext())).start();

        RadioGroup musicChoiceGroup = root.findViewById(R.id.backgroundGroup);
        musicChoiceGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            // i is the id of the checked button
            if (i == R.id.rain) audioId = R.raw.rain;
            else if (i == R.id.classical) audioId = R.raw.classical;
            else if (i == R.id.piano) audioId = R.raw.valleysunrise;
            else audioId = -1;
            // update the MediaPlayer
           updateBackgroundMedia(audioId);
        });
        return root;
    }

    // Some more time-intensive operations, don't need them until the fragment starts.
    @Override
    public void onStart() {
        super.onStart();
        maxVol =  audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        try {
            backgroundVol = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        }
        backgroundVolControl.setMax(maxVol);
        backgroundVolControl.setProgress(backgroundVol);
        backgroundVolControl.setOnSeekBarChangeListener(volumeChangeListener());
    }

    @Override
    public void onResume() {
        super.onResume();
        // set up the Engine
        LiveEffectEngine.create();
        LiveEffectEngine.setAPI(0);

        if (audioId != -1) {
            background = MediaPlayer.create(requireContext(), audioId);
            background.setPreferredDevice(getSink());
            background.setLooping(true);
            background.setVolume(backgroundVol, backgroundVol);
            background.setOnPreparedListener(mediaPlayer -> {
                playButton.setEnabled(true);
                if (isPlaying) background.start();
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (background != null) {
            background.stop();
            background.release();
            background = null;
        }
    }

    private void manageLectureStream() {
        // stop the stream if it is currently playing.
        if (isPlaying) {
            // reset to the play button
            playButton.setImageResource(R.drawable.ic_play);
            if (background != null && background.isPlaying()) background.pause();
            LiveEffectEngine.setEffectOn(false);

        } else {
            // FAB image should be a pause
            playButton.setImageResource(R.drawable.ic_pause);
            // start the background media player
            if (getSink() == null) {
                outputConnected = false;
            } else outputConnected = true;
            LiveEffectEngine.setPlaybackDeviceId(getSink().getId());
            if (background != null) {
                background.start();
            }
            LiveEffectEngine.setEffectOn(true);
        }
        // update the status.
        isPlaying = !isPlaying;
        playButton.setEnabled(inputConnected && outputConnected);
    }

    private void updateBackgroundMedia(int rawId) {
        // try to get the sink
        AudioDeviceInfo sink = getSink();
        if (sink == null) {
            Log.e(TAG, "No sink is attached.");
            return;
        }
        outputConnected = true;
        // Instantiate a mediaPlayer if there is not one currently.
        if (rawId == -1) {
            // release the current MediaPlayer
            if (background != null) background.reset();
        }
        else if (background == null) {
            background = MediaPlayer.create(requireContext(), rawId);
            // set the preferred device for the output.
            background.setPreferredDevice(sink);
            background.setOnPreparedListener(mediaPlayer -> {
            });
            background.setLooping(true);
        } else {
            // is the Media currently playing? Or should it be, in the case of no audio?
            boolean startPlay = background.isPlaying() || isPlaying;
            // Otherwise, update the data source.
            background.reset();
            try {
                AssetFileDescriptor afd = requireContext().getResources().openRawResourceFd(rawId);
                if (afd == null) return;
                background.setDataSource(afd);
                afd.close();
                background.setPreferredDevice(sink);
                background.prepare();
                if (startPlay) background.start();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        } playButton.setEnabled(inputConnected && outputConnected);
    }

//    // Check if there are two devices with the correct capabilities.
//   // Run in another thread.
//    private void checkDevices() {
//        // Handler to send the information back to the UI thread
//
//        // are there two connected? Are they both audio devices?
//        new Thread(() -> {
//            BluetoothDevice input = mainViewModel.getDevice(SharedData.INPUT_IDENTIFIER).getValue();
//            Integer connectionStatus = mainViewModel.getConnectionStatus(SharedData.INPUT_IDENTIFIER).getValue();
//            canPlay = false;
//            if (input != null && connectionStatus.equals(SharedData.STATUS_CONNECTED)) {
//                Log.i(TAG, "Input device is connected.");
//                // check that input device is the preferred device
//                if (checkIfDevicePreferred(input)) {
//                    Log.d(TAG, "Input device is preferred");
//                    canPlay = true;
//                }
//            }
//            // check that the AudioManager has knowledge of the devices.
//            getView().post((Runnable) () -> {
//                playButton.setEnabled(canPlay);
//            });
//        });
//    }


    @Override
    public void onStop() {
//        audioManager.stopBluetoothSco();
//        audioManager.setMode(AudioManager.MODE_NORMAL);
        LiveEffectEngine.setEffectOn(false);
        LiveEffectEngine.delete();
        if (background != null && background.isPlaying()) {
            background.release();
            background = null;
        }
        super.onStop();
    }

    // Start on another thread.
    private void assignDeviceToEngine(BluetoothDevice device) {
        // make sure device is bonded. Connect to audioManager.
        new Thread(() -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                inputConnected = false;
                if (device != null && device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
                    // set the id of the recording device for the engine, if device exists & is a source.
                    for (AudioDeviceInfo deviceInfo : devices) {
                        if (deviceInfo.getAddress().equals(device.getAddress())) {
                            LiveEffectEngine.setRecordingDeviceId(deviceInfo.getId());
                            inputConnected = true;
                            Log.d(TAG, "Device " + deviceInfo.getProductName() + " was set as recording.");
                        }
                    } if (!inputConnected) {
                        Log.e(TAG, "No source was set.");
                    }
                }
        }).start();
    }

    private AudioDeviceInfo getSink() {
        AudioDeviceInfo[] sinks = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        List<Integer> wiredType = Arrays.asList(
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_ACCESSORY,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET);
        for (AudioDeviceInfo sink : sinks) {
            if (wiredType.contains(sink.getType())) {
                outputConnected = true;
                Log.d(TAG, "Device type: " + sink.getType());
                return sink;
            }
        }
        outputConnected = false;
        return null;
    }


    private SeekBar.OnSeekBarChangeListener volumeChangeListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // change the volume of media to float value, squared (to approximate log).
                float vol = (float) Math.pow((double) i/maxVol, 2);
                if (background != null) {
                    try {
                        background.setVolume(vol, vol);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "invalid MediaPlayer for background");
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}