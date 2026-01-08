# Solusi Error "Invalid Login Credential"

## Masalah
Error "invalid login credential" terjadi karena:

1. **SHA Fingerprint belum ditambahkan** ke Firebase Console → menyebabkan `BAD_AUTHENTICATION`
2. **Email/password tidak ada** di Firebase Auth project baru `gphysolve`
3. **Akun masih di project lama** (`phyphox-34aed`) bukan di project baru

## Solusi Lengkap

### Langkah 1: Tambahkan SHA Fingerprint (WAJIB)

Ini adalah penyebab utama error `BAD_AUTHENTICATION`. Tanpa ini, Firebase Auth tidak akan bekerja.

#### Cara mendapatkan SHA fingerprint:

**Opsi A: Menggunakan Gradle (Paling Mudah)**
```bash
cd E:\G-Physolve\G-Uphysic
gradlew signingReport
```

Atau di Android Studio:
1. Klik kanan pada project → **Gradle** → **app** → **Tasks** → **android** → **signingReport**
2. Cari bagian **SHA1:** dan **SHA256:** di output

**Opsi B: Menggunakan keytool**
```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

#### Tambahkan ke Firebase Console:

1. Buka [Firebase Console](https://console.firebase.google.com/)
2. Pilih project **gphysolve**
3. Settings (⚙️) → **Project settings**
4. Scroll ke **"Your apps"** → klik app Android (`de.rwth_aachen.phyphox`)
5. Di **"SHA certificate fingerprints"**, klik **"Add fingerprint"**
6. Paste **SHA-1** → **Save**
7. Klik **"Add fingerprint"** lagi → Paste **SHA-256** → **Save**
8. **Download ulang** `google-services.json` dan ganti file yang ada

### Langkah 2: Buat Akun Baru atau Import User

Karena project Firebase baru (`gphysolve`) masih kosong, Anda perlu:

#### Opsi A: Buat Akun Baru (Paling Mudah)
1. Gunakan fitur **Register** di aplikasi
2. Buat akun dengan email/password baru
3. Login dengan akun baru tersebut

#### Opsi B: Import User dari Project Lama
Jika Anda ingin memindahkan user dari project lama:

1. Buka Firebase Console project lama (`phyphox-34aed`)
2. Export user data dari Firebase Auth
3. Import ke project baru (`gphysolve`)

**Atau** buat ulang akun dengan email/password yang sama di project baru.

### Langkah 3: Rebuild Aplikasi

Setelah menambahkan SHA fingerprint:

1. **Build** → **Clean Project**
2. **Build** → **Rebuild Project**
3. **Run** aplikasi

### Langkah 4: Test Login/Register

1. Coba **Register** dengan email baru
2. Atau **Login** dengan akun yang sudah dibuat di project baru

## Error Handling yang Diperbaiki

Saya sudah memperbaiki error handling di `LoginActivity.java` dan `RegisterActivity.java` agar menampilkan pesan error yang lebih jelas dalam bahasa Indonesia:

- ✅ "Email tidak terdaftar. Silakan daftar terlebih dahulu."
- ✅ "Password salah. Silakan coba lagi."
- ✅ "Email atau password salah. Silakan periksa kembali."
- ✅ "Email sudah terdaftar. Silakan gunakan email lain atau login."
- ✅ Dan lainnya...

## Checklist

- [ ] SHA-1 fingerprint sudah ditambahkan ke Firebase Console
- [ ] SHA-256 fingerprint sudah ditambahkan ke Firebase Console
- [ ] `google-services.json` sudah diupdate (download ulang dari Firebase Console)
- [ ] Aplikasi sudah di-rebuild
- [ ] Sudah mencoba Register dengan email baru
- [ ] Sudah mencoba Login dengan akun baru

## Catatan Penting

⚠️ **Error `BAD_AUTHENTICATION` dari Google Play Services** adalah normal dan tidak mempengaruhi Firebase Auth. Error tersebut muncul karena Google Play Services mencoba mengakses layanan Google lainnya, bukan Firebase.

Yang penting adalah:
- ✅ SHA fingerprint sudah ditambahkan
- ✅ `google-services.json` sudah benar
- ✅ Aplikasi sudah di-rebuild
- ✅ Login/Register berfungsi dengan baik

Jika setelah semua langkah di atas masih error, cek logcat untuk error message yang lebih detail dari Firebase Auth (bukan dari Google Play Services).

