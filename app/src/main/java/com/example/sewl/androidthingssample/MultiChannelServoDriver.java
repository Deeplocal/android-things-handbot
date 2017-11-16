package com.example.sewl.androidthingssample;

import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by mderrick on 10/10/17.
 */

public class MultiChannelServoDriver {

    private static final boolean ENABLE_SERVOS  = true;
    private static final byte I2C_ADDRESS       = 0x41;
    private static final byte PCA9685_MODE1     = 0x00;
    private static final byte RESET             = 0x00;
    private static final int PCA9685_PRESCALE   = 0xFE;
    private static final byte LED0_ON_L         = 0x06;
    private static final byte LED0_ON_H         = 0x07;
    private static final byte LED0_OFF_L        = 0x08;
    private static final byte LED0_OFF_H        = 0x09;
    private static final int SERVO_MAX_ANGLE    = 180;

    //  HS-5085 Servos
    // (900/(20ms/4096))
    private static final int SERVO_MIN          = 184;
    // (2100/(20ms/4096))
    private static final int SERVO_MAX          = 430;

    private I2cDevice i2cDevice;

    public void init() {
        PeripheralManagerService manager = new PeripheralManagerService();
        final List<String> deviceList = manager.getI2cBusList();
        if (deviceList.isEmpty()) {
            Log.i(TAG, "No I2C bus available on this device.");
        } else {
            openConnection(deviceList.get(0));
            writeReg(PCA9685_MODE1, RESET);
            setPWMFrequency(50.0d);
        }
    }

    public void setAngle(final int channel, int angle) {
        int off = (int) ((angle / (float) SERVO_MAX_ANGLE) * (SERVO_MAX - SERVO_MIN)) + SERVO_MIN;
        setPWM(channel, 0, off);
    }

    public void setPWM(int channel, int on, int off) {
        writeReg(LED0_ON_L + 4*channel, (byte) (on & 0xFF));
        writeReg(LED0_ON_H + 4*channel, (byte) ((on >> 8) & 0xFF));
        writeReg(LED0_OFF_L + 4*channel, (byte) (off & 0xFF));
        writeReg(LED0_OFF_H + 4*channel, (byte) ((off >> 8) & 0xFF));
    }

    private void setPWMFrequency(double frequency) {
        frequency *= 0.9;  // Correct for overshoot in the frequency setting (see issue #11).
        float prescaleval = 25000000.0f;
        prescaleval /= 4096;
        prescaleval /= frequency;
        prescaleval -= 1.0d;
        byte prescale = (byte) (((int) (prescaleval + 0.5)) & 0xFF);

        final byte oldmode = readReg(PCA9685_MODE1);
        byte newmode = (byte) ((oldmode & 0x7F) | 0x10);
        writeReg(PCA9685_MODE1, newmode); // go to sleep
        writeReg(PCA9685_PRESCALE, prescale); // set the prescaler
        writeReg(PCA9685_MODE1, oldmode);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                writeReg(PCA9685_MODE1, (byte) (oldmode | 0xa1));
                setAngle(0, 0);
            }
        }, 10);
    }

    private byte readReg(int reg) {
        if (i2cDevice != null) {
            try {
                return i2cDevice.readRegByte(reg);
            } catch (IOException e) {
                Log.e("PWMDriver", "Could not read reg");
            }
        }
        return 0;
    }

    private void writeReg(int reg, byte data) {
        if (ENABLE_SERVOS) {
            if (i2cDevice != null) try {
                i2cDevice.writeRegByte(reg, data);
            } catch (IOException e) {
                Log.e("PWMDriver", "Could not write reg: " + e);
            }
        }
    }

    private void openConnection(String busName) {
        try {
            PeripheralManagerService manager = new PeripheralManagerService();
            i2cDevice = manager.openI2cDevice(busName, I2C_ADDRESS);
        } catch (IOException e) {
            Log.w(TAG, "Unable to access I2C device", e);
        }
    }

    public void shutDown() {
        if (i2cDevice != null) {
            try {
                i2cDevice.close();
                i2cDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close I2C device", e);
            }
        }
    }
}
