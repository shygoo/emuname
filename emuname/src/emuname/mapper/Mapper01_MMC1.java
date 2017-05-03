package net.shygoo.emuname.mapper;

import net.shygoo.emuname.NES;
import net.shygoo.hw.PPU2C02;

import net.shygoo.emuname.mapper.*;

public class Mapper01_MMC1 extends Mapper {

	public String getName(){
		return "MMC1";
	}

	// register masks
	private static final int
	R0_MASK_MIRRORING  = 0b00000001, // 0 = horizontal, 1 = vertical
	R0_MASK_ONESCR     = 0b00000010, // 0 = one screen mirroring, 1 = normal
	R0_MASK_PRG_AREA   = 0b00000100, // 0 = $C000, 1 = $8000
	R0_MASK_PRG_SIZE   = 0b00001000, // 0 = $4000 (16k), 1 = $8000 (32k)
	R0_MASK_CHR_SIZE   = 0b00010000, // 0 = $2000, 1 = $4000
	
	R1_MASK_CHR_BANK_A = 0b00011111, // VROM $0000 CHR Bank A
	R2_MASK_CHR_BANK_B = 0b00011111, // VROM $1000 CHR Bank B
	
	R3_MASK_PRG_BANK   = 0b00001111,
	R3_PRGRAM_ENABLE   = 0b00010000;
	
	private static final int
	R0_RANGE = 0x8000,
	R1_RANGE = 0xA000,
	R2_RANGE = 0xC000,
	R3_RANGE = 0xE000;
	
	private static final int
	MASK_RESET = 0b10000000,
	STATE_DONE = 0b00010000;
	
	private int shift  = 0; // 0 1 2 3 4
	
	private int
	status    = 0b00000,
	register0 = 0b00000,
	register1 = 0b00000,
	register2 = 0b00000,
	register3 = 0b00000;
	
	private int prgRomSwitchBank = 0; // 0 = 8000, 1 = C000
	private int prgRomSwitchSize = 0x4000;
	
	public Mapper01_MMC1(NES nes){
		super(nes);
		//setName("MMC1");
	}
	
	public void write(int offset, int value){
		if(offset >= 0x8000){
			if((value & MASK_RESET) != 0){
				status = 0;
				shift = 0;
				return;
			}
			status |= (value & 1) << shift;
			shift = (shift + 1) % 5;
			if(shift == 0){ // fifth write
				offset = offset & 0xE000;
				switch(offset){
					case R0_RANGE:
						if((status & R0_MASK_ONESCR) != 0){
							nes.ppu.setMirroringMode(PPU2C02.MIRRORING_ONESCREEN);
						} else {
							nes.ppu.setMirroringMode(((status & R0_MASK_MIRRORING) == 0) ? PPU2C02.MIRRORING_HORIZONTAL : PPU2C02.MIRRORING_VERTICAL);
						}
						if((status & R0_MASK_PRG_SIZE) != 0) { // size is 32k, ignore bank number and swap at $8000
							prgRomSwitchBank = 0;
							prgRomSwitchSize = 0x8000;
						} else { // size is 16k, read bank number
							prgRomSwitchBank = ((status & R0_MASK_PRG_AREA) == 0) ? 1 : 0;
							prgRomSwitchSize = 0x4000;
						}
						break;
					case R1_RANGE:
						
						break;
					case R2_RANGE:
						
						break;
					case R3_RANGE:
						int bank = status & R3_MASK_PRG_BANK;
						if(prgRomSwitchSize == 0x8000){
							prgA = bank;
							prgB = bank + 1;
						} else {
							if(prgRomSwitchBank == 0){
								prgA = bank;
								prgB = getDefaultprgB();
							} else {
								prgA = getDefaultprgA();
								prgB = bank;
							}
						}
						// TODO R3_PRGRAM_ENABLE
						break;
				}
			}
		}
		//System.out.println("Bank A swapped");
	}
	
	public int getDefaultprgA(){
		return 0;
	}
	
	public int getDefaultprgB(){
		// mirror bank A if there is only one prg bank, else last bank in rom - looks true for most mappers
		return (nes.romPrgBanks < 2) ? 0 : nes.romPrgBanks - 1;
	}
	
}