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
	private CustomSVGCanvas svgCanvas;
	private NavigableImagePanel imagePanel;
	private DefaultListModel model = null;
	private boolean enabledPreview = true;

	public ItemSelectionListener(CardLayout layout, JPanel panel, NavigableImagePanel imagePanel, CustomSVGCanvas svgCanvas, DefaultListModel fileListModel) {
		cardLayout = layout;
		parent = panel;
		this.imagePanel = imagePanel;
		this.svgCanvas = svgCanvas;
		model = fileListModel;
	}
	
	public void setEnabledPreview(boolean b) {
		this.enabledPreview = b;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) return;
		int index = ((JList)e.getSource()).getSelectedIndex();
		
		updatePreviewImage(index);
	}

	public void updatePreviewImage(int index) {
		if (index < 0) {
			cardLayout.first(parent);
			imagePanel.setImage(null);
			return;
		}
		ListItem item = (ListItem)model.get(index);
		File file = item.getFile();
		
		if (enabledPreview && mainPanel.isImageFile(file) && item.isSelected()) {
			if (item.getSvgFile() != null) {
				file = item.getSvgFile();
			} else {
				File svgFile = Epub.convertToSvgFromImage(file);
				if (svgFile != null) {
					item.setSvgFile(svgFile);
					file = svgFile;
				}
			}
		}

		
		if (mainPanel.isSvgFile(file)) {
			cardLayout.last(parent);
			svgCanvas.setListItem(item);
			svgCanvas.setPreview(enabledPreview);
			svgCanvas.setURI(file.toURI().toString());
		} else {
			cardLayout.last(parent);
			svgCanvas.setListItem(item);
			svgCanvas.setPreview(enabledPreview);
			svgCanvas.setImage(file);
			
			/*
			cardLayout.first(parent);
			BufferedImage image = null;
			try {
				image = ImageIO.read(file);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			imagePanel.setImage(image);
			*/
		}
	}
}
