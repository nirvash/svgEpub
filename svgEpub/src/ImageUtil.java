import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.naming.InitialContext;
import javax.swing.JOptionPane;

import org.apache.batik.css.parser.ParseException;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.dom.util.XLinkSupport;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventTarget;

import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_legacy;
import com.googlecode.javacv.cpp.opencv_objdetect;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

public class ImageUtil {
	private static final String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
	
	public static void initialize(boolean isOpencvEnabled) {
	    if (isOpencvEnabled) {
	        Loader.load(opencv_objdetect.class);
	    }
	}

/*
 * 	public static File convertToBitmap(File imageFile, Rectangle imageSize) {
 */
	
	public static File convertToBitmap(IFile item, Rectangle imageSize) {
		String path = getTmpDirectory();
		String tmpFilename = path + UUID.randomUUID() + "." + PathUtil.getExtension(item.getFilename());
		File tmpFile = new File(tmpFilename);
		tmpFile.deleteOnExit();
		File tmpOutFile = new File(path + "tmpout.bmp");
		tmpOutFile.deleteOnExit();
/*		
		String outFilename = path + item.getFilename();
		outFilename = outFilename.replaceAll("\\.[^.]*$", ".bmp");
*/
		String outFilename = UUID.randomUUID() + ".bmp";
		IplImage image_source;
		
		try {
			copyFile(item.getInputStream(), tmpFile);
			image_source = cvLoadImage(tmpFile.getPath());
			if (image_source == null) {
				// workaround: Retry file copy (Sometimes it fails to decode rar archive?)
				tmpFile.delete();
				copyFile(item.getInputStream(), tmpFile);
				image_source = cvLoadImage(tmpFile.getPath());
				if (image_source == null) {
					JOptionPane.showMessageDialog(null, "Failed to convert " + item.getFilename());
					return null;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			tmpFile.delete();
		}
		
		double scale = 2;
		CvSize size_target = new CvSize((int)(image_source.width()*scale), (int)(image_source.height()*scale));
		if (imageSize != null) {
			imageSize.width = image_source.width();
			imageSize.height = image_source.height();
		}
		
		IplImage image_target = cvCreateImage( size_target, IPL_DEPTH_8U, 1 );
		try {
			if (!binalize(image_source, image_target, false)) {
				return null;
			}
			cvSaveImage(tmpOutFile.getPath(), image_target);
		} finally {
			cvReleaseImage(image_source);
			cvReleaseImage(image_target);
		}
		
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
	
	static boolean binalize(IplImage image_source, IplImage image_target, boolean isNot) {

		CvSize size_source = image_source.cvSize();
		CvSize size_target = image_target.cvSize();
		
		// Color detection
		IplImage image_hsv = cvCreateImage( size_source, IPL_DEPTH_8U, 3);
		cvCvtColor(image_source, image_hsv, CV_RGB2HSV);
		CvScalar mean = cvScalarAll(0);
		CvScalar std_dev= cvScalarAll(0);

		cvAvgSdv(image_hsv, mean, std_dev, null);
		boolean isColorImage = std_dev.val(1) > 3.0f;
		boolean containsIllust = std_dev.val(2) > 40.0f;
		boolean isComplicatedIllust = std_dev.val(2) > 85.0f;
		cvReleaseImage(image_hsv);
		
		// Binalize
		IplImage image_grey = cvCreateImage( size_source, IPL_DEPTH_8U, 1);
		cvCvtColor( image_source, image_grey, CV_BGR2GRAY );
				
		IplImage image_edge   = cvCreateImage( size_target, IPL_DEPTH_8U, 1 );
		
		cvResize(image_grey, image_target, CV_INTER_LANCZOS4);

		try {
			if (!isColorImage) {
				if (!containsIllust) {
					int blockSize = 31;
					int inv = isNot ? CV_THRESH_BINARY_INV : CV_THRESH_BINARY;
					cvAdaptiveThreshold( image_target , image_target, 255,
							 CV_ADAPTIVE_THRESH_MEAN_C,
							 inv, blockSize, 5 );
				} else if (isComplicatedIllust) {
					IplImage image_simple = cvCreateImage( size_source, IPL_DEPTH_8U, 1);
					cvSmooth (image_grey, image_simple, CV_BILATERAL, 10, 10, 60, 40);
					cvResize(image_simple, image_target, CV_INTER_LINEAR);
					int inv = isNot ? CV_THRESH_BINARY_INV : CV_THRESH_BINARY;
					cvThreshold(image_target, image_target, 0, 255,	inv | CV_THRESH_OTSU);
					cvReleaseImage(image_simple);
				} else {
					int blockSize = 41;
					cvAdaptiveThreshold( image_target , image_edge, 255,
							 CV_ADAPTIVE_THRESH_MEAN_C,
							 CV_THRESH_BINARY_INV, blockSize, 9 );
					
					IplImage image_beta = cvCreateImage( size_target, IPL_DEPTH_8U, 1 );
					cvThreshold(image_target, image_beta, 0, 255,	CV_THRESH_BINARY_INV | CV_THRESH_OTSU);
					
					cvOr(image_edge, image_beta, image_target, null);
					if (!isNot) {
						cvNot(image_target, image_target);
					}
					cvReleaseImage(image_beta);
				}
			} else {
				// color
				if (true) {
					return false;
				} else { // tone
					IplImage image_tone = cvCreateImage( size_source, IPL_DEPTH_8U, 1 );
					floydSteinberg(image_grey, image_tone);
					cvReleaseImage(image_grey);
					
					cvNot(image_tone, image_tone);
					
					IplImage image_tone2 = cvCreateImage( size_target, IPL_DEPTH_8U, 1 );
					cvResize(image_tone, image_tone2, CV_INTER_NN);
					cvReleaseImage(image_tone);
	
					IplImage mask = cvCreateImage( size_target, IPL_DEPTH_8U, 1 );
					cvThreshold(image_target, mask, 0, 255,	CV_THRESH_BINARY_INV | CV_THRESH_OTSU);
	
					cvCopy(image_edge, image_target);
					cvOr(image_edge, image_tone2, image_target, mask);
					cvNot(image_target, image_target);
					
					cvReleaseImage(image_tone2);
					cvReleaseImage(mask);
				}
			}
		} finally {
			cvReleaseImage(image_edge);
			cvReleaseImage(image_grey);
		}
		return true;		
	}
	
	static void floydSteinberg(IplImage in, IplImage out) {
		int[][] pattern = new int[][] {
				{ 0,  1, 7},
				{ 1, -1, 3},
				{ 1,  0, 5},
				{ 1,  1, 1}				
		};

		byte[] outBytes = new byte[in.widthStep() * in.height()];
		in.imageData().get(outBytes);
		
		int index;
		double e;
		for (int j=0; j<out.height()-1; j++) {
			for (int i=1; i<out.widthStep()-1; i++) {
				index = j * out.widthStep() + i;
				int c = (int)(outBytes[index] & 0xFF);
				if (c > 127) {
					e = (double)(c - 255);
					outBytes[index] = (byte)-1;

				} else {
					e = (double)c;
					outBytes[index] = (byte)0;
				}
				
				for (int[] pat : pattern) {
					index = (j + pat[0]) * out.widthStep() + (i + pat[1]);
					c = (int) (outBytes[index] & 0xff) + (int)(e * pat[2] / 16.0f);
					c = (c > 127) ? c - 256 : c;
					outBytes[index] = (byte)c;
				}
			}
		}
		
		out.imageData().put(outBytes);
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
	
	public static void copyFile(File source, File dest) throws IOException {
		try {
			FileInputStream in = new FileInputStream(source);
			copyFile(in, dest);
		} catch (Exception e) {
			
		}
	}

	public static Rectangle getImageSize(IFile item) {
    	String extension = PathUtil.getExtension(item.getFilename());
    	
    	Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix(extension);
        ImageReader imageReader = (ImageReader) readers.next();
    	Rectangle rect = new Rectangle();
    	
    	InputStream stream = null;
    	ImageInputStream imageInputStream = null;
    	for (int i=0; imageInputStream == null && i<3; i++) { // workaround : rar library somtimes fails to read...
			try {
				stream = item.getInputStream();
		        imageInputStream = ImageIO.createImageInputStream(stream);
		        imageReader.setInput(imageInputStream, false);
		        rect.width = imageReader.getWidth(0);
		        rect.height = imageReader.getHeight(0);
				stream.close();
				break;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (imageInputStream != null) { 
						imageInputStream.close();
						imageInputStream = null;
					}
					if (stream != null) {
						stream.close();
						stream = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
    	}

		return rect;
	}

	public static Rectangle getSvgSize(IFile item) {
		Rectangle rect = new Rectangle(0, 0, 584, 754);
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
		try {
			Document doc = f.createDocument(item.getFilename(), item.getInputStream());
			Element svgRoot = doc.getDocumentElement();
			
			Attr width = svgRoot.getAttributeNodeNS(null, "width");		
			if (width != null) {
				rect.width = parse(width.getValue());
			}
	
			Attr height = svgRoot.getAttributeNodeNS(null, "height");
			if (height != null) {
				rect.height = parse(height.getValue());
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, item.getFilename() + ":" + e.getMessage());
		}
		
		return rect;
	}

	public static Rectangle getSvgSize(String newURI) {
		Rectangle rect = new Rectangle(0, 0, 584, 754);
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
		try {
			Document doc = f.createDocument(newURI);
			Element svgRoot = doc.getDocumentElement();
			
			Attr width = svgRoot.getAttributeNodeNS(null, "width");		
			if (width != null) {
				rect.width = parse(width.getValue());
			}
	
			Attr height = svgRoot.getAttributeNodeNS(null, "height");
			if (height != null) {
				rect.height = parse(height.getValue());
			}
		} catch (Exception e) {
			
		}
		
		return rect;
	}

	private static int parse(String value) {
		Pattern p = Pattern.compile("(\\d+)(.*)");
		Matcher m = p.matcher(value);
		if (m.find()) {
			return Integer.parseInt(m.group(1));
		}
		throw new ParseException(null);
	}
	
	
	public static Document createSvgDocument(Rectangle clipRect, Rectangle imageRect, String imageURI, boolean isPreview, int margin) {
		DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
		if (clipRect == null) {
			clipRect = imageRect;
		}
		Rectangle rootRect = isPreview ? clipRect : imageRect;
		Document doc = impl.createDocument(svgNS, "svg", null);

		Element svgRootOuter = doc.getDocumentElement();

		svgRootOuter.setAttribute("id", "root");
		svgRootOuter.setAttributeNS(null , "width", "100%");
		svgRootOuter.setAttributeNS(null , "height", "100%");
		svgRootOuter.setAttributeNS(null, "viewBox", 
				String.format("%d %d %d %d",  -margin, -margin, rootRect.width + margin*2, rootRect.height + margin*2));
		svgRootOuter.setAttributeNS(null, "preserveAspectRatio", "xMidYMid meet");
		
		Element image = doc.createElementNS(svgNS, "image");
		image.setAttributeNS(null, "width", Integer.toString(imageRect.width));
		image.setAttributeNS(null, "height", Integer.toString(imageRect.height));
		XLinkSupport.setXLinkHref(image,  imageURI);
//		image.setAttributeNS(null, "xlink:href", imageURI);

		if (isPreview) {
			Element svgRootInner = (Element) doc.createElementNS(svgNS, "svg");
			svgRootInner.setAttribute("id", "root_inner");
			svgRootInner.setAttributeNS(null , "width", Integer.toString(clipRect.width));
			svgRootInner.setAttributeNS(null , "height", Integer.toString(clipRect.height));
			svgRootInner.setAttributeNS(null, "viewBox", 
					String.format("%d %d %d %d",  clipRect.x, clipRect.y, clipRect.width, clipRect.height));
			svgRootInner.setAttributeNS(null, "preserveAspectRatio", "xMidYMid slice");
			
			svgRootOuter.appendChild(svgRootInner);
			svgRootInner.appendChild(image);
		} else {
			svgRootOuter.appendChild(image);
		}
		return doc;
	}

	public static Rectangle getContentArea(IFile item) {
		Rectangle result = new Rectangle();
		String tmpFilename = "";
		try {
			if (PathUtil.isRasterFile(item.getFilename())) {
				String path = getTmpDirectory();
				tmpFilename = path + "tmp" + PathUtil.getExtension(item.getFilename());
				File tmpFile = new File(tmpFilename);
				tmpFile.deleteOnExit();
				try {
					copyFile(item.getInputStream(), tmpFile);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			} else {
				String path = getTmpDirectory();
				tmpFilename = path + "tmp.png";
				saveAsPNG(item, tmpFilename);
			}

			IplImage src_image = cvLoadImage(tmpFilename);
			IplImage gray_image = cvCreateImage(cvGetSize(src_image), IPL_DEPTH_8U, 1);
			cvCvtColor(src_image, gray_image, CV_BGR2GRAY);
			
			IplImage bi_image = cvCreateImage(cvGetSize(gray_image), IPL_DEPTH_8U, 1);
			cvThreshold(gray_image, bi_image, 0, 255, CV_THRESH_BINARY_INV | CV_THRESH_OTSU);
/*
			cvAdaptiveThreshold( gray_image , bi_image, 255,
					 CV_ADAPTIVE_THRESH_MEAN_C,
					 CV_THRESH_BINARY_INV, 31 , 5 );
*/
			CvMemStorage storage = CvMemStorage.create();
			CvSeq contours = new CvContour();
			int count = cvFindContours(bi_image, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);

			float scale = (float)bi_image.width() / 1600;
			boolean isFirst = true;
			while (contours != null && !contours.isNull()) {
                if (contours.elem_size() > 0) {
                	CvRect cr = cvBoundingRect(contours, 1);
                	Rectangle r = new Rectangle(cr.x(), cr.y(), cr.width(), cr.height());
                	if (r.width > 4.0*scale || r.height > 4.0*scale) {
	                	if (isFirst) {
	                		result.setBounds(r);
	                		isFirst = false;
	                	} else {
	                		result.add(r);
	                	}
                	}
                    CvSeq points = cvApproxPoly(contours, Loader.sizeof(CvContour.class),
                            storage, CV_POLY_APPROX_DP, cvContourPerimeter(contours)*0.02, 0);
                    cvDrawContours(src_image, points, CvScalar.BLUE, CvScalar.BLUE, -1, 1, CV_AA);
                }
                contours = contours.h_next();
            }

			cvReleaseImage(src_image);
			cvReleaseImage(gray_image);
			cvReleaseImage(bi_image);
			
//			cvClearMemStorage(storage);
//			cvReleaseMemStorage(storage);
//			storage.release();
//			cvReleaseMemStorage(contours.storage());
			storage.release();

		//	result = null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}

	private static void saveAsPNG(IFile item, String filepath) {
		try {
			PNGTranscoder t = new PNGTranscoder();
			InputStream stream = item.getInputStream();
			TranscoderInput input = new TranscoderInput(stream);
			File outFile = new File(filepath);
			outFile.deleteOnExit();
			OutputStream ostream = new FileOutputStream(outFile);
			TranscoderOutput output = new TranscoderOutput(ostream);
			
	        Rectangle rect = ImageUtil.getSvgSize(item);
	        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(rect.width));
	        t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, new Float(rect.height));
	        // Set the region.
	        t.addTranscodingHint(PNGTranscoder.KEY_AOI, rect);

	        t.addTranscodingHint(PNGTranscoder.KEY_FORCE_TRANSPARENT_WHITE, true);
	        t.addTranscodingHint(PNGTranscoder.KEY_BACKGROUND_COLOR, Color.white); 
	        
			t.transcode(input, output);
			
			ostream.flush();
			ostream.close();
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isTextImage(ListItem item) {
		String path = getTmpDirectory();
		String tmpFilename = path + UUID.randomUUID() + "." + PathUtil.getExtension(item.getFilename());
		File tmpFile = new File(tmpFilename);
		tmpFile.deleteOnExit();

		IplImage image_source;
		try {
			copyFile(item.getInputStream(), tmpFile);
			image_source = cvLoadImage(tmpFile.getPath());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			tmpFile.delete();
		}
		
		CvSize size_source = image_source.cvSize();
		
		// Color detection
		IplImage image_hsv = cvCreateImage( size_source, IPL_DEPTH_8U, 3);
		cvCvtColor(image_source, image_hsv, CV_RGB2HSV);
		CvScalar mean = cvScalarAll(0);
		CvScalar std_dev= cvScalarAll(0);

		cvAvgSdv(image_hsv, mean, std_dev, null);
		boolean isColorImage = std_dev.val(1) > 3.0f;
//		boolean containsIllust = std_dev.val(2) > 40.0f;
		boolean isComplicatedIllust = std_dev.val(2) > 85.0f;
		cvReleaseImage(image_hsv);
		cvReleaseImage(image_source);
		
		return isColorImage ? false : !isComplicatedIllust;
	}

	public static File layoutAnalysis(File file) {
		IplImage image_source = cvLoadImage(file.getPath());

		double scale = 1;
		CvSize size_target = new CvSize((int)(image_source.width()*scale), (int)(image_source.height()*scale));
		IplImage image_binary = cvCreateImage( size_target, IPL_DEPTH_8U, 1);
		
		binalize(image_source, image_binary, true);
		
		// Extract counters
		CvMemStorage storage = CvMemStorage.create();
		CvSeq contours = new CvContour();
		int count = cvFindContours(image_binary, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);
		ArrayList<Rectangle> rects = new ArrayList<Rectangle>();
		getRects(contours, rects);
		
		// Union intersected rects
		mergeRects(rects);
		
		// Remove small regions
		removeSmallRects(rects);

		// Draw rects
		Collections.sort(rects, new RectComparator());
//		drawRects(image_source, rects, 2, CvScalar.CYAN, CvScalar.GREEN);

		// Grouping
		ArrayList<LineElement> group = new ArrayList<LineElement>();
		getGroup(rects, group);
		drawGroup(image_source, group, 2, CvScalar.MAGENTA, CvScalar.CYAN);
		
		storage.release();
		
		cvSaveImage(file.getPath(), image_source);
		cvSaveImage("test.png", image_source);
		
		cvReleaseImage(image_binary);
		cvReleaseImage(image_source);
		return file;
	}


	private static void removeSmallRects(ArrayList<Rectangle> rects) {
		for (int i=rects.size()-1; i >= 0 ; i--) {
			Rectangle r1 = rects.get(i);
			if (r1.width * r1.height <= 9) {
				rects.remove(i);
			}
		}		
	}


	private static class LineElement {
		String label = "";
		Rectangle rect;
		ArrayList<Rectangle> elements = new ArrayList<Rectangle>();
		public LineElement(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return this.label;
		}
		
		public void add(LineElement l1) {
			if (rect == null) {
				rect = l1.rect;
			} else {
				rect.add(l1.rect);
			}
			elements.addAll(l1.elements);
		}

		public void add(Rectangle rectangle) {
			if (rect == null) {
				rect = rectangle;
			} else {
				rect.add(rectangle);
			}
			elements.add(new Rectangle(rectangle));
		}

		public void adjustElementWidth() {
			if (rect.height < rect.width*3) return;
			
			for (Rectangle r : elements) {
				r.x = rect.x;
				r.width = rect.width;
			}
			
			mergeRects(elements);
		}

		public LineElement extractRuby() {
			if (elements.size() < 5) return null;
			if (rect.height < rect.width*3) return null;
			
			ArrayList<Rectangle> list = new ArrayList<Rectangle>();
			double height = rect.width * 6;
			int split = (int)rect.height / (int)height;
			height += (rect.height % height) / split;
			
			for (int y=0; y + height < rect.height; y+=height/2) {
				ArrayList<Rectangle> sublist = new ArrayList<Rectangle>();
				getSubGroup(y, y+height, sublist);
				list.addAll(sublist);
			}
			
			if (list.size() <= 1) return null;
			for (int len = 0; len != list.size(); ) {
				len = list.size();
				mergeRects2(list);
			}
			
			if (list.size() != 2) return null;
			int bodyIndex = 0;
			int maxHeight = 0;
			for (int i=0; i<list.size(); i++) {
				if (maxHeight < list.get(i).height) {
					maxHeight = list.get(i).height;
					bodyIndex = i;
				}
			}
			list.remove(bodyIndex);
			LineElement ruby = new LineElement("ruby");
			
			for (Rectangle rubyRect : list) {
				for (int i=elements.size()-1; i>=0; i--) {
					Rectangle r = elements.get(i);
					if (!rubyRect.intersects(r)) continue;
					if (rubyRect.x <= r.getCenterX() && r.getCenterX() <= rubyRect.getMaxX()) {
						ruby.add(r);
						elements.remove(i);
					}
				}
				if (ruby.elements.isEmpty()) return null;
				if (elements.isEmpty()) {
					elements = ruby.elements;
					return null;
				}
			}
			
			calcBoundsRect();
			return ruby;
		}
		
		private void getSubGroup(int top, double bottom, ArrayList<Rectangle> sublist) {
			for (Rectangle r1 : elements) {
				if (!(top <= r1.getCenterY() && r1.getCenterY() < bottom)) continue;
				
				boolean found = false;
				for (int i=sublist.size()-1; i>=0; i--) {
					Rectangle r2 = sublist.get(i);
					if (isInside(r2.x, r2.getMaxX(), r1.getCenterX(), 0.9f)) {
						r2.add(r1);
						found = true;
						break;
					}
				}
				if (!found) {
					Rectangle r3 = new Rectangle(r1); 
					sublist.add(r3);
					for (Rectangle r4 : elements) {
						if (!(top <= r4.getCenterY() && r4.getCenterY() < bottom)) continue;
						if (isInside(r3.x, r3.getMaxX(), r4.getCenterX(), 0.9f)) {
							r3.add(r4);
						} else if (isInside(r4.x, r4.getMaxX(), r3.getCenterX(), 0.9f)) {
							r3.add(r4);
						}
					}					
				}
			}
		}

		private static double distance(Rectangle r1, Rectangle r2) {
			Point p1 = new Point((int)r1.getCenterX(), (int)r1.getCenterY());
			Point p2 = new Point((int)r2.getCenterX(), (int)r2.getCenterY());
			return p1.distance(p2);
		}

		private static boolean isInside(int left, double right, double x, float rate) {
			double margin = (right - left) * (1.0 - rate) / 2;
			left += margin;
			right -= margin;
			return (left < x && x < right);
		}

		private static void mergeRects2(ArrayList<Rectangle> rects) {
			for (int i=rects.size()-1; i >= 0 ; i--) {
				Rectangle r1 = rects.get(i);
				for (int j=0; j<i; j++) {
					Rectangle r2 = rects.get(j);
					boolean isOverwrap = false;
					isOverwrap |= isInside(r1.x, r1.getMaxX(), r2.getCenterX(), 0.7f);
					isOverwrap |= isInside(r2.x, r2.getMaxX(), r1.getCenterX(), 0.7f);
					if (isOverwrap) {
						r2.add(r1);
						rects.remove(i);
						break;
					}
				}
			}
		}
			
		public LineElement extractRuby2() {
			if (elements.size() < 5) return null;
			LineElement ruby = new LineElement("ruby");
			double avg = getAverate(elements);
			double std = getStandartDeviation(elements, avg);
			for (int i=elements.size()-1; i>=0; i--) {
				Rectangle r = elements.get(i);
				double ss = getStandardScore(elements, r.getCenterX(), avg, std);
				if (ss > 60) {
					ruby.add(r);
					elements.remove(i);
				}
			}
			if (ruby.elements.isEmpty()) return null;
			if (elements.isEmpty()) {
				elements = ruby.elements;
				return null;
			}
			
			calcBoundsRect();
			
			return ruby;
		}

		private void calcBoundsRect() {
			this.rect = null;
			for (Rectangle r : elements) {
				if (rect == null) {
					this.rect = new Rectangle(r);
				} else {
					this.rect.add(r);
				}
			}
		}

		private double getStandartDeviation(ArrayList<Rectangle> list, double avg) {
			double variance = 0;
			for (Rectangle r : list) {
				variance += Math.pow(r.getCenterX() - avg,  2);
			}
			double uv = variance / (list.size()-1);
			return Math.sqrt(uv);
		}

		private double getStandardScore(ArrayList<Rectangle> list,
				double x, double avg, double std) {
			return (50 + 10 * (x - avg) / std);
		}

		private double getAverate(ArrayList<Rectangle> list) {
			double sum = 0.0f;
			for (Rectangle r : list) {
				sum += r.getCenterX();
			}
			return sum / list.size();
		}
	}
	
	private static void getGroup(ArrayList<Rectangle> rects, ArrayList<LineElement> group) {
		group.clear();
		int i=0;
		for (Rectangle r : rects) {
			LineElement le = new LineElement(String.format("%d", i++));
			le.add(new Rectangle(r));
			group.add(le);
		}
		
		// Group y axis
		groupYAxis(group, 1.5f, true);
		
		// Group x axis
		groupXAxis(group);
		
		// Vertical line merge
		groupYAxis(group, 1.5f, false);
		groupYAxis(group, 2.0f, false);
		
		// separate ruby
		groupSeparateRuby(group);
		
		// Adjust element width
		//adjustElementWidth(group);
	}

	private static void groupSeparateRuby(ArrayList<LineElement> group) {
		ArrayList<LineElement> rubyList = new ArrayList<LineElement>();
		for (LineElement le : group) {
			LineElement ruby = le.extractRuby();
			if (ruby != null) {
				rubyList.add(ruby);
			}
		}
		
		group.addAll(rubyList);
	}

	private static double calcMedLineWidth(ArrayList<LineElement> group) {
		ArrayList<Double> widths = new ArrayList<Double>();
		for (int i=0; i<group.size(); i++) {
			Rectangle r = group.get(i).rect;
			if (r.height > r.width*3) {
				widths.add((double) r.width);
			}
		}
		Collections.sort(widths);
		double medLineWidth = widths.get(widths.size()/2);
		return medLineWidth;
	}

	private static void adjustElementWidth(ArrayList<LineElement> group) {
		for (LineElement le : group) {
			le.adjustElementWidth();
		}
	}

	private static void groupXAxis(ArrayList<LineElement> group) {
		for (int i=group.size()-1; i>=0; i--) {
			LineElement l1 = group.get(i);
			for (int j=0; j<i; j++) {
				LineElement l2 = group.get(j);
				if (overwrapX(l1.rect, l2.rect)) {
					l2.add(l1);
					group.remove(i);
					break;
				}
			}
		}
	}

	private static void groupYAxis(ArrayList<LineElement> group, double margin, boolean isChar) {
		for (int i=group.size()-1; i>=0; i--) {
			LineElement l1 = group.get(i);
			int k=0;
//			if (l1.toString().equals("33")) break;
			for (int j=i-1; j>=0; j--) {
				LineElement l2 = group.get(j);
				if (overwrapY(l1.rect, l2.rect, margin, isChar)) {
					l2.add(l1);
					group.remove(i);
					break;
				}
			}
		}
	}

	private static boolean overwrapY(Rectangle r1, Rectangle r2, double margin, boolean isChar) {
		return isChar ? overwrapYChar(r1, r2, margin) : overwrapYLine(r1, r2, margin);
	}

	private static boolean overwrapYChar(Rectangle r1, Rectangle r2, double margin) {
		// r1 is not merged yet.
		if (r1.width > r1.height || r2.width > r2.height) {
			// vertical element and horizontal element
			if (r1.width > r2.width * 8) return false;
			if (r2.width > r1.width * 8) return false;
	}
		
		boolean isOverwrap = !(r1.x > r2.getMaxX() || r1.getMaxX() < r2.x);
		if (!isOverwrap) return isOverwrap;
		
		double distance;
		if (r1.getMaxY() < r2.y) {
			distance = Math.abs(r2.y - r1.getMaxY());
		} else {
			distance = Math.abs(r1.y - r2.getMaxY());
		}

		if (r1.width*r1.height > r2.width*r2.height) {
			if (distance > Math.min(r2.height, r2.width) * margin) return false;
		} else {
			if (distance > Math.min(r1.height, r1.width) * margin) return false;
		}
		return true;
	}

	private static boolean overwrapYLine(Rectangle r1, Rectangle r2, double margin) {
//		if (r1.intersects(r2)) return true;
		if (r1.width*r1.height < r2.width*r2.height) {
			Point p1 = new Point((int)r1.getCenterX(), (int)r1.getCenterY());
			if (r2.contains(p1)) return true;
		} else {
			Point p2 = new Point((int)r2.getCenterX(), (int)r2.getCenterY());
			if (r1.contains(p2)) return true;
		}
		
		if (r1.width > r1.height || r2.width > r2.height) {
			// vertical element and horizontal element
			if (r1.width > r1.height * 2 && r1.width > r2.width) {
				if ((float)Math.abs(r1.width - r2.width) / (float)Math.max(r1.width, r2.width) > 0.2f) return false;
			} else if (r2.width > r2.height *2 && r2.width > r1.width) {
				if ((float)Math.abs(r1.width - r2.width) / (float)Math.max(r1.width, r2.width) > 0.2f) return false;
			}
		}
		
		boolean isOverwrap = !(r1.x > r2.getMaxX() || r1.getMaxX() < r2.x);
		if (!isOverwrap) return isOverwrap;
		
		double distance;
		if (r1.getMaxY() < r2.y) {
			distance = Math.abs(r2.y - r1.getMaxY());
		} else {
			distance = Math.abs(r1.y - r2.getMaxY());
		}

//			if (r1.width > r1.height*3) return false;
//			if (r2.width > r2.height*3) return false;
		if ((float)Math.abs(r1.width - r2.width) / (float)Math.max(r1.width, r2.width) < 0.2f) {
			// not horizontal line.
			if (r1.height > r1.width*3 && r2.height > r2.width*3) { 
				// both rects are vertical line.
				if (distance > Math.min(r2.height, r2.width) * 1.5f) {
					if (distance > Math.min(r1.height, r1.width) * 1.5f) return false;
				}
				return true;
			} else {
				int limit1 = Math.min(r1.height, r1.width);
				int limit2 = Math.min(r2.height, r2.width);
				int limit = Math.max(limit1, limit2);
				if (distance > limit * 1.3f) return false;
			}
		} else {
			if (r1.height > r1.width*3 && r2.height > r2.width*3) { // both rects are vertical line.
				int limit1 = Math.min(r1.height, r1.width);
				int limit2 = Math.min(r2.height, r2.width);
				int limit = Math.max(limit1, limit2);
				if (distance > limit * margin) return false;
			} else {
				if (distance > Math.min(r2.height, r2.width) * margin) return false;
				if (distance > Math.min(r1.height, r1.width) * margin) return false;
			}
		}

		return true;
	}


	private static boolean overwrapX(Rectangle r1, Rectangle r2) {
		if (r1.intersects(r2)) return true;
		
		if (r1.height > r1.width*3 || r2.height > r2.width*3) return false;

		boolean isOverwrap = !(r1.y > r2.getMaxY() || r1.getMaxY() < r2.y);
		if (!isOverwrap) return isOverwrap;
		
		double distance;
		if (r1.getMaxX() < r2.x) {
			distance = r2.x - r1.getMaxX();
		} else {
			distance = r1.x - r2.getMaxX();
		}
		
		if (r1.width*r1.height > r2.width*r2.height) {
			if (distance > Math.min(r2.width, r2.height)*2) return false;
		} else {
			if (distance > Math.min(r1.width, r1.height)*2) return false;
		}
		return true;
	}

	private static void mergeRects(ArrayList<Rectangle> rects) {
		for (int i=rects.size()-1; i >= 0 ; i--) {
			Rectangle r1 = rects.get(i);
			boolean isRemoved = false;
			for (int j=0; j<i; j++) {
				Rectangle r2 = rects.get(j);
				if (r1.intersects(r2)) {
					r2.add(r1);
					if (!isRemoved) {
						rects.remove(i);
						isRemoved = true;
					}
					r1 = r2;
				}
			}
		}
	}

	public static class RectComparator implements Comparator<Rectangle> {
		@Override
		public int compare(Rectangle cr1, Rectangle cr2) {
			int l1 = cr1.x;
			int l2 = cr2.x;
			int r1 = cr1.x + cr1.width;
			int r2 = cr2.x + cr2.width;
			if (l1 > r2 || r1 < l2) {
				return r2 - r1;
			}
			int t1 = cr1.y;
			int t2 = cr2.y;
			return t1 - t2;
		}
	}

	private static void getRects(CvSeq contours, ArrayList<Rectangle> rects) {
		while (contours != null && !contours.isNull()) {
            if (contours.elem_size() > 0) {
            	CvRect cr = cvBoundingRect(contours, 1);
            	Rectangle r = new Rectangle(cr.x(), cr.y(), cr.width(), cr.height());
            	rects.add(r);
            }
            contours = contours.h_next();
		}
	}
	

	private static void drawGroup(IplImage image,
			ArrayList<LineElement> group, int lineWidth, CvScalar rectColor,  CvScalar elemColor) {
		CvFont font = new CvFont(CV_FONT_BLACK, 0.3, 1);
		IplImage tmpImage = cvCreateImage(image.cvSize(), IPL_DEPTH_8U, 3);
    	cvNot(image, image);
		for (LineElement le : group) {
			Rectangle r = le.rect;
			cvSet(tmpImage, new CvScalar(255,255,255,0));
        	cvRectangle(tmpImage, cvPoint(r.x, r.y), 
    			    cvPoint(r.x + r.width, r.y + r.height),
    			    rectColor, lineWidth, 0, 0);
        	String text = le.toString();
        	cvPutText(tmpImage, text, cvPoint(r.x + r.width, r.y - 10), font, rectColor);
        	
        	cvNot(tmpImage, tmpImage);
        	cvAddWeighted(image, 1.0f, tmpImage, 1.0f, 0.0, image);
        	if (elemColor != null) {
        		drawRects(image, le.elements, 1, elemColor, null);
        	}
		}
    	cvNot(image, image);
    	cvReleaseImage(tmpImage);
    	
		for (LineElement le : group) {
        	if (elemColor != null) {
        		drawRects(image, le.elements, 1, elemColor, null);
        	}
		}
	}
	
	private static void drawRects(IplImage image,
			ArrayList<Rectangle> rects, int lineWidth, CvScalar rectColor, CvScalar textColor) {
		CvFont font = new CvFont(CV_FONT_HERSHEY_SCRIPT_SIMPLEX, 0.3, 1);
		int i = 0;
		for (Rectangle r : rects) {
        	cvRectangle(image, cvPoint(r.x, r.y), 
    			    cvPoint(r.x + r.width, r.y + r.height),
    			    rectColor, lineWidth, 0, 0);
        	if (textColor != null) {
	        	String text = Integer.toString(i++);
	        	cvPutText(image, text, cvPoint(r.x, r.y), font, textColor);
        	}
		}
	}
	
	private static void drawContours(IplImage image_out, CvSeq contours, CvMemStorage storage, CvScalar color) {
		CvFont font = new CvFont(CV_FONT_HERSHEY_SCRIPT_SIMPLEX, 0.3, 1);
		int i = 0;
		while (contours != null && !contours.isNull()) {
            if (contours.elem_size() > 0) {
/*
                CvSeq points = cvApproxPoly(contours, Loader.sizeof(CvContour.class),
                        storage, CV_POLY_APPROX_DP, cvContourPerimeter(contours)*0.02, 0);
                cvDrawContours(image_out, points, color, color, -1, 1, CV_AA);
*/                
            	CvRect cr = cvBoundingRect(contours, 1);
            	cvRectangle(image_out, cvPoint(cr.x(), cr.y()), 
            			    cvPoint(cr.x() + cr.width(), cr.y() + cr.height()),
            			    CvScalar.GREEN, 1, 0, 0);
            	String text = Integer.toString(i++);
            	cvPutText(image_out, text, cvPoint(cr.x(), cr.y()), font, CvScalar.CYAN);
            }
            contours = contours.h_next();
        }		
	}
}