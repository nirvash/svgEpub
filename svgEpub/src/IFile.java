import java.awt.Rectangle;
import java.io.InputStream;
import java.util.ArrayList;


public interface IFile {
	String getFilename();
	InputStream getInputStream();
	Rectangle getClipRect();
	ArrayList<ClipListItem> getClipList();
	String getURI();
}
