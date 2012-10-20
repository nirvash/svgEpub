import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

//VS4E -- DO NOT REMOVE THIS LINE!
public class mainPanel extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JScrollPane jScrollPane1;
	private JSplitPane jSplitPane1;

	private JScrollPane jScrollPane0;
	private JPanel jPanel4;
	private CardLayout cardLayout;
	private NavigableImagePanel naviImagePanel;
	private CustomSVGCanvas svgCanvas;

	private JPanel jPanel1;
	private JPanel jPanel2;
	private JPanel jPanel5;
	private JList jList0;
	
	private JButton jButtonRemove;
	private JButton jButtonClear;

	private JPanel jPanelNorth;
	private JButton jButtonCreateEpub;
	private JCheckBox jCheckBoxPreview;
	
	private SaveDialog saveDialog;
	private ConfigDialog configDialog;
	
	private ItemSelectionListener itemSelectionListener;
	
	private static CustomProperties properties = new CustomProperties();
	private JButton jButtonConfig;
	private JButton jButtonResetClip;
	private JPanel jPanel0;
	
	private JPopupMenu jListPopupMenu;
	private Rectangle copyClipRectangle = null;
	
	private JButton jButtonAutoClip;
	private JButton jButtonClipTemplate;
	private static final String PREFERRED_LOOK_AND_FEEL = "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";
	static public CustomProperties getProperty() {
		return properties;
	}
	
	public mainPanel() {
		initComponents();
	}

	private void initComponents() {
		try {
			File config = new File("config.xml");
			if (config.exists()) {
				properties.loadFromXML(new FileInputStream(config));
			} else {
				properties.loadFromXML(mainPanel.class.getResourceAsStream("config.xml"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Epub.setProperty(properties);
		add(getJPanel1());
		setSize(640, 452);
		
		ImageUtil.initialize(properties.getProperty("enable_opencv", "no").equals("yes"));		
		Runtime.getRuntime().addShutdownHook(new Shutdown());
	}
	
	private JButton getJButtonClipTemplate() {
		if (jButtonClipTemplate == null) {
			jButtonClipTemplate = new JButton();
			jButtonClipTemplate.setText("Clip Template");
			jButtonClipTemplate.setActionCommand("ClipTemplate");
			jButtonClipTemplate.addActionListener(this);
		}
		return jButtonClipTemplate;
	}

	private JButton getJButtonAutoClip() {
		if (jButtonAutoClip == null) {
			jButtonAutoClip = new JButton();
			jButtonAutoClip.setText("Auto Clip");
			jButtonAutoClip.setActionCommand("AutoClip");
			jButtonAutoClip.addActionListener(this);
			jButtonAutoClip.setEnabled(properties.getProperty("enable_opencv", "no").equals("yes"));
		}
		return jButtonAutoClip;
	}

	private JPanel getJPanel0() {
		if (jPanel0 == null) {
			jPanel0 = new JPanel();
		}
		return jPanel0;
	}

	private JPopupMenu getJListPopupMenu() {
		if (jListPopupMenu == null) {
			jListPopupMenu = new JPopupMenu();
			
			JMenuItem menuItem1 = new JMenuItem("Copy clip area");
			menuItem1.setActionCommand("CopyClip");
			menuItem1.addActionListener(this);
			jListPopupMenu.add(menuItem1);

			JMenuItem menuItem2 = new JMenuItem("Paste clip area");
			menuItem2.setActionCommand("PasteClip");
			menuItem2.addActionListener(this);
			jListPopupMenu.add(menuItem2);
		}
		return jListPopupMenu;
	}

	private JButton getJButtonResetClip() {
		if (jButtonResetClip == null) {
			jButtonResetClip = new JButton();
			jButtonResetClip.setText("Reset Clip");
			jButtonResetClip.setActionCommand("ResetClip");
			jButtonResetClip.addActionListener(this);
		}
		return jButtonResetClip;
	}

	class Shutdown extends Thread {
		public void run() {
			if (svgCanvas != null) {
				svgCanvas.setURI(null);
			}
		}
	}

	private JButton getJButtonConfig() {
		if (jButtonConfig == null) {
			jButtonConfig = new JButton();
			jButtonConfig.setText("Config");
			jButtonConfig.setActionCommand("Config");
			jButtonConfig.addActionListener(this);
		}
		return jButtonConfig;
	}

	private JCheckBox getJCheckBoxPreview() {
		if (jCheckBoxPreview == null) {
			jCheckBoxPreview = new JCheckBox();
			jCheckBoxPreview.setText("Preview");
			jCheckBoxPreview.addItemListener(new ItemListener() {
	
				public void itemStateChanged(ItemEvent event) {
					if (itemSelectionListener != null) {
						itemSelectionListener.setEnabledPreview(jCheckBoxPreview.isSelected());
						if (jList0 != null) {
							int index = jList0.getSelectedIndex();
							itemSelectionListener.updatePreviewImage(index);
						}
					}
					jButtonResetClip.setEnabled(!jCheckBoxPreview.isSelected());
				}
			});
		}
		return jCheckBoxPreview;
	}

	private CustomSVGCanvas getSvgCanvas() {
		if (svgCanvas == null) {
			svgCanvas = new CustomSVGCanvas();
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
	

	private JButton getJButtonCreateEpub() {
		if (jButtonCreateEpub == null) {
			jButtonCreateEpub = new JButton();
			jButtonCreateEpub.setText("Create EPUB");
			jButtonCreateEpub.setActionCommand("Create");
			jButtonCreateEpub.addActionListener(this);
		}
		return jButtonCreateEpub;
	}
	

	private JButton getJButtonClear() {
		if (jButtonClear == null) {
			jButtonClear = new JButton();
			jButtonClear.setText("Clear");
			jButtonClear.setActionCommand("Clear");
			jButtonClear.addActionListener(this);
//			jButton1.setToolTipText("Clear all images");
		}
		return jButtonClear;
	}
	
	private JButton getJButtonRemove() {
		if (jButtonRemove == null) {
			jButtonRemove = new JButton();
			jButtonRemove.setText("Remove");
			jButtonRemove.setActionCommand("Remove");
			jButtonRemove.addActionListener(this);
//			jButton0.setToolTipText("Remove selected images");
		}
		return jButtonRemove;
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
		    jList0.addListSelectionListener(getItemSelectionListener(fileListModel));
		    jList0.addMouseListener(new ListMouseListener(getItemSelectionListener(fileListModel)));
//		    jList0.setToolTipText("Drop image files here");
		    jList0.addMouseListener(new PopClickListener());
		}
		return jList0;
	}

	private ItemSelectionListener getItemSelectionListener(DefaultListModel fileListModel) {
		if (itemSelectionListener == null) {
			itemSelectionListener = new ItemSelectionListener(getCardLayout(), getJPanel4(), getNavigableImagePanel(), getSvgCanvas(), fileListModel);
			itemSelectionListener.setEnabledPreview(false);
		}
		return itemSelectionListener;
	}

	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jPanel1 = new JPanel();
			jPanel1.setLayout(new BorderLayout());
			jPanel1.add(getJPanelNorth(), BorderLayout.NORTH);
			jPanel1.add(getJSplitPane1(), BorderLayout.CENTER);
		}
		return jPanel1;
	}
	
	private JPanel getJPanelNorth() {
		if (jPanelNorth == null) {
			jPanelNorth = new JPanel();
			jPanelNorth.setLayout(new BoxLayout(jPanelNorth, BoxLayout.X_AXIS));
			jPanelNorth.add(getJButtonCreateEpub());
			jPanelNorth.add(getJCheckBoxPreview());
			jPanelNorth.add(getJButtonResetClip());
			jPanelNorth.add(getJButtonAutoClip());
			jPanelNorth.add(getJButtonClipTemplate());
			jPanelNorth.add(getJPanel0());
			jPanelNorth.add(getJButtonConfig());
		}
		return jPanelNorth;
	}

	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			jPanel2 = new JPanel();
			jPanel2.add(getJButtonRemove());
			jPanel2.add(getJButtonClear());
		}
		return jPanel2;
	}

	private JSplitPane getJSplitPane1() {
		if (jSplitPane1 == null) {
			jSplitPane1 = new JSplitPane();
			jSplitPane1.setDividerLocation(263);
			jSplitPane1.setDividerSize(5);
			jSplitPane1.setLeftComponent(geJPanel5());
			jSplitPane1.setRightComponent(getJPanel4());
		}
		return jSplitPane1;
	}

	private Component geJPanel5() {
		if (jPanel5 == null) {
			jPanel5 = new JPanel();
			jPanel5.setLayout(new BorderLayout());
			jPanel5.add(getJScrollPane1());
			jPanel5.add(getJPanel2(), BorderLayout.SOUTH);
		}
		return jPanel5;
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
				frame.setTitle("svgEpub");
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

			getSaveDialog().setLocationRelativeTo(this);
			getSaveDialog().setList(list);
			getSaveDialog().setVisible(true);
		} else if (e.getActionCommand().equals("Config")) {
			getConfigDialog().setMainPanel(this);
			getConfigDialog().setLocationRelativeTo(this);
			getConfigDialog().setVisible(true);
		} else if (e.getActionCommand().equals("ResetClip")) {
			int index = jList0.getSelectedIndex();
			if (index == -1) return;
			ListItem item = (ListItem) jList0.getModel().getElementAt(index);
			item.setClipRect(null);
			itemSelectionListener.updatePreviewImage(index);
		} else if (e.getActionCommand().equals("AutoClip")) {
			int index = jList0.getSelectedIndex();
			if (index == -1) return;
			ListItem item = (ListItem) jList0.getModel().getElementAt(index);
			item.setClipRect(ImageUtil.getContentArea(item));
			itemSelectionListener.updatePreviewImage(index);
		} else if (e.getActionCommand().equals("CopyClip")) {
			int index = jList0.getSelectedIndex();
			if (index == -1) return;
			ListItem item = (ListItem) jList0.getModel().getElementAt(index);
			if (item.getClipRect() == null) {
				copyClipRectangle = null;
			} else {
				copyClipRectangle  = new Rectangle(item.getClipRect());
			}
		} else if (e.getActionCommand().equals("PasteClip")) {
			if (jList0.isSelectionEmpty()) return;
			Object[] items = jList0.getSelectedValues();
			for (Object item : items) {
				((ListItem)item).setClipRect(copyClipRectangle);
			}
			itemSelectionListener.updatePreviewImage(jList0.getSelectedIndex());
		}
	}
	
	private SaveDialog getSaveDialog() {
		if (saveDialog == null) {
			saveDialog = new SaveDialog(this , "Save EPUB" , true, properties);
		}
		return saveDialog;
	}
	
	private ConfigDialog getConfigDialog() {
		if (configDialog == null) {
			configDialog = new ConfigDialog(this, "Config", true, properties);
		}
		return configDialog;
	}
	



	public void updateConfig() {
		DefaultListModel model = (DefaultListModel) jList0.getModel();
		@SuppressWarnings("unchecked")
		Enumeration<ListItem> elist = (Enumeration<ListItem>) model.elements();
		ArrayList<ListItem> list = (ArrayList<ListItem>) Collections.list(elist);
		for (ListItem item : list) {
			item.setSvgFile(null);
		}

		if (itemSelectionListener != null) {
			itemSelectionListener.updatePreviewImage(getJList0().getSelectedIndex());
		}
		ImageUtil.initialize(properties.getProperty("enable_opencv", "no").equals("yes"));
		jButtonAutoClip.setEnabled(properties.getProperty("enable_opencv", "no").equals("yes"));
	}
	
	class PopClickListener extends MouseAdapter {
	    public void mousePressed(MouseEvent e){
	        if (e.isPopupTrigger())
	            doPop(e);
	    }

	    public void mouseReleased(MouseEvent e){
	        if (e.isPopupTrigger())
	            doPop(e);
	    }

	    private void doPop(MouseEvent e){
	    	getJListPopupMenu().show(e.getComponent(), e.getX(), e.getY());
	    }
	}

}
