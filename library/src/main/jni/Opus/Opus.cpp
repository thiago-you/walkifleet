//
// Created by Denis on 18.06.2019.
//

#include <jni.h>
#include <dlfcn.h>
#include "Opus.h"
#include "Include/opus.h"


OpusEncoder* (*opusEncoderCreate)( opus_int32 Fs, int channels, int application, int *error);
OpusDecoder* (*opusDecoderCreate)( opus_int32 Fs, int channels, int *error);
void (*opusEncoderDestroy)(OpusEncoder* opusEncoder);
void (*opusDecoderDestroy)(OpusDecoder* opusDecoder);

opus_int32 (*opusEncode)(OpusEncoder *st, const opus_int16 *pcm, int frame_size, unsigned char *data, opus_int32 max_data_bytes);
int (*opusDecode)(OpusDecoder *st, const unsigned char *data, opus_int32 len, opus_int16 *pcm, int frame_size, int decode_fec);


extern "C" JNIEXPORT jlong JNICALL Java_com_example_walkiefleetclientandroid_Opus_CreateEncoder(JNIEnv *env, jclass type, jint audioSampleRate)
{
    int result = OPUS_INTERNAL_ERROR;

    void *lib = dlopen("libopus.so", RTLD_LAZY);
    if (lib == NULL)
        return 0;

    *(void **)(&opusEncoderCreate) = dlsym(lib, "opus_encoder_create");
    if (opusEncoderCreate == NULL)
        return 0;

    *(void **)(&opusEncode) = dlsym(lib, "opus_encode");
    if (opusEncode == NULL)
        return 0;

    OpusEncoder* encoder = opusEncoderCreate(opus_int32(audioSampleRate), 1, OPUS_APPLICATION_VOIP, &result);
    if (result != OPUS_OK)
        return 0;

    return reinterpret_cast<long>(encoder);
}

extern "C" JNIEXPORT jlong JNICALL Java_com_example_walkiefleetclientandroid_Opus_CreateDecoder(JNIEnv *env, jclass type, jint audioSampleRate)
{
    int result = OPUS_INTERNAL_ERROR;

    void *lib = dlopen("libopus.so", RTLD_LAZY);
    if (lib == NULL)
        return 0;

    *(void **)(&opusDecoderCreate) = dlsym(lib, "opus_decoder_create");
    if (opusDecoderCreate == NULL)
        return 0;

    *(void **)(&opusDecode) = dlsym(lib, "opus_decode");
    if (opusDecode == NULL)
        return 0;

    OpusDecoder* decoder = opusDecoderCreate(opus_int32(audioSampleRate), 1, &result);
    if (result != OPUS_OK)
        return 0;

    return reinterpret_cast<long>(decoder);
}

extern "C" JNIEXPORT void JNICALL Java_com_example_walkiefleetclientandroid_Opus_DestroyEncoder(JNIEnv *env, jclass type, jlong encoder)
{
    int result = OPUS_INTERNAL_ERROR;

    void *lib = dlopen("libopus.so", RTLD_LAZY);
    if (lib == NULL)
        return;

    *(void **)(&opusEncoderDestroy) = dlsym(lib, "opus_encoder_destroy");
    if (opusEncoderDestroy == NULL)
        return;

    OpusEncoder* opusEncoder = reinterpret_cast<OpusEncoder *>(encoder);
    opusEncoderDestroy(opusEncoder);
}

extern "C" JNIEXPORT void JNICALL Java_com_example_walkiefleetclientandroid_Opus_DestroyDecoder(JNIEnv *env, jclass type, jlong decoder)
{
    int result = OPUS_INTERNAL_ERROR;

    void *lib = dlopen("libopus.so", RTLD_LAZY);
    if (lib == NULL)
        return;

    *(void **)(&opusDecoderDestroy) = dlsym(lib, "opus_decoder_destroy");
    if (opusDecoderDestroy == NULL)
        return;

    OpusDecoder* opusDecoder = reinterpret_cast<OpusDecoder *>(decoder);
    opusDecoderDestroy(opusDecoder);
}

extern "C" JNIEXPORT jint JNICALL Java_com_example_walkiefleetclientandroid_Opus_Encode(JNIEnv *env, jclass type, jlong encoder, jshortArray pcm_, jint pcmSize, jbyteArray encoded_, jint encodedSize)
{
    jshort *pcm = env->GetShortArrayElements(pcm_, NULL);
    jbyte *encoded = env->GetByteArrayElements(encoded_, NULL);

    OpusEncoder* opusEncoder = reinterpret_cast<OpusEncoder *>(encoder);
    int result = opusEncode(opusEncoder, pcm, pcmSize, reinterpret_cast<unsigned char *>(encoded), encodedSize);

    env->ReleaseShortArrayElements(pcm_, pcm, 0);
    env->ReleaseByteArrayElements(encoded_, encoded, 0);

    return result;
}

extern "C" JNIEXPORT jint JNICALL Java_com_example_walkiefleetclientandroid_Opus_Decode(JNIEnv *env, jclass type, jlong decoder, jbyteArray encoded_, jint encodedSize, jshortArray pcm_, jint pcmSize)
{
    jbyte *encoded = env->GetByteArrayElements(encoded_, NULL);
    jshort *pcm = env->GetShortArrayElements(pcm_, NULL);

    OpusDecoder* opusDecoder = reinterpret_cast<OpusDecoder *>(decoder);
    int result = opusDecode(opusDecoder, reinterpret_cast<const unsigned char *>(encoded), encodedSize, pcm, pcmSize, 0);

    env->ReleaseByteArrayElements(encoded_, encoded, 0);
    env->ReleaseShortArrayElements(pcm_, pcm, 0);

    return result;
}