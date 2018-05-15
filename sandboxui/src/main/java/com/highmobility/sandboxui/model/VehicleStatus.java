package com.highmobility.sandboxui.model;

import android.util.Log;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.ChargeState;
import com.highmobility.autoapi.ClimateState;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.CommandResolver;
import com.highmobility.autoapi.LightsState;
import com.highmobility.autoapi.LockState;
import com.highmobility.autoapi.RooftopState;
import com.highmobility.autoapi.TrunkState;
import com.highmobility.autoapi.Type;
import com.highmobility.autoapi.property.CapabilityProperty;
import com.highmobility.autoapi.property.FrontExteriorLightState;
import com.highmobility.autoapi.property.TrunkLockState;
import com.highmobility.autoapi.property.TrunkPosition;
import com.highmobility.hmkit.Link;
import com.highmobility.utils.Bytes;

import java.util.Arrays;

/**
 * This class will keep the state of the vehicle according to commands received.
 */
public class VehicleStatus {
    public static final String TAG = "VehicleStatus";
    // means SDK cannot be terminated
    public static byte[] vehicleConnectedWithBle;

    public static boolean isVehicleConnectedWithBle(byte[] serial) {
        return vehicleConnectedWithBle != null && Arrays.equals(serial, vehicleConnectedWithBle)
                == true;
    }

    public Float insideTemperature;
    public Float batteryPercentage;

    public Boolean doorsLocked;
    public TrunkLockState trunkLockState;
    public TrunkPosition trunkLockPosition;
    public Boolean isWindshieldDefrostingActive;
    public Float rooftopDimmingPercentage;
    public Float rooftopOpenPercentage;
    public FrontExteriorLightState frontExteriorLightState;
    // unused
    public Boolean isRearExteriorLightActive;
    public Boolean isInteriorLightActive;
    public int[] lightsAmbientColor;

    private Capabilities capabilities;

    public CapabilityProperty[] getCapabilities() {
        return capabilities.getCapabilities();
    }

    public VehicleStatus() {
        // fake capas and VS
        update(CommandResolver.resolve(Bytes.bytesFromHex
                ("001001010003004900010009002300010203040506010009002400010203040506010004003300010100050020000102010003004101010005003500010201000300510001000300290201000600260001020301000400480001010004005400010100050036000102010004003400010100040037000101000500310001020100050038000102010005005800010201000500470001020100040057000101000700270001020304010005002500010201000400560001010003004400010005004600010201000500210001020100050028000102010004003000010100030043000100040055000101000700590001020304010005004500010201000400420001")));
        update(CommandResolver.resolve(Bytes.bytesFromHex
                ("00110101001131484D37354331323148314145333634300200010103001F506F727363686520436F6E63657074205374756479204D697373696F6E20450400104D79204F746865722056656869636C65050009422D484D2D3632383707000207E308000009000200000A0001040B00010499004200230101000100020002001E03000150050004BF19999A070004000000000800016409000200000A0004000000000B0001000C0001000D000900120314130D3901A499004800240101000441B800000200044190000003000441B8000004000441B0000006000100070001000800010009000441B80000050001000A000F600800080008000800080008000800990054003301010003000BB80300020001090001000A000B00401333334220000000000A000B01401333334220000000000A000B02401333334220000000000A000B03401333334220000000000D000200000E0002000099001B00200101000300000101000301000101000302000101000303000199000700410101000100990007003501010001009900070026010100010099000700480101000100990011005401010004461C4000020004447A00009900150036010100010002000100030001000400030000FF99000E00340101000201900200030075309900360031010100084252167241569C87020025416C6578616E646572706C61747A2C203130313738204265726C696E2C204765726D616E799900070058010100010099002300470101000100020000030000040008120314130D3901A4050008120314130D3901A499005F005701010005000000000001000501000000000100050200000000010005030000000002000100030001000400010005000100060004000000000700040000000008000100090001000A000200000A000201000B0001000C0001000D00010099000C00270101000102020002006499000B00250101000164020001009900210056010100030000000100030100000100030200000100030300000100030400009900070046010100010099000B002101010001000200010199000700280101000100990015003001010008425210E741561BEA0200044252147D9900070055010100010099001200590101000100020001000300000400010099001C0045010100020000010002010001000202000100020300010002040099000B0042010100010002000100")));
    }

    public void update(Command command) {
        if (command instanceof com.highmobility.autoapi.VehicleStatus) {
            com.highmobility.autoapi.VehicleStatus status = (com.highmobility.autoapi
                    .VehicleStatus) command;

            Command[] states = status.getStates();

            if (states == null) {
                Log.e(TAG, "update: null featureStates");
                return;
            }

            for (int i = 0; i < states.length; i++) {
                Command subCommand = states[i];
                if (subCommand instanceof ClimateState) {
                    ClimateState state = (ClimateState) subCommand;
                    insideTemperature = state.getInsideTemperature();
                    isWindshieldDefrostingActive = state.isDefrostingActive();
                } else if (subCommand instanceof ChargeState) {
                    ChargeState state = (ChargeState) subCommand;
                    batteryPercentage = state.getBatteryLevel();
                } else if (subCommand instanceof LockState) {
                    LockState state = (LockState) subCommand;
                    doorsLocked = state.isLocked();
                } else if (subCommand instanceof TrunkState) {
                    TrunkState state = (TrunkState) subCommand;
                    trunkLockState = state.getLockState();
                    trunkLockPosition = state.getPosition();
                } else if (subCommand instanceof RooftopState) {
                    RooftopState state = (RooftopState) subCommand;
                    rooftopDimmingPercentage = state.getDimmingPercentage();
                    rooftopOpenPercentage = state.getOpenPercentage();
                } else if (subCommand instanceof LightsState) {
                    LightsState lights = (LightsState) subCommand;
                    frontExteriorLightState = lights.getFrontExteriorLightState();
                    isRearExteriorLightActive = lights.isRearExteriorLightActive();
                    isInteriorLightActive = lights.isInteriorLightActive();
                }
            }
        } else if (command instanceof ClimateState) {
            ClimateState state = (ClimateState) command;
            insideTemperature = state.getInsideTemperature();
            isWindshieldDefrostingActive = state.isDefrostingActive();
        } else if (command instanceof LockState) {
            LockState state = (LockState) command;
            doorsLocked = state.isLocked();
        } else if (command instanceof TrunkState) {
            TrunkState state = (TrunkState) command;
            trunkLockState = state.getLockState();
            trunkLockPosition = state.getPosition();
        } else if (command instanceof RooftopState) {
            RooftopState state = (RooftopState) command;
            rooftopDimmingPercentage = state.getDimmingPercentage();
            rooftopOpenPercentage = state.getOpenPercentage();
        } else if (command instanceof ChargeState) {
            ChargeState state = (ChargeState) command;
            batteryPercentage = state.getBatteryLevel();
        } else if (command instanceof LightsState) {
            LightsState state = (LightsState) command;
            frontExteriorLightState = state.getFrontExteriorLightState();
            isRearExteriorLightActive = state.isRearExteriorLightActive();
            isInteriorLightActive = state.isInteriorLightActive();
            lightsAmbientColor = state.getAmbientColor();
        } else if (command instanceof Capabilities) {
            capabilities = (Capabilities) command;
        }
    }

    public void onLinkAuthenticated(Link link) {
        vehicleConnectedWithBle = link.getSerial();
    }

    public void onLinkReceived() {
        vehicleConnectedWithBle = new byte[]{0x00};
    }

    public boolean isSupported(Type type) {
        return capabilities == null ? false : capabilities.isSupported(type);
    }
}
