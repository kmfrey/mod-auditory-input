package com.example.thesisprototype.fragments.home;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.thesisprototype.MainViewModel;
import com.example.thesisprototype.R;

import java.util.regex.Pattern;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private HomeViewModel homeViewModel;
    private MainViewModel mainViewModel;
    private FragmentHomeBinding binding;
    private Button inputB;
    private Button outputB;
    private CompanionDeviceManager deviceManager;
    public static final int INPUT_IDENTIFIER = 0;
    public static final int OUTPUT_IDENTIFIER = 1;
    private final String TAG = "HomeFragment";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // for name
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        inputB = root.findViewById(R.id.inputButton);
        inputB.setOnClickListener(this);
        mainViewModel.getInput().observe(getViewLifecycleOwner(), btDevice -> {
            if (btDevice == null) inputB.setText(R.string.connect_input);
            else inputB.setText(R.string.disconnect_input);
        });

        outputB = root.findViewById(R.id.outputButton);
        outputB.setOnClickListener(this);
        mainViewModel.getOutput().observe(getViewLifecycleOwner(), btDevice -> {
            if (btDevice == null) outputB.setText(R.string.connect_output);
            else outputB.setText(R.string.disconnect_output);
        });
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        // retrieve instance of CompanionDeviceManager
        deviceManager = (CompanionDeviceManager)
                requireContext().getSystemService(Context.COMPANION_DEVICE_SERVICE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.inputButton) manageInput();
        else if (id == R.id.outputButton) manageOutput();
    }

    private void connectDevices(int type) {
        // Does not show mac addresses.
        BluetoothDeviceFilter deviceFilter = new BluetoothDeviceFilter.Builder()
                .setNamePattern(Pattern.compile("^(?!(.{2}:)).*"))
                .build();
        AssociationRequest pairingRequest = new AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .setSingleDevice(false)
                .build();
        deviceManager.associate(pairingRequest, new CompanionDeviceManager.Callback() {

            @Override
            public void onDeviceFound(IntentSender chooserLauncher) {
                IntentSenderRequest request = new IntentSenderRequest.Builder(chooserLauncher).build();
                if (type == INPUT_IDENTIFIER) startAssociationInput.launch(request);
                else startAssociationOutput.launch(request);
            }

            @Override
            public void onFailure(CharSequence error) {
                Toast.makeText(getContext(), R.string.no_devices, Toast.LENGTH_LONG).show();
            }
        }, null);
    }

    public void manageInput() {
        if (mainViewModel.getInput().getValue() == null) connectDevices(INPUT_IDENTIFIER);
        else {
            // stop the service if it is running
            if (isSdkOverS()) {
                deviceManager.stopObservingDevicePresence(mainViewModel.getInput().getValue().getAddress());
            }
            deviceManager.disassociate(mainViewModel.getInput().getValue().getAddress());
            mainViewModel.getInput().setValue(null);
        }
    }

    public void manageOutput() {
        if (mainViewModel.getOutput().getValue() == null) connectDevices(OUTPUT_IDENTIFIER);
        else {
            // stop the service if it is running
            if (isSdkOverS()) {
                deviceManager.stopObservingDevicePresence(mainViewModel.getOutput().getValue().getAddress());
            }
            deviceManager.disassociate(mainViewModel.getOutput().getValue().getAddress());
            mainViewModel.getOutput().setValue(null);
        }
    }

    private final ActivityResultLauncher<IntentSenderRequest> startAssociationInput =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> associateDevice(result, INPUT_IDENTIFIER));

    private final ActivityResultLauncher<IntentSenderRequest> startAssociationOutput =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> associateDevice(result, OUTPUT_IDENTIFIER));

    private void associateDevice(ActivityResult result, int identifier) {
        String toastText = identifier == INPUT_IDENTIFIER ? "Input" : "Output";

        if (result.getResultCode() == Activity.RESULT_OK) {
            // Start a thread to connect the device
            new Thread(() -> {
                Intent data = result.getData();
                if (data == null) return;
                BluetoothDevice device = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
                if (device != null && (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_DENIED || !isSdkOverS())) {
                    boolean bondingStart = device.createBond();
                    // assign to inputDevice or outputDevice
                    if (identifier == INPUT_IDENTIFIER) {
                        mainViewModel.getInputBond().postValue(bondingStart);
                        if (bondingStart) mainViewModel.getInput().postValue(device);
                        else mainViewModel.getInput().postValue(null);
                    }
                    else {
                        mainViewModel.getOutputBond().postValue(bondingStart);
                        if (bondingStart) mainViewModel.getOutput().postValue(device);
                        else mainViewModel.getOutput().postValue(null);
                    }
                    // start service if S or higher & feature for companion exists
                    if (isSdkOverS() && requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP)) {
                        deviceManager.startObservingDevicePresence(device.getAddress());
                    }
                }
            }).start();
        }
        else if (result.getResultCode() == Activity.RESULT_CANCELED) {
            Toast.makeText(getContext(),
                    "No device was connected for " + toastText + ".", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isSdkOverS() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.S;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
