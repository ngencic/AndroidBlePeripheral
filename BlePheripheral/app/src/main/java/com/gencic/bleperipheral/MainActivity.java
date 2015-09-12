package com.gencic.bleperipheral;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;

public class MainActivity extends Activity implements OnClickListener {

    static final String SERVICE_UUID = "11111111-0000-1000-8000-00805F9B34FB";
    static final String CHARACTERISTIC_UUID = "22222222-0000-1000-8000-00805F9B34FB";

    private TextView mTextViewLog;
    private BluetoothManager mBluetoothManager;
    private HashMap<String, BluetoothDevice> mDiscoveredDevices;
    private BluetoothGattServer mGattserver;
    private BluetoothDevice mConnectedDevice;
    private BluetoothLeScanner mScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextViewLog = (TextView) findViewById(R.id.text_view_log);
        findViewById(R.id.button_advertise).setOnClickListener(this);
        findViewById(R.id.button_scan).setOnClickListener(this);
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mDiscoveredDevices = new HashMap<>();
    }

    private void startAdvertising() {
        if (mBluetoothManager.getAdapter().isMultipleAdvertisementSupported()) {
            mGattserver = mBluetoothManager.openGattServer(this, new BluetoothGattServerCallback() {
                @Override
                public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                    super.onConnectionStateChange(device, status, newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        log("Device connected: " + device.getAddress());
                        mConnectedDevice = device;
                        sendMessage();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        log("Device disconnected: " + device.getAddress());
                        mConnectedDevice = null;
                    }

                }
            });
            BluetoothGattService service = new BluetoothGattService(UUID.fromString(SERVICE_UUID), SERVICE_TYPE_PRIMARY);
            service.addCharacteristic(new BluetoothGattCharacteristic(UUID.fromString(CHARACTERISTIC_UUID), 0, 0));
            mGattserver.addService(service);

            final BluetoothLeAdvertiser bluetoothLeAdvertiser = mBluetoothManager.getAdapter().getBluetoothLeAdvertiser();
            AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
            dataBuilder.setIncludeTxPowerLevel(false); //necessity to fit in 31 byte advertisement
            //dataBuilder.setManufacturerData(0, advertisingBytes);
            dataBuilder.addServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_UUID)));
            //dataBuilder.setServiceData(pUUID, new byte[]{});

            AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
            settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
            settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
            settingsBuilder.setConnectable(true);

            bluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), mAdvertiseCallback);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothLeAdvertiser.stopAdvertising(new AdvertiseCallback() {
                        @Override
                        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                            super.onStartSuccess(settingsInEffect);
                        }
                    });
                }
            }, 5000);
        } else {
            log("Central mode not supported by the device!");
        }
    }

    private void startScanning() {
        mDiscoveredDevices.clear();
        ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
        filterBuilder.setServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_UUID)));
        ScanFilter filter = filterBuilder.build();
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(filter);
        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
        ScanSettings settings = settingsBuilder.build();
        mScanner = mBluetoothManager.getAdapter().getBluetoothLeScanner();
        mScanner.startScan(filters, settings, mScanCallback);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDiscoveredDevices.clear();
                mScanner.stopScan(mScanCallback);
            }
        }, 5000);
    }

    private void connectToGattServer(BluetoothDevice device){
        device.connectGatt(MainActivity.this, false, new BluetoothGattCallback() {
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                for (int i = 0; i < gatt.getServices().size(); i++) {
                    log("Service discovered " + gatt.getServices().get(i).getUuid());
                    if (gatt.getServices().get(i).getUuid().toString().equals(SERVICE_UUID)) {
                        /*TODO BluetoothGattCharacteristic characteristic = gatt.getService(UUID.fromString(SERVICE_UUID)).getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));
                        gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);*/
                    }
                }
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    log("Connected to Gatt Server");
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    log("Disconnected from Gatt Server");
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                log("Characteristic changed");
            }
        });
    }

    private void sendMessage(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        log("Sending meesage to the client");
                        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(CHARACTERISTIC_UUID), 0, 0);
                        characteristic.setValue("Hello World");
                        mGattserver.notifyCharacteristicChanged(mConnectedDevice, characteristic, false);
                    }
                }, 1500);
            }
        });
    }

    private void log(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextViewLog.setText(msg + "\n" + mTextViewLog.getText());
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_advertise:
                startAdvertising();
                break;
            case R.id.button_scan:
                startScanning();
                break;
        }
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            log("Advertising started successfully");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            log("Advertising failed error code = " + errorCode);
        }
    };

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result.getDevice() != null) {
                mScanner.stopScan(this);
                if (!mDiscoveredDevices.containsKey(result.getDevice().getAddress())) {
                    mDiscoveredDevices.put(result.getDevice().getAddress(), result.getDevice());
                    log("Discovered device: " + result.getDevice());
                    connectToGattServer(result.getDevice());
                }
            }
        }
    };

}
