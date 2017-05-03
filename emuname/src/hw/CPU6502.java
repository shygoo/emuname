package net.shygoo.hw;

import net.shygoo.misc.Memory;

import net.shygoo.misc.Disassembler6502;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class CPU6502 {
	
	public Disassembler6502 dis;
	
	public Memory mem; // CPU RAM todo hide
	
	public int totalCycles = 0; // number of cycles current instruction is taking
	
	public String errorstr = "";
	
 // VECTORS
	
	private static final int V_NMI = 0xFFFA; // non maskable interrupts
	private static final int V_RST = 0xFFFC; // resets
	private static final int V_IRQ = 0xFFFE; // interrupt requests/BRK
	
 // STATUS FLAGS
	
	private static final int F_CARRY      = 0b00000001; // 0 C
	private static final int F_ZERO       = 0b00000010; // 1 Z
	private static final int F_IRQDISABLE = 0b00000100; // 2 I
	private static final int F_DECMODE    = 0b00001000; // 3 D
	private static final int F_BREAK      = 0b00010000; // 4 B
	private static final int F_UNUSED     = 0b00100000; // 5 -
	private static final int F_OVERFLOW   = 0b01000000; // 6 V
	private static final int F_SIGN       = 0b10000000; // 7 S
	
 // REGISTERS
	
	private int PC = 0;    // program counter
	private int AC = 0;    // accumulator
	private int XR = 0;    // x index register
	private int YR = 0;    // y index register
	private int SP = 0xFD; // stack pointer
	private int SR = 0b00100100; // status register, bit 5 is always 1
	
 // STATUS REGISTER MANAGEMENT
	
	private void statusCopyBits(int value, int mask){
		// copy bits from a value to the status register given a mask representing which bits to copy
		SR &= ~mask & 0xFF;
		SR |= value & mask;
	}	
	private void setSign(int regval){ // set SR sign bit to the sign bit of regval
		SR = ((regval & F_SIGN) != 0) ? (SR | F_SIGN) : (SR & ~F_SIGN);
	}
	private void setOverflow(boolean set){ // set SR overflow bit if condition is met
		SR = (set) ? (SR | F_OVERFLOW) : (SR & ~F_OVERFLOW);
	}
	private void setBreak(int val){ // set break if 1
		SR = (val != 0) ? (SR | F_BREAK) : (SR & ~F_BREAK);
	}
	private void setDecMode(boolean set){ // set decimal mode if condition is met
		SR = (set) ? (SR | F_DECMODE) : (SR & ~F_DECMODE);
	}
	private void setIrqDisable(boolean set){ // set interrupt disable if condition is met
		SR = (set) ? (SR | F_IRQDISABLE) : (SR & ~F_IRQDISABLE);
	}
	private void setZero(int regval){ // set zero if regval is 0
		SR = ((regval&0xFF) == 0) ? (SR | F_ZERO) : (SR & ~F_ZERO);
	}
	private void setCarry(boolean set){ // set carry if condition is met
		SR = (set) ? (SR | F_CARRY) : (SR & ~F_CARRY);
	}
	private void setZeroSign(int regval){ // shorthand for setZero & setSign combo
		setZero(regval);
		setSign(regval);
	}
	private boolean getSign(){
		return (SR & F_SIGN) != 0;
	}
	private boolean getOverflow(){
		return (SR & F_OVERFLOW) != 0;
	}
	private boolean getBreak(){
		return (SR & F_BREAK) != 0;
	}
	private boolean getDecMode(){
		return (SR & F_DECMODE) != 0;
	}
	private boolean getIrqDisable(){
		return (SR & F_IRQDISABLE) != 0;
	}
	private boolean getZero(){
		return (SR & F_ZERO) != 0;
	}
	private boolean getCarry(){
		return (SR & F_CARRY) != 0;
	}
	
 // MISC
	
	private void incPC(int offset){ // increment program counter by offset
		PC += offset;
	}
	public void setPC(int offset){
		PC = offset;
	}
	public void clock(int cycles){ // increment clock cycles
		//if(!dbgClockDisabled)
		totalCycles += cycles;
		//frameCycles += cycles;
	}
	private int getCurrentCycles(){ // return clock cycles and reset
		int t = totalCycles;
		totalCycles = 0;
		return t;
	}
	private void fixRegisterBits(){ // clear the 'fake' upper bits from the registers, use at the end of every step()
		AC &= 0xFF;
		XR &= 0xFF;
		YR &= 0xFF;
		SP &= 0xFF;
		PC &= 0xFFFF;
	}
	private boolean checkSignOverflow(int o1, int o2, int result){
		// check for sign overflow given two operands
		byte s1 = (byte)(o1 & F_SIGN);
		byte s2 = (byte)(o2 & F_SIGN);
		byte rs = (byte)(result & F_SIGN);
		if(s1 == s2 && rs != s1){ // operand signs match, result sign doesnt match
			return true;
		}
		return false;
	}
	
 // OPCODE BASE FUNCTIONS / MEMORY MANAGEMENT

	private int getMem8(int offset){ // fetch byte from memory at offset, cost cycle
		clock(1);
		return mem.read(offset, true); //(int)(mem[offset] & 0xFF);
	}
	private int getMem16(int offset){ // little endian
		return getMem8(offset) | (getMem8(offset+1) << 8);
	}
	private int getImm8(){ // fetch byte following PC
		return getMem8(PC+1);
	}
	private int getImm16(){ // fetch two bytes following PC, little endian
		return getMem16(PC+1);
	}
	private int getZp8(){ // fetch byte from zero page using immediate byte as zp offset
		return getMem8(getImm8());
	}
	private int getZpX8(){ // fetch byte from zero page using (immediate byte + XR value) as zp offset
		clock(1);
		return getMem8((getImm8() + XR) & 0xFF);
	}
	private int getZpY8(){ // fetch byte from zero page using (immediate byte + YR value) as zp offset
		clock(1);
		return getMem8((getImm8() + YR) & 0xFF);
	}
	private int getIndX8(){ // fetch byte from memory using 16 byte address from the zero page where (imm + XR) is the zp offset
		int zpOffset = getImm8() + XR;
		clock(1);
		if(zpOffset > 0xFF){
			zpOffset &= 0xFF;
		}
		if(zpOffset == 0xFF){ // special case where lsb is on page boundary
			return getMem8(getMem8(zpOffset) | (getMem8(0x00) << 8));
		}
		return getMem8(getMem16(zpOffset));
	}
	private int getIndY8(){ // fetch byte from memory using (16 byte address from zero page + XR) where imm is the zp offset
		int zpOffset = getImm8();
		int offset;
		if(zpOffset == 0xFF){
			offset = (getMem8(0xFF) | (getMem8(0x00) << 8)) + YR;
		} else {
			offset = getMem16(getImm8()) + YR;
		}
		if(offset > 0xFFFF){
			offset &= 0xFFFF;
			clock(1); // additional page cross cycle
		}
		return getMem8(offset);
	}
	private int getAbs8(){ // fetch byte from absolute address using immediate 16bits
		return getMem8(getImm16());
	}
	private int getAbsX8(){ // fetch byte from absolute address using immediate 16bits + XR
		int offset = getImm16() + XR;
		if(offset > 0xFFFF){
			offset &= 0xFFFF;
			clock(1);
		}
		return getMem8(offset);
	}
	private int getAbsY8(){ // fetch byte from absolute address using immediate 16bits + YR
		int offset = getImm16() + YR;
		if(offset > 0xFFFF){
			offset &= 0xFFFF;
			clock(1);
		}
		return getMem8(offset);
	}
	private void setMem8(int offset, int value){
		//mem[offset] = (byte)value;
		mem.write(offset, value);
		clock(1);
	}
	/*private void setMem16(int offset, int value){ // little endian
		mem[offset] = (byte)value;
		clock(1);
	}*/
	private void setZp8(int value){
		setMem8(getImm8(), value);
	}
	private void setZpX8(int value){
		setMem8((getImm8() + XR) & 0xFF, value);
		clock(1);
	}
	private void setZpY8(int value){
		setMem8((getImm8() + YR) & 0xFF, value);
		clock(1);
	}
	private void setAbs8(int value){
		setMem8(getImm16(), value);
	}
	private void setAbsX8(int value){
		setMem8((getImm16() + XR) & 0xFFFF, value);
		clock(1);
	}
	private void setAbsY8(int value){
		setMem8((getImm16() + YR) & 0xFFFF, value);
		clock(1);
	}
	private void setIndX8(int value){
		int zpOffset = (getImm8() + XR) & 0xFF;
		if(zpOffset == 0xFF){ // lsb of address is on page boundary, msb is at 0
			setMem8(getMem8(0xFF) | (getMem8(0x00) << 8), value);
			clock(1);
		} else {
			setMem8(getMem16(zpOffset), value);
			clock(1);
		}
	}
	private void setIndY8(int value){
		int zpOffset = getImm8();
		if(zpOffset == 0xFF){
			setMem8(((getMem8(0xFF) | (getMem8(0x00) << 8)) + YR) & 0xFFFF, value);
		} else {
			setMem8((getMem16(zpOffset) + YR) & 0xFFFF, value);
		}
		clock(1);
	}
	private void push8(int value){ // push 8 bits onto stack
		//System.out.printf("pushed %02x\n", value);
		setMem8(0x100 + SP, value);
		SP = (SP - 1) & 0xFF;
	}
	private void push16(int value){ // push 16 bits onto stack
		push8((value & 0xFF00) >> 8);
		push8(value & 0xFF);
	}
	private int pull8(){ // pull 8 bits from stack
		//System.out.printf("pulled ()\n");
		SP = (SP + 1) & 0xFF;
		return getMem8(0x100 + SP);
	}
	private int pull16(){ // pull 16 bits from stack
		return pull8() | (pull8() << 8);
	}
	private boolean interrupt(int vector){ // if interrupt disable is set, return false, else perform an interrupt and return true
		if(getIrqDisable()){
			setBreak(1);
			push16(PC);
			push8(SR);
			setPC(getMem16(V_IRQ));
			setIrqDisable(true);
			return true;
		} else {
			return false;
		}
	}
	public void nonMaskableInterrupt(){
		//System.out.printf("NMI occurred @ %04X [%02X] [%02X]\n", PC, mem[PC], mem[PC-1]);
		setBreak(1);
		push16(PC);
		push8(SR | F_UNUSED | F_BREAK);
		setPC(getMem16(V_NMI));
		setIrqDisable(true);
	}
	private void rBranch(boolean condition){ // do a relative branch
		byte relativeOffset = (byte)getImm8(); // casting to byte signs the value here, cost 1 cycle
		if(condition){
			clock(1);
			incPC(relativeOffset);
			if(PC > 0xFFFF){
				PC &= 0xFFFF;
				clock(1);  // add cycle for page cross
			}
		}
	}
	
	public void reset(){
		setPC(getMem16(V_RST));
	}
	
	// temporary vars
	private int t1;
	private int t2;
	
	public int step(){ // execute instruction at PC and return number of cycles taken
		//prestep();
		// if doCancelStep return 0;
		//if(dbgPcHistoryEnabled)
		//System.out.printf("[%04X] %s\n", PC, dis.decode(PC));
		
		//if(dbgPcHistoryEnabled){
		//	dbgPcHistoryPush();
		//}
		
		switch(getMem8(PC)){
			case 0x00:  // brk
				if(interrupt(0xFFFE)){
					setBreak(1);
				} else {
					incPC(1);
				}
				break;
			case 0x01: // ora (ind,x)
				AC |= getIndX8();
				setZeroSign(AC);
				incPC(2);
				break;
			// 02 03 04
			case 0x05: // ora zpg
				AC |= getZp8();
				setZeroSign(AC);
				incPC(2);
				break;
			case 0x06: // asl zpg
				t1 = getImm8();
				setMem8(t1, t2 = getMem8(t1) << 1);
				setZeroSign(t2);
				setCarry(t2 > 0xFF);
				incPC(2);
				clock(1);
				break;
			// 07
			case 0x08: // php
				push8(SR | F_BREAK);
				incPC(1);
				clock(1);
				break;
			case 0x09: // ora imm
				AC |= getImm8();
				setZeroSign(AC);
				incPC(2);
				break;
			case 0x0A: // asl
				AC = (AC << 1);
				setZeroSign(AC);
				setCarry(AC > 0xFF);
				incPC(1);
				break;
			// 0b 0c
			case 0x0D:  // ora abs
				AC |= getAbs8();
				setZeroSign(AC);
				incPC(3);
				break;
			case 0x0E: // asl abs
				t1 = getImm16();
				setMem8(t1, t2 = getMem8(t1) << 1);
				setZeroSign(t2);
				setCarry(t2 > 0xFF);
				incPC(3);
				clock(1);
				break;
			// 0f
			case 0x10: // bpl
				rBranch(!getSign());
				incPC(2);
				break;
			case 0x11: // ora (ind),y
				AC |= getIndY8();
				setZeroSign(AC);
				incPC(2);
				break; 
			// 12 13 14
			case 0x15: // ora zpg,x
				AC |= getZpX8();
				setZeroSign(AC);
				incPC(2);
				break;
			case 0x16: // asl zpg,x
				t1 = (getImm8() + XR) & 0xFF;
				setMem8(t1, t2 = getMem8(t1) << 1);
				setZeroSign(t2);
				setCarry(t2 > 0xFF);
				incPC(2);
				clock(2);
				break;
			// 17
			case 0x18: // clc
				setCarry(false);
				incPC(1);
				break;
			case 0x19: // ora abs,y
				AC |= getAbsY8();
				setZeroSign(AC);
				incPC(3);
				break;
			// 1a 1b 1c
			case 0x1D: // ora abs,x
				AC |= getAbsX8();
				setZeroSign(AC);
				incPC(3);
				break;
			case 0x1E: // asl abs,x
				t1 = (getImm16() + XR) & 0xFFFF;
				setMem8(t1, t2 = getMem8(t1) << 1);
				setZeroSign(t2);
				setCarry(t2 > 0xFF);
				incPC(3);
				clock(1);
				break;
			// 1f
			case 0x20: // jsr
				//System.out.println("JSR ------------------------------------------");
				t1 = getImm16();
				incPC(3);
				push16(PC - 1);
				setPC(t1);
				break;
			case 0x21: // and (ind,x)
				AC &= getIndX8();
				setZeroSign(AC);
				incPC(2);
				break;
			// 22 23
			case 0x24: // bit zpg
				t1 = getZp8();
				t2 = AC & t1;
				statusCopyBits(t1, F_OVERFLOW | F_SIGN);
				setZero(t2);
				incPC(2);
				break;
			case 0x25: // AND zpg
				AC &= getZp8();
				setZeroSign(AC);
				incPC(2);
				break;
			case 0x26: // rol zpg
				t1 = getImm8();
				t2 = getMem8(t1);
				t2 <<= 1;
				t2 |= getCarry() ? 1 : 0;
				setCarry(t2 > 0xFF);
				setMem8(t1, t2);
				setZeroSign(t2);
				incPC(2);
				clock(2);
				break; 
			// 27
			case 0x28: // plp
				SR = pull8() & ~F_BREAK | F_UNUSED;
				incPC(1);
				clock(2);
				break;
			case 0x29: // AND imm
				AC &= getImm8();
				setZeroSign(AC);
				incPC(2);
				break;
			case 0x2A: // rol  ;impl ac
				AC <<= 1;
				AC |= getCarry() ? 1 : 0;
				setCarry(AC > 0xFF);
				setZeroSign(AC);
				incPC(1);
				clock(1);
				break; 
			// 2b
			case 0x2C: // bit abs
				t1 = getAbs8();
				t2 = (AC & t1);
				statusCopyBits(t1, F_OVERFLOW | F_SIGN);
				setZero(t2);
				incPC(3);
				break;
			case 0x2D: // and abs
				AC &= getAbs8();
				setZeroSign(AC);
				incPC(3);
				break;
			case 0x2E: // rol abs
				t1 = getImm16();
				t2 = getMem8(t1);
				t2 <<= 1;
				t2 |= getCarry() ? 1 : 0;
				setCarry(t2 > 0xFF);
				setMem8(t1, t2);
				setZeroSign(t2);
				incPC(3);
				clock(3);
				break; 
			// 2f
			case 0x30: // bmi
				rBranch(getSign());
				incPC(2);
				break;
			case 0x31: // and (ind),y
				AC &= getIndY8();
				setZeroSign(AC);
				incPC(2);
				break;
			// 32 33 34
			case 0x35: // and zpg,x
				AC &= getZpX8();
				setZeroSign(AC);
				incPC(2);
				break;
			case 0x36: // rol zpg,x
				t1 = (getImm8()+XR) & 0xFF;
				t2 = getMem8(t1);
				t2 <<= 1;
				t2 |= getCarry() ? 1 : 0;
				setCarry(t2 > 0xFF);
				setZeroSign(t2);
				setMem8(t1, t2);
				incPC(2);
				clock(3);
				break; 
			// 37
			case 0x38: // sec
				setCarry(true);
				incPC(1);
				clock(1);
				break;
			case 0x39: // and abs,y
				AC &= getAbsY8();
				setZeroSign(AC);
				incPC(3);
				break;
			// 3a 3b 3c
			case 0x3D: // and abs,x
				AC &= getAbsX8();
				setZeroSign(AC);
				incPC(3);
				break;
			case 0x3E: // rol abs,x
				t1 = (getImm16() + XR) & 0xFFFF;
				t2 = getMem8(t1);
				t2 <<= 1;
				t2 |= getCarry() ? 1 : 0;
				setCarry(t2 > 0xFF);
				setZeroSign(t2);
				setMem8(t1, t2);
				incPC(3);
				clock(3);
				break; 
			// 3f
			case 0x40: // rti
				SR = pull8() | F_UNUSED & ~F_BREAK;
				setPC(pull16());
				clock(2);
				break;
			case 0x41: // eor ind,x
				AC ^= getIndX8();
				setZeroSign(AC);
				incPC(2);
				break;
			// 42 43 44
			case 0x45: // eor zpg
				AC ^= getZp8();
				setZeroSign(AC);
				incPC(2);
				break;
			case 0x46: // lsr zpg
				t1 = getImm8();
				t2 = getMem8(t1);
				setCarry((t2 & 1) == 1);
				setMem8(t1, t2 >>= 1);
				setZeroSign(t2);
				incPC(2);
				clock(1);
				break;
			// 47
			case 0x48: // pha
				push8(AC);
				incPC(1);
				clock(1);
				break;
			case 0x49: // eor imm
				AC ^= getImm8();
				setZeroSign(AC);
				incPC(2);
				break;
			case 0x4A: // lsr  ;impl ac
				t1 = AC & 1;
				AC = (AC >> 1);
				setZeroSign(AC);
				setCarry(t1 == 1);
				incPC(1);
				clock(1);
				break;
			// 4b
			case 0x4C: // jmp abs
				setPC(getImm16());
				break;
			case 0x4D: // eor abs
				AC ^= getAbs8();
				setZeroSign(AC);
				incPC(3);
				break;
			case 0x4E: // lsr abs
				t1 = getImm16() & 0xFFFF;
				t2 = getMem8(t1);
				setCarry((t2 & 1) == 1);
				setMem8(t1, t2 >>= 1);
				setZeroSign(t2);
				incPC(3);
				clock(2);
				break;
			// 4f
			case 0x50: // bvc
				rBranch(!getOverflow());
				incPC(2);
				break;
			case 0x51: // eor ind,y
				AC ^= getIndY8();
				setZeroSign(AC);
				incPC(2);
				break;
			// 52 53 54
			case 0x55: // eor zpg,x
				AC ^= getZpX8();
				setZeroSign(AC);
				incPC(2);
				break;
			case 0x56: // lsr zpg,x
				t1 = (getImm8() + XR) & 0xFF;
				t2 = getMem8(t1);
				setCarry((t2 & 1) == 1);
				setMem8(t1, t2 >>= 1);
				setZeroSign(t2);
				incPC(2);
				clock(2);
				break;
			// 57
			case 0x58: // CLI
				setIrqDisable(false);
				incPC(1);
				clock(1);
				break;
			case 0x59: // eor abs,y
				AC ^= getAbsY8();
				setZeroSign(AC);
				incPC(3);
				break;
			// 5a 5b 5c
			case 0x5D: // eor abs,x
				AC ^= getAbsX8();
				setZeroSign(AC);
				incPC(3);
				break;
			case 0x5E: // lsr abs,x
				t1 = (getImm16() + XR) & 0xFFFF;
				t2 = getMem8(t1);
				setCarry((t2 & 1) == 1);
				setMem8(t1, t2 >>= 1);
				setZeroSign(t2);
				incPC(3);
				clock(2);
				break;
			// 5f
			case 0x60: // rts
				//System.out.println("RTS");
				setPC(pull16() + 1);
				clock(3);
				break;
			case 0x61: // adc ind,x
				t1 = getIndX8(); //getMem8(getMem16((getImm8() + XR) & 0xFF));
				t2 = AC;
				AC += t1 + (getCarry()?1:0);
				setOverflow(checkSignOverflow(t1, t2, AC));
				setCarry(AC > 0xFF);
				setZeroSign(AC);
				incPC(2);
				break;
			// 62 63 64
			case 0x65: // adc zpg
				t1 = getZp8();
				t2 = AC;
				AC += t1 + (getCarry()?1:0);
				setOverflow(checkSignOverflow(t1, t2, AC));
				setCarry(AC > 0xFF);
				setZeroSign(AC);
				incPC(2);
				break;
			case 0x66: // ror zpg
				t1 = getZp8();
				t2 = t1 & 1;
				t1 >>= 1;
				t1 |= (getCarry()?1:0) << 7;
				setCarry(t2 == 1);
				setZp8(t1);
				setZeroSign(t1);
				incPC(2);
				break;
			// 67
			case 0x68: // pla
				AC = pull8();
				setZeroSign(AC);
				incPC(1);
				clock(2);
				break;
			case 0x69: // adc imm
				t1 = getImm8();
				t2 = AC;
				AC += t1 + (getCarry()?1:0);
				setOverflow(checkSignOverflow(t1, t2, AC));
				setCarry(AC > 0xFF);
				setZeroSign(AC);
				incPC(2);
				break;
			case 0x6A: // ror ac
				t1 = AC & 1;
				AC >>= 1;
				AC |= (getCarry()?1:0) << 7;
				setCarry(t1 == 1);
				setZeroSign(AC);
				incPC(1);
				clock(2);
				break;
			// 6b
			case 0x6C: // JMP ind
				int fetch = getImm16();
				if((fetch & 0xFF) == 0xFF){ // original 6502 page boundary bug
					setPC(getMem8(fetch) | (getMem8(fetch & 0xFF00) << 8));
				} else {
					setPC(getMem16(getImm16()));
				}
				break;
			case 0x6D: // adc abs
				t1 = getAbs8();
				t2 = AC;
				AC += t1 + (getCarry()?1:0);
				setOverflow(checkSignOverflow(t1, t2, AC));
				setCarry(AC > 0xFF);
				setZeroSign(AC);
				incPC(3);
				break;
			case 0x6E: // ror abs
				t1 = getAbs8();
				t2 = t1 & 1;
				t1 >>= 1;
				t1 |= (getCarry()?1:0) << 7;
				setCarry(t2 == 1);
				setAbs8(t1);
				setZeroSign(t1);
				incPC(3);
				break;
			// 6f
			case 0x70: // bvs
				rBranch(getOverflow());
				incPC(2);
				break;
			case 0x71: // adc ind,y
				t1 = getIndY8(); //getMem8((getMem16(getImm8()) + YR) & 0xFFFF); // todo clock cycle for page cross 
				t2 = AC;
				AC += t1 + (getCarry()?1:0);
				setOverflow(checkSignOverflow(t1, t2, AC));
				setCarry(AC > 0xFF);
				setZeroSign(AC);
				incPC(2);
				//System.out.println("adc ind,y");
				break;
			// 72 73 74
			case 0x75: // adc zpg,x
				t1 = getZpX8(); //(getZp8() + XR) & 0xFF;
				t2 = AC;
				AC += t1 + (getCarry()?1:0);
				setOverflow(checkSignOverflow(t1, t2, AC));
				setCarry(AC > 0xFF);
				setZeroSign(AC);
				incPC(2);
				break;
			case 0x76: // ror zpg,x
				t1 = getZpX8();
				t2 = t1 & 1;
				t1 >>= 1;
				t1 |= (getCarry()?1:0) << 7;
				setCarry(t2 == 1);
				setZpX8(t1);
				setZeroSign(t1);
				incPC(2);
				break;
			// 77
			case 0x78: // SEI
				setIrqDisable(true);
				incPC(1);
				clock(1);
				break;
			case 0x79: // adc abs,y
				t1 = getAbsY8(); //(getAbs8() + YR) & 0xFFFF;
				t2 = AC;
				AC += t1 + (getCarry()?1:0);
				setOverflow(checkSignOverflow(t1, t2, AC));
				setCarry(AC > 0xFF);
				setZeroSign(AC);
				incPC(3);
				break;
			// 7a 7b 7c
			case 0x7D: // adc abs,x
				t1 = getAbsX8(); //(getAbs8() + XR) & 0xFFFF;
				t2 = AC;
				AC += t1 + (getCarry()?1:0);
				setOverflow(checkSignOverflow(t1, t2, AC));
				setCarry(AC > 0xFF);
				setZeroSign(AC);
				incPC(3);
				break;
			case 0x7E: // ror abs,x
				t1 = getAbsX8();
				t2 = t1 & 1;
				t1 >>= 1;
				t1 |= (getCarry()?1:0) << 7;
				setCarry(t2 == 1);
				setAbsX8(t1);
				setZeroSign(t1);
				incPC(3);
				break;
			// 7f 80
			case 0x81: // sta ind,x
				setIndX8(AC);
				incPC(2);
				break;
			// 82 83
			case 0x84: // sty zp
				setZp8(YR);
				incPC(2);
				break;
			case 0x85: // sta zp
				setZp8(AC);
				incPC(2);
				break;
			case 0x86: // stx zp
				setZp8(XR);
				incPC(2);
				break;
			// 87
			case 0x88: // dey
				YR--;
				setZeroSign(YR);
				clock(1);
				incPC(1);
				break;
			// 89
			case 0x8A: // txa
				AC = XR;
				setZeroSign(AC);
				incPC(1);
				clock(1);
				break;
			// 8b
			case 0x8C: // sty abs
				setAbs8(YR);
				incPC(3);
				break;
			case 0x8D: // sta abs
				setAbs8(AC);
				incPC(3);
				break;
			case 0x8E: // stx abs
				setAbs8(XR);
				incPC(3);
				break;
			// 8f
			case 0x90: // bcc
				rBranch(!getCarry());
				incPC(2);
				break;
			case 0x91: // sta ind,y
				setIndY8(AC);
				incPC(2);
				//System.out.println("sta ind,y");
				break;
			// 92 93
			case 0x94: // sty zp,x
				setZpX8(YR);
				incPC(2);
				break;
			case 0x95: // sta zp,x
				setZpX8(AC);
				incPC(2);
				break;
			case 0x96: // stx zp,y
				setZpY8(XR);
				incPC(2);
				break;
			// 97
			case 0x98: // tya
				AC = YR;
				setZeroSign(AC);
				incPC(1);
				clock(1);
				break;
			case 0x99: // sta abs,y
				setAbsY8(AC);
				incPC(3);
				break;
			case 0x9A: // txs
				SP = XR;
				incPC(1);
				clock(1);
				break;
			// 9b 9c
			case 0x9D: // sta abs,x
				setAbsX8(AC);
				incPC(3);
				break;
			// 9e 9f
			case 0xA0: // ldy imm
				YR = getImm8();
				setZeroSign(YR);
				incPC(2);
				break;
			case 0xA1: // lda ind,x
				AC = getIndX8();
				setZeroSign(AC);
				incPC(2);
				break;
			case 0xA2: // ldx imm
				XR = getImm8();
				setZeroSign(XR);
				incPC(2);
				break;
			// a3
			case 0xA4: // ldy zpg
				YR = getZp8();
				setZeroSign(YR);
				incPC(2);
				break;
			case 0xA5: // lda zpg
				AC = getZp8();
				setZeroSign(AC);
				incPC(2);
				break;
			case 0xA6: // ldx zpg
				XR = getZp8();
				setZeroSign(XR);
				incPC(2);
				break;
			// a7
			case 0xA8: // tay
				YR = AC;
				setZeroSign(YR);
				incPC(1);
				clock(1);
				break;
			case 0xA9: // lda imm
				AC = getImm8();
				setZeroSign(AC);
				incPC(2);
				break;
			case 0xAA: // tax
				XR = AC;
				setZeroSign(XR);
				incPC(1);
				clock(1);
				break;
			// ab
			case 0xAC: // ldy abs
				YR = getAbs8();
				setZeroSign(YR);
				incPC(3);
				break;
			case 0xAD: // lda abs
				AC = getAbs8();
				setZeroSign(AC);
				incPC(3);
				break;
			case 0xAE: // ldx abs
				XR = getAbs8();
				setZeroSign(XR);
				incPC(3);
				break;
			// af
			case 0xB0: // bcs
				rBranch(getCarry());
				incPC(2);
				break;
			case 0xB1: // lda ind,y
				AC = getIndY8();
				setZeroSign(AC);
				incPC(2);
				break;
			// b2 b3
			case 0xB4: // ldy zpg,x
				YR = getZpX8();
				setZeroSign(YR);
				incPC(2);
				break;
			case 0xB5: // lda zpg,x
				AC = getZpX8();
				setZeroSign(AC);
				incPC(2);
				break;
			case 0xB6: // ldx zpg,y
				XR = getZpY8();
				setZeroSign(XR);
				incPC(2);
				break;
			// b7
			case 0xB8: // CLV
				setOverflow(false);
				incPC(1);
				clock(1);
				break;
			case 0xB9: // lda abs,y
				AC = getAbsY8();
				setZeroSign(AC);
				incPC(3);
				break;
			case 0xBA: // tsx
				XR = SP;
				setZeroSign(XR);
				incPC(1);
				clock(1);
				break;
			// bb
			case 0xBC: // ldy abs,x
				YR = getAbsX8();
				setZeroSign(YR);
				incPC(3);
				break;
			case 0xBD: // lda abs,x
				AC = getAbsX8();
				setZeroSign(AC);
				incPC(3);
				break;
			case 0xBE: // ldx abs,y
				XR = getAbsY8();
				setZeroSign(XR);
				incPC(3);
				break;
			// bf
			case 0xC0: // cpy imm
				t1 = getImm8();
				setCarry(YR >= t1);
				setZeroSign(YR - t1);
				incPC(2);
				break;
			case 0xC1: // cmp ind,x
				t1 = getIndX8();
				setCarry(AC >= t1);
				setZero(AC - t1);
				setSign(AC - t1);
				//setZeroSign(AC - t1);
				incPC(2);
				//System.out.printf("AC:%02X M:%02X r:%02X zflag: %d SR:%02X\n", AC, t1, AC-t1, getZero()?1:0, SR);
				break;
			// c2 c3
			case 0xC4: // cpy zpg
				t1 = getZp8();
				setCarry(YR >= t1);
				setZeroSign(YR - t1);
				incPC(2);
				break;
			case 0xC5: // cmp zpg
				t1 = getZp8();
				setCarry(AC >= t1);
				setZeroSign(AC - t1);
				incPC(2);
				break;
			case 0xC6: // dec zpg
				setZp8(t1 = getZp8() - 1);
				setZeroSign(t1);
				incPC(2);
				break;
			// c7
			case 0xC8: // iny
				YR++;
				setZeroSign(YR);
				incPC(1);
				clock(1);
				break;
			case 0xC9: // cmp imm
				t1 = getImm8();
				setCarry(AC >= t1);
				setZeroSign(AC - t1);
				incPC(2);
				break;
			case 0xCA: // dex
				XR--;
				setZeroSign(XR);
				clock(1);
				incPC(1);
				break;
			// cb
			case 0xCC: // cpy abs
				t1 = getAbs8();
				setCarry(YR >= t1);
				setZeroSign(YR - t1);
				incPC(3);
				break;
			case 0xCD: // cmp abs
				t1 = getAbs8();
				setCarry(AC >= t1);
				setZeroSign(AC - t1);
				incPC(3);
				break;
			case 0xCE: // dec abs
				setAbs8(t1 = getAbs8() - 1);
				setZeroSign(t1);
				incPC(3);
				break;
			// cf
			case 0xD0: // bne
				rBranch(!getZero());
				incPC(2);
				break;
			case 0xD1: // cmp ind,y
				t1 = getIndY8();
				setCarry(AC >= t1);
				setZeroSign(AC - t1);
				incPC(2);
				break;
			// d2 d3 d4
			case 0xD5: // cmp zpg,x
				t1 = getZpX8();
				setCarry(AC >= t1);
				setZeroSign(AC - t1);
				incPC(2);
				break;
			case 0xD6: // dec zpg,x
				setZpX8(t1 = getZpX8() - 1);
				setZeroSign(t1);
				incPC(2);
				break;
			// d7
			case 0xD8: // cld
				setDecMode(false);
				incPC(1);
				clock(1);
				break;
			case 0xD9: // cmp abs,y
				t1 = getAbsY8();
				setCarry(AC >= t1);
				setZeroSign(AC - t1);
				incPC(3);
				break;
			// da db dc
			case 0xDD: // cmp abs,x
				t1 = getAbsX8();
				setCarry(AC >= t1);
				setZeroSign(AC - t1);
				incPC(3);
				break;
			case 0xDE: // dec abs,x
				setAbsX8(t1 = getAbsX8() - 1);
				setZeroSign(t1);
				incPC(3);
				break;
			// df
			case 0xE0: // cpx imm
				t1 = getImm8();
				setCarry(XR >= t1);
				setZeroSign(XR - t1);
				incPC(2);
				break;
			case 0xE1: // sbc ind,x
				t1 = getIndX8();
				t2 = AC;
				AC = AC - t1 - (getCarry()?0:1);
				setOverflow(((t2 & 0x80) != (t1 & 0x80)) && ((t2 & 0x80) != (AC & 0x80)));
				setCarry(!(AC < 0));
				setZeroSign(AC);
				incPC(2);
				break;
			// e2 e3
			case 0xE4: // cpx zpg
				t1 = getZp8();
				setCarry(XR >= t1);
				setZeroSign(XR - t1);
				incPC(2);
				break;
			case 0xE5: // sbc zpg
				t1 = getZp8();
				t2 = AC;
				AC = AC - t1 - (getCarry()?0:1);
				//System.out.printf("M:%d AC:%d | AC result: %d\n", t1, t2, AC);
				setOverflow(((t2 & 0x80) != (t1 & 0x80)) && ((t2 & 0x80) != (AC & 0x80)));
				setCarry(!(AC < 0));
				//System.out.printf("new carry flag: %d\n", getCarry()?1:0);
				setZeroSign(AC);
				incPC(2);
				break;
			case 0xE6: // inc zpg
				setZp8(t1 = getZp8() + 1);
				setZeroSign(t1);
				incPC(2);
				break;
			// e7
			case 0xE8: // inx
				XR++;
				setZeroSign(XR);
				incPC(1);
				clock(1);
				break;
			case 0xE9: // sbc imm
				t1 = getImm8();
				t2 = AC;
				AC = AC - t1 - (getCarry()?0:1);
				//System.out.printf("M:%d AC:%d | AC result: %d\n", t1, t2, AC);
				//setOverflow((((t2 & 0x80) ^ (t1 & 0x80)) != 0) && (((t2 & 0x80) ^ (AC & 0x80)) != 0));
				//AC &= 0xFF;
				// !!((AC & 0x80) ^ (i & 0x80)) && !!((AC & 0x80) ^ (result & 0x80));
				setOverflow(((t2 & 0x80) != (t1 & 0x80)) && ((t2 & 0x80) != (AC & 0x80)));
				// operand signs mismatch and result sign mismatches operand sign
				setCarry(!(AC < 0));
				//System.out.printf("new carry flag: %d\n", getCarry()?1:0);
				setZeroSign(AC);
				incPC(2);
				break;
			case 0xEA: // nop
				incPC(1);
				clock(2);
				break;
			// eb
			case 0xEC: // cpx abs
				t1 = getAbs8();
				setCarry(XR >= t1);
				setZeroSign(XR - t1);
				incPC(3);
				break;
			case 0xED: // sbc abs
				t1 = getAbs8(); //getMem8((getAbs8() + YR) & 0xFFFF);
				t2 = AC;
				AC = AC - t1 - (getCarry()?0:1);
				setOverflow(((t2 & 0x80) != (t1 & 0x80)) && ((t2 & 0x80) != (AC & 0x80)));
				setCarry(!(AC < 0));
				setZeroSign(AC);
				incPC(3);
				break;
			case 0xEE: // inc abs
				setAbs8(t1 = getAbs8() + 1);
				setZeroSign(t1);
				incPC(3);
				break;
			// ef
			case 0xF0: // beq
				rBranch(getZero());
				incPC(2);
				break;
			case 0xF1: // sbc ind,y
				t1 = getIndY8();
				t2 = AC;
				AC = AC - t1 - (getCarry()?0:1);
				setOverflow(((t2 & 0x80) != (t1 & 0x80)) && ((t2 & 0x80) != (AC & 0x80)));
				setCarry(!(AC < 0));
				setZeroSign(AC);
				incPC(2);
				break;
			// f2 f3 f4
			case 0xF5: // sbc zpg,x
				t1 = getZpX8();
				t2 = AC;
				AC = AC - t1 - (getCarry()?0:1);
				setOverflow(((t2 & 0x80) != (t1 & 0x80)) && ((t2 & 0x80) != (AC & 0x80)));
				setCarry(!(AC < 0));
				setZeroSign(AC);
				incPC(2);
				break;
			case 0xF6: // inc zpg,x
				setZpX8(t1 = getZpX8() + 1);
				setZeroSign(t1);
				incPC(2);
				break;
			// f7
			case 0xF8: // sed
				setDecMode(true);
				incPC(1);
				clock(1);
				break;
			case 0xF9: // sbc abs,y
				t1 = getAbsY8(); //getMem8((getAbs8() + YR) & 0xFFFF);
				t2 = AC;
				AC = AC - t1 - (getCarry()?0:1);
				setOverflow(((t2 & 0x80) != (t1 & 0x80)) && ((t2 & 0x80) != (AC & 0x80)));
				setCarry(!(AC < 0));
				setZeroSign(AC);
				incPC(3);
				break;
			// fa fb fc
			case 0xFD: // sbc abs,x
				t1 = getAbsX8(); //getMem8((getAbs8() + XR) & 0xFFFF);
				t2 = AC;
				AC = AC - t1 - (getCarry()?0:1);
				//System.out.printf("M:%d AC:%d | AC result: %d\n", t1, t2, AC);
				setOverflow(((t2 & 0x80) != (t1 & 0x80)) && ((t2 & 0x80) != (AC & 0x80)));
				setCarry(!(AC < 0));
				//System.out.printf("new carry flag: %d\n", getCarry()?1:0);
				setZeroSign(AC);
				incPC(3);
				break;
			case 0xFE: // inc abs,x
				setAbsX8(t1 = getAbsX8() + 1);
				setZeroSign(t1);
				incPC(3);
				break;
			// ff
			default:
				//System.out.printf("(totalCycles %d)", totalCycles);
				//errorstr = "undefined opcode";
				//System.out.printf("undefined opcode (%02X) @ %04X\n", mem.data[PC]&0xFF, PC);
				incPC(1); // skip undefined opcodes
		}
		fixRegisterBits();
		//totalOps++;
		int t = totalCycles;
		totalCycles = 0;
		
		// CHECK CPU BREAKPOINT ON NEXT INSTRUCTION
		//if(dbgCheckPcWatch(PC)){
		//	dbgPcHit = true;
		//}
		
		// TODO decode next instruction's intention to determine read/write hits
		// switch(dis.opsyn[PC])
		
		//poststep();
		return t;
	}
	
	public void setMemory(Memory mem){
		this.mem = mem;
	}
	
	public CPU6502(Memory mem){
		this.mem = mem; // CPU RAM
		dis = new Disassembler6502(mem);
		//this.PC = getMem16(V_RST);
	}
	
	public int getPC(){return PC;}
	public int getAC(){return AC;}
	public int getXR(){return XR;}
	public int getYR(){return YR;}
	public int getSP(){return SP;}
	public int getSR(){return SR;}
	
	//public int getPC(){return PC;}
	public void setAC(int value){AC = value & 0xFF;}
	public void setXR(int value){XR = value & 0xFF;}
	public void setYR(int value){YR = value & 0xFF;}
	public void setSP(int value){SP = value & 0xFF;}
	public void setSR(int value){SR = value & 0xFF;}
	
	// STEP HOOKS
	//public void poststep(){
	//	return;
	//}
	//public void prestep(){
	//	return;
	//}
	//public void cancelStep(){
	//	// doCancelStep = true;
	//}
	
// DEBUGGING FEATURES
	/*
	public static final int
	DBG_HIT_NONE   = 0b000,
	DBG_HIT_PC     = 0b001,
	DBG_HIT_READ   = 0b010,
	DBG_HIT_WRITE  = 0b100;
	
	private boolean dbgPcHistoryEnabled = false;
	private int[] dbgPcHistory = new int[30]; // last 30 PC positions
	
	private boolean dbgClockDisabled        = false;
	private boolean dbgMemoryEventsDisabled = false;
	
	private boolean dbgPcHit    = false;
	private boolean dbgReadHit  = false;
	private boolean dbgWriteHit = false;
	
	private ArrayList<Integer> dbgPcWatches    = new ArrayList<Integer>();
	private ArrayList<Integer> dbgReadWatches  = new ArrayList<Integer>();
	private ArrayList<Integer> dbgWriteWatches = new ArrayList<Integer>();
	
	private Map<Integer, Integer> dbgPcWatchHits    = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> dbgReadWatchHits  = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> dbgWriteWatchHits = new HashMap<Integer, Integer>();
	
	private void dbgPcHistoryPush(){
		for(int i = 0; i < dbgPcHistory.length-1; i++){
			dbgPcHistory[i] = dbgPcHistory[i+1];
		}
		dbgPcHistory[dbgPcHistory.length-1] = PC;
	}
	
	public boolean dbgIsPcHit(){
		boolean ret = dbgPcHit;
		dbgPcHit = false;
		return ret;
	}
	public boolean dbgIsReadHit(){
		boolean ret = dbgReadHit;
		dbgReadHit = false;
		return ret;
	}
	public boolean dbgIsWriteHit(){
		boolean ret = dbgReadHit;
		dbgReadHit = false;
		return ret;
	}

	private void dbgClearHits(){
		dbgPcHit = false;
	}
	
	public int[] dbgGetPcHistory(){
		return dbgPcHistory;
	}
	
	public void dbgSetPcHistoryEnabled(boolean setting){
		dbgPcHistoryEnabled = setting;
	}
	public void dbgAddPcWatch(int offset){
		dbgPcWatches.add(offset);
	}
	public void dbgAddReadWatch(int offset){
		dbgReadWatches.add(offset);
	}
	public void dbgAddWriteWatch(int offset){
		dbgWriteWatches.add(offset);
	}
	public void dbgRemovePcWatch(int offset){
		dbgPcWatches.remove(dbgPcWatches.indexOf(offset));
	}
	public void dbgRemoveReadWatch(int offset){
		dbgReadWatches.remove(dbgReadWatches.indexOf(offset));
	}
	public void dbgRemoveWriteWatch(int offset){
		dbgWriteWatches.remove(dbgWriteWatches.indexOf(offset));
	}
	public void dbgClearPcWatches(){
		dbgPcWatches.clear();
	}
	public void dbgClearReadWatches(){
		dbgReadWatches.clear();
	}
	public void dbgClearWriteWatches(){
		dbgWriteWatches.clear();
	}
	
	public boolean dbgCheckPcWatch(int offset){
		return (dbgPcWatches.indexOf(offset) != -1);
	}
	public boolean dbgCheckReadWatch(int offset){
		return (dbgReadWatches.indexOf(offset) != -1);
	}
	public boolean dbgCheckWriteWatch(int offset){
		return (dbgWriteWatches.indexOf(offset) != -1);
	}
	
	private int dbgEffectedAddress = 0;
	private int dbgEffect;
	
	private int dbgGetEffect(){
		int op = mem.read(PC, false);
		dbgEffect = Disassembler6502.opactions[op];
		int s = Disassembler6502.opsyns[op];
		switch(dbgEffect){
			case 0: break;
			case 1: // read
			case 2: // write
			case 3: // readstack
			case 4: // writestack

		}
		return 1;
	}
	
	private int dbgGetEffectedAddress(int mode){
		switch(mode){
			case  2: return 1;
			case  3: return 1;
			case  4: return 1;
			case  5: return 1;
			case  6: return 1;
			case  7: return 1;
			case  8: return 1;
			case  9: return 1;
			case 10: return 1;
		}
		return 1;
	}
	
	private int dbgGetEffectedStackAddress(){
		return 1;
	}
	*/
}