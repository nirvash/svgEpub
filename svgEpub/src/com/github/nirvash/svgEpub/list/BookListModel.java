package com.github.nirvash.svgEpub.list;
import javax.swing.DefaultListModel;


public class BookListModel extends DefaultListModel {
	public void notifyModelUpdate(int begin, int end) {
		this.fireContentsChanged(this, begin, end);
	}
}
