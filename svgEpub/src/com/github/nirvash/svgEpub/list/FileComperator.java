package com.github.nirvash.svgEpub.list;
import java.io.File;
import java.util.Comparator;


public class FileComperator implements Comparator<File> {

	@Override
	public int compare(File a, File b) {
		return a.getName().compareToIgnoreCase(b.getName());
	}

}
