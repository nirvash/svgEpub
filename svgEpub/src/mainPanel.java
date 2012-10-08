import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.batik.swing.JSVGCanvas;
import org.dyno.visual.swing.layouts.Bilateral;
import org.dyno.visual.swing.layouts.Constraints;
import org.dyno.visual.swing.layouts.GroupLayout;


//VS4E -- DO NOT REMOVE THIS LINE!
public class mainPanel extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JScrollPane jScrollPane1;
	private JSplitPane jSplitPane1;

	private JScrollPane jScrollPane0;
	private JPanel jPanel4;
	private CardLayout cardLayout;
	private ImagePanel imagePanel;
	private NavigableImagePanel naviImagePanel;
	private JSVGCanvas svgCanvas;

	private JPanel jPanel1;
	private JPanel jPanel2;
	private JList jList0;
	
	private JButton jButton0;
	private JButton jButton1;

	private JPanel jPanelNorth;
	private JButton jButton2;
	private static final String PREFERRED_LOOK_AND_FEEL = "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";

	public mainPanel() {
		initComponents();
	}

	private void initComponents() {
		setLayout(new GroupLayout());
		add(getJPanel1(), new Constraints(new Bilateral(0, 0, 0), new Bilateral(0, 0, 0)));
		setSize(640, 452);
	}

	private JSVGCanvas getSvgCanvas() {
		if (svgCanvas == null) {
			svgCanvas = new JSVGCanvas();
			
			svgCanvas.setEnableImageZoomInteractor(true);
			svgCanvas.setEnablePanInteractor(true);
			svgCanvas.setEnableZoomInteractor(false);
			svgCanvas.setEnableRotateInteractor(false);
		}
		return svgCanvas;
	}
	
	private JPanel getJPanel4() {
		if (jPanel4 == null) {
			jPanel4 = new JPanel();
			jPanel4.setLayout(getCardLayout());
			jPanel4.add(getJScrollPane0(), "html");
			jPanel4.add(getSvgCanvas(), "svg");
		}
		return jPanel4;
	}
	
	private CardLayout getCardLayout() {
		if (cardLayout == null) {
			cardLayout = new CardLayout();
		}
		return cardLayout;
	}

	private JScrollPane getJScrollPane0() {
		if (jScrollPane0 == null) {
			jScrollPane0 = new JScrollPane();
			jScrollPane0.setAutoscrolls(true);
//			jScrollPane0.setViewportView(getImagePanel());
//			getImagePanel().setParent(jScrollPane0);
			jScrollPane0.setViewportView(getNavigableImagePanel());
		}
		return jScrollPane0;
	}
	
	
	private NavigableImagePanel getNavigableImagePanel() {
		if (naviImagePanel == null) {
			naviImagePanel = new NavigableImagePanel();
		}
		return naviImagePanel;
	}
	
	private ImagePanel getImagePanel() {
		if (imagePanel == null) {
			imagePanel = new ImagePanel();
		}
		return imagePanel;
	}

	private JButton getJButton2() {
		if (jButton2 == null) {
			jButton2 = new JButton();
			jButton2.setText("Create EPUB");
			jButton2.setActionCommand("Create");
			jButton2.addActionListener(this);
		}
		return jButton2;
	}
	

	private JButton getJButton1() {
		if (jButton1 == null) {
			jButton1 = new JButton();
			jButton1.setText("Clear");
			jButton1.setActionCommand("Clear");
			jButton1.addActionListener(this);
		}
		return jButton1;
	}
	
	private JButton getJButton0() {
		if (jButton0 == null) {
			jButton0 = new JButton();
			jButton0.setText("Remove");
			jButton0.setActionCommand("Remove");
			jButton0.addActionListener(this);
		}
		return jButton0;
	}

	private JScrollPane getJScrollPane1() {
		if (jScrollPane1 == null) {
			jScrollPane1 = new JScrollPane();
			jScrollPane1.setViewportView(getJList0());
		}
		return jScrollPane1;
	}

	private JList getJList0() {
		if (jList0 == null) {
			DefaultListModel fileListModel = new DefaultListModel();
			jList0 = new JList(fileListModel);
		    jList0.setTransferHandler(new FileDropHandler(fileListModel));
		    jList0.setCellRenderer(new FileRenderer());
		    jList0.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		    jList0.setDropMode(DropMode.INSERT);
		    jList0.setDragEnabled(true);
		    jList0.setBackground(Color.WHITE);
		    jList0.setSelectionBackground(new Color(200, 200, 255));
		    jList0.addListSelectionListener(new ItemSelectionListener(getCardLayout(), getJPanel4(), getNavigableImagePanel(), getSvgCanvas(), fileListModel));
		    jList0.addMouseListener(new ListMouseListener());
		    
		}
		return jList0;
	}

	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jPanel1 = new JPanel();
			jPanel1.setLayout(new BorderLayout());
			jPanel1.add(getJPanelNorth(), BorderLayout.NORTH);
			jPanel1.add(getJSplitPane1(), BorderLayout.CENTER);
			jPanel1.add(getJPanel2(), BorderLayout.SOUTH);
		}
		return jPanel1;
	}
	
	private JPanel getJPanelNorth() {
		if (jPanelNorth == null) {
			jPanelNorth = new JPanel();
			jPanelNorth.setLayout(new BoxLayout(jPanelNorth, BoxLayout.X_AXIS));
			jPanelNorth.add(getJButton2());
		}
		return jPanelNorth;
	}

	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			jPanel2 = new JPanel();
			jPanel2.setLayout(new BoxLayout(jPanel2, BoxLayout.X_AXIS));
			jPanel2.add(getJButton0());
			jPanel2.add(getJButton1());
		}
		return jPanel2;
	}

	private JSplitPane getJSplitPane1() {
		if (jSplitPane1 == null) {
			jSplitPane1 = new JSplitPane();
			jSplitPane1.setDividerLocation(263);
			jSplitPane1.setDividerSize(5);
			jSplitPane1.setLeftComponent(getJScrollPane1());
			jSplitPane1.setRightComponent(getJPanel4());
		}
		return jSplitPane1;
	}

	private static void installLnF() {
		try {
			String lnfClassname = PREFERRED_LOOK_AND_FEEL;
			UIManager.setLookAndFeel(lnfClassname);
		} catch (Exception e) {
			System.err.println("Cannot install " + PREFERRED_LOOK_AND_FEEL
					+ " on this platform:" + e.getMessage());
		}
	}

	/**
	 * Main entry of the class.
	 * Note: This class is only created so that you can easily preview the result at runtime.
	 * It is not expected to be managed by the designer.
	 * You can modify it as you like.
	 */
	public static void main(String[] args) {
		installLnF();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				mainPanel frame = new mainPanel();
				frame.setDefaultCloseOperation(mainPanel.EXIT_ON_CLOSE);
				frame.setTitle("mainPanel");
				frame.getContentPane().setPreferredSize(frame.getSize());
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Remove")) {
			if (!jList0.isSelectionEmpty()) {
				int indices[] = jList0.getSelectedIndices();
				DefaultListModel model = (DefaultListModel)jList0.getModel();
				int numRemoved = 0;
				for (int index : indices) {
					model.remove(index - numRemoved++);
				}
			}
		} else if (e.getActionCommand().equals("Clear")) {
			DefaultListModel model = (DefaultListModel)jList0.getModel();
			model.clear();
		} else if (e.getActionCommand().equals("Create")) {
			DefaultListModel model = (DefaultListModel) jList0.getModel();
			@SuppressWarnings("unchecked")
			Enumeration<ListItem> elist = (Enumeration<ListItem>) model.elements();
			ArrayList<ListItem> list = (ArrayList<ListItem>) Collections.list(elist);

			SaveDialog dialog = new SaveDialog(this , "Save EPUB" , true);
			dialog.setLocationRelativeTo(this);
			dialog.setList(list);
			dialog.setVisible(true);
		}
	}
	
	
	static boolean canHandle(File file) {
		return isSvgFile(file) || isImageFile(file);
	}


	static boolean isSvgFile(File file) {
		return file.isFile() && file.canRead() && file.getPath().endsWith(".svg");
	}
	
	static boolean isImageFile(File file) {
		return file.isFile() && file.canRead() && 
				(file.getPath().endsWith(".jpg") ||
				 file.getPath().endsWith(".gif") ||
				 file.getPath().endsWith(".png"));
	}

	
}
