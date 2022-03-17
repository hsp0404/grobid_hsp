package org.grobid.core.meta;

import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric;
import junit.framework.TestCase;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.data.Person;
import org.grobid.core.utilities.Pair;
import org.grobid.core.utilities.TextUtilities;
import scala.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.grobid.core.meta.MetaVO.Kor2EngName;

public class MetaVOTest extends TestCase {
    List<Person> testPersonList = new ArrayList<Person>();
//    Person tempEngPerson = new Person();
//    Person tempKorPerson = new Person();
    List<AuthorVO> authors = new ArrayList<>();
    String[] korName = {"박해성", "김상운", "정현종"};
    String[] engName = {"HaeSeong Park", "Kim Sang-Un", "Jung Hyun-Jong"};
    
    

    public List<Person> getTestPersonList() {
        for (int i = 0; i < this.korName.length; i++) {

            for (String k : korName) {
                Person korTemp = new Person();
                korTemp.setLastName(k.substring(0,1));
                korTemp.setFirstName(k.substring(1));
                korTemp.setLang("kr");
                testPersonList.add(korTemp);
            }
            for (String s : engName) {
                String n = TextUtilities.capitalizeFully(s, TextUtilities.fullPunctuations);
                String n2 = n.replaceAll("-", "");
                String[] split = n2.split(" ");
                Person engTemp = new Person();
                engTemp.setLastName(split[1]);
                engTemp.setFirstName(split[0]);
                engTemp.setLang("en");
                testPersonList.add(engTemp);
            }
        }
        
        
        return testPersonList;
    }

    public void testSetAuthor() throws EncoderException {
        HashMap<Integer, String> indexKorName = new HashMap<>();
        List<Person> authors = getTestPersonList();
        for (int i = 0; i < authors.size(); i++) {
            if(authors.get(i).getLang().equals("kr")){
                StringBuilder sb = new StringBuilder();
                sb.append(authors.get(i).getFirstName() == null ? "" : authors.get(i).getFirstName());
                sb.append(" ");
                sb.append(authors.get(i).getLastName() == null ? "" : authors.get(i).getLastName());
                String engName = Kor2EngName(sb.toString());
                indexKorName.put(i, engName);
            }
        }

        int n = 0;
        Soundex soundex = new Soundex();
        aut:
        for (Person author : authors) {
            if (author.getLang().equals("en")) {
                n++;
                author.setOrder(n);
                StringBuilder sb = new StringBuilder();
                String firstName = author.getFirstName() == null ? "" : author.getFirstName().toLowerCase().replaceAll("-", "");
                String middleName = author.getMiddleName() == null ? "" : author.getMiddleName().toLowerCase().replaceAll("-", "");
                String lastName = author.getLastName() == null ? "" : author.getLastName().toLowerCase().replaceAll("-", "");
                String fName = TextUtilities.capitalizeFully(firstName, TextUtilities.fullPunctuations);
                String mName = TextUtilities.capitalizeFully(middleName, TextUtilities.fullPunctuations);
                String lName = TextUtilities.capitalizeFully(lastName, TextUtilities.fullPunctuations);
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
                    this.authors.add(new AuthorVO(author));
                }else{
                    compare:
                    for (Map.Entry<Integer, String> integerStringEntry : indexKorName.entrySet()) {
                        String korName = integerStringEntry.getValue();
                        boolean result = true;
                        String[] korNameSplit = korName.split(" ");
                        for (int i = 0; i < engNameSplit.length; i++) {
                            int difference = soundex.difference(engNameSplit[i], korNameSplit[i]);
                            double v = ratcliffObershelpDistance(engNameSplit[i], korNameSplit[i], false);
                            if(difference == 4 || v >= 0.9){
                                continue;
                            }
                            if (difference < 3 || v < 0.4) {
                                result = false;
                                continue compare;
                            }
                        }

                        if (result) {
                            Pair<Person, Person> matchedAuthor = new Pair<>(authors.get(integerStringEntry.getKey()), author);
                            this.authors.add(new AuthorVO(matchedAuthor));
                            continue aut;
                        }
                    }
                }
            }
        }
        if (n == 0 && authors.size() != 0) {
            for (Person author : authors) {
                this.authors.add(new AuthorVO(author));
            }
        }
        assertEquals(korName.length, this.authors.size());
        
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