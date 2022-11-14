package net.scheffers.robot.pxcodec;

import net.scheffers.robot.pxcodec.codecs.EliminationCodec;
import net.scheffers.robot.pxcodec.codecs.HorizontalCroppingCodec;
import net.scheffers.robot.pxcodec.codecs.RawCodec;
import net.scheffers.robot.pxcodec.codecs.XORDeltaCodec;
import processing.core.PApplet;
import processing.core.PImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Main extends PApplet {
	
	public int numImages = 6572;
//	public int numImages = 2000;
	public String input  = "greyscale/";
	public String output = "output/";
	
	public boolean auto = false;
	public int currentImage;
	public PImage[] inputImages;
	public PImage[] outputImages;
	
	public Bitstream bitstream;
	public Codec codec;
	public boolean saved = false;
	
	public static void main(String[] args) {
		PApplet.main(Main.class.getName(), args);
	}
	
	@Override
	public void settings() {
		size(230, 380);
		noSmooth();
	}
	
	@Override
	public void setup() {
		// Load images.
		inputImages  = new PImage[numImages];
		outputImages = new PImage[numImages];
		for (int i = 0; i < numImages; i++) {
			inputImages[i] = loadImage(input + "out_" + (i+1) + ".png");
		}
		currentImage = 0;
		frameRate(30);
		
		bitstream = new Bitstream();
		codec = new XORDeltaCodec(new HorizontalCroppingCodec());
//		codec = new HorizontalCroppingCodec();
	}
	
	@Override
	public void draw() {
		background(255);
		if (currentImage < 0) currentImage = 0;
		else if (currentImage >= numImages) {
			currentImage = numImages - 1;
			if (!saved) {
				saved = true;
				try {
					saveBitstream();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		image(inputImages[currentImage], 10, 10, 210, 160);
		if (outputImages[currentImage] == null) {
			codec.encode(bitstream, inputImages[currentImage]);
			outputImages[currentImage] = codec.decode(bitstream);
		}
		if (outputImages[currentImage] != null) {
			image(outputImages[currentImage], 10, 180, 210, 160);
		}
		
		fill(0);
		textSize(12);
		int maxSize     = inputImages[0].width * inputImages[0].height * (currentImage+1);
		int compression = 100 - bitstream.size() * 100 / maxSize;
		int memorySize  = bitstream.size() / 16 * 100 / 65536;
		text(String.format(
				"Frame: %d / %d, %d bits\n%d%% compression, %d%% memory",
				currentImage+1, numImages, bitstream.size(),
				compression, memorySize
		), 10, 355);
		
		if (auto) currentImage ++;
	}
	
	protected void saveBitstream() throws IOException {
		FileOutputStream fd = new FileOutputStream(output + "bitstream.px16");
		bitstream.seek(0);
		
		int counter = 0;
		int perLine = 64;
		fd.write("\n\t.section \".rodata\"\nbitstream:".getBytes(StandardCharsets.UTF_8));
		while (bitstream.readAvailable() > 0) {
			int read = Math.min(bitstream.readAvailable(), 16);
			int data = bitstream.readInt(read);
			if ((counter % perLine) == 0) {
				fd.write(String.format("\n\t.db 0x%04x", data).getBytes(StandardCharsets.UTF_8));
			} else {
				fd.write(String.format(", 0x%04x", data).getBytes(StandardCharsets.UTF_8));
			}
			counter ++;
		}
		fd.write("\n".getBytes(StandardCharsets.UTF_8));
		fd.flush();
		fd.close();
	}
	
	@Override
	public void keyPressed() {
		if (key == ' ') {
			auto = !auto;
		} else if (keyCode == LEFT) {
			currentImage --;
		} else if (keyCode == RIGHT) {
			currentImage ++;
		}
	}
}
