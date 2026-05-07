#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/build/windows"
mkdir -p "$OUT"

if [[ -d /tmp/nv-codec-headers-11/include ]]; then
  DEFAULT_FFNV_CODEC_HEADERS=/tmp/nv-codec-headers-11/include
elif [[ -d /tmp/nv-codec-headers-12/include ]]; then
  DEFAULT_FFNV_CODEC_HEADERS=/tmp/nv-codec-headers-12/include
elif [[ -d /tmp/nv-codec-headers/include ]]; then
  DEFAULT_FFNV_CODEC_HEADERS=/tmp/nv-codec-headers/include
else
  DEFAULT_FFNV_CODEC_HEADERS=
fi

: "${FFNV_CODEC_HEADERS:=$DEFAULT_FFNV_CODEC_HEADERS}"
: "${JAVA_INCLUDE:=${JAVA_HOME:-}/include}"
if [[ -z "${CXX:-}" || "$(basename "$CXX")" != *w64-mingw32* ]]; then
  CXX=x86_64-w64-mingw32-g++
fi

if [[ -z "$FFNV_CODEC_HEADERS" || ! -f "$FFNV_CODEC_HEADERS/ffnvcodec/nvEncodeAPI.h" ]]; then
  echo "FFNV_CODEC_HEADERS must point to nv-codec-headers/include" >&2
  exit 1
fi

if [[ -z "$JAVA_INCLUDE" || ! -f "$JAVA_INCLUDE/jni.h" ]]; then
  echo "JAVA_INCLUDE must point to a JDK include directory" >&2
  exit 1
fi

EXTRA_LDFLAGS=()
if [[ -n "${MCFGTHREAD:-}" && -d "$MCFGTHREAD/lib" ]]; then
  EXTRA_LDFLAGS+=("-L$MCFGTHREAD/lib")
fi

"$CXX" \
  -std=c++17 -O2 -DNOMINMAX -DWIN32_LEAN_AND_MEAN -shared \
  -I"$FFNV_CODEC_HEADERS" \
  -I"$JAVA_INCLUDE" \
  -I"$JAVA_INCLUDE/linux" \
  "$ROOT/replaymod_native_encoder.cpp" \
  -o "$OUT/replaymod_native_encoder.dll" \
  "${EXTRA_LDFLAGS[@]}" \
  -static -static-libgcc -static-libstdc++ \
  -Wl,--out-implib,"$OUT/libreplaymod_native_encoder.a"

ls -lh "$OUT/replaymod_native_encoder.dll"
