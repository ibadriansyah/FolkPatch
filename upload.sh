#!/bin/bash

# =========================
# KONFIGURASI
# =========================
BOT_TOKEN="8372952744:AAEFNxioJXEZGL1mRTfHqHC3W6nDJ5eFyuM"
CHAT_ID="1115301990"

APK_PATH="/home/dabskutz/Project/build/Salsa-Patch_111609_1.3.7_on_main-release.apk"

# =========================
# VALIDASI FILE
# =========================
if [ ! -f "$APK_PATH" ]; then
    echo "❌ File tidak ditemukan:"
    echo "$APK_PATH"
    exit 1
fi

FILE_SIZE=$(du -m "$APK_PATH" | cut -f1)

if [ "$FILE_SIZE" -gt 50 ]; then
    echo "❌ Gagal upload: File lebih dari 50MB ($FILE_SIZE MB)"
    exit 1
fi

echo "✅ File ditemukan"
echo "📦 Ukuran: $FILE_SIZE MB"
echo "🚀 Mengupload ke Telegram..."

# =========================
# UPLOAD KE TELEGRAM
# =========================
RESPONSE=$(curl -f -s -X POST "https://api.telegram.org/bot$BOT_TOKEN/sendDocument" \
  -F chat_id="$CHAT_ID" \
  -F document=@"$APK_PATH" \
  -F caption="✅ Build otomatis selesai")

# =========================
# CEK STATUS
# =========================
if echo "$RESPONSE" | grep -q '"ok":true'; then
    echo "✅ Upload berhasil!"
else
    echo "❌ Upload gagal!"
    echo "Response:"
    echo "$RESPONSE"
fi
