import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_objdetect;


public class ImageUtil {
	static {
		Loader.load(opencv_objdetect.class);
	}
}
