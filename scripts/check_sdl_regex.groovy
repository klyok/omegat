/*
 * QA script to process SDL Trados .sdlqasettings files.
 * The script loads RegExRules from the specified file and
 * applies them to the current OmegaT project.
 * Output is printed in a style similar to check_rules.groovy.
 *
 * Script name: SDL QA Regex
 */

import groovy.xml.XmlParser
import java.util.regex.Pattern
import java.util.regex.Matcher
import org.apache.commons.text.StringEscapeUtils
import groovy.swing.SwingBuilder
import groovy.beans.Bindable
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.table.*
import javax.swing.event.*
import javax.swing.RowSorter.SortKey
import javax.swing.RowSorter
import javax.swing.SortOrder
import java.awt.Component
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.*
import org.omegat.core.Core

// Default path to the .sdlqasettings file inside project root
def settingsFile = new File(project.projectProperties.projectRoot, 'QA.sdlqasettings')

class QAResultData {
    @Bindable data = []
}

class IntegerComparator implements Comparator<Integer> {
    int compare(Integer o1, Integer o2) { o1 - o2 }
}

/**
 * Parse RegEx rules from the .sdlqasettings file.
 */
def parseRules(File f) {
    def parser = new XmlParser(false, false)
    def root = parser.parse(f)
    def qaGroup = root.'SettingsGroup'.find { it.@Id == 'QAVerificationSettings' }
    def cntSetting = qaGroup.'Setting'.find { it.@Id == 'RegExRulesCount' }
    int count = cntSetting?.text()?.toInteger() ?: 0
    console.println("Loading ${count} QA rules")
    def rules = []
    (0..<count).each { idx ->
        def set = qaGroup.'Setting'.find { it.@Id == "RegExRules${idx}" }
        if (!set) return
        def rule = set.'RegExRule'[0]
        def r = [
                description : rule.'Description'.text(),
                ignoreCase  : rule.'IgnoreCase'.text().toBoolean(),
                src         : StringEscapeUtils.unescapeXml(rule.'RegExSource'.text()),
                tgt         : StringEscapeUtils.unescapeXml(rule.'RegExTarget'.text()),
                cond        : rule.'RuleCondition'.text()
        ]
        console.println("Rule ${idx + 1}: src='${r.src}', tgt='${r.tgt}', desc='${r.description}', type='${r.cond}'")
        rules << r
    }
    console.println("Total rules loaded: ${rules.size()}")
    return rules
}

/** Create regex Pattern or return null when rule part is empty. */
def makePattern(String regex, boolean ignoreCase) {
    if (!regex) return null
    def flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0
    return Pattern.compile(regex, flags)
}

/** Count matcher occurrences. */
def countMatches(Matcher m) {
    int c = 0
    while (m.find()) c++
    return c
}

/**
 * Check if a rule triggers on given source/target pair.
 */
def applyRule(rule, String source, String target) {
    def srcP = makePattern(rule.src, rule.ignoreCase)
    def tgtP = makePattern(rule.tgt, rule.ignoreCase)
    def sm = srcP ? srcP.matcher(source) : null
    def tm = tgtP ? tgtP.matcher(target) : null

    switch (rule.cond) {
        case 'TargetOnly':
            return tm && tm.find()
        case 'SourceNotTarget':
            def srcMatch = sm ? sm.find() : false
            def tgtMatch = tm ? tm.find() : false
            return srcMatch && !tgtMatch
        case 'TargetNotSource':
            return (tm && tm.find()) && !(sm && sm.find())
        case 'DifferentCount':
            def sc = sm ? countMatches(sm) : 0
            def tc = tm ? countMatches(tm) : 0
            return sc != tc
        case 'GroupedSourceNotTarget':
            if (!(sm && sm.find())) return false
            def rep = rule.tgt
            (1..sm.groupCount()).each { i ->
                rep = rep.replace("\$${i}", Matcher.quoteReplacement(sm.group(i)))
            }
            def gp = makePattern(rep, rule.ignoreCase)
            return !(gp.matcher(target).find())
        default:
            return false
    }
}

if (!settingsFile.exists()) {
    console.println("Settings file not found: ${settingsFile}")
    return
}

console.clear()
console.println(res.getString('title')+"\n${'-'*15}")

rules = parseRules(settingsFile)

model = new QAResultData()
segment_count = 0

project.projectFiles.each { fi ->
    fi.entries.each { ste ->
        def source = ste.getSrcText()
        def target = project.getTranslationInfo(ste)?.translation ?: ''
        rules.each { r ->
            if (applyRule(r, source, target)) {
                console.println("${ste.entryNum()}\t${r.description}\t[${target}]")
                model.data << [seg: ste.entryNum(), rule: r.description, source: source, target: target]
                segment_count++
            }
        }
    }
}

console.println('-----')
console.println(res.getString('errors_count') + " ${segment_count}")

def showResults(locationxy = new Point(0, 0), width = 900, height = 550, scrollpos = 0,
                 sortColumn = 1, sortOrderDescending = false) {
    swing = new SwingBuilder()
    def frame = swing.frame(title: res.getString('title') + ". " + res.getString('errors_count') + " " + segment_count,
            minimumSize: [width, height], pack: true, show: true) {
        def tab
        def skroll
        skroll = scrollPane {
            tab = table() {
                tableModel(list: model.data) {
                    propertyColumn(editable: true, header: res.getString('segment'), propertyName: 'seg',
                            minWidth: 80, maxWidth: 80, preferredWidth: 80,
                            cellEditor: new TableCellEditor() {
                                void cancelCellEditing() {}
                                boolean stopCellEditing() { false }
                                Object getCellEditorValue() { value }
                                boolean isCellEditable(EventObject e) { true }
                                boolean shouldSelectCell(EventObject e) { true }
                                void addCellEditorListener(CellEditorListener l) {}
                                void removeCellEditorListener(CellEditorListener l) {}
                                Component getTableCellEditorComponent(JTable t, Object value, boolean isSelected, int row, int column) {
                                    Core.getEditor().gotoEntry(value)
                                }
                            },
                            cellRenderer: new TableCellRenderer() {
                                Component getTableCellRendererComponent(JTable table, Object value,
                                                                          boolean isSelected, boolean hasFocus,
                                                                          int row, int column) {
                                    def btn = new JButton()
                                    btn.setText(value.toString())
                                    return btn
                                }
                            }
                    )
                    propertyColumn(editable: false, header: res.getString('rule'), propertyName: 'rule',
                            minWidth: 120, preferredWidth: 180)
                    propertyColumn(editable: false, header: res.getString('target'), propertyName: 'target',
                            minWidth: 200, preferredWidth: 320)
                    propertyColumn(editable: false, header: res.getString('source'), propertyName: 'source',
                            minWidth: 200, preferredWidth: 320)
                }
            }
            tab.getTableHeader().setReorderingAllowed(false)
        }
        rowSorter = new TableRowSorter(tab.model)
        rowSorter.setComparator(0, new IntegerComparator())
        sortKeyz = new ArrayList<RowSorter.SortKey>()
        sortKeyz.add(new RowSorter.SortKey(sortColumn, sortOrderDescending ? SortOrder.DESCENDING : SortOrder.ASCENDING))
        rowSorter.setSortKeys(sortKeyz)
        tab.setRowSorter(rowSorter)

        skroll.getVerticalScrollBar().setValue(scrollpos)
        tab.scrollRectToVisible(new Rectangle(0, scrollpos, 1, scrollpos + 1))
        skroll.repaint()
    }
    frame.setLocation(locationxy)
}

showResults()
