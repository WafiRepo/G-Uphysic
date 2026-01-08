# Simplified History - Semua di Satu Document

## Perubahan

### Sebelum (Collection Terpisah)
- History disimpan di collection terpisah: `record_history`
- Setiap perubahan = 1 document baru di collection history
- Perlu query 2 collection untuk dapat data + history
- Bisa ada banyak versi history (3, 5, 10, dll)

### Sesudah (Array di Document Sama)
- History disimpan sebagai **array di dalam document yang sama**
- Semua data + history di **satu document saja**
- Tidak perlu collection terpisah
- History dibatasi **maksimal 10 entries** (auto remove yang paling lama)

## Struktur Baru

### Document Structure
```json
{
  "id": "1234567890",
  "version": 5,
  "status": "active",
  "createdAt": 1234567890000,
  "updatedAt": 1234567895000,
  "question": "Pertanyaan...",
  "locationName": "Jakarta",
  // ... semua field data ...
  
  // ===== HISTORY ARRAY (di dalam document yang sama) =====
  "history": [
    {
      "version": 5,
      "changeType": "update",
      "timestamp": 1234567895000,
      "timestampFormatted": "2024-01-15 11:45:00",
      "userId": "user456",
      "userName": "Jane Smith",
      "changedFields": {
        "locationName": "Jakarta Baru",
        "latitude": -6.2088
      }
    },
    {
      "version": 4,
      "changeType": "update",
      "timestamp": 1234567894000,
      "timestampFormatted": "2024-01-15 10:30:00",
      "userId": "user123",
      "userName": "John Doe",
      "changedFields": {
        "question": "Pertanyaan baru"
      }
    },
    // ... max 10 entries, oldest removed automatically
    {
      "version": 1,
      "changeType": "create",
      "timestamp": 1234567890000,
      "timestampFormatted": "2024-01-15 09:00:00",
      "userId": "user123",
      "userName": "John Doe",
      "changedFields": {
        "created": true
      }
    }
  ]
}
```

## Keuntungan

✅ **Lebih Sederhana**: Semua data di satu tempat
✅ **Lebih Efisien**: Tidak perlu query 2 collection
✅ **Lebih Cepat**: Load sekali dapat semua (data + history)
✅ **Auto Limit**: Maksimal 10 history entries (prevent document terlalu besar)
✅ **Tidak Ada Collection Terpisah**: Tidak perlu `record_history` collection

## Cara Kerja

### Saat Update:
1. Load existing document
2. Merge new data dengan existing
3. Increment version
4. **Add history entry ke array** (max 10, auto remove oldest)
5. Save document dengan history array

### History Entry Format:
```java
{
  "version": 5,                    // Version number
  "changeType": "update",         // "create" atau "update"
  "timestamp": 1234567895000,     // Unix timestamp
  "timestampFormatted": "...",    // Formatted string
  "userId": "user456",            // User ID yang mengubah
  "userName": "Jane Smith",       // User name yang mengubah
  "changedFields": {              // Field yang berubah
    "locationName": "Jakarta",
    "latitude": -6.2088
  }
}
```

## Limit History

History dibatasi **maksimal 10 entries** untuk:
- Mencegah document terlalu besar (Firestore limit 1MB)
- Keep only recent changes (last 10 updates)
- Oldest entry auto-removed saat add new entry

## Migration

Tidak perlu migration! Collection `record_history` lama bisa diabaikan atau dihapus. History baru akan tersimpan di array `history` di dalam document.

## Contoh Query

### Get Document dengan History
```java
FirestoreUtil.getDocument("record", documentId, DataModel.class,
    data -> {
        // Data sudah include history array
        ArrayList<Map<String, Object>> history = data.getHistory();
        for (Map<String, Object> entry : history) {
            Log.d("HISTORY", "Version: " + entry.get("version") + 
                  ", Changed by: " + entry.get("userName") +
                  ", At: " + entry.get("timestampFormatted"));
        }
    },
    e -> { /* error */ }
);
```

## Perbandingan

| Aspek | Collection Terpisah | Array di Document |
|-------|-------------------|-------------------|
| Jumlah Collection | 2 (record + record_history) | 1 (record saja) |
| Query | 2 queries | 1 query |
| Performance | Lebih lambat | Lebih cepat |
| Complexity | Lebih kompleks | Lebih sederhana |
| History Limit | Tidak terbatas | Max 10 entries |
| Document Size | Kecil | Sedang (tapi masih aman) |

## Kesimpulan

✅ **Lebih sederhana**: Semua di satu document
✅ **Lebih efisien**: Tidak perlu multiple queries
✅ **Lebih praktis**: History terbatas 10 entries (cukup untuk tracking)
✅ **Tidak ada collection terpisah**: Semua data + history di satu tempat

