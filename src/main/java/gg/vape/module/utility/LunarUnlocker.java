package gg.vape.module.utility;

import gg.vape.Vape;
import gg.vape.module.UtilityMod;
import gg.vape.notification.NotificationType;
import gg.vape.wrapper.impl.Minecraft;

/**
 * LunarUnlocker - Unlocks Lunar Client cosmetics client-side (1.8.9 only)
 *
 * This module bypasses Lunar Client's cosmetic verification by:
 * 1. Detecting Lunar Client runtime environment
 * 2. Finding the Lunar singleton instance
 * 3. Injecting fake login responses for cosmetics, emotes, badges, and sprays
 * 4. Setting all-access flags via reflection
 *
 * Based on Meowtils LunarUnlocker extension analysis.
 * For educational and research purposes only.
 */
public class LunarUnlocker extends UtilityMod {

    public LunarUnlocker() {
        super("LunarUnlocker", "Unlocks all Lunar Client cosmetics (1.8.9 only)");
        this.setDefaultVisibility(false);
    }

    @Override
    public void onEnable() {
        // Immediately disable - this is a one-shot action module
        this.setEnabled(false);

        // Check if player is in world
        if (Minecraft.thePlayer().isNull()) {
            Vape.INSTANCE.getNotificationManager().show(
                "LunarUnlocker",
                "Join a world first!",
                NotificationType.WARNING,
                3000,
                false
            );
            return;
        }

        // Check if Lunar Client is detected
        if (!LunarUnlockUtil.isAvailable()) {
            Vape.INSTANCE.getNotificationManager().show(
                "LunarUnlocker",
                "Lunar Client was not detected.",
                NotificationType.WARNING,
                3000,
                false
            );
            return;
        }

        // Perform unlock
        LunarUnlockUtil.UnlockResult result = LunarUnlockUtil.unlockAll();

        if (result.isSuccess()) {
            String message = result.getMessage();
            if (message == null || message.isEmpty()) {
                message = "Successfully unlocked Lunar cosmetics!";
            }
            Vape.INSTANCE.getNotificationManager().show(
                "LunarUnlocker",
                message,
                NotificationType.INFO,
                4000,
                false
            );
        } else {
            Vape.INSTANCE.getNotificationManager().show(
                "LunarUnlocker",
                result.getMessage(),
                NotificationType.WARNING,
                4000,
                false
            );
        }
    }
}
