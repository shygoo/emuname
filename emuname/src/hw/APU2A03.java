package net.shygoo.hw;

import java.nio.ByteBuffer;
import javax.sound.sampled.*;

public class APU2A03 {

	private Thread audioThread;

	private static final int CLOCK_SPEED = 1789773;
	
	private static final int SAMPLING_RATE = 15745; //157458;            // Audio sampling rate
	private static final int SAMPLE_SIZE = 1;                  // Audio sample size in bytes
	
	//private static final int SAMPLE_PEAK = 0x7F;
	
	//private SourceDataLine line;

	private double masterVolume = 0.0;
	
	private byte            pulse1Vol  = 0x7F;
	private double          pulse1Freq = 440;
	private SourceDataLine pulse1Line;
	private ByteBuffer     pulse1Buf; // pcm buffer
	
	private byte            pulse2Vol  = 0x7F;
	private double          pulse2Freq = 220;
	private SourceDataLine pulse2Line;
	private ByteBuffer     pulse2Buf; // pcm buffer

	private byte            triVol  = 0x7F;
	private double          triFreq = 110;
	private SourceDataLine triLine;
	private ByteBuffer     triBuf; // pcm buffer
	
	public byte noiseVol = 0x7F;
	private SourceDataLine noiseLine;
	private double noiseFreq = 440;
	private ByteBuffer noiseBuf;
	
	private boolean running = false;
	
	public APU2A03()  { // throws InterruptedException, LineUnavailableException
		
		AudioFormat format = new AudioFormat(SAMPLING_RATE, 8, 1, true, true);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		
		//if(!AudioSystem.isLineSupported(info)){
		//	System.out.println("Line matching " + info + " is not supported.");
		//	throw new LineUnavailableException();
		//}
		
		int bufferSize = 2200;
		
		try {
			pulse1Line = (SourceDataLine)AudioSystem.getLine(info);
			pulse1Buf = ByteBuffer.allocate(bufferSize);  
			
			pulse1Line.open(format, bufferSize);
			pulse1Line.start();
			
			pulse2Line = (SourceDataLine)AudioSystem.getLine(info);
			pulse2Buf = ByteBuffer.allocate(bufferSize);  
			
			pulse2Line.open(format, bufferSize);
			pulse2Line.start();
			
			triLine = (SourceDataLine)AudioSystem.getLine(info);
			triBuf = ByteBuffer.allocate(bufferSize);  
			
			triLine.open(format, bufferSize);
			triLine.start();
			
			noiseLine = (SourceDataLine)AudioSystem.getLine(info);
			noiseBuf = ByteBuffer.allocate(bufferSize); 
			
			noiseLine.open(format, bufferSize);
			noiseLine.start();
			
		} catch(Exception e){
			System.out.println("error occurred while opening audio lines");
		}
		
		setPulse1Vol(0);
		setPulse2Vol(0);
		setTriVol(0);
		
	}
	

	private class audioThread extends Thread {
		public void run(){
			double pulse1CyclePos = 0;
			double pulse2CyclePos = 0;
			double triCyclePos = 0;
			
			double noiseCyclePos = 0;
			
			while(running){
				
				double pulse1CycleInc = pulse1Freq / SAMPLING_RATE;  // Fraction of cycle between samples
				double pulse2CycleInc = pulse2Freq / SAMPLING_RATE;  // Fraction of cycle between samples
				double triCycleInc = triFreq / SAMPLING_RATE;  // Fraction of cycle between samples
				double noiseCycleInc = noiseFreq / SAMPLING_RATE;
				
				// Discard samples from previous pass
				pulse1Buf.clear();
				pulse2Buf.clear();
				triBuf.clear();
				noiseBuf.clear();
				
				// Figure out how many samples we can add
				int p1SamplesThisPass = pulse1Line.available()/SAMPLE_SIZE;   
				int p2SamplesThisPass = pulse2Line.available()/SAMPLE_SIZE;   
				int tSamplesThisPass = triLine.available()/SAMPLE_SIZE;   
				int n1SamplesThisPass = noiseLine.available()/SAMPLE_SIZE;
				
				for (int i = 0; i < p1SamplesThisPass; i++) {
					//double v = (pulse1CyclePos < 0.5) ? 1.0F : -1.0F;
					double v = Math.sin(2*Math.PI * pulse1CyclePos);
					v = v < 0 ? -1 : 1;
					//byte v = (pulse1CyclePos < 0.5) ? (byte)-128 : 127;
					pulse1Buf.put((byte)(masterVolume * v * pulse1Vol)); //
					//System.out.println((byte)(v * 0x50));
					pulse1CyclePos += pulse1CycleInc;
					if (pulse1CyclePos > 1) pulse1CyclePos -= 1;
				}
	
				for (int i = 0; i < p2SamplesThisPass; i++) {
					//double v = (pulse2CyclePos < 0.5F) ? 1.0F : -1.0F;
					double v = Math.sin(2*Math.PI * pulse2CyclePos);
					v = v < 0 ? -1 : 1;
					//byte v = (pulse1CyclePos < 0.5) ? (byte)-128 : 127;
					pulse2Buf.put((byte)(masterVolume * v * pulse2Vol));
					pulse2CyclePos += pulse2CycleInc;
					if (pulse2CyclePos > 1) pulse2CyclePos -= 1;
				}
				
				for (int i = 0; i < tSamplesThisPass; i++) {
					double v = Math.sin(2*Math.PI * triCyclePos);
					// squarized sine wave
					//v = ((v < 0 ? -1 : 1) + v*2) / 3;
					triBuf.put((byte)(masterVolume * triVol * v));
					triCyclePos += triCycleInc;
					if (triCyclePos > 1) triCyclePos -= 1;
				}
				
				for(int i = 0; i < n1SamplesThisPass; i++){
					//double v = Math.sin(2*Math.PI * noiseCyclePos);
					noiseBuf.put((byte)(masterVolume * noiseVol * (Math.random()*2-1)));
					noiseCyclePos += noiseCycleInc;
					if(noiseCyclePos > 1) noiseCyclePos -= 1;
				}
				
				//Write sine samples to the line buffer.  If the audio buffer is full, this will 
				// block until there is room (we never write more samples than buffer will hold)
				pulse1Line.write(pulse1Buf.array(), 0, pulse1Buf.position());
				pulse2Line.write(pulse2Buf.array(), 0, pulse2Buf.position());
				triLine.write(triBuf.array(), 0, triBuf.position());
				
				noiseLine.write(noiseBuf.array(), 0, noiseBuf.position());
				
				//Wait until the buffer is at least half empty  before we add more
				while (pulse1Line.getBufferSize()/2 < pulse1Line.available());
				while (pulse2Line.getBufferSize()/2 < pulse2Line.available());
				while (triLine.getBufferSize()/2 < triLine.available());
				while (noiseLine.getBufferSize()/2 < noiseLine.available());
				try { 
					sleep(1);	
				} catch(Exception e){
					
				}
			}
		}
	}
	
	public void start(){
		stop();
		running = true;
		audioThread = new audioThread();
		audioThread.start();
	}
	
	public void stop(){
		running = false;
		if(audioThread != null){
			while(audioThread.getState() != Thread.State.TERMINATED);
		}
	}
	
	public boolean isRunning(){
		return running;
	}
	
   	public void setPulse1Freq(double hz){
		pulse1Freq = hz;
		//System.out.println(hz);
		if(hz > 18000){
			setPulse1Vol(0);
			return;
		} else {
			//setPulse1Vol(1);
		}
	}
	
   	public void setPulse2Freq(double hz){
		pulse2Freq = hz;
		//System.out.println(hz);
		if(hz > 18000){
			setPulse2Vol(0);
			return;
		} else {
			//setPulse2Vol(1);
		}
	}
	
   	public void setTriFreq(double hz){
		triFreq = hz;
		//System.out.println(hz);
		if(hz > 18000){
			setTriVol(0);
			return;
		} else {
			setTriVol(1);
		}
	}
	
	public void setPulse1Vol(double volume){ // 0.00 - 1.00
		pulse1Vol = (byte)(volume * 0x7F);
	}
	
	public void setPulse2Vol(double volume){ // 0.00 - 1.00
		pulse2Vol = (byte)(volume * 0x7F);
	}
	
	public void setTriVol(double volume){
		triVol = (byte)(volume * 0x7F);
	}
	
	public void setNoiseVol(double volume){
		noiseVol = (byte)(volume * 0x7F);
	}
	/*public static void main(String[] args) throws Exception {
		FixedFreqSine f = new FixedFreqSine();
		Thread test = new Thread(){
			public void run(){
				f.start();
			}
		};
		test.start();
		f.setVelocity(2);
		Thread.sleep(1000);
		f.setFreq(110);
		
	}*/
	public void setMasterVolume(double volume){
		masterVolume = volume;
	}
}