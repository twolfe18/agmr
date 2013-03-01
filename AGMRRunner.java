
import edu.jhu.agiga.*;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.iterator.TObjectIntIterator;
import java.util.*;
import java.io.*;

public class AGMRRunner implements Runnable {

	public static final long minMem = 50 * 1024 * 1024;
	
	public static void main(String[] args) throws Exception {
		if(args.length < 3) {
			usage();
			return;
		}
		AGMRRunner runner = new AGMRRunner(new File(args[1]));
		AGMRDocumentMapper m = (AGMRDocumentMapper) Class.forName(args[0]).newInstance();
		runner.addMapper(m);
		for(int i=2; i<args.length; i++)
			runner.addAgigaFile(args[i]);
		assert runner.enoughFreeMem();
		runner.run();
	}

	public static void usage() {
		System.out.println("usage: java AGMRRunner <mapper class> <scratch dir> <agiga file+>");
	}

	private File scratchDir;
	private List<File> agigaFiles;
	private List<MapperHolder> mappers;

	public AGMRRunner(File scratchDir) {
		this.scratchDir = scratchDir;
		this.agigaFiles = new ArrayList<File>();
		this.mappers = new ArrayList<MapperHolder>();
	}

	public void addAgigaFile(String path) {
		File f = new File(path);
		assert f.exists() && f.isFile();
		agigaFiles.add(f);
	}

	public void addMapper(AGMRDocumentMapper m) {
		mappers.add(new MapperHolder(m));
	}

	public boolean enoughFreeMem() {
		return Runtime.getRuntime().freeMemory() > minMem;
	}

	public void run() {

		assert scratchDir.exists() && scratchDir.isDirectory();
		
		// for timing
		int interval = 1000;
		long start = System.currentTimeMillis();
		long last_start = start;
		int docs = 0;
		long sentences = 0;

		for(File agf : agigaFiles) {
			System.out.println("reading " + agf.getPath());
			StreamingDocumentReader reader = new StreamingDocumentReader(agf.getPath(), new AgigaPrefs());
			for(AgigaDocument doc : reader) {

				docs++;
				sentences += doc.getSents().size();
				if(docs % interval == 0) {
					long now = System.currentTimeMillis();
					System.out.printf("read %d docs and %d sentences so far, %.1f docs/sec recently, %.1f docs/sec overall, free mem = %d MB\n",
						docs, sentences, (1000.0*interval)/(now - last_start), (1000.0*docs)/(now - start),
						Runtime.getRuntime().freeMemory()/1024/1024);
					last_start = now;
				}

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
						catch(Exception e) { throw new RuntimeException(e); }
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
			catch(Exception e) { throw new RuntimeException(e); }
		}

		System.out.printf("done, processed %d files, %d documents, and %d sentences in %.1f seconds\n",
			agigaFiles.size(), docs, sentences, (System.currentTimeMillis()-start)/1000.0);
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
				if(s.contains("\n"))
					continue;
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
			System.out.print("dumping records to " + f.getPath() + "...");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
			for(int i=0; i<keys.length; i++) {
				assert !keys[i].contains("\n");
				bw.write(String.format("%d\t%s\n", counts.get(keys[i]), keys[i]));
			}
			bw.close();

			dumped.add(f);
			counts = new TObjectIntHashMap<String>();
			System.gc();
			System.out.println("\tdone");
		}

		/**
		 * returns the merged file
		 */
		public File merge(File putIn) throws IOException {
			assert dumped.size() > 0;
			File m = dumped.size() > 1
				? MergeCount.merge(dumped, putIn, false)
				: dumped.get(0);
			File named = new File(putIn, mapper.name() + ".merged");
			m.renameTo(named);
			return named;
		}

	}

}

