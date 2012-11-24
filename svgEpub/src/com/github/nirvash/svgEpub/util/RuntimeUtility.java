package com.github.nirvash.svgEpub.util;
/*
 * RuntimeUtility.java - GUI for converting raster images to SVG using AutoTrace
 *
 * Copyright (C) 2003 Robert McKinnon
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
// package net.sf.delineate.utility;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime helper methods.
 * @author robmckinnon@users.sourceforge.net
 */
public class RuntimeUtility {
    public static class ProcessStreamReadThread extends Thread {
    	private final BufferedReader br;
    	private final List<String> streamMessageList = new ArrayList<String>();

		public ProcessStreamReadThread(InputStream is) {
			br = new BufferedReader(new InputStreamReader(is));
		}
	
	   @Override
       public void run() {
           try {
               readStream();
           } catch (IOException e) {
               throw new RuntimeException(e);
           }
       }
	   
	   private void readStream() throws IOException {
           try {
               while (true) {
                   String line = br.readLine();
                   if (null == line) {
                       break;
                   }
                   streamMessageList.add(line);
               }
           } finally {
               br.close();
           }
       }
	   
	   public List<String> getStreamMessageList() {
           return streamMessageList;
       }
	   
	   @Override
	   public String toString() {
		   StringBuffer buf = new StringBuffer();
		   for (String mes : streamMessageList) {
			   buf.append(mes);
			   buf.append("\n");
		   }
		   return buf.toString();
	   }

		public void getOutput(StringBuffer msg) {
			   for (String line : streamMessageList) {
				  msg.append(line);
				  msg.append("\n");
			   }
		}
	}
    
	public static int execute(ArrayList<String> command) throws IOException, InterruptedException {
		return execute(command, null);
	}
	
	public static int execute(ArrayList<String> command, StringBuffer msg) throws IOException, InterruptedException {
    	ProcessBuilder pb = new ProcessBuilder(command);
    	pb.redirectErrorStream(true);
    	Process process = pb.start();

    	ProcessStreamReadThread stdoutThread = new ProcessStreamReadThread(process.getInputStream());
    	ProcessStreamReadThread stderrThread = new ProcessStreamReadThread(process.getErrorStream());
    	stdoutThread.start();
    	stderrThread.start();
    	int ret = process.waitFor();
    	stdoutThread.join();
    	stderrThread.join();
    	if (msg != null) {
    		stdoutThread.getOutput(msg);
    		stderrThread.getOutput(msg);
    	}
    	ret |= process.waitFor();
		return ret;
    }
}
