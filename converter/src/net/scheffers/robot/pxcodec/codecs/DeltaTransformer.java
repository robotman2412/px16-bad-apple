package net.scheffers.robot.pxcodec.codecs;

import net.scheffers.robot.pxcodec.Bitstream;
import net.scheffers.robot.pxcodec.Codec;
import processing.core.PImage;

public class DeltaTransformer extends Codec {
	
	protected Codec underlying;
	protected DeltaWriteBitstream writer;
	protected DeltaReadBitstream  reader;
	
	public DeltaTransformer(Codec underlying) {
		writer = new DeltaWriteBitstream();
		reader = new DeltaReadBitstream();
		this.underlying = underlying;
	}
	
	@Override
	public void finishEncode(Bitstream to) {
		writer.currentOutput = to;
		underlying.finishEncode(writer);
	}
	
	@Override
	public PImage encode(Bitstream to, PImage image) {
		writer.currentOutput = to;
		return underlying.encode(writer, image);
	}
	
	@Override
	public PImage decode(Bitstream from) {
		reader.currentInput = from;
		return underlying.decode(reader);
	}
	
	protected static class DeltaWriteBitstream extends Bitstream {
		public Bitstream currentOutput;
		protected boolean currentState;
		
		public DeltaWriteBitstream() {}
		
		@Override
		public void write(boolean... input) {
			for (boolean bit: input) {
				currentOutput.write(currentState != bit);
				currentState = bit;
			}
		}
	}
	
	protected static class DeltaReadBitstream extends Bitstream {
		public Bitstream currentInput;
		protected boolean currentState;
		
		public DeltaReadBitstream() {}
		
		@Override
		public int readAvailable() {
			return currentInput.readAvailable();
		}
		
		@Override
		public boolean read() {
			currentState ^= currentInput.read();
			return currentState;
		}
	}
}
