package dsa_assignment4;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.log4j.Logger;

import dsa_assignment4.CsvFormatter.RowComparator;

/**
 * A class containing only static methods to externally sort simplified CSV
 * files
 */
public class CsvUtils
{
	private static final Logger logger  = Logger.getLogger(CsvUtils.class);

	// Your "data" directory will be at the top level of your Eclipse
	// project directory for this assignment: do not change the name
	// or put it anywhere else: the marking software will cause your
	// program to fail if it tries to read or write any files outside
	// this directory
	private static final Path   dataDir = Paths.get("data");

	/**
	 * For marking purposes
	 * 
	 * @return Your student id
	 */
	public static String getStudentID()
	{
		//change this return value to return your student id number, e.g. 
		// return "1234567";
		return "1911437";
	}

	/**
	 * For marking purposes
	 * 
	 * @return Your name
	 */
	public static String getStudentName()
	{
		//change this return value to return your name, e.g.
		// return "John Smith";
		return "TOM MOSES";
	}

	/**
	 * An accessor method to return the path of your data directory
	 * 
	 * @return the path to your data directory
	 */
	public static Path getDataDir()
	{
		return dataDir;
	}

	/**
	 * A sample method to show the basic mechanism for reading and writing CSV
	 * files using the CsvFormatter class. This just copies the input file to
	 * the output file with no changes. However it has to make sure that the
	 * output file is created with the correct CSV header.
	 * 
	 * @param fromPath
	 *            The path of the CSV file to read from
	 * @param toPath
	 *            The path of the CSV file to write to
	 * @return true if it manages to complete without throwing exceptions (if
	 *         this were an empty method that you had to implement as part of
	 *         this assignment, you should leave the return value as false until
	 *         you had completed it to avoid unnecessary testing of an
	 *         unimplemented method
	 * @throws Exception
	 *             if anything goes wrong, e.g. if you can't open either file,
	 *             can't read from the fromPath file, can't write to the toPath
	 *             file, if the from file does not match the requirements of the
	 *             simplified CSV file format, etc.
	 */
	public static boolean copyCsv(Path fromPath, Path toPath)
		throws Exception
	{
		// Open both the from and the to files using a "try-with-resource" pattern
		// This ensures that, no matter what happens in terms of returns or exceptions,
		// both files will be correctly closed automatically
		try (Scanner from = new Scanner(fromPath); PrintWriter to = new PrintWriter(toPath.toFile()))
		{
			// Setup the CSV format from the "from" file
			CsvFormatter formatter = new CsvFormatter(from);

			// Output the CSV header row to the "to" file
			formatter.writeHeader(to);

			// copy each non-header row from the "from" file to the "to" file 
			String[] row;
			while ((row = formatter.readRow(from)) != null)
				formatter.writeRow(to, row);
		}
		return true;
	}

	/**
	 * Split an (unordered) CSV file into separate smaller CSV files (runs)
	 * containing sorted runs of row, where the rows are sorted in ascending
	 * order of the column identified by the <code>columnName</code> parameter.
	 * This is intended to be the first stage of a merge sort which produces
	 * sorted runs that can then be merged together.
	 * <p>
	 * This code should work on truly huge files: far larger than we can hold in
	 * memory at the same time. To simulate this without using huge files, we
	 * impose a limit on the size of each run, given by the
	 * <code>numRowLimit</code> parameter. Further, NO internal sort algorithms
	 * should be used: e.g. Arrays.sort, Collections.sort, SortedList etc.
	 * Instead a {@link PriorityQueue} must be used to generate the sorted runs:
	 * Have a loop. Inside the loop, read in a maximum of
	 * <code>numRowLimit</code> rows from the input and insert them into the
	 * priority queue and then extract them in order and write them out to a new
	 * split file.
	 * </p>
	 * <p>
	 * The split file should be a sibling (i.e. in the same directory) as the
	 * input file and have a name which is "temp_00000_" followed by the name of
	 * the input file, where the "00000" is replace by a sequence number:
	 * "00000" for the first split file, "00001" for the second etc.
	 * </p>
	 * 
	 * @param fromPath
	 *            The relative path where the input file is
	 * @param columnName
	 *            The header name of the column used for sorting
	 * @param numRowLimit
	 *            The maximum number of value rows (not including the header
	 *            row) that can be written into each split file
	 * @return the <code>Path[]</code> of paths for the full list of split files created
	 * @throws Exception
	 *             If anything goes wrong with opening, reading or writing the
	 *             files, or if the input file does not match the simplified CSV
	 *             requirements.
	 */
	public static Path[] splitSortCsv(Path fromPath, String columnName, int numRowLimit)
		throws Exception
	{
		Deque<Path> pathDeque = new LinkedList<>();

		// WRITE YOUR CODE HERE AND REPLACE THE RETURN STATEMENT

		try (Scanner from = new Scanner(fromPath))
		{
			CsvFormatter formatter = new CsvFormatter(from);
			int fileCounter = 1;
			Boolean moreLines = true;
			String[] row;
			PriorityQueue<String[]> pQueue = new PriorityQueue<String[]>(numRowLimit, formatter.new RowComparator(columnName));
			Path tmpPath = fromPath.resolveSibling(String.format("temp_%05d_%s",fileCounter, fromPath.getFileName()));
			pathDeque.push(tmpPath);
			PrintWriter to = new PrintWriter(tmpPath.toFile());

			while (moreLines)
			{
				try
				{
					row = formatter.readRow(from);
					pQueue.add(row);
					if (pQueue.size() == numRowLimit)
					{
						// write current priority queue to file
						formatter.writeHeader(to);
						while (pQueue.size() > 0)
						{
							formatter.writeRow(to, pQueue.poll());
						}
						// sets up next file
						fileCounter++;
						tmpPath = fromPath.resolveSibling(String.format("temp_%05d_%s",fileCounter, fromPath.getFileName()));
						pathDeque.push(tmpPath);
						to.close();
						to = new PrintWriter(tmpPath.toFile());
					}
				}
				catch (Exception e)
				{
					moreLines = false;

				}
			}
			formatter.writeHeader(to);
			while (pQueue.size() > 0)
			{
				formatter.writeRow(to, pQueue.poll());
			}
			to.close();
		}

		return pathDeque.toArray(new Path[0]);
	}


	/**
	 * Merge two ordered input CSV files into a single ordered output CSV file
	 * 
	 * The two input CSV files must be already ordered on the column specified
	 * by <code>columnName</code> and must have the same CSV format (same number
	 * of columns, same headers in the same order) The output file must
	 * similarly be of the same CSV format and ordered on the same column.
	 * 
	 * @param file1Path
	 *            The relative path of the first input file
	 * @param file2Path
	 *            The relative path of the second input file
	 * @param columnName
	 *            The column to order the output file on and, upon which, both
	 *            input files are ordered
	 * @param outputPath
	 *            The relative path of the output file
	 * @return true, if this method has been implemented. If it has not yet been
	 *         implemented, then it returns false and this is used to cause the
	 *         unit test to fail early without doing a lot of unnecessary work
	 * @throws Exception
	 *             If anything goes wrong with opening, reading or writing the
	 *             files, or if the input files do not match the simplified CSV
	 *             requirements or have different CSV formats
	 */
	public static boolean mergePairCsv(Path file1Path, Path file2Path, String columnName, Path outputPath)
		throws Exception
	{
		// WRITE YOUR CODE HERE AND REPLACE THE RETURN STATEMENT
		try (	Scanner from1 = new Scanner(file1Path);
				Scanner from2 = new Scanner(file2Path))
		{
			CsvFormatter formatter1 = new CsvFormatter(from1);
			CsvFormatter formatter2 = new CsvFormatter(from2);
			if (formatter1.getHeaderStrings().length != formatter2.getHeaderStrings().length)
			{
				throw new Exception("Headers not same length");
			}
			for (int i = 0; i < formatter1.getHeaderStrings().length; i++)
			{
				if (!formatter1.getHeaderStrings()[i].equals(formatter2.getHeaderStrings()[i]))
				{
					throw new Exception("Headers not same words");
				}
			}
			try (PrintWriter to = new PrintWriter(outputPath.toFile()))
			{
				formatter1.writeHeader(to);
				String[] row1 = null;
				String[] row2 = null;
				Boolean moreLines1 = true;
				Boolean moreLines2 = true;
				Comparator comparator = formatter1.new RowComparator(columnName);
				int choice;
				while (moreLines1 && moreLines2)
				{
					if (row1 == null)
					{
						try
						{
							row1 = formatter1.readRow(from1);
						}
						catch (Exception e)
						{
							moreLines1 = false;
						}
					}
					if (row2 == null)
					{
						try
						{
							row2 = formatter2.readRow(from2);
						}
						catch (Exception e)
						{
							moreLines2 = false;
						}
					}
					if (row1 == null)
					{
						moreLines1 = false;
					}
					if (row2 == null)
					{
						moreLines2 = false;
					}
					if (moreLines1 && moreLines2)
					{
						choice = comparator.compare(row1, row2);
						if (choice < 0)
						{
							formatter1.writeRow(to, row1);
							row1 = null;
						}
						else
						{
							formatter1.writeRow(to, row2);
							row2 = null;
						}
					}
				}
				while (moreLines1)
				{
					try
					{
						formatter1.writeRow(to, row1);
						row1 = formatter1.readRow(from1);
					}
					catch (Exception e)
					{
						moreLines1 = false;
					}
				}
				while (moreLines2)
				{
					try
					{
						formatter1.writeRow(to, row2);
						row2 = formatter1.readRow(from2);
					}
					catch (Exception e)
					{
						moreLines2 = false;
					}
				}
			}
		}
		catch (Exception e)
		{
			throw e;
		}

		return true;
	}

	/**
	 * Merge a list of ordered input CSV files into a single ordered output CSV
	 * file
	 * <p>
	 * The input CSV files must be already ordered on the column specified by
	 * <code>columnName</code> and must have the same CSV format (same number of
	 * columns, same headers in the same order) The output file must similarly
	 * be of the same CSV format and ordered on the same column.
	 * </p>
	 * <p>
	 * This method should merge all the files together by calling
	 * <code>mergePairCsv(...)</code> on pairs of files, starting with those on
	 * <code>pathList</code>, producing larger and larger intermediate file
	 * until the last pair-wise merge is used to produce the output file.
	 * </p>
	 * 
	 * @param pathList
	 *            An array of relative paths of the input files
	 * @param columnName
	 *            The column to order the output file on and, upon which, both
	 *            input files are ordered
	 * @param outputPath
	 *            The relative path of the output file
	 * @return true, if this method has been implemented. If it has not yet been
	 *         implemented, then it returns false and this is used to cause the
	 *         unit test to fail early without doing a lot of unnecessary work
	 * @throws Exception
	 *             If anything goes wrong with opening, reading or writing the
	 *             files, or if the input files do not match the simplified CSV
	 *             requirements or have different CSV formats
	 */
	public static boolean mergeListCsv(Path[] pathList, String columnName, Path outputPath)
		throws Exception
	{
		Deque<Path> paths = new LinkedList<>(Arrays.asList(pathList));

		// WRITE YOUR CODE HERE AND REPLACE THE RETURN STATEMENT


		int fileCounter = 0;
		while (paths.size() > 1)
		{
			fileCounter ++;
			Path tmpPath = outputPath.resolveSibling(String.format("temp_merge%05d_%s",fileCounter, outputPath.getFileName()));
			if (paths.size() == 2)
			{
				tmpPath = outputPath;
			}
			mergePairCsv(paths.removeFirst(), paths.removeFirst(), columnName, tmpPath);
			paths.addLast(tmpPath);
		}


		return true;

	}
}
