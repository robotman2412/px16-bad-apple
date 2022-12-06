package net.scheffers.robot.pxcodec.codecs;

import net.scheffers.robot.pxcodec.Bitstream;
import net.scheffers.robot.pxcodec.Codec;
import processing.core.PImage;

public class RLECompressor extends Codec {
	
	public final boolean useMetaMetaLength = false;
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
	
	protected class RLEWriteBitstream extends Bitstream {
		
		protected Bitstream buffer;
		protected int zeroGroupCount;
//		protected boolean trailingBit;
//		protected boolean hasTrailingBit;
		protected boolean[] incoming;
		protected int incomingLength;
		
		public RLEWriteBitstream() {
			buffer = new Bitstream();
			incoming = new boolean[chunkSize];
		}
		
		@Override
		public int readAvailable() {
			return buffer.readAvailable();
		}
		
		@Override
		public boolean read() {
			return buffer.read();
		}
		
		public void finish() {
			if (incomingLength > 0) {
				process();
				incomingLength = 0;
			}
			if (zeroGroupCount > 0) {
				writeRLE(zeroGroupCount);
				zeroGroupCount = 0;
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
			if (useMetaMetaLength) {
				writeRLEMetaMeta(length);
			} else {
				writeRLEMeta(length);
			}
		}
		
		protected void writeRLEMeta(int length) {
			length --;
			int metaLength = bitsRequired(length, true)-1;
			
			// Write meta-length.
			for (int i = 0; i < metaLength; i++) {
				buffer.write(true);
			}
			buffer.write(false);
			
			// Write length.
			buffer.writeInt(length, metaLength+1);
		}
		
		protected void writeRLEMetaMeta(int length) {
			length --;
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
		
		protected void process() {
			boolean isZero = true;
			for (int i = 0; i < chunkSize; i++) {
				if (incoming[i]) {
					isZero = false;
					break;
				}
			}
			
			if (isZero) {
				if (zeroGroupCount == 0) {
					// Write end of data block.
					for (int i = 0; i < chunkSize; i++) buffer.write(false);
				}
				zeroGroupCount++;

			} else {
				if (zeroGroupCount > 0) {
					// Flush RLE data.
					writeRLE(zeroGroupCount);
					zeroGroupCount = 0;
				}
				
				// Write literal data.
				buffer.write(incoming);
			}
		}
		
		@Override
		public void write(boolean... input) {
			for (boolean bit: input) {
				if (incomingLength == chunkSize) {
					process();
					incomingLength = 0;
				}
				incoming[incomingLength] = bit;
				incomingLength ++;
			}
		}
	}
	
	protected class RLEReadBitstream extends Bitstream {
		
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
			boolean[] seq = currentInput.read(chunkSize);
			boolean isZero = true;
			for (int i = 0; i < chunkSize; i++) {
				if (seq[i]) {
					isZero = false;
					break;
				}
			}
			
			if (isZero) {
				if (useMetaMetaLength) {
					// Read RLE MetaMetaLength.
					int metaMetaLength = countOnes();
					// Read MetaLength.
					int metaLength = currentInput.readInt(metaMetaLength)+1;
					// Read length.
					int length = currentInput.readInt(metaLength);
					
					// Generate zeroes in output.
					while (length >= 0) {
						for (int i = 0; i < chunkSize; i++) buffer.write(false);
						length --;
					}
				} else {
					// Read RLE MetaMetaLength.
					int metaLength = countOnes()+1;
					// Read length.
					int length = currentInput.readInt(metaLength);
					
					// Generate zeroes in output.
					while (length >= 0) {
						for (int i = 0; i < chunkSize; i++) buffer.write(false);
						length --;
					}
				}
			} else {
				// Literally write data seq.
				buffer.write(seq);
			}
		}
	}
}
