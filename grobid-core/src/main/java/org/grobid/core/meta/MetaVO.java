package org.grobid.core.meta;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.data.*;
import org.grobid.core.utilities.TextUtilities;
import scala.Option;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class MetaVO {
    private String title_ko;
    private String title_en;

    private String abstract_ko;
    private String abstract_en;

    private Set<String> authors_ko;
    private Set<String> authors_en;

    private List<Affiliation> affiliation_ko;
    private List<Affiliation> affiliation_en;

    private Set<String> keywords_ko;
    private Set<String> keywords_en;

    private Set<String> emails;
    private String submission;
    private String doi;

    private String reference;
    private String copyright;

    private List<Figure> figures;
    private List<Table> tables;
    private String assetPath;


    private List<BiblVO> biblList;
    private LanguageDetector detector;

    private Pattern koreanNamePattern;
    private Pattern englishNamePattern;

    private String key;


    public MetaVO() {
        detector = LanguageDetectorBuilder.fromLanguages(Language.KOREAN, Language.ENGLISH).build();
        this.keywords_ko = new LinkedHashSet<>();
        this.keywords_en = new LinkedHashSet<>();
        this.affiliation_ko = new ArrayList<>();
        this.affiliation_en = new ArrayList<>();
        this.authors_ko = new LinkedHashSet<>();
        this.authors_en = new LinkedHashSet<>();
        this.emails = new LinkedHashSet<>();
        this.koreanNamePattern = Pattern.compile("[가-힣]\\s?[가-힣]\\s?[가-힣]?[가-힣]?");
        this.englishNamePattern = Pattern.compile("[a-zA-Z]+([',. -][a-zA-Z ])?(['. -]?[a-zA-Z ]+)+");
    }

    public Language detect(String text){
        return detector.detectLanguageOf(text);
    }

    public void setTitle(String title){
        if (title.contains("//lang//")) {
            String[] titles = title.split("//lang//");
            for (String t : titles) {
                Language lang = detect(t);
                if (lang.name().equals("ENGLISH")){
                    this.title_en = t;
                }else{
                    this.title_ko = t;
                }
            }
        }else{
//            Language lang = detect(title);
//            if (lang.name().equals("ENGLISH")){
//                this.title_en = title;
//            }else{
//                this.title_ko = title;
//            }
            this.title_ko = title;
        }
    }

    public void setAbstract(String abstract_){
        String[] split = abstract_.split(" //lang// ");
        for (String abstract__ : split) {
            Language lang = detect(abstract__);
            if (lang.name().equals("ENGLISH")) {
                this.abstract_en = (this.abstract_en == null ? "" : this.abstract_en + " ") + abstract__;
            }else{
                this.abstract_ko = (this.abstract_ko == null ? "" : this.abstract_ko + " ") + abstract__;
            }

        }
    }

    public void setKeyword(List<Keyword> keywords) {
        for (Keyword keyword : keywords) {
            String k = keyword.getKeyword().trim();

            if (k == null || k.equals("") || k.equals(" ") || k.equals(",") || k.equals("|") || k.equals(",,")) {
                continue;
            }
            if(k.contains("|")){
                String[] split = k.split("\\|");
                for (String s : split) {
                    if (s == null || s.equals("") || s.equals(" ")) {
                        continue;
                    }
                    setKeywordLanguageDetector(s);
                }
            }else{
                setKeywordLanguageDetector(k);
            }

        }
    }

    private void setKeywordLanguageDetector(String k) {
        Language lang = detect(k);
        if (lang.name().equals("ENGLISH")) {
            if(this.keywords_en.contains(k)){
                this.keywords_ko.add(k);
            }else{
                this.keywords_en.add(k);
            }
        }else{
            this.keywords_ko.add(k);
        }
    }

    public void setAuthor(List<Person> authors) throws EncoderException {
//        authors = authors.replace("and", "");
//        authors = authors.replace("And", "");
//        authors = authors.replace("by", "");
//        authors = authors.replace("By", "");
//
//        Matcher enMatcher = englishNamePattern.matcher(authors);
//        Matcher koMatcher = koreanNamePattern.matcher(authors);
//        while(enMatcher.find()){
//            this.authors_en.add(enMatcher.group().trim());
//        }
//        while(koMatcher.find()){
//            this.authors_ko.add(koMatcher.group().replaceAll(" ", "").trim());
//        }
        HashMap<Integer, String> indexKorName = new HashMap<>();
        for (int i = 0; i < authors.size(); i++) {
            if(authors.get(i).getLang().equals("kr")){
                StringBuilder sb = new StringBuilder();
                sb.append(authors.get(i).getFirstName() == null ? "" : authors.get(i).getFirstName());
                sb.append(authors.get(i).getLastName() == null ? "" : authors.get(i).getLastName());
                String engName = Kor2EngName(sb.toString());
                indexKorName.put(i, engName);
            }
        }

        int n = 0;
        Soundex soundex = new Soundex();
        for (Person author : authors) {
            if (author.getLang().equals("en")) {
                n++;
                author.setOrder(n);
                StringBuilder sb = new StringBuilder();
                String firstName = author.getFirstName() == null ? "" : author.getFirstName().toLowerCase();
                String lastName = author.getLastName() == null ? "" : author.getLastName().toLowerCase();
                sb.append(firstName.replaceAll("-", " "));
                sb.append(" ");
                sb.append(lastName.replaceAll("-", " "));
                for (Map.Entry<Integer, String> integerStringEntry : indexKorName.entrySet()) {
                    if(soundex.difference(sb.toString(), integerStringEntry.getValue()) >= 3){
                        if (ratcliffObershelpDistance(sb.toString(), integerStringEntry.getValue(), false) >= 0.7) {
                            authors.get(integerStringEntry.getKey()).setOrder(n);
                        }
                    }else{
                        if (ratcliffObershelpDistance(sb.toString(), integerStringEntry.getValue(), false) >= 0.8) {
                            authors.get(integerStringEntry.getKey()).setOrder(n);
                        }
                    }
                }
            }
        }
    }

    public void setEmail(String emails){
        if(emails != null && emails != ""){
            String[] emailList = emails.split("\t");
            for (String s : emailList) {
                this.emails.add(s.replaceAll("\n", "").trim());
            }
        }
    }

    public void setAffiliation(List<Affiliation> affiliations) {
        for (Affiliation aff : affiliations) {
            String rawText = aff.getRawAffiliationString();
            Language lang = detect(rawText);
            if (lang.name().equals("ENGLISH")) {
                affiliation_en.add(aff);
            }else{
                affiliation_ko.add(aff);
            }

        }
    }

    public List<Affiliation> getAffiliation_ko() {
        return affiliation_ko;
    }

    public void setAffiliation_ko(List<Affiliation> affiliation_ko) {
        this.affiliation_ko = affiliation_ko;
    }

    public List<Affiliation> getAffiliation_en() {
        return affiliation_en;
    }

    public void setAffiliation_en(List<Affiliation> affiliation_en) {
        this.affiliation_en = affiliation_en;
    }

    public String getTitle_ko() {
        return title_ko;
    }

    public void setTitle_ko(String title_ko) {
        this.title_ko = title_ko;
    }

    public String getAbstract_ko() {
        return abstract_ko;
    }

    public void setAbstract_ko(String abstract_ko) {
        this.abstract_ko = abstract_ko;
    }

    public Set<String> getAuthors_ko() {
        return authors_ko;
    }

    public void setAuthors_ko(Set<String> authors_ko) {
        this.authors_ko = authors_ko;
    }

    public Set<String> getKeywords_ko() {
        return keywords_ko;
    }

    public void setKeywords_ko(Set<String> keywords_ko) {
        this.keywords_ko = keywords_ko;
    }

    public String getTitle_en() {
        return title_en;
    }

    public void setTitle_en(String title_en) {
        this.title_en = title_en;
    }

    public String getAbstract_en() {
        return abstract_en;
    }

    public void setAbstract_en(String abstract_en) {
        this.abstract_en = abstract_en;
    }

    public Set<String> getAuthors_en() {
        return authors_en;
    }

    public void setAuthors_en(Set<String> authors_en) {
        this.authors_en = authors_en;
    }

    public Set<String> getKeywords_en() {
        return keywords_en;
    }

    public void setKeywords_en(Set<String> keywords_en) {
        this.keywords_en = keywords_en;
    }

    public String getSubmission() {
        return submission;
    }

    public void setSubmission(String submission) {
        this.submission = submission;
    }

    public Set<String> getEmails() {
        return emails;
    }

    public void setEmails(Set<String> emails) {
        this.emails = emails;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public List<BiblVO> getBiblList() {
        return biblList;
    }

    public void setBiblList(List<BiblVO> biblList) {
        this.biblList = biblList;
    }

    public List<Figure> getFigures() {
        return figures;
    }

    public void setFigures(List<Figure> figures) {
        this.figures = figures;
    }

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public void setAssetPath(String assetPath) {
        this.assetPath = assetPath;
    }

    public static String Kor2EngName(String name){
        StringBuilder stringBuilder = new StringBuilder();
        char[] chars = name.toCharArray();
        Pattern compile = Pattern.compile(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");

        for (char aChar : chars) {
            Matcher matcher = compile.matcher(String.valueOf(aChar));
            if (matcher.matches()) {
                stringBuilder.append(getENGFirstElement(aChar));
                stringBuilder.append(getENGMiddleElement(aChar));
                stringBuilder.append(getENGLastElement(aChar));
                stringBuilder.append(" ");
            }
        }
        return stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
    }

    /* 초성, 중성, 종성 */
    private static final char[] firstSounds = {'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'};
    private static final char[] middleSounds = {'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'};
    private static final char[] lastSounds = {' ', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'};

    /* 초성, 중성, 종성 에 대한 영어 매핑 */
    private static final String[] efirstSounds = {"k", "kk", "n", "d", "dd", "r", "m", "p", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h"};
    private static final String[] emiddleSounds = {"ar", "ae", "ya", "ye", "u", "ae", "ye", "ye", "o", "wha", "whe", "whe", "yo", "wo", "wo", "whe", "we", "yu", "ue", "ae", "i"};
    private static final String[] elastSounds = {"", "k", "uk", "None", "n", "n", "n", "None", "l", "l", "None", "None", "None", "None", "None", "None", "m", "b", "p", "s", "ss", "ng", "t", "c", "c", "t", "p", "h"};
    /* 여기서 영어매핑 알파벳단어가 잘 안맞다면 적절히 바꿔서 사용하세요 */



    /* 한글 초성에 대한 영어 알파벳 반환 */
    public static String getENGFirstElement(char c) {
        return efirstSounds[(c - 0xAC00) / (21 * 28)];
    }
    public static String getENGFirstElement(String str) {
        if (str == null) return "\u0000";

        str = str.trim();
        int len = str.length();
        if (len == 0) return "\u0000";

        return getENGFirstElement(str.charAt(0));
    }
    /* 한글중성에 대한 영어 알파벳 반환 */
    public static String getENGMiddleElement(char c) {
        return emiddleSounds[((c-0xAC00) % (21 * 28)) / 28];
    }
    public static String getENGMiddleElement(String str) {
        if (str == null) return "\u0000";

        str = str.trim();
        int len = str.length();
        if (len == 0) return "\u0000";

        return getENGMiddleElement(str.charAt(0));
    }
    /* 한글 종성에 대한 영어 알파벳 반환 */
    public static String getENGLastElement(char c) {
        return elastSounds[(c - 0xAC00) % 28];
    }
    public static String getENGLastElement(String str) {
        if (str == null) return "\u0000";

        str = str.trim();
        int len = str.length();
        if (len == 0) return "\u0000";

        return getENGLastElement(str.charAt(0));
    }

    private double ratcliffObershelpDistance(String string1, String string2, boolean caseDependent) {
        if ( StringUtils.isBlank(string1) || StringUtils.isBlank(string2) )
            return 0.0;
        Double similarity = 0.0;
        if (!caseDependent) {
            string1 = string1.toLowerCase();
            string2 = string2.toLowerCase();
        }
        if (string1.equals(string2)) {
            similarity = 1.0;
        }

        if ( isNotEmpty(string1) && isNotEmpty(string2) ) {
            Option<Object> similarityObject =
                RatcliffObershelpMetric.compare(string1, string2);
            if (similarityObject.isDefined()) {
                similarity = (Double) similarityObject.get();
            }
        }

        return similarity;
    }
}
