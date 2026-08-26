package gg.vape.module.utility;

import gg.vape.Vape;
import gg.vape.module.UtilityMod;
import gg.vape.notification.NotificationType;
import gg.vape.wrapper.impl.Minecraft;

/**
 * LunarUnlocker - Unlocks Lunar Client cosmetics client-side
 *
 * This module bypasses Lunar Client's cosmetic verification by:
 * 1. Detecting Lunar Client runtime environment
 * 2. Finding the Lunar singleton instance
 * 3. Injecting fake login responses for cosmetics, emotes, badges, and sprays
 * 4. Setting all-access flags via reflection
 *
 * Works on Minecraft 1.8.9 (Forge/Vanilla) when Lunar Client is running.
 * Based on Meowtils LunarUnlocker extension analysis.
 * For educational and research purposes only.
 */
public class LunarUnlocker extends UtilityMod {

    public LunarUnlocker() {
        super("LunarUnlocker", "Unlocks all Lunar Client cosmetics (1.8.9)");
        // Make visible by default so users can find it
        this.setDefaultVisibility(true);
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

        // Try to perform unlock (will auto-detect Lunar Client)
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
            // Show more informative error
            String errorMsg = result.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Lunar Client not detected or unlock failed.";
            }
            Vape.INSTANCE.getNotificationManager().show(
                "LunarUnlocker",
                errorMsg,
                NotificationType.WARNING,
                4000,
                false
            );
        }
    }
}
