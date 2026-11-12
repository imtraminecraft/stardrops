package dev.stardrops.jaad.mp4.boxes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import dev.stardrops.jaad.mp4.MP4InputStream;

public class BoxImpl implements Box {

	private final String name;
	protected long size, type, offset;
	protected Box parent;
	protected final List<Box> children;

	public BoxImpl(String name) {
		this.name = name;

		children = new ArrayList<Box>(4);
	}

	public void setParams(Box parent, long size, long type, long offset) {
		this.size = size;
		this.type = type;
		this.parent = parent;
		this.offset = offset;
	}

	protected long getLeft(MP4InputStream in) throws IOException {
		return (offset+size)-in.getOffset();
	}

	/**
	 * Decodes the given input stream by reading this box and all of its
	 * children (if any).
	 * 
	 * @param in an input stream
	 * @throws IOException if an error occurs while reading
	 */
	public void decode(MP4InputStream in) throws IOException {
	}

	public long getType() {
		return type;
	}

	public long getSize() {
		return size;
	}

	public long getOffset() {
		return offset;
	}

	public Box getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name+" ["+BoxFactory.typeToString(type)+"]";
	}

	//container methods
	public boolean hasChildren() {
		return children.size()>0;
	}

	public boolean hasChild(long type) {
		boolean b = false;
		for(Box box : children) {
			if(box.getType()==type) {
				b = true;
				break;
			}
		}
		return b;
	}

	public Box getChild(long type) {
		Box box = null, b = null;
		int i = 0;
		while(box==null&&i<children.size()) {
			b = children.get(i);
			if(b.getType()==type) box = b;
			i++;
		}
		return box;
	}

	public List<Box> getChildren() {
		return Collections.unmodifiableList(children);
	}

	public List<Box> getChildren(long type) {
		List<Box> l = new ArrayList<Box>();
		for(Box box : children) {
			if(box.getType()==type) l.add(box);
		}
		return l;
	}

	// Stardrops: made forgiving. A child that reads past its own end (ffmpeg's tag boxes do this to JAAD) or cannot be read at all no
	// longer fails the whole file: we step back to where the child ends, or give up on the rest of this parent, and carry on.
	protected void readChildren(MP4InputStream in) throws IOException {
		final long end = offset+size;
		while(in.getOffset()<end) {
			final long start = in.getOffset();
			try {
				if(end-start<8) { in.skipBytes(end-start); break; }
				Box box = BoxFactory.parseBox(this, in);
				children.add(box);
				final long childEnd = box.getOffset()+box.getSize();
				if(box.getSize()<8||childEnd<=start) throw new IOException("box without a size");
				if(in.getOffset()<childEnd&&childEnd<=end) in.skipBytes(childEnd-in.getOffset());
				else if(in.getOffset()>childEnd) { if(in.hasRandomAccess()) in.seek(childEnd); else break; }
			}
			catch(IOException e) {
				if(in.getOffset()<end) in.skipBytes(end-in.getOffset());
				else if(in.getOffset()>end&&in.hasRandomAccess()) in.seek(end);
				break;
			}
		}
	}

	protected void readChildren(MP4InputStream in, int len) throws IOException {
		Box box;
		for(int i = 0; i<len; i++) {
			box = BoxFactory.parseBox(this, in);
			children.add(box);
		}
	}
}
