import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;


public class ListItem implements IFile {
	private File file;
	private File svgFile = null;
	private boolean isSelected = false;
	private boolean enableSelect = true;
	private Rectangle clipRect = null;
	
	public ListItem(File value) {
		this.file = value;
		if (PathUtil.isRasterFile(file)) {
			this.isSelected = true;
		} else if (PathUtil.isSvgFile(value)) {
			this.enableSelect = false;
		}
	}
	
	public boolean enableSelect() {
		return this.enableSelect;
	}
	
	public boolean isSelected() {
		return isSelected;
	}
	
	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}
	
	public String toString() {
		return file.getName();
	}

	public File getFile() {
		return file;
	}
	
	public InputStream getInputStream() {
		try {
			return new FileInputStream(file);
		} catch (Exception e) {
			return null;
		}
	}
	
	public File getSvgFile() {
		return svgFile;
	}
	
	public void setSvgFile(File file) {
		this.svgFile = file;
	}

	public Rectangle getClipRect() {
		return clipRect;
	}

	public void setClipRect(Rectangle clipRect) {
		this.clipRect = clipRect;
	}

	public String getDirName() {
		return file.getParentFile().getName();
	}

	public String getFilename() {
		return file.getName();
	}
	
	public String getURI() {
		return file.toURI().toString();
	}

}
