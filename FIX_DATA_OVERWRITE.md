# Fix: Data Lama Tertimpa oleh Data Baru

## Masalah
Setiap ada data baru datang, data lama tertimpa dan hilang.

## Penyebab
1. **Method merge tidak lengkap** - Hanya beberapa field yang di-merge
2. **Empty string menimpa existing** - Field kosong di newData menimpa field yang ada
3. **SetOptions.merge() behavior** - Empty string tetap di-merge dan menimpa existing

## Solusi yang Sudah Diterapkan

### 1. Perbaikan Method `mergeDataModel()`
- ✅ Sekarang merge SEMUA field penting
- ✅ Hanya update field yang punya value (non-empty)
- ✅ Preserve field yang tidak di-update
- ✅ Append untuk array (photoDraw, photoDocumentation) bukan replace

### 2. Method `cleanEmptyFields()`
- ✅ Set empty string menjadi null sebelum save
- ✅ Mencegah empty string menimpa existing data
- ✅ Firestore merge akan skip null fields

### 3. Logging
- ✅ Tambah logging untuk tracking merge process
- ✅ Bisa lihat field mana yang di-update

## Cara Kerja Sekarang

### Saat Update:
1. **Load existing document** dari Firestore
2. **Merge newData ke existing** - Hanya field non-empty yang di-update
3. **Clean empty fields** - Set empty string jadi null
4. **Save dengan SetOptions.merge()** - Null fields tidak akan menimpa existing

### Contoh:
```java
// Existing data
{
  "question": "Pertanyaan lama",
  "base64": "data_lama",
  "questionImageUrl1": "url_lama"
}

// New data (hanya update location)
{
  "locationName": "Jakarta Baru",
  "base64": "",  // Empty - akan jadi null
  "questionImageUrl1": ""  // Empty - akan jadi null
}

// Hasil setelah merge
{
  "question": "Pertanyaan lama",  // ✅ PRESERVED
  "base64": "data_lama",          // ✅ PRESERVED (karena newData.base64 jadi null)
  "questionImageUrl1": "url_lama", // ✅ PRESERVED (karena newData.questionImageUrl1 jadi null)
  "locationName": "Jakarta Baru"   // ✅ UPDATED
}
```

## Field yang Di-Preserve

Semua field berikut akan di-preserve jika newData tidak punya value:
- ✅ `question`, `typeData`, `desc`, `topics`
- ✅ `base64`, `base64_2`, `base64_3`, `base64_4`, `base64_5`
- ✅ `questionImageUrl1-5`, `questionImagePath1-5`
- ✅ `photoAnswer`, `photoAnswerUrl`, `photoAnswerPath`
- ✅ `photoDraw` (append, tidak replace)
- ✅ `photoDocumentation` (append, tidak replace)
- ✅ `locationName`, `latitude`, `longitude`
- ✅ `photoAcceleration`, `valueAcceleration`
- ✅ `creatorName`, `dibuatOleh`, `createdBy`
- ✅ `views` (preserve jika lebih besar)
- ✅ Semua field versioning

## Testing

### Test Case 1: Update Location Only
```java
DataModel newData = new DataModel();
newData.setLocationName("Jakarta");
newData.setLatitude(-6.2088);
newData.setLongitude(106.8456);
// Field lain kosong/null

// Expected: Hanya location yang di-update, field lain preserved
```

### Test Case 2: Update Question Image
```java
DataModel newData = new DataModel();
newData.setQuestionImageUrl1("new_url");
// Field lain kosong/null

// Expected: Hanya questionImageUrl1 yang di-update, field lain preserved
```

### Test Case 3: Append PhotoDraw
```java
DataModel newData = new DataModel();
ArrayList<String> newDraws = new ArrayList<>();
newDraws.add("new_canvas_url");
newData.setPhotoDraw(newDraws);
// Field lain kosong/null

// Expected: new_canvas_url ditambahkan ke existing photoDraw, tidak replace
```

## Catatan Penting

⚠️ **Masih ada code yang menggunakan method lama `addOrUpdateDocument()`**

File yang perlu di-update:
- `GeneratesActivity.java`
- `GeneratesClassQuestionActivity.java`
- `UserGeneratesQuestionActivity.java`
- `ImageCarouselActivity.java`
- `MapsActivity.java`

**Action Required**: Ganti semua `addOrUpdateDocument()` dengan `addOrUpdateDocumentWithVersioning()` untuk mencegah data hilang.

## Monitoring

Cek log dengan tag `FirestoreUtil` untuk melihat:
- Field mana yang di-update
- Field mana yang di-preserve
- Proses merge

```bash
adb logcat | grep FirestoreUtil
```

