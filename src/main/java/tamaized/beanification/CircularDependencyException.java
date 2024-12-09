package tamaized.beanification;

import java.io.Serial;

public class CircularDependencyException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 5485181528021698444L;

	public CircularDependencyException(String message) {
		super(message);
	}

}
