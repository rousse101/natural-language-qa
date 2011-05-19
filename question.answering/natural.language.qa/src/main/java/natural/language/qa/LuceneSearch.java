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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
			String index = "D:/dev/InformationRetrieval/wikiindex";
			String field = "contents";

			Directory indexDir = FSDirectory.open(new File(index));
			
			// ========
			// read index and extract the matches for 'graphics' in each file (position in terms of tokens not chars) 
//			IndexReader idxReader = IndexReader.open(indexDir);
//			Term t = new Term(field, "graphics");
//			TermPositions termPositions = idxReader.termPositions(t);
//			while (termPositions.next()) {
//				System.out.println(termPositions.doc() + ", " + termPositions.nextPosition() + ", "
//						+ idxReader.document(termPositions.doc()).get("path"));
//				System.out.println("++++++++++++++++++++++++++++++++++++");
//
//			}
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
			h.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
			
			// ================================================

			// Collect docs
			TopDocs res = searcher.search(query, maxRes);
			int numTotalHits = res.totalHits;
			ScoreDoc[] scoreDocs = res.scoreDocs;

//			for (ScoreDoc scoreDoc : scoreDocs) {
//				Document doc = searcher.doc(scoreDoc.doc);
//				String path = doc.get("path");
//				String content = readDocument(path);
//				LuceneSearchResult hit = new LuceneSearchResult(scoreDoc.doc, path, content);
//				results.add(hit);
//			}
			for (ScoreDoc scoreDoc : scoreDocs) {
				Document doc = searcher.doc(scoreDoc.doc);
				String path = doc.get("path");
				String content = readDocument(path);
//				String bestFragment = h.getBestFragment(analyzer, field, content);
				String[] bestFragments = h.getBestFragments(analyzer, field, content, 5);
				String frag = " ";
				for (String string : bestFragments) {
					frag = frag + " " + string;
				}
				if (frag == null || frag.length() == 0) {
					continue;
				}
				System.out.println(frag);
				LuceneSearchResult hit = new LuceneSearchResult(scoreDoc.doc, path, frag);
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
		StringBuffer strFileContents = new StringBuffer();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();
			while(line != null) {
				strFileContents.append(line).append("\n");
				line = reader.readLine();
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return strFileContents.toString();
	}

	public static void main(String[] args) throws Exception {
		LuceneSearch ls = new LuceneSearch();
		List<LuceneSearchResult> searchRes = ls.search("moon", 1000);

		for (LuceneSearchResult res : searchRes) {
			System.out.println("============================================================");
			System.out.println(res.getDocId());
			System.out.println(res.getPath());
			System.out.println("------------------------------------------------------------");
			System.out.println(res.getContent());
		}
	}

}
