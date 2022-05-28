package com.example.thesisprototype.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.thesisprototype.MainActivity;
import com.example.thesisprototype.R;
import com.example.thesisprototype.databinding.FragmentHomeBinding;

import java.util.List;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private static HomeFragment instance;
    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;
    private MainActivity mainActivity;
    private Button inputB;
    private Button outputB;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        instance = this;

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        mainActivity = (MainActivity) getActivity();
        inputB = root.findViewById(R.id.inputButton);
        inputB.setOnClickListener(this);
        outputB = root.findViewById(R.id.outputButton);
        outputB.setOnClickListener(this);
        return root;
    }

    public static HomeFragment getInstance() {
        return instance;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.inputButton) mainActivity.manageInput();
        else if (id == R.id.outputButton) mainActivity.manageOutput();
    }

    public void updateButton(int buttonID, boolean connected) {
        // Determine button based on ID.
        Button b = buttonID == R.id.inputButton ? inputB : outputB;
        TextView devices = getView().findViewById(R.id.connected_devices);
        // update text based on state.
        int textID;
        if (connected) textID = b.equals(inputB) ? R.string.disconnect_input : R.string.disconnect_output;
        else textID = b.equals(inputB) ? R.string.connect_input : R.string.connect_output;
        b.setText(textID);

        // get the connected devices and display names
        List<String> names = mainActivity.getConnectedDeviceNames();
        StringBuilder devicesString = new StringBuilder();
        names.forEach(devicesString::append);
        devices.setText(devicesString);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}