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
import java.util.Map;
import java.util.HashMap;

public class BLEService extends Service {
    private static final String TAG = "Service";
    private final IBinder mBinder = new LocalBinder();
    private final Map<String, BluetoothDevice> mDeviceMap = new HashMap();

    public void initialise() {
        Log.e(TAG, "BLE: TEST");
    }

    public void addDevice(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.e(TAG, "BLE: ADD: " + device.getName() + " AND " + device.toString());
    }

    public IBinder onBind(Intent paramIntent) {
        return this.mBinder;
    }

    //ANDROID LIFECYCLE
    public void onCreate() {
        Log.e(TAG, "BLE: Service onCreate");
        sendBroadcast(new Intent("BASDASDAS"));
    }

    public void onDestroy() {
        Log.e(TAG, "BLE: Service onDestroy");
    }

    public void onLowMemory() {
        Log.e(TAG, "BLE: Service onLowMemory");
    }

    public void onRebind(Intent intent) {
        Log.e(TAG, "BLE: Service onRebind");
    }

    public void onStart(Intent intent, int startId) {
        Log.e(TAG, "BLE: Service onStart");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "BLE: Service onStartCommand");
        return startId;
    }

    public void onTaskRemoved(Intent rootIntent) {
        Log.e(TAG, "BLE: Service onTraskRemoved");
    }

    public void onTrimMemory(int level) {
        Log.e(TAG, "BLE: Service onTrimMemory");
    }

    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "BLE: Service onUnbind");
        return true;
    }

    //END OF ANDROID LIFECYCLE

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        public BLEService getService() {
            return BLEService.this;
        }
    }
}