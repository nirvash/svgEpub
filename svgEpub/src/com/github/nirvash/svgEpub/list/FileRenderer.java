package com.github.nirvash.svgEpub.list;
import java.awt.Color;
import java.awt.Component;
import java.awt.LayoutManager;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;


public class FileRenderer extends JPanel implements ListCellRenderer {
	private static final long serialVersionUID = 1L;	
	private JLabel label = new JLabel();
	private JCheckBox checkbox =  new JCheckBox();
	static private ImageIcon[] icons = new ImageIcon[2];
	
	protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
	private static final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);
	

	public FileRenderer() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(checkbox);
		add(label);
		
		if (icons[0] == null) {
			java.net.URL textURL = this.getClass().getResource("/resources/icon_text.png");
			java.net.URL illustURL = this.getClass().getResource("/resources/icon_illust.png");
			icons[0] = new ImageIcon(textURL);
			icons[1] = new ImageIcon(illustURL);
		}
	}
	

	@Override
	public Component getListCellRendererComponent(
			JList list, 
			Object value,
			int index, 
			boolean isSelected, 
			boolean cellHasFocus) {
		ListItem item = (ListItem)value;

		setComponentOrientation(list.getComponentOrientation());
		Color bg = null;
		Color fg = null;
		JList.DropLocation dropLocation = list.getDropLocation();
		if (dropLocation != null
				&& !dropLocation.isInsert()
				&& dropLocation.getIndex() == index) {
			bg = UIManager.getColor("List.dropCellBackground");
			fg = UIManager.getColor("List.dropCellForeground");
			isSelected = true;
		}
			
		if (isSelected) {
			setBackground(bg == null ? list.getSelectionBackground() : bg);
			setForeground(fg == null ? list.getSelectionForeground() : fg);
		}
		else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		
		checkbox.setSelected(item.isConvertToSVG());
		label.setIcon(icons[item.isConvertToSVG() ? 0 : 1]);
		
		setEnabled(list.isEnabled());
		checkbox.setEnabled(item.canConvertToSVG());
		label.setEnabled(list.isEnabled());		

		label.setFont(list.getFont());
		

		/*
		Border border = null;
		if (cellHasFocus) {
			if (isSelected) {
				border = UIManager.getBorder("List.focusSelectedCellHighlightBorder");
			}
			if (border == null) {
				border = UIManager.getBorder("List.focusCellHighlightBorder");
			}
		} else {
			border = getNoFocusBorder();
		}
		setBorder(border);
		*/
	    
		label.setText(value.toString());
//		label.setIcon(FileSystemView.getFileSystemView().getSystemIcon(item.getFile()));
		return this;
/*
		Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		JLabel item = (JLabel)c;
		File file = (File)value;
		item.setText(file.getName());
		item.setIcon(FileSystemView.getFileSystemView().getSystemIcon(file));
		return item;
*/
	}
	
    private static Border getNoFocusBorder() {
    	if (System.getSecurityManager() != null) {
    		return SAFE_NO_FOCUS_BORDER;
    	} else {
    		return UIManager.getBorder("List.noFocusBorder");
    	}
    }

}
