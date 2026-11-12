# Shorts — Android Offline Video App

Project Android native berbasis Kotlin. Aplikasi memutar video pendek secara vertikal dari folder internal `Shorts` dan dapat digunakan tanpa akun atau server.

## Fitur versi awal
- Feed video vertikal, geser ke atas/bawah.
- Pemutaran otomatis dan repeat.
- Impor beberapa video dari penyimpanan perangkat.
- Salin video ke folder privat aplikasi `files/Shorts` agar feed tetap tersedia offline.
- Like dan simpan lokal per video.
- Komentar lokal per video.
- Bagikan video melalui aplikasi lain.
- Kelola/hapus video dari menu Folder.
- Ubah nama profil dan pilih foto profil (URI disimpan lokal).

## Cara membangun APK dengan GitHub Actions
1. Buat repository GitHub baru bernama `Shorts`.
2. Ekstrak ZIP project ini, lalu unggah seluruh isi folder ke root repository.
3. Pastikan `.github/workflows/build-apk.yml` ikut terunggah.
4. Buka tab **Actions** pada repository.
5. Pilih **Build Shorts APK** lalu tekan **Run workflow** (atau push ke `main`/`master`).
6. Setelah selesai, buka hasil workflow dan unduh artifact **Shorts-debug-apk**.
7. Ekstrak artifact ZIP, lalu instal `app-debug.apk` di Android.

## Build lokal
Memerlukan JDK 17 dan Gradle 8.9:
```bash
gradle assembleDebug
```
APK debug akan berada di `app/build/outputs/apk/debug/app-debug.apk`.

## Catatan
- Ini adalah aplikasi offline lokal, bukan platform video online. Like/komentar tidak disinkronkan ke pengguna lain.
- Video diimpor ke penyimpanan privat aplikasi; menghapus aplikasi akan menghapus salinan video tersebut.
- Versi ini menggunakan AndroidX Media3 ExoPlayer. Dukungan codec bergantung pada perangkat Android.
- APK dari workflow adalah APK debug untuk pengujian, belum ditandatangani sebagai rilis Play Store.
