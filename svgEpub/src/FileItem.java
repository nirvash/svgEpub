import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


public class FileItem implements IFile {
	private File file = null;
	private Rectangle clipRect = null;

	public FileItem(File file, Rectangle clipRect) {
		this.file = file;
		this.clipRect = clipRect;
	}

	@Override
	public String getFilename() {
		return file.getName();
	}

	@Override
	public InputStream getInputStream() {
		try {
			return new FileInputStream(file);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Rectangle getClipRect() {
		return clipRect;
	}
	
	@Override
	public String getURI() {
		return file.toURI().toString();
	}
	

}
