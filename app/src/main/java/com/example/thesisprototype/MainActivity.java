package com.example.thesisprototype;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.thesisprototype.models.MainViewModel;
import com.example.thesisprototype.utils.SharedData;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.PaintKt;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.thesisprototype.databinding.ActivityMainBinding;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter adapter;
    protected BluetoothDevice inputDevice;
    private BluetoothHeadset source;
    private BluetoothSocket sourceSocket;
    private int inputStatus;
    protected BluetoothDevice outputDevice;
    private BluetoothA2dp sink;
    private BluetoothSocket sinkSocket;
    private int outputStatus;
    private TextView devicesText;
    private boolean permissionsGranted;
    private MainViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.example.thesisprototype.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        model = new ViewModelProvider(this).get(MainViewModel.class);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_inclass)
                .build();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        adapter = getSystemService(BluetoothManager.class).getAdapter();

        devicesText = findViewById(R.id.connected_devices);
        model.getDevice(SharedData.INPUT_IDENTIFIER).observe(this, btDevice -> {
            inputDevice = btDevice;
            devicesText.setText(buildDeviceString());
        });
        model.getConnectionStatus(SharedData.INPUT_IDENTIFIER).observe(this, status -> {
            inputStatus = status;
            devicesText.setText(buildDeviceString());
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check for permissions, based on what API it is.
        requestPermission();
        // Check if bluetooth is enabled (if all permissions were granted).
        if (permissionsGranted && !adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            getContent.launch(enableBtIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(btReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    protected void onPause() {
        unregisterReceiver(btReceiver);
        super.onPause();
    }

    private CharSequence buildDeviceString() {
        // get the connected devices and display names
        StringBuilder devicesString = new StringBuilder();
        // either requires the bluetooth connect permission or for SDK to be R or lower.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            String iStatus = getString(getStatusString(inputStatus));
            if (inputDevice != null)
                devicesString.append("\nInput: ").append(inputDevice.getName()).append(", ").append(iStatus);
            else if (inputStatus != 0) devicesString.append("\nInput not connected, ").append(iStatus);
        }
        return devicesString;
    }

    private int getStatusString(Integer status) {
        switch(status) {
            case SharedData.STATUS_BONDED:
                return R.string.bonded;
            case SharedData.STATUS_CONNECTED:
                return R.string.connected;
            case SharedData.STATUS_CONNECTING:
            case SharedData.STATUS_BONDING:
                return R.string.updating;
            case SharedData.STATUS_BOND_FAILED:
            case SharedData.STATUS_CONNECT_FAILED:
                return R.string.failed;
            default:
                return R.string.unknown;
        }
    }

//    private void connectSinkDevice(BluetoothDevice device) {
//        new Thread(() -> {
//            // make sure it is bonded
//            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) !=
//                    PackageManager.PERMISSION_GRANTED
//                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return;
//            if (device.getBondState() != BluetoothDevice.BOND_BONDED) return;
//            // get the uuid for the device
//            try {
//                sinkSocket = device.createInsecureRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());
//            } catch (IOException e) {
//                Log.e("MainActivity", e.getMessage());
//                model.getConnectionStatus(SharedData.INPUT_IDENTIFIER).postValue(SharedData.STATUS_CONNECT_FAILED);
//            }
//        }).start();
//    }
//
//    private void connectSourceDevice(BluetoothDevice device) {
//        new Thread(() -> {
//            // make sure it is bonded
//            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) !=
//                    PackageManager.PERMISSION_GRANTED
//                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return;
//            if (device.getBondState() != BluetoothDevice.BOND_BONDED) return;
//            // get the uuid for the device
//            try {
//                sourceSocket = device.createInsecureRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());
//            } catch (IOException e) {
//                Log.e("MainActivity", e.getMessage());
//                model.getConnectionStatus(SharedData.OUTPUT_IDENTIFIER).postValue(SharedData.STATUS_CONNECT_FAILED);
//            }
//        }).start();
//    }

    // Code taken from https://youtu.be/nkayHRT8D_w
    // SDKs S and T use different permissions from R and below. Specify!
    private void requestPermission() {
        List<String> toRequest = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                    == PackageManager.PERMISSION_DENIED) {
                toRequest.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
        }
        else {
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
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_DENIED)
            toRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED) toRequest.add(Manifest.permission.RECORD_AUDIO);
        if (!toRequest.isEmpty()) {
            requestPermissionsLauncher.launch(toRequest.toArray(new String[0]));
        }
    }

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                // if below S, check admin permissions.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                        result.get(Manifest.permission.BLUETOOTH) != null &&
                        result.get(Manifest.permission.BLUETOOTH_ADMIN) != null) {
                    permissionsGranted = true;
                }
                // if S or above, check the new bt permissions and the service permission.
                else if (result.get(Manifest.permission.BLUETOOTH_CONNECT) != null &&
                        result.get(Manifest.permission.BLUETOOTH_SCAN) != null &&
                        result.get(Manifest.permission.REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE) != null) {
                    // can now run the app fully!
                    permissionsGranted = true;
                } else {
                    Toast.makeText(getApplicationContext(), R.string.permission_denied,
                            Toast.LENGTH_LONG).show();
                    permissionsGranted = false;
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

    // Watch if the bluetooth gets turned off!
    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    Toast.makeText(getApplicationContext(), "Bluetooth has been disconnected", Toast.LENGTH_LONG).show();
                    model.getConnectionStatus(SharedData.INPUT_IDENTIFIER).postValue(SharedData.STATUS_UNKNOWN);
                    model.getConnectionStatus(SharedData.OUTPUT_IDENTIFIER).postValue(SharedData.STATUS_UNKNOWN);
                    model.getDevice(SharedData.INPUT_IDENTIFIER).postValue(null);
                    model.getDevice(SharedData.INPUT_IDENTIFIER).postValue(null);
                }
            }
        }
    };

//    // ServiceListeners for the A2DP and Headset objects
//    private BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
//        @Override
//        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
//            if (i == BluetoothProfile.HEADSET) {
//                source = (BluetoothHeadset) bluetoothProfile;
//                model.getConnectionStatus(SharedData.INPUT_IDENTIFIER).postValue(SharedData.STATUS_CONNECTING);
//            } else if (i == BluetoothProfile.A2DP) {
//                model.getConnectionStatus(SharedData.OUTPUT_IDENTIFIER).postValue(SharedData.STATUS_CONNECTING);
//                sink = (BluetoothA2dp) bluetoothProfile;
//            }
//        }
//
//        @Override
//        public void onServiceDisconnected(int i) {
//            if (i == BluetoothProfile.HEADSET) {
//                model.getConnectionStatus(SharedData.INPUT_IDENTIFIER).postValue(null);
//                model.getDevice(SharedData.INPUT_IDENTIFIER).postValue(null);
//                source = null;
//            }
//            else if (i == BluetoothProfile.A2DP) {
//                model.getConnectionStatus(SharedData.OUTPUT_IDENTIFIER).postValue(null);
//                model.getDevice(SharedData.OUTPUT_IDENTIFIER).postValue(null);
//                sink = null;
//            }
//        }
//    };
}