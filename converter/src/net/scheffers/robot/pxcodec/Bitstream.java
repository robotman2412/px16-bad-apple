package net.scheffers.robot.pxcodec;

public class Bitstream {
	
	protected int numBits;
	protected int readIndex;
	protected int[] buffer;
	
	public Bitstream() {
		buffer = new int[1024];
	}
	
	protected void expandBuffer() {
		int[] newBuffer = new int[buffer.length * 3 / 2];
		System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
		buffer = newBuffer;
	}
	
	public void writeInt(int input, int size) {
		for (int i = 0; i < size; i++) {
			write(((input >> i) & 1) > 0);
		}
	}
	
	public void write(boolean... input) {
		// Ensure enough capacity.
		while (numBits + input.length > buffer.length * 32) expandBuffer();
		
		// Write to buffer.
		for (int i = 0; i < input.length; i++) {
			// Determine write index.
			int writeBit   = i + numBits;
			int writeIndex = writeBit / 32;
			writeBit %= 32;
			
			// Write to the buffer.
			if (input[i]) buffer[writeIndex] |= 1 << writeBit;
		}
		numBits += input.length;
	}
	
	public boolean[] read(int count) {
		// Clamp count to read.
		if (count > readAvailable()) count = readAvailable();
		
		// Read bits to a new buffer.
		boolean[] out = new boolean[count];
		for (int i = 0; i < count; i++) {
			out[i] = read();
		}
		
		return out;
	}
	
	public boolean read() {
		if (this.readIndex >= this.numBits) return false;
		
		int readBit   = this.readIndex % 32;
		int readIndex = this.readIndex / 32;
		boolean value = ((buffer[readIndex] >> readBit) & 1) > 0;
		this.readIndex ++;
		return value;
	}
	
	public int readInt(int size) {
		int out = 0;
		for (int i = 0; i < size; i++) {
			if (read()) out |= 1 << i;
		}
		return out;
	}
	
	public int readAvailable() {
		return numBits - readIndex;
	}
	
	public int size() {
		return numBits;
	}
	
	public int getReadIndex() {
		return readIndex;
	}
	
	public void seek(int index) {
		readIndex = index;
	}
	
	public void concat(Bitstream other) {
		int count = other.readAvailable();
		for (int i = 0; i < count; i++) {
			write(other.read());
		}
	}
	
}
