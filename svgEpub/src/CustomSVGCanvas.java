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
	private OnOutAction outAction = new OnOutAction();

	public class OnOverAction implements EventListener {
		@Override
		public void handleEvent(Event evt) {
			Cursor cur = new Cursor(Cursor.HAND_CURSOR);
			setCursor(cur);
		}
	}
	
	public class OnOutAction implements EventListener {
		@Override
		public void handleEvent(Event arg0) {
		}
		
	}

	public class OnUpAction implements EventListener {
		@Override
		public void handleEvent(Event ev) {
//			logger.info("onUP");
			Cursor cur = new Cursor(Cursor.DEFAULT_CURSOR);
			setCursor(cur);
			Document doc = getSVGDocument();
			EventTarget t = (EventTarget)doc.getDocumentElement();
//			EventTarget t = ev.getTarget();
			t.removeEventListener("mousemove", moveAction, false);
			t.removeEventListener("mouseup", upAction, false);
			
			SVGLocatable node = (SVGLocatable)ev.getTarget();
			clipRect = new Rectangle();
			clipRect.x = (int)node.getBBox().getX();
			clipRect.y = (int)node.getBBox().getY();
			clipRect.width = (int)node.getBBox().getWidth();
			clipRect.height = (int)node.getBBox().getHeight();
			
			mListItem.setClipRect(clipRect);
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
				Cursor cur = new Cursor(Cursor.MOVE_CURSOR);
				setCursor(cur);
				selectedItem.setAttribute("x",  "" + (dragpt.getX() - initialOffset.getX()));
				selectedItem.setAttribute("y",  "" + (dragpt.getY() - initialOffset.getY()));
				fill.setAttribute("x",  "" + (dragpt.getX() - initialOffset.getX() - lineWidthHalf));
				fill.setAttribute("y",  "" + (dragpt.getY() - initialOffset.getY() - lineWidthHalf));
			} else if (drag_mode == DRAG_RESIZE_BOTTOM_RIGHT) {
				Cursor cur = new Cursor(Cursor.SE_RESIZE_CURSOR);
				setCursor(cur);
				int width = (int) (initialRect.getWidth() + (dragpt.getX() - initialDragPoint.getX()));
				int height = (int) (initialRect.getHeight() + (dragpt.getY() - initialDragPoint.getY()));
				selectedItem.setAttribute("width", "" + width);
				selectedItem.setAttribute("height", "" + height);
				fill.setAttribute("width", "" + (width + lineWidthHalf*2));
				fill.setAttribute("height", "" + (height + lineWidthHalf*2));
			}
                                   			
			ev.stopPropagation();
		}
	}

	public class OnDownAction implements EventListener {
		@Override
		public void handleEvent(Event ev) {
			//logger.info("onDown");
			selectedItem = (Element)ev.getTarget();
			SVGLocatable thisNode = (SVGLocatable)ev.getTarget();
			DOMMouseEvent dme = (DOMMouseEvent)ev;
			int nowToX = dme.getClientX();
			int nowToY = dme.getClientY();

			SVGOMPoint pt = new SVGOMPoint(nowToX, nowToY);
			SVGMatrix mat = thisNode.getScreenCTM();
			mat = mat.inverse();
			initialDragPoint = (SVGOMPoint)pt.matrixTransform(mat);
			initialOffset = new SVGOMPoint(initialDragPoint.getX() - thisNode.getBBox().getX(), initialDragPoint.getY() - thisNode.getBBox().getY());
			initialRect = new Rectangle((int)thisNode.getBBox().getWidth(), (int)thisNode.getBBox().getHeight());
			
			
			if (initialOffset.getX() > thisNode.getBBox().getWidth() - 100 && 
				initialOffset.getY() > thisNode.getBBox().getHeight() - 100) {
				drag_mode = DRAG_RESIZE_BOTTOM_RIGHT;
				Cursor cur = new Cursor(Cursor.SE_RESIZE_CURSOR);
				setCursor(cur);
			} else {
				drag_mode = DRAG_MOVE;
				Cursor cur = new Cursor(Cursor.MOVE_CURSOR);
				setCursor(cur);
			}
			
			Document doc = getSVGDocument();
			EventTarget t = (EventTarget)doc.getDocumentElement();
			
//			EventTarget t = ev.getTarget();
			t.addEventListener("mousemove", moveAction, false);
			t.addEventListener("mouseup", upAction, false);
			ev.stopPropagation();
		}

	}

	private static final long serialVersionUID = 1L;
	private static final String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;

	protected Rectangle clipRect = null;

	protected Element selectedItem;
	protected Element fill;
	
	protected SVGOMPoint initialDragPoint;
	protected SVGOMPoint initialOffset;
	protected Rectangle initialRect;

	protected final int DRAG_MOVE = 0;
	protected final int DRAG_RESIZE_BOTTOM_RIGHT = 1;
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

	public void setImage(File file) {
		Rectangle imageRect = ImageUtil.getImageSize(file);
		Rectangle clipRect = mListItem.getClipRect();
		if (clipRect == null) {
			clipRect = imageRect;
		}
		Document doc = createDocument(clipRect, imageRect, file.toURI().toString());
		super.setDocument(doc);
	}
	
	@Override
	public void setURI(String newURI) {
		if (newURI == null) {
			super.setURI(null);
			return;
		}
		
		Rectangle svgRect = ImageUtil.getSvgSize(newURI);
		Rectangle clipRect = mListItem.getClipRect();
		if (clipRect == null) {
			clipRect = svgRect;
		}
		Document doc = createDocument(clipRect, svgRect, newURI);
		super.setDocument(doc);
	}
	
	private Document createDocument(Rectangle clipRect, Rectangle imageRect, String imageURI) {
		Document doc = ImageUtil.createSvgDocument(clipRect, imageRect, imageURI, mPreview);
		if (mPreview) return doc;
		
		Element svgRootOuter = doc.getDocumentElement();
		Element g = doc.createElementNS(svgNS, "g");
		g.setAttributeNS(null, "x", Integer.toString(clipRect.x));
		g.setAttributeNS(null, "y", Integer.toString(clipRect.y));
		g.setAttributeNS(null, "width", Integer.toString(clipRect.width));
		g.setAttributeNS(null, "height", Integer.toString(clipRect.height));
		g.setAttributeNS(null, "pointer-events", "all");
		
		Element rect = doc.createElementNS(svgNS, "rect");
		rect.setAttribute("id", "clipRegion");
		rect.setAttributeNS(null, "x", Integer.toString(clipRect.x));
		rect.setAttributeNS(null, "y", Integer.toString(clipRect.y));
		rect.setAttributeNS(null, "width", Integer.toString(clipRect.width));
		rect.setAttributeNS(null, "height", Integer.toString(clipRect.height));
		rect.setAttributeNS(null, "pointer-events", "fill");
		rect.setAttributeNS(null, "vector-effect", "non-scaling-stroke");
		rect.setAttributeNS(null, "style", "fill:none;stroke:black; stroke-opacity:1.0; stroke-dasharray: 5 2;");
		
		fill = doc.createElementNS(svgNS, "rect");
		int lineWidthHalf = 20 / 2;
		fill.setAttributeNS(null, "x", Integer.toString(clipRect.x - lineWidthHalf));
		fill.setAttributeNS(null, "y", Integer.toString(clipRect.y - lineWidthHalf));
		fill.setAttributeNS(null, "width", Integer.toString(clipRect.width + lineWidthHalf*2));
		fill.setAttributeNS(null, "height", Integer.toString(clipRect.height + lineWidthHalf*2));
		fill.setAttributeNS(null, "pointer-events", "none");
		fill.setAttributeNS(null, "vector-effect", "non-scaling-stroke");
		fill.setAttributeNS(null, "style", "fill:none; stroke:red; stroke-opacity:0.6; stroke-width: 20;");
		
		((EventTarget) g).addEventListener("mouseover", overAction, false);
		((EventTarget) g).addEventListener("mousedown", downAction, false);

		svgRootOuter.appendChild(g);
		g.appendChild(fill);
		g.appendChild(rect);
		return doc;
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







}



	
