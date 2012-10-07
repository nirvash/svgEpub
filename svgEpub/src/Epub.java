import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;


public class Epub {
	public void createEpub(Enumeration<File> list) {
		try {
			Book book = new Book();
			book.getMetadata().addTitle("Epub Test");
			book.getMetadata().addAuthor(new Author("test", "test2"));
			createPages(book, list);
			
			EpubWriter epubWriter = new EpubWriter();
			FileOutputStream out =  new FileOutputStream("test.epub");
			epubWriter.write(book, out);
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	private void createPages(Book book, Enumeration<File> list) throws IOException {
		InputStream is = mainPanel.class.getResourceAsStream("/resources/page_template.xhtml");
		String template = convertInputStreamToString(is);
		
		int page = 1;

		while (list.hasMoreElements()) {
			String pageName = String.format("page_%04d", page);
			String pageFile = pageName + ".xhtml";
			File file = list.nextElement();
			if (mainPanel.isSvgFile(file)) {
				createSvgPage(book, template, pageName, pageFile, file);
				page++;
			} else if (mainPanel.isImageFile(file)) {
				createImagePage(book, template, pageName, pageFile, file);
				page++;
			}
		}		
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	

	private void createSvgPage(Book book, String template, String pageName,
			String pageFile, File file) throws IOException,
			UnsupportedEncodingException {
		String svg = readFile(file);
		svg = svg.replaceAll("((<\\?xml.*>)|(<!DOCTYPE((.|\n|\r)*?)\">))", "");
		svg = svg.replaceFirst("(<svg(.|\n|\r)*?width=\")(.*?)(\".*)", "$1100%$4");
		svg = svg.replaceFirst("(<svg(.|\n|\r)*?height=\")(.*?)(\".*)", "$1100%$4");
		String html = template.replaceAll("%%BODY%%", svg);
		ByteArrayInputStream bi = new ByteArrayInputStream(html.getBytes("UTF-8"));
		book.addSection(pageName, new Resource(bi, pageFile));
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
}
