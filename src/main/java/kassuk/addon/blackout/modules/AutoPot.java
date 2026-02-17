package kassuk.addon.blackout.modules;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.BlackOutModule;
import kassuk.addon.blackout.enums.*;
import kassuk.addon.blackout.managers.Managers;
import kassuk.addon.blackout.utils.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;

import java.util.Objects;

/**
 * @author Marc3D
 * Ported from Meta's AutoSplashPotion module
 * Automatically throws splash potions based on health and status effects
 */

public class AutoPot extends BlackOutModule {
    public AutoPot() {
        super(BlackOut.BLACKOUT, "AutoPot", "Automatically throws splash potions.");
    }

    private final SettingGroup sgHealth = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("Speed");
    private final SettingGroup sgCalculations = settings.createGroup("Calculations");
    private final SettingGroup sgExperimental = settings.createGroup("Experimental");

    //--------------------Health--------------------//
    private final Setting<Boolean> heal = sgHealth.add(new BoolSetting.Builder()
        .name("Heal")
        .description("Automatically throw healing potions.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> keepHealInHotbar = sgHealth.add(new BoolSetting.Builder()
        .name("Keep Heal In Hotbar")
        .description("Automatically moves healing potions to hotbar.")
        .defaultValue(true)
        .visible(heal::get)
        .build()
    );
    private final Setting<Integer> healHotbar = sgHealth.add(new IntSetting.Builder()
        .name("Heal Hotbar Slot")
        .description("Which hotbar slot to keep healing potions.")
        .defaultValue(7)
        .range(1, 9)
        .sliderMax(9)
        .visible(() -> heal.get() && keepHealInHotbar.get())
        .build()
    );
    private final Setting<Double> healHealth = sgHealth.add(new DoubleSetting.Builder()
        .name("Heal Health")
        .description("Health to trigger healing at.")
        .defaultValue(12.0)
        .min(0.0)
        .sliderMax(20.0)
        .visible(heal::get)
        .build()
    );
    private final Setting<Integer> healDelay = sgHealth.add(new IntSetting.Builder()
        .name("Heal Delay")
        .description("Delay between healing potions in milliseconds.")
        .defaultValue(500)
        .range(0, 10000)
        .sliderMax(5000)
        .visible(heal::get)
        .build()
    );
    private final Setting<Boolean> checkGround = sgHealth.add(new BoolSetting.Builder()
        .name("Ground Check")
        .description("Only heal when on ground or near ground.")
        .defaultValue(false)
        .visible(heal::get)
        .build()
    );
    private final Setting<Integer> groundRange = sgHealth.add(new IntSetting.Builder()
        .name("Ground Range")
        .description("Distance from ground to allow healing.")
        .defaultValue(3)
        .range(1, 6)
        .sliderMax(6)
        .visible(() -> heal.get() && checkGround.get())
        .build()
    );
    private final Setting<Keybind> forceHealthBind = sgHealth.add(new KeybindSetting.Builder()
        .name("Force Health Bind")
        .description("Keybind to force throw healing potion.")
        .defaultValue(Keybind.none())
        .visible(heal::get)
        .build()
    );
    private final Setting<SwitchMode> switchMode = sgHealth.add(new EnumSetting.Builder<SwitchMode>()
        .name("Switch Mode")
        .description("How to switch items.")
        .defaultValue(SwitchMode.Silent)
        .build()
    );

    //--------------------Speed--------------------//
    private final Setting<Boolean> speed = sgSpeed.add(new BoolSetting.Builder()
        .name("Speed")
        .description("Automatically throw speed potions.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> speedDelay = sgSpeed.add(new IntSetting.Builder()
        .name("Speed Delay")
        .description("Delay between speed potions in milliseconds.")
        .defaultValue(5000)
        .range(0, 10000)
        .sliderMax(10000)
        .visible(speed::get)
        .build()
    );
    private final Setting<Double> speedHealth = sgSpeed.add(new DoubleSetting.Builder()
        .name("Speed Health")
        .description("Minimum health required to throw speed potions.")
        .defaultValue(4.0)
        .min(0.0)
        .sliderMax(20.0)
        .visible(speed::get)
        .build()
    );
    private final Setting<Boolean> speedCheckBelow = sgSpeed.add(new BoolSetting.Builder()
        .name("Check Below")
        .description("Don't throw speed if floating in air.")
        .defaultValue(true)
        .visible(speed::get)
        .build()
    );
    private final Setting<Boolean> speedCheckGround = sgSpeed.add(new BoolSetting.Builder()
        .name("Speed Ground Check")
        .description("Only throw speed when on ground.")
        .defaultValue(false)
        .visible(speed::get)
        .build()
    );
    private final Setting<Keybind> forceSpeedBind = sgSpeed.add(new KeybindSetting.Builder()
        .name("Force Speed Bind")
        .description("Keybind to force throw speed potion.")
        .defaultValue(Keybind.none())
        .visible(speed::get)
        .build()
    );

    //--------------------Calculations--------------------//
    private final Setting<Boolean> adaptiveHealing = sgCalculations.add(new BoolSetting.Builder()
        .name("Adaptive Healing")
        .description("Throw extra potion if health is very low.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Double> adaptiveHealth = sgCalculations.add(new DoubleSetting.Builder()
        .name("Adaptive Health")
        .description("Health threshold for adaptive healing.")
        .defaultValue(5.5)
        .min(0.5)
        .sliderMax(20.0)
        .visible(adaptiveHealing::get)
        .build()
    );
    private final Setting<InstantMode> instantThrow = sgCalculations.add(new EnumSetting.Builder<InstantMode>()
        .name("Instant Healing Mode")
        .description("Throw multiple potions instantly.")
        .defaultValue(InstantMode.NONE)
        .build()
    );
    private final Setting<Double> instantResetHP = sgCalculations.add(new DoubleSetting.Builder()
        .name("Instant Reset HP")
        .description("Health to reset instant throw cooldown.")
        .defaultValue(15.0)
        .min(0.0)
        .sliderMax(20.0)
        .visible(() -> instantThrow.get() == InstantMode.RESET_HP)
        .build()
    );
    private final Setting<Integer> instantResetDelay = sgCalculations.add(new IntSetting.Builder()
        .name("Instant Reset Delay")
        .description("Delay to reset instant throw cooldown in milliseconds.")
        .defaultValue(1000)
        .range(0, 10000)
        .sliderMax(5000)
        .visible(() -> instantThrow.get() == InstantMode.RESET_DELAY)
        .build()
    );
    private final Setting<Integer> instantAmount = sgCalculations.add(new IntSetting.Builder()
        .name("Instant Amount")
        .description("Number of potions to throw instantly.")
        .defaultValue(3)
        .range(1, 5)
        .sliderMax(5)
        .visible(() -> instantThrow.get() != InstantMode.NONE)
        .build()
    );
    private final Setting<Boolean> predictiveHealing = sgCalculations.add(new BoolSetting.Builder()
        .name("Predictive Healing")
        .description("Predict future damage and heal early.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Integer> predictionTicks = sgCalculations.add(new IntSetting.Builder()
        .name("Prediction Ticks")
        .description("How many ticks ahead to predict.")
        .defaultValue(3)
        .range(1, 10)
        .sliderMax(10)
        .visible(predictiveHealing::get)
        .build()
    );
    private final Setting<Double> predictionSensitivity = sgCalculations.add(new DoubleSetting.Builder()
        .name("Prediction Decay")
        .description("How much to weight damage trend.")
        .defaultValue(0.8)
        .min(0.0)
        .sliderMax(2.0)
        .visible(predictiveHealing::get)
        .build()
    );

    //--------------------Experimental--------------------//
    private final Setting<Boolean> debug = sgExperimental.add(new BoolSetting.Builder()
        .name("Debug")
        .description("Show debug messages in chat.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> doubleHealth = sgExperimental.add(new BoolSetting.Builder()
        .name("Double Healing")
        .description("Throw two healing potions at once.")
        .defaultValue(false)
        .visible(heal::get)
        .build()
    );
    private final Setting<Boolean> upThrow = sgExperimental.add(new BoolSetting.Builder()
        .name("Upwards Throw")
        .description("Throw upwards if block above head.")
        .defaultValue(false)
        .visible(heal::get)
        .build()
    );
    private final Setting<Integer> upThrowTimeout = sgExperimental.add(new IntSetting.Builder()
        .name("Upwards Throw Timeout")
        .description("Timeout for upwards throw in milliseconds.")
        .defaultValue(1000)
        .range(0, 10000)
        .sliderMax(5000)
        .visible(() -> heal.get() && upThrow.get())
        .build()
    );
    private final Setting<Double> rotationDegrees = sgExperimental.add(new DoubleSetting.Builder()
        .name("Rotation Degrees")
        .description("Pitch rotation when throwing potions.")
        .defaultValue(90.0)
        .min(70.0)
        .sliderMax(90.0)
        .build()
    );
    private final Setting<Boolean> swing = sgExperimental.add(new BoolSetting.Builder()
        .name("Swing")
        .description("Swing hand when throwing.")
        .defaultValue(true)
        .build()
    );
    private final Setting<SwingHand> swingHand = sgExperimental.add(new EnumSetting.Builder<SwingHand>()
        .name("Swing Hand")
        .description("Which hand to swing.")
        .defaultValue(SwingHand.RealHand)
        .visible(swing::get)
        .build()
    );

    // State
    private PotionType currentPotion = PotionType.NONE;
    private boolean instantThrowUsed = false;
    private long lastInstantThrowResetTime = 0;
    private long upThrowStartTime = 0;
    private long lastHealTime = 0;
    private long lastSpeedTime = 0;
    private float lastHealth = 20.0f;
    private float damageTrend = 0.0f;
    private boolean forceHealth = false;
    private boolean forceSpeed = false;

    @Override
    public void onActivate() {
        super.onActivate();
        resetState();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        resetState();
    }

    private void resetState() {
        currentPotion = PotionType.NONE;
        instantThrowUsed = false;
        lastInstantThrowResetTime = 0;
        upThrowStartTime = 0;
        lastHealTime = 0;
        lastSpeedTime = 0;
        lastHealth = 20.0f;
        damageTrend = 0.0f;
        forceHealth = false;
        forceSpeed = false;
    }

    @Override
    public String getInfoString() {
        if (mc.player == null) return null;
        return String.format("%.1f HP", mc.player.getHealth());
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (heal.get() && forceHealthBind.get().isPressed()) {
            forceHealth = true;
            if (debug.get()) debug("Force health triggered");
        }
        if (speed.get() && forceSpeedBind.get().isPressed()) {
            forceSpeed = true;
            if (debug.get()) debug("Force speed triggered");
        }

        if (checkGround.get() && !isNearGround()) {
            currentPotion = PotionType.NONE;
            if (debug.get()) debug("Ground check failed - not near ground");
            return;
        }

        if (currentPotion == PotionType.NONE) {
            if (forceHealth && heal.get() && hasPotion(PotionType.INSTANT_HEALTH)) {
                currentPotion = PotionType.INSTANT_HEALTH;
                forceHealth = false;
                if (debug.get()) debug("Force heal potion selected");
            } else if (forceSpeed && speed.get() && hasPotion(PotionType.SPEED)) {
                currentPotion = PotionType.SPEED;
                forceSpeed = false;
                if (debug.get()) debug("Force speed potion selected");
            } else {
                currentPotion = checkPotions();
                if (debug.get() && currentPotion != PotionType.NONE) {
                    debug("Auto selected potion: " + currentPotion);
                }
            }
        }

        if (currentPotion != PotionType.NONE) {
            float pitch = currentPotion == PotionType.SPEED ? 90.0f : getPitchForHealthPotion();

            BlockPos targetPos = mc.player.getBlockPos();

            boolean rotated = !SettingUtils.shouldRotate(RotationType.Interact) ||
                Managers.ROTATION.start(
                    targetPos,
                    priority,
                    RotationType.Interact,
                    Objects.hash(name + "pot")
                );

            if (!rotated) {
                if (debug.get()) debug("Rotation failed - waiting");
                return;
            }
            if (debug.get()) debug("Rotation ready, pitch: " + pitch);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTickPost(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        PotionType potionType = currentPotion;
        if (potionType == PotionType.NONE) return;

        // Reset instant throw cooldown
        switch (instantThrow.get()) {
            case RESET_HP:
                if (mc.player.getHealth() >= instantResetHP.get() && instantThrowUsed) {
                    instantThrowUsed = false;
                }
                break;
            case RESET_DELAY:
                if (System.currentTimeMillis() - lastInstantThrowResetTime >= instantResetDelay.get() && instantThrowUsed) {
                    instantThrowUsed = false;
                }
                break;
        }

        FindItemResult potion = findPotion(potionType);
        if (!potion.found()) {
            if (debug.get()) debug("No " + potionType + " potion found in hotbar!");
            currentPotion = PotionType.NONE;
            if (SettingUtils.shouldRotate(RotationType.Interact)) {
                Managers.ROTATION.end(Objects.hash(name + "pot"));
            }
            return;
        }
        if (debug.get()) debug("Found " + potionType + " in slot " + potion.slot());

        boolean switched = false;
        Hand handToUse;

        if (isPotionInHand(Hand.MAIN_HAND, potionType)) {
            handToUse = Hand.MAIN_HAND;
        } else if (isPotionInHand(Hand.OFF_HAND, potionType)) {
            handToUse = Hand.OFF_HAND;
        } else {
            switched = true;
            switch (switchMode.get()) {
                case Silent, Normal -> InvUtils.swap(potion.slot(), true);
                case PickSilent -> BOInvUtils.pickSwitch(potion.slot());
                case InvSwitch -> BOInvUtils.invSwitch(potion.slot());
            }
            handToUse = Hand.MAIN_HAND;
        }

        // Make hand final for lambda
        final Hand hand = handToUse;

        // Throw potion(s)
        if (potionType == PotionType.INSTANT_HEALTH) {
            int count = calculateThrowCount();
            if (debug.get()) debug("Throwing " + count + " heal potion(s)");
            for (int i = 0; i < count; i++) {
                sendSequenced(s -> new PlayerInteractItemC2SPacket(hand, s, mc.player.getYaw(), mc.player.getPitch()));
            }
            lastHealTime = System.currentTimeMillis();
        } else if (potionType == PotionType.SPEED && shouldThrowSpeed()) {
            if (debug.get()) debug("Throwing speed potion");
            sendSequenced(s -> new PlayerInteractItemC2SPacket(hand, s, mc.player.getYaw(), mc.player.getPitch()));
            lastSpeedTime = System.currentTimeMillis();
        } else if (potionType == PotionType.SPEED && !shouldThrowSpeed()) {
            if (debug.get()) debug("Speed check failed - not throwing");
        }

        if (swing.get()) clientSwing(swingHand.get(), hand);

        currentPotion = PotionType.NONE;
        if (SettingUtils.shouldRotate(RotationType.Interact)) {
            Managers.ROTATION.end(Objects.hash(name + "pot"));
        }

        if (switched) {
            switch (switchMode.get()) {
                case Silent -> InvUtils.swapBack();
                case PickSilent -> BOInvUtils.pickSwapBack();
                case InvSwitch -> BOInvUtils.swapBack();
            }
        }

        // Update damage trend for predictive healing
        if (heal.get() && keepHealInHotbar.get()) {
            float current = mc.player.getHealth();
            float diff = lastHealth - current;
            damageTrend = damageTrend * 0.8f + diff * 0.2f;
            lastHealth = current;

            // Auto-move healing potions to hotbar
            if (!hasPotion(PotionType.INSTANT_HEALTH)) {
                moveHealPotionToHotbar();
            }
        }
    }

    private float getPitchForHealthPotion() {
        if (upThrow.get() && hasBlockAboveHead()) {
            if (upThrowStartTime == 0) upThrowStartTime = System.currentTimeMillis();
            if (System.currentTimeMillis() - upThrowStartTime >= upThrowTimeout.get()) {
                return -90.0f;
            } else {
                return rotationDegrees.get().floatValue();
            }
        } else {
            upThrowStartTime = 0;
            return rotationDegrees.get().floatValue();
        }
    }

    private int calculateThrowCount() {
        int count;
        if (instantThrow.get() != InstantMode.NONE && !instantThrowUsed) {
            instantThrowUsed = true;
            lastInstantThrowResetTime = System.currentTimeMillis();
            count = instantAmount.get();
        } else if (doubleHealth.get()) {
            count = 2;
        } else {
            count = 1;
        }

        // Add extra potion if adaptive healing is enabled and health is very low
        if (adaptiveHealing.get() && mc.player.getHealth() <= adaptiveHealth.get() && count <= 2) {
            count++;
        }

        return count;
    }

    private boolean isNearGround() {
        if (mc.player.isOnGround()) return true;

        BlockPos pos = mc.player.getBlockPos();
        for (int i = 1; i <= groundRange.get(); i++) {
            if (!mc.world.getBlockState(pos.down(i)).isReplaceable()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBlockAboveHead() {
        BlockPos pos = BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        return !mc.world.getBlockState(pos.up(2)).isReplaceable() ||
               !mc.world.getBlockState(BlockPos.ofFloored(mc.player.getX(), mc.player.getY() + mc.player.getStandingEyeHeight(), mc.player.getZ())).isReplaceable();
    }

    private boolean shouldThrowSpeed() {
        boolean withinGround = mc.player.isOnGround() || (mc.player.getY() - getGroundLevel() < 3.0);

        if (speedCheckBelow.get()) {
            if (mc.world.getBlockState(mc.player.getBlockPos().down()).isReplaceable() && withinGround) {
                return false;
            }
        }

        if (mc.player.getHealth() < speedHealth.get()) return false;

        return !speedCheckGround.get() || mc.player.isOnGround();
    }

    private double getGroundLevel() {
        BlockPos pos = mc.player.getBlockPos();
        for (int i = 0; i < 20; i++) {
            if (!mc.world.getBlockState(pos.down(i)).isReplaceable()) {
                return pos.down(i).getY() + 1.0;
            }
        }
        return mc.player.getY();
    }

    private PotionType checkPotions() {
        // Check heal first (higher priority)
        if (heal.get()) {
            long timeSinceLastHeal = System.currentTimeMillis() - lastHealTime;
            if (timeSinceLastHeal < healDelay.get()) {
                if (debug.get()) debug("Heal on cooldown: " + (healDelay.get() - timeSinceLastHeal) + "ms");
            } else {
                float health = mc.player.getHealth();
                float predictedHealth = health;

                // Apply predictive healing
                if (predictiveHealing.get()) {
                    predictedHealth -= damageTrend * predictionTicks.get() * predictionSensitivity.get().floatValue();
                    if (debug.get()) debug("HP: " + health + " -> " + predictedHealth + " (trend: " + damageTrend + ")");
                }

                if (predictedHealth <= healHealth.get()) {
                    if (hasPotion(PotionType.INSTANT_HEALTH)) {
                        if (debug.get()) debug("Heal threshold reached! HP: " + predictedHealth + " <= " + healHealth.get());
                        return PotionType.INSTANT_HEALTH;
                    } else {
                        if (debug.get()) debug("Need heal but no potions!");
                    }
                }
            }
        }

        // Check speed
        if (speed.get()) {
            long timeSinceLastSpeed = System.currentTimeMillis() - lastSpeedTime;
            if (timeSinceLastSpeed < speedDelay.get()) {
                if (debug.get()) debug("Speed on cooldown: " + (speedDelay.get() - timeSinceLastSpeed) + "ms");
            } else if (mc.player.isGliding()) {
                if (debug.get()) debug("Speed check failed: gliding");
            } else if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                if (debug.get()) debug("Speed check failed: already has speed");
            } else if (!hasPotion(PotionType.SPEED)) {
                if (debug.get()) debug("Speed check failed: no speed potions");
            } else {
                if (debug.get()) debug("Speed potion ready!");
                return PotionType.SPEED;
            }
        }

        return PotionType.NONE;
    }

    private boolean hasPotion(PotionType type) {
        boolean found = InvUtils.find(itemStack -> isPotionType(itemStack, type)).found();
        if (debug.get() && !found) debug("No " + type + " in inventory!");
        return found;
    }

    private FindItemResult findPotion(PotionType type) {
        return InvUtils.findInHotbar(itemStack -> isPotionType(itemStack, type));
    }

    private boolean isPotionInHand(Hand hand, PotionType type) {
        ItemStack stack = hand == Hand.MAIN_HAND ? mc.player.getMainHandStack() : mc.player.getOffHandStack();
        return isPotionType(stack, type);
    }

    private boolean isPotionType(ItemStack stack, PotionType type) {
        if (!stack.isOf(Items.SPLASH_POTION)) return false;

        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) {
            if (debug.get()) debug("Potion has no contents component");
            return false;
        }

        // Check the base potion ID directly
        if (contents.potion().isPresent()) {
            var potion = contents.potion().get();
            String potionId = potion.getKey().map(k -> k.getValue().toString()).orElse("");

            if (debug.get()) debug("Checking potion ID: " + potionId);

            if (type == PotionType.INSTANT_HEALTH) {
                // Check for healing potions
                if (potionId.equals("minecraft:healing") || potionId.equals("minecraft:strong_healing")) {
                    if (debug.get()) debug("MATCH! Found healing potion");
                    return true;
                }
            } else if (type == PotionType.SPEED) {
                // Check for swiftness potions
                if (potionId.equals("minecraft:swiftness") || potionId.equals("minecraft:strong_swiftness") || potionId.equals("minecraft:long_swiftness")) {
                    if (debug.get()) debug("MATCH! Found speed potion");
                    return true;
                }
            }
        } else {
            if (debug.get()) debug("Potion has no base potion type");
        }

        if (debug.get()) debug("No matching potion type found");
        return false;
    }

    private void moveHealPotionToHotbar() {
        // Find healing potion in inventory (not in hotbar)
        FindItemResult potion = InvUtils.find(itemStack ->
            isPotionType(itemStack, PotionType.INSTANT_HEALTH)
        );

        if (!potion.found() || potion.isHotbar()) return;

        int targetSlot = healHotbar.get() - 1;

        // Use InvUtils to move potion to hotbar
        InvUtils.move().from(potion.slot()).toHotbar(targetSlot);
    }

    public enum SwitchMode {
        Disabled,
        Normal,
        Silent,
        PickSilent,
        InvSwitch
    }

    public enum InstantMode {
        NONE,
        RESET_HP,
        RESET_DELAY
    }

    private enum PotionType {
        INSTANT_HEALTH,
        SPEED,
        NONE
    }
}
