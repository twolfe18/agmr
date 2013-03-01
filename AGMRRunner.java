
import edu.jhu.agiga.*;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.iterator.TObjectIntIterator;
import java.util.*;
import java.io.*;

public class AGMRRunner implements Runnable {
	
	public static void main(String[] args) throws Exception {
		if(args.length != 3) {
			usage();
			return;
		}
		AGMRRunner runner = new AGMRRunner(new File(args[1]), new File(args[2]));
		AGMRDocumentMapper m = (AGMRDocumentMapper) Class.forName(args[0]).newInstance();
		runner.addMapper(m);
		assert runner.enoughFreeMem();
		runner.run();
	}

	public static void usage() {
		System.out.println("usage: java AGMRRunner <mapper class> <agiga file> <scratch dir>");
	}

	private File scratchDir;
	private File agigaFile;
	private List<MapperHolder> mappers;

	public AGMRRunner(File agigaFile, File scratchDir) {
		this.agigaFile = agigaFile;
		this.scratchDir = scratchDir;
		this.mappers = new ArrayList<MapperHolder>();
	}

	public void addMapper(AGMRDocumentMapper m) {
		mappers.add(new MapperHolder(m));
	}

	public boolean enoughFreeMem() {
		return Runtime.getRuntime().freeMemory() > 30*1024*1024;
	}

	public void run() {

		assert scratchDir.exists() && scratchDir.isDirectory();
		assert agigaFile.exists() && agigaFile.isFile();
		StreamingDocumentReader reader = new StreamingDocumentReader(agigaFile.getPath(), new AgigaPrefs());
		for(AgigaDocument doc : reader) {

			// call the mappers
			for(MapperHolder m : mappers)
				m.map(doc);

			// check if we're using too much memory
			if(!enoughFreeMem()) {
				// find the biggest mappers, dump them
				Collections.sort(mappers);
				for(MapperHolder m : mappers) {
					try {
						m.dump(scratchDir);
						if(enoughFreeMem()) break;
					}
					catch(Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}

		for(MapperHolder m : mappers) {
			try {
				m.dump(scratchDir);
				File merged = m.merge(scratchDir);
				System.out.printf("%s merged output into %s\n", m.description(), merged.getCanonicalPath());
			}
			catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
	}


	/**
	 * comparator sorts from biggest to smallest by #key-values
	 */
	static class MapperHolder implements Comparable<MapperHolder> {

		private TObjectIntHashMap<String> counts;
		private AGMRDocumentMapper mapper;
		private List<File> dumped;

		public MapperHolder(AGMRDocumentMapper m) {
			mapper = m;
			counts = new TObjectIntHashMap<String>();
			dumped = new ArrayList<File>();
		}

		public List<String> map(AgigaDocument doc) {
			List<String> items = mapper.map(doc);
			for(String s : items) {
				if(!counts.increment(s))
					counts.put(s, 1);
			}
			return items;
		}

		@Override
		public int compareTo(MapperHolder o) {
			return o.size() - size();
		}

		public String description() {
			return String.format("%s.%d", mapper.name(), dumped.size());
		}

		public int size() { return counts.size(); }

		/**
		 * dumps key-value pairs in sorted-by-key order
		 * to a file in the directory provided
		 */
		public void dump(File putIn) throws IOException {
			assert putIn.exists() && putIn.isDirectory();

			// sort the keys
			String[] keys = counts.keys(new String[0]);
			Arrays.sort(keys);

			// writeout each key-value pair
			File f = new File(putIn, description() + ".dump");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
			for(int i=0; i<keys.length; i++) {
				bw.write(String.format("%d\t%s\n", counts.get(keys[i]), keys[i]));
			}
			bw.close();

			dumped.add(f);
			counts = new TObjectIntHashMap<String>();
			System.gc();
		}

		/**
		 * returns the merged file
		 */
		public File merge(File putIn) throws IOException {
			assert putIn.exists() && putIn.isDirectory();

			int c = 0;
			List<File> remaining = new ArrayList<File>(dumped);
			while(remaining.size() > 1) {
				List<File> nr = new ArrayList<File>();
				for(int i=0; i<remaining.size(); i+=2) {
					if(i == remaining.size()-1)
						nr.add(remaining.get(i));
					else {
						File out = new File(putIn, "merge" + (c++));
						merge(remaining.get(i), remaining.get(i+1), out);
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
						w.write(String.format("%d\t%s", prev_c, prev_s));
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
				else if(s1 == null) {
					w.write(String.format("%d\t%s", prev_c, prev_s));
					prev_c = c2;
					prev_s = s2;
					s2 = null;
				}
				else if(s2 == null) {
					w.write(String.format("%d\t%s", prev_c, prev_s));
					prev_c = c1;
					prev_s = s1;
					s1 = null;
				}

				// both streams are expended, flush and exit
				else {
					w.write(String.format("%d\t%s", prev_c, prev_s));
					break;
				}
			}

			w.close();
			r1.close();
			r2.close();

		}
	}

}

