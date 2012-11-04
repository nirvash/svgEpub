import java.awt.Rectangle;
import java.util.ArrayList;

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
	
	public ArrayList<ClipListItem> getClipRect(ListItem item) {
		ArrayList<ClipListItem> list = new ArrayList<ClipListItem>();
		
		Rectangle imageRect = ImageUtility.getImageSize(item);
		if (id == Type.B5_Cover) {
			Rectangle clipRect = new Rectangle(imageRect);
			clipRect.x = imageRect.width / 2 / 3;
			clipRect.width = imageRect.width / 2 / 3 * 2;
			list.add(new ClipListItem(clipRect, "cover"));
		} else if (id == Type.TwoPage) {
			Rectangle clipRectLeft = new Rectangle(imageRect);
			clipRectLeft.width = imageRect.width / 2;

			Rectangle clipRectRight = new Rectangle(imageRect);
			clipRectRight.x = imageRect.width / 2 + 1;
			clipRectRight.width = imageRect.width / 2;

			list.add(new ClipListItem(clipRectRight, "Right"));
			list.add(new ClipListItem(clipRectLeft, "Left"));
		} else if (id == Type.TwoColumns) {
			Rectangle clipRectTop = new Rectangle(imageRect);
			clipRectTop.height = imageRect.height / 2;
	
			Rectangle clipRectBottom = new Rectangle(imageRect);
			clipRectBottom.y = imageRect.height / 2 + 1;
			clipRectBottom.height = imageRect.height / 2;
	
			list.add(new ClipListItem(clipRectTop, "Top"));
			list.add(new ClipListItem(clipRectBottom, "Bottom"));
		}
		return list;
	}
	
	enum Type {
		B5_Cover,
		TwoPage,
		TwoColumns
	}

	public static ArrayList<ClipTemplate> getList() {
		ArrayList<ClipTemplate> list = new ArrayList<ClipTemplate>();
		list.add(new ClipTemplate("Comic B5 Cover", ClipTemplate.Type.B5_Cover));
		list.add(new ClipTemplate("2 Page (Left/Right)", ClipTemplate.Type.TwoPage));
		list.add(new ClipTemplate("2 columns (Top/Bottom)", ClipTemplate.Type.TwoColumns));
		return list;
	}
}