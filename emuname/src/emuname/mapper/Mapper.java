package net.shygoo.emuname.mapper;

import net.shygoo.emuname.NES;
import net.shygoo.emuname.mapper.*;
//import net.shygoo.emuname.mapper.Mapper00_NROM;
import net.shygoo.misc.Memory;

// TODO two Memory objects for CHR-MEM and PRG-MEM here
//   pass mapper.chrMem directly to the ppu object as constructor arg
//   use mapper.prgMem in nes class when 4020+ is accessed
// additional memory object for save ram in mapper class


abstract public class Mapper {
	protected NES nes;
	
	protected int prgA;
	protected int prgB;
	
	public Memory chrMem;
	public Memory prgMem;
	
	protected Mapper(NES nes){
		this.nes = nes;
	}
	
	public int getDefaultPrgA(){
		return 0;
	}
	
	public int getDefaultPrgB(){
		// mirror bank A if there is only one prg bank, else last bank in rom - looks true for most mappers
		return (nes.romPrgBanks < 2) ? 0 : nes.romPrgBanks - 1;
	}
	
	public int getPrgA(){
		return prgA;
	}
	
	public int getPrgB(){
		return prgB;
	}
	
	public void setPrgA(int bank){
		prgA = bank;
	}
	
	public void setPrgB(int bank){
		prgB = bank;
	}
	
	public void setPrgsDefault(){
		setPrgA(getDefaultPrgA());
		setPrgB(getDefaultPrgB());
	}
	
	abstract public String getName();
}