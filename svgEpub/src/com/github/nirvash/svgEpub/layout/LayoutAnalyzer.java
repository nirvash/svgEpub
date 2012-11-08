package com.github.nirvash.svgEpub.layout;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacpp.FloatPointer;
import com.googlecode.javacv.cpp.opencv_core.CvArr;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_nonfree.*;
import com.googlecode.javacv.cpp.opencv_features2d.*;
import com.googlecode.javacv.cpp.opencv_imgproc.*;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_flann.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_features2d.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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


public class LayoutAnalyzer {
	public static void createFont(InputStream in) {
		File file = new File(PathUtil.getTmpDirectory()+"/tmpimage");
		try {
			ImageUtility.copyFile(in, file);
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		IplImage image_source = cvLoadImage(file.getPath());
		ArrayList<LayoutElement> elements = new ArrayList<LayoutElement>();

		analyzePageLayout(image_source, elements);
		learnGlyphs(image_source, elements);
		cvReleaseImage(image_source);
	}

	public static File analyzePageLayout(File file) {
		IplImage image_source = cvLoadImage(file.getPath());
		ArrayList<LayoutElement> elements = new ArrayList<LayoutElement>();

		analyzePageLayout(image_source, elements);
		drawGroup(image_source, elements, 2, CvScalar.MAGENTA, CvScalar.CYAN);
		
		cvSaveImage(file.getPath(), image_source);
		cvReleaseImage(image_source);
		return file;
	}
	
	public static void analyzePageLayout(IplImage image_source, ArrayList<LayoutElement> elements) {
		if (LayoutAnalyzer.getLineElement(image_source, elements, false, false)) {
			/*
				IplImage tmp = cvCloneImage(image_source);
				double angle = calcSkew(group, tmp);
				cvSaveImage("test_line.png", tmp);
				cvReleaseImage(tmp);
			 */
			double angle = LayoutAnalyzer.calcSkew(elements, null);
			LayoutAnalyzer.deskew(image_source, angle, new CvRect(0, 0, image_source.width(), image_source.height()));

			LayoutAnalyzer.getLineElement(image_source, elements, true, true);
			LayoutAnalyzer.analyzeLayout(image_source, elements);
		}
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
			ArrayList<LayoutElement> elements) {
		// Check horizontal element
		LayoutAnalyzer.checkTextHorizontal(elements);

		// Check vertical element
		LayoutAnalyzer.checkTextVertical(elements);

		// Detect column
		Rectangle textColumn = null;
		textColumn = getTextColumn(elements, textColumn);

		// Check elements in column
		LayoutAnalyzer.checkTextInColumn(elements, textColumn);

		// Check ruby element
		double rubyThreshold = LayoutAnalyzer.checkRubyElement(elements);

		// Check ruby element (elements which is next to vertical element)
		validateRuby(elements);

		// Check multi columns
		ArrayList<Rectangle> columnList = new ArrayList<Rectangle>();
		LayoutAnalyzer.checkMultiColumns(elements, columnList, rubyThreshold);

		// Align vertical elements
		LayoutAnalyzer.alignVerticalTextInColumn(elements, columnList);

		// checkCharacter
		LayoutAnalyzer.checkCharacter(elements);
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

	static void removeSmallRects(List<LayoutElement> rects) {
		for (int i=rects.size()-1; i >= 0 ; i--) {
			LayoutElement r1 = rects.get(i);
			if (r1.width() * r1.height() <= 9) {
				rects.remove(i);
			}
		}		
	}

	static void groupSeparateRuby(List<LayoutElement> group) {
		List<LayoutElement> rubyList = new ArrayList<LayoutElement>();
		for (LayoutElement le : group) {
			LayoutElement ruby = le.extractRuby();
			if (ruby != null) {
				rubyList.add(ruby);
			}
		}

		group.addAll(rubyList);
	}

	static void groupXAxis(List<LayoutElement> group) {
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

	static void groupYAxis(List<LayoutElement> group, double margin, boolean isChar) {
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

	static void getRects(CvSeq contours, List<LayoutElement> rects) {
		while (contours != null && !contours.isNull()) {
			if (contours.elem_size() > 0) {
				CvRect cr = cvBoundingRect(contours, 1);
				Rectangle r = new Rectangle(cr.x(), cr.y(), cr.width(), cr.height());
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

		cvSaveImage("test_line.png", image_test);
		cvReleaseImage(image_src);
		cvReleaseImage(image_test);

		return angle;
	}

	static void checkCharacter(ArrayList<LayoutElement> elements) {
		DoubleArrayList hlist = new DoubleArrayList();
		for (LayoutElement le : elements) {
			if (le.getType() != LayoutElement.TYPE_TEXT_VERTICAL) continue;
			for (LayoutElement r : le.elements) {
				hlist.add(r.height());
				r.rect.x = le.rect.x;
				r.rect.width = le.rect.width;
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
			Collections.sort(le.elements, new ImageUtility.RectComparator());
			LinkedList<LayoutElement> elems = new LinkedList<LayoutElement>(le.elements);
			Iterator<LayoutElement> itr = elems.iterator();
			LayoutElement r0 = null;
			LayoutElement r1 = itr.next();
			LayoutElement r2 = null;
			if (r1.y() < le.y() + avgHeight) {
				r1.rect.add(le.rect.getLocation());
			} else {
				// insert space character
				int height = Math.min(charHeight, r1.y() - le.y());
				Rectangle r = new Rectangle(le.x(), le.y(), le.width(), height);
				LayoutElement newElem = new LayoutElement(r);
				elems.addFirst(newElem);
				itr = elems.iterator();
				r1 = itr.next();
			}

			while (itr.hasNext()) {
				r2 = itr.next();

				boolean lastChar = false;
				boolean merged = false;
				do {
					merged = false;
					if (r1.height() < charHeight) {
						if (r2.y() - r1.getMaxY() > charHeight/2) break; // Too far to merge
						if (r2.getMaxY() - r1.y() <= charHeight+1) {
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

				if (r1.height() < charHeight) {
					int marginTop = 0, marginBottom = 0;
					if (r0 != null && r1.y() - r0.getMaxY() > 0) {
						marginTop = Math.max(0, (int)r0.getMaxY() - r1.y());
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

	static void alignVerticalTextInColumn(
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

	static boolean getLineElement(IplImage image_source,
			ArrayList<LayoutElement> group, boolean doAdjust, boolean drawResult) {
		double scale = 1;
		CvSize size_target = new CvSize((int)(image_source.width()*scale), (int)(image_source.height()*scale));
		IplImage image_binary = cvCreateImage( size_target, IPL_DEPTH_8U, 1);
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
		getRects(contours, rects);

		// Union intersected rects
		mergeRects(rects);

		// Remove small regions
		removeSmallRects(rects);

		// Draw rects
		Collections.sort(rects, new ImageUtility.RectComparator());
		if (drawResult) {
			//			drawRects(image_source, rects, 2, CvScalar.CYAN, CvScalar.GREEN);
		}

		// Grouping
		getLineElements(rects, group, image_source, doAdjust);

		storage.release();
		cvReleaseImage(image_binary);
		return true;
	}

	static void getLineElements(List<LayoutElement> rects, List<LayoutElement> group, IplImage image, boolean doAdjust) {
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

	static void drawGroup(IplImage image,
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

	static void drawRects(IplImage image,
			List<LayoutElement> rects, int lineWidth, CvScalar rectColor, CvScalar textColor) {
		CvFont font = new CvFont(CV_FONT_HERSHEY_SCRIPT_SIMPLEX, 0.3, 1);
		int i = 0;
		for (LayoutElement r : rects) {
			cvRectangle(image, cvPoint(r.x(), r.y()), 
					cvPoint(r.x() + r.width(), r.y() + r.height()),
					rectColor, lineWidth, 0, 0);
			if (textColor != null) {
				String text = Integer.toString(i++);
				cvPutText(image, text, cvPoint(r.x(), r.y()), font, textColor);
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
