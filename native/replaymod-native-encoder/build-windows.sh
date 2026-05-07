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
  DEFAULT_FFNV_CODEC_HEADERS=/nix/store/z1n98l14crhcbmimvsr3b56f30r3xcyb-nv-codec-headers-9.1.23.1/include
fi

: "${FFNV_CODEC_HEADERS:=$DEFAULT_FFNV_CODEC_HEADERS}"
: "${JAVA_INCLUDE:=/nix/store/k95pqfzyvrna93hc9a4cg5csl7l4fh0d-openjdk-21.0.7+6/include}"
: "${MCFGTHREAD:=/nix/store/kr1jwy18b77v1bd27yqyiykabi6c81kx-mcfgthread-x86_64-w64-mingw32-2.3.2}"
: "${CXX:=x86_64-w64-mingw32-g++}"

"$CXX" \
  -std=c++17 -O2 -DNOMINMAX -DWIN32_LEAN_AND_MEAN -shared \
  -I"$FFNV_CODEC_HEADERS" \
  -I"$JAVA_INCLUDE" \
  -I"$JAVA_INCLUDE/linux" \
  -L"$MCFGTHREAD/lib" \
  "$ROOT/replaymod_native_encoder.cpp" \
  -o "$OUT/replaymod_native_encoder.dll" \
  -static -static-libgcc -static-libstdc++ \
  -Wl,--out-implib,"$OUT/libreplaymod_native_encoder.a"

ls -lh "$OUT/replaymod_native_encoder.dll"
