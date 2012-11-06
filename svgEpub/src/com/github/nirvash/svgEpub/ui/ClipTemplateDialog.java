package com.github.nirvash.svgEpub.ui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.github.nirvash.svgEpub.CustomProperties;
import com.github.nirvash.svgEpub.clip.ClipTemplate;

//VS4E -- DO NOT REMOVE THIS LINE!
public class ClipTemplateDialog extends JDialog  {
	private static final long serialVersionUID = 1L;
	private JPanel jPanel0;
	private JButton jButtonOk;
	private JButton jButtonCancel;
	private JPanel jPanel2;
	private JList jList0;
	private JScrollPane jScrollPane0;
	private svgEpubMainPanel mainPanel;
	private static final String PREFERRED_LOOK_AND_FEEL = "javax.swing.plaf.metal.MetalLookAndFeel";

	
	public ClipTemplateDialog() {
		initComponents();
	}
	public ClipTemplateDialog(svgEpubMainPanel mainPanel, String string, boolean b,
			CustomProperties properties) {
		initComponents();
	}

	private void initComponents() {
		setFont(new Font("Dialog", Font.PLAIN, 12));
		setBackground(Color.white);
		setForeground(Color.black);
		add(getJPanel0(), BorderLayout.SOUTH);
		add(getJScrollPane0(), BorderLayout.CENTER);
		setSize(320, 162);
	}
	private JScrollPane getJScrollPane0() {
		if (jScrollPane0 == null) {
			jScrollPane0 = new JScrollPane();
			jScrollPane0.setViewportView(getJList0());
		}
		return jScrollPane0;
	}
	private JList getJList0() {
		if (jList0 == null) {
			jList0 = new JList();
			DefaultListModel listModel = new DefaultListModel();
			ArrayList<ClipTemplate> list = ClipTemplate.getList();
			for (ClipTemplate template : list) {
				listModel.addElement(template);
			}
			jList0.setModel(listModel);
		}
		return jList0;
	}
	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			jPanel2 = new JPanel();
		}
		return jPanel2;
	}
	private JButton getJButtonOk() {
		if (jButtonOk == null) {
			jButtonOk = new JButton();
			jButtonOk.setText("Apply");
			jButtonOk.addActionListener(new ActionListener() {
	
				public void actionPerformed(ActionEvent event) {
					jButtonOkActionActionPerformed(event);
				}
			});
		}
		return jButtonOk;
	}
	private JButton getJButtonCancel() {
		if (jButtonCancel == null) {
			jButtonCancel = new JButton();
			jButtonCancel.setText("Cancel");
			jButtonCancel.addActionListener(new ActionListener() {
	
				public void actionPerformed(ActionEvent event) {
					jButtonCancelActionActionPerformed(event);
				}
			});
		}
		return jButtonCancel;
	}
	private JPanel getJPanel0() {
		if (jPanel0 == null) {
			jPanel0 = new JPanel();
			jPanel0.setPreferredSize(new Dimension(100, 30));
			jPanel0.setLayout(new BoxLayout(jPanel0, BoxLayout.X_AXIS));
			jPanel0.add(getJPanel2());
			jPanel0.add(getJButtonOk());
			jPanel0.add(getJButtonCancel());
		}
		return jPanel0;
	}
	public void setMainPanel(svgEpubMainPanel mainPanel) {
		this.mainPanel = mainPanel;
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
				ClipTemplateDialog dialog = new ClipTemplateDialog();
				dialog.setDefaultCloseOperation(ClipTemplateDialog.DISPOSE_ON_CLOSE);
				dialog.setTitle("Select clip template");
				dialog.setLocationRelativeTo(null);
				dialog.getContentPane().setPreferredSize(dialog.getSize());
				dialog.pack();
				dialog.setVisible(true);
			}
		});
	}
	private void jButtonOkActionActionPerformed(ActionEvent event) {
		mainPanel.applyClipTemplate((ClipTemplate)jList0.getSelectedValue());
	}
	
	private void jButtonCancelActionActionPerformed(ActionEvent event) {
		setVisible(false);
	}

}
