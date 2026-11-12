package dev.stardrops.jaad.mp4.api.codec;

import dev.stardrops.jaad.mp4.api.DecoderInfo;
import dev.stardrops.jaad.mp4.boxes.impl.sampleentries.codec.CodecSpecificBox;
import dev.stardrops.jaad.mp4.boxes.impl.sampleentries.codec.SMVSpecificBox;

public class SMVDecoderInfo extends DecoderInfo {

	private SMVSpecificBox box;

	public SMVDecoderInfo(CodecSpecificBox box) {
		this.box = (SMVSpecificBox) box;
	}

	public int getDecoderVersion() {
		return box.getDecoderVersion();
	}

	public long getVendor() {
		return box.getVendor();
	}

	public int getFramesPerSample() {
		return box.getFramesPerSample();
	}
}
