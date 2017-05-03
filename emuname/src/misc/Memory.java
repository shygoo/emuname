package net.shygoo.misc;

// TODO change to abstract class, remove the 'data' array (make all storage entirely up to the subclass)
// add doEvents arg so plugins/data viewers can use the read and write functions without triggering events



abstract public class Memory {
	abstract public int read(int offset, boolean doEvents);
	abstract public void write(int offset, int value, boolean doEvents);
	// default doEvents to true:
	public int read(int offset){
		return read(offset, true);
	}
	public void write(int offset, int value){
		write(offset, value, true);
	}
}

/*

public class Memory {
	public byte[] data;
	public Memory(int byteSize){
		this.data = new byte[byteSize];
	}
	// override these:
	public int getByte(int offset){
		return data[offset] & 0xFF;
	}
	public void setByte(int offset, int value){
		data[offset] = (byte)(value & 0xFF);
	}
}

*/