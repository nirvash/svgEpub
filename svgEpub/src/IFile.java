import java.awt.Rectangle;
import java.io.InputStream;


public interface IFile {
	String getFilename();
	InputStream getInputStream();
	Rectangle getClipRect();
	String getURI();
}
