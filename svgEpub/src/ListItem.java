import java.io.File;


public class ListItem {
	private File file;
	private File svgFile = null;
	private boolean isSelected = false;
	private boolean enableSelect = true;
	
	public ListItem(File value) {
		this.file = value;
		if (mainPanel.isImageFile(file)) {
			this.isSelected = true;
		} else if (mainPanel.isSvgFile(value)) {
			this.enableSelect = false;
		}
	}
	
	public boolean enableSelect() {
		return this.enableSelect;
	}
	
	public boolean isSelected() {
		return isSelected;
	}
	
	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}
	
	public String toString() {
		return file.getName();
	}

	public File getFile() {
		return file;
	}
	
	public File getSvgFile() {
		return svgFile;
	}
	
	public void setSvgFile(File file) {
		this.svgFile = file;
	}

}
