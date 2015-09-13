package de.windowsfreak.testjni.codec;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This copies a source ByteBuffer which is not backed by a Byte[] array to a target ByteBuffer which is not backed by a Byte[] array.
 */
public class ByteCopyCodec implements Codec {
    @Override
    public int encode(ByteBuffer compressedData, ByteBuffer uncompressedData, int uncompressedLength) throws IOException {
        synchronized(compressedData) {
            synchronized (uncompressedData) {
                uncompressedData.rewind();
                compressedData.clear();
                byte[] tmp = new byte[uncompressedLength];
                uncompressedData.get(tmp);
                compressedData.put(tmp);
                compressedData.flip();
                return compressedData.limit();
            }
        }
    }

    @Override
    public void decode(ByteBuffer uncompressedData, int uncompressedLength, ByteBuffer compressedData, int compressedLength) throws IOException {
        new Exception("Warning! This method is not implemented.").printStackTrace();
    }
}