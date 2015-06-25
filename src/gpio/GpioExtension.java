package gpio;

import java.io.File;
import java.io.FileInputStream;
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

	static final String LED_PIN = "gpio18";
	static final String analogPinDir = "/proc/";

	static final String pinDir = "/sys/devices/virtual/misc/gpio/pin/";
	static final String modeDir= "/sys/devices/virtual/misc/gpio/mode/";
	
	static final String pwmDir = "/sys/devices/virtual/misc/pwmtimer/";
	static final String pwmEnable = pwmDir + "enable/";
	static final String pwmFreq = pwmDir + "freq/";
	static final String pwmLevel = pwmDir + "level/";
	
	static final String READ = "0";
	static final String WRITE = "1";
	
	static final String[] availableDigiPins = {"gpio0","gpio1","gpio2","gpio3","gpio4","gpio5","gpio6","gpio7",
        "gpio8", "gpio9", "gpio10", "gpio11", "gpio12", "gpio13",
        "gpio14", "gpio15", "gpio16", "gpio17", "gpio18", "gpio19"};
	
	static final String[] availablePWMs = {"gpio5", "gpio6", "gpio3", "gpio9", "gpio10", "gpio11"};
	static final String[] availableNamesPWM = {"pwm5", "pwm6", "pwm3", "pwm9", "pwm10", "pwm11"};
	
	static final HashMap<String, Integer>maxLevels = new HashMap<String,Integer>();
	static final HashMap<String, Integer>maxFrequencies = new HashMap<String,Integer>();
	
	static final String[] availableAnalogs = {"adc0", "adc1", "adc2", "adc3", "adc4", "adc5" };
	static final ArrayList<String> legalAnalogs = new ArrayList<String>();
	static final ArrayList<String> legalDigitals = new ArrayList<String>();
	static final ArrayList<String> legalPWMs = new ArrayList<String>();
	
	static final HashMap<String, HashMap<String, String>> legalModes = new HashMap<String, HashMap<String, String>>();
	static final HashMap<String, String> pinStates = new HashMap<String,String>();
	
	static HashMap<Integer, PortWatcher> portWatcherMap = new HashMap<Integer, PortWatcher>();
	static {
		//Digital Pins
		for (String pinName : availableDigiPins ) {
			HashMap<String, String> modesHere  = new HashMap<String, String>();
			modesHere.put("read", READ);
			modesHere.put("write", WRITE);
			legalModes.put(pinName, modesHere);
			pinStates.put(pinName, READ);
		}
		
		//PWM pins
		for (String pwmName : availablePWMs ) {
			HashMap<String, String> modesHere  = legalModes.get(pwmName);
			if (modesHere != null ) {
				if( pwmName.equalsIgnoreCase("gpio5") || pwmName.equalsIgnoreCase("gpio6") ) {
					modesHere.put("pwm", "2");
				} else {
					modesHere.put("pwm", "1");  //right now, the PWM functionality on 3,9,10,11 not working.
				}
			}
			legalModes.put(pwmName, modesHere);
		}
		maxLevels.put("pwm3",32);
		maxLevels.put("pwm5",255);
		maxLevels.put("pwm6",16);
		maxLevels.put("pwm9",32);
		maxLevels.put("pwm10",32);
		maxLevels.put("pwm11",32);
		
		maxFrequencies.put("pwm3",8192);
		maxFrequencies.put("pwm5",66700);
		maxFrequencies.put("pwm6",66700);
		maxFrequencies.put("pwm9",8192);
		maxFrequencies.put("pwm10",8192);
		maxFrequencies.put("pwm11",8192);
		
		
		for (String pwm : availableNamesPWM) {
			legalPWMs.add(pwm);
		}
		
		for (String digital: availableDigiPins) {
			legalDigitals.add(digital);
		}
		
		for (String analog: availableAnalogs) {
			legalAnalogs.add(analog);
		}
		
		
		
	}
	
	
	//adc0 and adc1 -- 0 to 2 volts, readings with 6 bits --> 0 to 63 is reading.
	//adc2 to adc5 -- 0 to 3.3 volts, readings with 12 bits --> 0 to 4095
	
	
	/*
	 * PWM filesystem map (assuming sysfs is insalled...)
	 * 
/sys/devices/virtual/misc/pwmtimer/freq/pwmX (r/w min_freq to max_freq )
/sys/devices/virtual/misc/pwmtimer/freq_range/pwmX (read only: freq range)
/sys/devices/virtual/misc/pwmtimer/level/pwmX (r/w 0 to max_level )
/sys/devices/virtual/misc/pwmtimer/enable/pwmX (r/w 1: enable, 0:disable)
/sys/devices/virtual/misc/pwmtimer/max_level/pwmX (read only)
	...where X should be one of 5/6/3/9/10/11
	 *
	 *
	 *usage:
	 *
1) set pin mode function to pwm mode
PWM5/6
 echo 2 > /sys/devices/virtual/misc/gpio/mode/gpio5
 echo 2 > /sys/devices/virtual/misc/gpio/mode/gpio6
PWM3/9/10/11
 echo 1 > /sys/devices/virtual/misc/gpio/mode/gpio3
 echo 1 > /sys/devices/virtual/misc/gpio/mode/gpio9
 echo 1 > /sys/devices/virtual/misc/gpio/mode/gpio10
 echo 1 > /sys/devices/virtual/misc/gpio/mode/gpio11
2) disable pwm before changing freq
 echo 0 > /sys/devices/virtual/misc/pwmtimer/enable/pwmX
3) set freq ( min_freq <= freq <= max_freq )
 echo FREQ > /sys/devices/virtual/misc/pwmtimer/freq/pwmX
 
NOTE: you can get freq first: cat /sys/devices/virtual/misc/pwmtimer/freq_range/pwmX
	 */

	
	
	@Override
	public void load(PrimitiveManager pm) throws ExtensionException {
		pm.addPrimitive("led-on", new LedOn() );
		pm.addPrimitive("led-off", new LedOff() );
		
		pm.addPrimitive("set-mode", new SetPinMode() );
		pm.addPrimitive("get-mode", new GetPinMode() );
		
		pm.addPrimitive("digital-read", new DigitalRead() );		
		pm.addPrimitive("digital-write", new DigitalWrite() );
		
		pm.addPrimitive("analog-read", new AnalogRead() );
		
		//if ( checkForSYSFS() ) {  //for now, allow the prim to show up even if SYSFS is not set up. 
			pm.addPrimitive("pwm-set-level", new PWMSet() );
			pm.addPrimitive("pwm-rest", new PWMRest());
		//}
		
		pm.addPrimitive("all-info", new GetAllPinInfo() );		
		pm.addPrimitive("tone", new Tone() );
		
		pm.addPrimitive("watch-port", new WatchPort() );
		pm.addPrimitive("port-change-count", new ReadChangeCount() );
		pm.addPrimitive("reset-port-change-count", new ResetChangeCount() );
		pm.addPrimitive("unwatch-port", new UnwatchPort() );
		pm.addPrimitive("unwatch-all-ports", new UnwatchAllPorts() );
	}
	
	public static class UnwatchAllPorts extends DefaultCommand {
		@Override
		public void perform(Argument[] args, Context ctxt)
				throws ExtensionException {
			
			for (Integer i: portWatcherMap.keySet() ) {
				PortWatcher pw = portWatcherMap.get(i);
				pw.terminate();
			}
			portWatcherMap.clear();
		}	
	}
	
	public static class UnwatchPort extends DefaultCommand {
		@Override
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] { Syntax.NumberType() });
		}
		
		@Override
		public void perform(Argument[] args, Context ctxt)
				throws ExtensionException {
			
			int pinNum = args[0].getIntValue();
			Integer pinID = new Integer(pinNum);
			PortWatcher pw = portWatcherMap.get(pinID);
			if (pw == null) {
				throw new ExtensionException("Port " + pinNum + " is not being watched");
			} else {
				pw.terminate();
				portWatcherMap.remove(pinID);
			}
		}
	}
	
	public static class WatchPort extends DefaultCommand {
		@Override
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] { Syntax.NumberType(), Syntax.NumberType() });
		}
		
		@Override
		public void perform(Argument[] args, Context ctxt)
				throws ExtensionException {
			
			int pinNum = args[0].getIntValue();
			int delay = args[1].getIntValue();
			PortWatcher portWatcher = new PortWatcher(pinDir, pinNum, delay);
			portWatcherMap.put(new Integer(pinNum), portWatcher);
			portWatcher.start();
		}
		
	}
	
	public static class ReadChangeCount extends DefaultReporter {

		@Override
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[] { Syntax.NumberType() }, Syntax.ListType() );
		}
		@Override
		public Object report(Argument[] args, Context ctxt)
				throws ExtensionException {
			Integer port = args[0].getIntValue();
			PortWatcher pw = portWatcherMap.get(port);
			if (pw == null) {
				throw new ExtensionException("Port " + port + " is not being watched");
			} else {
				return pw.getPortData();
			}
		}
		
	}
	
	public static class ResetChangeCount extends DefaultCommand {

		@Override
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] { Syntax.NumberType() });
		}
		
		@Override
		public void perform(Argument[] args, Context ctxt)
				throws ExtensionException {
			
			Integer port = args[0].getIntValue();
			PortWatcher pw = portWatcherMap.get(port);
			if (pw == null) {
				throw new ExtensionException("Port " + port + " is not being watched");
			} else {
				pw.resetCounter();
			} 
		}
		
	}

	
	private static boolean checkForSYSFS() {
		String filePathString = "/sys/devices/virtual/misc/pwmtimer/level";  //= pwmLevel minus last dir separator
		File f = new File(filePathString);
		return (f.exists() && f.isDirectory()); 
	}


	public static class LedOn extends DefaultCommand {
		@Override
		public void perform(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			
			File f = new File( pinDir + LED_PIN );
			try {
				FileOutputStream fos = new FileOutputStream( f );
				//yes, "0" means on, believe it or not(!)
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
			File f = new File( pinDir + LED_PIN );
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
		@Override
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] { Syntax.NumberType(),
					Syntax.StringType() });
		}

		@Override
		public void perform(Argument[] arg, Context arg1)
				throws ExtensionException, LogoException {
			
			int pinNum = arg[0].getIntValue();
			checkForValidDigitalPinNumber( pinNum );
			
			String pin = "gpio" + pinNum;
			String mode = arg[1].getString().toLowerCase();
			if ( legalModes.containsKey(pin) )
			{
				HashMap<String, String> pinsModes = legalModes.get(pin);
				if (pinsModes.containsKey(mode))
				{
					String modeValue = pinsModes.get(mode);
					try
					{
						File f = new File( modeDir + pin );
						FileOutputStream fos = new FileOutputStream( f );
						fos.write(modeValue.getBytes());
						fos.close();
						pinStates.put(pin, modeValue);
					}
					catch (Exception e)
					{
						e.printStackTrace();
						throw new ExtensionException( "An exception occurred in trying to set pin " + pin + " to mode " + mode + "." + e.getMessage());
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
	
	public static class GetPinMode extends DefaultReporter {
		@Override
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[] { Syntax.NumberType(),
					 }, Syntax.NumberType() );
		}
		@Override
		public Object report(Argument[] arg, Context ctxt)
		throws ExtensionException, LogoException {
			int pinNum = arg[0].getIntValue();
			checkForValidDigitalPinNumber( pinNum );
			
			String pin = "gpio" + pinNum;
			Double toreturn = -1.0;
			toreturn = getMode(pin);
			return toreturn;
		}
	}
	
	private static double getMode(String pin) throws ExtensionException {
		Double toreturn = -1.0;
		if ( legalModes.containsKey(pin) )
		{
			try
			{
				String contents = "";
				File f = new File( modeDir + pin );
				FileInputStream fis = new FileInputStream( f );
				byte[] contbytes = new byte[16];
				int j = fis.read(contbytes);
				for (int i = 0; i<j; i++) {
					contents += (char)contbytes[i];
				}
				if (contents.contains(":")) {
					int k = contents.indexOf(":");
					contents = contents.substring(k + 1);
				}
				while ( contents.endsWith("\\n") ) {
					contents = contents.substring(0,contents.length()-1);
				}
				toreturn = Double.valueOf(contents);
				fis.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new ExtensionException( "An exception occurred in trying to read mode of pin" + pin + ":\n" + e.getMessage());
			}
		}	
		else
		{
			throw new ExtensionException("Pin " + pin + " is not defined for this interface to pcDuino.");
		}
		return toreturn;
	}
	
	
	public static class GetAllPinInfo extends DefaultReporter {
		@Override
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[] { }, Syntax.ListType() );
		}
		
		@Override
		public Object report(Argument[] arg, Context ctxt)
		throws ExtensionException, LogoException {
			LogoListBuilder llb = new LogoListBuilder();
			LogoListBuilder littleb = new LogoListBuilder();
			for (String pinName : availableDigiPins ) {
				double mode = getMode(pinName);
				if (mode == 0.0) {
					littleb.add( getDigitalValue(pinName) );
				} else {
					if (mode < 1.5)
						littleb.add("WRITE" );
					else 
						littleb.add("PWM");
				}
			}
			llb.add(littleb.toLogoList());
			LogoListBuilder littleb2 = new LogoListBuilder();
			for (String aPinName : availableAnalogs ) {
				littleb2.add( getAnalogValue(aPinName) );
			}
			llb.add(littleb2.toLogoList());
			return llb.toLogoList();
		}

	}
	
	
	public static class AnalogRead extends DefaultReporter {
		@Override
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[] { Syntax.NumberType(),
					 }, Syntax.NumberType() );
		}
		
		@Override
		public Object report(Argument[] arg, Context ctxt)
		throws ExtensionException, LogoException {
			int pinNum = arg[0].getIntValue();
			checkForValidAnalogPinNumber(pinNum);
			String pinName = "adc" + pinNum;
			Double toreturn = -1.0;
			toreturn = getAnalogValue( pinName );
			return toreturn;
		}
	}
	
	private static double getAnalogValue( String pin ) throws ExtensionException {
		String contents = "";
		Double toreturn = -1.0;
		if ( legalAnalogs.contains(pin) )
		{
			try
			{
				File f = new File( analogPinDir + pin );
				FileInputStream fis = new FileInputStream( f );
				byte[] contbytes = new byte[16];
				int j = fis.read(contbytes);
				for (int i = 0; i<j; i++) {
					contents += (char)contbytes[i];
				}
				if (contents.contains(":")) {
					int k = contents.indexOf(":");
					contents = contents.substring(k + 1);
				}
				while ( contents.endsWith("\\n") ) {
					contents = contents.substring(0,contents.length()-1);
				}
				toreturn = Double.valueOf(contents);
				fis.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new ExtensionException( "An exception occurred in trying to read from analog pin" + pin + ":\n" + e.getMessage());
			}
		}	
		else
		{
			throw new ExtensionException("Analog pin " + pin + " is not defined for this interface to pcDuino.");
		}
		return toreturn;
	}
	
	
	
	public static class DigitalRead extends DefaultReporter {	
		@Override
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[] { Syntax.NumberType(),
					 }, Syntax.NumberType() );
		}
		@Override
		public Object report(Argument[] arg, Context ctxt)
				throws ExtensionException, LogoException {
			int pinNum = arg[0].getIntValue();
			checkForValidDigitalPinNumber( pinNum );
			String pinName = "gpio" + pinNum;
			double toreturn = -1.0;
			toreturn = getDigitalValue( pinName );
			return toreturn;
		}
	}
	
	static double getDigitalValue(String pin) throws ExtensionException {
		Double toreturn = -1.0;
		String contents = "";
		if ( legalDigitals.contains(pin) )
		{
			String mode = pinStates.get(pin);
			if ( mode.equals(READ) )
			{
					try
					{
						File f = new File( pinDir + pin );
						FileInputStream fis = new FileInputStream( f );
						int contint;
						while ((contint = fis.read()) != -1)
						{
							contents += (char)contint;
						}
						toreturn = Double.valueOf(contents);
						fis.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
						throw new ExtensionException( "An exception occurred in trying to read from pin " + pin + ".");
					}
			}
			else
			{
				throw new ExtensionException("Pin " + pin + " is not set to READ mode.");
			}
		}	
		else
		{
			throw new ExtensionException("Pin " + pin + " is not defined for this interface to pcDuino.");
		}
		return toreturn;
	}

	public static void checkForValidDigitalPinNumber( int num ) throws ExtensionException {
		if (num < 0) {throw new ExtensionException("Negative digital pin number, " + num + " is invalid."); }
		if (num > 13) {throw new ExtensionException("Digital pin numbers greater than 13 are not supported. " + num + " is invalid."); }
	}
	
	public static void checkForValidAnalogPinNumber( int num ) throws ExtensionException {
		if (num < 0) {throw new ExtensionException("Negative analog pin number, " + num + " is invalid."); }
		if (num > 5) {throw new ExtensionException("Analog pin numbers greater than 5 are not supported. " + num + " is invalid."); }
	}
	
	public static void checkForValidPWMPinNumber( int num ) throws ExtensionException {
		//"pwm3", "pwm5", "pwm6", "pwm9", "pwm10", "pwm11"
		if (num != 3 && num != 5 && num != 6 && num != 9 && num != 10 && num != 11) {
			throw new ExtensionException("Pin number " + num + " is not valid for PWM operations.");
		}	
	}
	
	public static int getPWMValueForPercent( int num, String pin  ) throws ExtensionException {
		int max = maxLevels.get(pin);
		//if (num > 100) {num = 100;}
		if (num < 0) { num = 0; }
		double percent = ((double)num) / 100.0;
		double value = percent * ((double)max);
		return (int) Math.round(value);
	}
	
	public static void checkForValidPWMFreq( int num, String pin ) throws ExtensionException {
		int max = maxFrequencies.get(pin);
		if (num < 2) {throw new ExtensionException("PWM Frequency must be >= 2. The value " + num + " is invalid."); }
		if (num > max) {throw new ExtensionException("PWM Frequency must be <= " + max + ". The value " + num + " is invalid."); }
	}

	public static class DigitalWrite extends DefaultCommand {

		@Override
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] { Syntax.NumberType(),
					Syntax.StringType() });
		}

		@Override
		public void perform(Argument[] arg, Context arg1)
				throws ExtensionException, LogoException {
			int pinNum = arg[0].getIntValue();
			checkForValidDigitalPinNumber( pinNum );
			String pinName = "gpio" + pinNum;
			String state = arg[1].getString();
			if ( legalDigitals.contains(pinName) )
			{
				String mode = pinStates.get(pinName);
				if ( mode.equals(WRITE) )
				{
					if (state.equalsIgnoreCase("HIGH") || state.equalsIgnoreCase("LOW") )
					{
						try
						{
							File f = new File( pinDir + pinName );
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
							throw new ExtensionException( "An exception occurred in trying to set pin " + pinName + " to mode " + mode + ".");
						}
					}
					else
					{
						throw new ExtensionException("The requested state " + state + " is not available in digital-write.  Use HIGH or LOW.");
					}
				}
				else
				{
					throw new ExtensionException("Pin " + pinName + " is not set to WRITE mode.");
				}
			}	
			else
			{
				throw new ExtensionException("Pin " + pinName + " is not defined for this interface to pcDuino.");
			}
			
		}
	}
	

	
	public static class PWMRest extends DefaultReporter {
		
		@Override
		public Syntax getSyntax() {
			return Syntax.reporterSyntax( new int[] { Syntax.NumberType() }, 
					Syntax.StringType() );
		}
		
		@Override
		public Object report(Argument[] arg, Context ctxt)
				throws ExtensionException, LogoException {

			int pinNum = arg[0].getIntValue();
			checkForValidPWMPinNumber( pinNum );
			
			String gpName = "gpio" + pinNum;
			String pwmName = "pwm" + pinNum;
			
			if (legalPWMs.contains(pwmName)) {
				try {
					String pwmMODE = legalModes.get(gpName).get("pwm");
					File fmode = new File( modeDir + gpName );
					FileOutputStream modefos = new FileOutputStream( fmode );
					modefos.write( pwmMODE.getBytes() );
					modefos.close();

					File fenable = new File( pwmEnable + pwmName );
					FileOutputStream enableFOS = new FileOutputStream( fenable );
					enableFOS.write( "0".getBytes() );
					enableFOS.close();
				} catch (FileNotFoundException fnfe) {
					if ( checkForSYSFS() ) {
						throw new ExtensionException( "File Not Found: " + fnfe.getMessage() );
					} else {
						throw new ExtensionException( "SYSFS does not seem to be set up.  PWM functions cannot work until it is set up.  File Not Found: " + fnfe.getMessage() );
					}
				} catch (IOException ioe ) {
					throw new ExtensionException( "IO Exception: " + ioe.getMessage() );
				}
			}
			return pwmName + "set to rest (disabled state).";
		}
	}


	public static class PWMSet extends DefaultReporter {
		
		@Override
		public Syntax getSyntax() {
			return Syntax.reporterSyntax( new int[] { Syntax.NumberType(), Syntax.NumberType()}, 
					Syntax.StringType() );
		}
		@Override
		public Object report(Argument[] arg, Context ctxt)
				throws ExtensionException, LogoException {

			int pinNum = arg[0].getIntValue();
			checkForValidPWMPinNumber( pinNum );
			String gpName = "gpio" + pinNum;
			String pwmName = "pwm" + pinNum;
			
			int pwmPerent = arg[1].getIntValue();
			int pwmLevelNum = getPWMValueForPercent( pwmPerent, pwmName );
			String pwmLevelValue = "" + pwmLevelNum;

			if (legalPWMs.contains(pwmName)) {
				try {
					String pwmMODE = legalModes.get(gpName).get("pwm");
					File fmode = new File( modeDir + gpName );
					FileOutputStream modefos = new FileOutputStream( fmode );
					modefos.write( pwmMODE.getBytes() );
					modefos.close();
					
					File fenable = new File( pwmEnable + pwmName );
					FileOutputStream enableFOS = new FileOutputStream( fenable );
					enableFOS.write( "0".getBytes() );
					enableFOS.close();
					
					File ffreq = new File( pwmFreq + pwmName );
					FileOutputStream freqfos = new FileOutputStream( ffreq );
					freqfos.write( "195".getBytes() );   //not sure that this is always the best frequency (TODO:
					freqfos.close();

					fenable = new File( pwmEnable + pwmName );
					enableFOS = new FileOutputStream( fenable );
					enableFOS.write( "1".getBytes() );
					enableFOS.close();
					
					File flevel = new File( pwmLevel + pwmName );
					FileOutputStream levelfos = new FileOutputStream( flevel );
					levelfos.write( pwmLevelValue.getBytes() );
					levelfos.close();
					//System.err.println( "level of  " + pwmName + " to " + pwmLevelValue);

				} catch (FileNotFoundException fnfe) {
					if ( checkForSYSFS() ) {
						throw new ExtensionException( "File Not Found: " + fnfe.getMessage() );
					} else {
						throw new ExtensionException( "SYSFS does not seem to be set up.  PWM functions cannot work until it is set up.  File Not Found: " + fnfe.getMessage() );
					}
				} catch (IOException ioe ) {
					throw new ExtensionException( "IO Exception: " + ioe.getMessage() );
				}


				return pwmName + " set to level " + pwmLevelValue ;
			} else {
				return "Some error in matching " + pwmName;
			}

		}
	}

	

	public static class Tone extends DefaultReporter {
		
		@Override
		public Syntax getSyntax() {
			return Syntax.reporterSyntax( new int[] { Syntax.NumberType(), Syntax.NumberType(), Syntax.NumberType() }, 
					Syntax.StringType() );
		}
		@Override
		public Object report(Argument[] arg, Context ctxt)
				throws ExtensionException, LogoException {

			int pinNum = arg[0].getIntValue();
			checkForValidPWMPinNumber( pinNum );
			String gpName = "gpio" + pinNum;
			String pwmName = "pwm" + pinNum;
			
			int pwmFreqNum = arg[1].getIntValue();
			checkForValidPWMFreq( pwmFreqNum, pwmName  );
			String pwmFrequencyValue = "" + pwmFreqNum;
			
			int pwmPercent = arg[2].getIntValue();
			int pwmLevelNum = getPWMValueForPercent( pwmPercent, pwmName );
			String pwmLevelValue = "" + pwmLevelNum;
			
			if (legalPWMs.contains(pwmName)) {
				try {
					String pwmMODE = legalModes.get(gpName).get("pwm");
					File fmode = new File( modeDir + gpName );
					FileOutputStream modefos = new FileOutputStream( fmode );
					modefos.write( pwmMODE.getBytes() );
					modefos.close();
					
					File fenable = new File( pwmEnable + pwmName );
					FileOutputStream enableFOS = new FileOutputStream( fenable );
					enableFOS.write( "0".getBytes() );
					enableFOS.close();
					
					File ffreq = new File( pwmFreq + pwmName );
					FileOutputStream freqfos = new FileOutputStream( ffreq );
					freqfos.write( pwmFrequencyValue.getBytes() );   
					freqfos.close();

					fenable = new File( pwmEnable + pwmName );
					enableFOS = new FileOutputStream( fenable );
					enableFOS.write( "1".getBytes() );
					enableFOS.close();
					
					File flevel = new File( pwmLevel + pwmName );
					FileOutputStream levelfos = new FileOutputStream( flevel );
					levelfos.write( pwmLevelValue.getBytes() );
					levelfos.close();
					//System.err.println( "level of  " + pwmName + " to " + pwmLevelValue);

				} catch (FileNotFoundException fnfe) {
					if ( checkForSYSFS() ) {
						throw new ExtensionException( "File Not Found: " + fnfe.getMessage() );
					} else {
						throw new ExtensionException( "SYSFS does not seem to be set up.  PWM functions cannot work until it is set up.  File Not Found: " + fnfe.getMessage() );
					}
				} catch (IOException ioe ) {
					throw new ExtensionException( "IO Exception: " + ioe.getMessage() );
				}


				return pwmName + " set to frequency " + pwmFrequencyValue ;
			} else {
				return "Some error in matching " + pwmName;
			}

		}
	}

}
