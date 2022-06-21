package org.grobid.core.meta;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import org.grobid.core.data.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BiblVO {
    private int id;
    private String title;
    private String report;
    private String publisher;
    private String doi;
    private String orcid;
    private List<String> authors;
    private String editor;
    private String pageRange;
    private String journalTitle;
    private String issue;
    private String volume;
    private String pubYear;
    private String pubPlace;
    private String institution;
    private String url;
    private String rawText;

    private Pattern koreanNamePattern;
    private Pattern englishNamePattern;
    private LanguageDetector detector;


    public BiblVO() {
        detector = LanguageDetectorBuilder.fromLanguages(Language.KOREAN, Language.ENGLISH).build();
        this.authors = new ArrayList<>();
        this.koreanNamePattern = Pattern.compile("[가-힣]\\s?[가-힣]\\s?[가-힣]?[가-힣]?");
//        this.englishNamePattern = Pattern.compile("[a-zA-Z]+(([',. -][a-zA-Z ])?[a-zA-Z]*){1,2}");
        this.englishNamePattern = Pattern.compile("[a-zA-Z]+([',. -][a-zA-Z ])?(['. -]?[a-zA-Z ]+)+");

    }

    public Language detect(String text){
        return detector.detectLanguageOf(text);
    }

    public void setAuthors(String authors) {
        authors = authors.replace("and", "");
        authors = authors.replace("by", "");

        Matcher enMatcher = englishNamePattern.matcher(authors);
        Matcher koMatcher = koreanNamePattern.matcher(authors);
        while(enMatcher.find()){
            this.authors.add(enMatcher.group().trim());
        }
        while(koMatcher.find()){
            this.authors.add(koMatcher.group().trim());
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public List<String> getAuthors() {
        return authors;
    }

//    public void setAuthors(List<String> authors) {
//        this.authors = authors;
//    }


    public String getEditor() {
        return editor;
    }

    public void setEditor(String editor) {
        this.editor = editor;
    }

    public String getPageRange() {
        return pageRange;
    }

    public void setPageRange(String pageRange) {
        if (pageRange != null) {
            this.pageRange = pageRange.replaceAll("--", "-");
        }
    }

    public String getJournalTitle() {
        return journalTitle;
    }

    public void setJournalTitle(String journalTitle) {
        this.journalTitle = journalTitle;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }
    public String getPubYear() {
        return pubYear;
    }

    public void setPubYear(String pubYear) {
        this.pubYear = pubYear;
    }

    public String getPubPlace() {
        return pubPlace;
    }

    public void setPubPlace(String pubPlace) {
        this.pubPlace = pubPlace;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }



}
