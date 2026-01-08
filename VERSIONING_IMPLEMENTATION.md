# Versioning Implementation - G-Physolve

## Overview
Implementasi sistem versioning untuk memisahkan data antara versi 1.0.0 dan 2.0.0 aplikasi, memastikan bahwa perubahan di v2 tidak mengganggu v1.

## Strategi
Menggunakan **1 Firebase Project** dengan collection prefix untuk isolasi data:
- **v1.0.0**: Collection names tanpa prefix (`record`, `questions`, `user`)
- **v2.0.0+**: Collection names dengan prefix `v2_` (`v2_record`, `v2_questions`)
- **Shared Collections**: `questions` dan `user` tetap shared (tidak di-version)

## File yang Diubah

### 1. VersionHelper.java (NEW)
**Location**: `app/src/main/java/de/rwth_aachen/phyphox/Helper/VersionHelper.java`

Utility class untuk mengelola version-aware collection names dan storage paths.

**Key Methods**:
- `getCollectionName(String baseName)`: Mengembalikan collection name dengan prefix version jika perlu
- `getStoragePath(String basePath)`: Mengembalikan storage path dengan prefix version jika perlu
- `isVersion2()`: Check jika current version adalah v2 atau lebih tinggi
- `getCurrentVersion()`: Mengembalikan version name (e.g., "2.0.0")
- `getCurrentVersionCode()`: Mengembalikan version code (e.g., 2)

**Shared Collections**:
- `questions`: Shared untuk referensi pertanyaan (tidak di-version)
- `user`: Shared untuk authentication (tidak di-version)

### 2. FirestoreUtil.java (UPDATED)
**Location**: `app/src/main/java/de/rwth_aachen/phyphox/Helper/FirestoreUtil.java`

Semua method yang menggunakan collection names sekarang menggunakan `VersionHelper.getCollectionName()` secara internal.

**Updated Methods**:
- `addOrUpdateDocumentWithBackup()`: Version-aware collection names
- `addOrUpdateDocument()`: Version-aware collection names
- `addOrUpdateDocumentWithVersioning()`: Version-aware collection names + menambahkan field `appVersion`
- `addDocument()`: Version-aware collection names
- `getDocument()`: Version-aware collection names
- `getAllDocuments()`: Version-aware collection names
- `getAllDocumentsWithRetry()`: Version-aware collection names
- `backupCollection()`: Version-aware collection names
- `restoreFromBackup()`: Version-aware collection names

**Note**: Semua method menerima **base collection name** (e.g., "record", "questions") dan secara otomatis mengkonversi ke versioned name jika perlu.

### 3. StorageUtil.java (UPDATED)
**Location**: `app/src/main/java/de/rwth_aachen/phyphox/Helper/StorageUtil.java`

Storage paths sekarang menggunakan `VersionHelper.getStoragePath()`.

**Changes**:
- Constants diubah menjadi private base names
- Ditambahkan getter methods yang version-aware:
  - `getStoragePathPhotos()`
  - `getStoragePathDrawings()`
  - `getStoragePathDocumentation()`
  - `getStoragePathNotes()`
  - `getStoragePathExperiments()`
- Legacy constants tetap ada untuk backward compatibility (deprecated)

**Updated Methods**:
- `uploadPhotoDocumentation()`: Menggunakan `getStoragePathDocumentation()`
- `uploadNotePhoto()`: Menggunakan `getStoragePathNotes()`
- `uploadExperimentPhotoWithSource()`: Menggunakan `getStoragePathExperiments()`

### 4. GeneratesActivity.java (UPDATED)
**Location**: `app/src/main/java/de/rwth_aachen/phyphox/activity/GeneratesActivity.java`

**Changes**:
- Menambahkan import `VersionHelper`
- Update direct collection access untuk "user" collection menggunakan `VersionHelper.getCollectionName("user")`

### 5. GeneratesClassQuestionActivity.java (UPDATED)
**Location**: `app/src/main/java/de/rwth_aachen/phyphox/activity/GeneratesClassQuestionActivity.java`

**Changes**:
- Menambahkan import `VersionHelper`
- Update direct collection access untuk "user" collection menggunakan `VersionHelper.getCollectionName("user")`

### 6. build.gradle (UPDATED)
**Location**: `app/build.gradle`

**Changes**:
```gradle
versionName "2.0.0"
versionCode 2
```

## Struktur Data

### Firestore Collections

#### v1.0.0
```
record/              (v1 data)
questions/           (shared - referensi)
user/                (shared - authentication)
```

#### v2.0.0
```
v2_record/           (v2 data - TERISOLASI)
questions/           (shared - referensi)
user/                (shared - authentication)
```

### Firebase Storage Paths

#### v1.0.0
```
documentation/       (v1 files)
photos/              (v1 files)
drawings/            (v1 files)
notes/               (v1 files)
experiments/         (v1 files)
questions/           (shared - referensi images)
```

#### v2.0.0
```
v2_documentation/    (v2 files - TERISOLASI)
v2_photos/           (v2 files - TERISOLASI)
v2_drawings/         (v2 files - TERISOLASI)
v2_notes/            (v2 files - TERISOLASI)
v2_experiments/      (v2 files - TERISOLASI)
questions/           (shared - referensi images)
```

## Field appVersion

Setiap document baru di v2.0.0 akan memiliki field `appVersion`:
```json
{
  "appVersion": "2.0.0",
  "versionCode": 2,
  ...
}
```

Field ini ditambahkan secara otomatis oleh `addOrUpdateDocumentWithVersioning()`.

## Cara Penggunaan

### Untuk Developer

#### Menggunakan FirestoreUtil
```java
// Gunakan base collection name, VersionHelper akan handle versioning secara otomatis
FirestoreUtil.addOrUpdateDocument(
    "record",  // Base name, akan jadi "v2_record" di v2.0.0
    documentId,
    data,
    () -> { /* success */ },
    e -> { /* failure */ }
);
```

#### Menggunakan StorageUtil
```java
// Gunakan getter methods yang version-aware
String path = StorageUtil.getStoragePathDocumentation();  // "v2_documentation" di v2.0.0
StorageUtil.uploadBytesWithUserIdAndMetadata(
    context,
    data,
    path,
    fileName,
    source,
    userName,
    onSuccess,
    onFailure,
    null
);
```

#### Direct Collection Access (TIDAK DISARANKAN)
```java
// Jika harus menggunakan direct access, gunakan VersionHelper
String collection = VersionHelper.getCollectionName("record");
firestore.collection(collection)...
```

### Untuk Query Data

#### Query v2 Data
```java
// Di v2.0.0, query akan otomatis ke "v2_record"
FirestoreUtil.getAllDocuments(
    "record",  // Base name
    DataModel.class,
    data -> { /* handle data */ },
    e -> { /* handle error */ }
);
```

#### Query Shared Collections
```java
// Collection "questions" dan "user" tetap shared
FirestoreUtil.getAllDocuments(
    "questions",  // Tidak di-version, tetap "questions"
    DataModel.class,
    data -> { /* handle data */ },
    e -> { /* handle error */ }
);
```

## Isolasi Data

### ✅ Yang Terisolasi
1. **Collection `record`**: v1 menggunakan `record`, v2 menggunakan `v2_record`
2. **Storage paths**: v1 menggunakan `documentation/`, v2 menggunakan `v2_documentation/`
3. **Data tidak bercampur**: v1 dan v2 memiliki data terpisah

### ✅ Yang Shared
1. **Collection `questions`**: Shared untuk referensi pertanyaan
2. **Collection `user`**: Shared untuk authentication
3. **Storage path `questions/`**: Shared untuk referensi images

## Backward Compatibility

### v1.0.0
- Tetap menggunakan collection names tanpa prefix
- Tidak ada perubahan behavior
- Data tetap di collection `record`, `questions`, `user`

### v2.0.0
- Menggunakan collection names dengan prefix `v2_`
- Data baru disimpan di `v2_record`
- Bisa membaca data dari `questions` (shared)

## Migration Path

Jika perlu migrate data dari v1 ke v2:
```java
// Load dari v1
FirestoreUtil.getAllDocuments("record", DataModel.class, v1Data -> {
    // Save ke v2
    for (DataModel data : v1Data) {
        // Update appVersion
        data.setAppVersion("2.0.0");
        // Save ke v2_record (akan otomatis karena version code >= 2)
        FirestoreUtil.addOrUpdateDocument("record", data.getId(), data, ...);
    }
}, ...);
```

## Testing

### Test v1.0.0
1. Set `versionCode = 1` di `build.gradle`
2. Build dan test aplikasi
3. Verify data disimpan di collection `record` (tanpa prefix)

### Test v2.0.0
1. Set `versionCode = 2` di `build.gradle`
2. Build dan test aplikasi
3. Verify data disimpan di collection `v2_record` (dengan prefix)
4. Verify data v1 tidak terpengaruh

## Checklist

- [x] VersionHelper class dibuat
- [x] FirestoreUtil updated untuk version-aware
- [x] StorageUtil updated untuk version-aware
- [x] GeneratesActivity updated
- [x] GeneratesClassQuestionActivity updated
- [x] build.gradle updated ke version 2.0.0
- [ ] Test v1.0.0 (set versionCode = 1)
- [ ] Test v2.0.0 (set versionCode = 2)
- [ ] Verify isolasi data
- [ ] Verify shared collections bekerja

## Notes

1. **Tidak perlu mengubah semua file**: Karena FirestoreUtil dan StorageUtil sudah handle versioning secara internal, file-file yang menggunakan utility classes ini tidak perlu diubah.

2. **Direct collection access**: Hanya file yang menggunakan direct collection access (tidak melalui FirestoreUtil) yang perlu diupdate.

3. **Shared collections**: Collection `questions` dan `user` tetap shared untuk memudahkan referensi dan authentication.

4. **Storage paths**: Path `questions/` tetap shared untuk referensi images, path lainnya di-version.

5. **Field appVersion**: Field ini ditambahkan secara otomatis untuk tracking version document.

## Future Considerations

1. **v3.0.0**: Jika perlu versi baru, cukup update `versionCode >= 3` dan VersionHelper akan menggunakan prefix `v3_`

2. **Migration tools**: Bisa dibuat tool untuk migrate data antar versi jika diperlukan

3. **Admin portal**: Admin portal perlu diupdate untuk query data dari multiple versions jika diperlukan

