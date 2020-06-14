package com.gmail.marcosav2010.communicator

import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * The maximum size of array to allocate.
 * Some VMs reserve some header words in an array.
 * Attempts to allocate larger arrays may result in
 * OutOfMemoryError: Requested array size exceeds VM limit
 */
private const val MAX_BUFFER_SIZE = Int.MAX_VALUE - 8

/**
 * Reads up to a specified number of bytes from the input stream. This
 * method blocks until the requested number of bytes have been read, end
 * of stream is detected, or an exception is thrown. This method does not
 * close the input stream.
 *
 * <p> The length of the returned array equals the number of bytes read
 * from the stream. If {@code len} is zero, then no bytes are read and
 * an empty byte array is returned. Otherwise, up to {@code len} bytes
 * are read from the stream. Fewer than {@code len} bytes may be read if
 * end of stream is encountered.
 *
 * <p> When this stream reaches end of stream, further invocations of this
 * method will return an empty byte array.
 *
 * <p> Note that this method is intended for simple cases where it is
 * convenient to read the specified number of bytes into a byte array. The
 * total amount of memory allocated by this method is proportional to the
 * number of bytes read from the stream which is bounded by {@code len}.
 * Therefore, the method may be safely called with very large values of
 * {@code len} provided sufficient memory is available.
 *
 * <p> The behavior for the case where the input stream is <i>asynchronously
 * closed</i>, or the thread interrupted during the read, is highly input
 * stream specific, and therefore not specified.
 *
 * <p> If an I/O error occurs reading from the input stream, then it may do
 * so after some, but not all, bytes have been read. Consequently the input
 * stream may not be at end of stream and may be in an inconsistent state.
 * It is strongly recommended that the stream be promptly closed if an I/O
 * error occurs.
 *
 * @implNote
 * The number of bytes allocated to read data from this stream and return
 * the result is bounded by {@code 2*(long)len}, inclusive.
 *
 * @param len the maximum number of bytes to read
 * @return a byte array containing the bytes read from this input stream
 * @throws IllegalArgumentException if {@code length} is negative
 * @throws IOException if an I/O error occurs
 * @throws OutOfMemoryError if an array of the required size cannot be
 *         allocated.
 *
 * @since 11
 */
@Throws(IOException::class)
fun InputStream.readXBytes(len: Int): ByteArray {
    require(len >= 0) { "len < 0" }
    var bufs: MutableList<ByteArray>? = null
    var result: ByteArray? = null
    var total = 0
    var remaining = len
    var n: Int
    do {
        val buf = ByteArray(remaining.coerceAtMost(DEFAULT_BUFFER_SIZE))
        var nread = 0

        // read to EOF which may read more or less than buffer size
        while (read(buf, nread,
                        (buf.size - nread).coerceAtMost(remaining)).also { n = it } > 0) {
            nread += n
            remaining -= n
        }
        if (nread > 0) {
            if (MAX_BUFFER_SIZE - total < nread) {
                throw OutOfMemoryError("Required array size too large")
            }
            total += nread
            if (result == null) {
                result = buf
            } else {
                if (bufs == null) {
                    bufs = ArrayList()
                    bufs.add(result)
                }
                bufs.add(buf)
            }
        }
        // if the last call to read returned -1 or the number of bytes
        // requested have been read then break
    } while (n >= 0 && remaining > 0)
    if (bufs == null) {
        if (result == null) {
            return ByteArray(0)
        }
        return if (result.size == total) result else Arrays.copyOf(result, total)
    }
    result = ByteArray(total)
    var offset = 0
    remaining = total
    for (b in bufs) {
        val count = b.size.coerceAtMost(remaining)
        System.arraycopy(b, 0, result, offset, count)
        offset += count
        remaining -= count
    }
    return result
}