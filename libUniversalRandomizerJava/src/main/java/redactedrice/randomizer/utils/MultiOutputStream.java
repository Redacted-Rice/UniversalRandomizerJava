package redactedrice.randomizer.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class MultiOutputStream extends OutputStream {
    private final List<OutputStream> streams;

    public MultiOutputStream(OutputStream... streams) {
        this.streams = new ArrayList<>();
        for (OutputStream stream : streams) {
            if (stream != null) {
                this.streams.add(stream);
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        for (OutputStream stream : streams) {
            stream.write(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (OutputStream stream : streams) {
            stream.write(b, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        for (OutputStream stream : streams) {
            stream.flush();
        }
    }

    @Override
    public void close() throws IOException {
        for (OutputStream stream : streams) {
            stream.close();
        }
    }
}
