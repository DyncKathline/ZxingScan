package org.dync.zxinglibrary.zxing;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import org.dync.zxinglibrary.zxing.camera.CameraManager;

/**
 * Detects ambient light and switches on the front light when very dark, and off
 * again when sufficiently light.
 *
 * @author Sean Owen
 * @author Nikolaus Huber
 */
final class AmbientLightManager implements SensorEventListener {

    private static final float TOO_DARK_LUX = 45.0f;
    private static final float BRIGHT_ENOUGH_LUX = 450.0f;

    private final Context context;
    private CameraManager cameraManager;
    private boolean isAuto;

    /**
     * 光传感器
     */
    private Sensor lightSensor;

    AmbientLightManager(Context context) {
        this.context = context;
    }

    public void setAuto(boolean isAuto) {
        this.isAuto = isAuto;
    }

    public void start(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
        if (isAuto) {
            SensorManager sensorManager = (SensorManager) context
                    .getSystemService(Context.SENSOR_SERVICE);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (lightSensor != null) {
                sensorManager.registerListener(this, lightSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    public void stop() {
        if (lightSensor != null) {
            SensorManager sensorManager = (SensorManager) context
                    .getSystemService(Context.SENSOR_SERVICE);
            sensorManager.unregisterListener(this);
            cameraManager = null;
            lightSensor = null;
        }
    }

    /**
     * 该方法会在周围环境改变后回调，然后根据设置好的临界值决定是否打开闪光灯
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float ambientLightLux = sensorEvent.values[0];
        if (cameraManager != null) {
            if(cameraManager.hasLight()) {
                if (ambientLightLux <= TOO_DARK_LUX) {
                    cameraManager.openLight();
                } else if (ambientLightLux >= BRIGHT_ENOUGH_LUX) {
                    cameraManager.offLight();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

}
