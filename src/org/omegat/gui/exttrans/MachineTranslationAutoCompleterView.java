package org.omegat.gui.exttrans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;

import org.omegat.core.Core;
import org.omegat.gui.editor.autocompleter.AutoCompleterItem;
import org.omegat.gui.editor.autocompleter.AutoCompleterListView;
import org.omegat.gui.editor.autocompleter.AutoCompleter;
import org.omegat.util.OStrings;
import org.omegat.util.Preferences;
import org.omegat.util.StringUtil;
import org.omegat.tokenizer.ITokenizer;

/**
 * Auto-completion view providing suggestions from machine translation results.
 */
public class MachineTranslationAutoCompleterView extends AutoCompleterListView {

    public MachineTranslationAutoCompleterView() {
        super(OStrings.getString("AC_MT_VIEW"));
    }

    @Override
    public List<AutoCompleterItem> computeListData(String prevText, boolean contextualOnly) {
        List<MachineTranslationInfo> infos = Core.getMachineTranslatePane().getDisplayedTranslations();
        if (infos == null || infos.isEmpty()) {
            return Collections.emptyList();
        }
        String token = getLastToken(prevText);
        List<AutoCompleterItem> contextual = new ArrayList<>();
        List<AutoCompleterItem> all = new ArrayList<>();
        Set<String> seenContext = new HashSet<>();
        Set<String> seenAll = new HashSet<>();
        Locale locale = getTargetLocale();
        for (MachineTranslationInfo info : infos) {
            String tr = info.result;
            String[] words = getTokenizer().tokenizeWordsToStrings(tr, ITokenizer.StemmingMode.NONE);
            for (String word : words) {
                if (seenAll.add(word.toLowerCase(locale))) {
                    all.add(new AutoCompleterItem(word, new String[] { info.translatorName }, 0));
                }
                if (!token.isEmpty() && word.startsWith(token) && !word.equals(token)) {
                    if (seenContext.add(word.toLowerCase(locale))) {
                        String payload = StringUtil.matchCapitalization(word, token, locale);
                        contextual.add(new AutoCompleterItem(payload, new String[] { info.translatorName }, token.length()));
                    }
                }
            }
        }

        if (!contextual.isEmpty() || contextualOnly) {
            return contextual;
        }
        return all;
    }

    @Override
    public String itemToString(AutoCompleterItem item) {
        return item.payload;
    }

    @Override
    protected boolean isEnabled() {
        return Preferences.isPreferenceDefault(Preferences.AC_MACHINETRANSLATION_ENABLED, true);
    }

    @Override
    public boolean shouldPopUp() {
        String leadingText = getLeadingText();
        List<AutoCompleterItem> entries = computeListData(leadingText, true);
        return !entries.isEmpty()
                && (leadingText.codePointCount(0, leadingText.length()) > 1
                        || entries.size() <= AutoCompleter.PAGE_ROW_COUNT);
    }

    private Locale getTargetLocale() {
        return getTargetLanguage().getLocale();
    }
}
