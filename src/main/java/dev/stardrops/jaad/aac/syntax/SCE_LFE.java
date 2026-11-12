package dev.stardrops.jaad.aac.syntax;

import dev.stardrops.jaad.aac.AACException;
import dev.stardrops.jaad.aac.DecoderConfig;

class SCE_LFE extends Element {

	private final ICStream ics;

	SCE_LFE(int frameLength) {
		super();
		ics = new ICStream(frameLength);
	}

	void decode(BitStream in, DecoderConfig conf) throws AACException {
		readElementInstanceTag(in);
		ics.decode(in, false, conf);
	}

	public ICStream getICStream() {
		return ics;
	}
}
