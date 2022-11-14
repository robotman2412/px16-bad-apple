package net.scheffers.robot.pxcodec.codecs;

import net.scheffers.robot.pxcodec.Bitstream;
import net.scheffers.robot.pxcodec.Codec;
import processing.core.PImage;

public class EliminationCodec extends Codec {
	
	protected Codec underlying;
	protected PImage previous;
	
	public EliminationCodec(Codec underlying) {
		this.underlying = underlying;
		this.previous   = null;
	}
	
	@Override
	public PImage encode(Bitstream to, PImage image) {
		// Prepare image to compare against previous.
		PImage out = image.copy();
		out.loadPixels();
		smartThreshold(out);
		out.updatePixels();
		
		// Compare against previous.
		if (previous != null) {
			// Load pixels of previous.
			if (previous.pixels == null || previous.pixels.length == 0) {
				previous.loadPixels();
			}
			
			// Do full comparison.
			boolean equal = true;
			for (int y = 0; y < out.height; y++) {
				for (int x = 0; x < out.width; x++) {
					int prev = previous.pixels[y*out.height+x] | 0xff000000;
					int curr = out     .pixels[y*out.height+x] | 0xff000000;
					if (prev != curr) {
						equal = false;
						break;
					}
				}
			}
			
			// When this is reached, image equals previous.
			if (equal) {
				to.write(false);
				return previous;
			}
		}
		
		// Encode full image.
		to.write(true);
		previous = underlying.encode(to, out);
		
		return previous;
	}
	
	@Override
	public PImage decode(Bitstream from) {
		return null;
	}
	
}
