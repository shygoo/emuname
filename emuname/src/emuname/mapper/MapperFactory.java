package net.shygoo.emuname.mapper;

import net.shygoo.emuname.NES;
import net.shygoo.emuname.mapper.*;
import net.shygoo.misc.Memory;

public class MapperFactory {
	private NES nes;
	public MapperFactory(NES nes){
		this.nes = nes;
	}
	public Mapper createMapperById(int iNESMapperId){
		switch(iNESMapperId){
			case 0x00: return new Mapper00_NROM(nes);
			case 0x01: return new Mapper01_MMC1(nes);
			case 0x02: return new Mapper02_UNROM(nes);
		}
		return null;
	}
}