package io.seldon.nlp;

import com.ibm.icu.text.Transliterator;

public class TransliteratorPeer {
private static ThreadLocal transHolder = new ThreadLocal();
    
    public static Transliterator getPunctuationTransLiterator()
	{
		Transliterator tl  = (Transliterator) transHolder.get();
		if (tl == null)
		{
			tl = Transliterator.getInstance("Any-Latin; Lower; NFD; [[:P:]] Remove; NFC;");
			transHolder.set(tl);
		}
		return tl;
	}
}
