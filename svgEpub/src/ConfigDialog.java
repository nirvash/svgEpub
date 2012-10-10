import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


//VS4E -- DO NOT REMOVE THIS LINE!
public class ConfigDialog extends JDialog implements ComponentListener, ActionListener {

	private static final long serialVersionUID = 1L;
	private static final String PREFERRED_LOOK_AND_FEEL = null;
	
	private CustomProperties properties;
	private JTable table;
	private mainPanel mainPanel = null;

	public ConfigDialog(Window parent) {
		super(parent);
		initComponents();
	}

	public ConfigDialog(mainPanel mainPanel, String title, boolean modal,
			CustomProperties properties ) {
		super(mainPanel, title, modal);
		this.properties = properties;
		initComponents();
	}

	public ConfigDialog(Dialog parent, String title, boolean modal) {
		super(parent, title, modal);
		initComponents();
	}

	public ConfigDialog(Dialog parent, String title) {
		super(parent, title);
		initComponents();
	}

	public ConfigDialog(Window parent, String title, ModalityType modalityType,
			GraphicsConfiguration arg) {
		super(parent, title, modalityType, arg);
		initComponents();
	}

	public ConfigDialog(Window parent, String title, ModalityType modalityType) {
		super(parent, title, modalityType);
		initComponents();
	}

	public ConfigDialog(Window parent, String title) {
		super(parent, title);
		initComponents();
	}

	public ConfigDialog(Window parent, ModalityType modalityType) {
		super(parent, modalityType);
		initComponents();
	}

	public ConfigDialog(Frame parent, String title) {
		super(parent, title);
		initComponents();
	}

	public ConfigDialog(Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
	}

	public ConfigDialog(Frame parent) {
		super(parent);
		initComponents();
	}

	public ConfigDialog() {
		initComponents();
	}

	public ConfigDialog(Dialog parent, boolean modal) {
		super(parent, modal);
		initComponents();
	}

	public ConfigDialog(Dialog parent) {
		super(parent);
		initComponents();
	}

	public ConfigDialog(Frame parent, String title, boolean modal,
			GraphicsConfiguration arg) {
		super(parent, title, modal, arg);
		initComponents();
	}

	public ConfigDialog(Frame parent, String title, boolean modal) {
		super(parent, title, modal);
		initComponents();
	}

	private void initComponents() {
		setLayout(new BorderLayout());
		JTable table = getJTable();
	    JScrollPane scrollpane = new JScrollPane(table);
	    scrollpane.getViewport().setBackground(Color.white);
	    getContentPane().add(scrollpane);
	    
	    JPanel jPanel = new JPanel();
	    jPanel.setLayout(new BorderLayout());
	    JButton button = new JButton("OK");
	    button.addActionListener(this);
	    jPanel.add(button, BorderLayout.EAST);
	    getContentPane().add(jPanel, BorderLayout.SOUTH);
		setSize(500, 400);
		addComponentListener(this);
	}
	
	private JTable getJTable() {
		if (table == null) {
			if (properties == null) {
				properties = new CustomProperties();
			}
			table = new PropertyTable(properties);
		}
		return table;
	}

	private static void installLnF() {
		try {
			String lnfClassname = PREFERRED_LOOK_AND_FEEL;
			if (lnfClassname == null)
				lnfClassname = UIManager.getCrossPlatformLookAndFeelClassName();
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
			public void run() {
				ConfigDialog dialog = new ConfigDialog();
				dialog.setDefaultCloseOperation(ConfigDialog.DISPOSE_ON_CLOSE);
				dialog.setTitle("ConfigDialog");
				dialog.setLocationRelativeTo(null);
				dialog.getContentPane().setPreferredSize(dialog.getSize());
				dialog.pack();
				dialog.setVisible(true);
			}
		});
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		if (getJTable().isEditing()) {
			getJTable().getCellEditor().stopCellEditing();
		}
		
		try {
			FileOutputStream xmlStream = new FileOutputStream("config.xml");
			properties.storeToXML(xmlStream, null, "UTF-8");
			xmlStream.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		if (mainPanel != null) {
			mainPanel.updateConfig();
		}
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentResized(ComponentEvent e) {
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		setVisible(false);
	}

	public void setMainPanel(mainPanel mainPanel) {
		this.mainPanel = mainPanel;
	}

}
