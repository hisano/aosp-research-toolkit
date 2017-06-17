package jp.hisano.aosp_research_toolkit;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;

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
		Stream.of(rootDirectoryPaths).forEach(EclipseProjectGenerator::createProject);
	}

	private static void createProject(String rootDirectoryPath) {
		File rootDirectory = new File(rootDirectoryPath);

		createEmptyProject(rootDirectory);

		createJar(rootDirectory, "out/target/common/obj/JAVA_LIBRARIES", Lists.newArrayList("*support*", "cts*"), RESOURCE_DIRECTORY_NAME + "/frameworks.jar");
		createJar(rootDirectory, "out/target/common/obj/APPS", Lists.newArrayList("*Tests_*", "*tests_*", "Cts*"), RESOURCE_DIRECTORY_NAME + "/apps.jar");

		createSourcesJar(rootDirectory, ".", RESOURCE_DIRECTORY_NAME + "/sources.jar");

		createZip(rootDirectory);
	}

	private static void createZip(File rootDirectory) {
		String targetFileName = rootDirectory.getName() + ".zip";
		System.out.println("Generating: " + targetFileName);
		File projectDirectory = getProjectDirectory(rootDirectory);
		ZipUtil.pack(projectDirectory, new File(projectDirectory.getParentFile(), targetFileName));

		System.out.println("Deleting working directory");
		FileUtils.deleteQuietly(projectDirectory);
	}

	private static final String RESOURCE_DIRECTORY_NAME = "res";

	private static void createEmptyProject(File rootDirectory) {
		try {
			String projectName = rootDirectory.getName();

			System.out.println("Start generating project: " + projectName);
			File projectDirectory = getProjectDirectory(rootDirectory);
	
			System.out.println("Deleting previous project: " + projectName);
			FileUtils.deleteQuietly(projectDirectory);
	
			System.out.println("Generating project: " + projectName);
			FileUtils.copyDirectory(new File("template"), projectDirectory);
	
			File projectFile = new File(projectDirectory, ".project");
			FileUtils.write(projectFile, FileUtils.readFileToString(projectFile).replace("PROJECT_NAME", projectName), "UTF-8");
	
			File launchFile = new File(projectDirectory, RESOURCE_DIRECTORY_NAME + "/launch.xml");
			FileUtils.write(launchFile, FileUtils.readFileToString(launchFile).replace("PROJECT_NAME", projectName), "UTF-8");
			launchFile.renameTo(new File(projectDirectory, RESOURCE_DIRECTORY_NAME + "/Debug Selected Process in 'Devices' View with '" + projectName + "' Project.launch"));
		} catch (IOException e) {
			throw new UncheckedIOException("Generating Eclipse project failed", e);
		}
	}

	private static File getProjectDirectory(File rootDirectory) {
		return new File("target/" + rootDirectory.getName());
	}

	private static void createSourcesJar(File sourceRootDirectory, String sourceDirectoryPath, String targetFilePath) {
		File workingDirectory = prepareWorkingDirectory();

		System.out.println("Searching java files");
		FileUtils.listFiles(new File(sourceRootDirectory, sourceDirectoryPath), new String[] {"java"}, true).stream().forEach(file -> {
			try {
				AtomicReference<String> packageName = new AtomicReference<>();
				AtomicBoolean isStub = new AtomicBoolean();
				FileUtils.readLines(file).stream().forEach(line -> {
					line = line.trim();
					if (line.startsWith("package ") && line.contains(";")) {
						packageName.set(StringUtils.substringBetween(line, "package ", ";").trim());
					}
					if (line.contains("Stub!")) {
						isStub.set(true);
					}
				});
				isStub.set(isStub.get() || isStubFile(file));

				if (packageName.get() == null) {
					System.err.println("Skip copying because parsing package name failed: " + file.getAbsolutePath());
					return;
				}
				if (isStub.get()) {
					System.out.println("Skip copying because file is stub: " + file.getAbsolutePath());
					return;
				}

//				System.out.println("Copying: " + file.getAbsolutePath());
				FileUtils.copyFile(file, new File(workingDirectory, packageName.get().replace(".", "/") + "/" + file.getName()));
			} catch (IOException e) {
				System.err.println("Copying failed: " + file);
				e.printStackTrace();
			}
		});
		deleteNotUsedFiles(workingDirectory);

		createJar(workingDirectory, new File(getProjectDirectory(sourceRootDirectory), targetFilePath));
	}

	private static boolean isStubFile(File file) {
		String path = file.getAbsolutePath();
		return path.contains("android_stubs_current_intermediates") || path.contains("android_uiautomator_intermediates");
	}

	private static void createJar(File sourceRootDirectory, String sourceDirectoryPath, List<String> excludedDirectoryPatterns, String targetFilePath) {
		File workingDirectory = prepareWorkingDirectory();

		System.out.println("Searching jar files");
		FileUtils.listFiles(new File(sourceRootDirectory, sourceDirectoryPath), new NameFileFilter("classes-full-debug.jar"), new NotFileFilter(new WildcardFileFilter(excludedDirectoryPatterns))).stream().forEach(file -> {
			System.out.println("Unpacking: " + file.getAbsolutePath());
			try {
				ZipUtil.unpack(file, workingDirectory);
			} catch (ZipException e) {
				e.printStackTrace();
			}
		});
		deleteNotUsedFiles(workingDirectory);

		createJar(workingDirectory, new File(getProjectDirectory(sourceRootDirectory), targetFilePath));
	}

	private static void createJar(File workingDirectory, File targetFile) {
		System.out.println("Deleting previous file: " + targetFile.getName());
		FileUtils.deleteQuietly(targetFile);

		System.out.println("Generating: " + targetFile.getName());
		ZipUtil.pack(workingDirectory, targetFile);
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
