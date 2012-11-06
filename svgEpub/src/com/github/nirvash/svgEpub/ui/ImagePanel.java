package com.github.nirvash.svgEpub.ui;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


public class ImagePanel extends JPanel {
	private static final long serialVersionUID = 9034023248282511515L;
	private Image img = null;
	private JScrollPane parent = null;
	private int width = 0;
	private int height = 0;

    public ImagePanel() {
    }

    @Override
    protected void  paintComponent(Graphics g)
    {
        super.paintComponents(g);
        if (img != null) {
	        Graphics2D g2d = (Graphics2D) g;
	        g2d.drawImage(img,0,0,width,height,null);
        }
    }

	public void setImage(File file) {
		if (file == null) {
			img = null;
			if (parent != null) {
				parent.doLayout();
			}
		} else {
			try {
				img = ImageIO.read(file);
				width = img.getWidth(null);
				height = img.getHeight(null);				
				setPreferredSize(new Dimension(width, height));
				if (parent != null) {
					parent.doLayout();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void setParent(JScrollPane jScrollPane0) {
		parent = jScrollPane0;
	}
}
