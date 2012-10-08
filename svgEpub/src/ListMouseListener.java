import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JCheckBox;
import javax.swing.JList;


public class ListMouseListener implements MouseListener {
	final int hotspot = new JCheckBox().getPreferredSize().width; 
	
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

		// Toggle selected state
		item.setSelected(!item.isSelected());

		// Repaint cell
		list.repaint(list.getCellBounds(index, index));		 
	}

	@Override
	public void mouseEntered(MouseEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent event) {
		// TODO Auto-generated method stub

	}

}
