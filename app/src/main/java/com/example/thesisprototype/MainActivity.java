package com.example.thesisprototype;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothManager;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.thesisprototype.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.thesisprototype.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter adapter;
    protected BluetoothDevice inputDevice;
    protected BluetoothDevice outputDevice;
    public int inputIdentifier = 0;
    public int outputIdentifier = 1;
    public CompanionDeviceManager deviceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.example.thesisprototype.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_inclass)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        BluetoothManager btManager = getSystemService(BluetoothManager.class);
        adapter = btManager.getAdapter();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bluetooth is a "normal" permission and is automatically granted to my application.
        // Bluetooth Connect and Bluetooth Scan needs to be granted
        requestPermission();
        // Check if bluetooth is enabled.
        if (!adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            getContent.launch(enableBtIntent);
        }
    }

    public List<String> getConnectedDeviceNames() {
        ArrayList<String> devices = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return devices;
        }
        if (inputDevice != null) devices.add(inputDevice.getName());
        if (outputDevice != null) devices.add(outputDevice.getName());
        return devices;
    }

    public BluetoothDevice getInputDevice() {
        return inputDevice;
    }

    public BluetoothDevice getOutputDevice() {
        return outputDevice;
    }

    public void manageInput() {
        if (inputDevice == null) connectDevices(inputIdentifier);
        else {
            deviceManager.disassociate(inputDevice.getAddress());
            inputDevice = null;
            HomeFragment.getInstance().updateButton(R.id.inputButton, false);
        }
    }

    public void manageOutput() {
        if (outputDevice == null) connectDevices(outputIdentifier);
        else {
            deviceManager.disassociate(outputDevice.getAddress());
            outputDevice = null;
            HomeFragment.getInstance().updateButton(R.id.outputButton, false);
        }
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
        deviceManager =
                (CompanionDeviceManager) getSystemService(Context.COMPANION_DEVICE_SERVICE);
        deviceManager.associate(pairingRequest, new CompanionDeviceManager.Callback() {

            @Override
            public void onDeviceFound(IntentSender chooserLauncher) {
                IntentSenderRequest request = new IntentSenderRequest.Builder(chooserLauncher).build();
                Log.i("MainActivity", "launching");
                if (type == inputIdentifier) startAssociationInput.launch(request);
                else startAssociationOutput.launch(request);
            }

            @Override
            public void onFailure(CharSequence error) {
                Toast.makeText(getApplicationContext(), R.string.no_devices, Toast.LENGTH_LONG).show();
            }
        }, null);
    }

    // Code taken from https://youtu.be/nkayHRT8D_w
    // Only for S and up
    private void requestPermission() {
        List<String> toRequest = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_DENIED) {
            toRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_DENIED) {
            toRequest.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE) ==
                PackageManager.PERMISSION_DENIED) {
            toRequest.add(Manifest.permission.REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE);
        }
        if (!toRequest.isEmpty()) {
            requestPermissionsLauncher.launch(toRequest.toArray(new String[0]));
        }
    }

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                if (result.get(Manifest.permission.BLUETOOTH_CONNECT) != null &&
                        result.get(Manifest.permission.BLUETOOTH_SCAN) != null &&
                        result.get(Manifest.permission.REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE) != null) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    Toast.makeText(getApplicationContext(), R.string.permission_denied,
                            Toast.LENGTH_LONG).show();
                }
            });


    private final ActivityResultLauncher<Intent> getContent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Notify that bluetooth was turned on
                    Toast.makeText(getApplicationContext(), R.string.bt_enabled,
                            Toast.LENGTH_SHORT).show();
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    // Notify that bluetooth is not on
                    Toast.makeText(getApplicationContext(), R.string.bt_disabled,
                            Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<IntentSenderRequest> startAssociationInput = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> associateDevice(result, inputIdentifier));

    private final ActivityResultLauncher<IntentSenderRequest> startAssociationOutput = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> associateDevice(result, outputIdentifier));

    private void associateDevice(ActivityResult result, int identifier) {
        int buttonId = identifier == inputIdentifier ? R.id.inputButton : R.id.outputButton;
        String toastText = identifier == inputIdentifier ? "Input" : "Output";

        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            if (data == null) return;
            BluetoothDevice device = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
            if (device != null && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_DENIED) {
                device.createBond();
                // assign to inputDevice or outputDevice
                if (identifier == inputIdentifier) inputDevice = device;
                else outputDevice = device;
                HomeFragment.getInstance().updateButton(buttonId, true);
                // start service if S or higher
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    deviceManager.startObservingDevicePresence(device.getAddress());
                }
                Toast.makeText(getApplicationContext(),
                        toastText + " device " + device.getName() + " connected",
                        Toast.LENGTH_SHORT).show();
            }
        }
        else if (result.getResultCode() == Activity.RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(),
                    "No device was connected for " + toastText + ".", Toast.LENGTH_SHORT).show();
        }
    }
}