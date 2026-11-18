package tamaized.beanification.util;

import java.text.MessageFormat;

public class SysoutLoggerWrapper implements LoggerWrapper {

	@Override
	public void info(String text, Object... params) {
		System.out.println(MessageFormat.format(text, params));
	}

	@Override
	public void debug(String text, Object... params) {
		info(text, params);
	}

	@Override
	public void error(String text, Object... params) {
		System.err.println(MessageFormat.format(text, params));
	}
}
