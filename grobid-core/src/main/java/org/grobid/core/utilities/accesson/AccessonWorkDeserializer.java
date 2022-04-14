package org.grobid.core.utilities.accesson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.grobid.core.data.Affiliation;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Keyword;
import org.grobid.core.data.Person;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.crossref.CrossrefDeserializer;
import org.grobid.core.utilities.crossref.WorkDeserializer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

public class AccessonWorkDeserializer extends WorkDeserializer {

    @Override
    protected BiblioItem deserializeOneItem(JsonNode item_) {
        JsonNode item = item_.get("koarArticle");
        BiblioItem biblio = null;

        if (item == null) {
            return biblio;
        }

        if (item.isObject()) {
            biblio = new BiblioItem();

            if (isVal(item.get("doi"))) {
                biblio.setDOI(item.get("doi").asText());
            } else{
                return biblio;
            }

            JsonNode firstTitles = item.get("firstTitle");
            if (isVal(firstTitles))
                biblio.setTitle(firstTitles.asText());

            JsonNode secondTitles = item.get("secondTitle");
            if (isVal(secondTitles)) {
                if (biblio.getTitle() != null && !biblio.getTitle().equals("")) {
                    biblio.setTitle(biblio.getTitle() + "//lang//" + secondTitles.asText());
                } else {
                    biblio.setTitle(secondTitles.asText());
                }
            }

            JsonNode firstAbstract = item.get("firstAbstr");
            if (isVal(firstAbstract)) {
                biblio.setAbstract(firstAbstract.asText());
            }
            JsonNode secondAbstract = item.get("secondAbstr");
            if (isVal(secondAbstract)) {
                if (biblio.getAbstract() != null && !biblio.getAbstract().equals("")) {
                    biblio.setAbstract(biblio.getAbstract() + "//lang//" + secondAbstract.asText());
                }else{
                    biblio.setAbstract(secondAbstract.asText());
                }
            }
            JsonNode lIssn = item.get("lIssn");
            if (isVal(lIssn)) {
                biblio.setISSN(lIssn.asText());
            }

            JsonNode beginPage = item.get("beginPage");
            JsonNode endPage = item.get("endPage");
            if (isVal(beginPage) && isVal(endPage)) {
                biblio.setPageRange(beginPage.asText() + "--" + endPage.asText());
            }

            JsonNode pubDate = item.get("pblicteDate");
            if (isVal(pubDate))
                biblio.setPublicationDate(pubDate.asText());

            JsonNode authorList = item.get("koarAuthrList");
            Lexicon lexicon = Lexicon.getInstance();
            if (isVal(authorList)) {
                Iterator<JsonNode> authorIt = authorList.elements();
                ArrayList<Person> koAuthorList = new ArrayList<>();
                ArrayList<Person> enAuthorList = new ArrayList<>();
                while (authorIt.hasNext()) {
                    JsonNode author = authorIt.next();
                    Person koPerson = new Person();
                    Person enPerson = new Person();
                    koPerson.setLang("ko");
                    enPerson.setLang("en");
                    
                    JsonNode authrSn = author.get("authrSn");
                    JsonNode firstAuthr = author.get("firstAuthr");
                    JsonNode secondAuthr = author.get("secondAuthr");
                    JsonNode orcidNode = author.get("orcid");
                    JsonNode authrEmail = author.get("authrEmail");

                    // 기관, iter 해야됨
                    JsonNode koarAuthrInsttList = author.get("koarAuthrInsttList");
                    
                    int order = isVal(authrSn) ? authrSn.asInt() : -1;
                    String orcid = isVal(orcidNode) ? orcidNode.asText() : "";
                    String email = isVal(authrEmail) ? authrEmail.asText() : "";
               
                    Iterator<JsonNode> affIt = koarAuthrInsttList.elements();
                    ArrayList<Affiliation> affiliations = new ArrayList<>();
                    while (affIt.hasNext()) {
                        JsonNode aff = affIt.next();
                        if (isVal(aff)) {
                            JsonNode firstAff = aff.get("firstInsttnm");
                            JsonNode secondAff = aff.get("secondInsttnm");

                            if (isVal(firstAff)) {
                                Affiliation a = new Affiliation();
                                a.setAffiliationString(firstAff.asText());
                                a.setRawAffiliationString(firstAff.asText());
                                affiliations.add(a);
                            }
                            if (isVal(secondAff)) {
                                Affiliation a = new Affiliation();
                                a.setAffiliationString(secondAff.asText());
                                a.setRawAffiliationString(secondAff.asText());
                                affiliations.add(a);
                            }
                        }
                    }
                    

                    if (isVal(firstAuthr)) {
                        if (firstAuthr.asText().matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
                            koPerson.setOrder(order);
                            koPerson.setORCID(orcid);
                            koPerson.setEmail(email);
                            koPerson.setAffiliations(affiliations);
                            String name = firstAuthr.asText();
                            if(lexicon.inLastNames(name.substring(0,1))){
                                koPerson.setLastName(name.substring(0,1));
                                koPerson.setFirstName(name.substring(1));
                            } else if (lexicon.inLastNames(name.substring(0, 2))) {
                                koPerson.setLastName(name.substring(0, 2));
                                koPerson.setFirstName(name.substring(2));
                            }
                            koAuthorList.add(koPerson);
                        } else{
                            enPerson.setOrder(order);
                            enPerson.setORCID(orcid);
                            enPerson.setEmail(email);
                            enPerson.setAffiliations(affiliations);
                            String name = firstAuthr.asText();
                            String[] split = name.split(",");
                            for (String s : split) {
                                if(lexicon.inLastNames(s.toLowerCase())){
                                    enPerson.setLastName(s);
                                } else{
                                    enPerson.setFirstName(s);
                                }
                            }
                            enAuthorList.add(enPerson);
                        }
                    }
                    if (isVal(secondAuthr)) {
                        if (secondAuthr.asText().matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
                            koPerson.setOrder(order);
                            koPerson.setORCID(orcid);
                            koPerson.setEmail(email);
                            koPerson.setAffiliations(affiliations);
                            String name = secondAuthr.asText();
                            if(lexicon.inLastNames(name.substring(0,1))){
                                koPerson.setLastName(name.substring(0,1));
                                koPerson.setFirstName(name.substring(1));
                            } else if (lexicon.inLastNames(name.substring(0, 2))) {
                                koPerson.setLastName(name.substring(0, 2));
                                koPerson.setFirstName(name.substring(2));
                            }
                            koAuthorList.add(koPerson);
                        } else{
                            enPerson.setOrder(order);
                            enPerson.setORCID(orcid);
                            enPerson.setEmail(email);
                            enPerson.setAffiliations(affiliations);
                            String name = secondAuthr.asText();
                            String[] split = name.split(",");
                            for (String s : split) {
                                if(lexicon.inLastNames(s.toLowerCase())){
                                    enPerson.setLastName(s);
                                } else{
                                    enPerson.setFirstName(s);
                                }
                            }
                            enAuthorList.add(enPerson);
                        }
                    }
                }
                koAuthorList.addAll(enAuthorList);
                biblio.setFullAuthors(koAuthorList);
            }

            JsonNode keywords = item.get("koarAuthrKwrdList");
            if (isVal(keywords)) {
                Iterator<JsonNode> keywordIt = keywords.elements();
                ArrayList<Keyword> koKeyword = new ArrayList<>();
                ArrayList<Keyword> enKeyword = new ArrayList<>();
                while (keywordIt.hasNext()) {
                    JsonNode keyword = keywordIt.next();
                    if (isVal(keyword)) {
                        JsonNode firstKeyword = keyword.get("firstKwrd");
                        JsonNode secondKeyword = keyword.get("secondKwrd");
                        if (isVal(firstKeyword))
                            if (firstKeyword.asText().matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
                                koKeyword.add(new Keyword(firstKeyword.asText()));
                            } else{
                                enKeyword.add(new Keyword(firstKeyword.asText()));
                            }
                        if (isVal(secondKeyword))
                            if (secondKeyword.asText().matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
                                koKeyword.add(new Keyword(secondKeyword.asText()));
                            } else{
                                enKeyword.add(new Keyword(secondKeyword.asText()));
                            }
                    }
                }
                koKeyword.addAll(enKeyword);
                biblio.setKeywords(koKeyword);
            }
        }

        return biblio;
    }

    private boolean isVal(JsonNode node) {
        return (node != null && (!node.isMissingNode()) && !node.asText().equals("")) ||
            (node.size() > 0);
    }
}
