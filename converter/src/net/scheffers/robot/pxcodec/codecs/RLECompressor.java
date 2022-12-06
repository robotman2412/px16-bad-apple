package net.scheffers.robot.pxcodec.codecs;

import net.scheffers.robot.pxcodec.Bitstream;
import net.scheffers.robot.pxcodec.Codec;
import processing.core.PImage;

public class RLECompressor extends Codec {
	
	public final int chunkSize = 2;
	
	protected RLEWriteBitstream writeBuffer;
	protected RLEReadBitstream readBuffer;
	protected Codec underlying;
	
	public RLECompressor(Codec underlying) {
		writeBuffer = new RLEWriteBitstream();
		readBuffer = new RLEReadBitstream();
		this.underlying = underlying;
	}
	
	@Override
	public PImage encode(Bitstream to, PImage image) {
		return underlying.encode(writeBuffer, image);
	}
	
	@Override
	public void finishEncode(Bitstream to) {
		writeBuffer.finish();
		to.concat(writeBuffer);
	}
	
	@Override
	public PImage decode(Bitstream from) {
		readBuffer.currentInput = from;
		return underlying.decode(readBuffer);
	}
	
	protected static class RLEWriteBitstream extends Bitstream {
		
		protected Bitstream buffer;
		protected int zeroPairCount;
		protected boolean trailingBit;
		protected boolean hasTrailingBit;
		
		public RLEWriteBitstream() { buffer = new Bitstream(); }
		
		@Override
		public int readAvailable() {
			return buffer.readAvailable();
		}
		
		@Override
		public boolean read() {
			return buffer.read();
		}
		
		public void finish() {
			if (hasTrailingBit) {
				processPair(trailingBit, false);
				hasTrailingBit = false;
			}
			if (zeroPairCount > 0) {
				writeRLE(zeroPairCount);
				zeroPairCount = 0;
			}
		}
		
		public static int bitsRequired(int number, boolean zeroRequired) {
			if (number == 0 && zeroRequired) return 1;
			
			int required = 0;
			while (number != 0) {
				required ++;
				number >>>= 1;
			}
			
			return required;
		}
		
		protected void writeRLE(int length) {
			int metaLength = bitsRequired(length, true);
			int metaMetaLength = bitsRequired(metaLength-1, false);
			
			// Write meta-meta-length.
			for (int i = 0; i < metaMetaLength; i++) {
				buffer.write(true);
			}
			buffer.write(false);
			
			// Write meta-length.
			if (metaMetaLength > 0) {
				buffer.writeInt(metaLength-1, metaMetaLength);
			}
			
			// Write length.
			buffer.writeInt(length, metaLength);
		}
		
		protected void processPair(boolean a, boolean b) {
			if (!a && !b) {
				if (zeroPairCount == 0) {
					// Write end of data block.
					buffer.write(false, false);
				}
				zeroPairCount ++;

			} else {
				if (zeroPairCount > 0) {
					// Flush RLE data.
					writeRLE(zeroPairCount);
					zeroPairCount = 0;
				}
				
				// Write literal data.
				buffer.write(a, b);
			}
		}
		
		@Override
		public void write(boolean... input) {
			for (boolean bit: input) {
				if (hasTrailingBit) {
					processPair(trailingBit, bit);
					hasTrailingBit = false;
				} else {
					trailingBit = bit;
					hasTrailingBit = true;
				}
			}
		}
	}
	
	protected static class RLEReadBitstream extends Bitstream {
		
		protected Bitstream buffer;
		public Bitstream currentInput;
		
		public RLEReadBitstream() {
			buffer = new Bitstream();
		}
		
		@Override
		public boolean read() {
			if (buffer.readAvailable() == 0) {
				decodeBits();
			}
			return buffer.read();
		}
		
		protected int countOnes() {
			int count = 0;
			while (currentInput.read()) count ++;
			return count;
		}
		
		protected void decodeBits() {
			boolean[] seq = currentInput.read(2);
			if (!seq[0] && !seq[1]) {
				// Read RLE MetaMetaLength.
				int metaMetaLength = countOnes();
				// Read MetaLength.
				int metaLength = currentInput.readInt(metaMetaLength)+1;
				// Read length.
				int length = currentInput.readInt(metaLength);
				
				// Generate zeroes in output.
				while (length > 0) {
					buffer.write(false, false);
					length --;
				}
			} else {
				// Literally write data seq.
				buffer.write(seq);
			}
		}
	}
}
