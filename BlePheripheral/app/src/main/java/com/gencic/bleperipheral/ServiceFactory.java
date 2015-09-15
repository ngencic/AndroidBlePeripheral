package com.gencic.bleperipheral;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;

/**
 * Created by ngencic on 9/15/15.
 */
public class ServiceFactory {

    public static BluetoothGattService generateService() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(Constants.CHARACTERISTIC_UUID),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_BROADCAST | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        characteristic.setValue(77, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString(Constants.DESCRIPTOR_UUID),
                BluetoothGattDescriptor.PERMISSION_READ);
        // characteristic.addDescriptor(descriptor);
        BluetoothGattService service = new BluetoothGattService(UUID.fromString(Constants.SERVICE_UUID), SERVICE_TYPE_PRIMARY);
        service.addCharacteristic(characteristic);
        return service;
    }

}
