package kassuk.addon.blackout.globalsettings;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.BlackOutModule;
import kassuk.addon.blackout.enums.MovementCorrection;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;

/**
 * Global anticheat settings for every blackout module.
 */
public class AntiCheatSettings extends BlackOutModule {
    public AntiCheatSettings() {
        super(BlackOut.SETTINGS, "AntiCheat", "Global anticheat settings for every blackout module.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<MovementCorrection> movementCorrection = sgGeneral.add(new EnumSetting.Builder<MovementCorrection>()
        .name("Movement Correction")
        .description("Prevents sprinting sideways which flags on Grim anticheat.")
        .defaultValue(MovementCorrection.OFF)
        .build()
    );
}
