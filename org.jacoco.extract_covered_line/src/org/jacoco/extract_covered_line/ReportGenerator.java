/*******************************************************************************
 * Copyright (c) 2009, 2022 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Brock Janiczak - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.extract_covered_line;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.*;
import java.io.FileWriter;
import java.util.stream.Collectors;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IMethodCoverage;

/**
 * This example creates a HTML report for eclipse like projects based on a
 * single execution data store called jacoco.exec. The report contains no
 * grouping information.
 *
 * The class files under test must be compiled with debug information, otherwise
 * source highlighting will not work.
 */
public class ReportGenerator {

	private final String title;

	private final File executionDataFile;
	private final File classesDirectory;
	private final File sourceDirectory;
	private final File reportDirectory;

	private ExecFileLoader execFileLoader;

	/**
	 * Create a new generator based for the given project.
	 *
	 * @param projectDirectory
	 */
	public ReportGenerator(final File projectDirectory) {
		this.title = projectDirectory.getName();
		this.executionDataFile = new File(projectDirectory, "jacoco.exec");
		this.classesDirectory = new File(projectDirectory, "classes");
		this.sourceDirectory = new File(projectDirectory, "src");
		this.reportDirectory = new File(projectDirectory, "coveragereport");
	}

	/**
	 * Create the report.
	 *
	 * @throws IOException
	 */
	public void create() throws IOException {

		// Read the jacoco.exec file. Multiple data files could be merged
		// at this point
		loadExecutionData();

		// Run the structure analyzer on a single class folder to build up
		// the coverage model. The process would be similar if your classes
		// were in a jar file. Typically you would create a bundle for each
		// class folder and each jar you want in your report. If you have
		// more than one bundle you will need to add a grouping node to your
		// report
		final IBundleCoverage bundleCoverage = analyzeStructure();

		createReport(bundleCoverage);

		myReport(bundleCoverage);
	}

	void myReport(final IBundleCoverage bundleCoverage) throws IOException {
		Collection<IPackageCoverage> packageCollection = bundleCoverage
				.getPackages();
		List<String> resultList = new ArrayList<String>();
		// for(int i= 0; i< packageCol.size(); i++){
		// System.out.println("Bundle Package ="+ packageCol.get(i));
		// }

		for (IPackageCoverage packageName : packageCollection) {
			// System.out.println(packageName);
			Collection<IClassCoverage> classCollection = packageName
					.getClasses();
			for (IClassCoverage className : classCollection) {
				// System.out.println(className);
				for (int i = className.getFirstLine(); i <= className
						.getLastLine(); i++) {
					// System.out.println(" Line Number = "+ Integer.valueOf(i)
					// + " Status= " + className.getLine(i).getStatus());

					final int status = className.getLine(i).getStatus();
					// className.getLine(i).getStatus()
					switch (status) {
					case ICounter.NOT_COVERED:
						// System.out.println("Not Covered" +" Class Name = "+
						// className + " Line Number = "+ Integer.valueOf(i) + "
						// Status= " + className.getLine(i).getStatus());
						break;
					case ICounter.PARTLY_COVERED:
						resultList.add(
								className.getName() + ":" + Integer.valueOf(i));
						// System.out.println("Partially Covered" +" Class Name
						// = "+ className + " Line Number = "+
						// Integer.valueOf(i) + " Status= " +
						// className.getLine(i).getStatus());
						break;
					case ICounter.FULLY_COVERED:

						// className = className.replaceAll( "[CLASS]" , " ");
						resultList.add(
								className.getName() + ":" + Integer.valueOf(i));
						// System.out.println("Fully Covered" +" Class Name = "+
						// className + " Line Number = "+ Integer.valueOf(i) + "
						// Status= " + className.getLine(i).getStatus());
						break;
					}

				}

				// Check the Enum Type( Covered, Not_Covered)
				/*
				 * Collection<IMethodCoverage> methodCol =
				 * className.getMethods(); for(IMethodCoverage methodName:
				 * methodCol){
				 *
				 * System.out.println(methodName); }
				 */

			}
		}
		System.out.println("List Size =" + resultList.size());
		FileWriter writer = new FileWriter(title+"/output.csv");
		String collect = resultList.stream().collect(Collectors.joining(","));
		// System.out.println(collect);

		writer.write(collect);
		writer.close();

		/*
		 * BufferedWriter bf = null; try { FileWriter outputFile = new
		 * FileWriter("Result.csv"); bf = new BufferedWriter(outputFile);
		 *
		 * for (Map.Entry<InterceptionPoint, InterceptionPoint > entry :
		 * Utility.resultInterception.entrySet()){
		 *
		 * String conflictingPair = entry.getKey() + ":" + entry.getValue();
		 * if(!conflictingListPair.contains(conflictingPair)) {
		 * conflictingListPair.add(conflictingPair); bf.write(conflictingPair);
		 * bf.newLine(); } } bf.flush(); } catch (IOException e) {
		 * System.out.println("An error occurred."); e.printStackTrace(); }
		 * finally { try { bf.close(); } catch (Exception e) { }
		 * //System.out.println(Utility.resultInterception); }
		 */

		// System.out.println("Bundle P
		// ="+bundleCoverage.getPackages().getClasses());
		// System.out.println("Bundle Package
		// ="+bundleCoverage.getPackages().getClasses());
	}

	private void createReport(final IBundleCoverage bundleCoverage)
			throws IOException {

		// Create a concrete report visitor based on some supplied
		// configuration. In this case we use the defaults
		final HTMLFormatter htmlFormatter = new HTMLFormatter();
		final IReportVisitor visitor = htmlFormatter
				.createVisitor(new FileMultiReportOutput(reportDirectory));

		// Initialize the report with all of the execution and session
		// information. At this point the report doesn't know about the
		// structure of the report being created
		visitor.visitInfo(execFileLoader.getSessionInfoStore().getInfos(),
				execFileLoader.getExecutionDataStore().getContents());

		// Populate the report structure with the bundle coverage information.
		// Call visitGroup if you need groups in your report.
		visitor.visitBundle(bundleCoverage,
				new DirectorySourceFileLocator(sourceDirectory, "utf-8", 4));

		// Signal end of structure information to allow report to write all
		// information out
		visitor.visitEnd();

	}

	private void loadExecutionData() throws IOException {
		execFileLoader = new ExecFileLoader();
		execFileLoader.load(executionDataFile);
	}

	private IBundleCoverage analyzeStructure() throws IOException {
		final CoverageBuilder coverageBuilder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(
				execFileLoader.getExecutionDataStore(), coverageBuilder);

		analyzer.analyzeAll(classesDirectory);

		return coverageBuilder.getBundle(title);
	}

	/**
	 * Starts the report generation process
	 *
	 * @param args
	 *            Arguments to the application. This will be the location of the
	 *            eclipse projects that will be used to generate reports for
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException {
		for (int i = 0; i < args.length; i++) {
			System.out.println("HELLO SHANTO");
			final ReportGenerator generator = new ReportGenerator(
					new File(args[i]));

			System.out.println(args[i]);
			generator.create();
		}
	}

}
