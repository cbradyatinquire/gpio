package gpio;

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultClassManager;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.PrimitiveManager;

public class GpioExtension extends DefaultClassManager {

	@Override
	public void load(PrimitiveManager pm) throws ExtensionException {
		pm.addPrimitive("test", new TestPrimitive()  );
	}

	
	public static class TestPrimitive extends DefaultReporter {

		@Override
		public Object report(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			return "Hello";
		}
		
	}
}
