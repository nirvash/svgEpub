import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JFileChooser;
import javax.swing.ProgressMonitor;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;


public class Epub extends Thread  {
	private ArrayList<ListItem> fileList;
	private String title;
	private String author;
	private String path;
	private ProgressMonitor monitor;

	@Override
	public void run() {
		createEpub(this.path, this.monitor);
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public void setMonitor(ProgressMonitor monitor) {
		this.monitor = monitor;
	}
	
	private void createEpub(String path, ProgressMonitor monitor) {
		try {
			Book book = new Book();
			book.getMetadata().addTitle(getTitle());
			book.getMetadata().addAuthor(new Author(getAuthor(), ""));
			book.getMetadata().setPageProgressionDirection("rtl");
			book.getSpine().setPageProgressionDirection("rtl");
			if (!createPages(book, fileList, monitor)) {
				return;
			}
			
			EpubWriter epubWriter = new EpubWriter();
			FileOutputStream out =  new FileOutputStream(path);
			epubWriter.write(book, out);
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
				createSvgPage(book, template, pageName, pageFile, file);
				page++;
			} else if (mainPanel.isImageFile(file)) {
				if (item.isSelected()) {
					File bitmapFile = null;
					File pnmFile = null;
					try {
						bitmapFile = convertToBitmap(file);
						if (bitmapFile == null || !bitmapFile.exists()) continue;
						
						pnmFile = convertToPnm(bitmapFile);
						if (pnmFile == null || !pnmFile.exists()) continue;
						
						File svgFile = convertToSvg(pnmFile);
						if (svgFile != null) {
							createSvgPage(book, template, pageName, pageFile, svgFile);
							page++;
						}
					} finally {
//						if (bitmapFile != null) bitmapFile.delete();
//						if (pnmFile != null) pnmFile.delete();
						
					}
				} else {
					createImagePage(book, template, pageName, pageFile, file);
					page++;
				}
			}
		}	
		return true;
	}


	private File convertToBitmap(File file) {
		String path = file.getParent() + "\\tmp\\";
		String outFilename = path + file.getName();
		outFilename = outFilename.replaceAll("\\.[^.]*$", ".bmp");		
		try {
			FileInputStream in = new FileInputStream(file);
			BufferedImage image = ImageIO.read(in);
			in.close();
			
//			BufferedImage binalized = BilevelUtil.binarize(image);
			
			File dir = new File(path);
			dir.mkdirs();
			OutputStream out = new FileOutputStream(outFilename);
			ImageIO.write(image, "bmp", out);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return new File(outFilename);
	}
	

	private File convertToPnm(File file) {
		String pnmFile = file.getPath().replaceAll("\\.[^.]*$", ".pnm");
		File mkbitmap = new File("thirdparty/mkbitmap.exe");
		if (!mkbitmap.exists()) {
			JFileChooser c = new JFileChooser();
			c.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int ret = c.showOpenDialog(null);
			if (ret == JFileChooser.APPROVE_OPTION) {
				mkbitmap = c.getSelectedFile();
			} else {
				return null;
			}
		}

		// 				"\"%s\" \"%s\" -o \"%s\" -x -s 2 -f 4 -b 1 -1 -t 0.5", 

		String command = String.format(
				"\"%s\" \"%s\" -o \"%s\" -x -s 2 -f 2 -b 2 -1 -t 0.5", 
				mkbitmap.getPath(), file.getPath(), pnmFile
				);
		try {
			RuntimeUtility.execute(command);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new File(pnmFile);
	}

	private File convertToSvg(File file) {
		String svgFile = file.getPath().replaceAll("\\.[^.]*$", ".svg");
		File potrace = new File("thirdparty/potrace.exe");
		if (!potrace.exists()) {
			JFileChooser c = new JFileChooser();
			c.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int ret = c.showOpenDialog(null);
			if (ret == JFileChooser.APPROVE_OPTION) {
				potrace = c.getSelectedFile();
			} else {
				return null;
			}
		}
//				"\"%s\" \"%s\" -o \"%s\" -s -r 167 --tight", 
		
		String command = String.format(
				"\"%s\" \"%s\" -o \"%s\" -s -u 7 -a 0", 
				potrace.getPath(), file.getPath(), svgFile
				);
		try {
			RuntimeUtility.execute(command);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new File(svgFile);
	}

	private void createImagePage(Book book, String template, String pageName,
			String pageFile, File file)  {
		try {
	    	String extension = getExtension(file.getName());
	    	String imageFile = "images/" + pageName + "." + extension;
	    	
	    	Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix(extension);
	        ImageReader imageReader = (ImageReader) readers.next();
	        
	    	FileInputStream stream = new FileInputStream(file);
	        ImageInputStream imageInputStream = ImageIO.createImageInputStream(stream);
	        imageReader.setInput(imageInputStream, false);
	        int width = imageReader.getWidth(0);
	        int height = imageReader.getHeight(0);
	        stream.close();
	        
	    	String tag = String.format("<svg version=\"1.1\" " +
	    			"xmlns=\"http://www.w3.org/2000/svg\" " +
	    			"xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
	    			"width=\"100%%\" height=\"100%%\" viewBox=\"0 0 %d %d\" " +
	    			"preserveAspectRatio=\"xMidYMid meet\">\n" +
	    			"<image width=\"%d\" height=\"%d\" xlink:href=\"%s\"/>\n</svg>", width, height, width, height, imageFile);
			String html = template.replaceAll("%%BODY%%", tag);
			
			ByteArrayInputStream bi = new ByteArrayInputStream(html.getBytes("UTF-8"));
			book.addSection(pageName, new Resource(bi, pageFile));
			
	        stream = new FileInputStream(file);	    	
			book.getResources().add(new Resource(stream, imageFile));
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	private void createSvgPage(Book book, String template, String pageName,
			String pageFile, File file) throws IOException,
			UnsupportedEncodingException {
		try {
	    	String extension = getExtension(file.getName());
	    	String imageFile = "images/" + pageName + "." + extension;
        
			String svg = readFile(file);
//			svg = svg.replaceAll("((<\\?xml.*>)|(<!DOCTYPE((.|\n|\r)*?)\">))", "");

			int width = 584;
			int height = 754;
			
			Pattern pw = Pattern.compile("(<svg(.|\n|\r)*?width=\")(\\d+)(.*\".*)");
			Matcher mw = pw.matcher(svg);
			if (mw.find()) {
				width = Integer.parseInt(mw.group(3));
			}
			
			Pattern ph = Pattern.compile("(<svg(.|\n|\r)*?height=\")(\\d+)(.*\".*)");
			Matcher mh = ph.matcher(svg);
			if (mh.find()) {
				height = Integer.parseInt(mh.group(3));
			}
/*			
			svg = svg.replaceFirst("(<svg(.|\n|\r)*?width=\")(.*?)(\".*)", "$1100%$4");
			svg = svg.replaceFirst("(<svg(.|\n|\r)*?height=\")(.*?)(\".*)", "$1100%$4");
*/			

	    	String tag = String.format("<svg version=\"1.1\" " +
	    			"xmlns=\"http://www.w3.org/2000/svg\" " +
	    			"xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
	    			"width=\"100%%\" height=\"100%%\" viewBox=\"0 0 %d %d\" " +
	    			"preserveAspectRatio=\"xMidYMid meet\">\n" +
	    			"<image width=\"%d\" height=\"%d\" xlink:href=\"%s\"/>\n</svg>", width, height, width, height, imageFile);
			String html = template.replaceAll("%%BODY%%", tag);
			
			ByteArrayInputStream bi = new ByteArrayInputStream(html.getBytes("UTF-8"));
			book.addSection(pageName, new Resource(bi, pageFile));
			bi.close();

//			ByteArrayInputStream bsvg = new ByteArrayInputStream(svg.getBytes("UTF-8"));
//			book.getResources().add(new Resource(bsvg, imageFile));
			FileInputStream stream = new FileInputStream(file);	    	
			book.getResources().add(new Resource(stream, imageFile));
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private void createSvgPage2(Book book, String template, String pageName,
			String pageFile, File file) throws IOException,
			UnsupportedEncodingException {
		String svg = readFile(file);
		svg = svg.replaceAll("((<\\?xml.*>)|(<!DOCTYPE((.|\n|\r)*?)\">))", "");
		svg = svg.replaceFirst("(<svg(.|\n|\r)*?width=\")(.*?)(\".*)", "$1100%$4");
		svg = svg.replaceFirst("(<svg(.|\n|\r)*?height=\")(.*?)(\".*)", "$1100%$4");
		String html = template.replaceAll("%%BODY%%", svg);
		ByteArrayInputStream bi = new ByteArrayInputStream(html.getBytes("UTF-8"));
		book.addSection(pageName, new Resource(bi, pageFile));
		bi.close();
	}

    static String convertInputStreamToString(InputStream is) throws IOException {
        InputStreamReader reader = new InputStreamReader(is);
        StringBuilder builder = new StringBuilder();
        char[] buf = new char[1024];
        int numRead;
        while (0 <= (numRead = reader.read(buf))) {
            builder.append(buf, 0, numRead);
        }
        return builder.toString();
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
    
    public String getExtension(String str) {
        String strs[] = str.split("\\.");
        return strs[strs.length - 1];
    }

	public void setList(ArrayList<ListItem> list) {
		fileList = list;
	}

	public String getTitle() {
		if (title == null || title.length() == 0) {
			return "untitled";
		}
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getAuthor() {
		if (author == null || author.length() == 0) {
			return "unknown";
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
}
