package net.scheffers.robot.pxcodec.codecs;

import net.scheffers.robot.pxcodec.Bitstream;
import net.scheffers.robot.pxcodec.Codec;
import processing.core.PImage;

public class RawCodec extends Codec {
	
	public int threshold(int in) {
		int r = (in >> 16) & 255;
		return r > 96 ? 0xffffffff : 0xff000000;
	}
	
	@Override
	public PImage encode(Bitstream to, PImage image) {
		PImage out = image.copy();
		
		out.loadPixels();
		smartThreshold(out);
		
		// Write to bitstream.
		for (int y = 0; y < out.height; y++) {
			for (int x = 0; x < out.width; x++) {
				to.write((out.pixels[y*out.width+x] & 255) > 127);
			}
		}
		
		out.updatePixels();
		return out;
	}
	
	@Override
	public PImage decode(Bitstream from) {
		return null;
	}
	
}
