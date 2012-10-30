import java.io.File;

import javax.swing.JOptionPane;

import net.sourceforge.tess4j.Tesseract;


public class Ocr {
	public static void test(IFile item) {
		File file = ImageUtil.convertToBitmap(item, null);

		Tesseract instance = Tesseract.getInstance();
		try {
			String result = instance.doOCR(file);
			JOptionPane.showInputDialog(result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
