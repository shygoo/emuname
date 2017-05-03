package net.shygoo.emuname;

import net.shygoo.hw.*;
import net.shygoo.misc.Memory;
import net.shygoo.emuname.mapper.*;

import java.lang.*;
import java.util.ArrayList;
import java.nio.file.*;

public class NES {
	
	// INES file format header offsets
	public static final int
	INES_HEADERSIZE = 16,
	INES_HEADER_PRGROM_SIZE = 0x04,
	INES_HEADER_CHRROM_SIZE = 0x05,
	INES_HEADER_FLAGS6      = 0x06,
	INES_HEADER_FLAGS7      = 0x07;
	
	// CPU memory register offsets
	public static final int
	REG_PPU_CTRL1       = 0x2000,
	REG_PPU_CTRL2       = 0x2001,
	REG_PPU_STATUS      = 0x2002,
	REG_PPU_OAM_ADDRESS = 0x2003,
	REG_PPU_OAM_IO      = 0x2004,
	REG_PPU_SCROLL      = 0x2005,
	REG_PPU_ADDRESS     = 0x2006,
	REG_PPU_IO          = 0x2007,
	
	REG_APU_PULSE1_CTRL = 0x4000,
	REG_APU_PULSE1_RC   = 0x4001,
	REG_APU_PULSE1_FT   = 0x4002,
	REG_APU_PULSE1_CT   = 0x4003,
	REG_APU_PULSE2_CTRL = 0x4004,
	REG_APU_PULSE2_RC   = 0x4005,
	REG_APU_PULSE2_FT   = 0x4006,
	REG_APU_PULSE2_CT   = 0x4007,
	REG_APU_TRI_CTRL1   = 0x4008,
	REG_APU_TRI_CTRL2   = 0x4009,
	REG_APU_TRI_FREQ1   = 0x400A,
	REG_APU_TRI_FREQ2   = 0x400B,
	REG_APU_NOISE_CTRL1 = 0x400C,
	REG_APU_NOISE_CTRL2 = 0x400D,
	REG_APU_NOISE_FREQ1 = 0x400E,
	REG_APU_NOISE_FREQ2 = 0x400F,
	REG_APU_DM_CTRL     = 0x4010,
	REG_APU_DM_DA       = 0x4011,
	REG_APU_DM_ADDRESS  = 0x4012,
	REG_APU_DM_LENGTH   = 0x4013,
	
	REG_PPU_OAM_DMA     = 0x4014,
	
	REG_APU_VCS         = 0x4015,
	
	REG_JOYPAD1         = 0x4016,
	REG_JOYPAD2         = 0x4017;
	
	// NES joypad button strobe positions
	public static final int
	BUTTON_A      = 0,
	BUTTON_B      = 1,
	BUTTON_SELECT = 2,
	BUTTON_START  = 3,
	BUTTON_UP     = 4,
	BUTTON_DOWN   = 5,
	BUTTON_LEFT   = 6,
	BUTTON_RIGHT  = 7;
	
	public static final int
	PORT_1 = 0,
	PORT_2 = 1;
	
	int[] joypadPort1 = new int[24];
	int[] joypadPort2 = new int[24];
	
	int joypadPort1State = 0;
	int joypadPort2State = 0;
	
	// Hardware objects
	public final CPU6502 cpu;
	public final PPU2C02 ppu;
	public final APU2A03 apu;
	
	public Memory mem;
	
	private MapperFactory mapperFactory;
	public Mapper mapper;
	
	public byte[] rom = new byte[0x10000];
	public int romPrgBanks = 0;
	public int romChrBanks = 0;
	
	int totalCycles;
	int frameCycles;
	int scanlineCycles;

	public NES() {
		mapperFactory = new MapperFactory(this);
		mapper = mapperFactory.createMapperById(0x00); //new Mapper00_NROM(this);
		resetMemory();
		cpu = new CPU6502(mem);
		ppu = new PPU2C02(this);
		apu = new APU2A03();
		
		mem.write(REG_PPU_STATUS, 0x80);
		//System.out.printf("ppustatus %02X", mem.read(REG_PPU_STATUS));
		
		//Thread soundThread = new Thread(){
		//	public void run(){
		//		try {
		//			apu.start();
		//			System.out.println("apu started");
		//		} catch(Exception e){
		//			System.out.println("apu error");
		//		}
		//	}
		//};
		
		//soundThread.start();
		//apu.start();
		//try{
		//	apu.wait();
		//} catch(Exception e){
		//	System.out.println("couldnt pause the sound thread");
		//}
	}
	
	public void resetMemory(){
		mem = new Memory() {
			private byte[] data = new byte[0x10000];
			private int mirrorMap(int offset){
				offset &= 0xFFFF;
				if(offset > 0x7FF && offset < 0x2000){ // work ram mirroring
					return offset & ~0x1800;
				} else if(offset >= 0x2008 && offset < 0x4000){ // PPU registers mirroring
					return 0x2000 + (offset % 8);
				}
				return offset;
			}
			@Override
			public int read(int offset, boolean doEvents){
				offset = mirrorMap(offset);
				if(offset >= 0x8000){ // read PRG-ROM through the mapper
					// (offset >= 0x4020 && offset < 0x6000) || 
					return getMapper().prgMem.read(offset);
				}
				switch(offset){
					case REG_PPU_STATUS: { // $2002 PPU Status
						int ret = data[REG_PPU_STATUS] & 0xFF;
						if(doEvents){
							data[REG_PPU_STATUS] = (byte)(ret & 0x7F);
							ppu.address = 0x0000;
							ppu.addressState = 0;
						}
						return ret;
					}
					case REG_PPU_OAM_IO: // $2004
						break;
					case REG_PPU_IO: {
						int ret = data[REG_PPU_IO] & 0xFF;
						int ppuresult = ret;
						if(doEvents){
							ppuresult = ppu.readNext();
						}
						data[REG_PPU_IO] = (byte)ppuresult;
						if(ppu.address > 0x3EFF){ // instant read if palette data
							return ppuresult;
						}
						return ret; // buffered read otherwise
					}
					case REG_JOYPAD1: {
						int ret = 0;
						if(joypadPort1[joypadPort1State] == 1) ret = 1;
						joypadPort1State = (joypadPort1State + 1) % 24;
						return ret;
					}
				}
				return data[offset] & 0xFF;
			}
			@Override
			public void write(int offset, int value, boolean doEvents) {
				offset = mirrorMap(offset);
				value &= 0xFF;
				if((offset >= 0x4020 && offset < 0x6000) || offset >= 0x8000){
					// writes to cartridge address space
					//System.out.println("write to cart address space");
					getMapper().prgMem.write(offset, value);
					return;
				}
				else if(offset >= 0x6000 && offset < 0x8000){
					// add handler for cartridge battery ram here
				}
				switch(offset){
					case REG_PPU_CTRL1: // $2000 PPU Control Register 1
						ppu.nameTableOffset = 0x2000 + (value & 0b00000011) * 0x400;
						ppu.increment       = (value & 0b00000100) != 0 ? 32 : 1;
						ppu.pTableSprOffset = (value & 0b00001000) != 0 ? 0x1000 : 0x0000;
						ppu.pTableBgOffset  = (value & 0b00010000) != 0 ? 0x1000 : 0x0000;
						ppu.sprSize         = (value & 0b00100000) >> 5;
						ppu.nmi             = (value & 0b10000000) != 0;
						break;
					case REG_PPU_CTRL2: // $2001 PPU Control Register 2
						ppu.monochrome = (value & 0b00000001) != 0;
						ppu.clipBg     = (value & 0b00000010) != 0;
						ppu.clipSpr    = (value & 0b00000100) != 0;
						ppu.showBg     = (value & 0b00001000) != 0;
						ppu.showSpr    = (value & 0b00010000) != 0;
						ppu.bgColor    = (value & 0b11000000) >> 6;
						break;
					case REG_PPU_OAM_IO: // write single byte to OAM
						int oamAddress = data[REG_PPU_OAM_ADDRESS] & 0xFF;
						ppu.oam[oamAddress] = (byte)value;
						data[REG_PPU_OAM_ADDRESS] = (byte)((oamAddress + 1) & 0xFF);
						break;
					case REG_PPU_SCROLL: // set PPU scroll x or y
						if(ppu.scrollState == 0){ // x
							ppu.scrollX = value;
							ppu.scrollState = 1;
						} else { // y
							ppu.scrollY = value;
							ppu.scrollState = 0;
						}
						break;
					case REG_PPU_ADDRESS: // set PPU address msb and lsb
						if(ppu.addressState == 0){ // msb
							ppu.address = (value << 8) & 0xFF00;
							ppu.addressState = 1;
						} else { // lsb
							ppu.address = (ppu.address & 0xFF00) | (value & 0xFF);
							ppu.addressState = 0;
						}
						break;
					case REG_PPU_IO: // vram write byte
						ppu.writeNext(value);
						break;
					case REG_APU_PULSE1_CT:
						//apu.test(value);
						break;
					case REG_APU_PULSE2_CT:
						//apu.test2(value);
						break;
					case REG_PPU_OAM_DMA: // OAM sprites DMA
						int oamCopyOffset = value * 0x100;
						for(int i = 0; i < 0x100; i++){
							ppu.oam[i] = data[oamCopyOffset + i];
						}
						cpu.clock(512);
						break;
					case 0x4016:
						joypadPort1State = 0;
						// controller 1 state = 0 ?
						break;
					case 0x4017:
						joypadPort2State = 0;
						// controller 2 state = 0 ?
						break;
				}
				data[offset] = (byte)value;
			}
		};
	}
	
	public boolean isButtonPressed(int port, int button){
		switch(port % 2){
			case PORT_1:
				return joypadPort1[port] != 0;
			case PORT_2:
				return joypadPort2[port] != 0;
		}
		return false;
	}
	
	public void setButtonPressed(int port, int button, boolean pressed){
		switch(port % 2){
			case PORT_1:
				joypadPort1[button % 24] = pressed?1:0;
			case PORT_2:
				joypadPort2[button % 24] = pressed?1:0;
		}
	}
	
	public void loadRom(String filename) {
		//emuRunning = false;
		System.out.printf("Loading %s ...\n", filename);
		Path path = Paths.get(filename);
		
		try {
			rom = Files.readAllBytes(path);
		} catch(Exception e){
			System.out.println("Error: Couldn't read file");
			return;
		}
		
		// Collect INES header information from the rom
		romPrgBanks = rom[INES_HEADER_PRGROM_SIZE] & 0xFF;
		romChrBanks = rom[INES_HEADER_CHRROM_SIZE] & 0xFF;
		
		System.out.printf(" PRG-ROM Banks: %d\n", romPrgBanks);
		System.out.printf(" CHR-ROM Banks: %d\n", romChrBanks);
		
		int prgRomSize = romPrgBanks * 0x4000;
		int chrRomSize = rom[INES_HEADER_CHRROM_SIZE] * 0x2000;
		int mapperId   = ((rom[INES_HEADER_FLAGS6] & 0xF0) >> 4) | (rom[INES_HEADER_FLAGS7] & 0xF0);
		boolean fourScreenMirroring = (rom[INES_HEADER_FLAGS6] & 0b1000) != 0;
		boolean verticalMirroring = (rom[INES_HEADER_FLAGS6] & 1) != 0;
		
		// Select PPU nametable mirroring
		if(fourScreenMirroring){
			ppu.setMirroringMode(PPU2C02.MIRRORING_FOURSCREEN);
			System.out.println(" PPU Mirroring: Four-screen");
		} else if(verticalMirroring){
			ppu.setMirroringMode(PPU2C02.MIRRORING_VERTICAL);
			System.out.println(" PPU Mirroring: Vertical");
		} else {
			ppu.setMirroringMode(PPU2C02.MIRRORING_HORIZONTAL);
			System.out.println(" PPU Mirroring: Horizontal");
		}
		
		// Select Mapper subclass
		mapper = mapperFactory.createMapperById(mapperId);
		mapper.setPrgsDefault();
		
		//if(!apu.isRunning()){
		//	startApu();
		//}
		
		System.out.printf(" Mapper: %s (%02X)\n", mapper.getName(), mapperId);
		
		//apu.start();
		cpu.reset();
	}
	
	//public void startApu(){
	//	apu.start();
	//}
	
	public void stopApu(){
		//apu.pause();
		//while(apu.getState() != Thread.State.TERMINATED); // wait
	}
	
	//public void cycleTest(){
	//	System.out.println("Running cycle count test ...");
	//	for(int i = 0; i < 0x100; i++){
	//		cpu.totalCycles = 0;
	//		cpu.setPC(0);
	//		cpu.mem.data[0] = (byte)i;
	//		System.out.printf("[%02X] %-20s (%d)\n", i, dis.decode(0), cpu.step());
	//	}
	//	System.out.println("Done");
	//	cpu.mem.data[0] = (byte)0x00;
	//}
	
	// Let the cpu and ppu run until a VBlank occurs
	public void runUntilVBlank(){
		mem.write(REG_PPU_STATUS, mem.read(REG_PPU_STATUS, false) & ~0x40, false);
		int phase = 0; // 0 1 2
		boolean spr0hit = false;
		while(frameCycles < 29780) { // 29780
			//System.out.printf("(%4d) %04X: %-15s // A:%02X X:%02X Y:%02X P:%02X SP:%02X\n", cpu.totalOps, cpu.PC, dis.decode(cpu.PC), cpu.AC, cpu.XR, cpu.YR, cpu.SR, cpu.SP);
			prestep();
			if(getDbgBreakState()) return;
			int cycles = cpu.step();
			poststep();
			if(getDbgBreakState()) return;
			totalCycles += cycles;
			frameCycles += cycles;
			scanlineCycles += cycles;
			if(scanlineCycles >= ((phase != 2) ? 113 : 114)){
				ppu.drawScanline();
				//mem.write(REG_PPU_STATUS, mem.read(REG_PPU_STATUS, false) | 0x40, false); // TESTING
				if(ppu.getSprite0Hit() && !spr0hit){
					mem.write(REG_PPU_STATUS, mem.read(REG_PPU_STATUS, false) | 0x40, false);
					spr0hit = true;
				}
				//if(ppu.getScanline8Sprites()){
				//	mem.write(REG_PPU_STATUS, mem.read(REG_PPU_STATUS, false) | 0x20, false);
				//}
				scanlineCycles = 0;
				phase = (phase + 1) % 3;
			}
		}
		// TODO 3 additional scanlines worth of cycles here while entering vblank phase (340)
		
		mem.write(REG_PPU_STATUS, mem.read(REG_PPU_STATUS, false) | 0x80, false); // VBLANK now
		if(ppu.nmi){
			cpu.nonMaskableInterrupt(); // jump now
		}

		for(int i = 0; i < 2273; ){
			prestep();
			if(getDbgBreakState()) return;
			i += cpu.step();
			poststep();
			if(getDbgBreakState()) return;
		} // 20 scanlines worth of cycles during VBLANK phase
		mem.write(REG_PPU_STATUS, mem.read(REG_PPU_STATUS, false) & ~0x80, false); // VBLANK done
		frameCycles = 0;
		ppu.resetScanlines();
		
		// PROCESS SOUND
		
		double t1 = (mem.read(0x4002) | (mem.read(0x4003) & 0b111) << 8);
		double f1 = (1789773 / (16 * (t1 + 1)));
		int v1 = mem.read(0x4000) & 0x0F;
		apu.setPulse1Vol(1.0 / 16 * v1);
		
		double t2 = (mem.read(0x4006) | (mem.read(0x4007) & 0b111) << 8);
		double f2 = (1789773 / (16 * (t2 + 1)));
		int v2 = mem.read(0x4004) & 0x0F;
		apu.setPulse2Vol(1.0 / 16 * v1);
		
		double t3 = (mem.read(0x400A) | (mem.read(0x400B) & 0b111) << 8);
		double f3 = (1789773 / (16 * (t3 + 1))) / 2;
		//System.out.printf("%f %f\n", t, f);
		apu.setPulse1Freq(f1);
		apu.setPulse2Freq(f2);
		apu.setTriFreq(f3);
		
		double nvol = mem.read(0x400C) & 0x0F;
		apu.setNoiseVol(1.0/16 * nvol);
		
	}
	
	public void prestep(){
		return;
	}
	
	public void poststep(){
		return;
	}
	
	boolean dbgBreakState = false;
	
	public void dbgBreak(){ // will cause runUntilVBlank to stop early
		dbgBreakState = true;
	}
	
	private boolean getDbgBreakState(){
		boolean ret = dbgBreakState;
		dbgBreakState = false;
		return ret;
	}
	
	public Mapper getMapper(){
		return mapper;
	}
	
}