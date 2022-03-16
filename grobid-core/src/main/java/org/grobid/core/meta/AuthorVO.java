package org.grobid.core.meta;

import org.grobid.core.data.Affiliation;
import org.grobid.core.data.Person;
import org.grobid.core.utilities.Pair;

import java.util.ArrayList;
import java.util.List;

public class AuthorVO {
    private String name_kr;
    private String name_en;
    private boolean isCorresp;
    
    private String orcid;
    private List<Affiliation> affiliations;
    private String email;

    public AuthorVO(Person person) {
        String lang = person.getLang();
        StringBuilder sb = new StringBuilder();
        if (lang.equals("en")) {
            sb.append(person.getFirstName());
            sb.append(" ");
            sb.append(person.getMiddleName() == null ? "" : person.getMiddleName() + " ");
            sb.append(person.getLastName());
            this.name_en = sb.toString();
        } else{
            sb.append(person.getLastName());
            sb.append(person.getMiddleName() == null ? "" : person.getMiddleName());
            sb.append(person.getFirstName());
            this.name_kr = sb.toString().replaceAll(" ","");
        }
        this.isCorresp = false;
        this.affiliations = person.getAffiliations();
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
        this.isCorresp = false;

        orcid = koreanAuthor.getORCID();
        if(orcid == null){
            orcid = engAuthor.getORCID();
        }

        // todo, kor, eng aff
        List<Affiliation> korAff = koreanAuthor.getAffiliations();
        List<Affiliation> engAff = engAuthor.getAffiliations();
        if (korAff != null && engAff != null) {
            if (korAff.size() > engAff.size()) {
                this.affiliations = korAff;
            } else{
                this.affiliations = engAff;
            }
    
        } else if (korAff != null) {
            this.affiliations = korAff;
        } else if (engAff != null) {
            this.affiliations = engAff;
        }
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

    public List<Affiliation> getAffiliations() {
        return affiliations;
    }

    public void setAffiliations(List<Affiliation> affiliations) {
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
}
