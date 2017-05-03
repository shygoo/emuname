package net.shygoo.emuname.mapper;

import net.shygoo.emuname.mapper.Mapper;
import net.shygoo.emuname.NES;
import net.shygoo.misc.Memory;

public class Mapper00_NROM extends Mapper {

	public String getName(){
		return "NROM";
	}
	
	public Mapper00_NROM(NES nes){
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
				// No mapper registers
				return;
			}
		};
		
		chrMem = new Memory(){
			private byte[] vRam = (nes.romChrBanks == 0) ? new byte[0x2000] : null;
			public int read(int offset, boolean doEvents){
				if(vRam == null){
					return nes.rom[NES.INES_HEADERSIZE + (nes.romPrgBanks * 0x4000) + offset]; // + (chrBank * 0x2000)
				} else {
					return vRam[offset] & 0xFF;
				}
				//System.out.println("Need VRAM????");
				//return 0;
			}
			public void write(int offset, int value, boolean doEvents){
				// VROM, read only
				// && nes.romChrBanks > 0
				if(vRam == null){
					nes.rom[NES.INES_HEADERSIZE + (nes.romPrgBanks * 0x4000) + offset] = (byte)(value & 0xFF); // + (chrBank * 0x2000)
				} else {
					vRam[offset] = (byte)(value & 0xFF);
				}
				//return;
			}
		};
	}
	
	public int getDefaultprgA(){
		return 0;
	}
	
	public int getDefaultprgB(){
		// mirror bank A if there is only one prg bank, else last bank in rom - looks true for most mappers
		return (nes.romPrgBanks < 2) ? 0 : nes.romPrgBanks - 1;
	}
	
}