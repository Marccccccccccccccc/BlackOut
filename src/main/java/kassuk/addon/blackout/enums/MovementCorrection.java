package kassuk.addon.blackout.enums;

/**
 * Movement correction modes to avoid sprinting sideways detection by anticheats like Grim.
 */
public enum MovementCorrection {
    /**
     * No movement correction is applied. This feels the best, as it does not
     * change the movement of the player and also not affects Sprinting.
     * However, this can be detected by anti-cheats.
     */
    OFF,

    /**
     * Corrects movement by changing the yaw when updating the movement.
     */
    STRICT,

    /**
     * Correct movement by changing the yaw when updating the movement,
     * but also tweaks the keyboard input to not aggressively change the
     * players walk direction.
     */
    SILENT,

    /**
     * Corrects movement by changing the actual look direction of the player.
     */
    CHANGE_LOOK
}
