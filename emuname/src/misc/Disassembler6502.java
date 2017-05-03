package net.shygoo.misc;

import net.shygoo.misc.Memory;

// 6502 disassembler

public class Disassembler6502 {
	private Memory mem;
	public int offset = 0;
	
	public final static String[] formats = {
		"%s",             // impl
		"%s #$%02X",      // imm
		"%s $%02X",       // zpg
		"%s $%02X,X",     // zpg,x 
		"%s $%02X,Y",     // zpg,y
		"%s $%02X%02X",   // abs
		"%s $%02X%02X,X", // abs,x
		"%s $%02X%02X,Y", // abs,y
		"%s ($%02X%02X)", // ind
		"%s ($%02X,X)",   // ind,x
		"%s ($%02X),Y",   // ind,y
		"%s $%04X",       // rel
		"%s$%02X"         // unknown
	};
	
	public final static int
	IMPL =  0, // opc
	IMM  =  1, // opc #${1}
	ZPG  =  2, // opc ${1}
	ZPGX =  3, // opc ${1},x
	ZPGY =  4, // opc ${1},y
	ABS  =  5, // opc ${2}{1}
	ABSX =  6, // opc ${2}{1},x
	ABSY =  7, // opc ${2}{1},y
	IND  =  8, // opc (${2}{1})
	INDX =  9, // opc (${1},x)
	INDY = 10, // opc (${1}),y
	REL  = 11, // todo change this
	UNK  = 12;
	
	public final static int[] lengths = { // amount to increment offset
		1, // IMPL
		2, // IMM 
		2, // ZPG 
		2, // ZPGX
		2, // ZPGY
		3, // ABS 
		3, // ABSX
		3, // ABSY
		3, // IND 
		2, // INDX
		2, // INDY
		2, // REL 
		1  // UNK
	};
	
	public final static String[] opnames = {
	//	00     01     02     03     04     05     06     07     08     09     0A     0B     0C     0D     0E     0F
		"BRK", "ORA", ";??", ";??", ";??", "ORA", "ASL", ";??", "PHP", "ORA", "ASL", ";??", ";??", "ORA", "ASL", ";??", // 00
		"BPL", "ORA", ";??", ";??", ";??", "ORA", "ASL", ";??", "CLC", "ORA", ";??", ";??", ";??", "ORA", "ASL", ";??", // 10
		"JSR", "AND", ";??", ";??", "BIT", "AND", "ROL", ";??", "PLP", "AND", "ROL", ";??", "BIT", "AND", "ROL", ";??", // 20
		"BMI", "AND", ";??", ";??", ";??", "AND", "ROL", ";??", "SEC", "AND", ";??", ";??", ";??", "AND", "ROL", ";??", // 30
		"RTI", "EOR", ";??", ";??", ";??", "EOR", "LSR", ";??", "PHA", "EOR", "LSR", ";??", "JMP", "EOR", "LSR", ";??", // 40
		"BVC", "EOR", ";??", ";??", ";??", "EOR", "LSR", ";??", "CLI", "EOR", ";??", ";??", ";??", "EOR", "LSR", ";??", // 50
		"RTS", "ADC", ";??", ";??", ";??", "ADC", "ROR", ";??", "PLA", "ADC", "ROR", ";??", "JMP", "ADC", "ROR", ";??", // 60
		"BVS", "ADC", ";??", ";??", ";??", "ADC", "ROR", ";??", "SEI", "ADC", ";??", ";??", ";??", "ADC", "ROR", ";??", // 70
		";??", "STA", ";??", ";??", "STY", "STA", "STX", ";??", "DEY", ";??", "TXA", ";??", "STY", "STA", "STX", ";??", // 80
		"BCC", "STA", ";??", ";??", "STY", "STA", "STX", ";??", "TYA", "STA", "TXS", ";??", ";??", "STA", ";??", ";??", // 90
		"LDY", "LDA", "LDX", ";??", "LDY", "LDA", "LDX", ";??", "TAY", "LDA", "TAX", ";??", "LDY", "LDA", "LDX", ";??", // A0
		"BCS", "LDA", ";??", ";??", "LDY", "LDA", "LDX", ";??", "CLV", "LDA", "TSX", ";??", "LDY", "LDA", "LDX", ";??", // B0
		"CPY", "CMP", ";??", ";??", "CPY", "CMP", "DEC", ";??", "INY", "CMP", "DEX", ";??", "CPY", "CMP", "DEC", ";??", // C0
		"BNE", "CMP", ";??", ";??", ";??", "CMP", "DEC", ";??", "CLD", "CMP", ";??", ";??", ";??", "CMP", "DEC", ";??", // D0
		"CPX", "SBC", ";??", ";??", "CPX", "SBC", "INC", ";??", "INX", "SBC", "NOP", ";??", "CPX", "SBC", "INC", ";??", // E0
		"BEQ", "SBC", ";??", ";??", ";??", "SBC", "INC", ";??", "SED", "SBC", ";??", ";??", ";??", "SBC", "INC", ";??", // F0
	};
	
	public final static int[] opsyns = {
	//	00     01     02     03     04     05     06     07     08     09     0A     0B     0C     0D     0E     0F
		IMPL,  INDX,  UNK,   UNK,   UNK,   ZPG,   ZPG,   UNK,   IMPL,  IMM,   IMPL,  UNK,   UNK,   ABS,   ABS,   UNK,   // 00
		REL,   INDY,  UNK,   UNK,   UNK,   ZPGX,  ZPGX,  UNK,   IMPL,  ABSY,  UNK,   UNK,   UNK,   ABSX,  ABSX,  UNK,   // 10
		ABS,   INDX,  UNK,   UNK,   ZPG,   ZPG,   ZPG,   UNK,   IMPL,  IMM,   IMPL,  UNK,   ABS,   ABS,   ABS,   UNK,   // 20
		REL,   INDY,  UNK,   UNK,   UNK,   ZPGX,  ZPGX,  UNK,   IMPL,  ABSY,  UNK,   UNK,   UNK,   ABSX,  ABSX,  UNK,   // 30
		IMPL,  INDX,  UNK,   UNK,   UNK,   ZPG,   ZPG,   UNK,   IMPL,  IMM,   IMPL,  UNK,   ABS,   ABS,   ABS,   UNK,   // 40
		REL,   INDY,  UNK,   UNK,   UNK,   ZPGX,  ZPGX,  UNK,   IMPL,  ABSY,  UNK,   UNK,   UNK,   ABSX,  ABSX,  UNK,   // 50
		IMPL,  INDX,  UNK,   UNK,   UNK,   ZPG,   ZPG,   UNK,   IMPL,  IMM,   IMPL,  UNK,   IND,   ABS,   ABS,   UNK,   // 60
		REL,   INDY,  UNK,   UNK,   UNK,   ZPGX,  ZPGX,  UNK,   IMPL,  ABSY,  UNK,   UNK,   UNK,   ABSX,  ABSX,  UNK,   // 70
		UNK,   INDX,  UNK,   UNK,   ZPG,   ZPG,   ZPG,   UNK,   IMPL,  UNK,   IMPL,  UNK,   ABS,   ABS,   ABS,   UNK,   // 80
		REL,   INDY,  UNK,   UNK,   ZPGX,  ZPGX,  ZPGY,  UNK,   IMPL,  ABSY,  IMPL,  UNK,   IMPL,  ABSX,  UNK,   UNK,   // 90
		IMM,   INDX,  IMM,   UNK,   ZPG,   ZPG,   ZPG,   UNK,   IMPL,  IMM,   IMPL,  UNK,   ABS,   ABS,   ABS,   UNK,   // A0
		REL,   INDY,  UNK,   UNK,   ZPGX,  ZPGX,  ZPGY,  UNK,   IMPL,  ABSY,  IMPL,  UNK,   ABSX,  ABSX,  ABSY,  UNK,   // B0
		IMM,   INDX,  UNK,   UNK,   ZPG,   ZPG,   ZPG,   UNK,   IMPL,  IMM,   IMPL,  UNK,   ABS,   ABS,   ABS,   UNK,   // C0
		REL,   INDY,  UNK,   UNK,   UNK,   ZPGX,  ZPGX,  UNK,   IMPL,  ABSY,  UNK,   UNK,   IMPL,  ABSX,  ABSX,  UNK,   // D0
		IMM,   INDX,  UNK,   UNK,   ZPG,   ZPG,   ZPG,   UNK,   IMPL,  IMM,   IMPL,  UNK,   ABS,   ABS,   ABS,   UNK,   // E0
		REL,   INDY,  UNK,   UNK,   UNK,   ZPGX,  ZPGX,  UNK,   IMPL,  ABSY,  UNK,   UNK,   IMPL,  ABSX,  ABSX,  UNK,   // F0
	};
	
	public final static int
	A_NA = 0, // none
	A_RD = 1, // read
	A_WT = 2, // write (and read-modify-write)
	A_RS = 3, // read stack
	A_WS = 4; // write stack
	
	public static final int[] opactions = {
	//	00     01     02     03     04     05     06     07     08     09     0A     0B     0C     0D     0E     0F
		A_NA,  A_WT,  A_NA,  A_NA,  A_NA,  A_RD,  A_WT,  A_NA,  A_WS,  A_NA,  A_NA,  A_NA,  A_NA,  A_RD,  A_WT,  A_NA,  // 00
		A_NA,  A_WT,  A_NA,  A_NA,  A_NA,  A_RD,  A_WT,  A_NA,  A_NA,  A_RD,  A_NA,  A_NA,  A_NA,  A_RD,  A_WT,  A_NA,  // 10
		A_WS,  A_WT,  A_NA,  A_NA,  A_RD,  A_RD,  A_WT,  A_NA,  A_RS,  A_NA,  A_NA,  A_NA,  A_RD,  A_RD,  A_WT,  A_NA,  // 20
		A_NA,  A_WT,  A_NA,  A_NA,  A_NA,  A_RD,  A_WT,  A_NA,  A_NA,  A_RD,  A_NA,  A_NA,  A_NA,  A_RD,  A_WT,  A_NA,  // 30
		A_RS,  A_WT,  A_NA,  A_NA,  A_NA,  A_RD,  A_WT,  A_NA,  A_WS,  A_NA,  A_NA,  A_NA,  A_NA,  A_RD,  A_WT,  A_NA,  // 40
		A_NA,  A_WT,  A_NA,  A_NA,  A_NA,  A_RD,  A_WT,  A_NA,  A_NA,  A_RD,  A_NA,  A_NA,  A_NA,  A_RD,  A_WT,  A_NA,  // 50
		A_RS,  A_WT,  A_NA,  A_NA,  A_NA,  A_WT,  A_WT,  A_NA,  A_RS,  A_NA,  A_NA,  A_NA,  A_NA,  A_WT,  A_WT,  A_NA,  // 60
		A_NA,  A_WT,  A_NA,  A_NA,  A_NA,  A_WT,  A_WT,  A_NA,  A_NA,  A_WT,  A_NA,  A_NA,  A_NA,  A_WT,  A_WT,  A_NA,  // 70
		A_NA,  A_WT,  A_NA,  A_NA,  A_WT,  A_WT,  A_WT,  A_NA,  A_NA,  A_NA,  A_NA,  A_NA,  A_WT,  A_WT,  A_WT,  A_NA,  // 80
		A_NA,  A_WT,  A_NA,  A_NA,  A_WT,  A_WT,  A_WT,  A_NA,  A_NA,  A_WT,  A_NA,  A_NA,  A_NA,  A_WT,  A_NA,  A_NA,  // 90
		A_NA,  A_RD,  A_NA,  A_NA,  A_RD,  A_RD,  A_RD,  A_NA,  A_NA,  A_NA,  A_NA,  A_NA,  A_RD,  A_RD,  A_RD,  A_NA,  // A0
		A_NA,  A_RD,  A_NA,  A_NA,  A_RD,  A_RD,  A_RD,  A_NA,  A_NA,  A_RD,  A_NA,  A_NA,  A_RD,  A_RD,  A_RD,  A_NA,  // B0
		A_NA,  A_RD,  A_NA,  A_NA,  A_RD,  A_RD,  A_WT,  A_NA,  A_NA,  A_NA,  A_NA,  A_NA,  A_RD,  A_RD,  A_WT,  A_NA,  // C0
		A_NA,  A_RD,  A_NA,  A_NA,  A_NA,  A_RD,  A_WT,  A_NA,  A_NA,  A_RD,  A_NA,  A_NA,  A_NA,  A_RD,  A_WT,  A_NA,  // D0
		A_NA,  A_WT,  A_NA,  A_NA,  A_RD,  A_WT,  A_WT,  A_NA,  A_NA,  A_NA,  A_NA,  A_NA,  A_RD,  A_WT,  A_WT,  A_NA,  // E0
		A_NA,  A_WT,  A_NA,  A_NA,  A_NA,  A_WT,  A_WT,  A_NA,  A_NA,  A_RD,  A_NA,  A_NA,  A_NA,  A_WT,  A_WT,  A_NA,  // F0
	};
	
	private int lastlen = 0; // save length of last decoded op here
	
	public String decode(int o){
		int op = mem.read(o, false) & 0xFF;
		String name = opnames[op];
		int syn = opsyns[op];
		String format = formats[syn];
		int len = lengths[syn];
		lastlen = len;
		if(syn == REL){
			return String.format(format, name, o + mem.read(o+1, false) + 2);
		}
		if(syn == UNK){
			return String.format(format, name, op);
		}
		switch(len){
			case 1: return name; // assume impl
			case 2: return String.format(format, name, mem.read(o+1, false) & 0xFF); // assume imm/zpg/ind,r
			case 3: return String.format(format, name, mem.read(o+2, false) & 0xFF, mem.read(o+1, false) & 0xFF); // assume abs
		}
		return "";
	}
	
	public String next(){ // decode, and increment internal offset
		String result = decode(offset);
		offset += lastlen; // length of opcode
		return result;
	}
	
	public void reset(){
		offset = 0;
	}
	
	public void setOffset(int offset){
		this.offset = offset;
	}
	
	public int getOffset(){
		return offset;
	}
	
	public Disassembler6502(Memory mem){
		this.mem = mem;
		//System.out.printf("Disassembler6502 : assigned to memory %d\n", mem.data.length);
	}
}