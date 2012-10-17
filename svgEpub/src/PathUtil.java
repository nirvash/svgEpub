
public class PathUtil {
	static public String getExtension(String str) {
        String strs[] = str.split("\\.");
        return strs[strs.length - 1];
    }
}
