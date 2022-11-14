package net.scheffers.robot.pxcodec.codecs;

import net.scheffers.robot.pxcodec.Bitstream;
import net.scheffers.robot.pxcodec.Codec;
import processing.core.PImage;

public class XORDeltaCodec extends Codec {
	
	protected Codec  underlying;
	protected PImage previous;
	
	public XORDeltaCodec(Codec underlying) {
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
		
		// XOR output with previous.
		PImage xor = out.copy();
		if (previous != null) {
			boolean difference = false;
			previous.loadPixels();
			xor.loadPixels();
			for (int y = 0; y < xor.height; y++) {
				for (int x = 0; x < xor.width; x++) {
					// Grab pixels to compare.
					int prev = previous.pixels[y*xor.width+x] & 0xffffff;
					int curr = xor     .pixels[y*xor.width+x] & 0xffffff;
					// Exclusive OR used as difference.
					int diff = xor.pixels[y*xor.width+x] = prev ^ curr;
					// Record whether the images differ.
					if (diff != 0) difference = true;
				}
			}
			xor.updatePixels();
			
			// If no difference, exit.
			if (!difference) {
				to.write(false);
				return previous;
			}
		}
		
		// Encode full image.
		to.write(true);
		xor = underlying.encode(to, xor);
		
		// Re-do XOR.
		if (previous != null) {
			previous.loadPixels();
			xor.loadPixels();
			for (int y = 0; y < xor.height; y++) {
				for (int x = 0; x < xor.width; x++) {
					// Grab pixels to compare.
					int prev = previous.pixels[y*xor.width+x] & 0xffffff;
					int curr = xor     .pixels[y*xor.width+x] & 0xffffff;
					// Exclusive OR used as difference.
					xor.pixels[y*xor.width+x] = prev ^ curr;
				}
			}
			xor.updatePixels();
		}
		previous = xor;
		
		return xor;
	}
	
	@Override
	public PImage decode(Bitstream from) {
		return new PImage(21, 16);
	}
}
