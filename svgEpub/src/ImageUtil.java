import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_objdetect;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

public class ImageUtil {
	static {
		Loader.load(opencv_objdetect.class);
	}

	public static File convertToBitmap(File imageFile) {
		String path = getTmpDirectory();
		String tmpFilename = path + "tmp" + getExtension(imageFile);
		File tmpFile = new File(tmpFilename);
		tmpFile.deleteOnExit();
		File tmpOutFile = new File(path + "tmpout.bmp");
		tmpOutFile.deleteOnExit();
		try {
			copyFile(imageFile, tmpFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		String outFilename = path + imageFile.getName();
		outFilename = outFilename.replaceAll("\\.[^.]*$", ".bmp");
		
		IplImage source_image = cvLoadImage(tmpFile.getPath());
		
		// Binalize
		int scale = 2;
		CvSize image_size = new CvSize(source_image.width()*scale, source_image.height()*scale);

		IplImage grey_image = cvCreateImage( source_image.cvSize(), IPL_DEPTH_8U, 1);
		cvCvtColor( source_image, grey_image, CV_BGR2GRAY );
		cvReleaseImage(source_image);
				
		IplImage target_image = cvCreateImage( image_size, IPL_DEPTH_8U, 1 );
		
		cvResize(grey_image, target_image, CV_INTER_LANCZOS4);
		cvReleaseImage(grey_image);

		IplImage threshold_image = cvCreateImage( image_size, IPL_DEPTH_8U, 1 );
		
		int blockSize = 31;
		cvAdaptiveThreshold( target_image , threshold_image, 255,
				 CV_ADAPTIVE_THRESH_MEAN_C,
				 CV_THRESH_BINARY, blockSize, 5 );
		
		cvSaveImage(tmpOutFile.getPath(), threshold_image);
		cvReleaseImage(target_image);
		cvReleaseImage(threshold_image);

		File outFile = new File(outFilename);
		outFile.deleteOnExit();
		try {
			copyFile(tmpOutFile, outFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		
		return outFile;
	}
	
	private static String getExtension(File file) {
		int index = file.getName().lastIndexOf(".");
		if (index > 0) {
			return file.getName().substring(index);
		}
		return null;
	}

	static private String getTmpDirectory() {
		String path = System.getProperty("java.io.tmpdir") + "/svgEpub/";
		File tmp = new File(path);
		if (!tmp.exists()) {
			tmp.mkdirs();
		}
		return path;
	}
	
	public static void copyFile(File source, File dest) throws IOException {
		if (!dest.exists()) {
			dest.createNewFile();
		}
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(source);
			out = new FileOutputStream(dest);

			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}
}
