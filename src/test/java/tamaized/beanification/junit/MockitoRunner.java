package tamaized.beanification.junit;


import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockitoAnnotations;

import javax.annotation.Nullable;

public class MockitoRunner implements BeforeEachCallback, AfterEachCallback {

	private @Nullable AutoCloseable mocks;

	@Override
	public void beforeEach(ExtensionContext context) {
		mocks = context.getTestInstance().map(MockitoAnnotations::openMocks).orElse(null);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		if (mocks != null)
			mocks.close();
	}
}
