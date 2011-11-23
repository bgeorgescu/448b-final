package vis.data.util;

import java.lang.Thread.UncaughtExceptionHandler;

public class ExceptionHandler {
	public static void terminateOnUncaught() {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				System.err.println("uncaught exception");
				e.printStackTrace(System.err);
				Runtime.getRuntime().exit(1);
			}
		});
	}
}
