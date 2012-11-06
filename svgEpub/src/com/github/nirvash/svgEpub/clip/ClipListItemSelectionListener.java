package com.github.nirvash.svgEpub.clip;
import java.io.File;

import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.github.nirvash.svgEpub.Epub;
import com.github.nirvash.svgEpub.list.BookListModel;
import com.github.nirvash.svgEpub.list.FileItem;
import com.github.nirvash.svgEpub.list.IFile;
import com.github.nirvash.svgEpub.list.ListItem;
import com.github.nirvash.svgEpub.ui.CustomSVGCanvas;
import com.github.nirvash.svgEpub.util.PathUtil;


public class ClipListItemSelectionListener implements ListSelectionListener {
	private CustomSVGCanvas svgCanvas = null;
	private JList fileList = null;
	private ClipListModel clipListModel = null;
	private boolean enabledPreview = true;

	public ClipListItemSelectionListener(CustomSVGCanvas svgCanvas,
			JList fileList,
			ClipListModel clipListModel) {
		this.svgCanvas = svgCanvas;
		this.fileList = fileList;
		this.clipListModel = clipListModel;
	}
	
	public void setEnabledPreview(boolean b) {
		this.enabledPreview = b;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (clipListModel.isUpdating()) return;
		if (e.getValueIsAdjusting()) return;
		int clipIindex = ((JList)e.getSource()).getSelectedIndex();
		
		ListItem listItem = (ListItem)fileList.getSelectedValue();
		if (listItem == null) return;
		listItem.setSelectedClipIndex(clipIindex);
		
		int fileIndex = fileList.getSelectedIndex();
		BookListModel bookModel = (BookListModel)fileList.getModel();
		bookModel.notifyModelUpdate(fileIndex, fileIndex);
		
		updatePreviewImage(fileIndex);
	}
	
	private void updatePreviewImage(int index) {
		if (index < 0) {
			svgCanvas.setURI(null);
			return;
		}
		BookListModel bookModel = (BookListModel)fileList.getModel();
		ListItem listItem = (ListItem)bookModel.get(index);
		IFile item = listItem;
		String filename = item.getFilename();
		if (enabledPreview && PathUtil.isRasterFile(filename) && listItem.isConvertToSVG()) {
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

		svgCanvas.setListItem(listItem);
		svgCanvas.setPreview(enabledPreview);
		if (PathUtil.isImageFile(item.getFilename())) {
			svgCanvas.setImage(item);
		} else {
			svgCanvas.setSvg(item);
		}
	}

}
