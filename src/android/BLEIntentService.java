package com.cojj.cordova.ble.service;

import android.app.IntentService;
import android.app.Activity;
import android.content.Context;
import android.bluetooth.*;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.os.Handler;
import android.os.PowerManager;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.os.Vibrator;
import android.os.PowerManager;
import java.security.SecureRandom;

import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.BluetoothLeScanner;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import com.cojj.cordova.ble.service.ShakeDetector.OnShakeListener;

import java.lang.Thread;
import java.lang.reflect.Method;

import java.util.List;
import java.util.UUID;

import org.apache.cordova.LOG;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;

import java.nio.charset.StandardCharsets;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;

import org.json.JSONObject;

public class BLEIntentService extends IntentService {
    
    /*
    private static final int BLE_INIT = 0;
    private static final int BLE_SCANING = 1;
    private static final int BLE_CONNECTING = 2;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 3;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 4;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 5;
    */
    
    private static final String TAG = "BLEIntentService";
    private final IBinder mBinder = new LocalBinder();
    private final Map<String, BLEDevice> mDeviceMap = new HashMap();
    private ConcurrentLinkedQueue<BLECommand> commandQueue = new ConcurrentLinkedQueue<BLECommand>();
    public final static UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUIDHelper.uuidFromString("2902");
    public static final String SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String TX_CHARACTERISTIC = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String RX_CHARACTERISTIC = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String[] KEYS = {"5a4f664b5638783646796f5752614d4d", "4EcafeHHNvY3H2Eh"};

    private CordovaInterface cordova;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothGatt gatt;
    private BluetoothGattService service;
    private BluetoothLeScanner mLEScanner;
    private Vibrator vibrator;

    private boolean connected = false;
    private boolean connecting = false;
    private boolean bleProcessing;
    private String key;
    private String credential;
    private byte scanSensitivity;

    private int step = 0;
    private byte[] rxBuffer = new byte[32];
    private String peripheralToConnect = "D0:37:1A:D7:9F:BF";
    private byte[] rndB = new byte[16];
    private byte[] rndBShifted = new byte[16];
    private byte[] rndA = new byte[16];
    private byte[] rndAShifted = new byte[16];
    private byte[] newIV = new byte[16];

	// The following are used for the shake detection
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private ShakeDetector mShakeDetector;

    //power
    private PowerManager.WakeLock wakeLock;

    private BluetoothGattCharacteristic txCharacteristics;

    //private String peripheralToConnect = "ED:45:A0:53:90:10";
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanData) {
            Log.w("Service", "BLE SCAN:" + device.toString() +" / "+rssi);
            BLEIntentService.this.scanResult(device, rssi, scanData);
        }
    };

    private final ScanCallback mScanCallback = new ScanCallback(){
        public void onScanResult(int callbackType, ScanResult result){
                Log.w("Service", "BLE SCAN:" + result.getDevice().toString() +" / "+result.getRssi());
                BLEIntentService.this.scanResult(result.getDevice(), result.getRssi(), null);
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
        }else if(status1 == 133 && BLEIntentService.this.connected == false){
            gatt.connect(); //handle weird error
        }else if(newState == BluetoothGatt.STATE_DISCONNECTED){
            BLEIntentService.this.rinseNrepeat();
        }
    }

    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
        Log.e("SERVICE", "BLE: onCharcacteristicChanged "+BLEIntentService.this.bytesToHex(characteristic.getValue())); 

        if(BLEIntentService.this.step == 0){
            BLEIntentService.this.firstSend(characteristic.getValue());
        }else if(BLEIntentService.this.step == 1 || BLEIntentService.this.step == 2){
            byte[] temp = characteristic.getValue();
            for(int i=0;i<temp.length;i++){
               BLEIntentService.this.rxBuffer[i+((step-1)*16)] = temp[i];
            }
            if(BLEIntentService.this.step == 2){
                 BLEIntentService.this.print();
            }
        }
        BLEIntentService.this.step++;
    }
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        Log.e("SERVICE", "BLE: onCharacteristicRead"); 
    }
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        Log.e("SERVICE", "BLE: onCharacteristicWrite");
        BLEIntentService.this.commandCompleted();
    }
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
        Log.e("SERVICE", "BLE: onDescriptorRead");   
    }
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
        Log.e("SERVICE", "BLE: onDescriptorWrite");
        BLEIntentService.this.commandCompleted();
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
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BLECommand command = new BLECommand(UUIDHelper.uuidFromString(BLEIntentService.SERVICE_UUID), UUIDHelper.uuidFromString(BLEIntentService.RX_CHARACTERISTIC), BLECommand.REGISTER_NOTIFY);
            BLEIntentService.this.queueCommand(command);
        } else {
            Log.e(TAG, "Service discovery failed. status = " + status);
            BLEIntentService.this.rinseNrepeat();
        }

    }
  };

  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive (Context context, Intent intent) {
        String action = intent.getAction();
        
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
        if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON){
            BLEIntentService.this.initialiseTwo();
        }
        if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF){
            //BLUETOOTH TURNED OFF

            //TO DO: CLEAR SERVICE MEMORY
            BLEIntentService.this.reset();
        }
        }
    }

        };

    public BLEIntentService() {
        super(BLEIntentService.class.getName());
    }

    public void initialise(CordovaInterface cordova) {
        Log.e(TAG, "BLE: TEST");

        this.cordova = cordova;
        Activity activity = cordova.getActivity();
        bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);

         // ShakeDetector initialization
		mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mShakeDetector = new ShakeDetector();
		mShakeDetector.setOnShakeListener(new OnShakeListener() {
			@Override
			public void onShake(int count) {
				/*
				 * The following method, "handleShakeEvent(count):" is a stub //
				 * method you would use to setup whatever you want done once the
				 * device has been shook.
				 */
				//handleShakeEvent(count);
                Log.e("SHAKE", "BLE: SHAKE");
                BLEIntentService.this.connectClosest();
			}
		});
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);

        if (Build.VERSION.SDK_INT >= 21) {
            this.mLEScanner = this.bluetoothAdapter.getBluetoothLeScanner();
        }

        if (bluetoothAdapter != null) {
            registerReceiver(this.mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            if (bluetoothAdapter.isEnabled()) {
                this.scan();
            }
        } else{
              // Device does not support Bluetooth
        }
    }

    public void initialiseTwo() {

        bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        //keepAwake
        this.keepAwake();

        // ShakeDetector initialization
		mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mShakeDetector = new ShakeDetector();
		mShakeDetector.setOnShakeListener(new OnShakeListener() {
			@Override
			public void onShake(int count) {
				/*
				 * The following method, "handleShakeEvent(count):" is a stub //
				 * method you would use to setup whatever you want done once the
				 * device has been shook.
				 */
				//handleShakeEvent(count);
                Log.e("SHAKE", "BLE: SHAKE");
                BLEIntentService.this.connectClosest();
			}
		});
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);

        if (Build.VERSION.SDK_INT >= 21) {
        this.mLEScanner = this.bluetoothAdapter.getBluetoothLeScanner();
        }

        if (bluetoothAdapter != null) {
            registerReceiver(this.mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            
            if (bluetoothAdapter.isEnabled()) {
                this.scan();
            }


        } else{
              // Device does not support Bluetooth
        }
    }

    public void scan(){
        if(Build.VERSION.SDK_INT >= 21){
            ScanSettings settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();   
            this.mLEScanner.startScan(new ArrayList<ScanFilter>(), settings, mScanCallback);

        }else{
            this.bluetoothAdapter.startLeScan(this.mLeScanCallback);
        }
    }

    public void stopScan(){
          if(Build.VERSION.SDK_INT >= 21){    
                this.mLEScanner.stopScan(mScanCallback);
            }else{
                this.bluetoothAdapter.stopLeScan(this.mLeScanCallback);
            }
    }

    public void scanResult(final BluetoothDevice device, int rssi, byte[] scanData){
        if(device.getName() == null || !device.getName().equals("TSEC")){
             Log.e(TAG, "BLE: SCAN NOT TSEC " + device.getName());
             return;
        }
        BLEDevice tempDevice = this.mDeviceMap.get(device.getAddress());
        if(tempDevice == null){
            tempDevice = new BLEDevice(device, rssi, this.scanSensitivity);
            this.mDeviceMap.put(device.getAddress(), tempDevice);
        }else{
            tempDevice.update(device, rssi);
        }

        if(tempDevice.check()){
            this.stopScan();
            this.connect(device.getAddress());
        }
    }

    public void connectClosest(){
        //check if empty
        String champion = "";
        float champRssi = -90.4124524F;
        for (Map.Entry<String, BLEDevice> entry : this.mDeviceMap.entrySet())
        {   
            final BLEDevice tempDevice = entry.getValue();
            if(champRssi < tempDevice.getAverage()){
                champion = tempDevice.getDevice().getAddress();
                champRssi = tempDevice.getAverage();
            }
        }

        Log.e(TAG, "BLE: CLOEST: "+ champion + champRssi);

        if(champRssi != -90.4124524F){
            //this means a candidate is found
            this.stopScan();
            this.connect(champion);

        }

        /*
        this.mDeviceMap.forEach((k,v) -> {
            Log.e(TAG, "BLE: CLOEST: "+v.getDevice().getAddress());
        });
        */
    }
    
    public void connect(String macAddress){
        if(this.connecting == true) return;

        this.connecting = true;
        this.vibrator.vibrate(500);
        this.gatt = this.mDeviceMap.get(macAddress).getDevice().connectGatt(this, false, this.mGattCallback);
    }

    public void reconnect(){
        this.gatt.disconnect();
        this.gatt.connect();
    }

    public void close(){
        if(this.gatt == null) return;
        this.refresh(this.gatt);
        this.gatt.disconnect();
        this.gatt.close();
        this.gatt = null;
    }

    public void sleep(int a){
        try{
            //5 max scan per 30 seconds
            Thread.sleep(a);
        } catch(Exception e){

        }
    }

    public void reset(){
        this.step = 0;
        this.mDeviceMap.clear();
        this.connecting = false;
        this.connected = false;
        this.key = "";
    }

    public void rinseNrepeat(){
            this.close();
            this.reset();
            this.sleep(4000);
            this.scan();
    }

    public void firstSend(byte[] received){

        if(received[0] != -86){
            this.gatt.disconnect();
            return;
        }
        
        this.key = this.KEYS[received[1]];
    
        
        this.txCharacteristics = service.getCharacteristic(UUIDHelper.uuidFromString(this.TX_CHARACTERISTIC));

        String nonce = this.randomString(16); //String nonce = "6OfKV8x6FyoWRaMa";
        this.rndB = nonce.getBytes();
        
        byte[] encrypted = encrypt(this.hexStringToByteArray(this.key),this.hexStringToByteArray("00000000000000000000000000000000"),nonce);

        this.newIV = encrypted;
    
        // ADD 0 at the front of the packet
        byte[] toSend = new byte[17];
        toSend[0] = 0;
        for(int i=0;i<encrypted.length;i++){
            toSend[i+1] = encrypted[i];
        }

        BLECommand command = new BLECommand(null, null, null, toSend, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        this.queueCommand(command);
        
    }

    public void print(){
        //Log.e(TAG, "BLE: RXBUFFER1: "+this.bytesToHex(this.rxBuffer));
        byte[] rxDecoded = decrypt(this.hexStringToByteArray(this.key),this.newIV,this.rxBuffer);
        //Log.e(TAG, "BLE: DECRYPTED: "+ this.bytesToHex(rxDecoded));

        //TO DO: check rndB

        //get rndA from 2nd half of the packet
        for(int i=0;i<16;i++){
            this.rndA[i] = rxDecoded[i];
            if(i == 0){
               this.rndAShifted[15] = rxDecoded[0];
            }else{
               this.rndAShifted[i-1] = rxDecoded[i];
            }
        }
        
        
        for(int i=16;i<32;i++){
            this.rndBShifted[i-16] = rxDecoded[i];
            this.newIV[i-16] = this.rxBuffer[i]; //newIV is shifted encrypted rndB from peripheral device
        }

        
        Log.e(TAG, "BLE: NEWIV: "+this.bytesToHex(this.newIV));
        Log.e(TAG, "BLE: RNDB: "+this.bytesToHex(this.rndB));
        Log.e(TAG, "BLE: RNDBSHIFTED: "+this.bytesToHex(this.rndBShifted));
        Log.e(TAG, "BLE: RNDA: "+this.bytesToHex(this.rndA));
        Log.e(TAG, "BLE: RNDASHIFTED: "+this.bytesToHex(this.rndAShifted));

        
        //send shifted rndA back
        BLECommand command = new BLECommand(null, null, null, encrypt(this.hexStringToByteArray(this.key), this.newIV, this.rndAShifted), BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        this.queueCommand(command);

        //generate session key
        byte[] sessionKey = new byte[16];
        for (int k = 0; k < 4; k++) {
            sessionKey[k] = this.rndA[k];
            sessionKey[k + 4] = this.rndB[k];
            sessionKey[k + 8] = this.rndA[k + 12];
            sessionKey[k + 12] = this.rndB[k + 12];
        }

        //send 1st half of credential
        //dummy cred: 0A925A8E4FD1F5894A7C8EDC23DA72DF.9FFF6E01B38EF5797C095D78F2E782B7
        this.newIV = encrypt(sessionKey, this.hexStringToByteArray("00000000000000000000000000000000"), this.hexStringToByteArray(this.credential.substring(0, 32)));
        command = new BLECommand(null, null, null, newIV, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        this.queueCommand(command);

        //send 2nd half of credential
        command = new BLECommand(null, null, null, encrypt(sessionKey, this.newIV, this.hexStringToByteArray(this.credential.substring(32, 64))), BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        this.queueCommand(command);

        //Log.e(TAG, "BLE: Cred1: "+ this.credential.substring(0, 32));
        //Log.e(TAG, "BLE: Cred2: "+ this.credential.substring(32, 64));
        
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

    /**
    * QUEUE
    */
    // add a new command to the queue
    private void queueCommand(BLECommand command) {
        Log.e(TAG, "BLE: Queuing Command " + command);
        commandQueue.add(command);
        /*
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        command.getCallbackContext().sendPluginResult(result);
        */
        if (!bleProcessing) {
            processCommands();
        }
    }

      // process the queue
    private void processCommands() {
        Log.e(TAG, "BLE: Processing Commands");

        if (bleProcessing) {
            return;
        }

        BLECommand command = commandQueue.poll();
        if (command != null) {
            if (command.getType() == BLECommand.READ) {
                LOG.d(TAG, "Read " + command.getCharacteristicUUID());
                bleProcessing = true;
                //readCharacteristic(command.getCallbackContext(), command.getServiceUUID(), command.getCharacteristicUUID());
            } else if (command.getType() == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) {
                LOG.d(TAG, "Write " + command.getCharacteristicUUID());
                bleProcessing = true;
                //writeCharacteristic(command.getCallbackContext(), command.getServiceUUID(), command.getCharacteristicUUID(), command.getData(), command.getType());
            } else if (command.getType() == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
                Log.e(TAG,"BLE: Write No Response " + bytesToHex(command.getData()));
                bleProcessing = true;
                txCharacteristics.setValue(command.getData());
                txCharacteristics.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                gatt.writeCharacteristic(txCharacteristics);
                //writeCharacteristic(command.getCallbackContext(), command.getServiceUUID(), command.getCharacteristicUUID(), command.getData(), command.getType());
            } else if (command.getType() == BLECommand.REGISTER_NOTIFY) {
                Log.w(TAG, "BLE: Register Notify " + command.getCharacteristicUUID());
                bleProcessing = true;

                service = gatt.getService(UUIDHelper.uuidFromString(this.SERVICE_UUID));
                
                //Keep getting null error here
                if(service == null){
                    this.rinseNrepeat();
                    return;
                }

                BluetoothGattCharacteristic characteristic = service.getCharacteristic(command.getCharacteristicUUID());

                Log.w(TAG, "BLE: SET CHAR NOTIFICATION: " + this.gatt.setCharacteristicNotification(characteristic, true));

                 // Why doesn't setCharacteristicNotification write the descriptor?
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_UUID);
                
                    // prefer notify over indicate
                    if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    } else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    } else {
                        
                    }

                    gatt.writeDescriptor(descriptor);
                        
                    
                

                //registerNotifyCallback(command.getCallbackContext(), command.getServiceUUID(), command.getCharacteristicUUID());

                //bleProcessing = false;
            } else if (command.getType() == BLECommand.REMOVE_NOTIFY) {
                LOG.d(TAG, "Remove Notify " + command.getCharacteristicUUID());
                bleProcessing = true;
                //removeNotifyCallback(command.getCallbackContext(), command.getServiceUUID(), command.getCharacteristicUUID());
            } else if (command.getType() == BLECommand.READ_RSSI) {
                LOG.d(TAG, "Read RSSI");
                bleProcessing = true;
                //readRSSI(command.getCallbackContext());
            } else {
                // this shouldn't happen
                throw new RuntimeException("Unexpected BLE Command type " + command.getType());
            }
        } else {
            LOG.d(TAG, "Command Queue is empty.");
        }

    }

    private void commandCompleted() {
        Log.e(TAG, "BLE: Processing Complete");
        bleProcessing = false;
        processCommands();
    }


        /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    private void keepAwake() {

        PowerManager powerMgr = (PowerManager)
                getSystemService(POWER_SERVICE);

        wakeLock = powerMgr.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "BackgroundMode");

        wakeLock.acquire();
        
    }

    public static byte[] encrypt(byte[] key, byte[] initVector, String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());

            return encrypted;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
        public static byte[] encrypt(byte[] key, byte[] initVector, byte[] value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value);

            return encrypted;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static byte[] decrypt(byte[] key, byte[] initVector, byte[] encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(encrypted);

            return original;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for ( int j = 0; j < bytes.length; j++ ) {
        int v = bytes[j] & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                             + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    static final String AB = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*()_+=-<>?:\"{}|\\][;/.,]";
    static SecureRandom rnd = new SecureRandom();

    String randomString( int len ){
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ ) 
        sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }

    //ANDROID LIFECYCLE
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e(TAG, "BLE: onHandleIntent");

        this.credential = intent.getStringExtra("credential");
        this.scanSensitivity = intent.getByteExtra("scanSensitivity", (byte) -59);
        initialiseTwo();
    }


    //END OF ANDROID LIFECYCLE

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        public BLEIntentService getService() {
            return BLEIntentService.this;
        }
    }
}