package org.grobid.core.meta;

import org.grobid.core.data.Affiliation;
import org.grobid.core.data.Person;
import org.grobid.core.utilities.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuthorVO implements Comparable<AuthorVO> {
    private String name_kr;
    private String name_en;
    private boolean isCorresp;
    
    private String orcid;
    private List<AffiliationVO> affiliations;
    private String email;
    private int order;

    public AuthorVO(Person person) {
        String lang = person.getLang() == null ? "en" : person.getLang();
        StringBuilder sb = new StringBuilder();
        if (lang.equals("en")) {
            sb.append(person.getFirstName());
            sb.append(" ");
            sb.append(person.getMiddleName() == null ? "" : person.getMiddleName() + " ");
            sb.append(person.getLastName());
            this.name_en = sb.toString();
        } else if (lang.equals("kr")){
            sb.append(person.getLastName());
            sb.append(person.getMiddleName() == null ? "" : person.getMiddleName());
            sb.append(person.getFirstName());
            this.name_kr = sb.toString().replaceAll(" ","");
        }
        this.isCorresp = false;
        ArrayList<AffiliationVO> affiliationVOS = new ArrayList<>();
        List<Affiliation> affiliations = person.getAffiliations();
        if (affiliations != null) {
            for (Affiliation affiliation : affiliations) {
                if (affiliation.getInstitutions() != null || affiliation.getDepartments() != null) {
                    affiliationVOS.add(new AffiliationVO(affiliation));
                }
            }
        }
        this.affiliations = affiliationVOS.stream()
            .distinct()
            .collect(Collectors.toList());
        this.orcid = person.getORCID();
        this.email = person.getEmail();
    }

    public AuthorVO(Pair<Person, Person> matchedAuthors) {
        Person koreanAuthor = matchedAuthors.getA();
        Person engAuthor = matchedAuthors.getB();

        StringBuilder sb = new StringBuilder();
        sb.append(koreanAuthor.getLastName());
        sb.append(koreanAuthor.getMiddleName() == null ? "" : koreanAuthor.getMiddleName());
        sb.append(koreanAuthor.getFirstName());
        this.name_kr = sb.toString().replaceAll(" ","");
        sb.delete(0, sb.length());
        sb.append(engAuthor.getFirstName());
        sb.append(" ");
        sb.append(engAuthor.getMiddleName() == null ? "" : engAuthor.getMiddleName() + " ");
        sb.append(engAuthor.getLastName());
        this.name_en = sb.toString();
        
        // todo corresponding author 
        this.isCorresp = koreanAuthor.getCorresp() || engAuthor.getCorresp();

        orcid = koreanAuthor.getORCID();
        if(orcid == null){
            orcid = engAuthor.getORCID();
        }

        if (koreanAuthor.getOrder() > engAuthor.getOrder()) {
            this.order = engAuthor.getOrder();
        } else {
            this.order = koreanAuthor.getOrder();
        }

        ArrayList<AffiliationVO> affiliationVOS = new ArrayList<>();
        List<Affiliation> koAff = koreanAuthor.getAffiliations();
        List<Affiliation> enAff = engAuthor.getAffiliations();

        if (koAff != null) {
            for (Affiliation affiliation : koAff) {
                if (affiliation.getRawAffiliationString() != null) {
                    affiliationVOS.add(new AffiliationVO(affiliation));
                }
            }
        }
        if (enAff != null) {
            for (Affiliation affiliation : enAff) {
                if (affiliation.getRawAffiliationString() != null) {
                    affiliationVOS.add(new AffiliationVO(affiliation));
                }
            }
        }
        this.affiliations = affiliationVOS.stream()
            .distinct()
            .collect(Collectors.toList());
        

//        // todo, kor, eng aff
//        List<Affiliation> korAff = koreanAuthor.getAffiliations();
//        List<Affiliation> engAff = engAuthor.getAffiliations();
//        if (korAff != null && engAff != null) {
//            if (korAff.size() > engAff.size()) {
//                this.affiliations = korAff;
//            } else{
//                this.affiliations = engAff;
//            }
//    
//        } else if (korAff != null) {
//            this.affiliations = korAff;
//        } else if (engAff != null) {
//            this.affiliations = engAff;
//        }
        email = koreanAuthor.getEmail();
        if (email == null) {
            email = engAuthor.getEmail();
        }


    }

    public String getName_kr() {
        return name_kr;
    }

    public void setName_kr(String name_kr) {
        this.name_kr = name_kr;
    }

    public String getName_en() {
        return name_en;
    }

    public void setName_en(String name_en) {
        this.name_en = name_en;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public List<AffiliationVO> getAffiliations() {
        return affiliations;
    }

    public void setAffiliations(List<AffiliationVO> affiliations) {
        this.affiliations = affiliations;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isCorresp() {
        return isCorresp;
    }

    public void setCorresp(boolean corresp) {
        isCorresp = corresp;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public int compareTo(AuthorVO o) {
        return getOrder() - o.getOrder();
    }
}
