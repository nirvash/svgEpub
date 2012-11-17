package com.github.nirvash.svgEpub;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import com.github.nirvash.svgEpub.layout.LayoutAnalyzer;
import com.github.nirvash.svgEpub.layout.LayoutElement;
import com.github.nirvash.svgEpub.list.IFile;
import com.github.nirvash.svgEpub.list.ListItem;
import com.github.nirvash.svgEpub.util.ImageUtility;
import com.github.nirvash.svgEpub.util.PathUtil;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.TessAPI.ETEXT_DESC;
import net.sourceforge.tess4j.TessAPI.TessResultIterator;
import net.sourceforge.tess4j.TessAPI1.TessPageIteratorLevel;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.vietocr.ImageIOHelper;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

public class Ocr {
	private TessAPI api = null;
	private TessAPI.TessBaseAPI handle = null;
	
	public class OcrResult {
		public int confidence = 0;
		public String text = "";
		public int getCodePoint() {
			if (text.length()>0) {
				return Character.codePointAt(text, 0);
			}
			return 0;
		}
	};
	
	public void init() {
		api = TessAPI.INSTANCE;
		handle = api.TessBaseAPICreate();
		api.TessBaseAPIInit3(handle, "tessdata", "jpn");
	}
	
	public void release() {
		if (api != null && handle != null) {
			api.TessBaseAPIDelete(handle);
			api = null;
			handle = null;
		}
	}
	
	public OcrResult doOcr(BufferedImage image) {
		if (image == null) return null;
		ByteBuffer buf = ImageIOHelper.convertImageData(image);
		int bpp = image.getColorModel().getPixelSize();
		int bytespp = bpp / 8;
		int bytespl = (int) Math.ceil(image.getWidth() * bpp / 8.0);
		
		api.TessBaseAPISetImage(handle, buf, image.getWidth(), image.getHeight(), bytespp, bytespl);
		api.TessBaseAPISetPageSegMode(handle, TessAPI.TessPageSegMode.PSM_SINGLE_CHAR);

		OcrResult result = new OcrResult();
		result.text = api.TessBaseAPIGetUTF8Text(handle);
		result.text = result.text.trim();
		result.text = result.text.replaceAll("\n", "");
		result.confidence = api.TessBaseAPIMeanTextConf(handle);
		
		return result;
	}
	
	public void test(IFile item) {
		if (api == null) {
			init();
		}
		
		try {
			BufferedImage image = binalize(item);
			if (image == null) return;
			OcrResult result = doOcr(image);
			if (result == null) return;

			File out = new File("out.txt");
			FileOutputStream os = new FileOutputStream(out);
			os.write(result.text.getBytes());
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void test2(IFile item) {
		if (api == null) {
			init();
		}
		
		try {
			File file = binalizeToFile(item);
			if (file == null) return;
			
			api.TessBaseAPISetVariable(handle, "tessedit_create_boxfile", "1");
			
			String text = api.TessBaseAPIProcessPages(handle, file.getAbsolutePath(), null, 0);

			File out = new File("out.txt");
			FileOutputStream os = new FileOutputStream(out);
			os.write(text.getBytes());
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static BufferedImage binalize(IFile item) {
		String path = PathUtil.getTmpDirectory();
		path += UUID.randomUUID().toString() + "." + PathUtil.getExtension(item.getFilename());
		File tmpFile = new File(path);
		if (!tmpFile.exists()) {
			if (!tmpFile.getParentFile().exists()) {
				tmpFile.getParentFile().mkdirs();
				tmpFile.getParentFile().deleteOnExit();
			}
				
			tmpFile.deleteOnExit();
			InputStream in = item.getInputStream();
			try {
				PathUtil.copyFile(in, tmpFile);
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		double scale = 1;
		IplImage image_source = cvLoadImage(tmpFile.getPath());
		CvSize size_target = new CvSize((int)(image_source.width()*scale), (int)(image_source.height()*scale));
		IplImage image_binary = cvCreateImage( size_target, IPL_DEPTH_8U, 1);
		ImageUtility.binalize(image_source, image_binary, true);		

		BufferedImage bImage = image_binary.getBufferedImage();
		cvReleaseImage(image_source);
		cvReleaseImage(image_binary);
		return bImage;
	}	
	
	private static File binalizeToFile(IFile item) {
		String path = PathUtil.getTmpDirectory();
		path += UUID.randomUUID().toString() + "." + PathUtil.getExtension(item.getFilename());
		File tmpFile = new File(path);
		if (!tmpFile.exists()) {
			if (!tmpFile.getParentFile().exists()) {
				tmpFile.getParentFile().mkdirs();
				tmpFile.getParentFile().deleteOnExit();
			}
				
			tmpFile.deleteOnExit();
			InputStream in = item.getInputStream();
			try {
				PathUtil.copyFile(in, tmpFile);
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		double scale = 1;
		IplImage image_source = cvLoadImage(tmpFile.getPath());
		CvSize size_target = new CvSize((int)(image_source.width()*scale), (int)(image_source.height()*scale));
		IplImage image_binary = cvCreateImage( size_target, IPL_DEPTH_8U, 1);
		ImageUtility.binalize(image_source, image_binary, true);		

		cvSaveImage(path, image_binary);
		cvReleaseImage(image_source);
		cvReleaseImage(image_binary);
		return tmpFile;
	}

	public static void createBoxFile(ListItem item) {
		ArrayList<LayoutElement> elements = new ArrayList<LayoutElement>();

		try {
			InputStream in = item.getInputStream();
			
			File file = new File(PathUtil.getTmpDirectory()+"work.png");
			file.deleteOnExit();
			try {
				PathUtil.copyFile(in, file);
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			IplImage image_source = cvLoadImage(file.getPath());
			double scale = 1;
			CvSize size_target = new CvSize((int)(image_source.width()*scale), (int)(image_source.height()*scale));
			IplImage image_binary = cvCreateImage( size_target, IPL_DEPTH_8U, 1);
			
			LayoutAnalyzer.createBoxImage(image_source, image_binary, elements, scale);
			in.close();
			cvReleaseImage(image_source);
			cvReleaseImage(image_binary);
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}	
}
