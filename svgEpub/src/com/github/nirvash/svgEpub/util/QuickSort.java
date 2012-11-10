package com.github.nirvash.svgEpub.util;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Quick sort algorithm (simple)
 * based on pseudo code on Wikipedia "Quick Sort" aricle
 * 
 * @see http://en.wikipedia.org/wiki/Quicksort#Simple_version
 */
public class QuickSort {

	static public <T> void sort(ArrayList<T> input, Comparator<T> comparator) {
		ArrayList<T> ret = quicksort(input, comparator);
		input.clear();
		input.addAll(ret);
	}

	/**
	 * This method sort the input ArrayList using quick sort algorithm.
	 * @param input the ArrayList of integers.
	 * @return sorted ArrayList of integers.
	 */
	static private <T> ArrayList<T> quicksort(ArrayList<T> input, Comparator<T> comparator){
		if(input.size() <= 1){
			return input;
		}
		
		int middle = (int) Math.ceil((double)input.size() / 2);
		T pivot = input.get(middle);

		ArrayList<T> less = new ArrayList<T>();
		ArrayList<T> greater = new ArrayList<T>();
		
		for (int i = 0; i < input.size(); i++) {
			if(comparator.compare(input.get(i), pivot)<=0) {
				if(i == middle){
					continue;
				}
				less.add(input.get(i));
			}
			else{
				greater.add(input.get(i));
			}
		}
		
		return concatenate(quicksort(less, comparator), pivot, quicksort(greater, comparator));
	}
	
	/**
	 * Join the less array, pivot integer, and greater array
	 * to single array.
	 * @param less integer ArrayList with values less than pivot.
	 * @param pivot the pivot integer.
	 * @param greater integer ArrayList with values greater than pivot.
	 * @return the integer ArrayList after join.
	 */
	private static <T> ArrayList<T> concatenate(ArrayList<T> less, T pivot, List<T> greater){
		
		ArrayList<T> list = new ArrayList<T>();
		
		for (int i = 0; i < less.size(); i++) {
			list.add(less.get(i));
		}
		
		list.add(pivot);
		
		for (int i = 0; i < greater.size(); i++) {
			list.add(greater.get(i));
		}
		
		return list;
	}
}