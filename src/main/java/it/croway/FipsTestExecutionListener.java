package it.croway;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Provider;
import java.security.Security;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class FipsTestExecutionListener implements TestExecutionListener {

	private static final Logger LOG = LoggerFactory.getLogger(FipsTestExecutionListener.class);

	private Path outputFile;

	private Set<String> logs;

	private static final List<String> allowedProviders = List.of(
			"SunPKCS11",
			"SUN",
			"SunEC",
			"SunJSSE",
			"SunJCE",
			"SunRsaSign",
			"XMLDSig"
	);

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		LOG.info("test plan {} starting", testPlan);

		logs = new ConcurrentSkipListSet<>();
		try {
			outputFile = Files.createTempFile("","-fips-test-execution-listener");
		} catch (IOException e) {
			LOG.error("temporary file cannot be created", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		LOG.info("flushing logs to file system {}", testPlan);
		try {
			Files.write(outputFile, logs);
		} catch (IOException e) {
			LOG.error("temporary file cannot be written", e);
			throw new RuntimeException(e);
		}

		LOG.info("File " + outputFile.toString() + " flushed");
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		LOG.debug("Executing test {}", testIdentifier.getDisplayName());

		logNotAllowedProviders(testIdentifier);
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		LOG.debug("Finished executing test {}", testIdentifier.getDisplayName());

		logNotAllowedProviders(testIdentifier);
	}

	private void logNotAllowedProviders(TestIdentifier testIdentifier) {
		for (Provider provider : Security.getProviders()) {
			if (!allowedProviders.contains(provider.getName())) {
				String log = String.format("Not allowed provider %s found in test %s",
						provider.getName() + " - " + provider.getInfo(), testIdentifier.getUniqueId());
				LOG.info(log);
				logs.add(log);
			}
		}
	}
}

