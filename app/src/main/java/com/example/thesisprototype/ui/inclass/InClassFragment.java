package com.example.thesisprototype.ui.inclass;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.thesisprototype.MainActivity;
import com.example.thesisprototype.R;
import com.example.thesisprototype.databinding.FragmentInclassBinding;

public class InClassFragment extends Fragment {

    private InClassViewModel inClassViewModel;
    private FragmentInclassBinding binding;
    private MainActivity mainActivity;
    private MediaPlayer lecture;
    private MediaPlayer background;

    private int maxVol;
    private int inputVol;
    private int backgroundVol;
    private SeekBar inputVolControl;
    private SeekBar backgroundVolControl;
    private RadioGroup musicChoiceGroup;

    private int lectureIdentifier = 0;
    private int backgroundIdentifier = 1;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        inClassViewModel =
                new ViewModelProvider(this).get(InClassViewModel.class);

        binding = FragmentInclassBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textDashboard;
        inClassViewModel.getText().observe(getViewLifecycleOwner(), s -> textView.setText(s));
        mainActivity = (MainActivity)getActivity();
        assert mainActivity != null;
        AudioManager manager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        maxVol =  manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        setUpVolumeControl(root);
        musicChoiceGroup = root.findViewById(R.id.backgroundGroup);

        return root;
    }

    private void setUpMedia() {
        lecture = new MediaPlayer();
        background = new MediaPlayer();

        // prepare them on different

    }

    private void setUpVolumeControl(View root) {
        inputVolControl = root.findViewById(R.id.lectureVolume);
        inputVolControl.setMax(maxVol);
        inputVolControl.setProgress(inputVol);
        inputVolControl.setOnSeekBarChangeListener(volumeChangeListener(lectureIdentifier));

        backgroundVolControl = root.findViewById(R.id.backgroundVolume);
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