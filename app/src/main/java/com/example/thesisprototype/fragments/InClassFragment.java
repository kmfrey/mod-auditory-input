package com.example.thesisprototype.fragments.inclass;


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.thesisprototype.MainViewModel;
import com.example.thesisprototype.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;

public class InClassFragment extends Fragment {

    private InClassViewModel inClassViewModel;
    private MainViewModel mainViewModel;
    private FragmentInclassBinding binding;
    private String TAG = "InClassFragment";
    private MediaPlayer lecture;
    private MediaPlayer background;
    private int audioId;
    private AudioManager audioManager;

    private int maxVol;
    private int inputVol;
    private int backgroundVol;
    private SeekBar inputVolControl;
    private SeekBar backgroundVolControl;
    BluetoothDevice output;
    BluetoothDevice input;
    private FloatingActionButton playButton;

    private boolean isPlaying = false;
    private boolean canPlay = false;
    private final int lectureIdentifier = 0;
    private final int backgroundIdentifier = 1;

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

        inputVolControl = root.findViewById(R.id.lectureVolume);
        backgroundVolControl = root.findViewById(R.id.backgroundVolume);
        playButton = root.findViewById(R.id.playButton);
        // Should be disabled until both input & output are assigned
        playButton.setEnabled(false);
        playButton.setOnClickListener(view -> manageLectureStream());

        mainViewModel.getOutput().observe(getViewLifecycleOwner(), btDevice -> {
            output = btDevice;
            if (checkDevices()) playButton.setEnabled(true);
        });
        mainViewModel.getInput().observe(getViewLifecycleOwner(), btDevice -> {
            input = btDevice;
            if (checkDevices()) playButton.setEnabled(true);
        });

        RadioGroup musicChoiceGroup = root.findViewById(R.id.backgroundGroup);
        musicChoiceGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            // i is the id of the checked button
            if (i == R.id.rain) audioId = R.raw.rain;
            else if (i == R.id.classical) audioId = R.raw.classical;
            else if (i == R.id.piano) audioId = R.raw.valleysunrise;
            else audioId = -1;
        });
        return root;
    }

    private void manageLectureStream() {
        // stop the stream if it is currently playing.
        if (isPlaying) {
            // reset to the play button
            playButton.setImageResource(R.drawable.ic_play);

        } else {
            // FAB image should be a pause
            playButton.setImageResource(R.drawable.ic_pause);

        }
        // update the status.
        isPlaying = !isPlaying;
    }

    // Check if there are two devices with the correct capabilities.
    private boolean checkDevices() {
        // are there two connected? Are they both audio devices?
        if (input == null || output == null) {
            canPlay = false;
            return false;
        }
        // check that the AudioManager has knowledge of the devices.
        AudioDeviceInfo[] devices = audioManager.getDevices(0);
        canPlay = Arrays.stream(devices).anyMatch(info -> info.getAddress().equals(input.getAddress()))
                && Arrays.stream(devices).anyMatch(info -> info.getAddress().equals(output.getAddress()));
        return canPlay;
    }

    // Some more time-intensive operations, don't need them until the fragment starts.
    @Override
    public void onStart() {
        super.onStart();
        maxVol =  audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        try {
            inputVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            backgroundVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        }
        setUpVolumeControl();
    }

    // Only called when there are two audio devices connected.
    private void createEngine() {

    }

    private void assignAudioDevices() {
        if (!canPlay) return;

    }

    private void setUpVolumeControl() {
        inputVolControl.setMax(maxVol);
        inputVolControl.setProgress(inputVol);
        inputVolControl.setOnSeekBarChangeListener(volumeChangeListener(lectureIdentifier));

        backgroundVolControl.setMax(maxVol);
        backgroundVolControl.setProgress(backgroundVol);
        backgroundVolControl.setOnSeekBarChangeListener(volumeChangeListener(backgroundIdentifier));
    }

    private SeekBar.OnSeekBarChangeListener volumeChangeListener(int identifier) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // change the volume of media to float value
                float vol = (float) i;
                if (identifier == lectureIdentifier && lecture != null) {
                    lecture.setVolume(vol, vol);
                }
                else if (identifier == backgroundIdentifier && background != null) {
                    background.setVolume(vol, vol);
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