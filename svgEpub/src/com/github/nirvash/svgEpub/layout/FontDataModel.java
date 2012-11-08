package com.github.nirvash.svgEpub.layout;

import java.util.ArrayList;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

import jknnl.kohonen.LearningDataModel;

public class FontDataModel implements LearningDataModel {
	ArrayList<LayoutElement> elements = new ArrayList<LayoutElement>();
	public FontDataModel(ArrayList<LayoutElement> elements, IplImage image, double scale, int width, int height) {
		for (LayoutElement le: elements) {
			this.elements.addAll(le.elements);
		}
		for (LayoutElement le: this.elements) {
			le.createVector(image, scale, width, height);
		}
	}

	@Override
	public double[] getData(int index) {
		return elements.get(index).getVector();
	}

	@Override
	public int getDataSize() {
		return elements.size();
	}

	public LayoutElement getLayoutElement(int i) {
		return elements.get(i);
	}


}
