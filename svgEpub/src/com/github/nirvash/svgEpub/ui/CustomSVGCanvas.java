package com.github.nirvash.svgEpub.ui;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.batik.dom.events.DOMMouseEvent;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.dom.svg.SVGOMPoint;
import org.apache.batik.dom.util.XLinkSupport;
import org.apache.batik.ext.awt.image.spi.ImageTagRegistry;
import org.apache.batik.script.rhino.RhinoInterpreter.ArgumentsBuilder;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.AbstractPanInteractor;
import org.apache.batik.swing.svg.SVGUserAgent;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.svg.GetSVGDocument;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGLocatable;
import org.w3c.dom.svg.SVGMatrix;
import org.w3c.dom.svg.SVGRect;

import com.github.nirvash.svgEpub.list.IFile;
import com.github.nirvash.svgEpub.list.ListItem;
import com.github.nirvash.svgEpub.util.ImageUtility;


public class CustomSVGCanvas extends JSVGCanvas  {
	public class OnClickAction implements EventListener {

		@Override
		public void handleEvent(Event evt) {
			// TODO Auto-generated method stub

		}

	}
	
	private OnUpAction upAction = new OnUpAction();
	private OnMoveAction moveAction = new OnMoveAction();
	private OnDownAction downAction = new OnDownAction();
	private OnOverAction overAction = new OnOverAction();
	

	public class OnOverAction implements EventListener {
		@Override
		public void handleEvent(Event evt) {
			Element e = (Element)evt.getTarget();
			String id = e.getAttribute("id");
			int cursorType = Cursor.MOVE_CURSOR;
			if (id.startsWith("handle")) {
				int index = Integer.parseInt(id.replace("handle", ""));
				cursorType = cursorTable[index];
			}
			Cursor cur = new Cursor(cursorType);
			setCursor(cur);
		}
	}
	
	public class OnUpAction implements EventListener {
		@Override
		public void handleEvent(Event ev) {
//			logger.info("onUP");
			Document doc = getSVGDocument();
//			EventTarget t = (EventTarget)doc.getElementById("root");
			EventTarget t = (EventTarget)hitRegion;
			t.removeEventListener("mousemove", moveAction, false);
			t.removeEventListener("mouseup", upAction, false);
			hitRegion.setAttributeNS(null, "pointer-events", "none");
			
			SVGLocatable node = (SVGLocatable)rabberBand;
			clipRect.x = (int)node.getBBox().getX();
			clipRect.y = (int)node.getBBox().getY();
			clipRect.width = (int)node.getBBox().getWidth();
			clipRect.height = (int)node.getBBox().getHeight();
			
			mListItem.setClipRect((Rectangle)clipRect.clone());
		}
	}

	public class OnMoveAction implements EventListener {
		@Override
		public void handleEvent(Event ev) {
			//logger.info("onMove");
			DOMMouseEvent dme = (DOMMouseEvent)ev;
			int nowToX = dme.getClientX();
			int nowToY = dme.getClientY();
			SVGOMPoint pt = new SVGOMPoint(nowToX, nowToY);
			SVGMatrix mat = ((SVGLocatable)selectedItem).getScreenCTM();
			mat = mat.inverse();
			SVGOMPoint dragpt = (SVGOMPoint)pt.matrixTransform(mat);
			int lineWidthHalf = 20 / 2;
		
			if (drag_mode == DRAG_MOVE) {
				int x = (int) (dragpt.getX() - initialOffset.getX());
				int y = (int) (dragpt.getY() - initialOffset.getY());
				Cursor cur = new Cursor(Cursor.MOVE_CURSOR);
				setCursor(cur);
				clipRect.x = x;
				clipRect.y = y;
				rabberBand.setAttribute("x",  "" + clipRect.x);
				rabberBand.setAttribute("y",  "" + clipRect.y);
				border.setAttribute("x",  "" + (clipRect.x - lineWidthHalf));
				border.setAttribute("y",  "" + (clipRect.y - lineWidthHalf));
				for (int i=0; i<4; i++) {
					grips[i].setAttributeNS(null, "x", Integer.toString(getGripPos(i, 0, clipRect.x, handleSize, clipRect.width)));
					grips[i].setAttributeNS(null, "y", Integer.toString(getGripPos(i, 1, clipRect.y, handleSize, clipRect.height)));
				}
			} else {
				int x = (int) (dragpt.getX() - initialOffset.getX());
				int y = (int) (dragpt.getY() - initialOffset.getY());
				int diffX = (int) (initialDragPoint.getX() - dragpt.getX());
				int diffY = (int) (initialDragPoint.getY() - dragpt.getY());
				Cursor cur = new Cursor(cursorTable[drag_mode]);
				setCursor(cur);
				switch (drag_mode) {
					case DRAG_RESIZE_UL: {
						int w = (int) (initialRect.getWidth() + diffX);
						if (w >= 0) {
							clipRect.x = x;
							clipRect.width = w;
						}
						int h = (int) (initialRect.getHeight() + diffY);
						if (h >= 0) {
							clipRect.y = y;
							clipRect.height = h;
						}
						break;
					}
					case DRAG_RESIZE_UR: {
						clipRect.width = (int) (initialRect.getWidth() - diffX);
						int h = (int) (initialRect.getHeight() + diffY);
						if (h >= 0) {
							clipRect.y = y;
							clipRect.height = h;
						}
						break;
					}
					case DRAG_RESIZE_BL: {
						int w = (int) (initialRect.getWidth() + diffX);
						if (w >= 0) {
							clipRect.x = x;
							clipRect.width = w;
						}
						clipRect.height = (int) (initialRect.getHeight() - diffY);
						break;
					}
					case DRAG_RESIZE_BR:
						clipRect.width = (int) (initialRect.getWidth() - diffX);
						clipRect.height = (int) (initialRect.getHeight() - diffY);
						break;
				};
				clipRect.width = Math.max(0, clipRect.width);
				clipRect.height = Math.max(0, clipRect.height);
				
				rabberBand.setAttribute("x",  "" + clipRect.x);
				rabberBand.setAttribute("y",  "" + clipRect.y);
				border.setAttribute("x",  "" + (clipRect.x - lineWidthHalf*scale));
				border.setAttribute("y",  "" + (clipRect.y - lineWidthHalf*scale));
				rabberBand.setAttribute("width", "" + clipRect.width);
				rabberBand.setAttribute("height", "" + clipRect.height);
				border.setAttribute("width", "" + (clipRect.width + lineWidthHalf*2*scale));
				border.setAttribute("height", "" + (clipRect.height + lineWidthHalf*2*scale));
				
				for (int i=0; i<4; i++) {
					grips[i].setAttributeNS(null, "x", Integer.toString(getGripPos(i, 0, clipRect.x, handleSize, clipRect.width)));
					grips[i].setAttributeNS(null, "y", Integer.toString(getGripPos(i, 1, clipRect.y, handleSize, clipRect.height)));
				}
			}
                                   			
			ev.stopPropagation();
		}
	}

	public class OnDownAction implements EventListener {
		@Override
		public void handleEvent(Event ev) {
			//logger.info("onDown");
			selectedItem = (Element)ev.getTarget();
//			SVGLocatable thisNode = (SVGLocatable)ev.getTarget();
			SVGLocatable thisNode = (SVGLocatable)rabberBand;
			DOMMouseEvent dme = (DOMMouseEvent)ev;
			int nowToX = dme.getClientX();
			int nowToY = dme.getClientY();

			SVGOMPoint pt = new SVGOMPoint(nowToX, nowToY);
			SVGMatrix mat = thisNode.getScreenCTM();
			mat = mat.inverse();
			initialDragPoint = (SVGOMPoint)pt.matrixTransform(mat);
			initialOffset = new SVGOMPoint(initialDragPoint.getX() - thisNode.getBBox().getX(), initialDragPoint.getY() - thisNode.getBBox().getY());
			initialRect = (Rectangle)clipRect.clone();

			Element target = (Element)ev.getTarget();
			if (target.getAttribute("id").startsWith("handle")) {
				int id = Integer.parseInt(target.getAttribute("id").replace("handle", ""));
				drag_mode = DRAG_RESIZE_BASE + id;
				Cursor cur = new Cursor(cursorTable[id]);
				setCursor(cur);
			} else {
				drag_mode = DRAG_MOVE;
				Cursor cur = new Cursor(Cursor.MOVE_CURSOR);
				setCursor(cur);
			}
			
			Document doc = getSVGDocument();
//			EventTarget t = (EventTarget)doc.getElementById("root");
			EventTarget t = (EventTarget)hitRegion;
			hitRegion.setAttributeNS(null, "pointer-events", "fill");
			t.addEventListener("mousemove", moveAction, false);
			t.addEventListener("mouseup", upAction, false);
			ev.stopPropagation();
		}

	}

	private static final long serialVersionUID = 1L;
	private static final String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;

	protected Rectangle clipRect = new Rectangle();

	protected Element selectedItem;
	protected Element rabberBand;
	protected Element hitRegion;
	protected Element border;
	protected Element[] grips = new Element[4];
	
	protected SVGOMPoint initialDragPoint;
	protected SVGOMPoint initialOffset;
	protected Rectangle initialRect;
	protected int handleSize = 100;
	protected float scale = 1.0f;


	protected final int DRAG_MOVE = 10;
	protected final int DRAG_RESIZE_BASE = 0;
	protected final int DRAG_RESIZE_UL = DRAG_RESIZE_BASE + 0;
	protected final int DRAG_RESIZE_UR = DRAG_RESIZE_BASE + 1;
	protected final int DRAG_RESIZE_BL = DRAG_RESIZE_BASE + 2;
	protected final int DRAG_RESIZE_BR = DRAG_RESIZE_BASE + 3;
	protected final int[] cursorTable = new int[] {
			Cursor.NW_RESIZE_CURSOR, Cursor.NE_RESIZE_CURSOR, Cursor.SW_RESIZE_CURSOR, Cursor.SE_RESIZE_CURSOR
	};
	protected int drag_mode = DRAG_MOVE;
	
	final Logger logger = Logger.getLogger("SampleLogging");
	private ListItem mListItem = null;
	private boolean mPreview;
	
	public CustomSVGCanvas() {
		super();
		initComponent();
	}

	public CustomSVGCanvas(SVGUserAgent ua, boolean eventsEnabled,
			boolean selectableText) {
		super(ua, eventsEnabled, selectableText);
		initComponent();
	}
	
	@SuppressWarnings("unchecked")
	private void initComponent() {
		setEnableImageZoomInteractor(false);
		setEnablePanInteractor(true);
		setEnableZoomInteractor(false);
		setEnableRotateInteractor(false);
		addMouseWheelListener(new WheelZooming());
		setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
		setDoubleBuffered(true);
		this.setEnableResetTransformInteractor(true);
		
		getInteractors().add(new AbstractPanInteractor() {
	    	public boolean startInteraction(InputEvent ie) {
	    		int mods = ie.getModifiers();
	            return ie.getID() == MouseEvent.MOUSE_PRESSED && (mods & InputEvent.BUTTON3_MASK) != 0;
	    	}
		});
		/*

		svgCanvas.getInteractors().add(new AbstractImageZoomInteractor() {
			public boolean startInteraction(InputEvent ie) {
				int mods = ie.getModifiers();
				return ie.getID() == MouseEvent.MOUSE_PRESSED && (mods & InputEvent.BUTTON1_MASK) != 0;
			}
		});
*/
	}

	public void setImage(IFile item) {
		Rectangle imageRect = ImageUtility.getImageSize(item);
		Rectangle clipRect = mListItem.getClipRect();
		if (clipRect == null) {
			clipRect = imageRect;
		}

		ImageTagRegistry.getRegistry().flushCache();
		if (this.getSVGDocument() != null) {
			this.flushImageCache();
			this.setDocument(null);
		}
		
		Document doc = createDocument(clipRect, imageRect, item);
		super.setDocument(doc);
	}
	
	public void setSvg(IFile item) {
		Rectangle svgRect = ImageUtility.getSvgSize(item);
		Rectangle clipRect = mListItem.getClipRect();
		if (clipRect == null) {
			clipRect = svgRect;
		}
		
		ImageTagRegistry.getRegistry().flushCache();
		if (this.getSVGDocument() != null) {
			this.flushImageCache();
			this.setDocument(null);
		}

		Document doc = createDocument(clipRect, svgRect, item);
		super.setDocument(doc);
	}
	
	private Document createDocument(Rectangle clipRect, Rectangle imageRect, IFile item) {
		String imageURI = item.getURI();
		this.clipRect.setBounds(clipRect);
		scale = (float)imageRect.width / 1600;
		int margin = (int)(100 * scale);
		Document doc = ImageUtility.createSvgDocument(clipRect, imageRect, imageURI, mPreview, margin);
		
		Element svgRootOuter = doc.getElementById("root");

		if (mPreview) return doc;
		float scaleX = mPreview ? (float)clipRect.width / imageRect.width : 1.0f;
		float scaleY = mPreview ? (float)clipRect.height / imageRect.height : 1.0f;
		Rectangle bgRect = mPreview ? new Rectangle(imageRect.x, imageRect.y, clipRect.width, clipRect.height) : imageRect;
		Element bg = doc.createElementNS(svgNS, "rect");
		bg.setAttributeNS(null, "x", Float.toString(bgRect.x - margin/2*scaleX));
		bg.setAttributeNS(null, "y", Float.toString(bgRect.y - margin/2*scaleY));
		bg.setAttributeNS(null, "width", Float.toString(bgRect.width + margin*scaleX));
		bg.setAttributeNS(null, "height", Float.toString(bgRect.height + margin*scaleY));
		bg.setAttributeNS(null, "pointer-events", "none");
		bg.setAttributeNS(null, "style", String.format("fill:none; stroke:silver; stroke-width:%f;", margin*scaleX));
		svgRootOuter.insertBefore(bg, svgRootOuter.getFirstChild());

/*
		Element symbol = doc.createElementNS(svgNS, "symbol");
		symbol.setAttribute("id", "rabberBand");
		symbol.setAttributeNS(null, "viewBox", String.format("%d %d %d %d", 
				-symbolMargin, -symbolMargin, clipRect.width + symbolMargin*2, clipRect.height+symbolMargin*2));
		symbol.setAttributeNS(null, "pointer-events", "all");
*/
		rabberBand = doc.createElementNS(svgNS, "rect");
		rabberBand.setAttribute("id", "clipRegion");
		rabberBand.setAttributeNS(null, "x", Integer.toString(clipRect.x));
		rabberBand.setAttributeNS(null, "y", Integer.toString(clipRect.y));
		rabberBand.setAttributeNS(null, "width", Integer.toString(clipRect.width));
		rabberBand.setAttributeNS(null, "height", Integer.toString(clipRect.height));
		rabberBand.setAttributeNS(null, "pointer-events", "fill");
		rabberBand.setAttributeNS(null, "style", "fill:none;stroke:black; stroke-opacity:1.0; stroke-dasharray: 5 2;");
		
		border = doc.createElementNS(svgNS, "rect");
		int lineWidthHalf = (int)(20 / 2 * scale);
		border.setAttributeNS(null, "x", Integer.toString(clipRect.x-lineWidthHalf));
		border.setAttributeNS(null, "y", Integer.toString(clipRect.y-lineWidthHalf));
		border.setAttributeNS(null, "width", Integer.toString(clipRect.width + lineWidthHalf*2));
		border.setAttributeNS(null, "height", Integer.toString(clipRect.height + lineWidthHalf*2));
		border.setAttributeNS(null, "pointer-events", "none");
		border.setAttributeNS(null, "style", String.format("fill:none; stroke:red; stroke-opacity:0.6; stroke-width: %d;", lineWidthHalf*2));
		
		for (int i=0; i<4; i++) {
			handleSize = (int)(80 * scale);
			Element handle = doc.createElementNS(svgNS, "rect");
			handle.setAttribute("id", String.format("handle%d", i));
			handle.setAttributeNS(null, "x", Integer.toString(getGripPos(i, 0, clipRect.x, handleSize, clipRect.width)));
			handle.setAttributeNS(null, "y", Integer.toString(getGripPos(i, 1, clipRect.y, handleSize, clipRect.height)));
			handle.setAttributeNS(null, "width", Integer.toString(handleSize));
			handle.setAttributeNS(null, "height", Integer.toString(handleSize));
			handle.setAttributeNS(null, "pointer-events", "fill");
			handle.setAttributeNS(null, "style", "fill:blue; stroke:black; stroke-width: 1;");
			svgRootOuter.appendChild(handle);
			((EventTarget) handle).addEventListener("mousemove", overAction, false);
			((EventTarget) handle).addEventListener("mousedown", downAction, false);
			grips[i] = handle;
		}

		svgRootOuter.appendChild(border);
		svgRootOuter.appendChild(rabberBand);

//		svgRootOuter.appendChild(symbol);
		/*
		rabberBand = doc.createElementNS(svgNS, "use");
		XLinkSupport.setXLinkHref(rabberBand, "#rabberBand");
		rabberBand.setAttributeNS(null, "x", Integer.toString(clipRect.x - symbolMargin));
		rabberBand.setAttributeNS(null, "y", Integer.toString(clipRect.y - symbolMargin));
		rabberBand.setAttributeNS(null, "width", Integer.toString(clipRect.width/2 + symbolMargin*2));
		rabberBand.setAttributeNS(null, "height", Integer.toString(clipRect.height/2 + symbolMargin*2));
*/
		hitRegion = doc.createElementNS(svgNS, "rect");
		hitRegion.setAttribute("id", "hitRegion");
		hitRegion.setAttributeNS(null, "x", Integer.toString(-3000));
		hitRegion.setAttributeNS(null, "y", Integer.toString(-3000));
		hitRegion.setAttributeNS(null, "width", Integer.toString(20000));
		hitRegion.setAttributeNS(null, "height", Integer.toString(20000));
		hitRegion.setAttributeNS(null, "pointer-events", "none");
		hitRegion.setAttributeNS(null, "style", "fill:none;stroke:none;");
		doc.getDocumentElement().appendChild(hitRegion);


		((EventTarget) rabberBand).addEventListener("mousemove", overAction, false);
		((EventTarget) rabberBand).addEventListener("mousedown", downAction, false);

		return doc;
	}

	
	private int getGripPos(int index, int j, int offset, int handleWidth, int width) {
		switch (index) {
		case 0:
			return offset-handleWidth;
		case 1:
			if (j==0) return offset+width;
			return offset-handleWidth;
		case 2:
			if (j==0) return offset-handleWidth;
			return offset+width;
		case 3:
		default:
			return offset+width;
		}
	}


	private class WheelZooming implements MouseWheelListener {
		boolean isFinished=true;
		TimerTask task;
		private Timer timer = new Timer();

		void updateWheelTransform(){
			if(!isFinished){
		        AffineTransform pt =  getPaintingTransform();
		        if (pt != null) {
		            AffineTransform rt = (AffineTransform)getRenderingTransform().clone();
		            rt.preConcatenate(pt);
		            setRenderingTransform(rt);
		        }
		        isFinished =true;
		        task=null;
			}
		}
		
		void taskStart(){
			if(task!=null)task.cancel();
			task=new TimerTask(){
				@Override
				public void run() {
					updateWheelTransform();
				}
			};
			timer.schedule(task, 500);
		}
		
		public void mouseWheelMoved(MouseWheelEvent ev) {
			try{
				double cx = ev.getX();
				double cy = ev.getY();
	            AffineTransform at = AffineTransform.getTranslateInstance(cx,cy);
	            if (ev.getWheelRotation() < 0) {
	            	if((at.getScaleX()*1.15) < Double.MAX_VALUE){
	    				at.scale(1.15, 1.15);
	    				at.translate(-cx,-cy);
	            	}
				} else {
	            	if ((at.getScaleX()*0.85) > Double.MIN_VALUE){
	            		at.scale(0.85, 0.85);
	            		at.translate(-cx,-cy);
	            	}
				}
				if (isFinished) {
					setPaintingTransform(null);
					isFinished=false;
				} else {
					at.concatenate(getPaintingTransform());
				}
				setPaintingTransform(at);
				taskStart();
			} catch (NullPointerException ne){}
		}
	}


	public void setListItem(ListItem item) {
		mListItem  = item;
	}

	public void setPreview(boolean preview) {
		mPreview = preview;
	}

	public void finish() {
		ImageTagRegistry.getRegistry().flushCache();
		if (this.getSVGDocument() != null) {
			this.setVisible(false);
			this.flushImageCache();
			this.setDocument(null);
			this.setSVGDocument(null);
		}
	}







}



	
