package tamaized.beanification.util;

public interface LoggerWrapper {

	void info(String text, Object... params);

	void debug(String text, Object... params);

	void error(String text, Object... params);

}
