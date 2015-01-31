package eu.crushedpixel.replaymod.online;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;

public class CountingHttpEntity extends FileEntity {

	private OutputStreamProgress outstream;

	public class OutputStreamProgress extends OutputStream {

		private final OutputStream outstream;
		private volatile long bytesWritten=0;

		public OutputStreamProgress(OutputStream outstream) {
			this.outstream = outstream;
		}

		@Override
		public void write(int b) throws IOException {
			outstream.write(b);
			bytesWritten++;
		}

		@Override
		public void write(byte[] b) throws IOException {
			outstream.write(b);
			bytesWritten += b.length;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			outstream.write(b, off, len);
			bytesWritten += len;
		}

		@Override
		public void flush() throws IOException {
			outstream.flush();
		}

		@Override
		public void close() throws IOException {
			outstream.close();
		}

		public long getWrittenLength() {
			return bytesWritten;
		}
	}

	public CountingHttpEntity(File file, ContentType type) {
		super(file, type);
	}

	@Override
	public void writeTo(OutputStream outstream) throws IOException {
		this.outstream = new OutputStreamProgress(outstream);
		super.writeTo(this.outstream);
	}

	public float getProgress() {
		if (outstream == null) {
			return 0;
		}
		long contentLength = getContentLength();
		if (contentLength <= 0) {
			return 0;
		}
		long writtenLength = outstream.getWrittenLength();
		return (writtenLength/contentLength);
	}
}