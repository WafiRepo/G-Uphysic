# Migration Guide - Menggunakan Schema Baru dengan Versioning

## Overview
Schema baru dengan versioning dan history tracking sudah siap digunakan. Ini akan mencegah data hilang saat update.

## Perubahan Utama

### 1. DataModel - Field Baru
Field versioning sudah ditambahkan:
- `version` - Nomor versi (auto increment)
- `status` - Status record (active, archived, deleted)
- `createdAt` - Timestamp saat dibuat
- `updatedAt` - Timestamp saat terakhir di-update
- `createdAtFormatted` - Format tanggal string
- `updatedAtFormatted` - Format tanggal string
- `lastHistoryId` - ID history terakhir

### 2. FirestoreUtil - Method Baru
Method baru: `addOrUpdateDocumentWithVersioning()`

**Method Lama (Masih Bisa Digunakan):**
```java
FirestoreUtil.addOrUpdateDocument("record", dataModel.getId(), dataModel,
    () -> {
        // Success
    },
    e -> {
        // Error
    }
);
```

**Method Baru (Recommended):**
```java
String userId = SessionManager.getId(context);
String userName = SessionManager.getName(context);

FirestoreUtil.addOrUpdateDocumentWithVersioning(
    "record",                    // Collection name
    dataModel.getId(),           // Document ID
    dataModel,                   // DataModel object
    userId,                      // User ID
    userName,                    // User Name
    () -> {
        // Success callback
        Log.d("SAVE", "Data saved with versioning");
    },
    e -> {
        // Error callback
        Log.e("SAVE", "Error: " + e.getMessage());
    }
);
```

## Cara Migrasi

### Step 1: Update RecordPreviewActivity

**Sebelum:**
```java
FirestoreUtil.addOrUpdateDocument("record", dataModel.getId(), dataModel,
    () -> {
        Log.d("Location", "Location saved successfully");
        Toast.makeText(this, "📍 Lokasi tersimpan", Toast.LENGTH_SHORT).show();
    },
    e -> {
        Log.e("Location", "Error saving location", e);
        Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
);
```

**Sesudah:**
```java
String userId = SessionManager.getId(this);
String userName = SessionManager.getName(this);

FirestoreUtil.addOrUpdateDocumentWithVersioning(
    "record",
    dataModel.getId(),
    dataModel,
    userId,
    userName,
    () -> {
        Log.d("Location", "Location saved successfully with versioning");
        Toast.makeText(this, "📍 Lokasi tersimpan", Toast.LENGTH_SHORT).show();
    },
    e -> {
        Log.e("Location", "Error saving location", e);
        Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
);
```

### Step 2: Update GeneratesActivity

**Sebelum:**
```java
FirestoreUtil.addOrUpdateDocumentWithBackup(
    "record",
    "record_backup",
    documentId,
    dataModel,
    () -> {
        // Success
    },
    e -> {
        // Error
    }
);
```

**Sesudah:**
```java
String userId = SessionManager.getId(this);
String userName = SessionManager.getName(this);

FirestoreUtil.addOrUpdateDocumentWithVersioning(
    "record",
    documentId,
    dataModel,
    userId,
    userName,
    () -> {
        // Success - history sudah otomatis tersimpan
    },
    e -> {
        // Error
    }
);
```

### Step 3: Update Activity Lainnya

Cari semua penggunaan `addOrUpdateDocument` dan ganti dengan `addOrUpdateDocumentWithVersioning`:

**File yang perlu di-update:**
- `RecordPreviewActivity.java`
- `GeneratesActivity.java`
- `GeneratesClassQuestionActivity.java`
- `UserGeneratesQuestionActivity.java`
- `ImageCarouselActivity.java`
- `MapsActivity.java`

## Keuntungan Schema Baru

### 1. Data Tidak Hilang
- Data lama tersimpan di collection `record_history`
- Setiap perubahan memiliki snapshot lengkap
- Bisa rollback ke versi sebelumnya

### 2. Audit Trail
- Track siapa yang mengubah data
- Track kapan data diubah
- Track field apa yang berubah

### 3. Versioning
- Setiap update increment version
- Bisa lihat history perubahan
- Bisa restore versi lama

### 4. Backward Compatible
- Field lama tetap ada
- Method lama masih bisa digunakan
- Tidak breaking change

## Collection Structure

### Main Collection: `record`
- Document ID: `{recordId}`
- Berisi data terbaru
- Field versioning otomatis di-update

### History Collection: `record_history`
- Document ID: `history_{recordId}_{version}`
- Berisi snapshot setiap perubahan
- Bisa query history berdasarkan recordId

## Contoh Query History

```java
// Get all history for a record
FirestoreUtil.getAllDocuments(
    "record_history",
    HistoryModel.class,
    "recordId",
    recordId,
    historyList -> {
        // Process history
        for (HistoryModel history : historyList) {
            Log.d("HISTORY", "Version: " + history.getVersion() + 
                  ", Changed by: " + history.getUpdatedBy().getUserName() +
                  ", At: " + history.getCreatedAtFormatted());
        }
    },
    e -> {
        Log.e("HISTORY", "Error: " + e.getMessage());
    }
);
```

## Testing

1. **Test Create**: Buat record baru, cek version = 1
2. **Test Update**: Update record, cek version increment
3. **Test History**: Cek collection `record_history` ada entry baru
4. **Test Merge**: Update sebagian field, cek field lain tidak hilang
5. **Test Rollback**: Load history dan restore versi lama

## Notes

- Method lama `addOrUpdateDocument()` masih bisa digunakan untuk backward compatibility
- Method baru `addOrUpdateDocumentWithVersioning()` recommended untuk semua save operation baru
- History collection otomatis dibuat saat pertama kali save
- Tidak perlu migration data existing, field versioning akan diisi otomatis saat pertama kali di-update

