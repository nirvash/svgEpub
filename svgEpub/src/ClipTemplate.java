import java.awt.Rectangle;

public class ClipTemplate {
	private String title;
	private Type id;
	public ClipTemplate(String title, Type id) {
		this.title = title;
		this.id = id;
	}
	public String toString() {
		return title;
	}
	public Rectangle getClipRect(ListItem item) {
		Rectangle imageRect = ImageUtil.getImageSize(item);
		Rectangle clipRect = new Rectangle(imageRect);
		if (id == Type.B5_Cover) {
			clipRect.x = imageRect.width / 2 / 3;
			clipRect.width = imageRect.width / 2 / 3 * 2;
		} else if (id == Type.TwoPage) {
			clipRect.width = imageRect.width / 2;
		}
		return clipRect;
	}
	
	enum Type {
		B5_Cover,
		TwoPage		
	};
}