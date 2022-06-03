package org.grobid.core.meta;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.data.*;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.Pair;
import org.grobid.core.utilities.TextUtilities;
import scala.Option;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class MetaVO {
    private boolean isConsolidated;
    private String title_ko;
    private String title_en;

    private String abstract_ko;
    private String abstract_en;
    
    private List<AuthorVO> authors;

//    private Set<String> authors_ko;
//    private Set<String> authors_en;

//    private List<Affiliation> affiliation_ko;
//    private List<Affiliation> affiliation_en;

    private List<String> keywords;

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

    private static List<Pair<String, String>> lastNamePairs;
    private List<LayoutToken> authorTokens = new ArrayList<>();





    public MetaVO() {
        isConsolidated = false;
        detector = LanguageDetectorBuilder.fromLanguages(Language.KOREAN, Language.ENGLISH).build();
        this.keywords = new ArrayList<>();
//        this.affiliation_ko = new ArrayList<>();
//        this.affiliation_en = new ArrayList<>();
//        this.authors_ko = new LinkedHashSet<>();
//        this.authors_en = new LinkedHashSet<>();
        this.authors = new ArrayList<>();
        this.emails = new LinkedHashSet<>();
        this.koreanNamePattern = Pattern.compile("[가-힣]\\s?[가-힣]\\s?[가-힣]?[가-힣]?");
        this.englishNamePattern = Pattern.compile("[a-zA-Z]+([',. -][a-zA-Z ])?(['. -]?[a-zA-Z ]+)+");
        lastNamePairs = new ArrayList<>();
        lastNamePairs.add(new Pair<>("jo", "cho"));
        lastNamePairs.add(new Pair<>("seong", "sung"));
        lastNamePairs.add(new Pair<>("gu", "koo"));
        lastNamePairs.add(new Pair<>("gi", "kee"));
        lastNamePairs.add(new Pair<>("jung", "jeong"));
        lastNamePairs.add(new Pair<>("sin", "shin"));
    }

    public Language detect(String text){
        return detector.detectLanguageOf(text);
    }

    public void setTitle(String title){
        if (title.contains("//lang//")) {
            String[] titles = title.split("//lang//");
            for (String t : titles) {
//                Language lang = detect(t);
//                if (lang.name().equals("ENGLISH")){
//                    this.title_en = t;
//                }else{
//                    this.title_ko = t;
//                }
                if (t.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
                    this.title_ko = t;
                } else{
                    this.title_en = t;
                }
            }
        }else{
//            Language lang = detect(title);
//            if (lang.name().equals("ENGLISH")){
//                this.title_en = title;
//            }else{
//                this.title_ko = title;
//            }
            if (title.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
                this.title_ko = title;
            } else{
                this.title_en = title;
            }
        }
    }

    public void setAbstract(String abstract_){
        String[] split = abstract_.split("//lang//");
        for (String abstract__ : split) {
            Language lang = detect(abstract__);
            if (lang.name().equals("ENGLISH")) {
                this.abstract_en = (this.abstract_en == null ? "" : this.abstract_en + " ") + abstract__;
            }else{
                this.abstract_ko = (this.abstract_ko == null ? "" : this.abstract_ko + " ") + abstract__;
            }

        }
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<Keyword> keywords) {
        for (Keyword keyword : keywords) {
            String k = keyword.getKeyword();
            if (k != null && !k.equals("")) {
                this.keywords.add(k);
            }
        }
    }

    //    public void setKeyword(List<Keyword> keywords) {
//        for (Keyword keyword : keywords) {
//            String k = keyword.getKeyword().trim();
//
//            if (k == null || k.equals("") || k.equals(" ") || k.equals(",") || k.equals("|") || k.equals(",,")) {
//                continue;
//            }
//            if(k.contains("|")){
//                String[] split = k.split("\\|");
//                for (String s : split) {
//                    if (s == null || s.equals("") || s.equals(" ")) {
//                        continue;
//                    }
//                    setKeywordLanguageDetector(s);
//                }
//            }else{
//                setKeywordLanguageDetector(k);
//            }
//
//        }
//    }
//
//    private void setKeywordLanguageDetector(String k) {
//        Language lang = detect(k);
//        if (lang.name().equals("ENGLISH")) {
//            if(this.keywords_en.contains(k)){
//                this.keywords_ko.add(k);
//            }else{
//                this.keywords_en.add(k);
//            }
//        }else{
//            this.keywords_ko.add(k);
//        }
//    }

    public void setAuthor(List<Person> authors) throws EncoderException {
        HashMap<Integer, String> indexKorName = new HashMap<>();
        int engOrder = 1;
        int korOrder = 1;
        for (Person a : authors) {
            String lang = a.getLang() == null ? "en" : a.getLang();
            a.setLang(lang);
            if(lang.equals("kr")){
                a.setOrder(korOrder++);
//                this.authors.add(new AuthorVO(a));
            }
            else {
                a.setOrder(engOrder++);
//                this.authors.add(new AuthorVO(a));
            }
        }
        if (engOrder == korOrder) {
            a:
            for (Person a : authors){
                if(a.isMatched())
                    continue a;

                b:
                for (Person b : authors) {
                    if (b.isMatched() || a == b) {
                        continue b;
                    }
                    if(a.getOrder() == b.getOrder()){
                        a.setMatched(true);
                        b.setMatched(true);
                        if (a.getLang().equals("kr"))
                            this.authors.add(new AuthorVO(new Pair<>(a,b)));
                        else
                            this.authors.add(new AuthorVO(new Pair<>(b, a)));
                        continue a;
                    }
                }
            }
        }else {
            for (Person a : authors) {
                this.authors.add(new AuthorVO(a));
            }
        }
        
//        for (Person a : authors) {
//            if(a.getOrder() == -1)
//                a.setOrder(order++);
//            String lang = a.getLang() == null ? "en" : a.getLang();
//            if (lang.equals("kr")) {
//                if (a.getFirstName() == null && a.getLastName().length() == 2) {
//                    String lastName = a.getLastName();
//                    String resultLastName = lastName.substring(0, 1);
//                    String resultFirstName = lastName.substring(1);
//                    a.setFirstName(resultFirstName);
//                    a.setLastName(resultLastName);
//                } else if (a.getLastName() == null && a.getFirstName().length() == 2) {
//                    String firstName = a.getFirstName();
//                    String resultFirstName = firstName.substring(1);
//                    String resultLastName = firstName.substring(0, 1);
//                    a.setFirstName(resultFirstName);
//                    a.setLastName(resultLastName);
//                }
//            }
//        }
//        for (int i = 0; i < authors.size(); i++) {
//            String lang = authors.get(i).getLang() == null ? "en" : authors.get(i).getLang();
//            if(lang.equals("kr")){
//                StringBuilder sb = new StringBuilder();
//                sb.append(authors.get(i).getFirstName() == null ? "" : authors.get(i).getFirstName());
//                sb.append(" ");
//                sb.append(authors.get(i).getLastName() == null ? "" : authors.get(i).getLastName());
//                String engName = Kor2EngName(sb.toString());
//                indexKorName.put(i, engName);
//            }
//        }
//
//        double range = 0.875;
//        int soundexRange = 4;
//
//        Set<Integer> removeList = new HashSet<>();
//        int n = process(indexKorName, authors, removeList, range, soundexRange, lastNamePairs);
//
//        for (Integer r : removeList) {
//            indexKorName.remove(r);
//        }
//        removeList.clear();
//
//        soundexRange = 4;
//        range = 0.65;
//        int n2 = process(indexKorName, authors, removeList, range, soundexRange, lastNamePairs);
//
//        for (Integer r : removeList) {
//            indexKorName.remove(r);
//        }
//
//        removeList.clear();
//
//        soundexRange = 3;
//        range = 0.4;
//        int n3 = process(indexKorName, authors, removeList, range, soundexRange, lastNamePairs);
//
//        for (Integer r : removeList) {
//            indexKorName.remove(r);
//        }
//
//        removeList.clear();
//
//        soundexRange = 2;
//        range = 0.4;
//        int n4 = process(indexKorName, authors, removeList, range, soundexRange, lastNamePairs);
//
//        for (Integer r : removeList) {
//            indexKorName.remove(r);
//        }
//
//        // 영문저자만 있는 것
//        if (n == 0 && n2 == 0 && n3 == 0 && n4 == 0 && authors.size() != 0) {
//            for (Person author : authors) {
//                this.authors.add(new AuthorVO(author));
//            }
//        }
//        if (this.authors.size() == 1) {
//            this.authors.get(0).setCorresp(true);
//        }
        
        Collections.sort(this.authors);

    }

    private static String clear(String s, String delimiters) {
        char[] chars = s.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char a : chars) {
            if (delimiters.indexOf(a) == -1) {
                sb.append(a);
            }
        }
        return sb.toString();
    }

    private int process(HashMap<Integer, String> indexKorName, List<Person> authors, Set<Integer> removeList, double range, int soundexRange, List<Pair<String, String>> lastNamePairs) throws EncoderException {
        int n = 0;
        Soundex soundex = new Soundex();
        aut:
        for (Person author : authors) {
            if (author.isMatched()) {
                continue;
            }
            String lang = author.getLang() == null ? "en" : author.getLang();
            if (lang.equals("en")) {
                n++;
                StringBuilder sb = new StringBuilder();
                String firstName = author.getFirstName() == null ? "" : author.getFirstName().toLowerCase().replaceAll("-", "");
                String middleName = author.getMiddleName() == null ? "" : author.getMiddleName().toLowerCase().replaceAll("-", "");
                String lastName = author.getLastName() == null ? "" : author.getLastName().toLowerCase().replaceAll("-", "");
//                String fName = TextUtilities.capitalizeFully(firstName, TextUtilities.fullPunctuations);
//                String mName = TextUtilities.capitalizeFully(middleName, TextUtilities.fullPunctuations);
//                String lName = TextUtilities.capitalizeFully(lastName, TextUtilities.fullPunctuations);
                String fName = clear(firstName, TextUtilities.fullPunctuations);
                String mName = clear(middleName, TextUtilities.fullPunctuations);
                String lName = clear(lastName, TextUtilities.fullPunctuations);
                if(!middleName.equals("")){
                    sb.append(fName);
                    sb.append(mName);
                    sb.append(" ");
                } else{
                    sb.append(fName.replaceAll(" ", ""));
                    sb.append(" ");
                }
                sb.append(lName);
                String engName = sb.toString();
                String[] engNameSplit = engName.split(" ");
                if (indexKorName.isEmpty()) {
                    author.setMatched(true);
                    this.authors.add(new AuthorVO(author));
                }else{
                    compare:
                    for (Map.Entry<Integer, String> integerStringEntry : indexKorName.entrySet()) {
                        String korName = integerStringEntry.getValue();
                        boolean result = true;
                        String[] korNameSplit = korName.split(" ");
                        compareEach:
                        for (int i = 0; i < engNameSplit.length; i++) {
                            if (engNameSplit[i].matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
                                continue;
                            }
                            int difference = soundex.difference(engNameSplit[i], korNameSplit[i]);
                            double v = ratcliffObershelpDistance(engNameSplit[i], korNameSplit[i], false);
                            if(isLastNameMatch(engNameSplit[i], korNameSplit[i], lastNamePairs)){
                                difference = 4;
                                v = 1.0;
                            }
                            if(difference == 4 && v >= 0.9){
                                continue compareEach;
                            }
                            if (difference < soundexRange || v < range) {
                                result = false;
                                continue compare;
                            }
                        }

                        if (result) {
                            removeList.add(integerStringEntry.getKey());
                            authors.get(integerStringEntry.getKey()).setMatched(true);
                            author.setMatched(true);
                            Pair<Person, Person> matchedAuthor = new Pair<>(authors.get(integerStringEntry.getKey()), author);
                            this.authors.add(new AuthorVO(matchedAuthor));
                            continue aut;
                        }
                    }
                }
            }
        }
        return n;
    }

    private boolean isLastNameMatch(String a, String b, List<Pair<String, String>> pairList){
        boolean result = false;
        a = a.toLowerCase();
        b = b.toLowerCase();
        for (Pair<String, String> p : pairList) {
            if ((p.getA().equals(a) && p.getB().equals(b)) || (p.getA().equals(b) && p.getB().equals(a))) {
                result = true;
            }
        }
        return result;
    }
        
        

//        int n = 0;
//        Soundex soundex = new Soundex();
//        aut:
//        for (Person author : authors) {
//            if (author.getLang().equals("en")) {
//                n++;
//                StringBuilder sb = new StringBuilder();
//                String firstName = author.getFirstName() == null ? "" : author.getFirstName().toLowerCase().replaceAll("-", "");
//                String middleName = author.getMiddleName() == null ? "" : author.getMiddleName().toLowerCase().replaceAll("-", "");
//                String lastName = author.getLastName() == null ? "" : author.getLastName().toLowerCase().replaceAll("-", "");
//                String fName = TextUtilities.capitalizeFully(firstName, TextUtilities.fullPunctuations);
//                String mName = TextUtilities.capitalizeFully(middleName, TextUtilities.fullPunctuations);
//                String lName = TextUtilities.capitalizeFully(lastName, TextUtilities.fullPunctuations);
//                if(!middleName.equals("")){
//                    sb.append(fName);
//                    sb.append(mName);
//                    sb.append(" ");
//                } else{
//                    sb.append(fName.replaceAll(" ", ""));
//                    sb.append(" ");
//                }
//                sb.append(lName);
//                String engName = sb.toString();
//                String[] engNameSplit = engName.split(" ");
//                if (indexKorName.isEmpty()) {
//                    this.authors.add(new AuthorVO(author));
//                }else{
//                    compare:
//                    for (Map.Entry<Integer, String> integerStringEntry : indexKorName.entrySet()) {
//                        String korName = integerStringEntry.getValue();
//                        boolean result = true;
//                        String[] korNameSplit = korName.split(" ");
//                        for (int i = 0; i < engNameSplit.length; i++) {
//                            int difference = soundex.difference(engNameSplit[i], korNameSplit[i]);
//                            double v = ratcliffObershelpDistance(engNameSplit[i], korNameSplit[i], false);
//                            if(difference == 4 || v >= 0.9){
//                                continue;
//                            }
//                            if (difference < 3 || v < 0.4) {
//                                result = false;
//                                continue compare;
//                            }
//                        }
//                        
//                        if (result) {
//                            Pair<Person, Person> matchedAuthor = new Pair<>(authors.get(integerStringEntry.getKey()), author);
//                            this.authors.add(new AuthorVO(matchedAuthor));
//                            continue aut;
//                        }
//                    }
//                }
//            }
//        }
//        if (n == 0 && authors.size() != 0) {
//            for (Person author : authors) {
//                this.authors.add(new AuthorVO(author));
//            }
//        }

        


//    }

    public void setEmail(String emails){
        if(emails != null && emails != ""){
            String[] emailList = emails.split("\t");
            for (String s : emailList) {
                this.emails.add(s.replaceAll("\n", "").trim());
            }
        }
    }

//    public void setAffiliation(List<Affiliation> affiliations) {
//        for (Affiliation aff : affiliations) {
//            String rawText = aff.getRawAffiliationString();
//            Language lang = detect(rawText);
//            if (lang.name().equals("ENGLISH")) {
//                affiliation_en.add(aff);
//            }else{
//                affiliation_ko.add(aff);
//            }
//
//        }
//    }
//
//    public List<Affiliation> getAffiliation_ko() {
//        return affiliation_ko;
//    }
//
//    public void setAffiliation_ko(List<Affiliation> affiliation_ko) {
//        this.affiliation_ko = affiliation_ko;
//    }
//
//    public List<Affiliation> getAffiliation_en() {
//        return affiliation_en;
//    }
//
//    public void setAffiliation_en(List<Affiliation> affiliation_en) {
//        this.affiliation_en = affiliation_en;
//    }


    public boolean isConsolidated() {
        return isConsolidated;
    }

    public void setConsolidated(boolean consolidated) {
        isConsolidated = consolidated;
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

//    public Set<String> getAuthors_ko() {
//        return authors_ko;
//    }
//
//    public void setAuthors_ko(Set<String> authors_ko) {
//        this.authors_ko = authors_ko;
//    }

//    public Set<String> getKeywords_ko() {
//        return keywords_ko;
//    }
//
//    public void setKeywords_ko(Set<String> keywords_ko) {
//        this.keywords_ko = keywords_ko;
//    }

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

//    public Set<String> getAuthors_en() {
//        return authors_en;
//    }
//
//    public void setAuthors_en(Set<String> authors_en) {
//        this.authors_en = authors_en;
//    }

//    public Set<String> getKeywords_en() {
//        return keywords_en;
//    }
//
//    public void setKeywords_en(Set<String> keywords_en) {
//        this.keywords_en = keywords_en;
//    }

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

    public List<AuthorVO> getAuthors() {
        return authors;
    }

    public void setAuthors(List<AuthorVO> authors) {
        this.authors = authors;
    }

//    public List<LayoutToken> getAuthorTokens() {
//        return authorTokens;
//    }
//
//    public void setAuthorTokens(List<LayoutToken> authorTokens) {
//        this.authorTokens = authorTokens;
//    }

    public static String Kor2EngName(String name){
        StringBuilder stringBuilder = new StringBuilder();
        char[] chars = name.toCharArray();
        Pattern compile = Pattern.compile(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");

        for (int i = 0; i < chars.length; i++) {
            Matcher matcher = compile.matcher(String.valueOf(chars[i]));
            if (chars[i] == ' ') {
                stringBuilder.append(" ");
            }
            if (matcher.matches()) {
                if (chars[i] == '이' && i == chars.length - 1) {
                    stringBuilder.append("lee");
                    stringBuilder.append(" ");
                    continue;
                } else if (chars[i] == '최'){
                    stringBuilder.append("choi");
                    stringBuilder.append(" ");
                    continue;
                } else if (chars[i] == '유' && i == chars.length - 1) {
                    stringBuilder.append("yoo");
                    stringBuilder.append(" ");
                    continue;
                } else if (chars[i] == '정' && i == chars.length - 1) {
                    stringBuilder.append("jung");
                    stringBuilder.append(" ");
                    continue;
                } else if (chars[i] == '운') {
                    stringBuilder.append("woon");
                    continue;
                } else if (chars[i] == '연') {
                    stringBuilder.append("yeon");
                    continue;
                } else if (chars[i] == '윤') {
                    stringBuilder.append("yoon");
                    continue;
                } else if (chars[i] == '은') {
                    stringBuilder.append("eun");
                    continue;
                } else if (chars[i] == '근') {
                    stringBuilder.append("geun");
                    continue;
                } else if (chars[i] == '학') {
                    stringBuilder.append("hak");
                    continue;
                }
                String engFirstElement = getENGFirstElement(chars[i]);
                String engMiddleElement = getENGMiddleElement(chars[i]);
                String engLastElement = getENGLastElement(chars[i]);
                
                if (i == chars.length - 1 && chars[i] == '박') {
                    stringBuilder.append("p");
                } else if (engFirstElement.equals("") && (engMiddleElement.equals("oo"))) {
                    stringBuilder.append("w");
                } else {
                    stringBuilder.append(engFirstElement);
                }
                
                if (engLastElement.equals("") || engLastElement.equals("ng") || engLastElement.equals("m") || engLastElement.equals("n")) {
                    stringBuilder.append(engMiddleElement.replaceAll("ar", "a"));
                    stringBuilder.append(engLastElement);
                } else {
                    stringBuilder.append(engMiddleElement);
                    stringBuilder.append(engLastElement);
                }
            }
        }
        return stringBuilder.toString();
    }

    /* 초성, 중성, 종성 */
    private static final char[] firstSounds = {'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'};
    private static final char[] middleSounds = {'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'};
    private static final char[] lastSounds = {' ', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'};

    /* 초성, 중성, 종성 에 대한 영어 매핑 */
    private static final String[] efirstSounds = {"k", "kk", "n", "d", "dd", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h"};
    private static final String[] emiddleSounds = {"ar", "ae", "ya", "ye", "eo", "ae", "yu", "ye", "o", "wa", "whe", "whe", "yo", "oo", "wo", "whe", "we", "yu", "ue", "ee", "i"};
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
