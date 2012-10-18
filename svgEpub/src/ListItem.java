import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;


public class ListItem implements IFile {
	private File file;

	private ZipFile zipFile;
	private String entryName;
	
	private File svgFile = null;
	private boolean isSelected = false;
	private boolean enableSelect = true;
	private Rectangle clipRect = null;
	
	public ListItem(File file) {
		this.zipFile = null;
		this.entryName = null;
		this.file = file;
		if (PathUtil.isRasterFile(file)) {
			this.isSelected = true;
		} else if (PathUtil.isSvgFile(file)) {
			this.enableSelect = false;
		}
	}
	
	public ListItem(File file, ZipFile zipFile, String entryName) {
		this.zipFile = zipFile;
		this.entryName = entryName;
		this.file = file;
		if (PathUtil.isRasterFile(entryName)) {
			this.isSelected = true;
		} else if (PathUtil.isSvgFile(entryName)) {
			this.isSelected = false;
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
		if (zipFile != null) {
			return entryName;
		} else {
			return file.getName();
		}
	}

	public File getFile() {
		return file;
	}
	
	public InputStream getInputStream() {
		if (zipFile != null) {
			try {
				ZipArchiveEntry e = zipFile.getEntry(entryName);
				InputStream stream = zipFile.getInputStream(e);
				return stream;
			} catch (Exception e) {
				return null;
			}
		} else {
			try {
				return new FileInputStream(file);
			} catch (Exception e) {
				return null;
			}
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
		if (zipFile != null) {
			String name = file.getName();
			int index = name.lastIndexOf(".");
			if (name.substring(index).equalsIgnoreCase(".zip")) {
				name = name.substring(0, index);
			}
			return name;
		} else {
			return file.getParentFile().getName();
		}
	}

	public String getFilename() {
		if (zipFile != null) {
			return entryName;
		} else {
			return file.getName();
		}
	}
	
	public String getURI() {
		if (zipFile != null) {
			String path = PathUtil.getTmpDirectory();
			path += entryName;
			File tmpFile = new File(path);
			if (!tmpFile.exists()) {
				if (!tmpFile.getParentFile().exists()) {
					tmpFile.getParentFile().mkdirs();
					tmpFile.getParentFile().deleteOnExit();
				}
					
				tmpFile.deleteOnExit();
				InputStream in = getInputStream();
				try {
					ImageUtil.copyFile(in, tmpFile);
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			return tmpFile.toURI().toString();
		} else {
			return file.toURI().toString();
		}
	}

}
