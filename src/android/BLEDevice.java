package com.cojj.cordova.ble.service;

import android.util.Log;
import android.bluetooth.BluetoothDevice;

public class BLEDevice {

    private byte THRASH_HOLD = -60;
    private short SMOOTHING_FACTOR = 2000;
    private byte MAX_DELTA = 2;

    private BluetoothDevice device;
    private int count = 0;
    private long lastSeen;
    private float average;
    private boolean unlocked;
    private byte thrashHoldCount;
    private boolean queueUnlock;
    BLEDevice(final BluetoothDevice device, int rssi){
        this.device = device;
        this.lastSeen = System.currentTimeMillis();
        this.average = rssi;
        this.unlocked = false;
        this.queueUnlock = false;
        this.thrashHoldCount = 0;
    }

    public void update(final BluetoothDevice device, int rssi){
        this.device = device;
        long now = System.currentTimeMillis();
        long tdif = now-this.lastSeen;
        this.lastSeen = now;

        // calculate thrashHoldCount
        if(rssi > THRASH_HOLD){
            this.thrashHoldCount++;
        }else{
            this.thrashHoldCount = 0;
        }

        if(device.getAddress().equals("D0:37:1A:D7:9F:BF")){
            Log.e("DEVICE", "BLE: "+tdif);
            Log.e("DEVICE", "BLE: "+this.average);
            Log.e("DEVICE", "BLE: "+this.thrashHoldCount);
        }
        
        //recalculatre average
        if(rssi > THRASH_HOLD+10){
            this.queueUnlock = true;
        } else if(tdif > SMOOTHING_FACTOR){
            this.average = rssi;
        }else{
            float deltaRSSI = rssi - this.average;

            if(deltaRSSI > MAX_DELTA){
                deltaRSSI = MAX_DELTA;
            }
            if(deltaRSSI < -MAX_DELTA){
                deltaRSSI = -MAX_DELTA;
            }

            this.average += (tdif * deltaRSSI)/SMOOTHING_FACTOR;
        }

        
    }

    public boolean check(){
        //return true if door needs to be opened

        //if(device.getAddress().equals("D0:37:1A:D7:9F:BF")) return true;
        if(
            this.queueUnlock ||
            this.average > THRASH_HOLD ||
            this.thrashHoldCount > 2
            ){
            Log.e("DEVICE", "BLE: OPEN"+this.queueUnlock+"/"+this.average+"/"+this.thrashHoldCount);
            this.queueUnlock = false;
            return true;
        } 
        
        return false;
    }

    /*
     * 
     */

    public BluetoothDevice getDevice(){
        return this.device;
    }

    public float getAverage(){
        return this.average;
    }


}