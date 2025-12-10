#!/bin/bash

# =========================
# CONFIG
# =========================
DIR="/home/dabskutz/Project/FolkPatch"
BUILD="/home/dabskutz/Project/build"

BOT_TOKEN="8372952744:AAEFNxioJXEZGL1mRTfHqHC3W6nDJ5eFyuM"
CHAT_ID="@SalsaKernel"

# =========================
# BUILD PROCESS
# =========================
cd "$DIR" || {
    echo "Gagal masuk ke директori project"
    exit 1
}

echo "Memulai build..."
./gradlew clean assembleRelease

if [ $? -ne 0 ]; then
    echo "Build gagal"

    curl -s -X POST "https://api.telegram.org/bot$BOT_TOKEN/sendMessage" \
    -d chat_id="$CHAT_ID" \
    -d text="Build gagal"

    exit 1
fi

echo "Build berhasil"

# =========================
# FIND APK
# =========================
APK_FILE=$(ls -t "$BUILD"/*.apk 2>/dev/null | head -n 1)

if [ -z "$APK_FILE" ]; then
    echo "APK tidak ditemukan di folder build"

    curl -s -X POST "https://api.telegram.org/bot$BOT_TOKEN/sendMessage" \
    -d chat_id="$CHAT_ID" \
    -d text="APK tidak ditemukan setelah build"

    exit 1
fi

echo "APK ditemukan:"
echo "$APK_FILE"

# =========================
# FILE SIZE CHECK
# =========================
FILE_SIZE=$(du -m "$APK_FILE" | cut -f1)

if [ "$FILE_SIZE" -gt 50 ]; then
    echo "APK terlalu besar: $FILE_SIZE MB"

    curl -s -X POST "https://api.telegram.org/bot$BOT_TOKEN/sendMessage" \
    -d chat_id="$CHAT_ID" \
    -d text="Upload dibatalkan. APK terlalu besar: $FILE_SIZE MB"

    exit 1
fi

# =========================
# CHANGELOG
# =========================
CHANGELOG=$(git log -1 --pretty=%B)

CAPTION="New Build Released

File: $(basename "$APK_FILE")
Date: $(date '+%d %B %Y %H:%M')
Size: ${FILE_SIZE}MB

Changelog:
$CHANGELOG
"

# =========================
# NOTIFIKASI
# =========================
curl -s -X POST "https://api.telegram.org/bot$BOT_TOKEN/sendMessage" \
-d chat_id="$CHAT_ID" \
-d text="Build selesai, memulai upload APK"

# =========================
# UPLOAD APK
# =========================
UPLOAD_RESPONSE=$(curl -s -X POST "https://api.telegram.org/bot$BOT_TOKEN/sendDocument" \
  -F chat_id="$CHAT_ID" \
  -F document=@"$APK_FILE" \
  -F caption="$CAPTION")

if echo "$UPLOAD_RESPONSE" | grep -q '"ok":true'; then
    echo "Upload berhasil ke channel"
else
    echo "Upload gagal"
    echo "$UPLOAD_RESPONSE"
    exit 1
fi
