package com.github.nirvash.svgEpub.layout;

public class ReflowPage {
	private StringBuffer css = new StringBuffer();
	private StringBuffer body = new StringBuffer();
	private boolean hasPage = false;
	
	public void appendCss(String css) {
		this.css.append(css);
		hasPage = true;
	}
	
	public String getCss() {
		return css.toString();
	}
	
	public void appendBody(StringBuffer body) {
		this.body.append(body);
		hasPage = true;
	}
	
	public String getBody() {
		return body.toString();
	}
	
	public void clear() {
		css.setLength(0);
		body.setLength(0);
		hasPage = false;
	}
	
	public boolean hasPage() {
		return hasPage;
	}
}
