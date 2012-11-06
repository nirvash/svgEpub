package com.github.nirvash.svgEpub;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * This class is a JTable subclass that displays a table of the JavaBeans
 * properties of any specified class.
 */
public class PropertyTable extends JTable {
	private static final long serialVersionUID = 1L;

	public PropertyTable(CustomProperties properties) {
		// Set the data model for this table
		try {
			setModel(new PropertyTableModel(properties));
		} catch (Exception e) {
			return;
		}

		// Tweak the appearance of the table by manipulating its column model
		TableColumnModel colmodel = getColumnModel();

		// Set column widths
		colmodel.getColumn(0).setPreferredWidth(125);
		colmodel.getColumn(1).setPreferredWidth(200);

		/*
		// Right justify the text in the first column
		TableColumn namecol = colmodel.getColumn(0);
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setHorizontalAlignment(SwingConstants.RIGHT);
		namecol.setCellRenderer(renderer);
		*/
	}
	
	private class PropertyTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		private CustomProperties properties;
		private final String columnNames[] = {
			"Property name", "Value"
		};
		
		public PropertyTableModel(CustomProperties properties) {
			this.properties = properties;
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return this.properties.size();
		}

		@Override
		public Object getValueAt(int row, int column) {
			if (row >= getRowCount()) return null;
			Object[] keys = properties.keySet().toArray();
			
			if (column == 0) {
				return keys[row];
			} else if (column == 1) {
				return properties.get(keys[row]);
			} else {
				return "";
			}
		}
		
		@Override
		public void setValueAt(Object value, int row, int column) {
			if (row >= getRowCount()) return;
			Object[] keys = properties.keySet().toArray();
			
			if (column == 0) {
			} else if (column == 1) {
				String text = (String)value;
				if (text != null) {
					properties.setProperty((String) keys[row], text);
				}
			} else {
			}		
		}

		
		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex != 0;
		}
		
	}
}

