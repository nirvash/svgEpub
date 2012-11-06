package com.github.nirvash.svgEpub.list;
import java.awt.Rectangle;
import java.io.InputStream;
import java.util.ArrayList;

import com.github.nirvash.svgEpub.clip.ClipListItem;


public interface IFile {
	String getFilename();
	InputStream getInputStream();
	Rectangle getClipRect();
	ArrayList<ClipListItem> getClipList();
	String getURI();
}
