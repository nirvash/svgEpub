import java.io.File;


public class ListItem {
	private File file;
	private boolean isSelected = false;
	
	public ListItem(File value) {
		this.file = value;
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

}
