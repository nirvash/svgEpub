import java.awt.CardLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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
		ListItem listItem = (ListItem)model.get(index);
		IFile item = listItem;
		String filename = item.getFilename();
		if (enabledPreview && PathUtil.isRasterFile(filename) && listItem.isSelected()) {
			File file = null;
			if (listItem.getSvgFile() != null) {
				file = listItem.getSvgFile();
				item = new FileItem(file, listItem.getClipRect());
			} else {
				File svgFile = Epub.convertToSvgFromImage(item);
				if (svgFile != null) {
					listItem.setSvgFile(svgFile);
					item = new FileItem(svgFile, listItem.getClipRect());
				}
			}
		}

		
		if (PathUtil.isSvgFile(item.getFilename())) {
			cardLayout.last(parent);
			svgCanvas.setListItem(listItem);
			svgCanvas.setPreview(enabledPreview);
			svgCanvas.setSvg(item);
		} else {
			cardLayout.last(parent);
			svgCanvas.setListItem(listItem);
			svgCanvas.setPreview(enabledPreview);
			svgCanvas.setImage(item);
		}
	}
}
