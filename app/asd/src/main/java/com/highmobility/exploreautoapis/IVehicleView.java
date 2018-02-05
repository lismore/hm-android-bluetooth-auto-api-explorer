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

import android.app.Activity;

import com.highmobility.exploreautoapis.storage.VehicleStatus;

/**
 * Created by root on 24/05/2017.
 */

public interface IVehicleView {
    void showLoadingView(boolean loading);
    void onCapabilitiesUpdate(VehicleStatus vehicle);
    void onVehicleStatusUpdate(VehicleStatus vehicle);
    void onError(boolean fatal, String message);
    void showBleInfoView(boolean show, String status);
    Activity getActivity();
}
