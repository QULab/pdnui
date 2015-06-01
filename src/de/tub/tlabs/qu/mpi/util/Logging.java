package de.tub.tlabs.qu.mpi.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Logging {

	private static final Formatter FORMATTER = new Formatter() {
		@Override
		public String format(LogRecord r) {
			StringBuilder sb = new StringBuilder();
			sb
			.append("t:").append(r.getMillis()).append(" ")
			.append("lvl:").append(r.getLevel()).append(" ")
			.append("l:").append(r.getLoggerName()).append(" ")
			.append("msg:").append(r.getMessage())
			.append("\n");
			return sb.toString();
		}
	};
	
	public static void init(String fname) {
		try {
			Logger global = Logger.getGlobal();
			global.setUseParentHandlers(false);
			global.setLevel(Level.FINEST);
			FileHandler fileHandler = new FileHandler(fname);
			fileHandler.setFormatter(FORMATTER);
			global.addHandler(fileHandler);				
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}