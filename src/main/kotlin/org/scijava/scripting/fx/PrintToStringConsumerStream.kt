package org.scijava.scripting.fx

import java.io.IOException
import java.io.OutputStream


class PrintToStringConsumerStream(private val target: (String) -> Unit) : OutputStream() {

    @Throws(IOException::class)
    override fun write(var1: Int) {
        NotImplementedError("Not supported!")
    }

    @Throws(IOException::class)
    override fun write(data: ByteArray) {
        this.write(data, 0, data.size)
    }

    @Throws(IOException::class)
    override fun write(data: ByteArray?, start: Int, size: Int) {
        if (data == null) {
            throw NullPointerException()
        } else if (start >= 0 && start <= data.size && size >= 0 && start + size <= data.size && start + size >= 0) {
            target(String(data, start, size))
        } else {
            throw IndexOutOfBoundsException()
        }
    }

    @Throws(IOException::class)
    override fun flush() {
    }

    @Throws(IOException::class)
    override fun close() {
    }


}