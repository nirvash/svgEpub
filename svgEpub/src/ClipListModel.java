import javax.swing.DefaultListModel;

public class ClipListModel extends DefaultListModel {
	private boolean isUpdating = false;
	public void notifyModelUpdate(int begin, int end) {
		this.fireContentsChanged(this, begin, end);
	}

	public void setUpdating(boolean b) {
		isUpdating = b;
	}

	public boolean isUpdating() {
		return isUpdating;
	}
}
