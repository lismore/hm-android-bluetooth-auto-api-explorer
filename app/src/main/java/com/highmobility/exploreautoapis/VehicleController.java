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

package com.highmobility.exploreautoapis;

import android.content.Intent;
import android.util.Log;

import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.CommandParseException;
import com.highmobility.autoapi.capability.FeatureCapability;
import com.highmobility.autoapi.incoming.Capabilities;
import com.highmobility.autoapi.incoming.Failure;
import com.highmobility.autoapi.incoming.IncomingCommand;
import com.highmobility.autoapi.incoming.LightsState;
import com.highmobility.autoapi.incoming.TrunkState;
import com.highmobility.exploreautoapis.remotecontrol.RemoteControlActivity;
import com.highmobility.exploreautoapis.storage.VehicleStatus;
import com.highmobility.hmkit.Broadcaster;
import com.highmobility.hmkit.BroadcasterListener;
import com.highmobility.hmkit.ConnectedLink;
import com.highmobility.hmkit.ConnectedLinkListener;
import com.highmobility.hmkit.Constants;
import com.highmobility.hmkit.Error.BroadcastError;
import com.highmobility.hmkit.Error.LinkError;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.Manager;

import java.util.Timer;
import java.util.TimerTask;

import static com.highmobility.exploreautoapis.VehicleActivity.REQUEST_CODE_REMOTE_CONTROL;
import static com.highmobility.exploreautoapis.VehicleActivity.TAG;
import static com.highmobility.exploreautoapis.remotecontrol.RemoteControlController
        .LINK_IDENTIFIER_MESSAGE;


/**
 * Created by root on 24/05/2017.
 */

public class VehicleController implements BroadcasterListener, ConnectedLinkListener {
    Timer initTimer;
    int retryCount;
    VehicleStatus vehicle;

    Manager manager;
    IVehicleView view;
    Command.Type sentCommand;

    TimerTask repeatTask;
    boolean initializing;

    Broadcaster broadcaster;
    ConnectedLink link;

    public VehicleController(IVehicleView view) {
        manager = Manager.getInstance();

        /*
         * Before using HMKit, you'll have to initialise the Manager singleton
         * with a snippet from the Platform Workspace:
         *
         *   1. Sign in to the workspace
         *   2. Go to the LEARN section and choose Android
         *   3. Follow the Getting Started instructions
         *
         * By the end of the tutorial you will have a snippet for initialisation,
         * that looks something like this:
         *
         *   Manager.getInstance().initialize(
         *     Base64String,
         *     Base64String,
         *     Base64String,
         *     getApplicationContext()
         *   );
         */

        // PASTE INIT SNIPPET HERE

        this.view = view;
        vehicle = VehicleStatus.getInstance();

        broadcaster = Manager.getInstance().getBroadcaster();
        broadcaster.setListener(this);

        startBroadcasting();
    }

    public void startRemoteControl() {
        Intent i = new Intent(view.getActivity(), RemoteControlActivity.class);
        i.putExtra(LINK_IDENTIFIER_MESSAGE, link.getSerial());
        view.getActivity().startActivityForResult(i, REQUEST_CODE_REMOTE_CONTROL);
    }

    public void onReturnFromRemoteControl() {
        if (link != null) {
            // take over the link listener, update the views.
            link.setListener(this);
            view.onVehicleStatusUpdate(vehicle);
        }
        // else link disconnected
    }

    public void onLockDoorsClicked() {
        view.showLoadingView(true);
        sentCommand = Command.DoorLocks.LOCK_UNLOCK;
        sendCommand(Command.DoorLocks.lockDoors(vehicle.doorsLocked == true ? false : true));
    }

    public void onLockTrunkClicked() {
        view.showLoadingView(true);
        sentCommand = Command.TrunkAccess.OPEN_CLOSE;
        TrunkState.LockState newLockState;
        TrunkState.Position newPosition;

        if (vehicle.trunkLockState == TrunkState.LockState.LOCKED) {
            newLockState = TrunkState.LockState.UNLOCKED;
            newPosition = TrunkState.Position.OPEN;
        } else {
            newLockState = TrunkState.LockState.LOCKED;
            newPosition = TrunkState.Position.CLOSED;
        }

        byte[] command = Command.TrunkAccess.setTrunkState(newLockState, newPosition);
        sendCommand(command);
    }

    public void onWindshieldDefrostingClicked() {
        view.showLoadingView(true);
        sentCommand = Command.Climate.START_STOP_DEFROSTING;
        byte[] command = Command.Climate.startDefrost(vehicle.isWindshieldDefrostingActive ?
                false : true);
        sendCommand(command);
    }

    public void onSunroofVisibilityClicked() {
        view.showLoadingView(true);
        sentCommand = Command.RooftopControl.CONTROL_ROOFTOP;

        float dimPercentage = vehicle.rooftopDimmingPercentage == 1f ? 0f : 1f;
        byte[] command = Command.RooftopControl.controlRooftop(dimPercentage, vehicle
                .rooftopOpenPercentage);
        sendCommand(command);
    }

    public void onSunroofOpenClicked() {
        view.showLoadingView(true);
        sentCommand = Command.RooftopControl.CONTROL_ROOFTOP;

        float openPercentage = vehicle.rooftopOpenPercentage == 0f ? 1f : 0f;
        byte[] command = Command.RooftopControl.controlRooftop(vehicle.rooftopDimmingPercentage,
                openPercentage);
        sendCommand(command);
    }

    public void onFrontExteriorLightClicked(LightsState.FrontExteriorLightState state) {
        view.showLoadingView(true);
        sentCommand = Command.Lights.CONTROL_LIGHTS;

        byte[] command = Command.Lights.controlLights(state,
                vehicle.isRearExteriorLightActive,
                vehicle.isInteriorLightActive,
                vehicle.lightsAmbientColor);

        sendCommand(command);
    }

    void sendCommand(byte[] command) {
        link.sendCommand(command, new Link.CommandCallback() {
            @Override
            public void onCommandSent() {
                onBleAckReceived();
            }

            @Override
            public void onCommandFailed(LinkError linkError) {
                onCommandError(1, linkError.getType() + " " + linkError.getMessage());
            }
        });
    }

    // Broadcaster listener

    @Override
    public void onStateChanged(Broadcaster.State state) {
        Log.d(TAG, "onStateChanged: set state");
        switch (broadcaster.getState()) {
            case IDLE:
                view.showBleInfoView(true, "Idle");

                if (state == Broadcaster.State.BLUETOOTH_UNAVAILABLE) {
                    startBroadcasting();
                }
                break;
            case BLUETOOTH_UNAVAILABLE:
                view.showBleInfoView(true, "Bluetooth N/A");
                break;
            case BROADCASTING:
                if (link == null) {
                    view.showBleInfoView(true, "Looking for links... " + manager.getBroadcaster()
                            .getName());
                }
                break;
        }
    }

    @Override
    public void onLinkReceived(ConnectedLink connectedLink) {
        if (link != null) {
            Log.d(TAG, "received new link, ignore");
            return;
        }

        link = connectedLink;
        link.setListener(this);
        view.showBleInfoView(true, "link: " + connectedLink.getState());
        Log.d(TAG, "onLinkReceived: ");
    }

    // Link listener

    @Override
    public void onLinkLost(ConnectedLink connectedLink) {
        if (connectedLink == link) {
            link.setListener(null);
            link = null;
            onStateChanged(broadcaster.getState());
        } else {
            Log.d(TAG, "unknown link lost");
        }
    }

    @Override
    public void onAuthorizationRequested(ConnectedLink connectedLink, ConnectedLinkListener
            .AuthorizationCallback callback) {
        callback.approve();
    }

    @Override
    public void onAuthorizationTimeout(ConnectedLink connectedLink) {
        view.onError(true, "authorization request timed out");
    }

    @Override
    public void onStateChanged(Link link, Link.State state) {
        Log.d(TAG, "link state changed " + link.getState());
        if (link == this.link) {
            if (link.getState() == Link.State.AUTHENTICATED) {
                vehicle.reset();
                view.showBleInfoView(false, "link: " + "authenticated");
                view.showLoadingView(true);
                initializing = true;
                sentCommand = Command.Capabilities.GET_CAPABILITIES;
                sendCommand(Command.Capabilities.getCapabilities());
            } else if (link.getState() == Link.State.CONNECTED) {
                view.showBleInfoView(true, "link: " + "connected");
            }
        }
    }

    @Override
    public void onCommandReceived(Link link, byte[] bytes) {
        onCommandReceived(bytes);
    }

    void startBroadcasting() {
        broadcaster.startBroadcasting(new Broadcaster.StartCallback() {
            @Override
            public void onBroadcastingStarted() {

            }

            @Override
            public void onBroadcastingFailed(BroadcastError broadcastError) {
                Log.e(TAG, "cant start broadcasting " + broadcastError.getType());
            }
        });
    }

    void onBleAckReceived() {
        if (initializing == true) {
            rescheduleInitTimer();
        }
    }

    void onCommandReceived(byte[] bytes) {
        try {
            IncomingCommand command = IncomingCommand.create(bytes);

            if (command.is(Command.Capabilities.CAPABILITIES)) {
                FeatureCapability[] capabilities = ((Capabilities) command).getCapabilities();
                if (capabilities == null || capabilities.length == 0) {
                    initializing = false;
                    cancelInitTimer();
                    view.onError(true, "No capabilities");
                    return;
                }
                rescheduleInitTimer();
                vehicle.onCapabilitiesReceived(((Capabilities) command).getCapabilities(),
                        true);
                view.onCapabilitiesUpdate(vehicle);

                // capabilities are asked only on initialization, follow it by get Lights
                if (vehicle.isCapable(Command.Identifier.LIGHTS)) {
                    sentCommand = Command.Lights.GET_LIGHTS_STATE;
                    sendCommand(Command.Lights.getLightsState());
                } else {
                    continueInitAfterGetLightsState(null);
                }
            } else if (command.is(Command.FailureMessage.FAILURE_MESSAGE)) {
                Failure failure = (Failure) command;
                Log.d(TAG, "failure " + failure.getFailureReason());

                if (sentCommand != null) {
                    if (initializing) {
                        if (sentCommand == Command.Lights.GET_LIGHTS_STATE && failure
                                .getFailureReason() == Failure.Reason.UNSUPPORTED_CAPABILITY) {
                            // never mind that there is no lights capa, continue with init
                            continueInitAfterGetLightsState(null);
                        } else {
                            // initialization failed
                            initializing = false;
                            cancelInitTimer();
                            view.onError(true, "Cannot get vehicle data: " + failure
                                    .getFailureReason());
                        }
                    } else {
                        view.showLoadingView(false);
                        view.onError(false, failure.getFailedType().getIdentifier() + " failed: "
                                + failure.getFailureReason());
                        sentCommand = null;
                    }
                }
            } else {
                if (initializing) {
                    if (command.is(Command.Lights.LIGHTS_STATE)) {
                        continueInitAfterGetLightsState((LightsState) command);
                        return;
                    } else if (command.is(Command.VehicleStatus.VEHICLE_STATUS)) {
                        cancelInitTimer();
                        initializing = false;
                    }
                }

                sentCommand = null;
                vehicle.update(command);
                view.onVehicleStatusUpdate(vehicle);
                view.showLoadingView(false);
            }
        } catch (CommandParseException e) {
            Log.d(TAG, "IncomingCommand parse exception ", e);
        }
    }

    void continueInitAfterGetLightsState(LightsState state) {
        rescheduleInitTimer();
        sentCommand = Command.VehicleStatus.GET_VEHICLE_STATUS;
        sendCommand(Command.VehicleStatus.getVehicleStatus());
        if (state != null) {
            vehicle.update(state);
            view.onVehicleStatusUpdate(vehicle);
        }
    }

    void cancelInitTimer() {
        if (initTimer != null) {
            Log.d(TAG, "cancelInitTimer:");
            repeatTask.cancel();
            initTimer.cancel();
            initTimer = null;
        }
    }

    TimerTask repeatTask() {
        return new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "run: app layer command wait timeout");
                failedToSendInitCommand("command timed out.");
            }
        };
    }

    void failedToSendInitCommand(final String message) {
        view.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                retryCount++;
                if (retryCount == 3) {
                    initializing = false;
                    view.onError(true, message);
                    retryCount = 0;
                    return;
                }

                Log.d(TAG, "init: try to send the command again " + (sentCommand != null ?
                        sentCommand.getIdentifier() : "null command"));
                if (sentCommand != null) {
                    // try to send command again
                    if (sentCommand == Command.VehicleStatus.GET_VEHICLE_STATUS) {
                        Log.d(TAG, "send vs");
                        sendCommand(Command.VehicleStatus.getVehicleStatus());
                    } else if (sentCommand == Command.Capabilities.GET_CAPABILITIES) {
                        Log.d(TAG, "send capa");
                        sendCommand(Command.Capabilities.getCapabilities());
                    } else if (sentCommand == Command.Lights.GET_LIGHTS_STATE) {
                        Log.d(TAG, "send lights");
                        sendCommand(Command.Lights.getLightsState());
                    }

                    if (initTimer != null) rescheduleInitTimer(); // no timer for telematics
                }
            }
        });
    }

    void rescheduleInitTimer() {
        cancelInitTimer();
        Log.d(TAG, "rescheduleInitTimer: ");
        initTimer = new Timer();
        if (repeatTask != null) repeatTask.cancel();
        repeatTask = repeatTask();
        initTimer.schedule(repeatTask, (long) ((Constants.commandTimeout + 5) * 1000));
    }

    void onCommandError(int errorCode, String message) {
        if (initializing == true &&
                (sentCommand == Command.VehicleStatus.GET_VEHICLE_STATUS
                        || sentCommand == Command.Capabilities.GET_CAPABILITIES
                        || sentCommand == Command.Lights.GET_LIGHTS_STATE)) {
            Log.d(TAG, "initialize, onCommandError: " + errorCode + " " + message);
            failedToSendInitCommand(message);
        } else if (sentCommand != null) {
            Log.d(TAG, "onCommandError: " + initializing);
            view.showLoadingView(false);
            view.onError(false, message);
            sentCommand = null;
        }
    }

    void onDestroy() {
        cancelInitTimer();
        manager.terminate();
        Log.d(TAG, "onDestroy: ");
        broadcaster = null;
    }
}
