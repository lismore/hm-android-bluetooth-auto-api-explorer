package com.highmobility.exploreautoapis;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.highmobility.hmkit.Command.Capability.AvailableGetStateCapability;
import com.highmobility.hmkit.Command.Capability.ClimateCapability;
import com.highmobility.hmkit.Command.Capability.FeatureCapability;
import com.highmobility.hmkit.Command.Capability.LightsCapability;
import com.highmobility.hmkit.Command.Capability.RooftopCapability;
import com.highmobility.hmkit.Command.Capability.TrunkAccessCapability;

import com.highmobility.exploreautoapis.storage.VehicleStatus;
import com.highmobility.hmkit.Command.Incoming.LightsState;
import com.highmobility.hmkit.Command.Incoming.TrunkState;

import butterknife.BindView;
import butterknife.ButterKnife;
import info.hoang8f.android.segmented.SegmentedGroup;

import static com.highmobility.hmkit.Command.Command.Identifier.*;

public class VehicleExteriorFragment extends Fragment {
    @BindView(R.id.list_view) ListView listView;
    Adapter listViewAdapter;
    VehicleActivity parent;
    VehicleStatus vehicle;

    public static VehicleExteriorFragment newInstance(VehicleStatus vehicle, VehicleActivity vehicleActivity) {
        VehicleExteriorFragment fragment = new VehicleExteriorFragment();
        fragment.setVehicle(vehicle);
        fragment.parent = vehicleActivity;
        return fragment;
    }

    public VehicleExteriorFragment() {}

    public void setVehicle(VehicleStatus vehicle) {
        this.vehicle = vehicle;
    }

    public void onVehicleStatusUpdate() {
        if (listViewAdapter == null) {
            createAdapter();
        }
        else {
            listViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_vehicle_exterior, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    private void createAdapter() {
        listViewAdapter = new Adapter(getContext(), vehicle);

        listView.setAdapter(listViewAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (((FeatureCapability)listViewAdapter.getItem(position)).getIdentifier() == REMOTE_CONTROL) {
                    VehicleExteriorFragment.this.parent.onRemoteControlClicked();
                }
            }
        });
    }

    class Adapter extends BaseAdapter {
        private Context context;
        private LayoutInflater inflater;
        public VehicleStatus vehicle;

        public Adapter(Context context, VehicleStatus vehicle) {
            this.context = context;
            this.vehicle = vehicle;
            inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public int getCount() {
            return vehicle.exteriorCapabilities.length;
        }

        @Override
        public Object getItem(int position) {
            return vehicle.exteriorCapabilities[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            FeatureCapability capability = vehicle.exteriorCapabilities[position];

            if (capability.getIdentifier() == REMOTE_CONTROL) {
                convertView = inflater.inflate(R.layout.list_item_exterior_remote_control, null, false);
            }
            else if (capability.getIdentifier() == ROOFTOP) {
                RooftopCapability rooftopCapability = (RooftopCapability)capability;
                convertView = inflater.inflate(R.layout.list_item_two_exterior_items, null, false);

                ImageView image = (ImageView) convertView.findViewById(R.id.icon);
                ImageView imageTwo = (ImageView) convertView.findViewById(R.id.icon2);
                TextView title = (TextView)convertView.findViewById(R.id.title);
                TextView titleTwo = (TextView)convertView.findViewById(R.id.title2);

                image.setImageResource(R.drawable.ext_sunroofopaquehdpi);
                imageTwo.setImageResource(R.drawable.ext_sunroofopaquehdpi);
                title.setText("ROOFTOP DIMMING");
                titleTwo.setText("ROOFTOP OPENING");

                // dimming
                if (rooftopCapability.getDimmingCapability() == RooftopCapability.DimmingCapability.AVAILABLE
                        || rooftopCapability.getDimmingCapability() == RooftopCapability.DimmingCapability.ONLY_OPAQUE_OR_TRANSPARENT) {
                    // show segmnent
                    RadioButton firstButton = (RadioButton)convertView.findViewById(R.id.first_button);
                    RadioButton secondButton = (RadioButton) convertView.findViewById(R.id.second_button);

                    firstButton.setText("TRANSPARENT");
                    secondButton.setText("OPAQUE");
                    // TODO: if these fail the segment is still changed
                    if (vehicle.rooftopDimmingPercentage == 1f) {
                        secondButton.toggle();
                        firstButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                VehicleExteriorFragment.this.parent.controller.onSunroofVisibilityClicked();
                            }
                        });
                    }
                    else {
                        firstButton.toggle();
                        secondButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                VehicleExteriorFragment.this.parent.controller.onSunroofVisibilityClicked();
                            }
                        });
                    }
                }
                else {
                    TextView stateTitle = (TextView)convertView.findViewById(R.id.state_title);
                    SegmentedGroup segmentedGroup = (SegmentedGroup)convertView.findViewById(R.id.segment_group);
                    segmentedGroup.setVisibility(View.GONE);
                    stateTitle.setVisibility(View.VISIBLE);
                    stateTitle.setText(vehicle.rooftopDimmingPercentage == 1f ? "OPAQUE" : "TRANSPARENT");
                }

                // open/close
                if (rooftopCapability.getOpenCloseCapability() == RooftopCapability.OpenCloseCapability.AVAILABLE
                        || rooftopCapability.getOpenCloseCapability() == RooftopCapability.OpenCloseCapability.ONLY_FULLY_OPEN_OR_CLOSED) {
                    RadioButton firstButton = (RadioButton)convertView.findViewById(R.id.first_button2);
                    RadioButton secondButton = (RadioButton) convertView.findViewById(R.id.second_button2);

                    if (vehicle.rooftopOpenPercentage == 0f) {
                        firstButton.setText("OPEN");
                        secondButton.setText("CLOSED");

                        secondButton.toggle();
                        firstButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                VehicleExteriorFragment.this.parent.controller.onSunroofOpenClicked();
                            }
                        });
                    }
                    else {
                        firstButton.setText("OPEN");
                        secondButton.setText("CLOSE");
                        firstButton.toggle();
                        secondButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                VehicleExteriorFragment.this.parent.controller.onSunroofOpenClicked();
                            }
                        });
                    }
                }
                else {
                    TextView stateTitle = (TextView)convertView.findViewById(R.id.state_title2);
                    SegmentedGroup segmentedGroup = (SegmentedGroup)convertView.findViewById(R.id.segment_group2);
                    segmentedGroup.setVisibility(View.GONE);
                    stateTitle.setVisibility(View.VISIBLE);
                    stateTitle.setText(vehicle.rooftopOpenPercentage == 0f ? "CLOSED" : "OPEN");
                }
            }
            else if (capability.getIdentifier() == LIGHTS) {
                LightsCapability lightsCapability = (LightsCapability)capability;

                convertView = inflater.inflate(R.layout.list_item_exterior_item_three_segments, null, false);
                ImageView image = (ImageView) convertView.findViewById(R.id.icon);
                TextView title = (TextView)convertView.findViewById(R.id.title);
                image.setImageResource(R.drawable.ext_remotehdpi);
                title.setText("FRONT LIGHTS");

                if (lightsCapability.getExteriorLightsCapability() == AvailableGetStateCapability.Capability.AVAILABLE) {
                    RadioButton firstButton = (RadioButton)convertView.findViewById(R.id.first_button);
                    RadioButton secondButton = (RadioButton) convertView.findViewById(R.id.second_button);
                    RadioButton thirdButton = (RadioButton) convertView.findViewById(R.id.third_button);
                    firstButton.setText("INACTIVE");
                    secondButton.setText("ACTIVE");
                    thirdButton.setText("FULL BEAM");


                    if (vehicle.frontExteriorLightState == LightsState.FrontExteriorLightState.INACTIVE) {
                        firstButton.toggle();
                    }
                    else if (vehicle.frontExteriorLightState == LightsState.FrontExteriorLightState.ACTIVE) {
                        secondButton.toggle();
                    }
                    else if (vehicle.frontExteriorLightState == LightsState.FrontExteriorLightState.ACTIVE_WITH_FULL_BEAM) {
                        thirdButton.toggle();
                    }

                    firstButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            VehicleExteriorFragment.this.parent.controller.onFrontExteriorLightClicked(LightsState.FrontExteriorLightState.INACTIVE);
                        }
                    });

                    secondButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            VehicleExteriorFragment.this.parent.controller.onFrontExteriorLightClicked(LightsState.FrontExteriorLightState.ACTIVE);
                        }
                    });

                    thirdButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            VehicleExteriorFragment.this.parent.controller.onFrontExteriorLightClicked(LightsState.FrontExteriorLightState.ACTIVE_WITH_FULL_BEAM);
                        }
                    });
                }
                else {
                    TextView stateTitle = (TextView)convertView.findViewById(R.id.state_title);
                    SegmentedGroup segmentedGroup = (SegmentedGroup)convertView.findViewById(R.id.segment_group);
                    segmentedGroup.setVisibility(View.GONE);
                    stateTitle.setVisibility(View.VISIBLE);

                    if (vehicle.frontExteriorLightState == LightsState.FrontExteriorLightState.INACTIVE) {
                        stateTitle.setText("INACTIVE");
                    }
                    else if (vehicle.frontExteriorLightState == LightsState.FrontExteriorLightState.ACTIVE) {
                        stateTitle.setText("ACTIVE");
                    }
                    else if (vehicle.frontExteriorLightState == LightsState.FrontExteriorLightState.ACTIVE_WITH_FULL_BEAM) {
                        stateTitle.setText("FULL BEAM");
                    }
                }
            }
            else {
                convertView = inflater.inflate(R.layout.list_item_exterior_item, null, false);
                ImageView image = (ImageView) convertView.findViewById(R.id.icon);
                TextView title = (TextView)convertView.findViewById(R.id.title);
                RadioButton firstButton = (RadioButton)convertView.findViewById(R.id.first_button);
                RadioButton secondButton = (RadioButton) convertView.findViewById(R.id.second_button);
                TextView stateTitle = (TextView)convertView.findViewById(R.id.state_title);
                SegmentedGroup segmentedGroup = (SegmentedGroup)convertView.findViewById(R.id.segment_group);

                if (capability.getIdentifier() == CLIMATE) {
                    ClimateCapability climateCapability = (ClimateCapability)capability;
                    image.setImageResource(R.drawable.ext_defrostactivehdpi);
                    title.setText("WINDSHIELD HEATING");

                    if (climateCapability.getClimateCapability() == AvailableGetStateCapability.Capability.AVAILABLE) {
                        if (vehicle.isWindshieldDefrostingActive) {
                            secondButton.toggle();
                            secondButton.setText("ACTIVE");
                            firstButton.setText("INACTIVATE");
                            firstButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    VehicleExteriorFragment.this.parent.controller.onWindshieldDefrostingClicked();
                                }
                            });
                        }
                        else {
                            firstButton.toggle();
                            secondButton.setText("ACTIVATE");
                            firstButton.setText("INACTIVE");
                            secondButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    VehicleExteriorFragment.this.parent.controller.onWindshieldDefrostingClicked();
                                }
                            });
                        }
                    }
                    else {
                        segmentedGroup.setVisibility(View.GONE);
                        stateTitle.setVisibility(View.VISIBLE);
                        stateTitle.setText(vehicle.isWindshieldDefrostingActive == true ? "ACTIVE" : "INACTIVE");
                    }
                }
                else if (capability.getIdentifier() == DOOR_LOCKS) {
                    AvailableGetStateCapability doorLocksCapability = (AvailableGetStateCapability)capability;

                    image.setImageResource(R.drawable.ext_doorslockedhdpi);
                    title.setText("DOOR LOCKS");

                    if (doorLocksCapability.getCapability() == AvailableGetStateCapability.Capability.AVAILABLE) {

                        if (vehicle.doorsLocked == true) {
                            firstButton.setText("UNLOCK");
                            secondButton.setText("LOCKED");
                            secondButton.toggle();
                            firstButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    VehicleExteriorFragment.this.parent.controller.onLockDoorsClicked();
                                }
                            });
                        }
                        else {
                            firstButton.setText("UNLOCKED");
                            secondButton.setText("LOCK");
                            firstButton.toggle();
                            secondButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    VehicleExteriorFragment.this.parent.controller.onLockDoorsClicked();
                                }
                            });
                        }
                    }
                    else {
                        segmentedGroup.setVisibility(View.GONE);
                        stateTitle.setVisibility(View.VISIBLE);
                        stateTitle.setText(vehicle.doorsLocked == true ? "LOCKED" : "UNLOCKED");
                    }
                }
                else if (capability.getIdentifier() == TRUNK_ACCESS) {
                    TrunkAccessCapability trunkAccessCapability = (TrunkAccessCapability)capability;

                    image.setImageResource(R.drawable.ext_trunklockedhdpi);
                    title.setText("TRUNK LOCK");

                    if (trunkAccessCapability.getLockCapability() == TrunkAccessCapability.LockCapability.AVAILABLE
                            || trunkAccessCapability.getLockCapability() == TrunkAccessCapability.LockCapability.GET_STATE_UNLOCK_AVAILABLE) {
                        if (vehicle.trunkLockState == TrunkState.LockState.LOCKED) {
                            firstButton.setText("UNLOCK");
                            secondButton.setText("LOCKED");
                            secondButton.toggle();

                            firstButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    VehicleExteriorFragment.this.parent.controller.onLockTrunkClicked();
                                }
                            });
                        }
                        else {
                            firstButton.setText("UNLOCKED");
                            secondButton.setText("LOCK");
                            firstButton.toggle();
                            secondButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    VehicleExteriorFragment.this.parent.controller.onLockTrunkClicked();
                                }
                            });
                        }
                    }
                    else {
                        segmentedGroup.setVisibility(View.GONE);
                        stateTitle.setVisibility(View.VISIBLE);
                        stateTitle.setText(vehicle.trunkLockState == TrunkState.LockState.LOCKED ? "LOCKED" : "UNLOCKED");
                    }
                }
            }

            return convertView;
        }
    }
}
