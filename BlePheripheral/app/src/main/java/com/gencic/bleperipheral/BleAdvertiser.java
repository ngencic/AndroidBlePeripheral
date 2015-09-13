package com.gencic.bleperipheral;

import android.bluetooth.BluetoothDevice;
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
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;

import java.util.UUID;

import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;

/**
 * Created by gencha on 13.9.15..
 */
public class BleAdvertiser {

    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattserver;
    private BluetoothDevice mConnectedDevice;
    private ILogger mLogger;
    private Context mContext;

    public BleAdvertiser(Context context, BluetoothManager bluetoothManager) {
        mBluetoothManager = bluetoothManager;
        mContext = context;
    }

    public void setLogger(ILogger logger) {
        mLogger = logger;
    }

    public void startAdvertising() {
        if (mBluetoothManager.getAdapter().isEnabled()) {
            if (mBluetoothManager.getAdapter().isMultipleAdvertisementSupported()) {
                mGattserver = mBluetoothManager.openGattServer(mContext, new BluetoothGattServerCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                        super.onConnectionStateChange(device, status, newState);
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            if (mLogger != null) {
                                mLogger.log("Device connected: " + device.getAddress());
                            }
                            mConnectedDevice = device;
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            if (mLogger != null) {
                                mLogger.log("Device disconnected: " + device.getAddress());
                            }
                            mConnectedDevice = null;
                        }
                    }

                    @Override
                    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                        super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                    }

                    @Override
                    public void onServiceAdded(int status, BluetoothGattService service) {
                        super.onServiceAdded(status, service);
                        BleAdvertiser.this.onServiceAdded();
                    }

                    @Override
                    public void onNotificationSent(BluetoothDevice device, int status) {
                        super.onNotificationSent(device, status);
                    }
                });

                BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(Constants.CHARACTERISTIC_UUID),
                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_BROADCAST | BluetoothGattCharacteristic.PROPERTY_WRITE,
                        BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
                characteristic.setValue(77, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString(Constants.DESCRIPTOR_UUID),
                        BluetoothGattDescriptor.PERMISSION_READ);
                // characteristic.addDescriptor(descriptor);
                BluetoothGattService service = new BluetoothGattService(UUID.fromString(Constants.SERVICE_UUID), SERVICE_TYPE_PRIMARY);
                service.addCharacteristic(characteristic);
                mGattserver.addService(service);
            } else {
                mLogger.log("Central mode not supported by the device!");
            }
        } else {
            mLogger.log("Bluetooth is disabled!");
        }
    }

    private void onServiceAdded(){
        final BluetoothLeAdvertiser bluetoothLeAdvertiser = mBluetoothManager.getAdapter().getBluetoothLeAdvertiser();
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(false); //necessity to fit in 31 byte advertisement
        //dataBuilder.setManufacturerData(0, advertisingBytes);
        dataBuilder.addServiceUuid(new ParcelUuid(UUID.fromString(Constants.SERVICE_UUID)));
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
    }

    public void sendMessage(String msg) {
        if (mConnectedDevice != null) {
            mLogger.log("Sending meesage to the client");
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(Constants.CHARACTERISTIC_UUID), BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ);
            characteristic.setValue(msg);
            mGattserver.notifyCharacteristicChanged(mConnectedDevice, characteristic, false);
        }
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            if (mLogger != null) {
                mLogger.log("Advertising started successfully");
            }
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            if (mLogger != null) {
                mLogger.log("Advertising failed error code = " + errorCode);
            }
        }
    };

}
