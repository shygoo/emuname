package net.shygoo.hw;

import net.shygoo.misc.Memory;
import net.shygoo.emuname.NES;

// TODO ppu.read/write -> ppu.readNext/writeNext
// pass Memory object of chrMem to the constructor

// picture processing unit
public class PPU2C02 {

	private final static int[] displayColors = {
		0x757575, // 00
		0x271B8F, // 01
		0x0000AB, // 02
		0x47009F, // 03
		0x8F0077, // 04
		0xAB0013, // 05
		0xA70000, // 06
		0x7F0B00, // 07
		0x432F00, // 08
		0x004700, // 09
		0x005100, // 0A
		0x003F17, // 0B
		0x1B3F5F, // 0C
		0x000000, // 0D
		0x000000, // 0E
		0x000000, // 0F
		0xBCBCBC, // 10
		0x0073EF, // 11
		0x233BEF, // 12
		0x8300F3, // 13
		0xBF00BF, // 14
		0xE7005B, // 15
		0xDB2B00, // 16
		0xCB4F0F, // 17
		0x8B7300, // 18
		0x009700, // 19
		0x00AB00, // 1A
		0x00933B, // 1B
		0x00838B, // 1C
		0x000000, // 1D
		0x000000, // 1E
		0x000000, // 1F
		0xFFFFFF, // 20
		0x3FBFFF, // 21
		0x5F97FF, // 22
		0xA78BFD, // 23
		0xF77BFF, // 24
		0xFF7763, // 25
		0xFF7763, // 26
		0xFF9B3B, // 27
		0xF3BF3F, // 28
		0x83D313, // 29
		0x4FDF4B, // 2A
		0x58F898, // 2B
		0x00EBDB, // 2C
		0x000000, // 2D
		0x000000, // 2E
		0x000000, // 2F
		0xFFFFFF, // 30
		0xABE7FF, // 31
		0xC7D7FF, // 32
		0xD7CBFF, // 33
		0xFFC7FF, // 34
		0xFFC7DB, // 35
		0xFFBFB3, // 36
		0xFFDBAB, // 37
		0xFFE7A3, // 38
		0xE3FFA3, // 39
		0xABF3BF, // 3A
		0xB3FFCF, // 3B
		0x9FFFF3, // 3C
		0x000000, // 3D
		0x000000, // 3E
		0x000000  // 3F
	};
	
	// 2000 2400 2800 2C00 / end @ 3000
	
	public final static int
	MIRRORING_ONESCREEN  = 0, // l1,l2,l3,l4 = l1,l1,l1,l1
	MIRRORING_HORIZONTAL = 1, // l2 = l1, l3 = l2, l4 = l2
	MIRRORING_VERTICAL   = 2, // l3 = l1, l4 = l2
	MIRRORING_FOURSCREEN = 3; // l1,l2,l3,l4 = l1,l2,l3,l4
	
	//ppu.setMirroring(PPU2C02.MIRRORING_ONESCREEN);
	
	//public byte[] mem;
	public Memory mem;
	//public Memory chrMem;
	public NES    nes;
	public byte[] oam;

	private int mirroring = MIRRORING_VERTICAL;
	
	//public boolean useVRAM   = false;
	
	//public int chrBank = 0;
	
	// scroll register
	public int scrollX         = 0;
	public int scrollY         = 0;
	public int scrollState     = 0; // 0 = x, 1 = y
	
	// address controller
	public int address         = 0;
	public int addressState    = 0; // 0 = address msb, 1 = address lsb
	
	// Set by control register 1:
	public int nameTableOffset = 0x2000;
	public int increment       = 1;
	public int pTableSprOffset = 0x0000; // pattern table sprites offset
	public int pTableBgOffset  = 0x0000; // pattern table backgrounds offset
	public int sprSize         = 0; // 0 = 8x8, 1 = 16x16
	public boolean nmi         = false;
	
	// Set by control register 2:
	public boolean monochrome = false;
	public boolean clipBg     = false;
	public boolean clipSpr    = false;
	public boolean showBg     = false;
	public boolean showSpr    = false;
	public int     bgColor    = 0;
	
	public int[] display = new int[0x10000]; // store RGB888 frame raster here
	
	public int currentScanline = 0; // max at 262
	
	//public boolean clearingBuffer = false;
	
	public void setMirroringMode(int mode){
		mirroring = mode;
	}
	
	public void incAddress(){
		address = (address + increment) & 0x3FFF;
	}
	
	public int readNext(){ // for I/O
		int ret = mem.read(address);
		incAddress();
		return ret;
	}
	
	public void writeNext(int value){ // for I/O
		mem.write(address, value);
		incAddress();
	}
	
	public void resetScanlines(){ // call during vblank
		currentScanline = 0;
	}
	
	private boolean sprite0Hit       = false;
	private boolean scanline8Sprites = false;
	
	public boolean getSprite0Hit(){
		boolean ret = sprite0Hit;
		sprite0Hit = false;
		return ret;
	}
	
	public boolean getScanline8Sprites(){
		boolean ret = scanline8Sprites;
		scanline8Sprites = false;
		return ret;
	}
	
	public void drawSpritesLine(){
		sprite0Hit = false;
		int bgColor = displayColors[mem.read(0x3F00) & 0x3F];
		int drawnSprites = 0;
		for(int i = 0xFC; i >= 0; i -= 4){
			int x = oam[i+3] & 0xFF;
			int y = oam[i+0] & 0xFF;
			if(currentScanline > (y + 7) || currentScanline < y) continue;
			drawnSprites++;
			int attributes     = oam[i+2] & 0xFF;
			boolean bgPriority = (attributes & 0b00100000) != 0;
			boolean xFlip      = (attributes & 0b01000000) != 0;
			boolean yFlip      = (attributes & 0b10000000) != 0;
			int spriteIndex    = (oam[i+1] & 0xFF);
			int paletteIndex   = (attributes & 0b00000011);
			for(int j = 0; j < 8; j++){ // j = byte selection & y offset
				int map0 = mem.read(pTableSprOffset + spriteIndex*16 + 0 + j); // j = y
				int map1 = mem.read(pTableSprOffset + spriteIndex*16 + 8 + j);
				for(int k = 0; k < 8; k++){ // k = color bit selection & x offset
					int absX = x + (xFlip ? k : 7-k); // absolute position of the pixel
					int absY = y + (yFlip ? 7-j : j);
					if(absY != currentScanline) continue;
					int colorIndex = ((map0 & (1 << k)) >> k) + ((map1 & (1 << k)) >> k) * 2;
					if(colorIndex == 0) continue; // transparent pixel
					int displayColor = displayColors[mem.read(0x3F10 + paletteIndex*4 + colorIndex) & 0x3F];
					if(i == 0 && display[(absY)*256 + (absX & 0xFF)] != bgColor){
						sprite0Hit = true;
					}
					if(!bgPriority || display[(absY)*256 + (absX & 0xFF)] == bgColor){
						display[(absY)*256 + (absX & 0xFF)] = displayColor;
					}
				}
			}
		}
		if(drawnSprites > 8) scanline8Sprites = true;
		//return sprite0Hit;
	}
	
	public void drawBackgroundLine(){
		if(currentScanline > 240){
			System.out.println("scanline overflow");
			return;
		}
		int bgColor = displayColors[mem.read(0x3F00) & 0x3F];
		int minIndex = (currentScanline / 8) * 32;
		int maxIndex = minIndex + 32;
		for(int i = minIndex; i < maxIndex; i++){ // nametable A (bottom left quadrant)
			int tileIndex = mem.read(nameTableOffset + i); // todo use scroll vals to get proper nametable offsets immediately
			int attributeTableOffset = nameTableOffset + 0x3C0;
			int x = (i % 32) * 8;
			int y = (i / 32) * 8;
			// TODO factor in scrolling and skip if off-screen here
			int attributeIndex = (i/0x80)*8 + (i&0x1F)/4;
			int square = (i&3)/2 + ((i&0x40) >> 5);
			int paletteIndex = ((mem.read(attributeTableOffset + attributeIndex) & (0b11 << square*2))) >> square*2;
			for(int j = 0; j < 8; j++){
				int map0 = mem.read(pTableBgOffset + tileIndex*16 + 0 + j); // j = y
				int map1 = mem.read(pTableBgOffset + tileIndex*16 + 8 + j);
				for(int k = 0; k < 8; k++){ // k = color bit selection & x offset
					int absX = x + (7 - k) - scrollX; // absolute position of the pixel
					if(absX < 0) continue;
					int absY = y + j;
					if(absY != currentScanline) continue;
					//absY += scrollY;
					//display[(absY)*256 + (absX)] = bgColor;
					int colorIndex = ((map0 & (1 << k)) >> k) + ((map1 & (1 << k)) >> k) * 2;
					int displayColor = displayColors[mem.read(0x3F00) & 0x3F];
					if(colorIndex != 0) {
						displayColor = displayColors[mem.read(0x3F00 + paletteIndex*4 + colorIndex) & 0x3F];
					}
					display[(absY)*256 + (absX)] = displayColor;
				}
			}
		}
		for(int i = minIndex; i < maxIndex; i++){  // nametable B (bottom right quadrant)
			int nameTableOffset = this.nameTableOffset + 0x400;
			int tileIndex = mem.read(nameTableOffset + i);
			int attributeTableOffset = nameTableOffset + 0x3C0;
			int x = (i % 32) * 8;
			int y = (i / 32) * 8;
			int attributeIndex = (i/0x80)*8 + (i&0x1F)/4;
			int square = (i&3)/2 + ((i&0x40) >> 5);
			int paletteIndex = ((mem.read(attributeTableOffset + attributeIndex) & (0b11 << square*2))) >> square*2;
			for(int j = 0; j < 8; j++){
				int map0 = mem.read(pTableBgOffset + tileIndex*16 + 0 + j); // j = y
				int map1 = mem.read(pTableBgOffset + tileIndex*16 + 8 + j);
				for(int k = 0; k < 8; k++){ // k = color bit selection & x offset
					int absX = x + (7 - k) + 256 - scrollX; // absolute position of the pixel
					if(absX < 0 || absX > 255) continue;
					int absY = y + j + scrollY; // + (yFlip ? 7-j : j);
					if(absY != currentScanline) continue;
					//display[(absY)*256 + (absX)] = bgColor;
					int colorIndex = ((map0 & (1 << k)) >> k) + ((map1 & (1 << k)) >> k) * 2;
					int displayColor = displayColors[mem.read(0x3F00)];
					if(colorIndex != 0) {
						displayColor = displayColors[mem.read(0x3F00 + paletteIndex*4 + colorIndex)];
					}
					display[(absY)*256 + (absX)] = displayColor;
				}
			}
		}
	}
	
	public void drawScanline(){ // should return 8-sprite thing and sprite 0 hit somehow
		if(currentScanline >= 240) return;
		int bgColor = displayColors[mem.read(0x3F00)];
		for(int x = 0; x < 256; x++){ // clear line first
			display[(currentScanline)*256 + x] = bgColor;
		}
		drawBackgroundLine();
		drawSpritesLine();
		currentScanline++;
	}
	
	// todo draw sprite and bg scanlines to seperate buffers, have a rasterize() function which draws to the final buffer with plugin hook
	// todo upscaler somewhere maybe
	
	// filter test
	//int bgColor = displayColors[mem.data[0x3F00]];
	//if(display[(absY + 2)*256 + ((absX + 2) & 0xFF)] == bgColor)
	//	display[(absY + 2)*256 + ((absX + 2) & 0xFF)] = shadowFilterTest(bgColor);
	// end filter test
	
	//private int shadowFilterTest(int color){
	//	int r = (color & 0xFF0000) >> 16;
	//	int g = (color & 0x00FF00) >>  8;
	//	int b = (color & 0x0000FF) >>  0;
	//	r = (r <= 0x22) ? (r + 0x22) : (r - 0x22);
	//	g = (g <= 0x22) ? (g + 0x22) : (g - 0x22);
	//	b = (b <= 0x22) ? (b + 0x22) : (b - 0x22);
	//	return ((r << 16) | (g << 8) | b);
	//}
	
	//public void setChrMem(Memory chrMem){
	//	this.chrMem = chrMem;
	//	resetMemory();
	//}
	
	public void resetMemory(){
		mem = new Memory(){
			byte[] data = new byte[0x4000];
			private int mirrorMap(int offset){
				offset &= 0x3FFF; // map 4xxx, 8xxx, Cxxx => 0xxx
				if(offset >= 0x3000 && offset < 0x3F00){ // 3000-3EFF => 2000-2EFF
					offset -= 0x1000;
				}
				// palette mirroring
				if(offset > 0x3F1F && offset < 0x4000){
					offset = 0x3F00 + (offset & 0x1F);
				}
				// alpha/bg palette entry 0 mirroring:
				if(offset > 0x3F00 && offset < 0x3F20 && (offset % 4 == 0)){
					return 0x3F00;
				}
				// nametable screen mirroring
				if(offset >= 0x2000 && offset < 0x3000){
					switch(mirroring){
						case MIRRORING_FOURSCREEN: return offset;
						case MIRRORING_ONESCREEN:  return offset & 0x23FF; // L1,L2,L3,L4 -> L1
						case MIRRORING_VERTICAL:   return offset & 0x27FF; // L3 -> L1, L4 -> L2
						case MIRRORING_HORIZONTAL:
							if(offset >= 0x2C00){
								return offset - 0x800; // L4 -> L2
							}
							if(offset >= 0x2800){
								return offset - 0x400; // L3 -> L2
							}
							if(offset >= 0x2400){
								return offset - 0x400; // L2 -> L1
							}
					}
				}
				return offset;
			}
			@Override
			public int read(int offset, boolean doEvents){
				offset = mirrorMap(offset);
				if(offset < 0x2000){
					return nes.getMapper().chrMem.read(offset);
				}
				return data[offset] & 0xFF;
			}
			@Override
			public void write(int offset, int value, boolean doEvents){
				offset = mirrorMap(offset);
				if(offset < 0x2000){
					nes.getMapper().chrMem.write(offset, value);
				} else {
					data[offset] = (byte)(value & 0xFF);
				}
			}
		};
	}
	
	public PPU2C02(NES nes){
		this.nes = nes;
		oam = new byte[0x100];
		resetMemory();
	}
}