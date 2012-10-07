import java.awt.Component;
import java.io.File;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.filechooser.FileSystemView;


public class FileRenderer extends DefaultListCellRenderer {
	private static final long serialVersionUID = 1L;

	@Override
	public Component getListCellRendererComponent(
			JList list, 
			Object value,
			int index, 
			boolean isSelected, 
			boolean cellHasFocus) {
		
		Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		JLabel item = (JLabel)c;
		File file = (File)value;
		item.setText(file.getName());
		item.setIcon(FileSystemView.getFileSystemView().getSystemIcon(file));
		return item;
	}

}
