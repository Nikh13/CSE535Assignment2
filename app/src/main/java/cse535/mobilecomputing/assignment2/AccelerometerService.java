package cse535.mobilecomputing.assignment2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import db.DatabaseHelper;

/**
 * Created by Nikhil on 2/27/16.
 */
public class AccelerometerService extends Service implements SensorEventListener {

    private SensorManager accelManager;
    private Sensor senseAccel;
    private DatabaseHelper dbHelper;

    @Override
    public void onCreate(){
        accelManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senseAccel = accelManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelManager.registerListener(this, senseAccel, 100000);
        dbHelper = DatabaseHelper.getInstance(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            int timestamp = DatabaseHelper.getTimeStamp();
            Record record = new Record(timestamp,sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2]);
//            Log.i("Service","Sensor values "+timestamp+" "+record.xVal+" "+record.yVal+" "+record.zVal);
            dbHelper.addRecord(record);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        accelManager.unregisterListener(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
