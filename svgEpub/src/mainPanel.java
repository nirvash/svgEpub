import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.dyno.visual.swing.layouts.Bilateral;
import org.dyno.visual.swing.layouts.Constraints;
import org.dyno.visual.swing.layouts.GroupLayout;


//VS4E -- DO NOT REMOVE THIS LINE!
public class mainPanel extends JFrame {

	private static final long serialVersionUID = 1L;
	private JScrollPane jScrollPane0;
	private JMenuItem jMenuItem0;
	private JMenu jMenu0;
	private JMenuBar jMenuBar0;
	private JSplitPane jSplitPane1;
	private JPanel jPanel1;
	private JList jList0;
	private JScrollPane jScrollPane1;
	private JPanel jPanel0;
	private static final String PREFERRED_LOOK_AND_FEEL = "javax.swing.plaf.metal.MetalLookAndFeel";
	public mainPanel() {
		initComponents();
	}

	private void initComponents() {
		setLayout(new GroupLayout());
		add(getJPanel1(), new Constraints(new Bilateral(0, 0, 0), new Bilateral(0, 0, 0)));
		setJMenuBar(getJMenuBar0());
		setSize(617, 483);
	}

	private JPanel getJPanel0() {
		if (jPanel0 == null) {
			jPanel0 = new JPanel();
			jPanel0.setLayout(new GroupLayout());
		}
		return jPanel0;
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
		    
		}
		return jList0;
	}

	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jPanel1 = new JPanel();
			jPanel1.setMinimumSize(new Dimension(0, 125));
			jPanel1.setLayout(new BoxLayout(jPanel1, BoxLayout.X_AXIS));
			jPanel1.add(getJSplitPane1());
		}
		return jPanel1;
	}

	private JSplitPane getJSplitPane1() {
		if (jSplitPane1 == null) {
			jSplitPane1 = new JSplitPane();
			jSplitPane1.setDividerLocation(197);
			jSplitPane1.setDividerSize(5);
			jSplitPane1.setMinimumSize(new Dimension(0, 100));
			jSplitPane1.setLeftComponent(getJScrollPane1());
			jSplitPane1.setRightComponent(getJPanel0());
		}
		return jSplitPane1;
	}

	private JMenuBar getJMenuBar0() {
		if (jMenuBar0 == null) {
			jMenuBar0 = new JMenuBar();
			jMenuBar0.add(getJMenu0());
		}
		return jMenuBar0;
	}

	private JMenu getJMenu0() {
		if (jMenu0 == null) {
			jMenu0 = new JMenu();
			jMenu0.setText("jMenu0");
			jMenu0.add(getJMenuItem0());
		}
		return jMenu0;
	}

	private JMenuItem getJMenuItem0() {
		if (jMenuItem0 == null) {
			jMenuItem0 = new JMenuItem();
			jMenuItem0.setText("jMenuItem0");
		}
		return jMenuItem0;
	}

	private JScrollPane getJScrollPane0() {
		if (jScrollPane0 == null) {
			jScrollPane0 = new JScrollPane();
			jScrollPane0.setViewportView(getJList0());
		}
		return jScrollPane0;
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

}
