package natural.language.qa;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryTermScorer;
import org.apache.lucene.search.highlight.Scorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class LuceneSearch {

	public List<LuceneSearchResult> search(String queryString, int maxRes) throws Exception {
		IndexSearcher searcher = null;
		List<LuceneSearchResult> results = new ArrayList<LuceneSearchResult>();
		try {
			String index = "D:/dev/eclipse-jee-helios-win32-x86_64/workspace/LuceneDemo/index";
			String field = "contents";

			Directory indexDir = FSDirectory.open(new File(index));
			
			// ========
			// read index and extract the matches for 'graphics' in each file (position in terms of tokens not chars) 
			IndexReader idxReader = IndexReader.open(indexDir);
			Term t = new Term(field, "graphics");
			TermPositions termPositions = idxReader.termPositions(t);
			while (termPositions.next()) {
				System.out.println(termPositions.doc() + ", " + termPositions.nextPosition() + ", "
						+ idxReader.document(termPositions.doc()).get("path"));
				System.out.println("++++++++++++++++++++++++++++++++++++");

			}
			// ========

			searcher = new IndexSearcher(indexDir);
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_31);

			QueryParser parser = new QueryParser(Version.LUCENE_31, field, analyzer);

			queryString = queryString.trim();
			if (queryString.length() == 0) {
				return results;
			}

			Query query = parser.parse(queryString);
			System.out.println("Searching for: " + query.toString(field));

			// ================================================
			Formatter f = new SimpleHTMLFormatter("", "");
			Encoder e = new DefaultEncoder();
			Scorer fs = new QueryTermScorer(query);
			Highlighter h = new Highlighter(f, e, fs);
			// ================================================

			// Collect docs
			TopDocs res = searcher.search(query, maxRes);
			int numTotalHits = res.totalHits;
			ScoreDoc[] scoreDocs = res.scoreDocs;

			// for (ScoreDoc scoreDoc : scoreDocs) {
			// Document doc = searcher.doc(scoreDoc.doc);
			// String path = doc.get("path");
			// String content = readDocument(path);
			// LuceneSearchResult hit = new LuceneSearchResult(scoreDoc.doc,
			// path, content);
			// results.add(hit);
			// }
			for (ScoreDoc scoreDoc : scoreDocs) {
				Document doc = searcher.doc(scoreDoc.doc);
				String path = doc.get("path");
				String content = readDocument(path);
				// doStuff(content, analyzer, field);
				String bestFragment = h.getBestFragment(analyzer, field, content);
				LuceneSearchResult hit = new LuceneSearchResult(scoreDoc.doc, path, bestFragment);
				results.add(hit);
			}
			System.out.println(numTotalHits + " total matching documents");
		} finally {
			if (searcher != null) {
				searcher.close();
			}
		}
		return results;
	}

	private void doStuff(String content, Analyzer analyzer, String fieldName) throws IOException {
		System.out.println("------------------------------------------------------------------------");
		TokenStream tokenStream = analyzer.tokenStream(fieldName, new StringReader(content));
		CharTermAttribute charTermAttr = tokenStream.addAttribute(CharTermAttribute.class);
		OffsetAttribute offsetAttr = tokenStream.addAttribute(OffsetAttribute.class);
		PositionIncrementAttribute posIncAttr = tokenStream.addAttribute(PositionIncrementAttribute.class);
		try {
			tokenStream.reset();

			while (tokenStream.incrementToken()) {
				System.out.println("ch: " + charTermAttr.toString());
				String str = content.substring(offsetAttr.startOffset(), offsetAttr.endOffset());
				System.out.println(str + ", inc " + posIncAttr.getPositionIncrement());
			}
		} finally {
			tokenStream.close();
		}
	}

	private String readDocument(String path) throws Exception {
		File file = null;
		BufferedInputStream bin = null;
		String strFileContents = "";
		try {
			// create FileInputStream object
			FileInputStream fin = new FileInputStream(path);

			// create object of BufferedInputStream
			bin = new BufferedInputStream(fin);

			// create a byte array
			byte[] contents = new byte[1024];

			int bytesRead = 0;

			while ((bytesRead = bin.read(contents)) != -1) {

				strFileContents = new String(contents, 0, bytesRead);
			}
		} finally {
			if (bin != null) {
				bin.close();
			}
		}
		return strFileContents;
	}

	public static void main(String[] args) throws Exception {
		LuceneSearch ls = new LuceneSearch();
		List<LuceneSearchResult> searchRes = ls.search("computer", 1000);

//		 for (LuceneSearchResult res : searchRes) {
//		 System.out.println("============================================================");
//		 System.out.println(res.getDocId());
//		 System.out.println(res.getPath());
//		 System.out.println("------------------------------------------------------------");
//		 System.out.println(res.getContent());
//		 }

	}

}
