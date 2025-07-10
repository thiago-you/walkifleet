package you.thiago.walkifleet;

import android.util.Log;

public class Opus
{
    private long _encoder = 0;
    private long _decoder = 0;
    private short[] _encodeBuffer;
    private short[] _decodeBuffer;
    private byte[] _encodedBytes;

    static
    {
        Log.i("Opus", String.format("Initializing Opus ..."));

        // Load .so
        System.loadLibrary("opus");
        System.loadLibrary("opus_wrapper");

    }

    public Opus(int audioSampleRate, int frameSizeInMs)
    {
        _encodeBuffer = new short[audioSampleRate / 1000 * frameSizeInMs];
        _decodeBuffer = new short[audioSampleRate / 1000 * frameSizeInMs];
        _encodedBytes = new byte[16384];

        _encoder = CreateEncoder(audioSampleRate);
        _decoder = CreateDecoder(audioSampleRate);
    }

    public byte[] Encode(short[] pcm)
    {
        int i = 0;
        while (i < _encodeBuffer.length)
        {
            _encodeBuffer[i] = pcm[i];
            i++;
        }

        int result = Encode(_encoder, _encodeBuffer, _encodeBuffer.length, _encodedBytes, _encodedBytes.length);
        if (result < 0)
        {
            Log.e("Opus", String.format("opus_encode error - %d)", result));
            return null;
        }

        byte[] bytes = new byte[result];
        System.arraycopy(_encodedBytes, 0, bytes, 0, result);
        return bytes;
    }

    public short[] Decode(byte[] encoded)
    {
        int result = Decode(_decoder, encoded, encoded.length, _decodeBuffer, _decodeBuffer.length);
        if (result != _decodeBuffer.length)
        {
            Log.e("Opus", String.format("opus_decode error - %d", result));
            return null;
        }

        short[] pcm = new short[result];
        System.arraycopy(_decodeBuffer, 0, pcm, 0, result);
        return pcm;
    }

    public void Close()
    {
        if (_encoder != 0)
        {
            DestroyEncoder(_encoder);
            _encoder = 0;
        }
        if (_decoder != 0)
        {
            DestroyDecoder(_decoder);
            _decoder = 0;
        }
    }

    private static native long CreateEncoder(int audioSampleRate);
    private static native long CreateDecoder(int audioSampleRate);
    private static native void DestroyEncoder(long encoder);
    private static native void DestroyDecoder(long decoder);
    private static native int Encode(long encoder, short[] pcm, int pcmSize, byte[] encoded, int encodedSize);
    private static native int Decode(long decoder, byte[] encoded, int encodedSize, short[] pcm, int pcmSize);
}
