package de.rwth_aachen.phyphox.Helper;

import de.rwth_aachen.phyphox.BuildConfig;

/**
 * VersionHelper - Utility class untuk mengelola version-aware collection names dan storage paths
 * 
 * Memastikan isolasi data antara versi aplikasi:
 * - v1.0.0: menggunakan collection names tanpa prefix (record, questions, user)
 * - v2.0.0+: menggunakan prefix "v2_" untuk collection names (v2_record, v2_questions)
 * 
 * Collection yang shared (tidak di-version):
 * - "questions": Shared untuk referensi pertanyaan
 * - "user": Shared untuk authentication dan user data
 */
public class VersionHelper {
    private static final String CURRENT_VERSION = BuildConfig.VERSION_NAME;
    private static final int CURRENT_VERSION_CODE = BuildConfig.VERSION_CODE;
    
    // Collections yang shared (tidak perlu di-version)
    private static final String[] SHARED_COLLECTIONS = {"questions", "user"};
    
    /**
     * Get collection name dengan version prefix jika perlu
     * 
     * @param baseName Nama collection base (e.g., "record", "questions")
     * @return Collection name dengan prefix version jika perlu
     * 
     * Examples:
     * - v1.0.0: "record" -> "record"
     * - v2.0.0: "record" -> "v2_record"
     * - v1.0.0: "questions" -> "questions" (shared)
     * - v2.0.0: "questions" -> "questions" (shared)
     */
    public static String getCollectionName(String baseName) {
        // Collection shared tidak perlu di-version
        for (String shared : SHARED_COLLECTIONS) {
            if (shared.equals(baseName)) {
                return baseName;
            }
        }
        
        // Versi 2.0.0+ menggunakan prefix "v2_"
        if (CURRENT_VERSION_CODE >= 2) {
            return "v2_" + baseName;
        }
        
        // Versi 1.0.0 tetap tanpa prefix
        return baseName;
    }
    
    /**
     * Get storage path dengan version prefix jika perlu
     * 
     * @param basePath Path base (e.g., "documentation", "photos")
     * @return Storage path dengan prefix version jika perlu
     * 
     * Examples:
     * - v1.0.0: "documentation" -> "documentation"
     * - v2.0.0: "documentation" -> "v2_documentation"
     */
    public static String getStoragePath(String basePath) {
        // Path "questions" tetap shared (tidak di-version)
        if ("questions".equals(basePath)) {
            return basePath;
        }
        
        // Versi 2.0.0+ menggunakan prefix "v2_"
        if (CURRENT_VERSION_CODE >= 2) {
            return "v2_" + basePath;
        }
        
        // Versi 1.0.0 tetap tanpa prefix
        return basePath;
    }
    
    /**
     * Check if current version is v2 or higher
     * 
     * @return true jika version code >= 2
     */
    public static boolean isVersion2() {
        return CURRENT_VERSION_CODE >= 2;
    }
    
    /**
     * Get current version name (e.g., "1.0.0", "2.0.0")
     * 
     * @return Version name string
     */
    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }
    
    /**
     * Get current version code (e.g., 1, 2)
     * 
     * @return Version code integer
     */
    public static int getCurrentVersionCode() {
        return CURRENT_VERSION_CODE;
    }
    
    /**
     * Check if a collection is shared (tidak di-version)
     * 
     * @param collectionName Nama collection
     * @return true jika collection adalah shared
     */
    public static boolean isSharedCollection(String collectionName) {
        for (String shared : SHARED_COLLECTIONS) {
            if (shared.equals(collectionName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get base collection name dari versioned collection name
     * 
     * @param versionedName Versioned collection name (e.g., "v2_record")
     * @return Base collection name (e.g., "record")
     */
    public static String getBaseCollectionName(String versionedName) {
        if (versionedName.startsWith("v2_")) {
            return versionedName.substring(3); // Remove "v2_" prefix
        }
        return versionedName;
    }
}

