import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.util.*;
import java.io.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;


public class CustomProperties extends LinkedHashMap<Object, Object> {
	private static final long serialVersionUID = 4112578634029874840L;

	protected CustomProperties defaults;

	public CustomProperties() {
		this(null);
	}

	public CustomProperties(CustomProperties defaults) {
		this.defaults = defaults;
	}

	public synchronized Object setProperty(String key, String value) {
		return put(key, value);
	}

	public synchronized void load(Reader reader) throws IOException {
		load0(new LineReader(reader));
	}

	public synchronized void load(InputStream inStream) throws IOException {
		load0(new LineReader(inStream));
	}

	private void load0(LineReader lr) throws IOException {
		char[] convtBuf = new char[1024];
		int limit;
		int keyLen;
		int valueStart;
		char c;
		boolean hasSep;
		boolean precedingBackslash;

		while ((limit = lr.readLine()) >= 0) {
			c = 0;
			keyLen = 0;
			valueStart = limit;
			hasSep = false;

			// System.out.println("line=<" + new String(lineBuf, 0, limit) +
			// ">");
			precedingBackslash = false;
			while (keyLen < limit) {
				c = lr.lineBuf[keyLen];
				// need check if escaped.
				if ((c == '=' || c == ':') && !precedingBackslash) {
					valueStart = keyLen + 1;
					hasSep = true;
					break;
				} else if ((c == ' ' || c == '\t' || c == '\f')
						&& !precedingBackslash) {
					valueStart = keyLen + 1;
					break;
				}
				if (c == '\\') {
					precedingBackslash = !precedingBackslash;
				} else {
					precedingBackslash = false;
				}
				keyLen++;
			}
			while (valueStart < limit) {
				c = lr.lineBuf[valueStart];
				if (c != ' ' && c != '\t' && c != '\f') {
					if (!hasSep && (c == '=' || c == ':')) {
						hasSep = true;
					} else {
						break;
					}
				}
				valueStart++;
			}
			String key = loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
			String value = loadConvert(lr.lineBuf, valueStart, limit
					- valueStart, convtBuf);
			put(key, value);
		}
	}

	class LineReader {
		public LineReader(InputStream inStream) {
			this.inStream = inStream;
			inByteBuf = new byte[8192];
		}

		public LineReader(Reader reader) {
			this.reader = reader;
			inCharBuf = new char[8192];
		}

		byte[] inByteBuf;
		char[] inCharBuf;
		char[] lineBuf = new char[1024];
		int inLimit = 0;
		int inOff = 0;
		InputStream inStream;
		Reader reader;

		int readLine() throws IOException {
			int len = 0;
			char c = 0;

			boolean skipWhiteSpace = true;
			boolean isCommentLine = false;
			boolean isNewLine = true;
			boolean appendedLineBegin = false;
			boolean precedingBackslash = false;
			boolean skipLF = false;

			while (true) {
				if (inOff >= inLimit) {
					inLimit = (inStream == null) ? reader.read(inCharBuf)
							: inStream.read(inByteBuf);
					inOff = 0;
					if (inLimit <= 0) {
						if (len == 0 || isCommentLine) {
							return -1;
						}
						return len;
					}
				}
				if (inStream != null) {
					// The line below is equivalent to calling a
					// ISO8859-1 decoder.
					c = (char) (0xff & inByteBuf[inOff++]);
				} else {
					c = inCharBuf[inOff++];
				}
				if (skipLF) {
					skipLF = false;
					if (c == '\n') {
						continue;
					}
				}
				if (skipWhiteSpace) {
					if (c == ' ' || c == '\t' || c == '\f') {
						continue;
					}
					if (!appendedLineBegin && (c == '\r' || c == '\n')) {
						continue;
					}
					skipWhiteSpace = false;
					appendedLineBegin = false;
				}
				if (isNewLine) {
					isNewLine = false;
					if (c == '#' || c == '!') {
						isCommentLine = true;
						continue;
					}
				}

				if (c != '\n' && c != '\r') {
					lineBuf[len++] = c;
					if (len == lineBuf.length) {
						int newLength = lineBuf.length * 2;
						if (newLength < 0) {
							newLength = Integer.MAX_VALUE;
						}
						char[] buf = new char[newLength];
						System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
						lineBuf = buf;
					}
					// flip the preceding backslash flag
					if (c == '\\') {
						precedingBackslash = !precedingBackslash;
					} else {
						precedingBackslash = false;
					}
				} else {
					// reached EOL
					if (isCommentLine || len == 0) {
						isCommentLine = false;
						isNewLine = true;
						skipWhiteSpace = true;
						len = 0;
						continue;
					}
					if (inOff >= inLimit) {
						inLimit = (inStream == null) ? reader.read(inCharBuf)
								: inStream.read(inByteBuf);
						inOff = 0;
						if (inLimit <= 0) {
							return len;
						}
					}
					if (precedingBackslash) {
						len -= 1;
						// skip the leading whitespace characters in following
						// line
						skipWhiteSpace = true;
						appendedLineBegin = true;
						precedingBackslash = false;
						if (c == '\r') {
							skipLF = true;
						}
					} else {
						return len;
					}
				}
			}
		}
	}

	private String loadConvert(char[] in, int off, int len, char[] convtBuf) {
		if (convtBuf.length < len) {
			int newLen = len * 2;
			if (newLen < 0) {
				newLen = Integer.MAX_VALUE;
			}
			convtBuf = new char[newLen];
		}
		char aChar;
		char[] out = convtBuf;
		int outLen = 0;
		int end = off + len;

		while (off < end) {
			aChar = in[off++];
			if (aChar == '\\') {
				aChar = in[off++];
				if (aChar == 'u') {
					// Read the xxxx
					int value = 0;
					for (int i = 0; i < 4; i++) {
						aChar = in[off++];
						switch (aChar) {
						case '0':
						case '1':
						case '2':
						case '3':
						case '4':
						case '5':
						case '6':
						case '7':
						case '8':
						case '9':
							value = (value << 4) + aChar - '0';
							break;
						case 'a':
						case 'b':
						case 'c':
						case 'd':
						case 'e':
						case 'f':
							value = (value << 4) + 10 + aChar - 'a';
							break;
						case 'A':
						case 'B':
						case 'C':
						case 'D':
						case 'E':
						case 'F':
							value = (value << 4) + 10 + aChar - 'A';
							break;
						default:
							throw new IllegalArgumentException(
									"Malformed \\uxxxx encoding.");
						}
					}
					out[outLen++] = (char) value;
				} else {
					if (aChar == 't')
						aChar = '\t';
					else if (aChar == 'r')
						aChar = '\r';
					else if (aChar == 'n')
						aChar = '\n';
					else if (aChar == 'f')
						aChar = '\f';
					out[outLen++] = aChar;
				}
			} else {
				out[outLen++] = (char) aChar;
			}
		}
		return new String(out, 0, outLen);
	}

	private String saveConvert(String theString, boolean escapeSpace,
			boolean escapeUnicode) {
		int len = theString.length();
		int bufLen = len * 2;
		if (bufLen < 0) {
			bufLen = Integer.MAX_VALUE;
		}
		StringBuffer outBuffer = new StringBuffer(bufLen);

		for (int x = 0; x < len; x++) {
			char aChar = theString.charAt(x);
			// Handle common case first, selecting largest block that
			// avoids the specials below
			if ((aChar > 61) && (aChar < 127)) {
				if (aChar == '\\') {
					outBuffer.append('\\');
					outBuffer.append('\\');
					continue;
				}
				outBuffer.append(aChar);
				continue;
			}
			switch (aChar) {
			case ' ':
				if (x == 0 || escapeSpace)
					outBuffer.append('\\');
				outBuffer.append(' ');
				break;
			case '\t':
				outBuffer.append('\\');
				outBuffer.append('t');
				break;
			case '\n':
				outBuffer.append('\\');
				outBuffer.append('n');
				break;
			case '\r':
				outBuffer.append('\\');
				outBuffer.append('r');
				break;
			case '\f':
				outBuffer.append('\\');
				outBuffer.append('f');
				break;
			case '=': // Fall through
			case ':': // Fall through
			case '#': // Fall through
			case '!':
				outBuffer.append('\\');
				outBuffer.append(aChar);
				break;
			default:
				if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
					outBuffer.append('\\');
					outBuffer.append('u');
					outBuffer.append(toHex((aChar >> 12) & 0xF));
					outBuffer.append(toHex((aChar >> 8) & 0xF));
					outBuffer.append(toHex((aChar >> 4) & 0xF));
					outBuffer.append(toHex(aChar & 0xF));
				} else {
					outBuffer.append(aChar);
				}
			}
		}
		return outBuffer.toString();
	}

	private static void writeComments(BufferedWriter bw, String comments)
			throws IOException {
		bw.write("#");
		int len = comments.length();
		int current = 0;
		int last = 0;
		char[] uu = new char[6];
		uu[0] = '\\';
		uu[1] = 'u';
		while (current < len) {
			char c = comments.charAt(current);
			if (c > 'ÿ' || c == '\n' || c == '\r') {
				if (last != current)
					bw.write(comments.substring(last, current));
				if (c > 'ÿ') {
					uu[2] = toHex((c >> 12) & 0xf);
					uu[3] = toHex((c >> 8) & 0xf);
					uu[4] = toHex((c >> 4) & 0xf);
					uu[5] = toHex(c & 0xf);
					bw.write(new String(uu));
				} else {
					bw.newLine();
					if (c == '\r' && current != len - 1
							&& comments.charAt(current + 1) == '\n') {
						current++;
					}
					if (current == len - 1
							|| (comments.charAt(current + 1) != '#' && comments
									.charAt(current + 1) != '!'))
						bw.write("#");
				}
				last = current + 1;
			}
			current++;
		}
		if (last != current)
			bw.write(comments.substring(last, current));
		bw.newLine();
	}

	public void store(Writer writer, String comments) throws IOException {
		store0((writer instanceof BufferedWriter) ? (BufferedWriter) writer
				: new BufferedWriter(writer), comments, false);
	}

	public void store(OutputStream out, String comments) throws IOException {
		store0(new BufferedWriter(new OutputStreamWriter(out, "8859_1")),
				comments, true);
	}

	private void store0(BufferedWriter bw, String comments, boolean escUnicode)
			throws IOException {
		if (comments != null) {
			writeComments(bw, comments);
		}
		bw.write("#" + new Date().toString());
		bw.newLine();
		synchronized (this) {
			for (Object obj : keySet()) {
				String key = (String) obj;
				String val = (String) get(key);
				key = saveConvert(key, true, escUnicode);
				/*
				 * No need to escape embedded and trailing spaces for value,
				 * hence pass false to flag.
				 */
				val = saveConvert(val, false, escUnicode);
				bw.write(key + "=" + val);
				bw.newLine();
			}
		}
		bw.flush();
	}

	public synchronized void loadFromXML(InputStream in) throws IOException,
			InvalidPropertiesFormatException {
		if (in == null)
			throw new NullPointerException();
		XMLUtils.load(this, in);
		in.close();
	}

	public synchronized void storeToXML(OutputStream os, String comment)
			throws IOException {
		if (os == null)
			throw new NullPointerException();
		storeToXML(os, comment, "UTF-8");
	}

	public synchronized void storeToXML(OutputStream os, String comment,
			String encoding) throws IOException {
		if (os == null)
			throw new NullPointerException();
		XMLUtils.save(this, os, comment, encoding);
	}

	public String getProperty(String key) {
		Object oval = super.get(key);
		String sval = (oval instanceof String) ? (String) oval : null;
		return ((sval == null) && (defaults != null)) ? defaults
				.getProperty(key) : sval;
	}

	public String getProperty(String key, String defaultValue) {
		String val = getProperty(key);
		return (val == null) ? defaultValue : val;
	}

	public Enumeration<?> propertyNames() {
		Hashtable<?, ?> h = new Hashtable<Object, Object>();
		enumerate(h);
		return h.keys();
	}

	public Set<String> stringPropertyNames() {
		Hashtable<String, String> h = new Hashtable<String, String>();
		enumerateStringProperties(h);
		return h.keySet();
	}

	public void list(PrintStream out) {
		out.println("-- listing properties --");
		Hashtable<?, ?> h = new Hashtable<Object, Object>();
		enumerate(h);
		for (Enumeration<?> e = h.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			String val = (String) h.get(key);
			if (val.length() > 40) {
				val = val.substring(0, 37) + "...";
			}
			out.println(key + "=" + val);
		}
	}

	public void list(PrintWriter out) {
		out.println("-- listing properties --");
		Hashtable<?, ?> h = new Hashtable<Object, Object>();
		enumerate(h);
		for (Enumeration<?> e = h.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			String val = (String) h.get(key);
			if (val.length() > 40) {
				val = val.substring(0, 37) + "...";
			}
			out.println(key + "=" + val);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private synchronized void enumerate(Hashtable h) {
		if (defaults != null) {
			defaults.enumerate(h);
		}
		for (Object obj : keySet()) {
			String key = (String) obj;
			h.put(key, get(key));
		}
	}

	private synchronized void enumerateStringProperties(
			Hashtable<String, String> h) {
		if (defaults != null) {
			defaults.enumerateStringProperties(h);
		}
		for (Object obj : keySet()) {
			Object k = obj;
			Object v = get(k);
			if (k instanceof String && v instanceof String) {
				h.put((String) k, (String) v);
			}
		}
	}

	private static char toHex(int nibble) {
		return hexDigit[(nibble & 0xF)];
	}

	/** A table of hex digits */
	private static final char[] hexDigit = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	static class XMLUtils {
		// XML loading and saving methods for Properties

		// The required DTD URI for exported properties
		private static final String PROPS_DTD_URI = "http://java.sun.com/dtd/properties.dtd";

		private static final String PROPS_DTD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<!-- DTD for properties -->"
				+ "<!ELEMENT properties ( comment?, entry* ) >"
				+ "<!ATTLIST properties"
				+ " version CDATA #FIXED \"1.0\">"
				+ "<!ELEMENT comment (#PCDATA) >"
				+ "<!ELEMENT entry (#PCDATA) >"
				+ "<!ATTLIST entry "
				+ " key CDATA #REQUIRED>";

		/**
		 * Version number for the format of exported properties files.
		 */
		private static final String EXTERNAL_XML_VERSION = "1.0";

		static void load(CustomProperties props, InputStream in)
				throws IOException, InvalidPropertiesFormatException {
			Document doc = null;
			try {
				doc = getLoadingDoc(in);
			} catch (SAXException saxe) {
				throw new InvalidPropertiesFormatException(saxe);
			}
			Element propertiesElement = (Element) doc.getChildNodes().item(1);
			String xmlVersion = propertiesElement.getAttribute("version");
			if (xmlVersion.compareTo(EXTERNAL_XML_VERSION) > 0)
				throw new InvalidPropertiesFormatException(
						"Exported Properties file format version "
								+ xmlVersion
								+ " is not supported. This java installation can read"
								+ " versions "
								+ EXTERNAL_XML_VERSION
								+ " or older. You"
								+ " may need to install a newer version of JDK.");
			importProperties(props, propertiesElement);
		}

		static Document getLoadingDoc(InputStream in) throws SAXException,
				IOException {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringElementContentWhitespace(true);
			dbf.setValidating(true);
			dbf.setCoalescing(true);
			dbf.setIgnoringComments(true);
			try {
				DocumentBuilder db = dbf.newDocumentBuilder();
				db.setEntityResolver(new Resolver());
				db.setErrorHandler(new EH());
				InputSource is = new InputSource(in);
				return db.parse(is);
			} catch (ParserConfigurationException x) {
				throw new Error(x);
			}
		}

		static void importProperties(CustomProperties props,
				Element propertiesElement) {
			NodeList entries = propertiesElement.getChildNodes();
			int numEntries = entries.getLength();
			int start = numEntries > 0
					&& entries.item(0).getNodeName().equals("comment") ? 1 : 0;
			for (int i = start; i < numEntries; i++) {
				Element entry = (Element) entries.item(i);
				if (entry.hasAttribute("key")) {
					Node n = entry.getFirstChild();
					String val = (n == null) ? "" : n.getNodeValue();
					props.setProperty(entry.getAttribute("key"), val);
				}
			}
		}

		static void save(CustomProperties props, OutputStream os,
				String comment, String encoding) throws IOException {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = null;
			try {
				db = dbf.newDocumentBuilder();
			} catch (ParserConfigurationException pce) {
				assert (false);
			}
			Document doc = db.newDocument();
			Element properties = (Element) doc.appendChild(doc
					.createElement("properties"));

			if (comment != null) {
				Element comments = (Element) properties.appendChild(doc
						.createElement("comment"));
				comments.appendChild(doc.createTextNode(comment));
			}

			Set<?> keys = props.keySet();
			Iterator<?> i = keys.iterator();
			while (i.hasNext()) {
				String key = (String) i.next();
				Element entry = (Element) properties.appendChild(doc
						.createElement("entry"));
				entry.setAttribute("key", key);
				entry.appendChild(doc.createTextNode(props.getProperty(key)));
			}
			emitDocument(doc, os, encoding);
		}

		static void emitDocument(Document doc, OutputStream os, String encoding)
				throws IOException {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer t = null;
			try {
				t = tf.newTransformer();
				t.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, PROPS_DTD_URI);
				t.setOutputProperty(OutputKeys.INDENT, "yes");
				t.setOutputProperty(OutputKeys.METHOD, "xml");
				t.setOutputProperty(OutputKeys.ENCODING, encoding);
			} catch (TransformerConfigurationException tce) {
				assert (false);
			}
			DOMSource doms = new DOMSource(doc);
			StreamResult sr = new StreamResult(os);
			try {
				t.transform(doms, sr);
			} catch (TransformerException te) {
				IOException ioe = new IOException();
				ioe.initCause(te);
				throw ioe;
			}
		}

		private static class Resolver implements EntityResolver {
			public InputSource resolveEntity(String pid, String sid)
					throws SAXException {
				if (sid.equals(PROPS_DTD_URI)) {
					InputSource is;
					is = new InputSource(new StringReader(PROPS_DTD));
					is.setSystemId(PROPS_DTD_URI);
					return is;
				}
				throw new SAXException("Invalid system identifier: " + sid);
			}
		}

		private static class EH implements ErrorHandler {
			public void error(SAXParseException x) throws SAXException {
				throw x;
			}

			public void fatalError(SAXParseException x) throws SAXException {
				throw x;
			}

			public void warning(SAXParseException x) throws SAXException {
				throw x;
			}
		}

	}
}