# Schema Design - G-Physolve Database

## Masalah yang Diatasi
- Data lama hilang saat update (karena merge tidak sempurna)
- Tidak ada tracking perubahan data
- Tidak ada history/audit trail
- Tidak ada versioning system

## Solusi: Schema dengan Versioning & History Tracking

### 1. Collection Structure

#### Collection: `record` (Main Collection)
Document ID: `{recordId}` (unique, tidak berubah)

```json
{
  // ===== IDENTIFIER & METADATA =====
  "id": "1234567890",                    // Primary ID (unique, tidak berubah)
  "version": 5,                          // Current version number
  "status": "active",                    // active, archived, deleted
  
  // ===== TIMESTAMPS =====
  "createdAt": 1234567890000,            // Timestamp saat pertama dibuat
  "updatedAt": 1234567895000,            // Timestamp saat terakhir di-update
  "createdAtFormatted": "2024-01-15 10:30:00",
  "updatedAtFormatted": "2024-01-15 11:45:00",
  
  // ===== USER TRACKING =====
  "createdBy": {
    "userId": "user123",
    "userName": "John Doe",
    "timestamp": 1234567890000
  },
  "updatedBy": {
    "userId": "user456",
    "userName": "Jane Smith",
    "timestamp": 1234567895000
  },
  "idCustomer": "user123",               // Owner/Creator ID
  "customerName": "John Doe",            // Owner/Creator Name
  
  // ===== QUESTION DATA =====
  "question": "Pertanyaan fisika...",
  "typeData": "Experiment",
  "typeQuestion": "Mechanics",
  "topics": "Kinematics",
  "desc": "Deskripsi experiment...",
  
  // ===== QUESTION IMAGES (Preferred: URLs) =====
  "questionImages": {
    "image1": {
      "url": "https://firebase.../image1.jpg",
      "path": "users/user123/questions/image1.jpg",
      "uploadedAt": 1234567890000,
      "uploadedBy": "user123"
    },
    "image2": {
      "url": "https://firebase.../image2.jpg",
      "path": "users/user123/questions/image2.jpg",
      "uploadedAt": 1234567891000,
      "uploadedBy": "user123"
    }
    // ... image3, image4, image5
  },
  
  // ===== LEGACY BASE64 (Deprecated, untuk backward compatibility) =====
  "base64": "",                          // Legacy - akan dihapus
  "base64_2": "",
  "base64_3": "",
  "base64_4": "",
  "base64_5": "",
  
  // ===== ANSWER DATA =====
  "photoAnswer": {
    "url": "https://firebase.../answer.jpg",
    "path": "users/user123/answers/answer.jpg",
    "uploadedAt": 1234567892000,
    "uploadedBy": "user123"
  },
  "photoAnswerUrl": "https://firebase.../answer.jpg",  // Legacy field
  "photoAnswerPath": "users/user123/answers/answer.jpg",
  
  // ===== CANVAS DRAWINGS =====
  "photoDraw": [
    {
      "url": "https://firebase.../canvas1.jpg",
      "path": "users/user123/canvas/canvas1.jpg",
      "uploadedAt": 1234567893000,
      "uploadedBy": "user123"
    }
  ],
  
  // ===== LOCATION DATA =====
  "location": {
    "latitude": -6.2088,
    "longitude": 106.8456,
    "locationName": "Jakarta, Indonesia",
    "updatedAt": 1234567894000,
    "updatedBy": "user123"
  },
  "latitude": -6.2088,                   // Legacy field
  "longitude": 106.8456,                 // Legacy field
  "locationName": "Jakarta, Indonesia",  // Legacy field
  
  // ===== EXPERIMENT DATA =====
  "photoAcceleration": "",
  "valueAcceleration": "",
  "photo": "",
  
  // ===== DOCUMENTATION =====
  "documentation": {
    "photos": [
      {
        "url": "https://firebase.../doc1.jpg",
        "path": "users/user123/docs/doc1.jpg",
        "uploadedAt": 1234567895000,
        "uploadedBy": "user123",
        "note": "Setup experiment"
      }
    ],
    "notes": "Documentation notes...",
    "lastUpdated": 1234567895000
  },
  
  // ===== STATISTICS =====
  "totalEdit": 5,
  "views": 100,
  "isFinished": false,
  
  // ===== ADMIN TRACKING =====
  "creatorName": "John Doe",
  "dibuatOleh": "John Doe",
  "sourceQuestionId": "",                // Jika ini copy dari question lain
  
  // ===== VERSION HISTORY REFERENCE =====
  "historyCollection": "record_history",  // Collection untuk history
  "lastHistoryId": "history_1234567895"  // ID history terakhir
}
```

#### Collection: `record_history` (History/Audit Trail)
Document ID: `history_{recordId}_{version}`

```json
{
  // ===== IDENTIFIER =====
  "historyId": "history_1234567890_5",
  "recordId": "1234567890",              // Reference ke record utama
  "version": 5,                          // Version number
  
  // ===== TIMESTAMPS =====
  "createdAt": 1234567895000,
  "createdAtFormatted": "2024-01-15 11:45:00",
  
  // ===== USER TRACKING =====
  "updatedBy": {
    "userId": "user456",
    "userName": "Jane Smith",
    "timestamp": 1234567895000
  },
  
  // ===== CHANGE TRACKING =====
  "changeType": "update",                 // create, update, delete, restore
  "changedFields": [                      // Field yang berubah
    "question",
    "questionImages.image2",
    "location"
  ],
  "changeDescription": "Updated question and added new image",
  
  // ===== DATA SNAPSHOT (Full data saat perubahan) =====
  "dataSnapshot": {
    // Full DataModel object saat perubahan
    "question": "Pertanyaan baru...",
    "questionImages": { ... },
    // ... semua field
  },
  
  // ===== PREVIOUS VALUES (Untuk rollback) =====
  "previousValues": {
    "question": "Pertanyaan lama...",
    "questionImages": { ... }
  }
}
```

#### Collection: `questions` (Question List)
Struktur sama dengan `record`, tapi untuk question list khusus.

### 2. Update Strategy

#### Saat Update Data:
1. **Load existing document** dari Firestore
2. **Increment version** (version + 1)
3. **Save history** ke `record_history` dengan:
   - Full snapshot data baru
   - Previous values yang berubah
   - Change tracking
4. **Merge data baru** dengan data existing (preserve fields yang tidak diubah)
5. **Update timestamps** (updatedAt, updatedBy)
6. **Save ke main collection** dengan SetOptions.merge()

#### Saat Create Data:
1. **Generate unique ID** (timestamp atau UUID)
2. **Set version = 1**
3. **Set timestamps** (createdAt, updatedAt)
4. **Save ke main collection**
5. **Save history** dengan changeType = "create"

### 3. Field Mapping (Backward Compatibility)

Field lama tetap ada untuk backward compatibility, tapi akan diisi dari struktur baru:

```java
// Old field → New structure
base64 → questionImages.image1.url (jika ada)
base64_2 → questionImages.image2.url (jika ada)
photoAnswer → photoAnswer.url
photoAnswerUrl → photoAnswer.url
latitude → location.latitude
longitude → location.longitude
```

### 4. Benefits

✅ **Data tidak hilang**: History tersimpan di collection terpisah
✅ **Audit trail**: Bisa track siapa, kapan, apa yang diubah
✅ **Versioning**: Bisa rollback ke version sebelumnya
✅ **Backward compatible**: Field lama tetap ada
✅ **Better structure**: Data lebih terorganisir dengan nested objects
✅ **Performance**: Bisa query history tanpa load full document

### 5. Migration Strategy

1. **Phase 1**: Tambah field baru (version, timestamps, dll) ke existing documents
2. **Phase 2**: Migrate data ke struktur baru (questionImages, location object, dll)
3. **Phase 3**: Update code untuk menggunakan struktur baru
4. **Phase 4**: Deprecate field lama (tapi tetap support untuk backward compatibility)

