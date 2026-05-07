# ReplayMod Development Notes

## Hardware H.264 Export

ReplayMod can use FFmpeg hardware H.264 encoders when the selected MP4 preset contains `%HARDWARE_H264%`.

Encoder priority is:

1. `h264_nvenc`
2. `h264_vaapi`
3. `h264_qsv`
4. `h264_amf`
5. `h264_videotoolbox`
6. `libx264` fallback

NVENC requires a working NVIDIA driver and an FFmpeg build compiled with NVENC support. The Nix dev shell provides FFmpeg, but it cannot provide the host NVIDIA kernel driver.

VAAPI requires `/dev/dri/renderD128` and an FFmpeg build with `h264_vaapi`. On Linux, ensure the user running Minecraft has access to the render device.
