package com.cojj.cordova.ble.service;

import android.app.Service;
import android.app.Activity;
import android.content.Context;
import android.bluetooth.*;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.os.Handler;

import java.lang.Thread;
import java.lang.reflect.Method;

import java.util.List;
import java.util.UUID;

import org.apache.cordova.LOG;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PermissionHelper;

import java.util.Map;
import java.util.HashMap;

public class BLEService extends Service {
    private static final String TAG = "Service";
    private final IBinder mBinder = new LocalBinder();
    private final Map<String, BluetoothDevice> mDeviceMap = new HashMap();

    private CordovaInterface cordova;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothGatt gatt;

    private boolean connected = false;

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanData) {
            LOG.w("Service", "BLE SCAN:" + device.toString());
            BLEService.this.mDeviceMap.put(device.getAddress(), device);
        }
    };

      private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
  {
   
    public void onConnectionStateChange(BluetoothGatt gatt, int status1, int newState)
    {
        super.onConnectionStateChange(gatt, status1, newState);
     Log.e("SERVICE", "BLE: onConnectionStateChange "+status1+"/"+newState);   
       if (newState == BluetoothGatt.STATE_CONNECTED) {

            connected = true;
            gatt.discoverServices();

        }else if(status1 == 133){
            gatt.connect();
        }else if(newState == BluetoothGatt.STATE_DISCONNECTED){
            //gatt.disconnect();
            //gatt.close();
            BLEService.this.close();
            //BLEService.this.connect("D0:37:1A:D7:9F:BF");
            //BLEService.this.reconnect();
        }
    }
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
        Log.e("SERVICE", "BLE: onCharacteristicChanged");   
    }
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        Log.e("SERVICE", "BLE: onCharacteristicRead"); 
    }
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        Log.e("SERVICE", "BLE: onCharacteristicWrite");
    }
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
        Log.e("SERVICE", "BLE: onDescriptorRead");   
    }
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
        Log.e("SERVICE", "BLE: onDescriptorWrite");
    }
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status){
        Log.e("SERVICE", "BLE: onMtuChanged");
    }
    public void	onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){
        Log.e("SERVICE", "BLE: onReadRemoteRssi");
    }
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status){
        Log.e("SERVICE", "BLE: onReliableWriteCompleted");
    }
    public void onServicesDiscovered(BluetoothGatt gatt, int status){
        Log.e("SERVICE", "BLE: onServicesDiscovered");
    }
  };

    public void initialise(CordovaInterface cordova) {
        Log.e(TAG, "BLE: TEST");

        this.cordova = cordova;
        Activity activity = cordova.getActivity();
        bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        this.bluetoothAdapter.startLeScan(this.mLeScanCallback);

        Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "BLE: Stopping Scan");
                    BLEService.this.bluetoothAdapter.stopLeScan(BLEService.this.mLeScanCallback);
                    BLEService.this.connect("D0:37:1A:D7:9F:BF");
                }
            }, 5 * 1000);
    }
    
    public void connect(String macAddress){
        this.gatt = this.mDeviceMap.get(macAddress).connectGatt(this, false, this.mGattCallback);
    }

    public void reconnect(){
        this.gatt.disconnect();
        this.gatt.connect();
    }

    public void close(){
        //Log.e(TAG, "BLE: CURRENT STATE1: "+this.bluetoothManager.getConnectionState(this.mDeviceMap.get("D0:37:1A:D7:9F:BF"), 7));
        this.gatt.disconnect();
        this.refresh(this.gatt);
        this.gatt.close();
        //Log.e(TAG, "BLE: CURRENT STATE2: "+this.bluetoothManager.getConnectionState(this.mDeviceMap.get("D0:37:1A:D7:9F:BF"), 7));
        this.gatt = null;
        
        try{
            Thread.sleep(600);
        } catch(Exception e){

        }
        this.connect("D0:37:1A:D7:9F:BF");
    }
    public void addDevice(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.e(TAG, "BLE: ADD: " + device.getName() + " AND " + device.toString());
    }

    private boolean refresh(BluetoothGatt paramBluetoothGatt)
    {
    
      try
      {
        Method localMethod = paramBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
        if (localMethod != null)
        {
          boolean bool = ((Boolean)localMethod.invoke(paramBluetoothGatt, new Object[0])).booleanValue();
          return bool;
        }
      }
      catch (Exception e)
      {

      }
    
    return false;
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