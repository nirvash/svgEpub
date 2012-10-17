import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JFileChooser;
import javax.swing.ProgressMonitor;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;
import nl.siegmann.epublib.util.StringUtil;


public class Epub {
	private ArrayList<ListItem> fileList;
	private String title;
	private String author;
	private String path;
	private String outputFilename;
	private ProgressMonitor monitor;
	static private CustomProperties properties;
	
	private class WorkThread extends Thread {
		@Override
		public void run() {
			createEpub(Epub.this.path, Epub.this.monitor);
		}
	}

	public void start() {
		Thread th = new WorkThread();
		th.start();
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public void setMonitor(ProgressMonitor monitor) {
		this.monitor = monitor;
	}
	
	static public void setProperty(CustomProperties properties) {
		Epub.properties = properties;
	}
	
	private void createEpub(String path, ProgressMonitor monitor) {
		try {
			Book book = new Book();
			book.getMetadata().addTitle(getTitle());
			book.getMetadata().addAuthor(new Author(getAuthor(), ""));
			book.getMetadata().setPageProgressionDirection(properties.getProperty("pageProgressionDirection"));
			book.getSpine().setPageProgressionDirection(properties.getProperty("pageProgressionDirection"));
			if (!createPages(book, fileList, monitor)) {
				return;
			}
			
			EpubWriter epubWriter = new EpubWriter();
			FileOutputStream out =  new FileOutputStream(path);
			epubWriter.write(book, out);
			monitor.setProgress(monitor.getMaximum());
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	private boolean createPages(Book book, ArrayList<ListItem> list, ProgressMonitor monitor) throws IOException {
		InputStream is = mainPanel.class.getResourceAsStream("/resources/page_template.xhtml");
		String template = convertInputStreamToString(is);
		
		int page = 1;

		for (ListItem item : fileList) {
			monitor.setProgress(page-1);
			if ( monitor.isCanceled() ) {
				monitor.close();
				return false;
			}
			String pageName = String.format("page_%04d", page);
			String pageFile = pageName + ".xhtml";
			File file = item.getFile();
			if (mainPanel.isSvgFile(file)) {
				createSvgPage(book, template, pageName, pageFile, file, item.getClipRect());
				page++;
			} else if (mainPanel.isImageFile(file)) {
				if (item.isSelected()) {
					File svgFile = convertToSvgFromImage(file);
					if (svgFile != null) {
						createSvgPage(book, template, pageName, pageFile, svgFile, item.getClipRect());
						svgFile.delete();
						page++;
					} else {
						createImagePage(book, template, pageName, pageFile, file, item.getClipRect());
						page++;
					}
				} else {
					createImagePage(book, template, pageName, pageFile, file, item.getClipRect());
					page++;
				}
			}
		}	
		return true;
	}
	
	static public File convertToSvgFromImage(File imageFile) {
		File bitmapFile = null;
		File pnmFile = null;
		Rectangle imageSize = new Rectangle();
		try {
			if (properties.getProperty("enable_opencv", "no").equals("yes")) {
				pnmFile = ImageUtil.convertToBitmap(imageFile, imageSize);
			} else {
				bitmapFile = convertToBitmap(imageFile, imageSize);
				if (bitmapFile == null || !bitmapFile.exists()) return null;
			
				pnmFile = convertToPnm(bitmapFile);
			}
			if (pnmFile == null || !pnmFile.exists()) return null;
		
			return convertToSvg(pnmFile, imageSize);
		} finally {
			if (pnmFile != null) pnmFile.delete();
			if (bitmapFile != null) bitmapFile.delete();
		}
	}


	static private File convertToBitmap(File file, Rectangle imageSize) {
		String path = getTmpDirectory();
		String outFilename = path + file.getName();
		outFilename = outFilename.replaceAll("\\.[^.]*$", ".bmp");		
		try {
			FileInputStream in = new FileInputStream(file);
			BufferedImage image = ImageIO.read(in);
			in.close();

			if (imageSize != null) {
				imageSize.width = image.getWidth();
				imageSize.height = image.getHeight();
			}

			OutputStream out = new FileOutputStream(outFilename);
			ImageIO.write(image, "bmp", out);
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		File outFile = new File(outFilename);
		outFile.deleteOnExit();
		return outFile;
	}
	

	static private String getTmpDirectory() {
		String path = System.getProperty("java.io.tmpdir") + "/svgEpub/";
		File tmp = new File(path);
		if (!tmp.exists()) {
			tmp.mkdirs();
		}
		return path;
	}

	static private File convertToPnm(File file) {
		String path = getTmpDirectory();
		String pnmFile = path + file.getName();
		pnmFile = pnmFile.replaceAll("\\.[^.]*$", ".pnm");
		File mkbitmap = getMkbitmapFile();
		if (mkbitmap == null || !mkbitmap.exists()) {
			JFileChooser c = new JFileChooser();
			c.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int ret = c.showOpenDialog(null);
			if (ret == JFileChooser.APPROVE_OPTION) {
				mkbitmap = c.getSelectedFile();
			} else {
				return null;
			}
		}

		String command = String.format(
				"\"%s\" \"%s\" -o \"%s\" " + properties.getProperty("mkbitmap_option"), 
				mkbitmap.getPath(), file.getPath(), pnmFile
				);
		try {
			RuntimeUtility.execute(command);
		} catch (Exception e) {
			e.printStackTrace();
		}
		File outFile = new File(pnmFile);
		outFile.deleteOnExit();
		return outFile;
	}
	
	static private File getMkbitmapFile() {
		try {
			if(System.getenv("ProgramFiles(x86)") != null) {
				return new File(properties.getProperty("mkbitmap_path_win64"));
			}
			else {
				return new File(properties.getProperty("mkbitmap_path_win32"));			
			}
		} catch (Exception e) {
			return null;
		}
	}


	static private File convertToSvg(File file, Rectangle imageSize) {
		String path = getTmpDirectory();		
		String svgFile = path + file.getName();
		svgFile = svgFile.replaceAll("\\.[^.]*$", ".svg");
		File potrace = getPotraceFile();
		if (potrace == null || !potrace.exists()) {
			return null;
		}
		
		String command = String.format(
				"\"%s\" \"%s\" -o \"%s\" -W %dpt -H %dpt %s", 
				potrace.getPath(), file.getPath(), svgFile, imageSize.width, imageSize.height, 
				properties.getProperty("potrace_option")
				);
		try {
			RuntimeUtility.execute(command);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		File outFile = new File(svgFile);
		outFile.deleteOnExit();
		return outFile;
	}

	static private File getPotraceFile() {
		try {
			if(System.getenv("ProgramFiles(x86)") != null) {
				return new File(properties.getProperty("potrace_path_win64"));
			}
			else {
				return new File(properties.getProperty("potrace_path_win32"));			
			}
		} catch (Exception e) {
			return null;
		}
	}

	private void createImagePage(Book book, String template, String pageName,
			String pageFile, File file, Rectangle clipRect)  {
		try {
	    	String extension = PathUtil.getExtension(file.getName());
	    	String imageURI = "images/" + pageName + "." + extension;
	    	
			Rectangle imageRect = ImageUtil.getImageSize(file);
			Document doc = ImageUtil.createSvgDocument(clipRect, imageRect, imageURI, true);
			doc.normalizeDocument();

			String svgTag = serializeDocument(doc);
			String html = template.replaceAll("%%BODY%%", svgTag);
			
			ByteArrayInputStream bi = new ByteArrayInputStream(html.getBytes("UTF-8"));
			book.addSection(pageName, new Resource(bi, pageFile));
			
	        FileInputStream stream = new FileInputStream(file);	    	
			book.getResources().add(new Resource(stream, imageURI));
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	private void createSvgPage(Book book, String template, String pageName,
			String pageFile, File file, Rectangle clipRect) throws IOException,
			UnsupportedEncodingException {
		try {
	    	String extension = PathUtil.getExtension(file.getName());
	    	String imageURI = "images/" + pageName + "." + extension;
        
	    	Rectangle imageRect = ImageUtil.getSvgSize(file.toURI().toString());
			Document doc = ImageUtil.createSvgDocument(clipRect, imageRect, imageURI, true);
			doc.normalizeDocument();

			String svgTag = serializeDocument(doc);
			String html = template.replaceAll("%%BODY%%", svgTag);
			
			ByteArrayInputStream bi = new ByteArrayInputStream(html.getBytes("UTF-8"));
			book.addSection(pageName, new Resource(bi, pageFile));
			bi.close();

			FileInputStream stream = new FileInputStream(file);	    	
			book.getResources().add(new Resource(stream, imageURI));
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String serializeDocument(Document doc) {
		try {
			//set up a transformer
			TransformerFactory transfac = TransformerFactory.newInstance();
			Transformer trans = transfac.newTransformer();
			trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
	
			//create string from xml tree
			StringWriter sw = new StringWriter();
			StreamResult result = new StreamResult(sw);
			DOMSource source = new DOMSource(doc);
			trans.transform(source, result);
			return sw.toString();		
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return null;
	}


    static String convertInputStreamToString(InputStream is) throws IOException {
        InputStreamReader reader = new InputStreamReader(is);
        try {
	        StringBuilder builder = new StringBuilder();
	        char[] buf = new char[1024];
	        int numRead;
	        while (0 <= (numRead = reader.read(buf))) {
	            builder.append(buf, 0, numRead);
	        }
	        return builder.toString();
        } finally {
            reader.close();
        }
    }
    
    private static String readFile(File file) throws IOException {
    	FileInputStream stream = new FileInputStream(file);
    	try {
    		FileChannel fc = stream.getChannel();
    		MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
    		/* Instead of using default, pass in a decoder. */
    		return Charset.defaultCharset().decode(bb).toString();
    	}
    	finally {
    		stream.close();
    	}
	}
    
	public void setList(ArrayList<ListItem> list) {
		fileList = list;
	}

	public String getTitle() {
		if (title == null || title.length() == 0) {
			return "";
		}
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getAuthor() {
		if (author == null || author.length() == 0) {
			return "";
		}
		return author;
	}
	
	public void setAuthor(String author) {
		this.author = author;
	}

	public int getTotalPage() {
		if (fileList == null) return 0;
		return fileList.size();
	}

	public String getOutputFilename() {
		return this.outputFilename;
	}

	public void analyzerTitleAndPath() {
		this.title = "";
		this.author = "";
		this.outputFilename = "foobar.epub";
		
		if (fileList.isEmpty()) {
			return;
		}
		
		File dir = fileList.get(0).getFile().getParentFile();
		if (dir == null) {
			return;
		}
	
		String dirName = dir.getName();
		this.outputFilename = dirName + ".epub";
		int titleGroup = 0;
		int authorGroup = 0;
		try {
			String regex1 = properties.getProperty("title_author_regex_1");
			if (StringUtil.isNotBlank(properties.getProperty("title_capturing_group_1"))) {
				titleGroup = Integer.parseInt(properties.getProperty("title_capturing_group_1"));
			}
			if (StringUtil.isNotBlank(properties.getProperty("author_capturing_group_1"))) {
				authorGroup = Integer.parseInt(properties.getProperty("author_capturing_group_1"));
			}
			Pattern p1 = Pattern.compile(regex1);
			Matcher m1 = p1.matcher(dirName);
			if (m1.find() && m1.groupCount() >= Math.max(titleGroup, authorGroup)) {
				if (titleGroup > 0) {
					this.title = m1.group(titleGroup);
				}
				if (authorGroup > 0) {
					this.author = m1.group(authorGroup);
				}
				return;
			}
			
			String regex2 = properties.getProperty("title_author_regex_2");
			titleGroup = 0;
			authorGroup = 0;
			if (StringUtil.isNotBlank(properties.getProperty("title_capturing_group_2"))) {
				titleGroup = Integer.parseInt(properties.getProperty("title_capturing_group_2"));
			}
			if (StringUtil.isNotBlank(properties.getProperty("author_capturing_group_2"))) {
				authorGroup = Integer.parseInt(properties.getProperty("author_capturing_group_2"));
			}
			Pattern p2 = Pattern.compile(regex2);
			Matcher m2 = p2.matcher(dirName);
			if (m2.find() && m2.groupCount() >= Math.max(titleGroup, authorGroup)) {
				if (titleGroup > 0) {
					this.title = m2.group(titleGroup);
				}
				if (authorGroup > 0) {
					this.author = m2.group(authorGroup);
				}
				return;
			}
		} catch (Exception e) {
			this.title = dirName;
		}		
	}
}
