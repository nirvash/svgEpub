package com.github.nirvash.svgEpub.util;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Iterator;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.nirvash.svgEpub.layout.LayoutElement;
import com.github.nirvash.svgEpub.list.IFile;
import com.github.nirvash.svgEpub.list.ListItem;
import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_objdetect;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

public class ImageUtility {
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
		String path = PathUtil.getTmpDirectory();
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
			PathUtil.copyFile(item.getInputStream(), tmpFile);
			image_source = cvLoadImage(tmpFile.getPath());
			if (image_source == null) {
				// workaround: Retry file copy (Sometimes it fails to decode rar archive?)
				tmpFile.delete();
				PathUtil.copyFile(item.getInputStream(), tmpFile);
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
			PathUtil.copyFile(tmpOutFile, outFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		
		return outFile;
	}
	
	public static boolean binalize(IplImage image_source, IplImage image_target, boolean isNot) {

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
		
		cvResize(image_grey, image_target, CV_INTER_LINEAR);
		cvNot(image_target, image_target);
		cvAddWeighted(image_target, 1.0f, image_target, 0.3f, 0.0f, image_target);
		cvNot(image_target, image_target);

		try {
			if (!isColorImage) {
				if (!containsIllust) {
					/*
					int blockSize = 31;
					int inv = isNot ? CV_THRESH_BINARY_INV : CV_THRESH_BINARY;
					cvAdaptiveThreshold( image_target , image_target, 255,
							 CV_ADAPTIVE_THRESH_MEAN_C ,
							 inv, blockSize, 5 );
					*/
					int blockSize = 23;
					cvAdaptiveThreshold( image_target , image_edge, 255,
							 CV_ADAPTIVE_THRESH_MEAN_C,
							 CV_THRESH_BINARY_INV, blockSize, 5 );
					
					IplImage image_beta = cvCreateImage( size_target, IPL_DEPTH_8U, 1 );

					cvThreshold(image_target, image_beta, 0, 255,	CV_THRESH_BINARY_INV | CV_THRESH_OTSU);
					cvOr(image_edge, image_beta, image_target, null);
					
					if (!isNot) {
						cvNot(image_target, image_target);
					}
					cvReleaseImage(image_beta);
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
	
	public static Document createSvgDocument2(Rectangle clipRect, Rectangle imageRect, String imageURI) {
		DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
		Document doc = impl.createDocument(svgNS, "svg", null);

		Element svgRootOuter = doc.getDocumentElement();

		svgRootOuter.setAttribute("id", "root");
		svgRootOuter.setAttributeNS(null , "width", Integer.toString(clipRect.width));
		svgRootOuter.setAttributeNS(null , "height", Integer.toString(clipRect.height));
		svgRootOuter.setAttributeNS(null, "viewBox", 
				String.format("%d %d %d %d",  0, 0, clipRect.width, clipRect.height));
		svgRootOuter.setAttributeNS(null, "preserveAspectRatio", "xMidYMid meet");
		
		Element image = doc.createElementNS(svgNS, "image");
		image.setAttributeNS(null, "width", Integer.toString(imageRect.width));
		image.setAttributeNS(null, "height", Integer.toString(imageRect.height));
		XLinkSupport.setXLinkHref(image,  imageURI);

		Element svgRootInner = (Element) doc.createElementNS(svgNS, "svg");
		svgRootInner.setAttribute("id", "root_inner");
		svgRootInner.setAttributeNS(null , "width", Integer.toString(clipRect.width));
		svgRootInner.setAttributeNS(null , "height", Integer.toString(clipRect.height));
		svgRootInner.setAttributeNS(null, "viewBox", 
				String.format("%d %d %d %d",  clipRect.x, clipRect.y, clipRect.width, clipRect.height));
		svgRootInner.setAttributeNS(null, "preserveAspectRatio", "xMidYMid slice");
		
		svgRootOuter.appendChild(svgRootInner);
		svgRootInner.appendChild(image);

		return doc;
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
		svgRootOuter.setAttribute("version", "1.1");
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
			svgRootInner.setAttribute("version", "1.1");
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

	public static Document createSvgDocument2(Rectangle clipRect, Rectangle imageRect, Document svgBody, boolean isPreview, int margin) {
		DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
		if (clipRect == null) {
			clipRect = imageRect;
		}
		Rectangle rootRect = isPreview ? clipRect : imageRect;
		Document doc = svgBody;

		Element svgRoot = doc.getDocumentElement();
		NodeList svgNodes = doc.getElementsByTagName("svg");
		Element svgNode = (Element)svgNodes.item(0);
		Node documentRoot = svgNode.getParentNode();
		documentRoot.removeChild(svgNode);

		Element svgRootOuter = (Element) doc.createElementNS(svgNS, "svg");

		svgRootOuter.setAttribute("id", "root");
		svgRootOuter.setAttribute("version", "1.1");
		svgRootOuter.setAttributeNS(null , "width", "100%");
		svgRootOuter.setAttributeNS(null , "height", "100%");
		svgRootOuter.setAttributeNS(null, "viewBox", 
				String.format("%d %d %d %d",  -margin, -margin, rootRect.width + margin*2, rootRect.height + margin*2));
		svgRootOuter.setAttributeNS(null, "preserveAspectRatio", "xMidYMid meet");

//		Element image = doc.createElementNS(svgNS, "image");
//		image.setAttributeNS(null, "width", Integer.toString(imageRect.width));
//		image.setAttributeNS(null, "height", Integer.toString(imageRect.height));
		
//		XLinkSupport.setXLinkHref(image,  imageURI);
//		image.setAttributeNS(null, "xlink:href", imageURI);

		if (isPreview) {
			Element svgRootInner = (Element) doc.createElementNS(svgNS, "svg");
			svgRootInner.setAttribute("id", "root_inner");
			svgRootInner.setAttribute("version", "1.1");
			svgRootInner.setAttributeNS(null , "width", Integer.toString(clipRect.width));
			svgRootInner.setAttributeNS(null , "height", Integer.toString(clipRect.height));
			svgRootInner.setAttributeNS(null, "viewBox", 
					String.format("%d %d %d %d",  clipRect.x, clipRect.y, clipRect.width, clipRect.height));
			svgRootInner.setAttributeNS(null, "preserveAspectRatio", "xMidYMid slice");
			
			svgRootOuter.appendChild(svgRootInner);
			svgRootInner.appendChild(svgNode);
		} else {
			svgRootOuter.appendChild(svgNode);
		}
		
		documentRoot.appendChild(svgRootOuter);
		
		return doc;
	}

	public static Rectangle getContentArea(IFile item) {
		Rectangle result = new Rectangle();
		String tmpFilename = "";
		try {
			if (PathUtil.isRasterFile(item.getFilename())) {
				String path = PathUtil.getTmpDirectory();
				tmpFilename = path + "tmp" + PathUtil.getExtension(item.getFilename());
				File tmpFile = new File(tmpFilename);
				tmpFile.deleteOnExit();
				try {
					PathUtil.copyFile(item.getInputStream(), tmpFile);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			} else {
				String path = PathUtil.getTmpDirectory();
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
			
	        Rectangle rect = ImageUtility.getSvgSize(item);
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
		String path = PathUtil.getTmpDirectory();
		String tmpFilename = path + UUID.randomUUID() + "." + PathUtil.getExtension(item.getFilename());
		File tmpFile = new File(tmpFilename);
		tmpFile.deleteOnExit();

		IplImage image_source;
		try {
			PathUtil.copyFile(item.getInputStream(), tmpFile);
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

	public static boolean isSpreadPage(ListItem item) {
		boolean isSpreadPage = false;
		try{
			InputStream is = item.getInputStream();
			BufferedImage image = ImageIO.read(is);
			int width = image.getWidth();
			int height = image.getHeight();
			double minWidth = height*182*2*0.8/257;
			double maxWidth = height*182*2*1.2/257;
			if (minWidth < width && width < maxWidth) {
				isSpreadPage = true;
			}
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return isSpreadPage;
	}
}