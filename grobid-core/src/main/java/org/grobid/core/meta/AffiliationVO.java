package org.grobid.core.meta;

import org.grobid.core.data.Affiliation;

public class AffiliationVO {
    private String institution = null;
    private String department = null;
    private String country = null;
    private String postCode = null;
    private String postBox = null;
    private String region = null;
    private String settlement = null;
    private String addrLine = null;
    private String rawText = null;

    public AffiliationVO(Affiliation aff) {
        if(aff.getInstitutions() != null)
            institution = aff.getInstitutions().toString().replaceAll("\\[", "").replaceAll("]", "");
        if(aff.getDepartments() != null)
            department = aff.getDepartments().toString().replaceAll("\\[", "").replaceAll("]", "");
        country = aff.getCountry();
        postCode = aff.getPostCode();
        postBox = aff.getPostBox();
        region = aff.getRegion();
        settlement = aff.getSettlement();
        addrLine = aff.getAddrLine();
        rawText = aff.getRawAffiliationString();
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getPostBox() {
        return postBox;
    }

    public void setPostBox(String postBox) {
        this.postBox = postBox;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSettlement() {
        return settlement;
    }

    public void setSettlement(String settlement) {
        this.settlement = settlement;
    }

    public String getAddrLine() {
        return addrLine;
    }

    public void setAddrLine(String addrLine) {
        this.addrLine = addrLine;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AffiliationVO) {
            AffiliationVO o = (AffiliationVO) obj;
            return o.toString().equals(this.toString());
        } else{
            return false;
        }
    }

    @Override
    public int hashCode() {
        return rawText.hashCode();
    }
}
