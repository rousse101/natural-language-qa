

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MakeCorpus {

	private static final String ANCHOR_PATTERN_STR = "<a\\b[^>]*href=\"[^>]*>(.*?)</a>";
	private static final String DOC_PATTERN_STR = "<doc\\b[^>]*id=\"(\\d+)\"[^>]*>(.*?)</doc>";
	private static final Pattern ANCHOR_PATTERN = Pattern.compile(ANCHOR_PATTERN_STR, Pattern.DOTALL);
	private static final Pattern DOC_PATTERN = Pattern.compile(DOC_PATTERN_STR, Pattern.DOTALL);

	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();
		String indir = args[0];
		System.out.println("Input " + indir);
		String outdir = args[1];
		System.out.println("Output " + outdir);
		File dir = new File(indir);
		File[] listFiles = dir.listFiles();
		for (File parentDir : listFiles) {
			File[] innermostDirs = parentDir.listFiles();
			for (File leaf : innermostDirs) {
				String outDir = outdir + "/" + parentDir.getName();
				processFile(leaf.getAbsolutePath(), outDir);
			}
		}
		long end = System.currentTimeMillis();
		System.out.println("Finished in : " + (end - start) + " ms");
	}
	
	private static void processFile(String fileName, String outDir) throws Exception {
		// read file into a String
		String fileContent = readFileAsString(fileName);

		// extract different articles
		List<Article> articles = new LinkedList<Article>();
		Matcher matcher = DOC_PATTERN.matcher(fileContent);
		while (matcher.find()) {
			int id = Integer.parseInt(matcher.group(1));
			String text = stripAnchorTags(matcher.group(2));
			articles.add(new Article(id, text));
		}
		// for each article strip all anchor tags and write the article in
		// separate file
		for (Article article : articles) {
			String outFileName = outDir + "/" + article.getId();
			new File(outDir).mkdirs();
			writeStringToFile(article.getText(), outFileName);
		}
	}

	private static String stripAnchorTags(String text) {
		Matcher matcher = ANCHOR_PATTERN.matcher(text);
		while (matcher.find()) {
			text = text.replace(matcher.group(0), matcher.group(1));
		}
		return text;
	}

	private static String readFileAsString(String filePath) throws java.io.IOException {
		byte[] buffer = new byte[(int) new File(filePath).length()];
		BufferedInputStream f = null;
		try {
			f = new BufferedInputStream(new FileInputStream(filePath));
			f.read(buffer);
		} finally {
			if (f != null)
				try {
					f.close();
				} catch (IOException ignored) {
				}
		}
		return new String(buffer);
	}

	private static void writeStringToFile(String content, String fileName) throws IOException {
		File fout = new File(fileName);
		fout.createNewFile();
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(fout));
			writer.write(content);
		} finally {
			if (writer!= null) {
				writer.close();
			}
		}
	}

}
