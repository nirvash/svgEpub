package com.github.nirvash.svgEpub.layout;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;


import java.awt.Point;
import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cern.colt.list.DoubleArrayList;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_highgui;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;

public class LayoutElement {
		public static final int TYPE_UNKNOWN = 0;
		public static final int TYPE_IMAGE = 1;
		public static final int TYPE_TEXT_VERTICAL = 2;
		public static final int TYPE_TEXT_HORIZONTAL = 3;
		public static final int TYPE_RUBY = 4;
		public static final int TYPE_CHARACTER = 5;
		public static final int TYPE_LF = 6;

		int type = TYPE_UNKNOWN;
		int id = 0;
		public Rectangle rect;
		List<LayoutElement> elements = new ArrayList<LayoutElement>();
		int codePoint = 0;
		String text = "";
		boolean hasLF = false;

		public LayoutElement(int id) {
			this.id = id;;
		}
		
		public LayoutElement(int id, int type) {
			this.id = id;;
			this.type = type;
		}
		
		public LayoutElement(Rectangle rect) {
			this.rect = rect;
		}

		public double calcAngle(IplImage image) {
			CvMemStorage storage = cvCreateMemStorage(0);
			CvSeq points = cvCreateSeq(CV_SEQ_ELTYPE_POINT, Loader.sizeof(CvSeq.class), Loader.sizeof(CvPoint.class), storage);
			for (LayoutElement r : elements) {
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

		private double getCenterX() {
			return this.rect.getCenterX();
		}

		private double getCenterY() {
			return this.rect.getCenterY();
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
		
		public void addChild(LayoutElement child) {
			if (rect == null) {
				rect = new Rectangle(child.rect);
			} else {
				rect.add(child.rect);
			}
			elements.add(child);
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
			elements.add(new LayoutElement(new Rectangle(rectangle)));
		}

		public void adjustElementWidth() {
			if (rect.height < rect.width*3) return;
			
			for (LayoutElement r : elements) {
				r.rect.x = rect.x;
				r.rect.width = rect.width;
			}
			
			LayoutAnalyzer.mergeRects(elements);
		}

		public List<LayoutElement> extractRuby() {
			if (rect.height < rect.width*3) return null;
			DoubleArrayList rightEdges = new DoubleArrayList();
			for (int i=0; i<elements.size(); i++) {
				LayoutElement ch1 = elements.get(i);
				if (ch1.width() < rect.width * 0.3 && ch1.getMaxX() < (rect.x + rect.width * 0.7)) continue;
				boolean isMostRight = true;
				for (int j=i+1; j<elements.size(); j++) {
					LayoutElement ch2 = elements.get(j);
					if (ch2.getMaxX() <= ch1.getMaxX()) continue;
					if (ch2.y() > ch1.getMaxY()) continue;
					if (ch2.getMaxY() < ch1.y()) continue;
					isMostRight = false;
					break;
				}
				if (isMostRight) {
					rightEdges.add(ch1.getMaxX()-rect.x);
				}
			}

			if (rightEdges.size() > 5) {
				int removeMinErrorIndex = (int)(rightEdges.size()*0.07);
				rightEdges.sort();
				rightEdges.removeFromTo(0, removeMinErrorIndex);
			}
			
			double[] avgList = new double[2];
			double limit = 2;
			double th = LayoutAnalyzer.calcThreshold(rightEdges, 1, avgList, limit, true);
			if (th == 0) return null;
			if (avgList[1]-avgList[0] < rect.width*0.2) return null;
			if (rect.width - avgList[0] < rect.width*0.3) return null;
			
			ArrayList<LayoutElement> rubyList = new ArrayList<LayoutElement>();
			for (int i=elements.size()-1; i>=0; i--) {
				LayoutElement ch1 = elements.get(i);
				if (ch1.getMaxX() >= rect.x + avgList[0] + this.rect.width * 0.1) {
					if (ch1.x() >= rect.x + avgList[0]) {
						ch1.setType(LayoutElement.TYPE_RUBY);
						rubyList.add(ch1);
						elements.remove(i);
					} else {
						int rubyWidth = (int)(ch1.getMaxX()-(rect.x+avgList[0]));
						if (ch1.rect.width - rubyWidth < rect.width * 0.1) {
							ch1.setType(LayoutElement.TYPE_RUBY);
							rubyList.add(ch1);
							elements.remove(i);
						} else {
							LayoutElement rubyCh = new LayoutElement(new Rectangle());
							rubyCh.rect.x = rect.x + (int)avgList[0];
							rubyCh.rect.y = ch1.rect.y;
							rubyCh.rect.width = rubyWidth;
							rubyCh.rect.height = ch1.rect.height;
							rubyCh.setType(LayoutElement.TYPE_RUBY);
							rubyList.add(rubyCh);
							ch1.rect.width -= rubyWidth;
						}
					}
				}
			}

			if (rubyList.isEmpty()) return null;
			if (elements.isEmpty()) {
				elements = rubyList;
				return null;
			}
			
			calcBoundsRect();
			return rubyList;
		}
		
		public LayoutElement extractRubyOld() {
			if (elements.size() < 5) return null;
			if (rect.height < rect.width*3) return null;
			
			ArrayList<LayoutElement> list = new ArrayList<LayoutElement>();
			double height = rect.width * 6;
			int split = (int)rect.height / (int)height;
			height += (rect.height % height) / split;
			
			for (int y=0; y + height < rect.height; y+=height/2) {
				ArrayList<LayoutElement> sublist = new ArrayList<LayoutElement>();
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
				if (maxHeight < list.get(i).height()) {
					maxHeight = list.get(i).height();
					bodyIndex = i;
				}
			}
			list.remove(bodyIndex);
			LayoutElement ruby = new LayoutElement(0);
			ruby.setType(LayoutElement.TYPE_RUBY);
			
			for (LayoutElement rubyRect : list) {
				for (int i=elements.size()-1; i>=0; i--) {
					LayoutElement r = elements.get(i);
					if (!rubyRect.intersects(r)) continue;
					if (rubyRect.x() <= r.getCenterX() && r.getCenterX() <= rubyRect.getMaxX()) {
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
		
		public int width() {
			return rect.width;
		}

		public int height() {
			return rect.height;
		}

		public boolean intersects(LayoutElement r) {
			return rect.intersects(r.rect);
		}

		private void getSubGroup(int top, double bottom, ArrayList<LayoutElement> sublist) {
			for (LayoutElement r1 : elements) {
				if (!(top <= r1.getCenterY() && r1.getCenterY() < bottom)) continue;
				
				boolean found = false;
				for (int i=sublist.size()-1; i>=0; i--) {
					LayoutElement r2 = sublist.get(i);
					if (isInside(r2.x(), r2.getMaxX(), r1.getCenterX(), 0.9f)) {
						r2.add(r1);
						found = true;
						break;
					}
				}
				if (!found) {
					LayoutElement r3 = new LayoutElement(r1.rect); 
					sublist.add(r3);
					for (LayoutElement r4 : elements) {
						if (!(top <= r4.getCenterY() && r4.getCenterY() < bottom)) continue;
						if (isInside(r3.x(), r3.getMaxX(), r4.getCenterX(), 0.9f)) {
							r3.add(r4);
						} else if (isInside(r4.x(), r4.getMaxX(), r3.getCenterX(), 0.9f)) {
							r3.add(r4);
						}
					}					
				}
			}
		}

		public double getMaxX() {
			return rect.getMaxX();
		}

		public double getMaxY() {
			return rect.getMaxY();
		}

		public int x() {
			return rect.x;
		}

		public int y() {
			return rect.y;
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

		private static void mergeRects2(ArrayList<LayoutElement> rects) {
			for (int i=rects.size()-1; i >= 0 ; i--) {
				LayoutElement r1 = rects.get(i);
				for (int j=0; j<i; j++) {
					LayoutElement r2 = rects.get(j);
					boolean isOverwrap = false;
					isOverwrap |= isInside(r1.x(), r1.getMaxX(), r2.getCenterX(), 0.7f);
					isOverwrap |= isInside(r2.x(), r2.getMaxX(), r1.getCenterX(), 0.7f);
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
				LayoutElement le = elements.get(i);
				double ss = getStandardScore(le.getCenterX(), avg, std);
				if (ss > 60) {
					ruby.add(le);
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

		public void calcBoundsRect() {
			this.rect = null;
			for (LayoutElement le : elements) {
				if (rect == null) {
					this.rect = new Rectangle(le.rect);
				} else {
					this.rect.add(le.rect);
				}
			}
		}

		private double getStandartDeviation(List<LayoutElement> list, double avg) {
			double variance = 0;
			for (LayoutElement le : list) {
				variance += Math.pow(le.getCenterX() - avg,  2);
			}
			double uv = variance / (list.size()-1);
			return Math.sqrt(uv);
		}

		private double getStandardScore(double x, double avg, double std) {
			return (50 + 10 * (x - avg) / std);
		}

		private double getAverate(List<LayoutElement> list) {
			double sum = 0.0f;
			for (LayoutElement le : list) {
				sum += le.getCenterX();
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

		double[] vector = null;
		public void createVector(IplImage image, double scale, int width, int height) {
			vector = new double[height];
			CvRect roi = LayoutAnalyzer.toCvRect(rect, scale);
			cvSetImageROI(image, roi);
			CvSize targetSize = new CvSize(width, height);
			IplImage image_dst = cvCreateImage( targetSize, IPL_DEPTH_8U, 1);
			cvResize(image, image_dst, CV_INTER_LINEAR);
			ByteBuffer buf = image_dst.getByteBuffer();
			for (int y=0; y<height; y++) {
				double v = 0.0f;
				for (int x=0; x<width; x++) {
					v += ((buf.get(y*image_dst.widthStep()+x) & 0xFF) / 255) << x;
				}
				vector[y] = v;
			}
//			cvSaveImage(UUID.randomUUID()+".png", image_dst);
			cvReleaseImage(image_dst);
		}
		
		public double[] getVector() {
			return vector;
		}
		
		public void setText(String text) {
			this.text = text;
		}
		
		public String getText() {
			return text;
		}

		public void setCodePoint(int codePoint) {
			this.codePoint = codePoint;	
		}
		
		public int getCodePoint() {
			return this.codePoint;
		}

		public List<LayoutElement> getChildren() {
			return this.elements;
		}

		public void setLF(boolean hasLF) {
			this.hasLF = hasLF;
		}

		public boolean hasLF() {
			return hasLF;
		}
	}