package dev.jbang.cli;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.io.FileMatchers.aReadableFile;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.jbang.BaseTest;
import dev.jbang.util.Util;

public class TestInit extends BaseTest {

	@Test
	void testInit(@TempDir Path outputDir) throws IOException {
		Path out = outputDir.resolve("test.java");
		HashMap<String, Object> props = new HashMap<>();
		props.put("baseName", "test");
		new Init().renderQuteTemplate(out, "init-hello.java.qute", props);
		assertThat(Util.readString(out), Matchers.containsString("class test"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "bad.name.java", "Bad-Name.java" })
	void testInvalidInit(String filename) {
		Exception ex = assertThrows(ExitException.class, () -> {
			new Init().renderQuteTemplate(Paths.get(filename), "init-hello.java.qute");
		});
		assertThat(ex.getMessage(), Matchers.containsString("is not a valid class name in java."));
	}

	@Test
	void testCli(@TempDir Path outputDir) throws IOException {
		Path x = outputDir.resolve("edit.java");
		String s = x.toString();
		int result = JBang.getCommandLine().execute("init", "--verbose", "--template=cli", s);
		assertThat(result, is(0));
		assertThat(new File(s).exists(), is(true));
		MatcherAssert.assertThat(Util.readString(x), Matchers.containsString("picocli"));
	}

	@Test
	void testOldInit(@TempDir Path outputDir) throws IOException {
		Path x = outputDir.resolve("nonexist.java");
		String s = x.toString();
		assertEquals(JBang.getCommandLine().execute("--init", s), 2);
	}

	@Test
	void testMissingTemplate(@TempDir Path outputDir) throws IOException {
		Path x = outputDir.resolve("edit.java");
		String s = x.toString();
		int result = JBang.getCommandLine().execute("init", "--template=bogus", s);
		assertThat(result, not(0));
		assertThat(new File(s).exists(), is(false));
	}

	@Test
	void testDefaultInit(@TempDir Path outputDir) throws IOException {
		Path x = outputDir.resolve("edit.java");
		String s = x.toString();
		int result = JBang.getCommandLine().execute("init", s);
		assertThat(result, is(0));
		assertThat(new File(s).exists(), is(true));
		assertThat(Util.readString(x), Matchers.containsString("class edit"));
	}

	@Test
	void testInitExtensionlessKebab(@TempDir Path outputDir) throws IOException {
		Path x = outputDir.resolve("xyz-plug");
		String s = x.toString();
		int result = JBang.getCommandLine().execute("init", s);
		assertThat(result, is(0));
		assertThat(new File(s).exists(), is(true));
		assertThat(Util.readString(x), Matchers.containsString("class XyzPlug"));
	}

	@Test
	void testInitExtensionless(@TempDir Path outputDir) throws IOException {
		Path x = outputDir.resolve("xyzplug");
		String s = x.toString();
		int result = JBang.getCommandLine().execute("init", s);
		assertThat(result, is(0));
		assertThat(new File(s).exists(), is(true));
		assertThat(Util.readString(x), Matchers.containsString("class xyzplug"));
	}

	@Test
	void testCatalog() throws IOException {
//		int result = JBang.getCommandLine().execute("run", "jget@quintesse");
//		int result = JBang.getCommandLine().execute("catalog", "update");
		int result = JBang.getCommandLine().execute("catalog", "list", "quintesse");
		assertThat(result, is(0));
	}

	@Test
	void testInitMultipleFilesWithExt() throws IOException {
		testInitMultipleFiles("{filename}", "edit.java", "edit.java", false);
	}

	@Test
	void testInitMultipleFilesNoExt() throws IOException {
		testInitMultipleFiles("{filename}", "edit", "edit", false);
	}

	@Test
	void testInitMultipleFilesImplicitExt() throws IOException {
		testInitMultipleFiles("{basename}.java", "edit", "edit.java", false);
	}

	@Test
	void testInitMultipleFilesOutSub() throws IOException {
		testInitMultipleFiles("{basename}.java", "sub/edit", "edit.java", false);
	}

	@Test
	void testInitMultipleFilesAbsPaths() throws IOException {
		testInitMultipleFiles("{filename}", "edit.java", "edit.java", true);
	}

	@Test
	void testInitMultipleFilesWrongName() throws IOException {
		testFailMultipleFiles("{filename}", "edit.md", "edit.java", true, 2);
	}

	void testInitMultipleFiles(String targetName, String initName, String outName, boolean abs) throws IOException {
		Path outFile = setupInitMultipleFiles(targetName, initName, abs);
		int result = JBang.getCommandLine().execute("init", "-t=name", outFile.toString());
		assertThat(result, is(0));
		assertThat(outFile.resolveSibling(outName).toFile(), aReadableFile());
		Path f2 = outFile.resolveSibling("file2.java");
		assertThat(f2.toFile(), aReadableFile());
		String baseName = Util.getBaseName(Paths.get(initName).getFileName().toString());
		assertThat(Util.readString(f2), Matchers.containsString("// " + baseName + " with " + outFile));
		assertThat(outFile.resolveSibling("file3.md").toFile(), aReadableFile());

	}

	void testFailMultipleFiles(String targetName, String initName, String outName, boolean abs, int expectedResult)
			throws IOException {
		Path outFile = setupInitMultipleFiles(targetName, initName, abs);
		int result = JBang.getCommandLine().execute("init", "-t=name", outFile.toString());
		assertThat(result, is(expectedResult));
	}

	Path setupInitMultipleFiles(String targetName, String initName, boolean abs) throws IOException {
		Path cwd = Util.getCwd();
		Path tplDir = Files.createDirectory(cwd.resolve("tpl"));
		Path f1 = Files.createFile(tplDir.resolve("file1.java"));
		Path f2 = Files.createFile(tplDir.resolve("file2.java.qute"));
		Util.writeString(f2, "// {baseName} with {scriptref}");
		Path f3 = Files.createFile(tplDir.resolve("file3.md"));
		if (abs) {
			int addResult = JBang	.getCommandLine()
									.execute("template", "add", "-f", cwd.toString(), "--name=name",
											targetName + "=" + f1.toString(), f2.toString(), f3.toString());
			assertThat(addResult, is(0));
		} else {
			int addResult = JBang	.getCommandLine()
									.execute("template", "add", "-f", cwd.toString(), "--name=name",
											targetName + "=tpl/file1.java", "tpl/file2.java.qute", "tpl/file3.md");
			assertThat(addResult, is(0));
		}
		Path appDir = Files.createDirectory(cwd.resolve("app"));
		Util.setCwd(appDir);
		return appDir.resolve(initName);
	}

	@Test
	void testProperties() throws IOException {
		Path cwd = Util.getCwd();
		Path f1 = Files.write(cwd.resolve("file1.java.qute"), "{prop1}{prop2}".getBytes());

		Path out = cwd.resolve("result.java");
		Map<String, Object> m = new HashMap<>();
		m.put("prop1", "propvalue");
		m.put("prop2", "rocks");

		new Init().renderQuteTemplate(out, cwd.resolve("file1.java.qute").toFile().getAbsolutePath(), m);

		String outcontent = Util.readString(out);

		assertThat(outcontent, containsString("propvaluerocks"));
	}

	@Test
	void testInitProperties() throws IOException {
		Path cwd = Util.getCwd();
		Path f1 = Files.write(cwd.resolve("file1.java.qute"), "{prop1}{prop2}".getBytes());
		Path out = cwd.resolve("result.java");

		int addResult = JBang	.getCommandLine()
								.execute("template", "add", "-f", cwd.toString(), "--name=name",
										"{filename}" + "=" + f1.toAbsolutePath().toString());

		assertThat(out.toFile().exists(), not(true));

		int result = JBang	.getCommandLine()
							.execute("init", "--verbose", "--template=name", "-Dprop1=propvalue", "-Dprop2=rocks",
									out.toAbsolutePath().toString());

		assertThat(result, is(0));
		assertThat(out.toFile().exists(), is(true));

		String outcontent = Util.readString(out);

		assertThat(outcontent, containsString("propvaluerocks"));
	}
}
