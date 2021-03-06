package gpio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;

import org.nlogo.api.LogoList;
import org.nlogo.api.LogoListBuilder;

public class PortWatcher extends Thread {

	int countChanges = 0;
	int lastValue = 0;
	String pinString;
	Path pinPath;
	File f; 
	
	long startTime;
	boolean keepgoing = true;
	int delayTime = 0;
	
	
	public void terminate() { keepgoing = false; }
	
	//currently unused, replaced by the delay-specifing constructor
	public PortWatcher ( String pinDir, int pinNumber ) {
		pinString = "gpio" + pinNumber;
		f = new File( pinDir + pinString );
	}
	
	public PortWatcher ( String pinDir, int pinNumber, int dlay ) {
		pinString = "gpio" + pinNumber;
		if (dlay > 0) { delayTime = dlay; }
		f = new File( pinDir + pinString );
	}
	
	public void resetCounter() {
		countChanges = 0;
		startTime = System.currentTimeMillis();
	}
	
	public LogoList getPortData() {
		LogoListBuilder llb = new LogoListBuilder();
		long endTime = System.currentTimeMillis();
		long diff = endTime - startTime;
		double secs = ((double)diff) / 1000.0;
		
		llb.add( new Double( secs ) );
		llb.add( new Double( countChanges ) );
		return llb.toLogoList();
	}
	
	@Override
	public void run() {
		startTime = System.currentTimeMillis();
		while (keepgoing) {
			try {
			if ( GpioExtension.pinStates.get(pinString).equals(GpioExtension.READ) )
			{
				FileInputStream fis = new FileInputStream( f );
				int currentvalue = fis.read();
				fis.close();

				if (currentvalue != lastValue) {
					countChanges++;
					lastValue = currentvalue;
				}
			} 
			}catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			}catch (IOException ioe) {
				ioe.printStackTrace();
			}
			
			if (delayTime > 0) {
				try {
					Thread.sleep(delayTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
