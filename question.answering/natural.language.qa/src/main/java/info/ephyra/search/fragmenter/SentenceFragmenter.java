package info.ephyra.search.fragmenter;

import java.text.BreakIterator;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.search.highlight.Fragmenter;

public class SentenceFragmenter implements Fragmenter {

	private BreakIterator sentenceIterator = BreakIterator.getSentenceInstance();
	private OffsetAttribute offsetAttribute;
	// holds the starting character of the previous token
	private int prevTokenPosition;

	public boolean isNewFragment() {
		// the starting character of the current token
		int currTokenPosition = offsetAttribute.startOffset();
		int prevSentenceEnd = sentenceIterator.preceding(currTokenPosition);
		boolean isNewSentence = false;
		// if current sentence start is after last token then this is a new sentence
		if (prevSentenceEnd > prevTokenPosition) {
			isNewSentence = true;
		}
		prevTokenPosition = currTokenPosition;
		return isNewSentence;
	}

	public void start(String originalText, TokenStream tokenStream) {
		prevTokenPosition = 0;
		sentenceIterator.setText(originalText);
		offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
	}

}
