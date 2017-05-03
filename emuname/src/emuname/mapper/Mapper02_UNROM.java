package net.shygoo.emuname.mapper;

import net.shygoo.emuname.mapper.*;
import net.shygoo.emuname.NES;

import net.shygoo.misc.Memory;

public class Mapper02_UNROM extends Mapper {

	public String getName(){
		return "UNROM";
	}
	
	public Mapper02_UNROM(NES nes){
		super(nes);
		
		prgMem = new Memory(){
			public int read(int offset, boolean doEvents){
				if(offset >= 0x8000 && offset < 0xC000) { // prgA
					return nes.rom[NES.INES_HEADERSIZE + (prgA * 0x4000) + (offset & 0x3FFF)] & 0xFF;
				} else if(offset >= 0xC000){ // prgB
					return nes.rom[NES.INES_HEADERSIZE + (prgB * 0x4000) + (offset & 0x3FFF)] & 0xFF;
				}
				return 0;
			}
			public void write(int offset, int value, boolean doEvents){
				if(offset >= 0x8000){
					prgA = value;
				}
			}
		};
		
		chrMem = new Memory(){
			byte[] vRam = new byte[0x2000];
			public int read(int offset, boolean doEvents){
				return vRam[offset];
			}
			public void write(int offset, int value, boolean doEvents){
				vRam[offset] = (byte)value;
			}
		};
		
	}
	
	public int getDefaultprgA(){
		return 0;
	}
	
	public int getDefaultprgB(){
		return (nes.romPrgBanks < 2) ? 0 : nes.romPrgBanks - 1;
	}
}