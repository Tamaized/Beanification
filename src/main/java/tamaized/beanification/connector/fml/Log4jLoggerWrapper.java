package tamaized.beanification.connector.fml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tamaized.beanification.BeanContext;
import tamaized.beanification.util.LoggerWrapper;

public class Log4jLoggerWrapper implements LoggerWrapper {

	@SuppressWarnings("LoggerInitializedWithForeignClass")
	private static final Logger LOGGER = LogManager.getLogger(BeanContext.class);

	@Override
	public void info(String text, Object... params) {
		LOGGER.info(text, params);
	}

	@Override
	public void debug(String text, Object... params) {
		LOGGER.debug(text, params);
	}

	@Override
	public void error(String text, Object... params) {
		LOGGER.error(text, params);
	}
}
