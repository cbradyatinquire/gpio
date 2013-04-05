package gpio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultClassManager;
import org.nlogo.api.DefaultCommand;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.PrimitiveManager;

public class GpioExtension extends DefaultClassManager {

	static final String pins = "/sys/devices/virtual/misc/gpio/pin/";
	static final String modes = "/sys/devices/virtual/misc/gpio/mode/";
	
	
	@Override
	public void load(PrimitiveManager pm) throws ExtensionException {
		pm.addPrimitive("test", new TestPrimitive()  );
		pm.addPrimitive("led-on", new LedOn() );
		pm.addPrimitive("led-off", new LedOff() );
	}

	
	public static class TestPrimitive extends DefaultReporter {

		@Override
		public Object report(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			return "Hello";
		}
		
	}
	
	public static class LedOn extends DefaultCommand {

		@Override
		public void perform(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			
			File f = new File( pins + "gpio18" );

			try {
				FileOutputStream fos = new FileOutputStream( f );

				fos.write( "0".getBytes() );
				fos.close();
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			} catch (IOException ioe ) {
				ioe.printStackTrace();
			}

		}
		
	}
	
	
	public static class LedOff extends DefaultCommand {

		@Override
		public void perform(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			File f = new File( pins + "gpio18" );
			try {
				FileOutputStream fos = new FileOutputStream( f );
				fos.write( "1".getBytes() );
				fos.close();
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			} catch (IOException ioe ) {
				ioe.printStackTrace();
			}

		}
	   	
	}
}
