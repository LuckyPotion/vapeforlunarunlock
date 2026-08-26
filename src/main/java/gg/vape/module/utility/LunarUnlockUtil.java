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
     * Now always returns true to allow unlock attempts
     */
    public static boolean isAvailable() {
        // Always return true - let the unlock attempt itself determine if Lunar is present
        return true;
    }

    /**
     * Unlock all cosmetic systems
     * Attempts unlock without pre-checking for Lunar Client
     */
    public static UnlockResult unlockAll() {
        System.out.println("[LunarUnlocker] Starting unlock process...");

        // Try to find Lunar Client singleton
        System.out.println("[LunarUnlocker] Attempting to find Lunar Client singleton...");
        Object lunarClient = findLunarClientSingleton();

        if (lunarClient == null) {
            System.out.println("[LunarUnlocker] Standard search failed, trying aggressive search...");
            // Try harder - search for any object that might be the Lunar client
            lunarClient = findLunarClientAggressively();
        }

        if (lunarClient == null) {
            System.out.println("[LunarUnlocker] ERROR: Lunar Client instance not found after all attempts");
            return UnlockResult.failure("Lunar Client not found. Are you running Lunar Client 1.8.9?");
        }

        System.out.println("[LunarUnlocker] Found Lunar Client instance: " + lunarClient.getClass().getName());

        List<String> unlocked = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        // Try to unlock each cosmetic system
        boolean anySuccess = false;

        System.out.println("[LunarUnlocker] Attempting to unlock cosmetics v2...");
        if (applyUnlock(lunarClient, COSMETIC_LOGIN_V2, true, false)) {
            unlocked.add("cosmetics (v2)");
            anySuccess = true;
            System.out.println("[LunarUnlocker] SUCCESS: Cosmetics v2 unlocked");
        } else {
            System.out.println("[LunarUnlocker] Cosmetics v2 failed, trying v1...");
            if (applyUnlock(lunarClient, COSMETIC_LOGIN_V1, true, false)) {
                unlocked.add("cosmetics (v1)");
                anySuccess = true;
                System.out.println("[LunarUnlocker] SUCCESS: Cosmetics v1 unlocked");
            } else {
                failed.add("cosmetics");
                System.out.println("[LunarUnlocker] FAILED: Both cosmetics versions failed");
            }
        }

        System.out.println("[LunarUnlocker] Attempting to unlock emotes...");
        if (applyUnlock(lunarClient, EMOTE_LOGIN, false, false)) {
            unlocked.add("emotes");
            anySuccess = true;
            System.out.println("[LunarUnlocker] SUCCESS: Emotes unlocked");
        } else {
            failed.add("emotes");
            System.out.println("[LunarUnlocker] FAILED: Emotes unlock failed");
        }

        System.out.println("[LunarUnlocker] Attempting to unlock badges...");
        if (applyUnlock(lunarClient, BADGE_LOGIN, false, false)) {
            unlocked.add("badges");
            anySuccess = true;
            System.out.println("[LunarUnlocker] SUCCESS: Badges unlocked");
        } else {
            failed.add("badges");
            System.out.println("[LunarUnlocker] FAILED: Badges unlock failed");
        }

        System.out.println("[LunarUnlocker] Attempting to unlock sprays...");
        if (applyUnlock(lunarClient, SPRAY_LOGIN, false, false)) {
            unlocked.add("sprays");
            anySuccess = true;
            System.out.println("[LunarUnlocker] SUCCESS: Sprays unlocked");
        } else {
            failed.add("sprays");
            System.out.println("[LunarUnlocker] FAILED: Sprays unlock failed");
        }

        // If at least one system was unlocked, consider it a success
        if (anySuccess) {
            System.out.println("[LunarUnlocker] Unlock completed with partial success");
            return UnlockResult.success(unlocked, failed);
        }

        // All failed
        System.out.println("[LunarUnlocker] ERROR: All unlock attempts failed");
        return UnlockResult.failure("All unlock attempts failed. Lunar cosmetic systems not accessible.");
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
            System.out.println("[LunarUnlocker] Searching for Lunar Client class...");
            // Try the primary method: com.moonsworth.lunar.LunarClient
            Class<?> lunarClass = null;
            try {
                System.out.println("[LunarUnlocker] Trying: com.moonsworth.lunar.LunarClient");
                lunarClass = Class.forName("com.moonsworth.lunar.LunarClient");
                System.out.println("[LunarUnlocker] Found class: com.moonsworth.lunar.LunarClient");
            } catch (ClassNotFoundException e1) {
                // Try alternative class names
                try {
                    System.out.println("[LunarUnlocker] Trying: lunar.LunarClient");
                    lunarClass = Class.forName("lunar.LunarClient");
                    System.out.println("[LunarUnlocker] Found class: lunar.LunarClient");
                } catch (ClassNotFoundException e2) {
                    try {
                        System.out.println("[LunarUnlocker] Trying: com.lunarclient.LunarClient");
                        lunarClass = Class.forName("com.lunarclient.LunarClient");
                        System.out.println("[LunarUnlocker] Found class: com.lunarclient.LunarClient");
                    } catch (ClassNotFoundException e3) {
                        // Could not find any Lunar client class
                        System.out.println("[LunarUnlocker] No Lunar Client class found in standard search");
                        return null;
                    }
                }
            }

            if (lunarClass == null) {
                return null;
            }

            System.out.println("[LunarUnlocker] Found Lunar class: " + lunarClass.getName());
            System.out.println("[LunarUnlocker] Attempting to get singleton instance...");

            // Try getInstance() method
            try {
                System.out.println("[LunarUnlocker] Trying getInstance() method...");
                Method getInstance = lunarClass.getMethod("getInstance");
                Object instance = getInstance.invoke(null);
                if (instance != null) {
                    System.out.println("[LunarUnlocker] Got instance via getInstance(): " + instance.getClass().getName());
                    return instance;
                }
            } catch (NoSuchMethodException | SecurityException e) {
                System.out.println("[LunarUnlocker] getInstance() method not found");
            }

            // Try INSTANCE field
            try {
                System.out.println("[LunarUnlocker] Trying INSTANCE field...");
                Field instanceField = lunarClass.getField("INSTANCE");
                Object instance = instanceField.get(null);
                if (instance != null) {
                    System.out.println("[LunarUnlocker] Got instance via INSTANCE field: " + instance.getClass().getName());
                    return instance;
                }
            } catch (NoSuchFieldException e) {
                System.out.println("[LunarUnlocker] INSTANCE field not found");
            }

            // Try instance field (lowercase)
            try {
                System.out.println("[LunarUnlocker] Trying instance field...");
                Field instanceField = lunarClass.getField("instance");
                Object instance = instanceField.get(null);
                if (instance != null) {
                    System.out.println("[LunarUnlocker] Got instance via instance field: " + instance.getClass().getName());
                    return instance;
                }
            } catch (NoSuchFieldException e) {
                System.out.println("[LunarUnlocker] instance field not found");
            }

            // Try getDeclaredField for private fields
            try {
                System.out.println("[LunarUnlocker] Trying private INSTANCE field...");
                Field instanceField = lunarClass.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                Object instance = instanceField.get(null);
                if (instance != null) {
                    System.out.println("[LunarUnlocker] Got instance via private INSTANCE field: " + instance.getClass().getName());
                    return instance;
                }
            } catch (NoSuchFieldException e) {
                System.out.println("[LunarUnlocker] Private INSTANCE field not found");
            }

            System.out.println("[LunarUnlocker] No singleton instance found in class");
            return null;
        } catch (Exception e) {
            System.out.println("[LunarUnlocker] Exception in findLunarClientSingleton: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Aggressively search for Lunar Client instance by checking all loaded classes
     */
    private static Object findLunarClientAggressively() {
        try {
            System.out.println("[LunarUnlocker] Starting aggressive search for Lunar Client...");
            // Search through all loaded classes for Lunar-related patterns
            ClassLoader[] loaders = getClassLoaders();
            System.out.println("[LunarUnlocker] Checking " + loaders.length + " class loaders...");

            for (int i = 0; i < loaders.length; i++) {
                ClassLoader loader = loaders[i];
                String originalName = loader.getClass().getName();
                System.out.println("[LunarUnlocker] Checking loader " + (i + 1) + ": " + originalName);

                // Special handling for Lunar's custom ClassLoader
                String loaderName = originalName.toLowerCase();
                System.out.println("[LunarUnlocker] Lowercase name: " + loaderName);
                System.out.println("[LunarUnlocker] Contains 'lunar': " + loaderName.contains("lunar"));
                System.out.println("[LunarUnlocker] Contains 'moonsworth': " + loaderName.contains("moonsworth"));

                if (loaderName.contains("lunar") || loaderName.contains("moonsworth")) {
                    System.out.println("[LunarUnlocker] Found Lunar ClassLoader! Enumerating loaded classes...");

                    // Try to enumerate all loaded classes in this ClassLoader
                    Object instance = scanLoadedClasses(loader);
                    if (instance != null) {
                        System.out.println("[LunarUnlocker] SUCCESS via class enumeration!");
                        return instance;
                    }
                }

                // Try to find any class with "Lunar" in its name
                try {
                    // Common Lunar Client class patterns
                    String[] possibleClasses = {
                        "com.moonsworth.lunar.LunarClient",
                        "com.lunarclient.LunarClient",
                        "lunar.LunarClient",
                        "net.lunarclient.LunarClient",
                        "com.moonsworth.lunar.client.LunarClient",
                        "com.lunarclient.client.LunarClient"
                    };

                    for (String className : possibleClasses) {
                        try {
                            System.out.println("[LunarUnlocker] Aggressive search trying: " + className);
                            Class<?> clazz = Class.forName(className, false, loader);
                            System.out.println("[LunarUnlocker] Found class: " + className);

                            // Try all possible singleton patterns
                            Object instance = tryGetSingletonInstance(clazz);
                            if (instance != null) {
                                System.out.println("[LunarUnlocker] Aggressive search SUCCESS! Found instance: " + instance.getClass().getName());
                                return instance;
                            }
                        } catch (ClassNotFoundException e) {
                            // Continue to next class
                        } catch (Exception e) {
                            System.out.println("[LunarUnlocker] Exception checking " + className + ": " + e.getMessage());
                        }
                    }
                } catch (Exception ignored) {
                    // Continue to next loader
                }
            }

            System.out.println("[LunarUnlocker] Aggressive search found no instances");
            return null;
        } catch (Exception e) {
            System.out.println("[LunarUnlocker] Exception in aggressive search: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Scan all loaded classes in a ClassLoader looking for Lunar Client
     */
    private static Object scanLoadedClasses(ClassLoader loader) {
        try {
            // Use reflection to access the ClassLoader's loaded classes
            java.lang.reflect.Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            java.util.Vector<Class<?>> classes = (java.util.Vector<Class<?>>) classesField.get(loader);

            System.out.println("[LunarUnlocker] Found " + classes.size() + " loaded classes in Lunar ClassLoader");

            // Look for classes that might be the main LunarClient class
            int lunarClasses = 0;
            for (Class<?> clazz : classes) {
                String className = clazz.getName();

                // Count Lunar-related classes for diagnostics
                if (className.contains("lunar") || className.contains("moonsworth") || className.contains("Lunar")) {
                    lunarClasses++;

                    // Log potential main client classes
                    if (className.endsWith("LunarClient") || className.contains("LunarClient$") ||
                        className.matches(".*\\.LC$") || className.matches(".*\\.[A-Z]{2,}$")) {
                        System.out.println("[LunarUnlocker] Potential main class: " + className);

                        // Try to get singleton instance
                        Object instance = tryGetSingletonInstance(clazz);
                        if (instance != null) {
                            System.out.println("[LunarUnlocker] Found working instance from: " + className);
                            return instance;
                        }
                    }
                }
            }

            System.out.println("[LunarUnlocker] Scanned " + lunarClasses + " Lunar-related classes, no singleton found");

            // If we found many Lunar classes, try a broader search
            if (lunarClasses > 50) {
                System.out.println("[LunarUnlocker] Large Lunar codebase detected, trying broader patterns...");

                for (Class<?> clazz : classes) {
                    String className = clazz.getName();

                    // Look for any class with singleton-like fields
                    if (className.contains("lunar") || className.contains("moonsworth")) {
                        try {
                            // Check if this class has INSTANCE or getInstance
                            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
                            for (java.lang.reflect.Field field : fields) {
                                if (field.getName().equals("INSTANCE") || field.getName().equals("instance")) {
                                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                                        System.out.println("[LunarUnlocker] Found INSTANCE field in: " + className);
                                        Object instance = tryGetSingletonInstance(clazz);
                                        if (instance != null) {
                                            return instance;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Continue
                        }
                    }
                }
            }

            return null;
        } catch (NoSuchFieldException e) {
            System.out.println("[LunarUnlocker] Could not access ClassLoader.classes field (Java 9+?)");
            return null;
        } catch (Exception e) {
            System.out.println("[LunarUnlocker] Exception scanning loaded classes: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Try all possible ways to get singleton instance from a class
     */
    private static Object tryGetSingletonInstance(Class<?> clazz) {
        try {
            // Method 1: getInstance()
            try {
                Method getInstance = clazz.getMethod("getInstance");
                Object instance = getInstance.invoke(null);
                if (instance != null) return instance;
            } catch (Exception ignored) {}

            // Method 2: get()
            try {
                Method get = clazz.getMethod("get");
                Object instance = get.invoke(null);
                if (instance != null) return instance;
            } catch (Exception ignored) {}

            // Method 3: INSTANCE field
            try {
                Field field = clazz.getField("INSTANCE");
                Object instance = field.get(null);
                if (instance != null) return instance;
            } catch (Exception ignored) {}

            // Method 4: instance field
            try {
                Field field = clazz.getField("instance");
                Object instance = field.get(null);
                if (instance != null) return instance;
            } catch (Exception ignored) {}

            // Method 5: Check all static fields
            try {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        try {
                            field.setAccessible(true);
                            Object instance = field.get(null);
                            if (instance != null && instance.getClass() == clazz) {
                                return instance;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}

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
