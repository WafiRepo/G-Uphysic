# Database Recovery System - G-Physolve

## Overview
Sistem recovery database ini dirancang untuk mengatasi masalah hilangnya data history dari page progress dan Firestore. Sistem ini menyediakan mekanisme backup otomatis, data recovery, dan validasi integritas data.

## Masalah yang Diatasi

### 1. Data History Hilang
- **Penyebab**: Inkonsistensi dalam penyimpanan data antara collection `record` dan `questions`
- **Solusi**: Sistem backup otomatis dan sinkronisasi data antar collection

### 2. Filtering Issues
- **Penyebab**: Data tidak ter-load karena masalah dengan `idCustomer` atau SessionManager
- **Solusi**: Fallback loading mechanism dan data validation

### 3. Data Corruption
- **Penyebab**: Document dengan field yang hilang atau tidak valid
- **Solusi**: Data integrity validation dan automatic repair

## Komponen Sistem

### 1. Enhanced FirestoreUtil (`app/src/main/java/de/rwth_aachen/phyphox/Helper/FirestoreUtil.java`)

#### Fitur Baru:
- **Backup System**: `addOrUpdateDocumentWithBackup()` - Menyimpan data ke primary dan backup collection
- **Retry Mechanism**: Automatic retry untuk operasi yang gagal (3x attempt)
- **Batch Operations**: Backup dan restore dalam batch untuk performa optimal
- **Multi-Collection Support**: Load data dari multiple collections sekaligus

#### Method Utama:
```java
// Backup document dengan dual storage
addOrUpdateDocumentWithBackup(primaryCollection, backupCollection, documentId, data, onSuccess, onFailure)

// Load dari multiple collections
getAllDocumentsFromMultipleCollections(collections, clazz, field, value, onSuccess, onFailure)

// Backup entire collection
backupCollection(sourceCollection, backupCollection, clazz, onSuccess, onFailure)

// Restore dari backup
restoreFromBackup(backupCollection, targetCollection, clazz, onSuccess, onFailure)
```

### 2. Enhanced HistoryRecordFragment (`app/src/main/java/de/rwth_aachen/phyphox/fragment/HistoryRecordFragment.java`)

#### Fitur Baru:
- **Multi-Collection Loading**: Load data dari `record` dan `questions` collection sekaligus
- **Fallback Mechanism**: Jika primary method gagal, gunakan fallback loading
- **Automatic Backup**: Backup data setiap kali berhasil load
- **Backup Recovery**: Restore data dari backup jika main collections gagal

#### Flow Recovery:
1. **Primary**: Load dari multiple collections menggunakan `getAllDocumentsFromMultipleCollections()`
2. **Fallback**: Jika gagal, load dari individual collections
3. **Backup**: Jika semua gagal, restore dari backup collection
4. **Auto-Backup**: Buat backup setiap kali data berhasil di-load

### 3. DataRecoveryService (`app/src/main/java/de/rwth_aachen/phyphox/service/DataRecoveryService.java`)

#### Fitur:
- **Background Recovery**: Service yang berjalan di background untuk recovery otomatis
- **Orphaned Data Detection**: Deteksi data yang tidak memiliki `idCustomer`
- **Data Integrity Validation**: Validasi field yang required
- **Comprehensive Backup**: Backup otomatis untuk semua collection

#### Recovery Process:
1. **Check Orphaned Data**: Cari data tanpa `idCustomer`
2. **Validate Integrity**: Validasi field required
3. **Create Backup**: Backup comprehensive untuk semua data
4. **Auto-Repair**: Perbaiki data yang bisa diperbaiki otomatis

### 4. Admin Panel (`admin/data_recovery.html`)

#### Dashboard Features:
- **Status Overview**: Total records, backup collections, orphaned data, recovery status
- **Recovery Progress**: Progress bar dan log untuk recovery process
- **Backup Management**: View, restore, dan manage backup collections
- **Orphaned Data**: Deteksi dan fix data yang tidak valid
- **Data Integrity**: Monitor dan resolve data integrity issues

## Cara Penggunaan

### 1. Automatic Recovery
Sistem akan otomatis:
- Backup data setiap kali berhasil disimpan
- Retry operasi yang gagal
- Validate data integrity
- Create comprehensive backup

### 2. Manual Recovery via Admin Panel
1. Buka `admin/data_recovery.html`
2. Klik "Start Recovery" untuk menjalankan recovery otomatis
3. Klik "Create Backup" untuk membuat backup manual
4. Monitor progress dan status recovery

### 3. Backup Management
- **View Backup**: Lihat detail backup collection
- **Restore Data**: Restore data dari backup collection
- **Fix Orphaned**: Perbaiki data yang tidak valid
- **Resolve Issues**: Handle data integrity issues

## Struktur Database

### Collections:
- **`record`**: Data OutClass/InClass experiments
- **`questions`**: Custom questions data
- **`backup_*`**: Backup collections dengan format `backup_userId_timestamp`
- **`review_required`**: Data yang memerlukan review manual
- **`comprehensive_backup_*`**: Backup comprehensive dengan timestamp

### Document Structure:
```json
{
  "id": "document_id",
  "idCustomer": "user_id",
  "customerName": "user_name",
  "typeData": "experiment_type",
  "topics": "Out Class|In Class|Custom Question",
  "question": "question_text",
  "dateTime": "2024-01-01 12:00:00",
  "isFinished": false,
  "photo": "image_url",
  "photoDraw": ["drawing_urls"],
  "base64": "compressed_image_data"
}
```

## Monitoring dan Logging

### Log Tags:
- **`HISTORY_LOAD`**: Loading data operations
- **`BACKUP`**: Backup operations
- **`BACKUP_RESTORE`**: Restore operations
- **`DataRecoveryService`**: Service operations

### Log Levels:
- **DEBUG**: Normal operations
- **WARNING**: Non-critical issues
- **ERROR**: Critical failures

## Troubleshooting

### 1. Data Tidak Muncul di History
1. Check log untuk `HISTORY_LOAD` tags
2. Verify `idCustomer` field ada dan valid
3. Check backup collections untuk data yang hilang
4. Run recovery process via admin panel

### 2. Backup Gagal
1. Check Firebase permissions
2. Verify collection names valid
3. Check network connectivity
4. Monitor error logs

### 3. Recovery Gagal
1. Check service logs
2. Verify backup collections exist
3. Check data integrity
4. Manual intervention via admin panel

## Best Practices

### 1. Regular Monitoring
- Monitor admin panel secara berkala
- Check backup collections count
- Monitor orphaned data count

### 2. Backup Strategy
- Automatic backup setiap save operation
- Comprehensive backup setiap recovery process
- Manual backup sebelum maintenance

### 3. Data Validation
- Validate `idCustomer` field selalu ada
- Ensure required fields tidak null
- Monitor data integrity issues

## Performance Considerations

### 1. Batch Operations
- Backup dan restore dalam batch (max 500 documents)
- Use WriteBatch untuk multiple operations
- Implement retry mechanism untuk reliability

### 2. Memory Management
- Load data dalam chunks jika diperlukan
- Implement pagination untuk large datasets
- Clean up resources setelah operations

### 3. Network Optimization
- Compress base64 data sebelum upload
- Implement retry dengan exponential backoff
- Cache frequently accessed data

## Security Considerations

### 1. Access Control
- Admin panel hanya untuk authorized users
- Firebase security rules untuk backup collections
- Audit log untuk semua recovery operations

### 2. Data Privacy
- Backup collections tidak expose sensitive data
- Implement data retention policies
- Secure backup storage

## Maintenance

### 1. Regular Cleanup
- Archive old backup collections
- Clean up resolved integrity issues
- Monitor storage usage

### 2. Performance Tuning
- Optimize query performance
- Monitor Firebase usage limits
- Implement caching strategies

### 3. Updates
- Regular review dan update recovery logic
- Monitor Firebase SDK updates
- Test recovery process secara berkala

## Support dan Kontak

Jika mengalami masalah dengan sistem recovery:
1. Check logs dan error messages
2. Review admin panel status
3. Run manual recovery process
4. Contact development team dengan log details

---

**Version**: 1.0.0  
**Last Updated**: January 2024  
**Maintainer**: G-Physolve Development Team 