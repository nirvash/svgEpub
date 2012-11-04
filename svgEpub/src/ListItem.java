import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;


public class ListItem implements IFile {
	private File file;

	private ZipFile zipFile;
	private String entryName;
	
	private Archive rarFile;
	private FileHeader entryHeader;
	
	private File svgFile = null;
	private boolean isConvertToSVG = false;
	private boolean canConvertToSVG = true;
	
	private ArrayList<ClipListItem> clipRectList = new ArrayList<ClipListItem>();
	private int selectedClipIndex = 0;
	
	private static CustomProperties properties;
	public static void setProperties(CustomProperties properties) {
		ListItem.properties = properties;
	}
	
	public ListItem(File file) {
		this.zipFile = null;
		this.entryName = null;
		this.rarFile = null;
		this.entryHeader = null;
		this.file = file;
		if (PathUtil.isRasterFile(file)) {
			this.isConvertToSVG = false;
		} else if (PathUtil.isSvgFile(file)) {
			this.canConvertToSVG = false;
		}
		initClipRect();
	}
	
	public ListItem(File file, ZipFile zipFile, String entryName) {
		this.zipFile = zipFile;
		this.entryName = entryName;
		this.rarFile = null;
		this.entryHeader = null;
		this.file = file;
		if (PathUtil.isRasterFile(entryName)) {
			this.isConvertToSVG = false;
		} else if (PathUtil.isSvgFile(entryName)) {
			this.canConvertToSVG = false;
		}
		initClipRect();
	}

	public ListItem(File file, Archive rarFile, FileHeader fh) {
		this.zipFile = null;
		this.entryName = null;
		this.rarFile = rarFile;
		this.entryHeader = fh;
		this.file = file;
		if (PathUtil.isRasterFile(fh.getFileNameString())) {
			this.isConvertToSVG = false;
		} else if (PathUtil.isSvgFile(fh.getFileNameString())) {
			this.canConvertToSVG = false;
		}
		initClipRect();
	}
	
	private void initClipRect() {
		this.clipRectList.clear();
		this.clipRectList.add(new ClipListItem(null, "clip 01"));
	}

	public boolean canConvertToSVG() {
		return this.canConvertToSVG;
	}
	
	public boolean isConvertToSVG() {
		return isConvertToSVG;
	}
	
	public void setConvertToSVG(boolean isConvert) {
		this.isConvertToSVG = isConvert;
	}
	
	public String toString() {
		if (zipFile != null) {
			return entryName;
		} else if (rarFile != null) {
			return entryHeader.getFileNameString();
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
		} else if (rarFile != null) {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				rarFile.extractFile(entryHeader, out);
				byte[] data = out.toByteArray();
				out.close();
				ByteArrayInputStream in = new ByteArrayInputStream(data);
				return in;
//				return rarFile.getInputStream(entryHeader);
			} catch (Exception e) {
				e.printStackTrace();
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
		if (clipRectList.size() > 0 && clipRectList.size() > selectedClipIndex) {
			ClipListItem item = clipRectList.get(selectedClipIndex);
			return item.getClipRect();
		} else {
			return null;
		}
	}
	

	public int getSelectedClipIndex() {
		return selectedClipIndex;
	}
	
	public void setSelectedClipIndex(int clipIindex) {
		this.selectedClipIndex = clipIindex;
	}

	public ArrayList<ClipListItem> getClipList() {
		return this.clipRectList;
	}
	
	public void setClipList(ArrayList<ClipListItem> list) {
		this.clipRectList = list;
	}


	public void setClipRect(Rectangle clipRect) {
		ClipListItem item = clipRectList.get(this.selectedClipIndex);
		item.setClipRect(clipRect);
	}
	
	public void resetClipRect() {
		clipRectList.clear();
		clipRectList.add(new ClipListItem(null, "clip 01"));
		this.selectedClipIndex = 0;
	}

	

	public String getDirName() {
		if (zipFile != null) {
			String name = file.getName();
			int index = name.lastIndexOf(".");
			if (name.substring(index).equalsIgnoreCase(".zip")) {
				name = name.substring(0, index);
			}
			return name;
		} else if (rarFile != null) {
			String name = file.getName();
			int index = name.lastIndexOf(".");
			if (name.substring(index).equalsIgnoreCase(".rar")) {
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
		} else if (rarFile != null) {
			return entryHeader.getFileNameString();
		} else {
			return file.getName();
		}
	}
	
	public String getURI() {
		boolean isDebug = false;
		if (properties != null) {
			isDebug = properties.getProperty("debug", "no").equals("yes");
		}
		if (zipFile != null || rarFile != null) {
			String path = PathUtil.getTmpDirectory();
			path += UUID.randomUUID().toString() + "." + PathUtil.getExtension(getFilename());
			File tmpFile = new File(path);
			if (!tmpFile.exists()) {
				if (!tmpFile.getParentFile().exists()) {
					tmpFile.getParentFile().mkdirs();
					tmpFile.getParentFile().deleteOnExit();
				}
					
				tmpFile.deleteOnExit();
				InputStream in = getInputStream();
				try {
					ImageUtility.copyFile(in, tmpFile);
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			if (isDebug) {
				tmpFile = LayoutAnalyzer.analyzePageLayout(tmpFile);
			}
			return tmpFile.toURI().toString();
		} else {
			if (isDebug) {
				String path = PathUtil.getTmpDirectory();
				path += UUID.randomUUID().toString() + "." + PathUtil.getExtension(getFilename());
				File tmpFile = new File(path);
				if (!tmpFile.exists()) {
					if (!tmpFile.getParentFile().exists()) {
						tmpFile.getParentFile().mkdirs();
						tmpFile.getParentFile().deleteOnExit();
					}
						
					tmpFile.deleteOnExit();
					InputStream in = getInputStream();
					try {
						ImageUtility.copyFile(in, tmpFile);
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					}
				}
				tmpFile = LayoutAnalyzer.analyzePageLayout(tmpFile);
	
				return tmpFile.toURI().toString();
			} else {
				return file.toURI().toString();
			}
		}
	}

	public void analyze() {
		if (!canConvertToSVG) return;
		if (PathUtil.isSvgFile(getFilename())) return;

		this.isConvertToSVG = ImageUtility.isTextImage(this);
	}
}
