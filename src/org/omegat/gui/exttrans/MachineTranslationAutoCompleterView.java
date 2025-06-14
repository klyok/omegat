package org.omegat.gui.exttrans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.omegat.core.Core;
import org.omegat.gui.editor.autocompleter.AutoCompleterItem;
import org.omegat.gui.editor.autocompleter.AutoCompleterListView;
import org.omegat.util.OStrings;
import org.omegat.util.Preferences;

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
        List<AutoCompleterItem> result = new ArrayList<>();
        for (MachineTranslationInfo info : infos) {
            String tr = info.result;
            if (contextualOnly) {
                if (!token.isEmpty() && tr.startsWith(token) && !tr.equals(token)) {
                    result.add(new AutoCompleterItem(tr, new String[] { info.translatorName }, token.length()));
                }
            } else {
                result.add(new AutoCompleterItem(tr, new String[] { info.translatorName }, 0));
            }
        }
        return result;
    }

    @Override
    public String itemToString(AutoCompleterItem item) {
        return item.payload;
    }

    @Override
    protected boolean isEnabled() {
        return Preferences.isPreferenceDefault(Preferences.AC_MACHINETRANSLATION_ENABLED, true);
    }
}
