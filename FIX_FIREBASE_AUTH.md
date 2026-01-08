# Cara Memperbaiki Error Firebase Auth (BAD_AUTHENTICATION)

## Masalah
Error `BAD_AUTHENTICATION` dan `Long live credential not available` terjadi karena **SHA-1/SHA-256 fingerprint** belum terdaftar di Firebase Console untuk project baru `gphysolve`.

## Solusi

### Langkah 1: Dapatkan SHA-1 dan SHA-256 Fingerprint

#### Opsi A: Menggunakan Gradle (Paling Mudah)
Jalankan perintah ini di terminal Android Studio atau command prompt:

```bash
cd android
./gradlew signingReport
```

Atau di Windows:
```cmd
cd android
gradlew signingReport
```

Cari bagian **SHA1:** dan **SHA256:** di output.

#### Opsi B: Menggunakan keytool Manual

**Windows:**
```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**Mac/Linux:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Jika keytool tidak ditemukan, cari di:
- Windows: `C:\Program Files\Java\jdk*\bin\keytool.exe`
- Mac: `/Applications/Android Studio.app/Contents/jre/Contents/Home/bin/keytool`
- Linux: Biasanya di `/usr/bin/keytool` atau di Java JDK path

### Langkah 2: Tambahkan Fingerprint ke Firebase Console

1. Buka [Firebase Console](https://console.firebase.google.com/)
2. Pilih project **gphysolve**
3. Klik ikon **Settings (⚙️)** di kiri atas → **Project settings**
4. Scroll ke bawah ke bagian **"Your apps"**
5. Klik pada aplikasi Android Anda (`de.rwth_aachen.phyphox`)
6. Di bagian **"SHA certificate fingerprints"**, klik **"Add fingerprint"**
7. Paste **SHA-1** fingerprint → **Save**
8. Klik **"Add fingerprint"** lagi → Paste **SHA-256** fingerprint → **Save**

### Langkah 3: Download ulang google-services.json

1. Masih di halaman yang sama (Project settings → Your apps → Android app)
2. Scroll ke bawah, klik **"Download google-services.json"**
3. Ganti file `app/google-services.json` dengan file yang baru diunduh
4. **Rebuild** aplikasi di Android Studio

### Langkah 4: Clean dan Rebuild

Di Android Studio:
1. **Build** → **Clean Project**
2. **Build** → **Rebuild Project**
3. **Run** aplikasi

### Langkah 5: Test Login/Register

Setelah rebuild, coba login atau register lagi. Error seharusnya sudah hilang.

## Catatan Penting

- **Debug keystore** digunakan untuk development
- Untuk **production**, Anda perlu menambahkan SHA-1/SHA-256 dari **release keystore** juga
- Setiap kali membuat keystore baru, perlu menambahkan fingerprint baru ke Firebase

## Jika Masih Error

1. Pastikan package name di `build.gradle` sama dengan di Firebase Console:
   ```gradle
   applicationId "de.rwth_aachen.phyphox"
   ```

2. Pastikan `google-services.json` sudah benar dan terbaru

3. Clear app data di device/emulator:
   - Settings → Apps → [Nama App] → Clear Data

4. Uninstall dan reinstall aplikasi

5. Cek logcat untuk error message yang lebih detail

## Error Message yang Diperbaiki

Saya sudah memperbaiki error handling di `LoginActivity.java` dan `RegisterActivity.java` agar menampilkan error message yang lebih detail, sehingga Anda bisa melihat error spesifik dari Firebase.

