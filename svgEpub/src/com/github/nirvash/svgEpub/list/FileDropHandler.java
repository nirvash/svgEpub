package com.github.nirvash.svgEpub.list;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.github.nirvash.svgEpub.util.PathUtil;


public class FileDropHandler extends TransferHandler {
	private static final long serialVersionUID = 1L;
	private final DataFlavor localObjectFlavor;
	private Object[] transferedObjects = null;
	private int[] indices = null;
	private int addIndex = -1; //Location where items were added
	private int addCount = 0; //Number of items added.

	public FileDropHandler(DefaultListModel aModel) {
        localObjectFlavor = new ActivationDataFlavor(
        		Object[].class, DataFlavor.javaJVMLocalObjectMimeType, "Array of Items");
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

        if (handleDropFile(support)) {
        	return true;
        }
        
        return handleMoveListItem(support);
    }

	private boolean handleDropFile(TransferSupport support) {
		JList target = (JList)support.getComponent();
		JList.DropLocation location = (JList.DropLocation)support.getDropLocation();
		DefaultListModel listModel = (DefaultListModel)target.getModel();
		int index = location.getIndex();
		if (index < 0 || index > listModel.getSize()) {
			index = listModel.getSize();
		}
		
        Transferable transferable = support.getTransferable();
		try {
            @SuppressWarnings("unchecked")
			List<File> values = (List<File>)transferable.getTransferData(DataFlavor.javaFileListFlavor);
            Collections.sort(values, new FileComperator());
            for (File value : values) {
            	if (PathUtil.isImageFile(value)) {
	            	listModel.add(index++, new ListItem(value));
            	} else if (PathUtil.isZipFile(value)) {
            		index = addZipFileItems(listModel, index, value);
            	} else if (PathUtil.isRarFile(value)) {
            		index = addRarFileItems(listModel, index, value);
            	} else if (value.isDirectory()) {
            		index = addDirectoryItems(listModel, index, value);
            	}
            }
        } catch (UnsupportedFlavorException e) {
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
                
        return true;
	}
	


	private int addDirectoryItems(DefaultListModel listModel, int index,
			File dir) {
		FileFilter filter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return PathUtil.isImageFile(pathname);
			}
		};
		File[] files = PathUtil.getFiles(dir, filter);
		for (File file : files) {
        	listModel.add(index++, new ListItem(file));
		}
		return index;
	}

	private int addZipFileItems(DefaultListModel listModel, int index, File file) {
		ZipFile zipFile;
		try {
			zipFile = new ZipFile(file, null);
			Enumeration<? extends ZipArchiveEntry> e = zipFile.getEntries();
			while (e.hasMoreElements()) {
				ZipArchiveEntry ze = e.nextElement();
				if (ze.isDirectory()) continue;				
				if (!PathUtil.isImageFile(ze.getName())) continue;
				listModel.add(index++, new ListItem(file, zipFile, ze.getName()));
			}
// TODO : close if all items are removed.
//			zf.close();
		} catch (Exception e) {
			return index;
		}
		
		return index;
	}
	
	private int addRarFileItems(DefaultListModel listModel, int index, File file) {
		try {
			final Archive rarFile = new Archive(file);
			for (final FileHeader fh : rarFile.getFileHeaders()) {
				if (fh.isDirectory()) continue;
				if (!PathUtil.isImageFile(fh.getFileNameString())) continue;
				listModel.add(index++, new ListItem(file, rarFile, fh));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return index;
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
		} catch (UnsupportedFlavorException e) {
			return false;
		} catch (java.io.IOException e) {
			e.printStackTrace();
			return false;
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
	}
}