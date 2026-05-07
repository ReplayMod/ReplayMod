#include <jni.h>

#include <windows.h>

#include <ffnvcodec/dynlink_cuda.h>
#include <ffnvcodec/nvEncodeAPI.h>

#include <algorithm>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <stdexcept>
#include <string>
#include <vector>

namespace {

struct Sample {
    uint64_t offset = 0;
    uint32_t size = 0;
    bool key = false;
};

struct Mp4Writer {
    FILE *file = nullptr;
    uint32_t width = 0;
    uint32_t height = 0;
    uint32_t fps = 0;
    uint64_t mdatSizeOffset = 0;
    uint64_t mdatDataStart = 0;
    std::vector<Sample> samples;
    std::vector<uint8_t> sps;
    std::vector<uint8_t> pps;

    static void putU8(std::vector<uint8_t> &out, uint8_t v) { out.push_back(v); }
    static void putU16(std::vector<uint8_t> &out, uint16_t v) {
        out.push_back((uint8_t) (v >> 8));
        out.push_back((uint8_t) v);
    }
    static void putU24(std::vector<uint8_t> &out, uint32_t v) {
        out.push_back((uint8_t) (v >> 16));
        out.push_back((uint8_t) (v >> 8));
        out.push_back((uint8_t) v);
    }
    static void putU32(std::vector<uint8_t> &out, uint32_t v) {
        out.push_back((uint8_t) (v >> 24));
        out.push_back((uint8_t) (v >> 16));
        out.push_back((uint8_t) (v >> 8));
        out.push_back((uint8_t) v);
    }
    static void putU64(std::vector<uint8_t> &out, uint64_t v) {
        putU32(out, (uint32_t) (v >> 32));
        putU32(out, (uint32_t) v);
    }
    static void putType(std::vector<uint8_t> &out, const char *type) {
        out.insert(out.end(), type, type + 4);
    }
    static void patchU32(std::vector<uint8_t> &out, size_t pos, uint32_t v) {
        out[pos] = (uint8_t) (v >> 24);
        out[pos + 1] = (uint8_t) (v >> 16);
        out[pos + 2] = (uint8_t) (v >> 8);
        out[pos + 3] = (uint8_t) v;
    }
    static size_t beginBox(std::vector<uint8_t> &out, const char *type) {
        size_t pos = out.size();
        putU32(out, 0);
        putType(out, type);
        return pos;
    }
    static void endBox(std::vector<uint8_t> &out, size_t pos) {
        patchU32(out, pos, (uint32_t) (out.size() - pos));
    }

    static uint64_t tell(FILE *f) {
        return (uint64_t) _ftelli64(f);
    }

    static void writeU32(FILE *f, uint32_t v) {
        uint8_t b[4] = {(uint8_t) (v >> 24), (uint8_t) (v >> 16), (uint8_t) (v >> 8), (uint8_t) v};
        fwrite(b, 1, 4, f);
    }

    static void writeU64(FILE *f, uint64_t v) {
        writeU32(f, (uint32_t) (v >> 32));
        writeU32(f, (uint32_t) v);
    }

    void open(const std::string &path, uint32_t w, uint32_t h, uint32_t frameRate) {
        width = w;
        height = h;
        fps = frameRate;
        file = fopen(path.c_str(), "wb+");
        if (!file) {
            throw std::runtime_error("failed to open output file");
        }

        std::vector<uint8_t> ftyp;
        putU32(ftyp, 24);
        putType(ftyp, "ftyp");
        putType(ftyp, "isom");
        putU32(ftyp, 0x200);
        putType(ftyp, "isom");
        putType(ftyp, "avc1");
        fwrite(ftyp.data(), 1, ftyp.size(), file);

        writeU32(file, 1);
        fwrite("mdat", 1, 4, file);
        mdatSizeOffset = tell(file);
        writeU64(file, 0);
        mdatDataStart = tell(file);
    }

    static bool findStartCode(const uint8_t *data, size_t size, size_t from, size_t &pos, size_t &len) {
        for (size_t i = from; i + 3 < size; i++) {
            if (data[i] == 0 && data[i + 1] == 0 && data[i + 2] == 1) {
                pos = i;
                len = 3;
                return true;
            }
            if (i + 4 < size && data[i] == 0 && data[i + 1] == 0 && data[i + 2] == 0 && data[i + 3] == 1) {
                pos = i;
                len = 4;
                return true;
            }
        }
        return false;
    }

    void writeAnnexB(const uint8_t *data, size_t size, bool key) {
        std::vector<std::pair<const uint8_t *, size_t>> nals;
        size_t sc = 0, scLen = 0;
        if (!findStartCode(data, size, 0, sc, scLen)) {
            nals.push_back({data, size});
        } else {
            size_t nalStart = sc + scLen;
            while (nalStart < size) {
                size_t next = 0, nextLen = 0;
                if (!findStartCode(data, size, nalStart, next, nextLen)) {
                    next = size;
                }
                while (nalStart < next && data[nalStart] == 0) {
                    nalStart++;
                }
                if (next > nalStart) {
                    nals.push_back({data + nalStart, next - nalStart});
                }
                if (next == size) {
                    break;
                }
                nalStart = next + nextLen;
            }
        }

        Sample sample;
        sample.offset = tell(file);
        sample.key = key;
        for (auto &nal : nals) {
            if (nal.second == 0) {
                continue;
            }
            uint8_t type = nal.first[0] & 0x1f;
            if (type == 7) {
                sps.assign(nal.first, nal.first + nal.second);
            } else if (type == 8) {
                pps.assign(nal.first, nal.first + nal.second);
            }
            writeU32(file, (uint32_t) nal.second);
            fwrite(nal.first, 1, nal.second, file);
            sample.size += 4 + (uint32_t) nal.second;
        }
        if (sample.size > 0) {
            samples.push_back(sample);
        }
    }

    std::vector<uint8_t> makeMoov() {
        if (sps.size() < 4 || pps.empty()) {
            throw std::runtime_error("NVENC did not output H.264 SPS/PPS");
        }
        std::vector<uint8_t> out;
        uint32_t movieTimescale = 1000;
        uint32_t movieDuration = (uint32_t) ((samples.size() * 1000ull + fps - 1) / fps);

        size_t moov = beginBox(out, "moov");
        size_t mvhd = beginBox(out, "mvhd");
        putU32(out, 0);
        putU32(out, 0);
        putU32(out, 0);
        putU32(out, movieTimescale);
        putU32(out, movieDuration);
        putU32(out, 0x00010000);
        putU16(out, 0x0100);
        putU16(out, 0);
        putU32(out, 0); putU32(out, 0);
        putU32(out, 0x00010000); putU32(out, 0); putU32(out, 0);
        putU32(out, 0); putU32(out, 0x00010000); putU32(out, 0);
        putU32(out, 0); putU32(out, 0); putU32(out, 0x40000000);
        for (int i = 0; i < 6; i++) putU32(out, 0);
        putU32(out, 2);
        endBox(out, mvhd);

        size_t trak = beginBox(out, "trak");
        size_t tkhd = beginBox(out, "tkhd");
        putU32(out, 0x00000007);
        putU32(out, 0); putU32(out, 0);
        putU32(out, 1);
        putU32(out, 0);
        putU32(out, movieDuration);
        putU32(out, 0); putU32(out, 0);
        putU16(out, 0); putU16(out, 0);
        putU16(out, 0); putU16(out, 0);
        putU32(out, 0x00010000); putU32(out, 0); putU32(out, 0);
        putU32(out, 0); putU32(out, 0x00010000); putU32(out, 0);
        putU32(out, 0); putU32(out, 0); putU32(out, 0x40000000);
        putU32(out, width << 16);
        putU32(out, height << 16);
        endBox(out, tkhd);

        size_t mdia = beginBox(out, "mdia");
        size_t mdhd = beginBox(out, "mdhd");
        putU32(out, 0);
        putU32(out, 0); putU32(out, 0);
        putU32(out, fps);
        putU32(out, (uint32_t) samples.size());
        putU16(out, 0x55c4);
        putU16(out, 0);
        endBox(out, mdhd);

        size_t hdlr = beginBox(out, "hdlr");
        putU32(out, 0); putU32(out, 0); putType(out, "vide");
        putU32(out, 0); putU32(out, 0); putU32(out, 0);
        const char name[] = "VideoHandler";
        out.insert(out.end(), name, name + sizeof(name));
        endBox(out, hdlr);

        size_t minf = beginBox(out, "minf");
        size_t vmhd = beginBox(out, "vmhd");
        putU32(out, 1); putU16(out, 0); putU16(out, 0); putU16(out, 0); putU16(out, 0);
        endBox(out, vmhd);
        size_t dinf = beginBox(out, "dinf");
        size_t dref = beginBox(out, "dref");
        putU32(out, 0); putU32(out, 1);
        size_t url = beginBox(out, "url ");
        putU32(out, 1);
        endBox(out, url);
        endBox(out, dref);
        endBox(out, dinf);

        size_t stbl = beginBox(out, "stbl");
        size_t stsd = beginBox(out, "stsd");
        putU32(out, 0); putU32(out, 1);
        size_t avc1 = beginBox(out, "avc1");
        for (int i = 0; i < 6; i++) putU8(out, 0);
        putU16(out, 1);
        putU16(out, 0); putU16(out, 0);
        putU32(out, 0); putU32(out, 0); putU32(out, 0);
        putU16(out, (uint16_t) width);
        putU16(out, (uint16_t) height);
        putU32(out, 0x00480000); putU32(out, 0x00480000);
        putU32(out, 0);
        putU16(out, 1);
        putU8(out, 0);
        for (int i = 0; i < 31; i++) putU8(out, 0);
        putU16(out, 24);
        putU16(out, 0xffff);
        size_t avcC = beginBox(out, "avcC");
        putU8(out, 1);
        putU8(out, sps[1]);
        putU8(out, sps[2]);
        putU8(out, sps[3]);
        putU8(out, 0xff);
        putU8(out, 0xe1);
        putU16(out, (uint16_t) sps.size());
        out.insert(out.end(), sps.begin(), sps.end());
        putU8(out, 1);
        putU16(out, (uint16_t) pps.size());
        out.insert(out.end(), pps.begin(), pps.end());
        endBox(out, avcC);
        endBox(out, avc1);
        endBox(out, stsd);

        size_t stts = beginBox(out, "stts");
        putU32(out, 0); putU32(out, 1); putU32(out, (uint32_t) samples.size()); putU32(out, 1);
        endBox(out, stts);

        size_t stss = beginBox(out, "stss");
        putU32(out, 0);
        uint32_t keyCount = 0;
        for (const auto &s : samples) if (s.key) keyCount++;
        putU32(out, keyCount);
        for (uint32_t i = 0; i < samples.size(); i++) if (samples[i].key) putU32(out, i + 1);
        endBox(out, stss);

        size_t stsc = beginBox(out, "stsc");
        putU32(out, 0); putU32(out, 1); putU32(out, 1); putU32(out, 1); putU32(out, 1);
        endBox(out, stsc);

        size_t stsz = beginBox(out, "stsz");
        putU32(out, 0); putU32(out, 0); putU32(out, (uint32_t) samples.size());
        for (const auto &s : samples) putU32(out, s.size);
        endBox(out, stsz);

        size_t co64 = beginBox(out, "co64");
        putU32(out, 0); putU32(out, (uint32_t) samples.size());
        for (const auto &s : samples) putU64(out, s.offset);
        endBox(out, co64);
        endBox(out, stbl);
        endBox(out, minf);
        endBox(out, mdia);
        endBox(out, trak);
        endBox(out, moov);
        return out;
    }

    void close() {
        if (!file) return;
        uint64_t end = tell(file);
        _fseeki64(file, (int64_t) mdatSizeOffset, SEEK_SET);
        writeU64(file, end - (mdatSizeOffset - 8));
        _fseeki64(file, (int64_t) end, SEEK_SET);
        std::vector<uint8_t> moov = makeMoov();
        fwrite(moov.data(), 1, moov.size(), file);
        fclose(file);
        file = nullptr;
    }

    ~Mp4Writer() {
        if (file) fclose(file);
    }
};

struct Encoder {
    HMODULE nvencDll = nullptr;
    HMODULE cudaDll = nullptr;
    NV_ENCODE_API_FUNCTION_LIST api = {};
    struct {
        tcuInit *cuInit = nullptr;
        tcuDeviceGet *cuDeviceGet = nullptr;
        tcuCtxCreate_v2 *cuCtxCreate = nullptr;
        tcuCtxDestroy_v2 *cuCtxDestroy = nullptr;
        tcuCtxPushCurrent_v2 *cuCtxPushCurrent = nullptr;
        tcuCtxPopCurrent_v2 *cuCtxPopCurrent = nullptr;
        tcuGLGetDevices_v2 *cuGLGetDevices = nullptr;
        tcuGraphicsGLRegisterImage *cuGraphicsGLRegisterImage = nullptr;
        tcuGraphicsUnregisterResource *cuGraphicsUnregisterResource = nullptr;
        tcuGraphicsMapResources *cuGraphicsMapResources = nullptr;
        tcuGraphicsUnmapResources *cuGraphicsUnmapResources = nullptr;
        tcuGraphicsSubResourceGetMappedArray *cuGraphicsSubResourceGetMappedArray = nullptr;
        tcuGetErrorName *cuGetErrorName = nullptr;
        tcuGetErrorString *cuGetErrorString = nullptr;
    } cu;
    CUcontext cudaContext = nullptr;
    void *encoder = nullptr;
    NV_ENC_OUTPUT_PTR bitstream = nullptr;
    Mp4Writer mp4;
    uint32_t width = 0;
    uint32_t height = 0;
    uint32_t fps = 0;
    uint32_t frame = 0;
    bool closed = false;

    const char *statusName(NVENCSTATUS status) {
        switch (status) {
            case NV_ENC_SUCCESS: return "NV_ENC_SUCCESS";
            case NV_ENC_ERR_NO_ENCODE_DEVICE: return "NV_ENC_ERR_NO_ENCODE_DEVICE";
            case NV_ENC_ERR_UNSUPPORTED_DEVICE: return "NV_ENC_ERR_UNSUPPORTED_DEVICE";
            case NV_ENC_ERR_INVALID_ENCODERDEVICE: return "NV_ENC_ERR_INVALID_ENCODERDEVICE";
            case NV_ENC_ERR_INVALID_DEVICE: return "NV_ENC_ERR_INVALID_DEVICE";
            case NV_ENC_ERR_DEVICE_NOT_EXIST: return "NV_ENC_ERR_DEVICE_NOT_EXIST";
            case NV_ENC_ERR_INVALID_PTR: return "NV_ENC_ERR_INVALID_PTR";
            case NV_ENC_ERR_INVALID_EVENT: return "NV_ENC_ERR_INVALID_EVENT";
            case NV_ENC_ERR_INVALID_PARAM: return "NV_ENC_ERR_INVALID_PARAM";
            case NV_ENC_ERR_INVALID_CALL: return "NV_ENC_ERR_INVALID_CALL";
            case NV_ENC_ERR_OUT_OF_MEMORY: return "NV_ENC_ERR_OUT_OF_MEMORY";
            case NV_ENC_ERR_ENCODER_NOT_INITIALIZED: return "NV_ENC_ERR_ENCODER_NOT_INITIALIZED";
            case NV_ENC_ERR_UNSUPPORTED_PARAM: return "NV_ENC_ERR_UNSUPPORTED_PARAM";
            case NV_ENC_ERR_LOCK_BUSY: return "NV_ENC_ERR_LOCK_BUSY";
            case NV_ENC_ERR_NOT_ENOUGH_BUFFER: return "NV_ENC_ERR_NOT_ENOUGH_BUFFER";
            case NV_ENC_ERR_INVALID_VERSION: return "NV_ENC_ERR_INVALID_VERSION";
            case NV_ENC_ERR_MAP_FAILED: return "NV_ENC_ERR_MAP_FAILED";
            case NV_ENC_ERR_NEED_MORE_INPUT: return "NV_ENC_ERR_NEED_MORE_INPUT";
            case NV_ENC_ERR_ENCODER_BUSY: return "NV_ENC_ERR_ENCODER_BUSY";
            case NV_ENC_ERR_EVENT_NOT_REGISTERD: return "NV_ENC_ERR_EVENT_NOT_REGISTERD";
            case NV_ENC_ERR_GENERIC: return "NV_ENC_ERR_GENERIC";
            case NV_ENC_ERR_INCOMPATIBLE_CLIENT_KEY: return "NV_ENC_ERR_INCOMPATIBLE_CLIENT_KEY";
            case NV_ENC_ERR_UNIMPLEMENTED: return "NV_ENC_ERR_UNIMPLEMENTED";
            case NV_ENC_ERR_RESOURCE_REGISTER_FAILED: return "NV_ENC_ERR_RESOURCE_REGISTER_FAILED";
            case NV_ENC_ERR_RESOURCE_NOT_REGISTERED: return "NV_ENC_ERR_RESOURCE_NOT_REGISTERED";
            case NV_ENC_ERR_RESOURCE_NOT_MAPPED: return "NV_ENC_ERR_RESOURCE_NOT_MAPPED";
#ifdef NV_ENC_ERR_NEED_MORE_OUTPUT
            case NV_ENC_ERR_NEED_MORE_OUTPUT: return "NV_ENC_ERR_NEED_MORE_OUTPUT";
#endif
            default: return "NV_ENC_ERR_UNKNOWN";
        }
    }

    void check(NVENCSTATUS status, const char *what) {
        if (status != NV_ENC_SUCCESS) {
            char buf[256];
            snprintf(buf, sizeof(buf), "%s failed with NVENC status %d (%s)",
                     what, (int) status, statusName(status));
            throw std::runtime_error(buf);
        }
    }

    void checkCu(CUresult status, const char *what) {
        if (status != CUDA_SUCCESS) {
            const char *name = nullptr;
            const char *message = nullptr;
            if (cu.cuGetErrorName) cu.cuGetErrorName(status, &name);
            if (cu.cuGetErrorString) cu.cuGetErrorString(status, &message);
            char buf[512];
            snprintf(buf, sizeof(buf), "%s failed with CUDA status %d%s%s%s%s",
                     what, (int) status,
                     name ? " (" : "", name ? name : "", name ? ")" : "",
                     message ? message : "");
            throw std::runtime_error(buf);
        }
    }

    FARPROC loadProc(HMODULE module, const char *name) {
        FARPROC proc = GetProcAddress(module, name);
        if (!proc) {
            std::string msg = "missing function ";
            msg += name;
            throw std::runtime_error(msg);
        }
        return proc;
    }

    void loadCuda() {
        cudaDll = LoadLibraryA("nvcuda.dll");
        if (!cudaDll) {
            throw std::runtime_error("failed to load nvcuda.dll");
        }
        cu.cuInit = (tcuInit *) loadProc(cudaDll, "cuInit");
        cu.cuDeviceGet = (tcuDeviceGet *) loadProc(cudaDll, "cuDeviceGet");
        cu.cuCtxCreate = (tcuCtxCreate_v2 *) loadProc(cudaDll, "cuCtxCreate_v2");
        cu.cuCtxDestroy = (tcuCtxDestroy_v2 *) loadProc(cudaDll, "cuCtxDestroy_v2");
        cu.cuCtxPushCurrent = (tcuCtxPushCurrent_v2 *) loadProc(cudaDll, "cuCtxPushCurrent_v2");
        cu.cuCtxPopCurrent = (tcuCtxPopCurrent_v2 *) loadProc(cudaDll, "cuCtxPopCurrent_v2");
        cu.cuGLGetDevices = (tcuGLGetDevices_v2 *) loadProc(cudaDll, "cuGLGetDevices_v2");
        cu.cuGraphicsGLRegisterImage = (tcuGraphicsGLRegisterImage *) loadProc(cudaDll, "cuGraphicsGLRegisterImage");
        cu.cuGraphicsUnregisterResource = (tcuGraphicsUnregisterResource *) loadProc(cudaDll, "cuGraphicsUnregisterResource");
        cu.cuGraphicsMapResources = (tcuGraphicsMapResources *) loadProc(cudaDll, "cuGraphicsMapResources");
        cu.cuGraphicsUnmapResources = (tcuGraphicsUnmapResources *) loadProc(cudaDll, "cuGraphicsUnmapResources");
        cu.cuGraphicsSubResourceGetMappedArray = (tcuGraphicsSubResourceGetMappedArray *) loadProc(cudaDll, "cuGraphicsSubResourceGetMappedArray");
        cu.cuGetErrorName = (tcuGetErrorName *) GetProcAddress(cudaDll, "cuGetErrorName");
        cu.cuGetErrorString = (tcuGetErrorString *) GetProcAddress(cudaDll, "cuGetErrorString");
    }

    void createCudaContext() {
        checkCu(cu.cuInit(0), "cuInit");

        CUdevice device = (CUdevice) -1;
        unsigned int count = 0;
        CUdevice glDevices[8] = {};

        // The NVENC encoder must run on the same NVIDIA device that owns the current OpenGL
        // context, otherwise cuGraphicsGLRegisterImage will fail later. Probe (in order)
        // CURRENT_FRAME, NEXT_FRAME and ALL to find a valid CUDA<->GL interop device.
        const CUGLDeviceList probes[] = {
                CU_GL_DEVICE_LIST_CURRENT_FRAME,
                CU_GL_DEVICE_LIST_NEXT_FRAME,
                CU_GL_DEVICE_LIST_ALL,
        };
        std::string probeReport;
        for (CUGLDeviceList probe : probes) {
            count = 0;
            CUresult result = cu.cuGLGetDevices(&count, glDevices,
                                                 (unsigned int) (sizeof(glDevices) / sizeof(glDevices[0])),
                                                 probe);
            if (result == CUDA_SUCCESS && count > 0) {
                device = glDevices[0];
                break;
            }
            if (!probeReport.empty()) probeReport += ", ";
            probeReport += "list=" + std::to_string((int) probe)
                    + " result=" + std::to_string((int) result)
                    + " count=" + std::to_string((int) count);
        }

        if (device == (CUdevice) -1) {
            // We deliberately do NOT fall back to cuDeviceGet(0) here: when the OpenGL
            // context is owned by a non-NVIDIA GPU (AMD/Intel iGPU on hybrid machines),
            // the resulting CUcontext can be created, but cuGraphicsGLRegisterImage will
            // crash the JVM later. Surface a clean failure and let Java fall back to FFmpeg.
            std::string msg = "no NVIDIA CUDA device matches the current OpenGL context";
            if (!probeReport.empty()) {
                msg += " (probes: " + probeReport + ")";
            }
            msg += ". The native NVENC path requires NVIDIA to be the OpenGL renderer; "
                   "on hybrid AMD/Intel + NVIDIA systems set Windows graphics preference "
                   "for Minecraft (java.exe/javaw.exe) to High performance, or disable "
                   "the native encoder via -Dreplaymod.nativeEncoder=false.";
            throw std::runtime_error(msg);
        }

        checkCu(cu.cuCtxCreate(&cudaContext, CU_CTX_SCHED_BLOCKING_SYNC, device), "cuCtxCreate");
    }

    void openEncodeSession() {
        NV_ENC_OPEN_ENCODE_SESSION_EX_PARAMS session = {};
        session.version = NV_ENC_OPEN_ENCODE_SESSION_EX_PARAMS_VER;
        session.deviceType = NV_ENC_DEVICE_TYPE_CUDA;
        session.device = cudaContext;
        session.apiVersion = NVENCAPI_VERSION;
        check(api.nvEncOpenEncodeSessionEx(&session, &encoder), "NvEncOpenEncodeSessionEx");
    }

    void destroyEncoderSession() {
        if (encoder) {
            api.nvEncDestroyEncoder(encoder);
            encoder = nullptr;
        }
    }

    NV_ENC_INITIALIZE_PARAMS makeInit(GUID presetGuid, uint32_t frameRate, int tuningInfo) {
        NV_ENC_INITIALIZE_PARAMS init = {};
        init.version = NV_ENC_INITIALIZE_PARAMS_VER;
        init.encodeGUID = NV_ENC_CODEC_H264_GUID;
        init.presetGUID = presetGuid;
        init.encodeWidth = width;
        init.encodeHeight = height;
        init.darWidth = width;
        init.darHeight = height;
        init.frameRateNum = frameRate;
        init.frameRateDen = 1;
        init.enableEncodeAsync = 0;
        init.enablePTD = 1;
        init.maxEncodeWidth = width;
        init.maxEncodeHeight = height;
#if NVENCAPI_MAJOR_VERSION >= 10
        if (tuningInfo >= 0) {
            init.tuningInfo = (NV_ENC_TUNING_INFO) tuningInfo;
        }
#endif
        return init;
    }

    bool getPresetConfig(GUID presetGuid, int tuningInfo, NV_ENC_CONFIG &config,
                         const char *attemptName, std::string &attempts) {
        NV_ENC_PRESET_CONFIG preset = {};
        preset.version = NV_ENC_PRESET_CONFIG_VER;
        preset.presetCfg.version = NV_ENC_CONFIG_VER;

#if NVENCAPI_MAJOR_VERSION >= 10
        NVENCSTATUS status;
        if (tuningInfo >= 0 && api.nvEncGetEncodePresetConfigEx) {
            status = api.nvEncGetEncodePresetConfigEx(
                    encoder, NV_ENC_CODEC_H264_GUID, presetGuid,
                    (NV_ENC_TUNING_INFO) tuningInfo, &preset);
        } else {
            status = api.nvEncGetEncodePresetConfig(
                    encoder, NV_ENC_CODEC_H264_GUID, presetGuid, &preset);
        }
#else
        (void) tuningInfo;
        NVENCSTATUS status = api.nvEncGetEncodePresetConfig(
                encoder, NV_ENC_CODEC_H264_GUID, presetGuid, &preset);
#endif

        if (status != NV_ENC_SUCCESS) {
            if (!attempts.empty()) attempts += "; ";
            attempts += attemptName;
            attempts += "-preset=";
            attempts += std::to_string((int) status);
            attempts += "(";
            attempts += statusName(status);
            attempts += ")";
            return false;
        }

        config = preset.presetCfg;
        config.version = NV_ENC_CONFIG_VER;
        return true;
    }

    // Apply a minimal H.264 configuration on top of the preset that NVENC returned.
    // Older drivers and recent Blackwell drivers reject some combinations that older
    // ReplayMod releases set unconditionally (explicit bit depths, outputAUD, etc.) so
    // we only override the rate-control essentials and leave the rest at preset defaults.
    void applyH264Config(NV_ENC_CONFIG &config, uint32_t frameRate, uint32_t bitrate, bool constQp) {
        config.profileGUID = NV_ENC_CODEC_PROFILE_AUTOSELECT_GUID;
        config.gopLength = frameRate * 2;
        config.frameIntervalP = 1;
        config.frameFieldMode = NV_ENC_PARAMS_FRAME_FIELD_MODE_FRAME;
        config.mvPrecision = NV_ENC_MV_PRECISION_DEFAULT;
#ifdef NV_ENC_RC_PARAMS_VER
        config.rcParams.version = NV_ENC_RC_PARAMS_VER;
#endif
        config.rcParams.zeroReorderDelay = 1;
        if (constQp) {
            config.rcParams.rateControlMode = NV_ENC_PARAMS_RC_CONSTQP;
            config.rcParams.averageBitRate = 0;
            config.rcParams.maxBitRate = 0;
            config.rcParams.vbvBufferSize = 0;
            config.rcParams.vbvInitialDelay = 0;
            config.rcParams.constQP.qpIntra = 20;
            config.rcParams.constQP.qpInterP = 23;
            config.rcParams.constQP.qpInterB = 23;
        } else {
            config.rcParams.rateControlMode = NV_ENC_PARAMS_RC_VBR;
            config.rcParams.averageBitRate = bitrate;
            config.rcParams.maxBitRate = bitrate * 2;
            config.rcParams.vbvBufferSize = std::max<uint32_t>(1, bitrate / std::max<uint32_t>(1, frameRate)) * 2;
            config.rcParams.vbvInitialDelay = config.rcParams.vbvBufferSize / 2;
        }
        config.encodeCodecConfig.h264Config.level = NV_ENC_LEVEL_AUTOSELECT;
        config.encodeCodecConfig.h264Config.idrPeriod = frameRate * 2;
        config.encodeCodecConfig.h264Config.repeatSPSPPS = 1;
        config.encodeCodecConfig.h264Config.chromaFormatIDC = 1;
        // outputAUD is intentionally NOT set - Blackwell drivers reject it for H.264.
#ifdef NV_ENC_BIT_DEPTH_8
        // NVENC API ≥ 12 introduced explicit bit-depth fields; the default value is
        // NV_ENC_BIT_DEPTH_INVALID (0) which the driver rejects, so always set 8-bit.
        config.encodeCodecConfig.h264Config.inputBitDepth = NV_ENC_BIT_DEPTH_8;
        config.encodeCodecConfig.h264Config.outputBitDepth = NV_ENC_BIT_DEPTH_8;
#endif
    }

    bool tryInitialize(const char *name, NV_ENC_INITIALIZE_PARAMS &init,
                       std::string &attempts, bool reopenAfterFailure) {
        NVENCSTATUS status = api.nvEncInitializeEncoder(encoder, &init);
        if (status == NV_ENC_SUCCESS) {
            return true;
        }
        if (!attempts.empty()) attempts += "; ";
        attempts += name;
        attempts += "=";
        attempts += std::to_string((int) status);
        attempts += "(";
        attempts += statusName(status);
        attempts += ")";
        destroyEncoderSession();
        if (reopenAfterFailure) {
            openEncodeSession();
        }
        return false;
    }

    void open(const std::string &path, uint32_t w, uint32_t h, uint32_t frameRate, uint32_t bitrate) {
        width = w;
        height = h;
        fps = frameRate;
        nvencDll = LoadLibraryA("nvEncodeAPI64.dll");
        if (!nvencDll) {
            throw std::runtime_error("failed to load nvEncodeAPI64.dll");
        }
        auto create = (NVENCSTATUS (NVENCAPI *)(NV_ENCODE_API_FUNCTION_LIST *))
                GetProcAddress(nvencDll, "NvEncodeAPICreateInstance");
        if (!create) {
            throw std::runtime_error("NvEncodeAPICreateInstance not found");
        }
        api.version = NV_ENCODE_API_FUNCTION_LIST_VER;
        check(create(&api), "NvEncodeAPICreateInstance");

        loadCuda();
        createCudaContext();

        openEncodeSession();

        std::string attempts;
        bool initialized = false;

        struct PresetAttempt {
            const char *name;
            GUID guid;
            int tuning;
        };
        std::vector<PresetAttempt> presetAttempts = {
#if NVENCAPI_MAJOR_VERSION >= 10
                {"p4-hq", NV_ENC_PRESET_P4_GUID, NV_ENC_TUNING_INFO_HIGH_QUALITY},
                {"p4-lowlatency", NV_ENC_PRESET_P4_GUID, NV_ENC_TUNING_INFO_LOW_LATENCY},
                {"p1-lowlatency", NV_ENC_PRESET_P1_GUID, NV_ENC_TUNING_INFO_LOW_LATENCY},
                {"p5-hq", NV_ENC_PRESET_P5_GUID, NV_ENC_TUNING_INFO_HIGH_QUALITY},
#else
                {"legacy-hq", NV_ENC_PRESET_HQ_GUID, -1},
                {"legacy-lowlatency-hq", NV_ENC_PRESET_LOW_LATENCY_HQ_GUID, -1},
                {"legacy-lowlatency-hp", NV_ENC_PRESET_LOW_LATENCY_HP_GUID, -1},
                {"legacy-default", NV_ENC_PRESET_DEFAULT_GUID, -1},
                {"legacy-hp", NV_ENC_PRESET_HP_GUID, -1},
#endif
        };

        for (const auto &presetAttempt : presetAttempts) {
            NV_ENC_CONFIG cbrConfig = {};
            if (getPresetConfig(presetAttempt.guid, presetAttempt.tuning, cbrConfig, presetAttempt.name, attempts)) {
                applyH264Config(cbrConfig, frameRate, bitrate, false);
                NV_ENC_INITIALIZE_PARAMS init = makeInit(presetAttempt.guid, frameRate, presetAttempt.tuning);
                init.encodeConfig = &cbrConfig;
                std::string name = std::string(presetAttempt.name) + "-cbr";
                initialized = tryInitialize(name.c_str(), init, attempts, true);
            }
            if (initialized) {
                break;
            }

            NV_ENC_CONFIG constQpConfig = {};
            if (getPresetConfig(presetAttempt.guid, presetAttempt.tuning, constQpConfig, presetAttempt.name, attempts)) {
                applyH264Config(constQpConfig, frameRate, bitrate, true);
                NV_ENC_INITIALIZE_PARAMS init = makeInit(presetAttempt.guid, frameRate, presetAttempt.tuning);
                init.encodeConfig = &constQpConfig;
                std::string name = std::string(presetAttempt.name) + "-constqp";
                initialized = tryInitialize(name.c_str(), init, attempts, true);
            }
            if (initialized) {
                break;
            }

            NV_ENC_INITIALIZE_PARAMS init = makeInit(presetAttempt.guid, frameRate, presetAttempt.tuning);
            init.encodeConfig = nullptr;
            std::string name = std::string(presetAttempt.name) + "-preset-only";
            initialized = tryInitialize(name.c_str(), init, attempts, true);
            if (initialized) {
                break;
            }
        }
        if (!initialized) {
            std::string msg = "NvEncInitializeEncoder failed for all native encoder configurations: ";
            msg += attempts;
            throw std::runtime_error(msg);
        }

        NV_ENC_CREATE_BITSTREAM_BUFFER bs = {};
        bs.version = NV_ENC_CREATE_BITSTREAM_BUFFER_VER;
        check(api.nvEncCreateBitstreamBuffer(encoder, &bs), "NvEncCreateBitstreamBuffer");
        bitstream = bs.bitstreamBuffer;
        mp4.open(path, width, height, fps);
    }

    void encode(uint32_t frameId, uint32_t textureTarget, uint32_t textureId) {
        checkCu(cu.cuCtxPushCurrent(cudaContext), "cuCtxPushCurrent");
        CUgraphicsResource graphicsResource = nullptr;
        CUarray mappedArray = nullptr;
        NV_ENC_REGISTERED_PTR registeredResource = nullptr;
        NV_ENC_INPUT_PTR mappedInput = nullptr;
        bool mapped = false;

        try {
            checkCu(cu.cuGraphicsGLRegisterImage(
                    &graphicsResource, textureId, textureTarget, CU_GRAPHICS_REGISTER_FLAGS_READ_ONLY),
                    "cuGraphicsGLRegisterImage");
            checkCu(cu.cuGraphicsMapResources(1, &graphicsResource, nullptr), "cuGraphicsMapResources");
            mapped = true;
            checkCu(cu.cuGraphicsSubResourceGetMappedArray(&mappedArray, graphicsResource, 0, 0),
                    "cuGraphicsSubResourceGetMappedArray");

            NV_ENC_REGISTER_RESOURCE reg = {};
            reg.version = NV_ENC_REGISTER_RESOURCE_VER;
            reg.resourceType = NV_ENC_INPUT_RESOURCE_TYPE_CUDAARRAY;
            reg.width = width;
            reg.height = height;
            reg.pitch = width * 4;
            reg.resourceToRegister = mappedArray;
            reg.bufferFormat = NV_ENC_BUFFER_FORMAT_ABGR;
            reg.bufferUsage = NV_ENC_INPUT_IMAGE;
            check(api.nvEncRegisterResource(encoder, &reg), "NvEncRegisterResource");
            registeredResource = reg.registeredResource;

            NV_ENC_MAP_INPUT_RESOURCE map = {};
            map.version = NV_ENC_MAP_INPUT_RESOURCE_VER;
            map.registeredResource = reg.registeredResource;
            check(api.nvEncMapInputResource(encoder, &map), "NvEncMapInputResource");
            mappedInput = map.mappedResource;

            NV_ENC_PIC_PARAMS pic = {};
            pic.version = NV_ENC_PIC_PARAMS_VER;
            pic.inputWidth = width;
            pic.inputHeight = height;
            pic.inputPitch = width * 4;
            pic.frameIdx = frameId;
            pic.inputTimeStamp = frame;
            pic.inputDuration = 1;
            pic.inputBuffer = map.mappedResource;
            pic.outputBitstream = bitstream;
            pic.bufferFmt = map.mappedBufferFmt;
            pic.pictureStruct = NV_ENC_PIC_STRUCT_FRAME;
            if (frame == 0) {
                pic.encodePicFlags = NV_ENC_PIC_FLAG_FORCEIDR | NV_ENC_PIC_FLAG_OUTPUT_SPSPPS;
            }
            check(api.nvEncEncodePicture(encoder, &pic), "NvEncEncodePicture");
            lockPacket(frame == 0);
            frame++;
            if (mappedInput) {
                api.nvEncUnmapInputResource(encoder, mappedInput);
                mappedInput = nullptr;
            }
            if (registeredResource) {
                api.nvEncUnregisterResource(encoder, registeredResource);
                registeredResource = nullptr;
            }
        } catch (...) {
            if (mappedInput) api.nvEncUnmapInputResource(encoder, mappedInput);
            if (registeredResource) api.nvEncUnregisterResource(encoder, registeredResource);
            if (mapped) cu.cuGraphicsUnmapResources(1, &graphicsResource, nullptr);
            if (graphicsResource) cu.cuGraphicsUnregisterResource(graphicsResource);
            CUcontext popped = nullptr;
            cu.cuCtxPopCurrent(&popped);
            throw;
        }
        if (mapped) checkCu(cu.cuGraphicsUnmapResources(1, &graphicsResource, nullptr), "cuGraphicsUnmapResources");
        if (graphicsResource) checkCu(cu.cuGraphicsUnregisterResource(graphicsResource), "cuGraphicsUnregisterResource");
        CUcontext popped = nullptr;
        checkCu(cu.cuCtxPopCurrent(&popped), "cuCtxPopCurrent");
    }

    void lockPacket(bool keyHint) {
        NV_ENC_LOCK_BITSTREAM lock = {};
        lock.version = NV_ENC_LOCK_BITSTREAM_VER;
        lock.outputBitstream = bitstream;
        check(api.nvEncLockBitstream(encoder, &lock), "NvEncLockBitstream");
        bool key = keyHint || lock.pictureType == NV_ENC_PIC_TYPE_IDR || lock.pictureType == NV_ENC_PIC_TYPE_I;
        mp4.writeAnnexB((const uint8_t *) lock.bitstreamBufferPtr, lock.bitstreamSizeInBytes, key);
        check(api.nvEncUnlockBitstream(encoder, bitstream), "NvEncUnlockBitstream");
    }

    void close() {
        if (closed) return;
        closed = true;
        if (encoder) {
            NV_ENC_PIC_PARAMS eos = {};
            eos.version = NV_ENC_PIC_PARAMS_VER;
            eos.encodePicFlags = NV_ENC_PIC_FLAG_EOS;
            api.nvEncEncodePicture(encoder, &eos);
        }
        mp4.close();
        if (bitstream) api.nvEncDestroyBitstreamBuffer(encoder, bitstream);
        bitstream = nullptr;
        destroyEncoderSession();
        if (cudaContext) cu.cuCtxDestroy(cudaContext);
        cudaContext = nullptr;
        if (cudaDll) FreeLibrary(cudaDll);
        cudaDll = nullptr;
        if (nvencDll) FreeLibrary(nvencDll);
        nvencDll = nullptr;
    }

    ~Encoder() {
        try { close(); } catch (...) {}
    }
};

std::string jstringToUtf(JNIEnv *env, jstring s) {
    const char *chars = env->GetStringUTFChars(s, nullptr);
    if (!chars) throw std::runtime_error("failed to read Java string");
    std::string result(chars);
    env->ReleaseStringUTFChars(s, chars);
    return result;
}

void throwIo(JNIEnv *env, const std::exception &e) {
    jclass cls = env->FindClass("java/io/IOException");
    if (cls) env->ThrowNew(cls, e.what());
}

} // namespace

extern "C" __declspec(dllexport) jlong JNICALL
Java_com_replaymod_render_NativeOpenGlEncoder_nativeOpen(
        JNIEnv *env, jclass, jstring outputFile, jint width, jint height, jint fps, jint bitrate, jboolean) {
    try {
        auto *encoder = new Encoder();
        encoder->open(jstringToUtf(env, outputFile), (uint32_t) width, (uint32_t) height,
                      (uint32_t) fps, (uint32_t) bitrate);
        return (jlong) encoder;
    } catch (const std::exception &e) {
        throwIo(env, e);
        return 0;
    }
}

extern "C" __declspec(dllexport) void JNICALL
Java_com_replaymod_render_NativeOpenGlEncoder_nativeEncode(
        JNIEnv *env, jclass, jlong handle, jint frameId, jint textureTarget, jint textureId) {
    try {
        if (!handle) throw std::runtime_error("native encoder handle is null");
        ((Encoder *) handle)->encode((uint32_t) frameId, (uint32_t) textureTarget, (uint32_t) textureId);
    } catch (const std::exception &e) {
        throwIo(env, e);
    }
}

extern "C" __declspec(dllexport) void JNICALL
Java_com_replaymod_render_NativeOpenGlEncoder_nativeClose(JNIEnv *env, jclass, jlong handle) {
    try {
        if (!handle) return;
        Encoder *encoder = (Encoder *) handle;
        encoder->close();
        delete encoder;
    } catch (const std::exception &e) {
        throwIo(env, e);
    }
}

extern "C" __declspec(dllexport) void JNICALL
Java_com_replaymod_render_NativeOpenGlEncoder_nativeAbort(JNIEnv *, jclass, jlong handle) {
    if (!handle) return;
    Encoder *encoder = (Encoder *) handle;
    delete encoder;
}
