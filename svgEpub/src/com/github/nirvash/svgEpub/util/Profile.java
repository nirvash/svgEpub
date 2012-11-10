package com.github.nirvash.svgEpub.util;

public class Profile {
	private static long laptime = 0;
	
	public static void setLaptime(String message) {
		long laptime = System.currentTimeMillis();
		long diff = laptime - Profile.laptime;
		System.out.format("%8d: %8d: %s\n", laptime, diff, message);
		Profile.laptime = laptime;
	}
}
