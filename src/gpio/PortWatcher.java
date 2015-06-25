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
	
	
	
	public PortWatcher ( String pinDir, int pinNumber ) {
		pinString = "gpio" + pinNumber;
		f = new File( pinDir + pinString );
		//pinPath = FileSystems.getDefault().getPath(pinDir, pinString);
		//f = pinPath.toFile();
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
		while (true) {
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
			
			/*try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
		}
	}

}
