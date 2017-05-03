public class Cartridge {

	// INES file format header offsets
	public static final int
	INES_HEADERSIZE = 16,
	INES_HEADER_PRGROM_SIZE = 0x04,
	INES_HEADER_CHRROM_SIZE = 0x05,
	INES_HEADER_FLAGS6      = 0x06,
	INES_HEADER_FLAGS7      = 0x07;
	
	public static final int
	MIRRORING_VERTICAL   = 0,
	MIRRORING_HORIZONTAL = 1,
	MIRRORING_ONESCREEN  = 2,
	MIRRORING_FOURSCEREN = 3;

	private final byte[] rawFileData;
	private final Mapper mapper;
	
	private final int prgBankCount;
	private final int chrBankCount;
	
	private final int mirroring;
		
	public Cartridge(NES nes, String romPath){
	
		try {
			rawFileData = Files.readAllBytes(path);
		} catch(Exception e){
			System.out.println("Error: Couldn't read file");
			return;
		}
	
		prgBankCount = rawFileData[INES_HEADER_PRGROM_SIZE] & 0xFF;
		chrBankCount = rawFileData[INES_HEADER_CHRROM_SIZE] & 0xFF;
		
		int prgRomSize = romPrgBanks * 0x4000;
		int chrRomSize = rawFileData[INES_HEADER_CHRROM_SIZE] * 0x2000;
		
		int mapperId   = ((rawFileData[INES_HEADER_FLAGS6] & 0xF0) >> 4) | (rawFileData[INES_HEADER_FLAGS7] & 0xF0);
		
		boolean fourScreenMirroring = (rawFileData[INES_HEADER_FLAGS6] & 0b1000) != 0;
		boolean verticalMirroring = (rawFileData[INES_HEADER_FLAGS6] & 1) != 0;
	
		// Select PPU nametable mirroring
		if(fourScreenMirroring){
			mirroring = MIRRORING_FOURSCREEN;
			System.out.println(" PPU Mirroring: Four-screen");
		} else if(verticalMirroring){
			mirroring = MIRRORING_VERTICAL;
			System.out.println(" PPU Mirroring: Vertical");
		} else {
			mirroring = MIRRORING_HORIZONTAL;
			System.out.println(" PPU Mirroring: Horizontal");
		}
	
		mapper = mapperFactory.createMapperById(mapperId);
		mapper.setPrgsDefault();
		
		System.out.printf(" Mapper: %s (%02X)\n", mapper.getName(), mapperId);
		
	}
	
	public int getPrgBankCount(){
		return prgBankCount;
	}
	
	public int getChrBankCount(){
		return chrBankCount;
	}
	
	public int getMirroring(){
		return mirroring;
	}
	
}