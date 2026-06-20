# tutorial cara compile / build apk ini di termux android secara mandiri

Dokumen ini berisi panduan lengkap langkah demi langkah untuk mengompilasi dan membangun berkas APK **Game Booster & Network Ultra Optimizer** menggunakan aplikasi **Termux** langsung di ponsel Android Anda tanpa komputer!

---

## 🛠️ prasyarat utama
1. Pasang aplikasi **Termux** (disarankan versi terbaru dari [F-Droid](https://f-droid.org/en/packages/com.termux/)).
2. Pastikan paket internet stabil karena Termux akan mengunduh paket compiler JDK dan dependensi Gradle.
3. Berikan izin akses penyimpanan internal pada ponsel ke Termux.

---

## 🚀 langkah 1: persiapan lingkungan termux
Buka aplikasi Termux, lalu jalankan rentetan perintah berikut satu per satu untuk memperbarui repositori bawaan dan memasang paket-paket yang diperlukan:

```bash
# 1. Update list paket dan upgrade sistem internal Termux
pkg update && pkg upgrade -y

# 2. Berikan izin akses penyimpanan rom penyimpanan hp
termux-setup-storage

# 3. Instal git, java openjdk-17, dan gradle compile engine
pkg install git openjdk-17 gradle -y
```

> **Verifikasi Instalasi:**  
> Untuk memastikan instalasi berhasil, ketik:
> ```bash
> java --version
> gradle --version
> ```
> *(Pastikan keluar output versi Java 17 dan Gradle)*

---

## 📂 langkah 2: clone / unduh project dari github
Apabila Anda telah men-sync project ini ke akun GitHub pribadi Anda, Anda dapat langsung mengunduhnya dengan perintah `git clone`:

```bash
# Ganti url di bawah dengan url repository GitHub pribadi Anda
git clone https://github.com/USERNAME/NAMA-REPO-ANDA.git

# Masuk ke direktori folder project
cd NAMA-REPO-ANDA
```

---

## 🔨 langkah 3: compile pro-aktif ke apk siap install
Gunakan Gradle wrapper (`./gradlew`) bawaan yang sudah lengkap kami sertakan di template project ini atau langsung gunakan program Gradle Termux untuk mengompilasi aplikasi:

```bash
# 1. Pastikan gradlew memiliki izin eksekusi penuh
chmod +x gradlew

# 2. Jalankan kompilasi proyek mode DEBUG (Instan & Tanpa Signings Rumit)
./gradlew assembleDebug
```

### ☕ alternatif (Jika mengalami kendala Gradle internal):
Jika gradlew menemui kendala versi gradle distribusi di Termux, Anda bisa langsung memaksakan kompilasi menggunakan Gradle lokal dari paket Termux:
```bash
gradle assembleDebug
```

---

## 📦 langkah 4: lokasi berkas apk hasil kompilasi
Setelah proses kompilasi sukses dan melihat pesan **`BUILD SUCCESSFUL`**, berkas APK siap install akan berada di direktori berikut:

```bash
# Berkas APK siap pasang ada di:
./app/build/outputs/apk/debug/app-debug.apk
```

### 📲 cara memindahkan apk ke penyimpanan internal untuk langsung diinstall:
Gunakan perintah `cp` untuk memindahkannya ke folder Downloads penyimpanan HP agar mudah dicari di File Manager ponsel Anda:
```bash
cp ./app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/GameBoosterUtra.apk
```
Sekarang, buka File Manager HP Anda, buka folder **Downloads**, klik **`GameBoosterUtra.apk`**, dan pasang!

---

## ⚡ troubleshooting & tips optimasi termux
- **Error: Out of Memory (OOM):**  
  Jika Termux tertutup tiba-tiba sewaktu mengompilasi, batasi kapasitas penggunaan memory RAM daemon gradle dengan membuat konfigurasi JVM dalam proyek:
  ```bash
  echo "org.gradle.jvmargs=-Xmx1024m" >> gradle.properties
  ```
- **Error: Java Version Mismatch:**  
  Aplikasi ini menggunakan Kotlin terbaru dan jetpack compose yang mewajibkan kompilasi Java 17 ke atas. Jalankan `java -version` untuk meyakinkan versi JVM Termux berada di rentang Java 17.

Selamat bermain game dengan latency ultra-rendah dan koneksi super cepat stabil! 🚀🔥
