# ReplayMod Native OpenGL Encoder ABI

ReplayMod can bypass the FFmpeg `rawvideo` stdin path for default, non-depth,
non-antialiased H.264 MP4 renders by loading a native library named
`replaymod_native_encoder`.

The Java side passes the current OpenGL framebuffer color texture directly to
the native encoder. The native implementation is expected to keep frame data on
the GPU, using CUDA/OpenGL interop plus NVENC, or the platform-specific direct
resource path supported by the NVIDIA Video Codec SDK.

## Loading

ReplayMod loads the library in this order:

1. Absolute path from `-Dreplaymod.nativeEncoder.library=/path/to/library`
2. Absolute path from `REPLAYMOD_NATIVE_ENCODER_LIBRARY`
3. `System.loadLibrary("replaymod_native_encoder")`

Set `-Dreplaymod.nativeEncoder=false` or `REPLAYMOD_NATIVE_ENCODER=false` to
force the existing FFmpeg path. If the native library is missing or fails to
initialize, ReplayMod falls back to the FFmpeg path by default. Set
`-Dreplaymod.nativeEncoder.required=true` or
`REPLAYMOD_NATIVE_ENCODER_REQUIRED=true` only when you want a missing native
encoder to fail the render instead of falling back.

## Required JNI Methods

The library must export these JNI methods for
`com.replaymod.render.NativeOpenGlEncoder`:

```c
JNIEXPORT jlong JNICALL Java_com_replaymod_render_NativeOpenGlEncoder_nativeOpen(
    JNIEnv *env,
    jclass clazz,
    jstring outputFile,
    jint width,
    jint height,
    jint fps,
    jint bitrate,
    jboolean flipVertical);

JNIEXPORT void JNICALL Java_com_replaymod_render_NativeOpenGlEncoder_nativeEncode(
    JNIEnv *env,
    jclass clazz,
    jlong handle,
    jint frameId,
    jint textureTarget,
    jint textureId);

JNIEXPORT void JNICALL Java_com_replaymod_render_NativeOpenGlEncoder_nativeClose(
    JNIEnv *env,
    jclass clazz,
    jlong handle);

JNIEXPORT void JNICALL Java_com_replaymod_render_NativeOpenGlEncoder_nativeAbort(
    JNIEnv *env,
    jclass clazz,
    jlong handle);
```

`nativeEncode` is called on the render thread while the OpenGL context is
current. The native encoder should either consume the texture synchronously
before returning or copy/register it into an internal GPU-side ring before the
next Minecraft frame overwrites the framebuffer attachment.

## Build

On Nix, build the Windows DLL with:

```bash
nix shell nixpkgs#pkgsCross.mingwW64.stdenv.cc nixpkgs#nv-codec-headers -c \
  native/replaymod-native-encoder/build-windows.sh
```

The DLL is written to:

```text
native/replaymod-native-encoder/build/windows/replaymod_native_encoder.dll
```

Place it in Prism's instance native directory or launch with:

```text
-Dreplaymod.nativeEncoder.library=C:\path\to\replaymod_native_encoder.dll
```

## Expected Native Flow

1. Open an NVENC session and MP4 muxer in `nativeOpen`.
2. Register or import the OpenGL texture/CUDA resource for each frame.
3. Perform vertical flip and BGRA/RGBA to NVENC-compatible input conversion on
   GPU when needed.
4. Submit the frame to NVENC.
5. Write encoded packets to the muxer.
6. Flush and finalize the MP4 in `nativeClose`.

NVIDIA's Video Codec SDK documents external input resources, OpenGL texture
resources, and the `NvEncRegisterResource`/`NvEncMapInputResource` flow.
