import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
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
import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

import com.googlecode.javacpp.FloatPointer;
import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
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
		double colorH = std_dev.val(0);
		double colorS = std_dev.val(1);
		double colorV = std_dev.val(2);
		boolean isColorImage = colorS > 10.0f;
		boolean containsIllust = colorV > 40.0f;
		boolean isComplicatedIllust = colorV > 85.0f;
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
	
	private static CvRect getMinAreaRect(CvSeq contours) {
		CvMemStorage storage = cvCreateMemStorage(0);
		CvSeq points = cvCreateSeq(CV_SEQ_ELTYPE_POINT, Loader.sizeof(CvSeq.class), Loader.sizeof(CvPoint.class), storage);
		while (contours != null && !contours.isNull()) {
            if (contours.elem_size() > 0) {
            	CvRect cr = cvBoundingRect(contours, 1);
            	cvSeqPush(points, new CvPoint(cr.x(), cr.y()));
            	cvSeqPush(points, new CvPoint(cr.x(), cr.y()+cr.height()));
            	cvSeqPush(points, new CvPoint(cr.x()+cr.width(), cr.y()));
            	cvSeqPush(points, new CvPoint(cr.x()+cr.width(), cr.y()+cr.height()));
            }
            contours = contours.h_next();
		}
		CvRect rect = cvBoundingRect(points, 0);
		cvClearSeq(points);
		cvReleaseMemStorage(storage);
		return rect;
	}
	
	public static void deskew(IplImage image, double angle, CvRect rect) {
/*
		cvDrawRect(image, new CvPoint(rect.x(), rect.y()), new CvPoint(rect.x()+rect.width(), rect.y()+rect.height()), 
				   CvScalar.RED, 1, 0, 0);
*/
		if (angle == 0.0f) return;
		
		CvMat rotMat = cvCreateMat(2, 3, CV_32FC1);
		CvPoint2D32f center = new CvPoint2D32f(rect.x()+rect.width()/2, rect.y()+rect.height()/2);
		double degree = Math.toDegrees(angle);
		cv2DRotationMatrix(center, degree, 1, rotMat);
		cvWarpAffine(image, image, rotMat, CV_INTER_LINEAR + CV_WARP_FILL_OUTLIERS, cvScalarAll(255));
	}
	
	private static double calcSkew(ArrayList<LayoutElement> group, IplImage image) {
		double angle = 0;
		DoubleArrayList angles = new DoubleArrayList();
		for (LayoutElement le : group) {
			if (le.rect.height < le.rect.width*3) continue;
			if (le.elements.size() < 20) continue;
			double theta = le.calcAngle(image);
			if (theta < -Math.PI/2) theta = Math.PI + theta;
			angles.add(theta);
		}
		if (angles.size()>0) {
			angles.sort();
			int percent = 5;
			int n = angles.size();
			int k = (int)Math.round(n * ( percent / 100.0f) / 2.0f);
			if (n > 10 && k == 0) k = 1;
			double mean = Descriptive.mean(angles);
			angle = Descriptive.trimmedMean(angles, mean, k, k);
		}
		
		return angle;
	}

	
	public static double calcSkew(IplImage image) {
		IplImage image_src = cvCloneImage(image);

//		cvSaveImage("test_src_in.png", image_src);
		IplImage image_test = cvCloneImage(image_src);
		cvNot(image_test, image_test);
		double length = image_src.height() / 3.f;

//		cvNot(image_src, image_src);
		cvCanny(image_src, image_src, 50, 200, 3);
//		cvSaveImage("test_src_canny.png", image_src);

		CvMemStorage storage = CvMemStorage.create();
		double angle = 0;
		int num_lines = 0;
		/*
		CvSeq lines = cvHoughLines2(image_src, storage, CV_HOUGH_PROBABILISTIC, 1, Math.PI/180, 40, length, 20);

		for (int i=0; i<lines.total(); i++) {
			Pointer point = cvGetSeqElem(lines, i);
			CvPoint p1 = new CvPoint(point).position(0);
			CvPoint p2 = new CvPoint(point).position(1);
			cvLine(image_test, p1, p2, CvScalar.BLACK, 3, 8, 0);
			angle += Math.atan2((double)(p2.y() - p1.y()), (double)(p2.x() - p1.x()));
		}
		*/
		CvSeq lines = cvHoughLines2(image_src, storage, CV_HOUGH_STANDARD, 1, Math.PI/180, 50, 0, 0);
		for (int i=0; i < Math.min(lines.total(),  100); i++) {
			FloatPointer cvline = new FloatPointer(cvGetSeqElem(lines, i));
			double rho = cvline.position(0).get();
			double theta = cvline.position(1).get();
			double a = Math.cos(theta);
			double b = Math.sin(theta);
			double x0 = a * rho;
			double y0 = b * rho;
			CvPoint p1 = new CvPoint((int)Math.round(x0 + 1000*(-b)), (int)Math.round(y0 + 1000*a));
			CvPoint p2 = new CvPoint((int)Math.round(x0 - 1000*(-b)), (int)Math.round(y0 - 1000*a));
			cvLine(image_test, p1, p2, CvScalar.BLACK, 3, 8, 0);

			angle += theta;
			num_lines++;
		}
		angle /= num_lines;

		storage.release();
		
		cvSaveImage("test_line.png", image_test);
		cvReleaseImage(image_src);
		cvReleaseImage(image_test);
		
		return angle;
	}

	public static File layoutAnalysis(File file) {
		IplImage image_source = cvLoadImage(file.getPath());
		ArrayList<LayoutElement> group = new ArrayList<LayoutElement>();

		if (getLineGroup(image_source, group, false, false)) {
/*
			IplImage tmp = cvCloneImage(image_source);
			double angle = calcSkew(group, tmp);
			cvSaveImage("test_line.png", tmp);
			cvReleaseImage(tmp);
*/
			double angle = calcSkew(group, null);
			deskew(image_source, angle, new CvRect(0, 0, image_source.width(), image_source.height()));
			
			getLineGroup(image_source, group, true, true);
			labeling(image_source, group);
			
			drawGroup(image_source, group, 2, CvScalar.MAGENTA, CvScalar.CYAN);
			
//			cvSaveImage("test_after.png", image_source);
		}
		
		cvSaveImage(file.getPath(), image_source);

		cvReleaseImage(image_source);
		return file;
	}



	private static void labeling(IplImage image_source,
			ArrayList<LayoutElement> elements) {
		// Check horizontal element
		checkTextHorizontal(elements);
		
		// Check vertical element
		checkTextVertical(elements);
		
		// Detect column
		Rectangle textColumn = null;
		textColumn = getTextColumn(elements, textColumn);
		
		// Check elements in column
		checkTextInColumn(elements, textColumn);

		// Check ruby element
		double rubyThreshold = checkRubyElement(elements);

		// Check ruby element (elements which is next to vertical element)
		validateRuby(elements);

		// Check multi columns
		ArrayList<Rectangle> columnList = new ArrayList<Rectangle>();
		checkMultiColumns(elements, columnList, rubyThreshold);
		
		// Align vertical elements
		alignVerticalTextInColumn(elements, columnList);
			
		// checkCharacter
		checkCharacter(elements);
	}

	private static void checkCharacter(ArrayList<LayoutElement> elements) {
		DoubleArrayList hlist = new DoubleArrayList();
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			for (Rectangle r : le.elements) {
				hlist.add(r.height);
				r.x = le.rect.x;
				r.width = le.rect.width;
			}
		}
		if (hlist.size()==0) return;
		
		double[] avglist = new double[2];
		double threshold = calcThreshold(hlist, 1, avglist);
		double avgHeight = Math.max(avglist[0], avglist[1]);

		int charHeight = 0;
		int limit = Math.max(2,(int)(hlist.size() * 0.1f));
		if (limit < hlist.size()) {
			hlist.sort();
			charHeight = (int)hlist.get(hlist.size()-limit);
		} else {
			charHeight = (int)hlist.get(hlist.size()-1);
		}

		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			if (le.elements.size()==0) continue;
			Collections.sort(le.elements, new RectComparator());
			LinkedList<Rectangle> elems = new LinkedList<Rectangle>(le.elements);
			Iterator<Rectangle> itr = elems.iterator();
			Rectangle r0 = null;
			Rectangle r1 = itr.next();
			Rectangle r2 = null;
			if (r1.y < le.rect.y + avgHeight) {
				r1.add(le.rect.getLocation());
			} else {
				// insert space character
				int height = Math.min(charHeight, r1.y - le.rect.y);
				elems.addFirst(new Rectangle(le.rect.x, le.rect.y, le.rect.width, height));
				itr = elems.iterator();
				r1 = itr.next();
			}
			
			while (itr.hasNext()) {
				r2 = itr.next();

				boolean lastChar = false;
				boolean merged = false;
				do {
					merged = false;
					if (r1.height < charHeight) {
						if (r2.y - r1.getMaxY() > charHeight/2) break; // Too far to merge
						if (r2.getMaxY() - r1.y <= charHeight+1) {
							merged = true;
							r1.add(r2);
							itr.remove();
							if (!itr.hasNext()) {
								lastChar = true;
								break;
							}
							r2 = itr.next();
						}
					}
				} while (merged);
				if (lastChar) break;
	
				if (r1.height < charHeight) {
					int marginTop = 0, marginBottom = 0;
					if (r0 != null && r1.y - r0.getMaxY() > 0) {
						marginTop = Math.max(0, (int)r0.getMaxY() - r1.y);
					}
					marginBottom = Math.max(0, r2.y - (int)r1.getMaxY());
					int maxMarginBottom = marginBottom;
					int diff = charHeight - r1.height;
					marginTop -= diff/2;
					marginBottom -= (diff - diff/2);
					if (marginTop < 0) {
						marginBottom += marginTop;
						marginTop = 0;
					}
					if (marginBottom < 0) {
						marginBottom = 0;
					}
					
					marginBottom = Math.min(marginBottom, maxMarginBottom);

					if (r0 != null) r1.add(new Point(r1.x, (int)r0.getMaxY() + marginTop));
					r1.add(new Point(r1.x, r2.y - marginBottom));
				}

				r0 = r1;
				r1 = r2;
			}
			
			// Align the last character in the line.
			if (r1 != null) {
				if (r0 != null && r1.y - r0.getMaxY() > 0) {
					r1.add(new Point(r1.x, (int)r0.getMaxY()));
				}
				if (r1.height < charHeight) {
					r1.height += charHeight - r1.height;
					le.rect.add(r1);
				}
			}

			le.elements.clear();
			le.elements.addAll(elems);
		}		
	}

	private static void checkMultiColumns(ArrayList<LayoutElement> elements, 
			ArrayList<Rectangle> columnList, double rubyThreshold) {
		DoubleArrayList ylist = new DoubleArrayList();
		columnList.clear();
		Rectangle singleColumn = null;

		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			ylist.add(le.rect.y);
			if (singleColumn == null) {
				singleColumn = new Rectangle(le.rect);
			} else {
				singleColumn.add(le.rect);
			}
		}
		
		double threshold = calcThreshold(ylist, 1, null);
		if (threshold == 0) {
			columnList.add(singleColumn);
			return;
		}

		Rectangle topColumn = null, bottomColumn = null;
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			if (le.rect.width < rubyThreshold) continue;
			if (le.rect.y < threshold) {
				if (topColumn == null) {
					topColumn = new Rectangle(le.rect);
				} else {
					topColumn.add(le.rect);
				}

			} else {
				if (bottomColumn == null) {
					bottomColumn = new Rectangle(le.rect);
				} else {
					bottomColumn.add(le.rect);
				}
			}
		}
		
		if (topColumn == null || bottomColumn == null ||
			topColumn.isEmpty() || bottomColumn.isEmpty() || topColumn.intersects(bottomColumn)) {
			columnList.add(singleColumn);
		} else {
			columnList.add(topColumn);
			columnList.add(bottomColumn);
		}
	}

	private static void alignVerticalTextInColumn(
			ArrayList<LayoutElement> elements, ArrayList<Rectangle> columnList) {
		for (Rectangle column : columnList) {
			for (LayoutElement le : elements) {
				if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
				if (column.intersects(le.rect)) {
					int diff = le.rect.y - column.y;
					if (diff < le.rect.width*3) {
						le.rect.y = column.y;
						le.rect.height += diff;
					}
				}
			}
		}
	}

	private static void validateRuby(ArrayList<LayoutElement> elements) {
		for (LayoutElement le : elements) {
			if (le.getType() == LayoutElement.TYPE_UNKNOWN) {
				for (LayoutElement vert : elements) {
					if (vert.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
					Rectangle body = new Rectangle(vert.rect);
					body.width *= 1.2f;
					if (body.intersects(le.rect)) {
						le.setType(LayoutElement.TYPE_RUBY);
						break;
					}
				}
			} else if (le.getType() == LayoutElement.TYPE_RUBY) {
				boolean found = false;
				for (LayoutElement vert : elements) {
					if (vert.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
					Rectangle body = new Rectangle(vert.rect);
					body.width *= 1.2f;
					if (body.intersects(le.rect)) {
						le.setType(LayoutElement.TYPE_RUBY);
						found = true;
						break;
					}
				}
				if (!found) {
					le.setType(LayoutElement.TYPE_TEXT_VERTICAL);
				}
			}
		}
	}

	private static void checkTextInColumn(ArrayList<LayoutElement> elements, Rectangle textColumn) {
		if (textColumn == null) return;
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_UNKNOWN) continue;
			if (textColumn.intersects(le.rect)) {
				le.setType(LayoutElement.TYPE_TEXT_VERTICAL);
			}
		}
	}

	private static Rectangle getTextColumn(ArrayList<LayoutElement> elements,
			Rectangle textColumn) {
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			if (textColumn == null) {
				textColumn = new Rectangle(le.rect);
			} else {
				textColumn.add(le.rect);
			}
		}
		return textColumn;
	}

	private static double checkRubyElement(ArrayList<LayoutElement> elements) {
		DoubleArrayList widths = new DoubleArrayList();
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL &&
				le.getType() != LayoutElement.TYPE_RUBY) continue;
			widths.add(le.rect.width);
		}
		
		double rubyThreshold = calcThreshold(widths, 1, null);
		if (rubyThreshold == 0) return rubyThreshold;
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			if (le.rect.width < rubyThreshold) {
				le.setType(LayoutElement.TYPE_RUBY);
			}
		}
		return rubyThreshold;
	}

	private static void checkTextVertical(
			ArrayList<LayoutElement> elements) {
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_UNKNOWN) continue;
			if (le.rect.height > le.rect.width*3) {
				le.setType(LayoutElement.TYPE_TEXT_VERTICAL);
			}
		}
	}

	private static void checkTextHorizontal(ArrayList<LayoutElement> elements) {
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_UNKNOWN) continue;
			if (le.rect.width > le.rect.height*3) {
				le.setType(LayoutElement.TYPE_TEXT_HORIZONTAL);
			}
		}
	}

	private static double calcThreshold(DoubleArrayList lengthList, int k, double[] avglist) {
		if (lengthList.size()<4) return 0; // Too few data to analyze
		double threshold = 0;
		double maxIndexBegin = 0;
		double avg = Descriptive.mean(lengthList);
		double max = -1.0f;
		double ravg = 0;
		lengthList.sort();

		final int limit = (int)avg * 2;
		int[] hist = new int[limit+1];

		lengthList.trimToSize();
		for (double w : lengthList.elements()) {
			int j = (int)w;
			if (j > limit) {
				j = limit;
			}
			hist[j]++;
		}

		for (int n=0; n<k; n++) {
			if (n>0) {
				Arrays.fill(hist, 0, (int) threshold, 0);
				avg = 0;
				int num = 0;
				for (int j=0; j<limit; j++) {
					avg += hist[j] * j;
					num += hist[j];
				}
				if (num>0) {
					avg /= num;
				}
				threshold = 0;
				ravg = 0;
				max = 0;
			}
			boolean isRepeat = false;
			for (int i = 0; i < limit; i++) {
				double n1 = 0;
				double wn1 = 0;
				double avg1 = 0;
				for (int j=0; j<i; j++) {
					n1 += hist[j];
					wn1 += hist[j] * j;
				}
				if (n1 != 0) {
					avg1 = wn1 / n1;
				}
				
				double var1 = 0;
				for (int j=0; j<i; j++) {
					var1 += (j-avg1) * (j-avg1) * hist[j];
				}
				if (n1 != 0) {
					var1 /= n1;
				}
				
				double n2 = 0;
				double wn2 = 0;
				double avg2 = 0;
				for (int j=i; j<limit; j++) {
					n2 += hist[j];
					wn2 += hist[j] * j;
				}
				if (n2 != 0) {
					avg2 = wn2 / n2;
				}
				
				double var2 = 0;
				for (int j=i; j<limit; j++) {
					var2 += (j-avg2) * (j-avg2) * hist[j];
				}
				if (n2 != 0) {
					var2 /= n2;
				}
	
				double w = (n1 * var1 + n2 * var2);
				double b = n1 * (avg1-avg)*(avg1-avg) + n2 * (avg2-avg)*(avg2-avg);
				double r = b/w;
				if (r > max) {
					max = r;
					isRepeat = true;
					maxIndexBegin = i;
					threshold = i;
					ravg = avg1 / avg2;
					if (avglist != null) {
						avglist[0] = avg1;
						avglist[1] = avg2;
					}
				} else if (r == max && isRepeat) {
					threshold = (i + maxIndexBegin) / 2;
				} else {
					isRepeat = false;
				}
			}
			
			if (Math.abs(ravg) > 0.7f) {
				// Maybe single-humped distribution
				return 0;
			}
		}
		
		return threshold;
	}

	private static boolean getLineGroup(IplImage image_source,
			ArrayList<LayoutElement> group, boolean doAdjust, boolean drawResult) {
		double scale = 1;
		CvSize size_target = new CvSize((int)(image_source.width()*scale), (int)(image_source.height()*scale));
		IplImage image_binary = cvCreateImage( size_target, IPL_DEPTH_8U, 1);
//		cvSaveImage("test_src.png", image_source);
		
		binalize(image_source, image_binary, true);
//		cvSaveImage("test_binary.png", image_binary);
		// Extract counters
		CvMemStorage storage = CvMemStorage.create();
		CvSeq contours = new CvContour();
		int count = cvFindContours(image_binary, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
		if (count > 10000) return false; // maybe image.
//		drawContours(image_source, contours, storage, CvScalar.RED);
		
		List<Rectangle> rects = new ArrayList<Rectangle>();
		getRects(contours, rects);

		// Union intersected rects
		mergeRects(rects);
		
		// Remove small regions
		removeSmallRects(rects);

		// Draw rects
		Collections.sort(rects, new RectComparator());
		if (drawResult) {
//			drawRects(image_source, rects, 2, CvScalar.CYAN, CvScalar.GREEN);
		}
		
		// Grouping
		getLineElements(rects, group, image_source, doAdjust);
		
		storage.release();
		cvReleaseImage(image_binary);
		return true;
	}




	private static void removeSmallRects(List<Rectangle> rects) {
		for (int i=rects.size()-1; i >= 0 ; i--) {
			Rectangle r1 = rects.get(i);
			if (r1.width * r1.height <= 9) {
				rects.remove(i);
			}
		}		
	}


	private static class LayoutElement {
		public static final int TYPE_UNKNOWN = 0;
		public static final int TYPE_IMAGE = 1;
		public static final int TYPE_TEXT_VERTICAL = 2;
		public static final int TYPE_TEXT_HORIZONTAL = 3;
		public static final int TYPE_RUBY = 4;

		int type = TYPE_UNKNOWN;
		int id = 0;
		Rectangle rect;
		List<Rectangle> elements = new ArrayList<Rectangle>();

		public LayoutElement(int id) {
			this.id = id;;
		}
		
		public LayoutElement(int id, int type) {
			this.id = id;;
			this.type = type;
		}

		public double calcAngle(IplImage image) {
			CvMemStorage storage = cvCreateMemStorage(0);
			CvSeq points = cvCreateSeq(CV_SEQ_ELTYPE_POINT, Loader.sizeof(CvSeq.class), Loader.sizeof(CvPoint.class), storage);
			for (Rectangle r : elements) {
            	cvSeqPush(points, new CvPoint((int)r.getCenterX(), (int)r.getCenterY()));
			}
			
			float[] line = new float[4];
			cvFitLine(points, CV_DIST_L2, 0, 0.01f, 0.01f, line);
			double angle = -Math.atan2(line[0], line[1]);
//			double degree = Math.toDegrees(angle);
			if (image != null) {
				double a = Math.cos(angle);
				double b = Math.sin(angle);
				double x0 = line[2];
				double y0 = line[3];
				double length = rect.height;
				CvPoint p1 = new CvPoint((int)Math.round(x0 + length*(-b)), (int)Math.round(y0 + length*a));
				CvPoint p2 = new CvPoint((int)Math.round(x0 - length*(-b)), (int)Math.round(y0 - length*a));
				cvLine(image, p1, p2, CvScalar.BLACK, 3, CV_AA, 0);
			}

			cvClearSeq(points);
			cvReleaseMemStorage(storage);
			return angle;
		}

		@Override
		public String toString() {
			final String[] types = new String[] {
				"U", "I", "V", "H", "R"	
			};
			return types[type] + Integer.toString(this.id);
		}
		
		public int getId() {
			return id;
		}
		
		public void add(LayoutElement l1) {
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

		public LayoutElement extractRuby() {
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
			LayoutElement ruby = new LayoutElement(0);
			ruby.setType(LayoutElement.TYPE_RUBY);
			
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
			
		public LayoutElement extractRuby2() {
			if (elements.size() < 5) return null;
			LayoutElement ruby = new LayoutElement(0, LayoutElement.TYPE_RUBY);
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

		private double getStandartDeviation(List<Rectangle> list, double avg) {
			double variance = 0;
			for (Rectangle r : list) {
				variance += Math.pow(r.getCenterX() - avg,  2);
			}
			double uv = variance / (list.size()-1);
			return Math.sqrt(uv);
		}

		private double getStandardScore(List<Rectangle> list,
				double x, double avg, double std) {
			return (50 + 10 * (x - avg) / std);
		}

		private double getAverate(List<Rectangle> list) {
			double sum = 0.0f;
			for (Rectangle r : list) {
				sum += r.getCenterX();
			}
			return sum / list.size();
		}

		public void setType(int type) {
			this.type = type;
		}
		
		public int getType() {
			return type;
		}
		
		public CvScalar getColor() {
			final CvScalar[] colorTable = new CvScalar[] {
				CvScalar.RED, CvScalar.BLUE, CvScalar.MAGENTA, CvScalar.CYAN, CvScalar.GREEN
			};
			return colorTable[type];
		}
	}
	
	private static void getLineElements(List<Rectangle> rects, List<LayoutElement> group, IplImage image, boolean doAdjust) {
		group.clear();
		int i=0;
		for (Rectangle r : rects) {
			LayoutElement le = new LayoutElement(i++);
			le.add(new Rectangle(r));
			group.add(le);
		}

		// Check images
		checkImageElement(group, image);
		
		// Group y axis
		groupYAxis(group, 1.5f, true);
		
		
		// Vertical line merge
		if (!doAdjust) {
			groupYAxis(group, 1.5f, false);
			
			// Group x axis
			groupXAxis(group);
			
			groupYAxis(group, 2.0f, false);
		
			// separate ruby
			groupSeparateRuby(group);
		} else {
			groupYAxis(group, 1.5f, false);
			
			// Group x axis
			groupXAxis(group);
			
			groupYAxis(group, 2.0f, false);
		}
		
		// Adjust element width
		if (doAdjust) {
			adjustElementWidth(group);
		}
	}

	private static void checkImageElement(List<LayoutElement> group,
			IplImage image) {
		CvSize size = image.cvSize();
		for (LayoutElement le : group) {
			if (le.rect.width < size.width() * 0.1f) continue;
			if (le.rect.height < size.height() * 0.1f) continue;
			
			double leArea = le.rect.width * le.rect.height;
			double area = image.width() * image.height();
			int w = image.width();
			int h = image.height();
			if (Math.sqrt(leArea/area) > 0.10f) {
				le.setType(LayoutElement.TYPE_IMAGE);
			} else {
				CvRect leRect = new CvRect(le.rect.x, le.rect.y, le.rect.width, le.rect.height);
				CvSize leSize = new CvSize(le.rect.width, le.rect.height);
				// Color detection
				IplImage image_hsv = cvCreateImage( leSize, IPL_DEPTH_8U, 3);
				cvSetImageROI(image, leRect);
				cvCvtColor(image, image_hsv, CV_RGB2HSV);
				CvScalar mean = cvScalarAll(0);
				CvScalar std_dev= cvScalarAll(0);
	
				cvAvgSdv(image_hsv, mean, std_dev, null);
				double colorH = std_dev.val(0);
				double colorS = std_dev.val(1);
				double colorV = std_dev.val(2);

				if (colorV > 40.0f) {
					// iilust
					colorS = colorV;
				}
				// TODO:
			}
		}
		cvResetImageROI(image);
		
	}

	private static void groupSeparateRuby(List<LayoutElement> group) {
		List<LayoutElement> rubyList = new ArrayList<LayoutElement>();
		for (LayoutElement le : group) {
			LayoutElement ruby = le.extractRuby();
			if (ruby != null) {
				rubyList.add(ruby);
			}
		}
		
		group.addAll(rubyList);
	}

	private static double calcMedLineWidth(List<LayoutElement> group) {
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

	private static void adjustElementWidth(List<LayoutElement> group) {
		for (LayoutElement le : group) {
			le.adjustElementWidth();
		}
	}

	private static void groupXAxis(List<LayoutElement> group) {
		for (int i=group.size()-1; i>=0; i--) {
			LayoutElement l1 = group.get(i);
			if (l1.getType() == LayoutElement.TYPE_IMAGE) continue;
			for (int j=0; j<i; j++) {
				LayoutElement l2 = group.get(j);
				if (l2.getType() == LayoutElement.TYPE_IMAGE) continue;
				if (overwrapX(l1.rect, l2.rect)) {
					l2.add(l1);
					group.remove(i);
					break;
				}
			}
		}
	}

	private static void groupYAxis(List<LayoutElement> group, double margin, boolean isChar) {
		for (int i=group.size()-1; i>=0; i--) {
			LayoutElement l1 = group.get(i);
			if (l1.getType() == LayoutElement.TYPE_IMAGE) continue;
//			if (l1.toString().equals("33")) break;
			for (int j=i-1; j>=0; j--) {
				LayoutElement l2 = group.get(j);
				if (l2.getType() == LayoutElement.TYPE_IMAGE) continue;
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
			// ruby check
			if (r1.width < r2.width && r2.getMaxX() < r1.getCenterX()) return false;
			if (r1.width > r2.width && r1.getMaxX() < r2.getCenterX()) return false;
			
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

	private static void mergeRects(List<Rectangle> rects) {
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

	private static void getRects(CvSeq contours, List<Rectangle> rects) {
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
			ArrayList<LayoutElement> group, int lineWidth, CvScalar rectColor,  CvScalar elemColor) {
		CvFont font = new CvFont(CV_FONT_NORMAL, 0.3, 1);
		IplImage tmpImage = cvCreateImage(image.cvSize(), IPL_DEPTH_8U, 3);
    	cvNot(image, image);
		for (LayoutElement le : group) {
			Rectangle r = le.rect;
			cvSet(tmpImage, new CvScalar(255,255,255,0));
        	rectColor = le.getColor();

        	cvRectangle(tmpImage, cvPoint(r.x, r.y), 
    			    cvPoint(r.x + r.width, r.y + r.height),
    			    rectColor, lineWidth, 0, 0);
        	String text = le.toString();
        	cvPutText(tmpImage, text, cvPoint(r.x, r.y - 4), font, rectColor);
        	
        	cvNot(tmpImage, tmpImage);
        	cvAddWeighted(image, 1.0f, tmpImage, 1.0f, 0.0, image);
        	if (elemColor != null) {
        		drawRects(image, le.elements, 1, elemColor, null);
        	}
		}
    	cvNot(image, image);
    	cvReleaseImage(tmpImage);
    	
		for (LayoutElement le : group) {
        	if (elemColor != null) {
        		drawRects(image, le.elements, 1, elemColor, null);
        	}
		}
	}
	
	private static void drawRects(IplImage image,
			List<Rectangle> rects, int lineWidth, CvScalar rectColor, CvScalar textColor) {
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
            	
/*            	
            	CvRect cr = cvBoundingRect(contours, 1);
            	cvRectangle(image_out, cvPoint(cr.x(), cr.y()), 
            			    cvPoint(cr.x() + cr.width(), cr.y() + cr.height()),
            			    CvScalar.GREEN, 1, 0, 0);
            	String text = Integer.toString(i++);
            	cvPutText(image_out, text, cvPoint(cr.x(), cr.y()), font, CvScalar.CYAN);
*/
            }
            contours = contours.h_next();
        }		
	}
}