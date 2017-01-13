package com.cojj.cordova.ble.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import java.util.List;
import java.util.UUID;

public class BLEService extends Service {

    private final IBinder mBinder = new LocalBinder();

    public IBinder onBind(Intent paramIntent) {
        return this.mBinder;
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        public BLEService getService() {
            return BLEService.this;
        }
    }
}