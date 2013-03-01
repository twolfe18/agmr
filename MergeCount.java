
import java.io.*;
import java.util.*;

class MergeCount {

	// TODO write a main method to give CLI access to this

	public static File merge(List<File> input, File outputDir) throws IOException {
		return merge(input, outputDir, true);
	}

	/**
	 * merges many soreted files
	 * will put scratch files in outputDir named "merge.*"
	 */
	public static File merge(List<File> input, File outputDir, boolean deleteScratch) throws IOException {
		assert outputDir.exists() && outputDir.isDirectory();
		assert input.size() > 1;
		int c = 0;
		List<File> remaining = new ArrayList<File>(input);
		while(remaining.size() > 1) {
			List<File> nr = new ArrayList<File>();
			for(int i=0; i<remaining.size(); i+=2) {
				if(i == remaining.size()-1)
					nr.add(remaining.get(i));
				else {
					File out = new File(outputDir, "merge." + (c++));
					merge(remaining.get(i), remaining.get(i+1), out);
					if(deleteScratch) {
						remaining.get(i).delete();
						remaining.get(i+1).delete();
					}
					nr.add(out);
				}
			}
			remaining = nr;
		}
		return remaining.get(0);
	}

	/**
	 * expects two sorted files
	 */
	private static void merge(File countFile1, File countFile2, File output) throws IOException {

		System.out.printf("merging %s and %s into %s...", countFile1.getPath(), countFile2.getPath(), output.getPath());

		BufferedReader r1 = new BufferedReader(new InputStreamReader(new FileInputStream(countFile1), "UTF-8"));
		BufferedReader r2 = new BufferedReader(new InputStreamReader(new FileInputStream(countFile2), "UTF-8"));
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), "UTF-8"));

		int c1 = 0, c2 = 0;				// counts
		String s1 = null, s2 = null;	// keys

		// current state
		int prev_c = 0;
		String prev_s = null;

		while(true) {

			// read in a line if needed
			if(s1 == null && r1.ready()) {
				String[] ar = r1.readLine().split("\t", 2);
				c1 = Integer.parseInt(ar[0]);
				s1 = ar[1];	// don't trim, print doesn't have newline
			}
			if(s2 == null && r2.ready()) {
				String[] ar = r2.readLine().split("\t", 2);
				c2 = Integer.parseInt(ar[0]);
				s2 = ar[1];	// don't trim, print doesn't have newline
			}

			// note that s1 = null means to read s1 on the next loop
			if(s1 != null && s2 != null) {

				// both lines are equal to the last one
				if(s1.equals(prev_s) && s2.equals(prev_s)) {
					prev_c += c1 + c2;
					s1 = null;
					s2 = null;
				}

				// one line is equal, accum and make null (for reading in next loop)
				else if(s1.equals(prev_s)) {
					prev_c += c1;
					s1 = null;
				}
				else if(s2.equals(prev_s)) {
					prev_c += c2;
					s2 = null;
				}

				// neither line is equal, write out prev, store minimum
				else {
					if(prev_s != null)
						w.write(String.format("%d\t%s\n", prev_c, prev_s));
					if(s1.compareTo(s2) < 0) {
						prev_c = c1;
						prev_s = s1;
						s1 = null;
					}
					else {
						prev_c = c2;
						prev_s = s2;
						s2 = null;
					}
				}
			}

			// one file is done, just write from the non-empty one
			else if(s1 == null && s2 != null) {
				w.write(String.format("%d\t%s\n", prev_c, prev_s));
				prev_c = c2;
				prev_s = s2;
				s2 = null;
			}
			else if(s1 != null && s2 == null) {
				w.write(String.format("%d\t%s\n", prev_c, prev_s));
				prev_c = c1;
				prev_s = s1;
				s1 = null;
			}

			// both streams are expended, flush and exit
			else {
				w.write(String.format("%d\t%s\n", prev_c, prev_s));
				break;
			}
		}

		w.close();
		r1.close();
		r2.close();
		System.out.println("\tdone");

	}

}

