package org.grobid.core.meta;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import org.grobid.core.data.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public void setAuthor(String authors){
        authors = authors.replace("and", "");
        authors = authors.replace("And", "");
        authors = authors.replace("by", "");
        authors = authors.replace("By", "");

        Matcher enMatcher = englishNamePattern.matcher(authors);
        Matcher koMatcher = koreanNamePattern.matcher(authors);
        while(enMatcher.find()){
            this.authors_en.add(enMatcher.group().trim());
        }
        while(koMatcher.find()){
            this.authors_ko.add(koMatcher.group().replaceAll(" ", "").trim());
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
}
