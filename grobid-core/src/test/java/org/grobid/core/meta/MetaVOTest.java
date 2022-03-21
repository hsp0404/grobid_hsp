package org.grobid.core.meta;

import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric;
import junit.framework.TestCase;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.data.Person;
import org.grobid.core.utilities.Pair;
import org.grobid.core.utilities.TextUtilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import scala.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.remove;
import static org.grobid.core.meta.MetaVO.Kor2EngName;
import static org.junit.Assert.assertEquals;

public class MetaVOTest {
    public MetaVOTest() throws Exception {
    }
    
    List<Person> testPersonList = new ArrayList<Person>();
    List<AuthorVO> authors = new ArrayList<>();
    String path = System.getProperty("user.dir");
    List<String> nameLine = new ArrayList<>();
    Map<String,String> nameMap = new HashMap<>();
    
    List<String> korName = new ArrayList<>();
    List<String> engName = new ArrayList<>();
    
    public void nameInit() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path + "/src/test/java/org/grobid/core/meta/kor_eng_name.txt"));
        String str;
        while ((str = br.readLine()) != null) {
            if (str.equals("stop")) {
                break;
            }
            if (str.length() >= 1) {
                nameLine.add(str);
            }
        }
        Collections.shuffle(nameLine);
        for (String str2 : nameLine) {
            String[] split = str2.split(" / ");
            korName.add(split[0]);
            engName.add(split[1]);
            nameMap.put(split[0], split[1]);
        }
        br.close();
    }
    

    public List<Person> getTestPersonList() throws IOException {
        nameInit();

        for (String k : korName) {
            Person korTemp = new Person();
//            korTemp.setLastName(k.substring(0,2));
//            korTemp.setFirstName(k.substring(1));
            korTemp.setFirstName(k.substring(0,2));
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
        
        
        return testPersonList;
    }

    @DisplayName("셔플 후 통합 테스트")
    @RepeatedTest(20)
    public void testSetAuthor() throws EncoderException, IOException {
        ArrayList<Pair<String, String>> lastNamePairs = new ArrayList<>();
        lastNamePairs.add(new Pair<>("jo", "cho"));
        lastNamePairs.add(new Pair<>("seong", "sung"));
        lastNamePairs.add(new Pair<>("gu", "koo"));
        lastNamePairs.add(new Pair<>("gi", "kee"));
        lastNamePairs.add(new Pair<>("jung", "jeong"));
        lastNamePairs.add(new Pair<>("sin", "shin"));
        HashMap<Integer, String> indexKorName = new HashMap<>();
        List<Person> authors = getTestPersonList();
        for (Person a : authors) {
            if (a.getLang().equals("kr")) {
                if (a.getFirstName() == null && a.getLastName().length() == 2) {
                    String lastName = a.getLastName();
                    String resultLastName = lastName.substring(0, 1);
                    String resultFirstName = lastName.substring(1);
                    a.setFirstName(resultFirstName);
                    a.setLastName(resultLastName);
                } else if (a.getLastName() == null && a.getFirstName().length() == 2) {
                    String firstName = a.getFirstName();
                    String resultFirstName = firstName.substring(1);
                    String resultLastName = firstName.substring(0, 1);
                    a.setFirstName(resultFirstName);
                    a.setLastName(resultLastName);
                }
            }
        }
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
        
        double range = 0.875;
        int soundexRange = 4;

        Set<Integer> removeList = new HashSet<>();
        int n = process(indexKorName, authors, removeList, range, soundexRange, lastNamePairs);

        for (Integer r : removeList) {
            indexKorName.remove(r);
        }
        removeList.clear();

        soundexRange = 4;
        range = 0.65;
        int n2 = process(indexKorName, authors, removeList, range, soundexRange, lastNamePairs);

        for (Integer r : removeList) {
            indexKorName.remove(r);
        }
        
        removeList.clear();

        soundexRange = 3;
        range = 0.4;
        int n3 = process(indexKorName, authors, removeList, range, soundexRange, lastNamePairs);

        for (Integer r : removeList) {
            indexKorName.remove(r);
        }

        removeList.clear();

        soundexRange = 2;
        range = 0.4;
        int n4 = process(indexKorName, authors, removeList, range, soundexRange, lastNamePairs);

        for (Integer r : removeList) {
            indexKorName.remove(r);
        }
        
        
        
        
        if (n == 0 && n2 == 0 && n3 == 0 && authors.size() != 0) {
            for (Person author : authors) {
                this.authors.add(new AuthorVO(author));
            }
        }
        int z = 0;
        for (AuthorVO author : this.authors) {
            z++;
//            System.out.println(z);
//            System.out.println(author.getName_en() == null ? author.getName_kr() : author.getName_en());
            assertEquals(nameMap.get(author.getName_kr()).replaceAll("-", "").toLowerCase(), author.getName_en().toLowerCase());
        }
        assertEquals(korName.size(), this.authors.size());
        
    }

    private int process(HashMap<Integer, String> indexKorName, List<Person> authors, Set<Integer> removeList, double range, int soundexRange, List<Pair<String, String>> lastNamePairs) throws EncoderException {
        int n = 0;
        Soundex soundex = new Soundex();
        aut:
        for (Person author : authors) {
            if (author.isMatched()) {
                continue;
            }
            if (author.getLang().equals("en")) {
                n++;
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
                        compareEach:
                        for (int i = 0; i < engNameSplit.length; i++) {
                            int difference = soundex.difference(engNameSplit[i], korNameSplit[i]);
                            double v = ratcliffObershelpDistance(engNameSplit[i], korNameSplit[i], false);
                            if(isLastNameMatch(engNameSplit[i], korNameSplit[i], lastNamePairs)){
                                difference = 4;
                                v = 1.0;
                            }
                            if(difference == 4 && v >= 0.9){
                                System.out.println(korNameSplit[i] + ", " + engNameSplit[i]);
                                System.out.println("diff : " + difference + " ratcliff : " + v);
                                continue compareEach;
                            }
                            if (difference < soundexRange || v < range) {
                                System.out.println(korNameSplit[i] + ", " + engNameSplit[i]);
                                System.out.println("diff : " + difference + " ratcliff : " + v);
                                result = false;
                                continue compare;
                            }
                            System.out.println(korNameSplit[i] + ", " + engNameSplit[i]);
                            System.out.println("diff : " + difference + " ratcliff : " + v);
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