/*
 * HMKit Auto API - Auto API Parser for Java
 * Copyright (C) 2018 High-Mobility <licensing@high-mobility.com>
 *
 * This file is part of HMKit Auto API.
 *
 * HMKit Auto API is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HMKit Auto API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HMKit Auto API.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.highmobility.exploreautoapis.remotecontrol;

import android.os.CountDownTimer;

import java.util.Calendar;

public class TapToControlCommandConverter {
    private static final String TAG = "CommandConverter";
    int latestIndex = -1;

    Long lastCommandTime = Long.MAX_VALUE;
    int tapsInSameIndex = 0;
    int speed;
    int angle;

    CountDownTimer timeoutTimer;
    ITapToControlCommandConverter listener;

    TapToControlCommandConverter(ITapToControlCommandConverter listener) {
        this.listener = listener;
    }

    public int getSpeed() {
        return speed;
    }

    public int getAngle() {
        return angle;
    }

    public void onMoveButtonClicked(int index) {
        if (Calendar.getInstance().getTimeInMillis() - lastCommandTime > 300) tapsInSameIndex = 0;

        lastCommandTime = Calendar.getInstance().getTimeInMillis();

        if (latestIndex == index) {
            tapsInSameIndex++;
        }
        else {
            tapsInSameIndex = 0;
        }

        if ((latestIndex < 3 && index > 2) || (latestIndex > 2 && index < 3)) {
            // opposite direction tap
            speed = 0;
            listener.onSpeedChanged(0);
        }

        // observe the angle change
        switch (index) {
            case 0:
            case 3: {
                if (angle != 100) {
                    angle = 100;
                    listener.onAngleChanged(angle);
                }
                break;
            }
            case 1:
            case 4: {
                if (angle != 0) {
                    angle = 0;
                    listener.onAngleChanged(angle);
                }
                break;
            }
            case 2:
            case 5: {
                if (angle != -100) {
                    angle = -100;
                    listener.onAngleChanged(angle);
                }
                break;
            }
        }

        latestIndex = index;

        // if 3 taps have been in the same direction after correct interval, change the speed
        if (tapsInSameIndex > 2) {
            switch (latestIndex) {
                case 0:
                case 1:
                case 2:
                    if (speed != 1) listener.onSpeedChanged(1);
                    speed = 1;
                    break;
                case 3:
                case 4:
                case 5:
                    if (speed != -1) listener.onSpeedChanged(-1);
                    speed = -1;
                    break;
            }
        }

        if (timeoutTimer != null) timeoutTimer.cancel();

        // reset the speed and tap count if no tap after 310ms
        timeoutTimer = new CountDownTimer(310, 300) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (Calendar.getInstance().getTimeInMillis() - lastCommandTime > 300) {
                    tapsInSameIndex = 0;
                    if (speed != 0) {
                        speed = 0;
                        listener.onSpeedChanged(0);
                    }
                }
            }
        }.start();
    }

    public void onStopClicked() {
        tapsInSameIndex = 0;
        lastCommandTime = Long.MAX_VALUE;
        speed = 0;
        if (timeoutTimer != null) timeoutTimer.cancel();
        listener.onSpeedChanged(0);
    }
}