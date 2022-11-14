package net.scheffers.robot.pxcodec;

import processing.core.PImage;

public abstract class Codec {
	
	// Encode an image and return the decoded result.
	public abstract PImage encode(Bitstream to, PImage image);
	
	// Decode an image.
	public abstract PImage decode(Bitstream from);
	
	// Determines whether the image has either more darkness or brightness.
	public boolean isDark(PImage image) {
		int lightcount = 0;
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				if ((image.pixels[y*image.width+x] & 255) > 127) {
					lightcount ++;
				}
			}
		}
		return lightcount < image.width * image.height / 2;
	}
	
	// Threshold filters an image.
	public void threshold(PImage image, int level) {
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				image.pixels[y*image.width+x] = (image.pixels[y*image.width+x] & 255) > level ? 0xffffffff : 0xff000000;
			}
		}
	}
	
	// Smart threshold filter.
	public void smartThreshold(PImage image) {
		int darkLevel  = 96;
		int lightLevel = 255 - darkLevel;
		threshold(image, isDark(image) ? darkLevel : lightLevel);
	}
	
}
