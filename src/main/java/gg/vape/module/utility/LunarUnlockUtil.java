package gg.vape.module.utility;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * LunarUnlockUtil - Core utility for unlocking Lunar Client cosmetics
 *
 * This class uses reflection to:
 * - Detect Lunar Client runtime
 * - Find Lunar Client singleton instance
 * - Build fake login responses for various cosmetic systems
 * - Invoke handlers to register cosmetics as "owned"
 *
 * Ported from Meowtils LunarUnlocker extension.
 */
public final class LunarUnlockUtil {

    // Lunar Client cosmetic system class names
    private static final String COSMETIC_LOGIN_V2 = "com.lunarclient.websocket.cosmetic.v2.LoginResponse";
    private static final String COSMETIC_LOGIN_V1 = "com.lunarclient.websocket.cosmetic.v1.LoginResponse";
    private static final String EMOTE_LOGIN = "com.lunarclient.websocket.emote.v1.LoginResponse";
    private static final String BADGE_LOGIN = "com.lunarclient.websocket.badge.v1.LoginResponse";
    private static final String SPRAY_LOGIN = "com.lunarclient.websocket.spray.v1.LoginResponse";

    private static Boolean lunarRuntime = null;

    /**
     * Check if Lunar Client runtime is available
     */
    public static boolean isAvailable() {
        if (lunarRuntime == null) {
            lunarRuntime = detectLunarRuntime();
        }
        return lunarRuntime;
    }

    /**
     * Unlock all cosmetic systems
     * Will attempt unlock even if Lunar detection is uncertain
     */
    public static UnlockResult unlockAll() {
        // Try to find Lunar Client singleton (even if detection is uncertain)
        Object lunarClient = findLunarClientSingleton();
        if (lunarClient == null) {
            return UnlockResult.failure("Lunar Client instance not found. Make sure you're running Lunar Client 1.8.9.");
        }

        List<String> unlocked = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        // Try to unlock each cosmetic system
        if (applyUnlock(lunarClient, COSMETIC_LOGIN_V2, true, false)) {
            unlocked.add("cosmetics (v2)");
        } else if (applyUnlock(lunarClient, COSMETIC_LOGIN_V1, true, false)) {
            unlocked.add("cosmetics (v1)");
        } else {
            failed.add("cosmetics");
        }

        if (applyUnlock(lunarClient, EMOTE_LOGIN, false, false)) {
            unlocked.add("emotes");
        } else {
            failed.add("emotes");
        }

        if (applyUnlock(lunarClient, BADGE_LOGIN, false, false)) {
            unlocked.add("badges");
        } else {
            failed.add("badges");
        }

        if (applyUnlock(lunarClient, SPRAY_LOGIN, false, false)) {
            unlocked.add("sprays");
        } else {
            failed.add("sprays");
        }

        if (!failed.isEmpty()) {
            return UnlockResult.failure("Could not apply unlock (" + String.join(", ", failed) + ")");
        }

        return UnlockResult.success(unlocked, failed);
    }

    /**
     * Apply unlock for a specific cosmetic system
     */
    private static boolean applyUnlock(Object lunarClient, String loginResponseClassName,
                                      boolean isCosmetic, boolean artistTools) {
        try {
            ClassLoader loader = lunarClient.getClass().getClassLoader();
            Class<?> loginClass = resolveClass(loginResponseClassName, loader);

            Object loginResponse = buildLoginResponse(loginClass, isCosmetic, artistTools);

            // Try to invoke the login handler
            if (invokeLoginHandler(lunarClient, loginClass, loginResponse)) {
                return true;
            }

            // Fallback: try direct cosmetic manager invocation
            if (invokeCosmeticManagerDirect(lunarClient, loginClass, loginResponse)) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Build a fake login response object
     */
    private static Object buildLoginResponse(Class<?> loginClass, boolean isCosmetic, boolean artistTools)
            throws Exception {
        Method newBuilder = loginClass.getMethod("newBuilder");
        Object builder = newBuilder.invoke(null);

        if (isCosmetic) {
            // Set all cosmetics flags
            setBoolean(builder, "setHasAllCosmeticsFlag", true);
            setBoolean(builder, "setHasAllEmotesFlag", true);
            setBoolean(builder, "setHasAllBadgesFlag", true);
            setBoolean(builder, "setHasAllSpraysFlag", true);

            if (artistTools) {
                setBoolean(builder, "setArtistTools", true);
            }

            // Add default outfit for v2
            if (loginClass.getName().equals(COSMETIC_LOGIN_V2)) {
                addDefaultOutfit(builder);
            }
        }

        Method build = builder.getClass().getMethod("build");
        return build.invoke(builder);
    }

    /**
     * Add a default outfit (for cosmetic v2)
     */
    private static void addDefaultOutfit(Object builder) throws Exception {
        Class<?> outfitClass = Class.forName("com.lunarclient.websocket.cosmetic.v2.Outfit");
        Method newOutfitBuilder = outfitClass.getMethod("newBuilder");
        Object outfitBuilder = newOutfitBuilder.invoke(null);

        // Set outfit properties
        Method setName = outfitBuilder.getClass().getMethod("setName", String.class);
        setName.invoke(outfitBuilder, "Infinite Yield");

        Method setFavorite = outfitBuilder.getClass().getMethod("setFavorite", Boolean.TYPE);
        setFavorite.invoke(outfitBuilder, true);

        Method buildOutfit = outfitBuilder.getClass().getMethod("build");
        Object outfit = buildOutfit.invoke(outfitBuilder);

        // Create outfit tree
        Class<?> outfitTreeClass = Class.forName("com.lunarclient.websocket.cosmetic.v2.OutfitTree");
        Method newTreeBuilder = outfitTreeClass.getMethod("newBuilder");
        Object treeBuilder = newTreeBuilder.invoke(null);

        Method addOutfits = treeBuilder.getClass().getMethod("addOutfits", outfitClass);
        addOutfits.invoke(treeBuilder, outfit);

        Method buildTree = treeBuilder.getClass().getMethod("build");
        Object outfitTree = buildTree.invoke(treeBuilder);

        // Get outfit ID and set it as default
        Method getId = outfit.getClass().getMethod("getId");
        Object outfitId = getId.invoke(outfit);

        Method setDefaultOutfitId = builder.getClass().getMethod("setDefaultOutfitId", String.class);
        setDefaultOutfitId.invoke(builder, outfitId);

        Method setOutfitTree = builder.getClass().getMethod("setOutfitTree", outfitTreeClass);
        setOutfitTree.invoke(builder, outfitTree);
    }

    /**
     * Invoke the login handler on the Lunar client instance
     */
    private static boolean invokeLoginHandler(Object lunarClient, Class<?> loginClass, Object loginResponse) {
        try {
            // Find methods that accept the login response
            for (Method method : lunarClient.getClass().getMethods()) {
                if (method.getParameterCount() == 1 &&
                    method.getReturnType() == Void.TYPE &&
                    method.getParameterTypes()[0].isAssignableFrom(loginClass)) {
                    method.invoke(lunarClient, loginResponse);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Try to invoke cosmetic manager directly
     */
    private static boolean invokeCosmeticManagerDirect(Object lunarClient, Class<?> loginClass, Object loginResponse) {
        try {
            // Try to find cosmetic manager field and invoke on it
            return invokeHandlerOnTarget(lunarClient, loginClass, loginResponse);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Recursively search for handler in object's fields
     */
    private static boolean invokeHandlerOnTarget(Object target, Class<?> loginClass, Object loginResponse) {
        try {
            for (Method method : target.getClass().getMethods()) {
                if (method.getParameterCount() == 1 &&
                    method.getReturnType() == Void.TYPE &&
                    method.getParameterTypes()[0].isAssignableFrom(loginClass)) {
                    method.invoke(target, loginResponse);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Helper to set boolean field on builder
     */
    private static void setBoolean(Object builder, String methodName, boolean value) {
        try {
            Method method = builder.getClass().getMethod(methodName, Boolean.TYPE);
            method.invoke(builder, value);
        } catch (Exception ignored) {
        }
    }

    /**
     * Detect if we're running in Lunar Client
     * This is now more lenient and will return true if any Lunar classes are found
     */
    private static boolean detectLunarRuntime() {
        try {
            ClassLoader[] loaders = getClassLoaders();
            for (ClassLoader loader : loaders) {
                try {
                    // Try multiple Lunar-specific classes
                    Class.forName("com.moonsworth.lunar.LunarClient", false, loader);
                    return true;
                } catch (ClassNotFoundException e1) {
                    try {
                        Class.forName("com.lunarclient.websocket.LunarWebSocket", false, loader);
                        return true;
                    } catch (ClassNotFoundException e2) {
                        try {
                            Class.forName("com.lunarclient.bukkitapi.LunarClientAPI", false, loader);
                            return true;
                        } catch (ClassNotFoundException e3) {
                            // Continue checking
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Find Lunar Client singleton instance
     * Tries multiple methods to locate the instance
     */
    private static Object findLunarClientSingleton() {
        try {
            // Try the primary method: com.moonsworth.lunar.LunarClient
            Class<?> lunarClass = null;
            try {
                lunarClass = Class.forName("com.moonsworth.lunar.LunarClient");
            } catch (ClassNotFoundException e1) {
                // Try alternative class names
                try {
                    lunarClass = Class.forName("lunar.LunarClient");
                } catch (ClassNotFoundException e2) {
                    try {
                        lunarClass = Class.forName("com.lunarclient.LunarClient");
                    } catch (ClassNotFoundException e3) {
                        // Could not find any Lunar client class
                        return null;
                    }
                }
            }

            if (lunarClass == null) {
                return null;
            }

            // Try getInstance() method
            try {
                Method getInstance = lunarClass.getMethod("getInstance");
                Object instance = getInstance.invoke(null);
                if (instance != null) {
                    return instance;
                }
            } catch (NoSuchMethodException | SecurityException e) {
                // Method doesn't exist, try field
            }

            // Try INSTANCE field
            try {
                Field instanceField = lunarClass.getField("INSTANCE");
                Object instance = instanceField.get(null);
                if (instance != null) {
                    return instance;
                }
            } catch (NoSuchFieldException e) {
                // Field doesn't exist
            }

            // Try instance field (lowercase)
            try {
                Field instanceField = lunarClass.getField("instance");
                Object instance = instanceField.get(null);
                if (instance != null) {
                    return instance;
                }
            } catch (NoSuchFieldException e) {
                // Field doesn't exist
            }

            // Try getDeclaredField for private fields
            try {
                Field instanceField = lunarClass.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                return instanceField.get(null);
            } catch (NoSuchFieldException e) {
                // Last attempt failed
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Resolve class with preferred loader
     */
    private static Class<?> resolveClass(String name, ClassLoader preferredLoader) throws ClassNotFoundException {
        try {
            return Class.forName(name, false, preferredLoader);
        } catch (ClassNotFoundException e) {
            // Try other loaders
            for (ClassLoader loader : getClassLoaders()) {
                try {
                    return Class.forName(name, false, loader);
                } catch (ClassNotFoundException ignored) {
                }
            }
            throw new ClassNotFoundException(name);
        }
    }

    /**
     * Get all available class loaders
     */
    private static ClassLoader[] getClassLoaders() {
        return new ClassLoader[]{
            Thread.currentThread().getContextClassLoader(),
            LunarUnlockUtil.class.getClassLoader(),
            ClassLoader.getSystemClassLoader()
        };
    }

    /**
     * Result of unlock operation
     */
    public static class UnlockResult {
        private final boolean success;
        private final String message;
        private final List<String> unlocked;
        private final List<String> failed;

        private UnlockResult(boolean success, String message, List<String> unlocked, List<String> failed) {
            this.success = success;
            this.message = message;
            this.unlocked = unlocked;
            this.failed = failed;
        }

        public static UnlockResult success(List<String> unlocked, List<String> failed) {
            String msg = "Unlocked: " + String.join(", ", unlocked);
            return new UnlockResult(true, msg, unlocked, failed);
        }

        public static UnlockResult failure(String message) {
            return new UnlockResult(false, message, new ArrayList<>(), new ArrayList<>());
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getUnlocked() {
            return unlocked;
        }

        public List<String> getFailed() {
            return failed;
        }
    }
}
