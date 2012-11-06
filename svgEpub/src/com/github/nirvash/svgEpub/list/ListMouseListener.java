package com.github.nirvash.svgEpub.list;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JCheckBox;
import javax.swing.JList;


public class ListMouseListener implements MouseListener {
	final int hotspot = new JCheckBox().getPreferredSize().width; 
	private ItemSelectionListener itemSelectListener;
	
	public ListMouseListener(ItemSelectionListener itemSelectListener) {
		this.itemSelectListener = itemSelectListener;
	}
	
	@Override
	public void mouseClicked(MouseEvent event) {
		JList list = (JList) event.getSource();
		// Get index of item clicked
		int index = list.locationToIndex(event.getPoint());
		
        if(index < 0) { 
            return;
        }
        
        if(event.getX() > list.getCellBounds(index, index).x + hotspot) {
            return; 
        }
               
		ListItem item = (ListItem)list.getModel().getElementAt(index);
		if (!item.canConvertToSVG()) {
			return;
		}

		// Toggle selected state
		item.setConvertToSVG(!item.isConvertToSVG());

		// Repaint cell
		list.repaint(list.getCellBounds(index, index));		 
		
		// Update preview image
		itemSelectListener.updateItem(index);
	}

	@Override
	public void mouseEntered(MouseEvent event) {
	}

	@Override
	public void mouseExited(MouseEvent event) {
	}

	@Override
	public void mousePressed(MouseEvent event) {
	}

	@Override
	public void mouseReleased(MouseEvent event) {
	}

}
