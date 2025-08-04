# Fix User Login Issue

## Masalah
User yang terdaftar melalui admin portal tidak bisa login ke aplikasi Android.

## Penyebab
Aplikasi Android mengharapkan struktur data user yang berbeda dengan yang disimpan oleh admin portal:

### Struktur yang diharapkan oleh Android App (UserModel):
```java
{
    id: String,                    // User ID (sama dengan document ID)
    name: String,                  // Nama user
    email: String,                 // Email user
    totalVisitingIntroduction: Long, // Default: 0L
    totalVisitDurationMillis: Long  // Default: 0L
}
```

### Struktur yang disimpan oleh Admin Portal (sebelum perbaikan):
```javascript
{
    name: String,                  // Nama user
    email: String,                 // Email user
    role: String,                  // Role user (admin/user)
    active: Boolean,               // Status aktif
    createdAt: Timestamp           // Waktu pembuatan
}
```

## Solusi yang Diterapkan

### 1. Perbaikan Admin Portal
- **File yang diperbaiki:**
  - `admin/users.html` - Fungsi "Add User" manual dan "Upload CSV"
  - `admin/create_admin.html` - Fungsi pembuatan admin

- **Perubahan:**
  - Menambahkan field `id` dengan nilai `userCredential.user.uid`
  - Menambahkan field `totalVisitingIntroduction` dengan nilai `0`
  - Menambahkan field `totalVisitDurationMillis` dengan nilai `0`

### 2. Tool Perbaikan User yang Sudah Ada
- **File baru:** `admin/fix_existing_users.html`
- **Fungsi:**
  - Scan user yang sudah ada untuk menemukan yang tidak memiliki field yang diperlukan
  - Menambahkan field yang hilang secara otomatis
  - Progress tracking dan error reporting

### 3. Navigasi Admin Portal
- Menambahkan link "Fix Users" di semua halaman admin portal

## Cara Menggunakan

### Untuk User Baru
User yang dibuat melalui admin portal setelah perbaikan ini akan otomatis memiliki struktur data yang benar.

### Untuk User yang Sudah Ada
1. Login ke admin portal
2. Klik menu "Fix Users" di sidebar
3. Klik tombol "Scan Users" untuk melihat user yang perlu diperbaiki
4. Klik tombol "Fix Users" untuk memperbaiki user secara otomatis

## Verifikasi
Setelah perbaikan, user yang terdaftar melalui admin portal seharusnya bisa login ke aplikasi Android dengan normal.

## Struktur Data Setelah Perbaikan
```javascript
{
    id: String,                    // User ID (sama dengan document ID)
    name: String,                  // Nama user
    email: String,                 // Email user
    role: String,                  // Role user (admin/user)
    active: Boolean,               // Status aktif
    totalVisitingIntroduction: 0,  // Default: 0
    totalVisitDurationMillis: 0,   // Default: 0
    createdAt: Timestamp           // Waktu pembuatan
}
```

## Catatan Penting
- Tool "Fix Users" hanya menambahkan field yang hilang, tidak mengubah data yang sudah ada
- Semua user baru yang dibuat melalui admin portal akan otomatis memiliki struktur data yang benar
- Perbaikan ini tidak mempengaruhi user yang sudah bisa login dengan normal 