package com.example.thesisprototype.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.PaintKt;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.thesisprototype.models.MainViewModel;
import com.example.thesisprototype.R;
import com.example.thesisprototype.databinding.FragmentHomeBinding;
import com.example.thesisprototype.models.HomeViewModel;
import com.example.thesisprototype.utils.SharedData;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private HomeViewModel homeViewModel;
    private MainViewModel mainViewModel;
    private FragmentHomeBinding binding;
    private Button inputB;
    private Button outputB;
    private CompanionDeviceManager deviceManager;
    private BluetoothAdapter btAdapter;
    private List<BluetoothDevice> possibleConnections;
    private String inputName;
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
        mainViewModel.getDevice(SharedData.INPUT_IDENTIFIER).observe(getViewLifecycleOwner(), btDevice -> {
            if (btDevice == null) {
                inputB.setText(R.string.connect_input);
                inputName = null;
            } else {
                inputB.setText(R.string.disconnect_input);
                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                        || !isSdkOverS()) inputName = btDevice.getName();
            }
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        // retrieve instance of CompanionDeviceManager
        deviceManager = (CompanionDeviceManager)
                requireContext().getSystemService(Context.COMPANION_DEVICE_SERVICE);
        btAdapter = requireContext().getSystemService(BluetoothManager.class).getAdapter();
        checkForDevices();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().registerReceiver(bondReceiver,
                new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        IntentFilter aclIntent = new IntentFilter();
        aclIntent.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        aclIntent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        requireActivity().registerReceiver(connectReceiver, aclIntent);
    }

    @Override
    public void onPause() {
        requireActivity().unregisterReceiver(bondReceiver);
        requireActivity().unregisterReceiver(connectReceiver);
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        Integer directionId = SharedData.UNKNOWN_IDENTIFIER;
        if (id == R.id.inputButton) directionId = SharedData.INPUT_IDENTIFIER;
//        else if (id == R.id.outputButton) directionId = SharedData.OUTPUT_IDENTIFIER;

        if (mainViewModel.getDevice(directionId).getValue() == null) {
            if (possibleConnections != null && !possibleConnections.isEmpty()) {
                // start a dialog, ask if they want to connect.
                createConnectionDialog(directionId);
            } else pairNewDevice(directionId);
        } else {
            deviceManager.disassociate(mainViewModel.getDevice(directionId).getValue().getAddress());
            updateModel(null, SharedData.STATUS_UNKNOWN, directionId);
        }
    }

    private void pairNewDevice(int type) {
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
                if (type == SharedData.INPUT_IDENTIFIER) startAssociationInput.launch(request);
                else startAssociationOutput.launch(request);
            }

            @Override
            public void onFailure(CharSequence error) {
                Toast.makeText(getContext(), R.string.no_devices, Toast.LENGTH_LONG).show();
            }
        }, null);
    }

    // Look for already paired devices that fit the audio profile.
    private void checkForDevices() {
        new Thread(() -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_DENIED || !isSdkOverS()) {
                if (!btAdapter.getBondedDevices().isEmpty()) {
                    possibleConnections = btAdapter.getBondedDevices()
                            .stream().filter(btDevice ->
                                    SharedData.AUDIO_CLASSES.contains(btDevice.getBluetoothClass().getDeviceClass()))
                            .collect(Collectors.toList());
                }
            }
        }).start();
    }

    // Dialog to select an already paired device.
    private void createConnectionDialog(Integer id) {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED
                && isSdkOverS()) return;
        AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
        int titleId = id.equals(SharedData.INPUT_IDENTIFIER) ? R.string.input_alert_title : R.string.output_alert_title;
        adBuilder.setTitle(getString(titleId));
        // get bluetooth devices
        Map<String, BluetoothDevice> nameToDevice = possibleConnections.stream()
                .collect(Collectors.toMap(BluetoothDevice::getName, device -> device));
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.select_dialog_singlechoice);
        adapter.addAll(nameToDevice.keySet());

        adBuilder.setNegativeButton("Connect New", (dialogInterface, i) -> {
            dialogInterface.dismiss();
            // start the pairing process if they want to connect a new device.
            pairNewDevice(id);
        });
        adBuilder.setAdapter(adapter, (dialogInterface, i) -> {
            String name = adapter.getItem(i);
            // make sure it is bonded
            nameToDevice.get(name).createBond();
            updateModel(nameToDevice.get(name), SharedData.STATUS_BONDED, id);
        });
        adBuilder.show();
    }

    private final ActivityResultLauncher<IntentSenderRequest> startAssociationInput =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> associateDevice(result, SharedData.INPUT_IDENTIFIER));

    private final ActivityResultLauncher<IntentSenderRequest> startAssociationOutput =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> associateDevice(result, SharedData.OUTPUT_IDENTIFIER));

    // associate a device through the companion device manager. For previously unpaired devices.
    private void associateDevice(ActivityResult result, int identifier) {
        String toastText = identifier == SharedData.INPUT_IDENTIFIER ? "Input" : "Output";

        if (result.getResultCode() == Activity.RESULT_OK) {
            // Start a thread to connect the device
            new Thread(() -> {
                Intent data = result.getData();
                if (data == null) return;
                BluetoothDevice device = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
                if (device != null && (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_DENIED
                        || !isSdkOverS())) {
                    // check the device profile: make sure it is an AV object.
                    BluetoothClass btClass = device.getBluetoothClass();
                    // list of appropriate types (Audio, headphone-like)
                    if (!SharedData.AUDIO_CLASSES.contains(btClass.getDeviceClass())) return;

                    // Update the model with the status and device.
                    if (device.createBond()) {
                        updateModel(device, SharedData.STATUS_BONDING, identifier);
                    } else updateModel(null, SharedData.STATUS_BOND_FAILED, identifier);
                }
            }).start();
        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
            Toast.makeText(getContext(),
                    "No device was connected for " + toastText + ".", Toast.LENGTH_SHORT).show();
        }
    }

    // helper to return whether the SDK is over 31 (most recent public release).
    private boolean isSdkOverS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    // helper method to update the MainViewModel with the device and connection status.
    private void updateModel(BluetoothDevice device, Integer status, Integer id) {
        if (id == -1) {
            Log.e(TAG, "Could not update ViewModel. ID not found.");
            return;
        }
        mainViewModel.getDevice(id).postValue(device);
        mainViewModel.getConnectionStatus(id).postValue(status);

    }

    // Receiver for if bonds update in the pairing process.
    private final BroadcastReceiver bondReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = SharedData.UNKNOWN_IDENTIFIER;
            BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    || !isSdkOverS())
                id = bondedDevice.getName().equals(inputName) ? SharedData.INPUT_IDENTIFIER : SharedData.OUTPUT_IDENTIFIER;
            if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1) == BluetoothDevice.BOND_BONDED) {
                updateModel(bondedDevice, SharedData.STATUS_BONDED, id);
            }
            else updateModel(null, SharedData.STATUS_BOND_FAILED, id);
        }
    };

    // Receiver for device connecting.
    private final BroadcastReceiver connectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = SharedData.UNKNOWN_IDENTIFIER;
            Log.d(TAG, "Received connection");
            BluetoothDevice connectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    || !isSdkOverS())
                id = connectedDevice.getName().equals(inputName) ? SharedData.INPUT_IDENTIFIER : SharedData.OUTPUT_IDENTIFIER;
            if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                mainViewModel.getConnectionStatus(id).postValue(SharedData.STATUS_CONNECTED);
            } else mainViewModel.getConnectionStatus(id).postValue(SharedData.STATUS_CONNECT_FAILED);
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
