import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.TessAPI.ETEXT_DESC;
import net.sourceforge.tess4j.TessAPI.TessResultIterator;
import net.sourceforge.tess4j.TessAPI1.TessPageIteratorLevel;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.vietocr.ImageIOHelper;


public class Ocr {
	public static void test(IFile item) {
		File file = ImageUtility.convertToBitmap(item, null);
		
		Tesseract tess = Tesseract.getInstance();
		try {
/*
 * 			String result = tess.doOCR(file);
			JOptionPane.showMessageDialog(null, result);
 */
			
			TessAPI api = TessAPI.INSTANCE;
			TessAPI.TessBaseAPI handle = api.TessBaseAPICreate();
			
			InputStream stream = item.getInputStream();
			BufferedImage image = ImageIO.read(stream);
			stream.close();
			ByteBuffer buf = ImageIOHelper.convertImageData(image);
			int bpp = image.getColorModel().getPixelSize();
			int bytespp = bpp / 8;
			int bytespl = (int) Math.ceil(image.getWidth() * bpp / 8.0);
			
			api.TessBaseAPIInit3(handle, "tessdata", "eng");
			api.TessBaseAPISetImage(handle, buf, image.getWidth(), image.getHeight(), bytespp, bytespl);
			api.TessBaseAPISetPageSegMode(handle, TessAPI.TessPageSegMode.PSM_AUTO);
			
			String hocr = api.TessBaseAPIGetHOCRText(handle, 0);
			// JOptionPane.showMessageDialog(null, hocr);

			File out = new File("out.html");
			FileOutputStream os = new FileOutputStream(out);
			os.write(hocr.getBytes());
			os.close();
			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
}
