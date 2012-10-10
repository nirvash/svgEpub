import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import nl.siegmann.epublib.util.StringUtil;

//VS4E -- DO NOT REMOVE THIS LINE!
public class SaveDialog extends JDialog implements ActionListener, ComponentListener {

	private static final long serialVersionUID = 1L;
	private JButton jButton0;
	private JButton jButton1;
	private JPanel jPanel0;
	private JPanel jPanel1;
	private JPanel jPanel2;
	private JLabel jLabel0;
	private JTextField jTextField0;
	private JButton jButton2;
	private JButton jButton3;
	private JLabel jLabel1;
	private JTextField jTextField1;
	private JLabel jLabel2;
	private JTextField jTextField2;
	private JLabel jLabel3;
	private JTextField jTextField3;
	private Epub epubWriter = new Epub();
	private static final String PREFERRED_LOOK_AND_FEEL = "javax.swing.plaf.metal.MetalLookAndFeel";
	
	private Properties properties;
	
	public SaveDialog(Window parent) {
		super(parent);
		initComponents();
	}

	public SaveDialog(mainPanel mainPanel, String title, boolean modal,
			Properties properties) {
		super(mainPanel, title, modal);
		this.properties = properties;
		initComponents();
	}

	public SaveDialog(Dialog parent, String title, boolean modal) {
		super(parent, title, modal);
		initComponents();
	}

	public SaveDialog(Dialog parent, String title) {
		super(parent, title);
		initComponents();
	}

	public SaveDialog(Window parent, String title, ModalityType modalityType,
			GraphicsConfiguration arg) {
		super(parent, title, modalityType, arg);
		initComponents();
	}

	public SaveDialog(Window parent, String title, ModalityType modalityType) {
		super(parent, title, modalityType);
		initComponents();
	}

	public SaveDialog(Window parent, String title) {
		super(parent, title);
		initComponents();
	}

	public SaveDialog(Window parent, ModalityType modalityType) {
		super(parent, modalityType);
		initComponents();
	}

	public SaveDialog(Frame parent, String title) {
		super(parent, title);
		initComponents();
	}

	public SaveDialog(Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
	}

	public SaveDialog(Frame parent) {
		super(parent);
		initComponents();
	}

	public SaveDialog() {
		initComponents();
	}

	public SaveDialog(Dialog parent, boolean modal) {
		super(parent, modal);
		initComponents();
	}

	public SaveDialog(Dialog parent) {
		super(parent);
		initComponents();
	}

	public SaveDialog(Frame parent, String title, boolean modal,
			GraphicsConfiguration arg) {
		super(parent, title, modal, arg);
		initComponents();
	}

	public SaveDialog(Frame parent, String title, boolean modal) {
		super(parent, title, modal);
		initComponents();
	}

	private void initComponents() {
		setFont(new Font("Dialog", Font.PLAIN, 12));
		setBackground(Color.white);
		setForeground(Color.black);
		add(getJPanel0(), BorderLayout.SOUTH);
		add(getJPanel2(), BorderLayout.CENTER);
		setSize(475, 221);
		addComponentListener(this);
	}

	private JTextField getJTextField3() {
		if (jTextField3 == null) {
			jTextField3 = new JTextField();
			jTextField3.setText("test.epub");
			jTextField3.setMinimumSize(new Dimension(4, 30));
			jTextField3.setPreferredSize(new Dimension(63, 30));
		}
		return jTextField3;
	}

	private JLabel getJLabel3() {
		if (jLabel3 == null) {
			jLabel3 = new JLabel();
			jLabel3.setText("Filename");
		}
		return jLabel3;
	}

	private JTextField getJTextField2() {
		if (jTextField2 == null) {
			jTextField2 = new JTextField();
			jTextField2.setMinimumSize(new Dimension(4, 30));
			jTextField2.setPreferredSize(new Dimension(63, 30));
		}
		return jTextField2;
	}

	private JLabel getJLabel2() {
		if (jLabel2 == null) {
			jLabel2 = new JLabel();
			jLabel2.setText("Author");
		}
		return jLabel2;
	}

	private JTextField getJTextField1() {
		if (jTextField1 == null) {
			jTextField1 = new JTextField();
			jTextField1.setText("test");
			jTextField1.setMinimumSize(new Dimension(4, 30));
			jTextField1.setPreferredSize(new Dimension(63, 30));
		}
		return jTextField1;
	}

	private JLabel getJLabel1() {
		if (jLabel1 == null) {
			jLabel1 = new JLabel();
			jLabel1.setText("Title");
		}
		return jLabel1;
	}

	private JButton getJButton2() {
		if (jButton2 == null) {
			jButton2 = new JButton();
			jButton2.setText("...");
			jButton2.setActionCommand("File");
			jButton2.addActionListener(this);
			jButton2.setMinimumSize(new Dimension(43, 20));
			jButton2.setPreferredSize(new Dimension(43, 20));
			jButton2.setMaximumSize(new Dimension(43, 20));
		}
		return jButton2;
	}
	
	private JButton getJButton3() {
		if (jButton3 == null) {
			jButton3 = new JButton();
			jButton3.setText("Auto");
			jButton3.setActionCommand("GetTitleAndAuthor");
			jButton3.addActionListener(this);
			jButton3.setMinimumSize(new Dimension(63, 20));
			jButton3.setPreferredSize(new Dimension(63, 20));
			jButton3.setMaximumSize(new Dimension(63, 20));
		}
		return jButton3;
	}

	private JTextField getJTextField0() {
		if (jTextField0 == null) {
			String dir = "";
			try {
				dir = properties.getProperty("savePath");
			} catch (Exception e) {
				// NOP
			}
			if (!StringUtil.isNotBlank(dir)) {
				dir = (new JFileChooser()).getCurrentDirectory().getPath();
			}
			jTextField0 = new JTextField(dir);
			jTextField0.setMinimumSize(new Dimension(4, 30));
			jTextField0.setPreferredSize(new Dimension(63, 30));
		}
		return jTextField0;
	}

	private JLabel getJLabel0() {
		if (jLabel0 == null) {
			jLabel0 = new JLabel();
			jLabel0.setText("Path");
		}
		return jLabel0;
	}

	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			jPanel2 = new JPanel();
			jPanel2.setLayout(new GridBagLayout());
			addGrid(jPanel2, getJLabel0(), 		0, 0, 0.0f);
			addGrid(jPanel2, getJTextField0(), 	1, 0, 1.0f);
			addGrid(jPanel2, getJButton2(), 	2, 0, 0.0f);
			addGrid(jPanel2, getJLabel1(), 		0, 1, 0.0f);
			addGrid(jPanel2, getJTextField1(),	1, 1, 1.0f);
			addGrid(jPanel2, getJButton3(),		2, 1, 0.0f);
			addGrid(jPanel2, getJLabel2(),		0, 2, 0.0f);
			addGrid(jPanel2, getJTextField2(),	1, 2, 1.0f);
			addGrid(jPanel2, getJLabel3(),		0, 3, 0.0f);
			addGrid(jPanel2, getJTextField3(),	1, 3, 1.0f);
		}
		return jPanel2;
	}

	private void addGrid(JComponent parent, JComponent child, int x, int y, float w) {
		GridBagLayout layout = (GridBagLayout)parent.getLayout();
		GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = w;
        gbc.weighty = 1.0f;
        gbc.insets = new Insets(5, 3, 5, 3);
        layout.setConstraints(child, gbc);
        parent.add(child);
	}

	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jPanel1 = new JPanel();
		}
		return jPanel1;
	}

	private JPanel getJPanel0() {
		if (jPanel0 == null) {
			jPanel0 = new JPanel();
			jPanel0.setPreferredSize(new Dimension(100, 40));
			jPanel0.setLayout(new BoxLayout(jPanel0, BoxLayout.X_AXIS));
			jPanel0.add(getJPanel1());
			jPanel0.add(getJButton0());
			jPanel0.add(getJButton1());
		}
		return jPanel0;
	}

	private JButton getJButton1() {
		if (jButton1 == null) {
			jButton1 = new JButton();
			jButton1.setText("Cancel");
			jButton1.setActionCommand("Cancel");
			jButton1.addActionListener(this);
		}
		return jButton1;
	}

	private JButton getJButton0() {
		if (jButton0 == null) {
			jButton0 = new JButton();
			jButton0.setText("Save");
			jButton0.setActionCommand("Save");
			jButton0.addActionListener(this);
		}
		return jButton0;
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
			public void run() {
				SaveDialog dialog = new SaveDialog();
				dialog.setDefaultCloseOperation(SaveDialog.DISPOSE_ON_CLOSE);
				dialog.setTitle("Save");
				dialog.setLocationRelativeTo(null);
				dialog.getContentPane().setPreferredSize(dialog.getSize());
				dialog.pack();
				dialog.setVisible(true);
			}
		});
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Cancel")) {
			this.setVisible(false);
		} else if (e.getActionCommand().equals("Save")) {
			save();
		} else if (e.getActionCommand().equals("File")) {
			String path = selectFilePath();
			getJTextField0().setText(path);
		} else if (e.getActionCommand().equals("GetTitleAndAuthor")) {
			epubWriter.analyzerTitleAndPath();
			getJTextField1().setText(epubWriter.getTitle());
			getJTextField2().setText(epubWriter.getAuthor());
			getJTextField3().setText(epubWriter.getOutputFilename());
			
		}
	}
	
	private void save() {
		String path = jTextField0.getText() + "\\" + jTextField3.getText();
		epubWriter.setTitle(jTextField1.getText());
		epubWriter.setAuthor(jTextField2.getText());
		epubWriter.setPath(path);
		ProgressMonitor monitor = new ProgressMonitor(this, "Saving", "Generating epub file...", 0, epubWriter.getTotalPage());
		monitor.setMillisToDecideToPopup(0);
		monitor.setMillisToPopup(0);
		epubWriter.setMonitor(monitor);
		epubWriter.start();		
	}

	private String selectFilePath() {
		JFileChooser chooser = new JFileChooser();
		String currentPath = getJTextField0().getText();
		chooser.setCurrentDirectory(new File(currentPath));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);
		int selected = chooser.showDialog(this, "Select");
		if (selected == JFileChooser.APPROVE_OPTION) {
			File directory = chooser.getSelectedFile();
			return directory.getPath();
		}
		return currentPath;
	}

	public void setList(ArrayList<ListItem> list) {
		epubWriter.setList(list);		
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
		properties.setProperty("savePath", jTextField0.getText());
		FileOutputStream os;
		try {
			os = new FileOutputStream("config.xml");
			properties.storeToXML(os, null, "UTF-8");
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
	}

	@Override
	public void componentResized(ComponentEvent arg0) {
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
	}

}
