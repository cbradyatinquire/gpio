


import java.io.*;

public class Main {
	public static void main( String [] args )  {

String pinpath = "/sys/devices/virtual/misc/gpio/pin/";

String modepath = "/sys/devices/virtual/misc/gpio/mode/";
String whichpin = "gpio8";
try  {

	File f = new File(pinpath + whichpin);
 	File m = new File(modepath + whichpin);
	FileOutputStream mos = new FileOutputStream( m );
	FileOutputStream fos = new FileOutputStream( f );
	mos.write("1".getBytes() );
	mos.close();

	fos.write("1".getBytes() );
	fos.close();

}
catch (Exception e) { e.printStackTrace(); }


		System.out.println("Hi");

	}



}