package dev.stardrops.jaad.mp4.boxes.impl;

import java.io.IOException;
import dev.stardrops.jaad.mp4.MP4InputStream;
import dev.stardrops.jaad.mp4.boxes.FullBox;
import dev.stardrops.jaad.mp4.od.Descriptor;

public class ObjectDescriptorBox extends FullBox {

	private Descriptor objectDescriptor;

	public ObjectDescriptorBox() {
		super("Object Descriptor Box");
	}

	@Override
	public void decode(MP4InputStream in) throws IOException {
		super.decode(in);
		objectDescriptor = Descriptor.createDescriptor(in);
	}

	public Descriptor getObjectDescriptor() {
		return objectDescriptor;
	}
}
