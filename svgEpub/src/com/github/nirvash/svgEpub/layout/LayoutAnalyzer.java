package com.github.nirvash.svgEpub.layout;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacpp.FloatPointer;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_nonfree.*;
import com.googlecode.javacv.cpp.opencv_features2d.*;
import com.googlecode.javacv.cpp.opencv_imgproc.*;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_flann.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_features2d.*;

import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import jknnl.kohonen.WTALearningFunction;
import jknnl.learningFactorFunctional.ConstantFunctionalFactor;
import jknnl.metrics.EuclidesMetric;
import jknnl.network.DefaultNetwork;
import jknnl.topology.HexagonalTopology;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

import com.github.nirvash.svgEpub.Epub;
import com.github.nirvash.svgEpub.list.FileItem;
import com.github.nirvash.svgEpub.util.ImageUtility;
import com.github.nirvash.svgEpub.util.PathUtil;
import com.github.nirvash.svgEpub.util.Profile;
import com.github.nirvash.svgEpub.util.QuickSort;
import com.github.nirvash.svgEpub.util.RuntimeUtility;


public class LayoutAnalyzer {
	static String fontForgePath = "";
	public static void setFontForgePath(String path) {
		fontForgePath = path;
	}
	
	public static File createFont(IplImage image_source, IplImage image_binary, double scale,
			ArrayList<LayoutElement> elements, int page) {
		Profile.setLaptime("createFont");

		analyzePageLayout(image_source, image_binary, elements, false, scale);
//		learnGlyphs(image_source, elements);
		String fontPath = PathUtil.getTmpDirectory()+"work.ttf";
		Profile.setLaptime("saveFont");
		File fontFile = saveFont(image_source, image_binary, elements, fontPath, scale);
		fontFile.deleteOnExit();
		
		Profile.setLaptime("createFont (end)");
		return fontFile;
	}

	private static File saveFont(IplImage image_source, IplImage image_binary2,
			ArrayList<LayoutElement> elements, String fontPath, double scaleIn) {
		String ext_in = "bmp";
		String ext_out = "svg";

		Profile.setLaptime("binalize");
		double scale = 4.0;
		CvSize targetSize = new CvSize((int)(image_source.width()*scale), (int)(image_source.height()*scale));
		IplImage image_binary = cvCreateImage( targetSize, IPL_DEPTH_8U, 1);
		ImageUtility.binalize(image_source, image_binary, false);
		scale = scale/scaleIn;
		
//		cvNot(image_binary, image_binary);
		String tmpdir = PathUtil.getTmpDirectory();
		String inputPath = tmpdir + "font\\";
		File inputFile = new File(inputPath);
		if (inputFile.exists()) inputFile.delete();
		inputFile.mkdirs();

		Profile.setLaptime("potrace");
		int index = 0x3400;
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			for (LayoutElement r : le.elements) {
				if (r.getType() != LayoutElement.TYPE_CHARACTER) continue;
				CvRect rect = toCvRect(r.rect, scale);
				cvSetImageROI(image_binary, rect);
				CvSize size = new CvSize(rect.width(), rect.height());
				IplImage image_char = cvCreateImage(size, IPL_DEPTH_8U, 1);
				cvCopy(image_binary, image_char);

//				cvSaveImage(filename, image_binary);
				BufferedImage bImage = image_char.getBufferedImage();
				BufferedImage bImageBinary = new BufferedImage(bImage.getWidth(), bImage.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
				Graphics2D g = bImageBinary.createGraphics();
				g.drawImage(bImage, 0, 0, null);

				String charBitmapFilename = String.format("%su%04x.%s", inputPath, index, ext_in);
				File charBitmapFile = new File(charBitmapFilename);
				try {
					ImageIO.write(bImageBinary, ext_in, charBitmapFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
				cvReleaseImage(image_char);
				
				String svgFilename = String.format("%su%04x.%s", inputPath, index, ext_out);
				File svgFile = Epub.convertToSvg(charBitmapFile, svgFilename, new Rectangle(bImage.getWidth(), bImage.getHeight()));
				svgFile.deleteOnExit();
				charBitmapFile.delete();

				r.setCodePoint(index);
				r.setText(new String(Character.toChars(index)));
				index++;
			}
		}
		
		cvReleaseImage(image_binary);
		Profile.setLaptime("fontforge");
		File batchFile = new File("fontforge-script.bat");
		if (!batchFile.exists()) return null;
		
		File fontforge = new File(fontForgePath);
		if (fontforge == null || !fontforge.exists()) {
			return null;
		}
		
		File fontFile = new File(fontPath);
		fontFile.deleteOnExit();
		String scriptPath = getScriptPath();
		String workdir = PathUtil.getTmpDirectory();
		inputPath = "font\\u^*." + ext_out; // escape for command propt.

		ArrayList<String> commands = new ArrayList<String>();
		commands.add(String.format("\"%s\"", batchFile.getAbsoluteFile()));
		commands.add(String.format("\"%s\\\"", fontforge.getAbsoluteFile()));
		commands.add(String.format("\"%s\"", workdir));
		commands.add(String.format("\"%s\"", scriptPath));
		commands.add(String.format("\"%s\"", fontPath));
		commands.add(String.format("\"%s\"", inputPath));

		try {
			StringBuffer msg = new StringBuffer();
			int ret = RuntimeUtility.execute(commands, msg);
			System.out.print(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fontFile;
	}

	private static String getScriptPath() {
		File script = new File("import.pe");
		try {
			if (!script.exists()) {
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				PathUtil.copyFile(classLoader.getResourceAsStream("resources/import.pe"), script);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return script.getAbsolutePath();
	}

	public static File analyzePageLayout(File file) {
		IplImage image_source = cvLoadImage(file.getPath());
		ArrayList<LayoutElement> elements = new ArrayList<LayoutElement>();

		double scale = 1;
		CvSize size_target = new CvSize((int)(image_source.width()*scale), (int)(image_source.height()*scale));
		IplImage image_binary = cvCreateImage( size_target, IPL_DEPTH_8U, 1);

		analyzePageLayout(image_source, image_binary, elements, true, scale);
		drawPageLayout(image_source, elements, scale, 2, CvScalar.MAGENTA, CvScalar.CYAN);
		
		cvSaveImage(file.getPath(), image_source);
		cvReleaseImage(image_source);
		cvReleaseImage(image_binary);
		return file;
	}
	
	public static void analyzePageLayout(IplImage image_source, IplImage image_binary, ArrayList<LayoutElement> elements, boolean draw, double scale) {
		Profile.setLaptime("analyzePageLayout (start)");
		if (LayoutAnalyzer.getLineElement(image_source, image_binary, elements, false, false)) {
			/*
				IplImage tmp = cvCloneImage(image_source);
				double angle = calcSkew(group, tmp);
				cvSaveImage("test_line.png", tmp);
				cvReleaseImage(tmp);
			 */
			Profile.setLaptime("calcSkew");
			double angle = LayoutAnalyzer.calcSkew(elements, null);
			Profile.setLaptime("deskew");
			LayoutAnalyzer.deskew(image_source, angle, new CvRect(0, 0, image_source.width(), image_source.height()));

			Profile.setLaptime("getLineElement");
			LayoutAnalyzer.getLineElement(image_source, image_binary, elements, true, true);
			Profile.setLaptime("analyzeLayout");
			LayoutAnalyzer.analyzeLayout(image_source, elements, draw, scale);
		}
		Profile.setLaptime("analyzePageLayout (end)");
	}

	private static void learnGlyphs(IplImage image_source,
			ArrayList<LayoutElement> elements) {
		double scale = 1.0f;
		CvSize size = new CvSize((int)(image_source.width()*scale), 
 								 (int)(image_source.height()*scale));
		IplImage image_binary = cvCreateImage( size, IPL_DEPTH_8U, 1);
		ImageUtility.binalize(image_source, image_binary, false);

		int i=0;
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			for (LayoutElement r : le.elements) {
				CvRect rect = toCvRect(r.rect, scale);
				cvSetImageROI(image_binary, rect);
				extractSift(image_binary, rect, i);
				//extractSurf(image_source, image_binary, i);
				i++;
			}
		}
		
		cvReleaseImage(image_binary);
	}

	private static void extractSift(IplImage image_binary, CvRect size, int i ) {
		double aspect = (double)(size.width())/size.height();
//		CvSize charSize = new CvSize((int)(100*aspect), 100);
		CvSize charSize = new CvSize(size.width(), size.height());

		IplImage image_char = cvCreateImage(charSize, image_binary.depth(), 1);
		cvResize(image_binary, image_char, CV_INTER_NN);
		
		
		KeyPoint keyPoints = new KeyPoint();
		int nfeatures = 0;
		int nOctaveLayers = 4;
		double contrastThreshold = 0.01;
		double edgeThreshold = 15;
		double sigma = 0.5;
		SIFT sift = new SIFT(nfeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma);
//		sift.detect(image_char, descriptors, keyPoints);
		CvMat descriptors = cvCreateMat(1, 128, CV_32F); // row will be allocated by detect func.
		sift.detectAndCompute(image_char, null, keyPoints, descriptors, false);
		/*
		if (keyPoints.size() != 0) {
			IndexParams indexParams = new KDTreeIndexParams(4);
			Index index = new Index(descriptors, indexParams, FLANN_DIST_EUCLIDEAN);
			SearchParams searchParams = new SearchParams(64, 0, true);

			
			int knn = 2;
			CvArr databaseMat = cvCreateMat(1, 128, CV_32F);
			cvGetM
			CvMat indiciesMat = null;
			CvMat distMat = null;
			index.knnSearch(databaseMat, indiciesMat, distMat, knn, searchParams);
		}
*/		
		IplImage tmp = cvCreateImage(cvGetSize(image_char), image_char.depth(), 3);
		drawKeypoints(image_char, keyPoints, tmp, CvScalar.RED, DrawMatchesFlags.DRAW_RICH_KEYPOINTS);
		cvSaveImage(String.format("font\\%d.png", i), tmp);
		cvReleaseImage(image_char);
		cvReleaseImage(tmp);
	}

	private static void extractSurf(IplImage image_source,
			IplImage image_binary, int i) {
		double hessianThreshold = 300;
		int nOctaves = 4;
		int nOctaveLayers = 2;
		boolean extended = true;
		boolean upright = true;
		SURF surf = new SURF(hessianThreshold, nOctaves, nOctaveLayers, extended, upright);
		KeyPoint keyPoints = new KeyPoint();
		surf.detect(image_source, null, keyPoints);
		
		IplImage tmp = cvCreateImage(cvGetSize(image_source), image_binary.depth(), 3);
		drawKeypoints(image_source, keyPoints, tmp, CvScalar.RED, DrawMatchesFlags.DRAW_RICH_KEYPOINTS);
		cvSaveImage(String.format("font\\%d.png", i), tmp);
		cvReleaseImage(tmp);
	}


	private static void learnGlyphsSOM(IplImage image_source,
			ArrayList<LayoutElement> elements) {
		
		double scale = 1.0f;
		CvSize size = new CvSize((int)(image_source.width()*scale), 
 								 (int)(image_source.height()*scale));
		IplImage image_binary = cvCreateImage( size, IPL_DEPTH_8U, 1);
		ImageUtility.binalize(image_source, image_binary, false);

		HexagonalTopology topology = new HexagonalTopology(40, 40);
		double[] maxWeight = new double[16];
		Arrays.fill(maxWeight, 1 << 16);
		
		DefaultNetwork network = new DefaultNetwork(16, maxWeight, topology);
		ConstantFunctionalFactor constantFator = new ConstantFunctionalFactor(0.8);
		FontDataModel learningData = new FontDataModel(elements, image_binary, scale, 16, 16);
		WTALearningFunction learning = new WTALearningFunction(network, 500, new EuclidesMetric(), learningData, constantFator);
		learning.learn();

		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		
		for (int i=0; i<learningData.getDataSize(); i++) {
			int ret = learning.getBestNeuron(learningData.getData(i));
			map.put(ret, i);
		}
		
		for (int i=0; i<learningData.getDataSize(); i++) {
			LayoutElement le = learningData.getLayoutElement(i);
			
			cvSetImageROI(image_binary, toCvRect(le.rect, scale));
			String filename = String.format("in\\%d.png", i);
			cvSaveImage(filename, image_binary);
			
			int ret = learning.getBestNeuron(learningData.getData(i));
			
			le = learningData.getLayoutElement(map.get(ret));
			
			cvSetImageROI(image_binary, toCvRect(le.rect, scale));
			filename = String.format("out\\%d.png", i);
			cvSaveImage(filename, image_binary);
		}
		
		
		int i = 0x3041; //small hiragana a
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			for (LayoutElement r : le.elements) {
				cvSetImageROI(image_binary, toCvRect(r.rect, scale));
				String filename = String.format("u%04x.bmp", i);
				String filenameSvg = String.format("u%04x.svg", i++);
				cvSaveImage(filename, image_binary);
				
				File tmpFile = new File(filename);
				File svgFile = Epub.convertToSvgFromImage(new FileItem(tmpFile, null));
				File moveDir = new File("c:\\fontforge\\svg");
				if (!moveDir.exists()) moveDir.mkdirs();
				File moveFile = new File(moveDir.getPath() + "\\" + filenameSvg);
				if (moveFile.exists()) moveFile.delete();
				svgFile.renameTo(moveFile);
				tmpFile.delete();
			}
		}
		
		
		cvReleaseImage(image_binary);
	}

	public static CvRect toCvRect(Rectangle r, double scale) {
		return new CvRect((int)(r.x*scale), (int)(r.y*scale), 
				          (int)(r.width*scale), (int)(r.height*scale));
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

	static void analyzeLayout(IplImage image_source,
			ArrayList<LayoutElement> elements, boolean draw, double scale) {
		// Check horizontal element
		LayoutAnalyzer.checkTextHorizontal(elements);

		// Check vertical element
		LayoutAnalyzer.checkTextVertical(elements);

		// Detect column
		Rectangle textColumn = null;
		textColumn = getTextColumn(elements, textColumn);
//		drawRectangle(image_source, textColumn, CvScalar.YELLOW);

		// Check elements in column
		LayoutAnalyzer.checkTextInColumn(elements, textColumn);

		// Check ruby element
		double rubyThreshold = LayoutAnalyzer.checkRubyElement(elements);

		// Check ruby element (elements which is next to vertical element)
		validateRuby(elements);

		// Merge vertical lines
		mergeVerticalLines(elements);
		
		// Check multi columns
		ArrayList<Rectangle> columnList = new ArrayList<Rectangle>();
		LayoutAnalyzer.checkMultiColumns(elements, columnList, rubyThreshold);
		
		// Align vertical elements
		LayoutAnalyzer.alignVerticalTextInColumn(elements, columnList);
		
		// Merge horizontal elements in vertical lines.
		mergeHorizontalElementsInVerticalLines(elements);
		
		// Extract nombre
		extractNonbre(elements, columnList);

		// checkCharacter
		LayoutAnalyzer.checkCharacter(elements, scale);
		
		// Add control code
		addControlCode(elements, columnList);

		if (draw) {
			drawColumns(image_source, columnList, scale);
		}
	}

	private static void extractNonbre(ArrayList<LayoutElement> elements,
			ArrayList<Rectangle> columnList) {
		Rectangle top, bottom;
		if (columnList.size() == 2) {
			top = columnList.get(0);
			bottom = columnList.get(1);
		} else if (columnList.size() == 1) {
			top = columnList.get(0);
			bottom = top;
		} else {
			return;
		}
		
//		extractTopRightNonbre(elements, top);
		extractBottomRightNonbre(elements, bottom);
	}

	private static void extractTopRightNonbre(
			ArrayList<LayoutElement> elements, Rectangle top) {
		DoubleArrayList ylist = new DoubleArrayList();
		for (LayoutElement le : elements) {
			if (top.intersects(le.rect)) {
				if (le.y() < top.y + le.width()*3) {
					ylist.add(le.y());
				}
			}
		}
		
		boolean hasNonbre = false;
		double threshold = calcThreshold(ylist, 1, null, 0.99, false);
		if (threshold == 0) return;
		int count = 0;
		for (LayoutElement le : elements) {
			if (top.intersects(le.rect)) {
				if (le.y() < threshold) {
					count++;
				}
			}
		}
		if (count > 1) return;
		
		for (LayoutElement le : elements) {
			if (top.intersects(le.rect)) {
				if (le.y() < threshold) {
					LayoutElement nonbre = new LayoutElement(0, LayoutElement.TYPE_TEXT_HORIZONTAL);
					for (int i=le.elements.size()-1; i>=0; i--) {
						LayoutElement ch = le.elements.get(i);
						if (ch.rect.getCenterY() < threshold) {
							nonbre.addChild(ch);
							le.elements.remove(i);
						}
					}
					if (nonbre.elements.size()>0) {
						le.calcBoundsRect();
						elements.add(nonbre);
						hasNonbre = true;
					}
				}
				break;
			}
		}
		
		if (hasNonbre) {
			Rectangle column = null;
			for (LayoutElement le : elements) {
				if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
				if (top.intersects(le.rect)) {
					if (column == null) {
						column = new Rectangle(le.rect);
					} else {
						column.add(le.rect);
					}
				}
			}
			top.setBounds(column);
		}
	}
	
	private static void extractBottomRightNonbre(
			ArrayList<LayoutElement> elements, Rectangle bottom) {
		DoubleArrayList ylist = new DoubleArrayList();
		for (LayoutElement le : elements) {
			if (bottom.intersects(le.rect)) {
				if (le.getMaxY() > bottom.getMaxY() - le.width()*3) {
					ylist.add(le.getMaxY());
				}
			}
		}
		
		boolean hasNonbre = false;
		double threshold = calcThreshold(ylist, 1, null, 0.99, false);
		if (threshold == 0) return;
		int count = 0;
		for (LayoutElement le : elements) {
			if (bottom.intersects(le.rect)) {
				if (le.getMaxY() > threshold) {
					count++;
				}
			}
		}
		if (count > 1) return;
		
		for (LayoutElement le : elements) {
			if (bottom.intersects(le.rect)) {
				if (le.getMaxY() > threshold) {
					LayoutElement nonbre = new LayoutElement(0, LayoutElement.TYPE_TEXT_HORIZONTAL);
					for (int i=le.elements.size()-1; i>=0; i--) {
						LayoutElement ch = le.elements.get(i);
						if (ch.rect.getCenterY() > threshold) {
							nonbre.addChild(ch);
							le.elements.remove(i);
						}
					}
					if (nonbre.elements.size()>0) {
						le.calcBoundsRect();
						elements.add(nonbre);
						hasNonbre = true;
					}
				}
				break;
			}
		}
		
		if (hasNonbre) {
			Rectangle column = null;
			for (LayoutElement le : elements) {
				if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
				if (bottom.intersects(le.rect)) {
					if (column == null) {
						column = new Rectangle(le.rect);
					} else {
						column.add(le.rect);
					}
				}
			}
			bottom.setBounds(column);
		}
	}

	private static void drawColumns(IplImage image_source,
			ArrayList<Rectangle> columnList, double scale) {
		for (Rectangle column : columnList) {
			drawRectangle(image_source, column, CvScalar.RED, scale);
		}
	}

	private static void drawRectangle(IplImage image_source, Rectangle column, CvScalar color, double scale) {
		if (column == null) return;
		CvPoint p1 = new CvPoint((int)(column.x/scale), (int)(column.y/scale));
		CvPoint p2 = new CvPoint((int)(column.getMaxX()/scale), (int)(column.getMaxY()/scale));
		cvDrawRect(image_source, p1, p2, color, 1, 0, 0);
	}

	private static void addControlCode(ArrayList<LayoutElement> elements,
			ArrayList<Rectangle> columnList) {
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			for (Rectangle column : columnList) {
				if (!column.intersects(le.rect)) continue; 
				for (LayoutElement ch : le.elements) {
					ch.setType(LayoutElement.TYPE_CHARACTER);
				}
				
				// 追い出しで行末に空白ができることを考慮
				if (le.getMaxY() < column.getMaxY() - le.width()*3) {
					le.setLF(true);
				}
			}
		}
		
	}

	private static void mergeHorizontalElementsInVerticalLines(
			ArrayList<LayoutElement> elements) {
		// merge character parts (ニ, 三, う, etc.)
		for (int i=elements.size()-1; i>=0; i--) {
			LayoutElement le1 = elements.get(i);
			if (le1.getType() != LayoutElement.TYPE_TEXT_HORIZONTAL) continue;
			for (int j=elements.size()-1; j>=0; j--) {
				LayoutElement le2 = elements.get(j);
				if (le2.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
				if (le2.rect.contains(le1.rect)) {
					le2.add(le1);
					elements.remove(i);
					break;
				}
			}
		}
	}

	private static void mergeVerticalLines(ArrayList<LayoutElement> elements) {
		for (int i=elements.size()-1; i>=0; i--) {
			LayoutElement le1 = elements.get(i);
			if (le1.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			for (int j=i-1; j>=0; j--) {
				LayoutElement le2 = elements.get(j);
				if (le2.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
				if (!(le2.x() < le1.rect.getCenterX() && le1.rect.getCenterX() < le2.getMaxX())) continue;
				int diff = (int) (le2.y() > le1.y() ? le2.y()-le1.getMaxY() : le1.y() - le2.getMaxY());
				int width = Math.max(le1.width(), le2.width());
				if (diff < width) {
					le2.add(le1);
					elements.remove(i);
					break;
				}
			}
		}		
	}

	static void validateRuby(ArrayList<LayoutElement> elements) {
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
			} else if (le.getType() == LayoutElement.TYPE_TEXT_VERTICAL) {
				for (LayoutElement vert : elements) {
					if (vert.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
					Rectangle body = new Rectangle(vert.rect);
					body.width *= 1.1f;
					if (body.intersects(le.rect) && le.width() < vert.width() * 0.8) {
						le.setType(LayoutElement.TYPE_RUBY);
						break;
					}
				}
			}
		}
		
		
	}

	static Rectangle getTextColumn(ArrayList<LayoutElement> elements,
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

	static void removeSmallRects(List<LayoutElement> rects, double scale) {
		for (int i=rects.size()-1; i >= 0 ; i--) {
			LayoutElement r1 = rects.get(i);
			if (r1.width() * r1.height() <= 9*scale*scale) {
				rects.remove(i);
			}
		}		
	}

	static void separateRuby(List<LayoutElement> group) {
		List<LayoutElement> rubyList = new ArrayList<LayoutElement>();
		for (LayoutElement le : group) {
			List<LayoutElement> rubys = le.extractRuby();
			if (rubys != null) {
				rubyList.addAll(rubys);
			}
		}

		if (!rubyList.isEmpty()) {
			group.addAll(rubyList);
		}
	}

	static void groupXAxis(List<LayoutElement> group) {
		for (int i=group.size()-1; i>=0; i--) {
			LayoutElement l1 = group.get(i);
			if (l1.getType() == LayoutElement.TYPE_IMAGE) continue;
			for (int j=0; j<i; j++) {
				LayoutElement l2 = group.get(j);
				if (l2.getType() == LayoutElement.TYPE_IMAGE) continue;
				if (overwrapX(l1.rect, l2.rect, true)) {
					l2.add(l1);
					group.remove(i);
					break;
				}
			}
		}
	}

	static void groupYAxis(List<LayoutElement> group, double margin, boolean isChar) {
		for (int i=group.size()-1; i>=0; i--) {
			LayoutElement l1 = group.get(i);
			if (l1.getType() == LayoutElement.TYPE_IMAGE) continue;
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
	
	static void groupYAxis2(List<LayoutElement> group) {
		for (int i=group.size()-1; i>=0; i--) {
			LayoutElement l1 = group.get(i);
			if (l1.getType() == LayoutElement.TYPE_IMAGE) continue;
			for (int j=i-1; j>=0; j--) {
				LayoutElement l2 = group.get(j);
				if (l2.getType() == LayoutElement.TYPE_IMAGE) continue;
				if ((l2.x() < l1.rect.getCenterX() && l1.rect.getCenterX() < l2.getMaxX()) ||
					(l1.x() < l2.rect.getCenterX() && l2.rect.getCenterX() < l1.getMaxX())) {
					double distance;
					if (l1.getMaxY() < l2.y()) {
						distance = Math.abs(l2.y() - l1.getMaxY());
					} else {
						distance = Math.abs(l1.y() - l2.getMaxY());
					}
					
					if (distance > Math.min(l1.width(), l2.width()) * 1f) continue;

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

	static boolean overwrapYLine(Rectangle r1, Rectangle r2, double margin) {
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

		boolean isOverwrap = !(r1.x >= r2.getMaxX() || r1.getMaxX() <= r2.x);
		if (!isOverwrap) return isOverwrap;
		
		Rectangle intersect = r1.intersection(r2);
		int area = intersect.width*intersect.height;
		if (area > r1.width*r1.height*0.4 || area > r2.width*r2.height*0.4) {
			return true;
		}

		double distance;
		if (r1.getMaxY() < r2.y) {
			distance = Math.abs(r2.y - r1.getMaxY());
		} else {
			distance = Math.abs(r1.y - r2.getMaxY());
		}

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
	
	private static boolean overwrapXSimple(Rectangle r1, Rectangle r2) {
		if (r1.intersects(r2)) return true;

		boolean isOverwrap = !(r1.y > r2.getMaxY() || r1.getMaxY() < r2.y);
		if (!isOverwrap) return isOverwrap;
		return true;
	}


	private static boolean overwrapX(Rectangle r1, Rectangle r2, boolean verticalLimitaion) {
		if (verticalLimitaion && (r1.height > r1.width*3 || r2.height > r2.width*3)) return false;
		if (r1.intersects(r2)) return true;

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

	static void mergeRects(List<LayoutElement> rects) {
		for (int i=rects.size()-1; i >= 0 ; i--) {
			LayoutElement r1 = rects.get(i);
			boolean isRemoved = false;
			for (int j=0; j<i; j++) {
				LayoutElement r2 = rects.get(j);
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

	static void getRects(CvSeq contours, List<LayoutElement> rects, double scale) {
		while (contours != null && !contours.isNull()) {
			if (contours.elem_size() > 0) {
				CvRect cr = cvBoundingRect(contours, 1);
				Rectangle r = new Rectangle((int)(cr.x()), (int)(cr.y()), (int)(cr.width()), (int)(cr.height()));
				rects.add(new LayoutElement(r));
			}
			contours = contours.h_next();
		}
	}

	static double calcSkew(ArrayList<LayoutElement> group, IplImage image) {
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

//		cvSaveImage("test_line.png", image_test);
		cvReleaseImage(image_src);
		cvReleaseImage(image_test);

		return angle;
	}

	static void checkCharacter(ArrayList<LayoutElement> elements, double scale) {
		// merge horizontal
		mergeCharHorizontal(elements);

		alignCharacterHeight(elements, scale);
		alignCharacterWidth(elements);
		normalizeCharacterSize(elements);
	}

	private static void normalizeCharacterSize(ArrayList<LayoutElement> elements) {
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			for (LayoutElement r : le.elements) {
				if (r.width() == r.height()) continue;
				if (r.height() < r.width()) {
					int diff = r.width() - r.height();
					r.rect.y -= diff / 2;
					r.rect.height += diff;
				}
			}
		}
	}

	static void alignCharacterWidth(ArrayList<LayoutElement> elements) {
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			int center = 0;
			int count = 0;
			for (LayoutElement r : le.elements) {
				if (r.width() > le.width() * 0.8) {
					count++;
					center += r.rect.getCenterX();
				}
			}
			if (count > 2) {
				center /= count;
			} else {
				center = (int)le.rect.getCenterX();
			}
			for (LayoutElement r : le.elements) {
				if (r.rect.getCenterX() >= center) {
					int width = (int)((r.getMaxX() - center) * 2);
					r.rect.x = Math.max(center - width/2, le.x());
					r.rect.width = Math.min(width, le.width());
				} else {
					int width = (int)((center - r.x()) * 2);
					r.rect.x = Math.max(center - width/2, le.x());
					r.rect.width = Math.min(width, le.width());
				}
				int diff = Math.min(r.height(), le.width()) - r.width();

				if (diff > 0) {
					int rcenter = (int)r.rect.getCenterX();
					r.rect.width += diff;
					r.rect.x -= r.rect.getCenterX() - rcenter;
					/*
					int right = (int)r.getMaxX() - center;
					int left = center - r.x();
					if (right - left > diff) {
						r.rect.x -= diff;
					} else if (right - left > 0) {
						int rdiff = right - left;
						r.rect.x -= rdiff + (diff - rdiff) / 2;
					} else if (left - right > diff) {
						// nop
					} else {
						int rdiff = left - right;
						r.rect.x -= (diff - rdiff) / 2 + rdiff;
					}
					r.rect.width += diff;
					*/
				}
			}
		}
	}
	
	static void alignCharacterHeight(ArrayList<LayoutElement> elements, double scale) {
		DoubleArrayList hlist = new DoubleArrayList();
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			for (LayoutElement r : le.elements) {
				hlist.add(r.height());
			}
		}
		if (hlist.size()==0) return;

		double[] avglist = new double[2];
		double threshold = calcThreshold(hlist, 1, avglist, 0.7, false);
		double avgHeight = Math.max(avglist[0], avglist[1]);

		int charHeightGlobal = 0;
		int limit = Math.max(2,(int)(hlist.size() * 0.1f));
		if (limit < hlist.size()) {
			hlist.sort();
			charHeightGlobal = (int)hlist.get(hlist.size()-limit);
		} else {
			charHeightGlobal = (int)hlist.get(hlist.size()-1);
		}

		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			if (le.elements.size()==0) continue;
			int charHeight = charHeightGlobal;
			if (charHeight < le.width() * 0.8f || charHeight > le.width() * 1.2f) {
				charHeight = le.width();
			}
			Collections.sort(le.elements, new CharacterInLineComparator());
			LinkedList<LayoutElement> elems = new LinkedList<LayoutElement>(le.elements);

			int index = 0;
			LayoutElement r0 = null;
			LayoutElement r1 = elems.get(index);
			LayoutElement r2 = null;
			if (r1.y() < le.y() + avgHeight) {
				r1.rect.add(le.rect.getLocation());
			} else {
				int distY = r1.y() - le.y();
				if (distY < charHeight*0.9) {
					int height = Math.min(charHeight, distY);
					
					// insert space character
					Rectangle r = new Rectangle(le.x(), le.y(), le.width(), height);
					LayoutElement newElem = new LayoutElement(r);
					elems.addFirst(newElem);
					r1 = elems.get(index);
				}
			}
			index++;

			for (;index < elems.size(); index++) {
				r2 = elems.get(index);
				if (r0 != null) {
					int distY = r1.y() - (int)r0.getMaxY();
					if (distY > charHeight*0.9) {
						// insert space character
						Rectangle r = new Rectangle(le.x(), (int)r0.getMaxY(), le.width(), charHeight);
						LayoutElement newElem = new LayoutElement(r);
						elems.add(index-1, newElem);
						r0 = newElem;
						index++;
					} else if (distY > le.width()*0.2 && r1.height()+distY*0.8<charHeight) {
						// Adjust blank for characters (へ,つ)
						r1.rect.y -= distY*0.8;
						r1.rect.height += distY*0.8;
					}
				}
				
				// separate too large character
				while (r1.height() > charHeight*1.2f) {
					Rectangle r = new Rectangle(r1.x(), r1.y(), r1.width(), charHeight);
					LayoutElement newElem = new LayoutElement(r);
					elems.add(index-1, newElem);
					r0 = newElem;
					r1.rect.y += charHeight;
					r1.rect.height -= charHeight;
					index++;
				}

				boolean lastChar = false;
				boolean merged = false;
				do {
					merged = false;
					if (r1.height() < charHeight) {
						if (r2.y() - r1.getMaxY() > charHeight/2) break; // Too far to merge
						if (r2.getMaxY() - r1.y() <= charHeight+2*scale) {
							merged = true;
							r1.add(r2);
							elems.remove(index);
							if (index >= elems.size()) {
								lastChar = true;
								break;
							}
							r2 = elems.get(index);
						} else if (r1.height() < charHeight*0.7) {
							// following character is too large.
							if (r2.height() > charHeight*1.05f) {
								int diff = charHeight - (r2.y() - r1.y());
								Rectangle r2Top = new Rectangle(r2.x(), r2.y(), r2.width(), diff);
								r2.rect.y += diff;
								r2.rect.height -= diff;
								r1.rect.add(r2Top);
							}
						}
					}
				} while (merged);
				if (lastChar) break;

				if (r1.height() < charHeight) {
					int marginTop = 0, marginBottom = 0;
					if (r0 != null && r1.y() - r0.getMaxY() > 0) {
						marginTop = Math.max(0, r1.y() - (int)r0.getMaxY());
					}
					marginBottom = Math.max(0, r2.y() - (int)r1.getMaxY());
					int maxMarginBottom = marginBottom;
					int diff = charHeight - r1.height();
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

					if (r0 != null) r1.rect.add(new Point(r1.x(), (int)r0.getMaxY() + marginTop));
					r1.rect.add(new Point(r1.x(), r2.y() - marginBottom));
				}

				r0 = r1;
				r1 = r2;
			}

			// separate too large character
			while (r1.height() > charHeight*1.2f) {
				Rectangle r = new Rectangle(r1.x(), r1.y(), r1.width(), charHeight);
				LayoutElement newElem = new LayoutElement(r);
				elems.add(index-1, newElem);
				r0 = newElem;
				r1.rect.y += charHeight;
				r1.rect.height -= charHeight;
				index++;
			}
			
			// Align the last character in the line.
			if (r1 != null) {
				if (r0 != null && r1.y() - r0.getMaxY() > 0) {
					r1.rect.add(new Point(r1.x(), (int)r0.getMaxY()));
				}
				if (r1.height() < charHeight) {
					r1.rect.height += charHeight - r1.height();
					le.rect.add(r1.rect);
				}
			}

			le.elements.clear();
			le.elements.addAll(elems);
		}		
	}

	private static void mergeCharHorizontal(ArrayList<LayoutElement> elements) {
		for (LayoutElement le : elements) {
			for (int i=le.elements.size()-1; i>=0; i--) {
				LayoutElement ch1 = le.elements.get(i);
				for (int j=i-1; j>=0; j--) {
					LayoutElement ch2 = le.elements.get(j);
					if (overwrapXSimple(ch1.rect, ch2.rect)) {
						ch2.add(ch1);
						le.elements.remove(i);
						break;
					}
				}
			}
		}
	}

	static void checkMultiColumns(ArrayList<LayoutElement> elements, 
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

		double threshold = calcThreshold(ylist, 1, null, 0.7, false);
		if (threshold == 0) {
			if (singleColumn != null) {
				columnList.add(singleColumn);
			}
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
	
	
	static void alignVerticalTextInColumn(
			ArrayList<LayoutElement> elements, ArrayList<Rectangle> columnList) {
		sortVerticalText(elements, columnList);
		
		boolean hasNewLine = false;
		for (int i=elements.size()-1; i>=0; i--) {
			LayoutElement le = elements.get(i);
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			for (Rectangle column : columnList) {
				if (column.intersects(le.rect)) {
					if (hasNewLine) {
						le.setLF(true);
					}
					hasNewLine = false;
					int diff = le.rect.y - column.y;
					if (diff < le.rect.width*3) {
						le.rect.y = column.y;
						le.rect.height += diff;
						
						for (int j=0; j<elements.size(); j++) {
							LayoutElement le2 = elements.get(j);
							if (le2.getType() != LayoutElement.TYPE_TEXT_HORIZONTAL) continue;
							if (le.rect.contains(le2.rect)) {
								le.add(le2);
								elements.remove(j);
								if (j<i) {
									i--;
								}
								diff = Math.min(diff, le2.y() - column.y);
							}
						}
						if (diff > le.rect.width*0.4) {
							hasNewLine = true;
						}
					}
					break;
				}
			}
		}
	}

	private static void sortVerticalText(ArrayList<LayoutElement> elements,
			ArrayList<Rectangle> columnList) {
		ArrayList<LayoutElement> columns = new ArrayList<LayoutElement>();
		
		for (int i=0; i<columnList.size(); i++) {
			columns.add(new LayoutElement(0));
			for (int j=elements.size()-1; j>=0; j--) {
				LayoutElement le = elements.get(j);
				if (columnList.get(i).contains(le.rect.getCenterX(), le.rect.getCenterY())) {
					columns.get(i).elements.add(le);
					elements.remove(j);
				}
			}
		}
		
		for (int i=0; i<columnList.size(); i++) {
			Collections.sort(columns.get(i).elements, new RectComparator());
			elements.addAll(columns.get(i).elements);
		}
	}

	static void checkTextInColumn(ArrayList<LayoutElement> elements, Rectangle textColumn) {
		if (textColumn == null) return;
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_UNKNOWN) continue;
			if (textColumn.intersects(le.rect)) {
				le.setType(LayoutElement.TYPE_TEXT_VERTICAL);
			}
		}
	}

	static double checkRubyElement(ArrayList<LayoutElement> elements) {
		DoubleArrayList widths = new DoubleArrayList();
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			widths.add(le.width());
		}
		if (widths.size()>2) {
			widths.sort();
			widths.remove(0);
			widths.remove(widths.size()-1);
		}

		double rubyThreshold = calcThreshold(widths, 1, null, 0.7, false);
		if (rubyThreshold == 0) return rubyThreshold;
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			if (le.width() < rubyThreshold) {
				le.setType(LayoutElement.TYPE_RUBY);
			} else {
				le.setType(LayoutElement.TYPE_TEXT_VERTICAL);
			}
		}
		
		// merge rubys (for cases は,け, に, etc.)
		for (int i=elements.size()-1; i>=0; i--) {
			LayoutElement le1 = elements.get(i);
			if (le1.getType() != LayoutElement.TYPE_RUBY) continue;
			for (int j=i-1; j>=0; j--) {
				LayoutElement le2 = elements.get(j);
				if (le2.getType() != LayoutElement.TYPE_RUBY) continue;
				if (!overwrapX(le1.rect, le2.rect, false)) continue;
				int width = (int) (le1.x() < le2.x() ? le2.getMaxX()-le1.x() : le1.getMaxX()-le2.x());
				if (width < rubyThreshold) {
					le2.add(le1);
					elements.remove(i);
					break;
				}
			}
		}
		return rubyThreshold;
	}

	static void checkTextVertical(
			ArrayList<LayoutElement> elements) {
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_UNKNOWN) continue;
			if (le.rect.height > le.rect.width*3) {
				le.setType(LayoutElement.TYPE_TEXT_VERTICAL);
			}
		}
	}

	static void checkTextHorizontal(ArrayList<LayoutElement> elements) {
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_UNKNOWN) continue;
			if (le.rect.width > le.rect.height*3) {
				le.setType(LayoutElement.TYPE_TEXT_HORIZONTAL);
			}
		}
	}

	public static double calcThreshold(DoubleArrayList lengthList, int k, double[] avglist, double limitRate, boolean useRelative) {
		if (lengthList.size()<4) return 0; // Too few data to analyze
		double threshold = 0;
		double maxIndexBegin = 0;
		double avg = Descriptive.mean(lengthList);
		double max = -1.0f;
		double ravg = 0;
		lengthList.sort();

		final int limit = (int)Math.ceil(lengthList.get(lengthList.size()-1))+1;
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
			double avg1 = 0, avg2 = 0;
			for (int i = 0; i < limit; i++) {
				double n1 = 0;
				double wn1 = 0;
				avg1 = 0;
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
				avg2 = 0;
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
					if (useRelative) {
						if (avg2-avg == 0) {
							ravg = 100000;
						} else {
							ravg = Math.abs(avg1-avg)/Math.abs(avg2-avg);
						}
					} else {
						ravg = avg1 / avg2;
					}
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

			if (useRelative) {
				if (Math.abs(ravg) > limitRate) {
					return 0;
				}
			} else {
				if (Math.abs(ravg) > limitRate) {
					// Maybe single-humped distribution
					return 0;
				}
			}
		}
		
		return threshold;
	}

	static void checkImageElement(List<LayoutElement> group,
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

	static void adjustElementWidth(List<LayoutElement> group) {
		for (LayoutElement le : group) {
			le.adjustElementWidth();
		}
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

	static boolean getLineElement(IplImage image_source, IplImage image_binary,
			ArrayList<LayoutElement> group, boolean doAdjust, boolean drawResult) {
		//		cvSaveImage("test_src.png", image_source);

		ImageUtility.binalize(image_source, image_binary, true);
		//		cvSaveImage("test_binary.png", image_binary);
		// Extract counters
		CvMemStorage storage = CvMemStorage.create();
		CvSeq contours = new CvContour();
		int count = cvFindContours(image_binary, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
		if (count > 10000) return false; // maybe image.
		//		drawContours(image_source, contours, storage, CvScalar.RED);

		List<LayoutElement> rects = new ArrayList<LayoutElement>();
		double scale = image_binary.width() / image_source.width();
		getRects(contours, rects, scale);
		
		// Union intersected rects
		mergeRects(rects);

		// Remove small regions
		removeSmallRects(rects, scale);

		removeOnBorderElement(rects, image_binary.width(), image_binary.height());


		// Draw rects
		Collections.sort(rects, new RectComparator());
		if (drawResult) {
//			drawLineElements(image_source, rects, scale, 2, CvScalar.CYAN, CvScalar.GREEN);
		}

		// Grouping
		mergeCharacterElements(rects, group, image_source, doAdjust);

		storage.release();
		return true;
	}

	private static void removeOnBorderElement(List<LayoutElement> rects,
			int width, int height) {
		int left = (int)(width * 0.01f);
		int right = width - left;
		int top = (int)(height * 0.01f);
		int bottom = height - top;
		Rectangle rLeft = new Rectangle(0, 0, left, height);
		Rectangle rRight = new Rectangle(right,0, left, height);
		Rectangle rTop = new Rectangle(0, 0, width, top);
		Rectangle rBottom = new Rectangle(0, bottom, width, top);

		for (int i=rects.size()-1; i>=0; i--) {
			LayoutElement le = rects.get(i);
			if (le.width()*le.height() > width*0.04*height*0.04) continue;
			if (le.rect.intersects(rLeft) || le.rect.intersects(rRight) ||
				le.rect.intersects(rTop) || le.rect.intersects(rBottom)) {
				rects.remove(i);
			}
		}
	}

	static void mergeCharacterElements(List<LayoutElement> rects, List<LayoutElement> group, IplImage image, boolean doAdjust) {
		group.clear();
		int i=0;
		for (LayoutElement r : rects) {
			LayoutElement le = new LayoutElement(i++);
			le.add(new Rectangle(r.rect));
			group.add(le);
		}

		// Check images
		checkImageElement(group, image);

		// Group y axis
		groupYAxis(group, 1.5f, true);

		// Vertical line merge
		if (doAdjust) {
			groupYAxis2(group);

			groupYAxis(group, 1.5f, false);

			// Group x axis
			groupXAxis(group);

			groupYAxis(group, 2.0f, false);

			separateRuby(group);

			// Adjust element width
//			adjustElementWidth(group);
			
		} else {
			// rough layout analysis
			groupYAxis(group, 1.5f, false);

			// Group x axis
			groupXAxis(group);

			groupYAxis(group, 2.0f, false);

			// separate ruby
//			groupSeparateRuby(group);
		}
	}

	static void drawPageLayout(IplImage image,
			ArrayList<LayoutElement> elements, double scale, int lineWidth, CvScalar rectColor,  CvScalar elemColor) {
		CvFont font = new CvFont(CV_FONT_NORMAL, 0.3, 1);
		IplImage tmpImage = cvCreateImage(image.cvSize(), IPL_DEPTH_8U, 3);
		cvNot(image, image);
		for (LayoutElement le : elements) {
			Rectangle r = le.rect;
			cvSet(tmpImage, new CvScalar(255,255,255,0));
			rectColor = le.getColor();

			cvRectangle(tmpImage, cvPoint((int)(r.x/scale), (int)(r.y/scale)), 
					cvPoint((int)((r.x + r.width)/scale), (int)((r.y + r.height)/scale)),
					rectColor, lineWidth, 0, 0);
			String text = le.toString();
			cvPutText(tmpImage, text, cvPoint((int)(r.x/scale), (int)(r.y/scale) - 4), font, rectColor);

			cvNot(tmpImage, tmpImage);
			cvAddWeighted(image, 1.0f, tmpImage, 1.0f, 0.0, image);
			if (elemColor != null) {
				drawLineElements(image, le.elements, scale, 1, elemColor, null);
			}
		}
		cvNot(image, image);
		cvReleaseImage(tmpImage);

		for (LayoutElement le : elements) {
			if (elemColor != null) {
				drawLineElements(image, le.elements, scale, 1, elemColor, null);
			}
		}
	}

	static void drawLineElements(IplImage image,
			List<LayoutElement> elements, double scale, int lineWidth, CvScalar rectColor, CvScalar textColor) {
		CvFont font = new CvFont(CV_FONT_HERSHEY_SCRIPT_SIMPLEX, 0.3, 1);
		int i = 0;
		for (LayoutElement r : elements) {
			CvPoint p1 = new CvPoint((int)(r.x()/scale), (int)(r.y()/scale));
			CvPoint p2 = new CvPoint((int)((r.x()+r.width())/scale), (int)((r.y()+r.height())/scale));
			cvRectangle(image, p1, p2,
					rectColor, lineWidth, 0, 0);
			if (textColor != null) {
				String text = String.format("%d, %d", (int)(r.width()/scale), (int)(r.height()/scale));
				cvPutText(image, text, new CvPoint((int)(r.getMaxX()/scale+2), (int)(r.y()/scale)), font, textColor);
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
	
	public static class ColumnComparator implements Comparator<LayoutElement> {
		ArrayList<Rectangle> columnList = null;
		public ColumnComparator(ArrayList<Rectangle> columnList) {
			this.columnList = columnList;
		}
		@Override
		public int compare(LayoutElement cr1, LayoutElement cr2) {
			Rectangle c1 = null, c2 = null;
			for (Rectangle c : columnList) {
				if (c.contains(cr1.rect.getCenterX(), cr1.rect.getCenterY())) {
					c1 = c;
					if (c2 != null) break;
				}
				if (c.contains(cr2.rect.getCenterX(), cr2.rect.getCenterY())) {
					c2 = c;
					if (c1 != null) break;
				}
			}
			
			int v1 = c1 != null ? c1.y * 100000 : 0;
			int v2 = c2 != null ? c2.y * 100000 : 0;
			
			int l1 = cr1.x();
			int l2 = cr2.x();
			int r1 = cr1.x() + cr1.width();
			int r2 = cr2.x() + cr2.width();
			
			if (l1 > r2 || r1 < l2) {
				return r2 - r1;
			}
			int t1 = cr1.y() + v1;
			int t2 = cr2.y() + v2;
			return t1 - t2;
		}
	}	

	public static class CharacterInLineComparator implements Comparator<LayoutElement> {
		@Override
		public int compare(LayoutElement cr1, LayoutElement cr2) {
			int t1 = cr1.y();
			int t2 = cr2.y();
			return t1 - t2;
		}
	}	

	public static class RectComparator implements Comparator<LayoutElement> {
		@Override
		public int compare(LayoutElement cr1, LayoutElement cr2) {
			int l1 = cr1.x();
			int l2 = cr2.x();
			int r1 = cr1.x() + cr1.width();
			int r2 = cr2.x() + cr2.width();
			if (l1 > r2 || r1 < l2) {
				return r2 - r1;
			}
			int t1 = cr1.y();
			int t2 = cr2.y();
			return t1 - t2;
		}
	}	

}
