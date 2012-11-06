package com.github.nirvash.svgEpub.list;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

public class ListClipDropHandler extends TransferHandler { 
	private static final long serialVersionUID = 1L;
	private final DataFlavor localObjectFlavor;
	private Object[] transferedObjects = null;
	private int[] indices = null;
	private int addIndex = -1; //Location where items were added
	private int addCount = 0; //Number of items added.
	
	public interface onMoveCallback {
		public void onMove();
	}
	private onMoveCallback callback = null;
	
	public ListClipDropHandler(DefaultListModel aModel, onMoveCallback callback) {
        localObjectFlavor = new ActivationDataFlavor(
        		Object[].class, DataFlavor.javaJVMLocalObjectMimeType, "Array of Items");
        this.callback = callback;
    }
	
	@Override
	protected Transferable createTransferable(JComponent c) {
		JList list = (JList)c;
		indices = list.getSelectedIndices();
		transferedObjects = list.getSelectedValues();
		return new DataHandler(transferedObjects, localObjectFlavor.getMimeType());
	}
	
	@Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }

        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
        	   support.isDataFlavorSupported(localObjectFlavor);
    }
	
	@Override
	public int getSourceActions(JComponent c) {
		return TransferHandler.MOVE;
	}
	
	@Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        
        return handleMoveListItem(support);
    }
	

	private boolean handleMoveListItem(TransferSupport support) {
		JList target = (JList)support.getComponent();
		JList.DropLocation location = (JList.DropLocation)support.getDropLocation();
		DefaultListModel listModel = (DefaultListModel)target.getModel();
		int index = location.getIndex();
		if (index < 0 || index > listModel.getSize()) {
			index = listModel.getSize();
		}
		addIndex = index;

        Transferable transferable = support.getTransferable();
		try {
			Object[] values = (Object[])transferable.getTransferData(localObjectFlavor);
			addCount = values.length;
			for (Object value : values) {
				int idx = index++;
				listModel.add(idx,  value);
				target.addSelectionInterval(idx, idx);
			}
		} catch (java.io.IOException e) {
			e.printStackTrace();
			return false;
		} catch (UnsupportedFlavorException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	protected void exportDone(JComponent c, Transferable data, int action) {
		cleanup(c, action == TransferHandler.MOVE);
	}
	
	private void cleanup(JComponent c, boolean remove) {
		if (remove && indices != null) {
			JList source = (JList)c;
			DefaultListModel model = (DefaultListModel)source.getModel();
			if (addCount > 0) {
				for (int i=0; i<indices.length; i++) {
					if (indices[i] >= addIndex) {
						indices[i] += addCount;
					}
				}
			}
			for (int i=indices.length-1; i>=0; i--) {
				model.remove(indices[i]);
			}
		}
		indices = null;
		addCount = 0;
		addIndex = -1;
		
		if (callback != null) callback.onMove();
	}
	
}
