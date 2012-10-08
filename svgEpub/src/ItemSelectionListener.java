import java.awt.CardLayout;
import java.io.File;

import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.batik.swing.JSVGCanvas;


public class ItemSelectionListener implements ListSelectionListener {
	private CardLayout cardLayout;
	private JPanel parent;
	private JSVGCanvas svgCanvas;
	private JEditorPane pane = null;
	private DefaultListModel model = null;

	public ItemSelectionListener(CardLayout layout, JPanel panel, JEditorPane jEditorPane, JSVGCanvas jsvgCanvas, DefaultListModel fileListModel) {
		cardLayout = layout;
		parent = panel;
		pane = jEditorPane;
		svgCanvas = jsvgCanvas;
		model = fileListModel;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) return;
		int index = ((JList)e.getSource()).getSelectedIndex();
		ListItem item = (ListItem)model.get(index);
		File file = item.getFile();
		
		String html = "";
		if (mainPanel.isSvgFile(file)) {
			cardLayout.last(parent);
			svgCanvas.setURI(file.toURI().toString());
		} else {
			cardLayout.first(parent);
			html = String.format("<html><head>" +
					"<style type=\"text/css\">" +
					"img { max-height: %dpx; max-width: %dpx; }" +
					"</style></head>" +
					"<body><img src=\"%s\"></body></html>", 
					pane.getHeight(), pane.getWidth(),
					file.toURI());
			pane.setContentType("text/html");
			pane.setText(html);
		}
	}
}
