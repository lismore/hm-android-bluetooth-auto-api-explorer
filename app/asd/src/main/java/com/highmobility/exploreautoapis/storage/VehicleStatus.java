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

package com.highmobility.exploreautoapis.storage;

import android.util.Log;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.capability.AvailableCapability;
import com.highmobility.autoapi.capability.AvailableGetStateCapability;
import com.highmobility.autoapi.capability.ClimateCapability;
import com.highmobility.autoapi.capability.FeatureCapability;
import com.highmobility.autoapi.capability.LightsCapability;
import com.highmobility.autoapi.capability.RooftopCapability;
import com.highmobility.autoapi.capability.TrunkAccessCapability;
import com.highmobility.autoapi.incoming.ChargeState;
import com.highmobility.autoapi.incoming.ClimateState;
import com.highmobility.autoapi.incoming.IncomingCommand;
import com.highmobility.autoapi.incoming.LightsState;
import com.highmobility.autoapi.incoming.LockState;
import com.highmobility.autoapi.incoming.TrunkState;
import com.highmobility.autoapi.vehiclestatus.Charging;
import com.highmobility.autoapi.vehiclestatus.Climate;
import com.highmobility.autoapi.vehiclestatus.DoorLocks;
import com.highmobility.autoapi.vehiclestatus.FeatureState;
import com.highmobility.autoapi.vehiclestatus.Lights;
import com.highmobility.autoapi.vehiclestatus.RooftopState;
import com.highmobility.autoapi.vehiclestatus.TrunkAccess;

import java.util.ArrayList;

import static com.highmobility.autoapi.Command.Identifier.CHARGING;
import static com.highmobility.autoapi.Command.Identifier.CLIMATE;
import static com.highmobility.autoapi.Command.Identifier.DOOR_LOCKS;
import static com.highmobility.autoapi.Command.Identifier.LIGHTS;
import static com.highmobility.autoapi.Command.Identifier.REMOTE_CONTROL;
import static com.highmobility.autoapi.Command.Identifier.ROOFTOP;
import static com.highmobility.autoapi.Command.Identifier.TRUNK_ACCESS;
import static com.highmobility.autoapi.Command.Identifier.VEHICLE_LOCATION;
import static com.highmobility.exploreautoapis.VehicleActivity.TAG;

/**
 * Created by root on 30/05/2017.
 */

public class VehicleStatus {
    public float insideTemperature;
    public float batteryPercentage;

    public boolean doorsLocked;
    public TrunkState.LockState trunkLockState;
    public TrunkState.Position trunkLockPosition;
    public boolean isWindshieldDefrostingActive;
    public float rooftopDimmingPercentage;
    public float rooftopOpenPercentage;
    public LightsState.FrontExteriorLightState frontExteriorLightState;
    public boolean isRearExteriorLightActive;
    public boolean isInteriorLightActive;
    public int[] lightsAmbientColor;

    public FeatureCapability[] exteriorCapabilities = new FeatureCapability[0];
    public FeatureCapability[] overviewCapabilities = new FeatureCapability[0];

    static VehicleStatus instance;
    public static VehicleStatus getInstance() {
        if (instance == null) {
            instance = new VehicleStatus();
        }

        return instance;
    }

    public void reset() {
        insideTemperature = 0f;
        batteryPercentage = 0f;
        doorsLocked = false;
        trunkLockState = null;
        trunkLockPosition = null;
        isWindshieldDefrostingActive = false;
        rooftopDimmingPercentage = 0f;
        rooftopOpenPercentage = 0f;
        exteriorCapabilities = null;
        overviewCapabilities = null;
        frontExteriorLightState = LightsState.FrontExteriorLightState.INACTIVE;
        isRearExteriorLightActive = false;
        isInteriorLightActive = false;
        lightsAmbientColor = null;
    }

    public void update(IncomingCommand command) {
        if (command.is(Command.VehicleStatus.VEHICLE_STATUS)) {
            com.highmobility.autoapi.incoming.VehicleStatus status = (com.highmobility.autoapi.incoming.VehicleStatus)command;
            FeatureState[] featureStates = status.getFeatureStates();

            if (featureStates == null) {
                Log.e(TAG, "update: null featureStates");
                return;
            }

            for (int i = 0; i < featureStates.length; i++) {
                FeatureState featureState = featureStates[i];
                if (featureState.getIdentifier() == CLIMATE) {
                    Climate state = (Climate) featureState;
                    insideTemperature = state.getInsideTemperature();
                    isWindshieldDefrostingActive = state.isDefrostingActive();
                }
                else if (featureState.getIdentifier() == CHARGING) {
                    Charging state = (Charging) featureState;
                    batteryPercentage = state.getBatteryLevel();
                }
                else if (featureState.getIdentifier() == DOOR_LOCKS) {
                    DoorLocks state = (DoorLocks) featureState;
                    doorsLocked = state.isLocked();
                }
                else if (featureState.getIdentifier() == TRUNK_ACCESS) {
                    TrunkAccess state = (TrunkAccess) featureState;
                    trunkLockState = state.getLockState();
                    trunkLockPosition = state.getPosition();
                }
                else if (featureState.getIdentifier() == ROOFTOP) {
                    RooftopState state = (RooftopState) featureState;
                    rooftopDimmingPercentage = state.getDimmingPercentage();
                    rooftopOpenPercentage = state.getOpenPercentage();
                }
                else if (featureState.getIdentifier() == LIGHTS) {
                    Lights lights = (Lights) featureState;
                    frontExteriorLightState = lights.getFrontExteriorLightState();
                    isRearExteriorLightActive = lights.isRearExteriorLightActive();
                    isInteriorLightActive = lights.isInteriorLightActive();
                }
            }
        }
        else if (command.is(Command.VehicleLocation.VEHICLE_LOCATION)) {
            // nothing
        }
        else if (command.is(Command.Climate.CLIMATE_STATE)) {
            ClimateState state = (ClimateState)command;
            isWindshieldDefrostingActive = state.isDefrostingActive();
            insideTemperature = state.getInsideTemperature();
        }
        else if (command.is(Command.RemoteControl.CONTROL_COMMAND)) {

        }
        else if (command.is(Command.DoorLocks.LOCK_STATE)) {
            LockState state = (LockState) command;
            doorsLocked = state.isLocked();
        }
        else if (command.is(Command.TrunkAccess.TRUNK_STATE)) {
            TrunkState state = (TrunkState)command;

            trunkLockState = state.getLockState();
            trunkLockPosition = state.getPosition();
        }
        else if (command.is(Command.RooftopControl.ROOFTOP_STATE)) {
            com.highmobility.autoapi.incoming.RooftopState state = (com.highmobility.autoapi.incoming.RooftopState)command;
            rooftopDimmingPercentage = state.getDimmingPercentage();
            rooftopOpenPercentage = state.getOpenPercentage();
        }
        else if (command.is(Command.Charging.CHARGE_STATE)) {
            ChargeState state = (ChargeState)command;
            batteryPercentage = state.getBatteryLevel();
        }
        else if (command.is(Command.Lights.LIGHTS_STATE)) {
            LightsState state = (LightsState)command;
            frontExteriorLightState = state.getFrontExteriorLightState();
            isRearExteriorLightActive = state.isRearExteriorLightActive();
            isInteriorLightActive = state.isInteriorLightActive();
            lightsAmbientColor = state.getAmbientColor();
        }
    }

    // if usingBle is false, remote control capability is not added.
    public void onCapabilitiesReceived(FeatureCapability[] capabilities, boolean usingBle) {
        ArrayList<FeatureCapability> exteriorCapabilities = new ArrayList<>();
        ArrayList<FeatureCapability> overviewCapabilities = new ArrayList<>();

        for (int i = 0; i < capabilities.length; i++) {
            FeatureCapability capability = capabilities[i];
            Command.Identifier feature = capability.getIdentifier();

            if (feature == ROOFTOP) {
                RooftopCapability rooftopCapability = (RooftopCapability)capability;
                if (rooftopCapability.getDimmingCapability() != RooftopCapability.DimmingCapability.UNAVAILABLE) {
                    overviewCapabilities.add(capability);
                    exteriorCapabilities.add(capability);
                }
            }
            else if (feature == REMOTE_CONTROL && usingBle) {
                AvailableCapability remoteControlCapability = (AvailableCapability)capability;
                if (remoteControlCapability.getCapability() == AvailableCapability.Capability.AVAILABLE) {
                    overviewCapabilities.add(capability);
                    exteriorCapabilities.add(capability);
                }
            }
            else if (feature == CLIMATE) {
                ClimateCapability climateCapability = (ClimateCapability) capability;
                if (climateCapability.getClimateCapability() != AvailableGetStateCapability.Capability.UNAVAILABLE) {
                    overviewCapabilities.add(capability);
                    exteriorCapabilities.add(capability);
                }
            }
            else if (feature == DOOR_LOCKS) {
                AvailableGetStateCapability doorLocksCapability = (AvailableGetStateCapability)capability;
                if (doorLocksCapability.getCapability() != AvailableGetStateCapability.Capability.UNAVAILABLE) {
                    overviewCapabilities.add(capability);
                    exteriorCapabilities.add(capability);
                }
            }
            else if (feature == TRUNK_ACCESS) {
                TrunkAccessCapability trunkAccessCapability = (TrunkAccessCapability)capability;
                if (trunkAccessCapability.getLockCapability() != TrunkAccessCapability.LockCapability.UNAVAILABLE) {
                    overviewCapabilities.add(capability);
                    exteriorCapabilities.add(capability);
                }
            }
            else if (feature == VEHICLE_LOCATION) {
                AvailableCapability locationCapability = (AvailableCapability)capability;
                if (locationCapability.getCapability() == AvailableCapability.Capability.AVAILABLE) {
                    overviewCapabilities.add(capability);
                }
            }
            else if (feature == CHARGING) {
                AvailableGetStateCapability chargingCapability = (AvailableGetStateCapability)capability;
                if (chargingCapability.getCapability() != AvailableGetStateCapability.Capability.UNAVAILABLE) {
                    overviewCapabilities.add(capability);
                }
            }
            else if (feature == LIGHTS) {
                LightsCapability lightsCapability = (LightsCapability)capability;
                if (lightsCapability.getExteriorLightsCapability() != AvailableGetStateCapability.Capability.UNAVAILABLE) {
                    exteriorCapabilities.add(lightsCapability);
                }
            }
        }

        this.exteriorCapabilities = exteriorCapabilities.toArray(new FeatureCapability[exteriorCapabilities.size()]);
        this.overviewCapabilities = overviewCapabilities.toArray(new FeatureCapability[overviewCapabilities.size()]);
    }

    public boolean isCapable(Command.Identifier feature) {
        for (FeatureCapability capability : exteriorCapabilities) {
            if (capability.getIdentifier() == feature) return true;
        }

        for (FeatureCapability capability : overviewCapabilities) {
            if (capability.getIdentifier() == feature) return true;
        }

        return false;
    }
}