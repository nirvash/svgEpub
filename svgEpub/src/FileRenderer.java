import java.awt.Color;
import java.awt.Component;
import java.awt.LayoutManager;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
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
	
	protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
	private static final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);
	
	public FileRenderer() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(checkbox);
		add(label);
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
			/*
			label.setBackground(bg == null ? list.getSelectionBackground() : bg);
			label.setForeground(fg == null ? list.getSelectionForeground() : fg);
			checkbox.setBackground(bg == null ? list.getSelectionBackground() : bg);
			checkbox.setForeground(fg == null ? list.getSelectionForeground() : fg);
			*/
		}
		else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
			/*			
			list.setBackground(list.getBackground());
			list.setForeground(list.getForeground());
			checkbox.setBackground(list.getBackground());
			checkbox.setForeground(list.getForeground());
*/
		}
		
		checkbox.setSelected(item.isSelected());
	    	
		setEnabled(list.isEnabled());
		checkbox.setEnabled(list.isEnabled());
		label.setEnabled(list.isEnabled());		

		label.setFont(list.getFont());
	    	
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
	    
		label.setText(value.toString());
		label.setIcon(FileSystemView.getFileSystemView().getSystemIcon(item.getFile()));
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
