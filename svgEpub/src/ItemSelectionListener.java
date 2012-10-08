import java.awt.CardLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.batik.swing.JSVGCanvas;


public class ItemSelectionListener implements ListSelectionListener {
	private CardLayout cardLayout;
	private JPanel parent;
	private JSVGCanvas svgCanvas;
	private NavigableImagePanel imagePanel;
	private DefaultListModel model = null;

	public ItemSelectionListener(CardLayout layout, JPanel panel, NavigableImagePanel imagePanel, JSVGCanvas jsvgCanvas, DefaultListModel fileListModel) {
		cardLayout = layout;
		parent = panel;
		this.imagePanel = imagePanel;
		svgCanvas = jsvgCanvas;
		model = fileListModel;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) return;
		int index = ((JList)e.getSource()).getSelectedIndex();
		if (index < 0) {
			cardLayout.first(parent);
			imagePanel.setImage(null);
			return;
		}
		ListItem item = (ListItem)model.get(index);
		File file = item.getFile();
		
		if (mainPanel.isSvgFile(file)) {
			cardLayout.last(parent);
			svgCanvas.setURI(file.toURI().toString());
		} else {
			cardLayout.first(parent);
			BufferedImage image = null;
			try {
				image = ImageIO.read(file);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			imagePanel.setImage(image);
		}
	}
}
