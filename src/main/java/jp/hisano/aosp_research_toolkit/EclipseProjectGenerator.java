package jp.hisano.aosp_research_toolkit;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.zeroturnaround.zip.ZipUtil;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public final class EclipseProjectGenerator {
	public static void main(String... args) {
		if (args.length == 0) {
			System.err.println("No AOSP root directory");
			System.exit(1);
		}
		createProjects(args);
	}

	private static void createProjects(String... rootDirectoryPaths) {
		for (String rootDirectoryPath: rootDirectoryPaths) {
			createProject(rootDirectoryPath);
		}
	}

	private static void createProject(String rootDirectoryPath) {
		File rootDirectory = new File(rootDirectoryPath);

		try {
			createEmptyProject(rootDirectory);
		} catch (IOException e) {
			System.err.println("Generating Eclipse project failed");
			e.printStackTrace();
			System.exit(1);
		}

		createJar(rootDirectory, "out/target/common/obj/JAVA_LIBRARIES", "frameworks.jar", Lists.newArrayList("*support*", "cts*"));
		createJar(rootDirectory, "out/target/common/obj/APPS", "apps.jar", Lists.newArrayList("*Tests_*", "*tests_*", "Cts*"));

		createSourcesJar(rootDirectory, ".");

		createZip(rootDirectory);
	}

	private static void createZip(File rootDirectory) {
		System.out.println("Generate zip");
		File projectDirectory = getProjectDirectory(rootDirectory);
		ZipUtil.pack(projectDirectory, new File(projectDirectory.getParentFile(), rootDirectory.getName() + ".zip"));
	}

	private static void createEmptyProject(File rootDirectory) throws IOException {
		System.out.println("Generate Eclipse project");
		File projectDirectory = getProjectDirectory(rootDirectory);

		FileUtils.deleteQuietly(projectDirectory);

		FileUtils.copyDirectory(new File("template"), projectDirectory);

		File projectFile = new File(projectDirectory, ".project");
		FileUtils.write(projectFile, FileUtils.readFileToString(projectFile).replace("$PROJECT_NAME", rootDirectory.getName()));
	}

	private static File getProjectDirectory(File rootDirectory) {
		return new File("target/" + rootDirectory.getName());
	}

	private static void createSourcesJar(File rootDirectory, String sourcePath) {
		File workingDirectory = prepareWorkingDirectory();

		Pattern PACKAGE_PATTERN = Pattern.compile(".*package +([\\w\\.]+);.*");
		System.out.println("Searching java files");
		FileUtils.listFiles(new File(rootDirectory, sourcePath), new String[] {"java"}, true).stream().forEach(file -> {
			try {
				String content = Joiner.on("  ").join(FileUtils.readLines(file));

				Matcher matcher = PACKAGE_PATTERN.matcher(content);
				if (!matcher.matches()) {
					System.err.println("Copying failed: " + file);
					return;
				}
				String packageName = matcher.group(1);

				String path = packageName.replace(".", "/") + "/" + file.getName();

				if (content.contains("Stub!")) {
					System.out.println("Skip stub: " + file.getAbsolutePath());
					return;
				}

				System.out.println("Copying: " + path);
				FileUtils.copyFile(file, new File(workingDirectory, path));
			} catch (IOException e) {
				System.err.println("Copying failed: " + file);
				e.printStackTrace();
			}
		});
		deleteNotUsedFiles(workingDirectory);

		generateJarFile(workingDirectory, new File(getProjectDirectory(rootDirectory), "sources.jar"));
	}

	private static void generateJarFile(File workingDirectory, File targetFile) {
		System.out.println("Deleting " + targetFile.getName());
		FileUtils.deleteQuietly(targetFile);

		System.out.println("Generating: " + targetFile.getName());
		ZipUtil.pack(workingDirectory, targetFile);
	}

	private static void createJar(File rootDirectory, String sourcePath, String targetFileName, List<String> excludedDirectoryPatterns) {
		File workingDirectory = prepareWorkingDirectory();

		System.out.println("Searching jar files");
		FileUtils.listFiles(new File(rootDirectory, sourcePath), new NameFileFilter("classes-full-debug.jar"), new NotFileFilter(new WildcardFileFilter(excludedDirectoryPatterns))).stream().forEach(file -> {
			System.out.println("Unpacking: " + file.getAbsolutePath());
			ZipUtil.unpack(file, workingDirectory);
		});
		deleteNotUsedFiles(workingDirectory);

		generateJarFile(workingDirectory, new File(getProjectDirectory(rootDirectory), targetFileName));
	}

	private static void deleteNotUsedFiles(File workingDirectory) {
		FileUtils.listFiles(workingDirectory, TrueFileFilter.INSTANCE, null).stream().forEach(FileUtils::deleteQuietly);
	}

	private static File prepareWorkingDirectory() {
		File workingDirectory = new File(FileUtils.getTempDirectory(), EclipseProjectGenerator.class.getSimpleName());
		System.out.println("Deleting temporary directory");
		FileUtils.deleteQuietly(workingDirectory);
		return workingDirectory;
	}
}
