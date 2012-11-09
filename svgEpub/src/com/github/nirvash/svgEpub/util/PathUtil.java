package com.github.nirvash.svgEpub.util;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TreeSet;


public class PathUtil {
	static public String getExtension(String str) {
        String strs[] = str.split("\\.");
        return strs[strs.length - 1];
    }
	
	public static boolean isImageFile(File file) {
		return isSvgFile(file) || isRasterFile(file);
	}
	
	public static boolean isImageFile(String filepath) {
		return isSvgFile(filepath) || isRasterFile(filepath);
	}

	public static boolean isRasterFile(String filename) {
		return  hasExtension(filename, ".jpg")  ||
				hasExtension(filename, ".jpeg") ||
				hasExtension(filename, ".gif")  ||
				hasExtension(filename, ".png");
	}
	
	public static boolean isRasterFile(File file) {
		return file.isFile() && file.canRead() && isRasterFile(file.getName());
	}
	
	public static boolean isSvgFile(File file) {
		return file.isFile() && file.canRead() && hasExtension(file, ".svg");
	}
	
	public static boolean isSvgFile(String filename) {
		return hasExtension(filename, ".svg");
	}

	private static boolean hasExtension(String filename, String ext) {
		return filename.toLowerCase().endsWith(ext);
	}

	private static boolean hasExtension(File file, String ext) {
		return file.getName().toLowerCase().endsWith(ext);
	}
	
	public static boolean isZipFile(File file) {
		return file.isFile() && file.canRead() &&
				hasExtension(file, ".zip");
	}

	public static boolean isRarFile(File file) {
		return file.isFile() && file.canRead() &&
				hasExtension(file, ".rar");
	}

	public static String getTmpDirectory() {
		String path = System.getProperty("java.io.tmpdir") + "svgEpub\\";
		File tmp = new File(path);
		if (!tmp.exists()) {
			tmp.mkdirs();
		}
		return path;
	}

	public static File[] getFiles(File dir, FileFilter filter) {
		TreeSet<File> set = new TreeSet<File>();
		getFiles(set, dir, filter);
		return (File[])set.toArray(new File[set.size()]);
	}
	
	private static void getFiles(TreeSet<File> set, File dir, FileFilter filter) {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (filter.accept(file)) {
				set.add(file);
			} else if (file.isDirectory()) {
				getFiles(set, file, filter);
			}
		}
	}

	public static boolean isExist(String path) {
		File file = new File(path);
		return file.exists();
	}

	public static void copyFile(File source, File dest) throws IOException {
		try {
			FileInputStream in = new FileInputStream(source);
			PathUtil.copyFile(in, dest);
		} catch (Exception e) {
			
		}
	}

	public static void copyFile(InputStream source, File dest) throws IOException {
		if (!dest.exists()) {
			dest.createNewFile();
		}
		OutputStream out = null;
		try {
			out = new FileOutputStream(dest);
	
			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = source.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			if (source != null) {
				source.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}
}
