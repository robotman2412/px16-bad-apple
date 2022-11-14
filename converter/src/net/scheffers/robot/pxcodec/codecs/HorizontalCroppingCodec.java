package net.scheffers.robot.pxcodec.codecs;

import net.scheffers.robot.pxcodec.Bitstream;
import net.scheffers.robot.pxcodec.Codec;
import processing.core.PImage;

public class HorizontalCroppingCodec extends Codec {
	
	@Override
	public PImage encode(Bitstream to, PImage image) {
		PImage out = image.copy();
		
		out.loadPixels();
		smartThreshold(out);
		
		// Turn image into columns.
		int[] cols = new int[image.width];
		for (int x = 0; x < out.width; x++) {
			for (int y = 0; y < out.height; y++) {
				if ((out.pixels[y*out.width+x] & 255) > 127) cols[x] |= 1 << y;
			}
		}
		
		// Determine left boundary.
		int     startX = 1;
		boolean startBright = false;
		if (cols[0] == 0xffff || cols[0] == 0x0000) {
			startBright = cols[0] == 0xffff;
			for (int x = 1; x < out.width; x++) {
				startX = x;
				if (cols[x] != cols[0]) break;
			}
		} else {
			startX = 0;
		}
		
		// Determine right boundary.
		int     endX = out.width - 2;
		boolean endBright = false;
		if (cols[out.width-1] == 0xffff || cols[out.width-1] == 0x0000) {
			endBright = cols[out.width-1] == 0xffff;
			for (int x = out.width - 2; x >= 0; x--) {
				endX = x;
				if (cols[x] != cols[out.width-1]) break;
			}
		} else {
			endX = out.width - 1;
		}
		
		// Write start position.
		to.write(startBright);
		to.writeInt(startX, 5);
		// Write end position.
		to.write(endBright);
		to.writeInt(endX, 5);
		// Write pixel data.
		for (int x = startX; x <= endX; x++) {
			to.writeInt(cols[x], 16);
		}
		
		out.updatePixels();
		return out;
	}
	
	@Override
	public PImage decode(Bitstream from) {
		PImage out = new PImage(21, 16);
		out.loadPixels();
		
		// Make a column array.
		int[] cols = new int[21];
		
		// Load start position.
		boolean startBright = from.read();
		int     startX      = from.readInt(5);
		// Load end position.
		boolean endBright   = from.read();
		int     endX        = from.readInt(5);
		
		// Pad start.
		for (int i = 0; i < startX; i++) {
			cols[i] = startBright ? 0xffffffff : 0xff000000;
		}
		// Pad end.
		for (int i = 20; i > endX; i--) {
			cols[i] = endBright ? 0xffffffff : 0xff000000;
		}
		// Read pixel data.
		for (int i = startX; i <= endX; i++) {
			cols[i] = from.readInt(16);
		}
		
		// Convert columns to the image.
		for (int x = 0; x < out.width; x++) {
			for (int y = 0; y < out.height; y++) {
				out.pixels[y*out.width+x] = ((cols[x] >> y) & 1) > 0 ? 0xffffffff : 0xff000000;
			}
		}
		
		out.updatePixels();
		return out;
	}
	
}
