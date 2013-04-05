package gpio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultClassManager;
import org.nlogo.api.DefaultCommand;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.PrimitiveManager;
import org.nlogo.api.Syntax;

public class GpioExtension extends DefaultClassManager {

	static final String pinDir = "/sys/devices/virtual/misc/gpio/pin/";
	static final String modeDir= "/sys/devices/virtual/misc/gpio/mode/";
	
	
	static final String UNINITIALIZED = "uninitialized";
	static final String WRITE = "output";
	static final String READ = "input";
	static final ArrayList<String> legalModes = new ArrayList<String>();
	static {
		legalModes.add(UNINITIALIZED);
		legalModes.add(WRITE);
		legalModes.add(READ);
	}
	
	
	static final String[] availablePins = {"gpio0","gpio1","gpio2","gpio3","gpio4","gpio5","gpio6","gpio7",
            "gpio8", "gpio9", "gpio10", "gpio11", "gpio12", "gpio13",
            "gpio14", "gpio15", "gpio16", "gpio17", "gpio18", "gpio19"};
	static final ArrayList<String> pinList = new ArrayList<String>();
	static final HashMap<String, String> pinStates = new HashMap<String, String>();
	static {	
		for (String p : availablePins)
		{
			pinList.add( p );
			pinStates.put(p, UNINITIALIZED);
		}	
	}
	
	
	@Override
	public void load(PrimitiveManager pm) throws ExtensionException {
		pm.addPrimitive("test", new TestPrimitive()  );
		pm.addPrimitive("led-on", new LedOn() );
		pm.addPrimitive("led-off", new LedOff() );
		pm.addPrimitive("set-pin-mode", new SetPinMode() );
		pm.addPrimitive("digital-write", new DigitalWrite() );
//		pm.addPrimitive("digital-read", new DigitalRead() );
		pm.addPrimitive("get-pin-info", new GetPinInfo() );
	}

	public static class GetPinInfo extends DefaultReporter {
		static final LogoListBuilder lb = new LogoListBuilder();
		static {
			for (String p : pinList)
			{
				lb.add( p );
			}
		}
		@Override
		public Object report(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			return lb.toLogoList();
		}
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
			
			File f = new File( pinDir + "gpio18" );

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
			File f = new File( pinDir + "gpio18" );
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
	
	public static class SetPinMode extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] { Syntax.StringType(),
					Syntax.StringType() });
		}

		@Override
		public void perform(Argument[] arg, Context arg1)
				throws ExtensionException, LogoException {
			String pin = arg[0].getString();
			String mode = arg[1].getString();
			if ( pinList.contains(pin) )
			{
				if (legalModes.contains(mode))
				{
					try
					{
					File f = new File( modeDir + pin );
					FileOutputStream fos = new FileOutputStream( f );
					if (mode.equalsIgnoreCase(WRITE))
						fos.write( "1".getBytes() );
					else
						fos.write("0".getBytes() );
					fos.close();
					pinStates.put(pin, mode);
					}
					catch (Exception e)
					{
						e.printStackTrace();
						throw new ExtensionException( "An exception occurred in trying to set pin " + pin + " to mode " + mode + ".");
					}
				}
				else
				{
					throw new ExtensionException("The requested mode " + mode + " is not defined for this interface to pcDuino.");
				}
			}
			else
			{
				throw new ExtensionException("Pin " + pin + " is not defined for this interface to pcDuino.");
			}
			
		}
	}
	
	
	
	
	public static class DigitalWrite extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] { Syntax.StringType(),
					Syntax.StringType() });
		}

		@Override
		public void perform(Argument[] arg, Context arg1)
				throws ExtensionException, LogoException {
			String pin = arg[0].getString();
			String state = arg[1].getString();
			if ( pinList.contains(pin) )
			{
				String mode = pinStates.get(pin);
				if ( mode.equals(WRITE) )
				{
					if (state.equalsIgnoreCase("HIGH") || state.equalsIgnoreCase("LOW") )
					{
						try
						{
							File f = new File( pinDir + pin );
							FileOutputStream fos = new FileOutputStream( f );
							if (state.equalsIgnoreCase("HIGH"))
								fos.write( "1".getBytes() );
							else
								fos.write("0".getBytes() );
							fos.close();
						}
						catch (Exception e)
						{
							e.printStackTrace();
							throw new ExtensionException( "An exception occurred in trying to set pin " + pin + " to mode " + mode + ".");
						}
					}
					else
					{
						throw new ExtensionException("The requested state " + state + " is not available in digital-write.  Use HIGH or LOW.");
					}
				}
				else
				{
					throw new ExtensionException("Pin " + pin + " is not set to WRITE mode.");
				}
			}	
			else
			{
				throw new ExtensionException("Pin " + pin + " is not defined for this interface to pcDuino.");
			}
			
		}
	}
	
	
	
}
