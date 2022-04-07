/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grobid.service.tests;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.grobid.core.meta.AuthorVO;
import org.grobid.core.meta.MetaVO;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.GrobidPaths;
import org.grobid.service.GrobidRestService;
import org.grobid.service.GrobidServiceConfiguration;
import org.grobid.service.main.GrobidServiceApplication;
import org.grobid.service.module.GrobidServiceModuleTest;
import org.grobid.service.process.GrobidRestProcessFiles;
import org.grobid.service.util.BibTexMediaType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.Assert.*;

/**
 * Tests the RESTful service of the grobid-service project. This class can also
 * tests a remote system, when setting system property
 * org.grobid.service.test.uri to host to test.
 *
 * @author Florian Zipser
 */
public class GrobidRestServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidRestServiceTest.class);
    @Inject
    private GrobidRestProcessFiles restProcessFiles;

    @BeforeClass
    public static void setInitialContext() throws Exception {
    }

    @AfterClass
    public static void destroyInitialContext() throws Exception {
    }

    @ClassRule
    public static DropwizardAppRule<GrobidServiceConfiguration> APP =
            new DropwizardAppRule<>(GrobidServiceApplication.class, GrobidServiceModuleTest.TEST_CONFIG_FILE);


    private String baseUrl() {
        return String.format("http://localhost:%d%s" + "api/", APP.getLocalPort(), APP.getEnvironment().getApplicationContext().getContextPath());
    }

    @Before
    public void setUp() throws IOException {
        JerseyGuiceUtils.reset();

        GrobidServiceModuleTest testWorkerModule = new GrobidServiceModuleTest() {
            // redefine methods that are needed:
        };

        Guice.createInjector(testWorkerModule).injectMembers(this);
    }


    private static File getResourceDir() {
        return (new File("./src/test/resources/"));
    }

    private static Client getClient() {
        Client client = new JerseyClientBuilder().build();
        client.register(MultiPartFeature.class);
        return client;
    }


    /**
     * test the synchronous fully state less rest call
     */
    @Test
    public void testFullyRestLessHeaderDocument() throws Exception {
        String resp = getStrResponse(sample4(), GrobidPaths.PATH_HEADER);
        assertNotNull(resp);
    }


    /*
     * Test the synchronous fully state less rest call
     */
    @Test
    public void testFullyRestLessFulltextDocument() throws Exception {
        String resp = getStrResponse(sample4(), GrobidPaths.PATH_FULL_TEXT);
        assertNotNull(resp);
    }

    /**
     * Test the synchronous state less rest call for dates
     */
    @Test
    public void testRestDate() throws Exception {
        String resp = getStrResponse("date", "November 14 1999", GrobidPaths.PATH_DATE);
        assertNotNull(resp);
    }

    /**
     * Test the synchronous state less rest call for author sequences in headers
     */
    @Test
    public void testRestNamesHeader() throws Exception {
        String names = "Ahmed Abu-Rayyan *,a, Qutaiba Abu-Salem b, Norbert Kuhn * ,b, Cäcilia Maichle-Mößmer b";

        String resp = getStrResponse("names", names, GrobidPaths.PATH_HEADER_NAMES);
        assertNotNull(resp);
    }

    /**
     * Test the synchronous state less rest call for author sequences in
     * citations
     */
    @Test
    public void testRestNamesCitations() throws Exception {
        String names = "Marc Shapiro and Susan Horwitz";
        String resp = getStrResponse("names", names, GrobidPaths.PATH_CITE_NAMES);
        assertNotNull(resp);
    }


    /**
     * Test the synchronous state less rest call for affiliation + address
     * blocks
     */
    @Test
    public void testRestAffiliations() throws Exception {
        String affiliations = "Atomic Physics Division, Department of Atomic Physics and Luminescence, "
                + "Faculty of Applied Physics and Mathematics, Gdansk University of "
                + "Technology, Narutowicza 11/12, 80-233 Gdansk, Poland";
        String resp = getStrResponse("affiliations", affiliations, GrobidPaths.PATH_AFFILIATION);
        assertNotNull(resp);
    }

    /**
     * Test the synchronous state less rest call for patent citation extraction.
     * Send all xml and xml.gz ST36 files found in a given folder test/resources/patent
     * to the web service and write back the results in the test/sample
     */
    @Test
    @Ignore
    public void testRestPatentCitation() throws Exception {
        Client client = getClient();
        
        File xmlDirectory = new File(getResourceDir().getAbsoluteFile() + "/patent");
        File[] files = xmlDirectory.listFiles();
        assertNotNull(files);

        for (final File currXML : files) {
            try {
                if (currXML.getName().toLowerCase().endsWith(".xml") ||
                        currXML.getName().toLowerCase().endsWith(".xml.gz")) {

                    assertTrue("Cannot run the test, because the sample file '" + currXML
                            + "' does not exists.", currXML.exists());
                    FormDataMultiPart form = new FormDataMultiPart();
                    form.field("input", currXML, MediaType.MULTIPART_FORM_DATA_TYPE);
                    form.field("consolidate", "0", MediaType.MULTIPART_FORM_DATA_TYPE);

                    Response response = client.target(
                            baseUrl() + GrobidPaths.PATH_CITATION_PATENT_ST36)
                            .request()
                            .accept(MediaType.APPLICATION_XML + ";charset=utf-8")
                            .post(Entity.entity(form, MediaType.MULTIPART_FORM_DATA_TYPE));

                    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

                    String tei = response.readEntity(String.class);

                    File outputFile = new File(getResourceDir().getAbsoluteFile() +
                            "/../sample/" + currXML.getName().replace(".xml", ".tei.xml").replace(".gz", ""));

                    // writing the result in the sample directory
                    FileUtils.writeStringToFile(outputFile, tei, "UTF-8");
                }
            } catch (final Exception exp) {
                LOGGER.error("An error occured while processing the file "
                        + currXML.getAbsolutePath() + ". Continuing the process for the other files");
            }
        }
    }

    @Test
    public void testGetVersion_shouldReturnCurrentGrobidVersion() throws Exception {
        Response resp = getClient().target(baseUrl() + GrobidPaths.PATH_GET_VERSION)
                .request()
                .get();

        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        assertEquals(GrobidProperties.getVersion(), resp.readEntity(String.class));
    }

    @Test
    public void isAliveReturnsTrue() throws Exception {
        Response resp = getClient().target(baseUrl() + GrobidPaths.PATH_IS_ALIVE)
                .request()
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        assertEquals("true", resp.readEntity(String.class));
    }

    @Test
    public void processCitationReturnsCorrectBibTeXForMissingFirstName() {
        Form form = new Form();
        form.param(GrobidRestService.CITATION, "Graff, Expert. Opin. Ther. Targets (2002) 6(1): 103-113");
        Response response = getClient().target(baseUrl()).path(GrobidPaths.PATH_CITATION)
                                       .request()
                                       .accept(BibTexMediaType.MEDIA_TYPE)
                                       .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("@article{-1,\n" +
                "  author = {Graff},\n" +
                "  journal = {Expert. Opin. Ther. Targets},\n" +
                "  date = {2002},\n" +
                "  year = {2002},\n" +
                "  pages = {103--113},\n" +
                "  volume = {6},\n" +
                "  number = {1}\n" +
                "}\n",
            response.readEntity(String.class));
    }

    @Test
    public void processCitationReturnsBibTeX() {
        Form form = new Form();
        form.param(GrobidRestService.CITATION, "Kolb, S., Wirtz G.: Towards Application Portability in Platform as a Service\n" +
            "Proceedings of the 8th IEEE International Symposium on Service-Oriented System Engineering (SOSE), Oxford, United Kingdom, April 7 - 10, 2014.");
        Response response = getClient().target(baseUrl()).path(GrobidPaths.PATH_CITATION)
                                       .request()
                                       .accept(BibTexMediaType.MEDIA_TYPE)
                                       .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("@inproceedings{-1,\n" +
                "  author = {Kolb, S and Wirtz, G},\n" +
                "  booktitle = {Towards Application Portability in Platform as a Service Proceedings of the 8th IEEE International Symposium on Service-Oriented System Engineering (SOSE)},\n" +
                "  date = {2014},\n" +
                "  year = {2014},\n" +
//                "  year = {April 7 - 10, 2014},\n" +
                "  address = {Oxford, United Kingdom}\n" +
                "}\n",
            response.readEntity(String.class));
    }

    @Test
    public void processCitationReturnsBibTeXAndCanInludeRaw() {
        Form form = new Form();
        form.param(GrobidRestService.CITATION, "Kolb, S., Wirtz G.: Towards Application Portability in Platform as a Service\n" +
            "Proceedings of the 8th IEEE International Symposium on Service-Oriented System Engineering (SOSE), Oxford, United Kingdom, April 7 - 10, 2014.");
        form.param(GrobidRestService.INCLUDE_RAW_CITATIONS, "1");
        Response response = getClient().target(baseUrl()).path(GrobidPaths.PATH_CITATION)
                                       .request()
                                       .accept(BibTexMediaType.MEDIA_TYPE)
                                       .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("@inproceedings{-1,\n" +
                "  author = {Kolb, S and Wirtz, G},\n" +
                "  booktitle = {Towards Application Portability in Platform as a Service Proceedings of the 8th IEEE International Symposium on Service-Oriented System Engineering (SOSE)},\n" +
                "  date = {2014},\n" +
                "  year = {2014},\n" +
//                "  year = {April 7 - 10, 2014},\n" +
                "  address = {Oxford, United Kingdom},\n" +
                "  raw = {Kolb, S., Wirtz G.: Towards Application Portability in Platform as a Service\n" +
                "Proceedings of the 8th IEEE International Symposium on Service-Oriented System Engineering (SOSE), Oxford, United Kingdom, April 7 - 10, 2014.}\n" +
                "}\n",
            response.readEntity(String.class));
    }

    @Test
    public void testFile() throws IOException, ParseException {
        Reader reader = new FileReader("/Users/hs/Desktop/jsonResult.txt");

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(reader);

        JSONArray jsonArr_ = (JSONArray) obj;

        JSONArray jsonArr = new JSONArray();

        Random rand = new Random();

        File[] files = new File("/Users/hs/Desktop/pdfs/").listFiles();

        for (int i = 0; i < 50; i++) {
            int i1 = rand.nextInt(13892);
//            int i1 = rand.nextInt(files.length);

            JSONObject j = (JSONObject) jsonArr_.get(i1);
            JSONObject titleObj = (JSONObject) j.get("title");
            String enTitle = (String) titleObj.get("en");
            String koTitle = (String) titleObj.get("ko");

            JSONObject abstractObj = (JSONObject) j.get("abstract");
            String enAbstract = (String) abstractObj.get("en");
            String koAbstract = (String) abstractObj.get("ko");

//            JSONObject keywordObj = (JSONObject) j.get("keyword");
//            String enKeyword = (String) keywordObj.get("en");
//            String koKeyword = (String) keywordObj.get("ko");

            String authors = (String) j.get("authors");
            String docId = (String) j.get("doc_id");

            File file = new File("/Users/hs/Desktop/pdfs/" + docId + ".pdf");
            
            while ((enTitle == null && koTitle == null) || (enAbstract == null && koAbstract == null) || authors == null) {
//            while ((enTitle != null || koTitle != null) && (enAbstract != null || koAbstract != null) && authors != null && !file.exists()) {
                i1 = rand.nextInt(13892);
//                i1 = rand.nextInt(files.length);
                j = (JSONObject) jsonArr_.get(i1);
                titleObj = (JSONObject) j.get("title");
                enTitle = (String) titleObj.get("en");
                koTitle = (String) titleObj.get("ko");
                docId = (String) j.get("doc_id");


                abstractObj = (JSONObject) j.get("abstract");
                enAbstract = (String) titleObj.get("en");
                koAbstract = (String) titleObj.get("ko");

                file = new File("/Users/hs/Desktop/pdfs/" + docId + ".pdf");

//                keywordObj = (JSONObject) j.get("keyword");
//                enKeyword = (String) titleObj.get("en");
//                koKeyword = (String) titleObj.get("ko");

                authors = (String) j.get("authors");
            }

            jsonArr.add(j);
        }

        String doc_id = "";
        
        HashMap<JSONObject, Map<String, String>> titleMap = new HashMap<>();
        HashMap<JSONObject, Map<String, String>> abstractMap = new HashMap<>();
        HashMap<String, String> authorsMap = new HashMap<>();

        for (Object o : jsonArr) {
            JSONObject jsonObj = (JSONObject) o;
            doc_id = (String) jsonObj.get("doc_id");
            LinkedHashMap<String, InputStream> paramMap = new LinkedHashMap<>();
            ArrayList<MetaVO> res_ = new ArrayList<>();

            File pdfFile = new File("/Users/hs/Desktop/pdfs/" + doc_id + ".pdf");
            if (pdfFile.exists()) {
                System.out.println("exist");
                paramMap.put(doc_id, new FileInputStream(pdfFile));
                Object response = restProcessFiles.getMetaData(paramMap).getEntity();
                if (response instanceof List) {
                    res_ = (ArrayList<MetaVO>) response;
                } else{
                    continue;
                }
            }else{
                try (BufferedInputStream in = new BufferedInputStream(new URL("https://www.koreascience.or.kr/article/" + doc_id + ".pdf").openStream())) {
                    System.out.println("non exist.. save..");
                    FileUtils.copyInputStreamToFile(in, pdfFile);
                    paramMap.put(doc_id, new FileInputStream(pdfFile));
                    Object response = restProcessFiles.getMetaData(paramMap).getEntity();
                    if (response instanceof List) {
                        res_ = (ArrayList<MetaVO>) response;
                    } else{
                        continue;
                    }
                }
            }
            MetaVO res = res_.get(0);
            
            JSONObject titleObj = (JSONObject) jsonObj.get("title");
            titleObj.put("doc_id", doc_id);
            JSONObject abstractObj = (JSONObject) jsonObj.get("abstract");
            String authorsObj = (String) jsonObj.get("authors");
            
            String title_ko = res.getTitle_ko();
            String title_en = res.getTitle_en();
            String abstract_ko = res.getAbstract_ko();
            String abstract_en = res.getAbstract_en();
            List<AuthorVO> authors = res.getAuthors();
            StringBuilder sb = new StringBuilder();
            if (authorsObj.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
                for (AuthorVO author : authors) {
                    sb.append(author.getName_kr());
                }
            } else{
                for (AuthorVO author : authors) {
                    sb.append(author.getName_en());
                }
                String[] split = authorsObj.split(";");
                StringBuilder sbb = new StringBuilder();
                for (String s : split) {
                    String[] s1 = s.split(" ");
                    List<String> strings = Arrays.asList(s1);
                    Collections.reverse(strings);
                    for (String string : strings) {
                        sbb.append(string);
                    }
                }
                authorsObj = sbb.toString();
            }
            authorsMap.put(authorsObj, sb.toString().replaceAll("null", ""));

            HashMap<String, String> respTitleMap = new HashMap<>();
            HashMap<String, String> respAbstractMap = new HashMap<>();
            respTitleMap.put("ko", title_ko);
            respTitleMap.put("en", title_en);
            respAbstractMap.put("ko", abstract_ko);
            respAbstractMap.put("en", abstract_en);

            titleMap.put(titleObj, respTitleMap);
            abstractMap.put(abstractObj, respAbstractMap);
        }
        System.out.println("----------------TITLE------------------");
        System.out.println(compare(titleMap, false));
        System.out.println("----------------ABSTRACT------------------");
        System.out.println(compare(abstractMap, false));
        System.out.println("----------------AUTHORS------------------");
        System.out.println(compareOneLang(authorsMap));
    }
    
    private static double compare(HashMap<JSONObject, Map<String,String>> map, Boolean copy) {
        int i = 0;
        int n = 0;
        int similarity = 0;
        for (Map.Entry<JSONObject, Map<String, String>> entry : map.entrySet()) {
            JSONObject jsonObject = entry.getKey();
            Map<String, String> target = entry.getValue();

            String koReal = (String) jsonObject.get("ko");
            String enReal = (String) jsonObject.get("en");
            String docId = (String) jsonObject.get("doc_id");

            String koTarget = target.get("ko") == null ? "" : target.get("ko");
            String enTarget = target.get("en") == null ? "" : target.get("en");

            if (koReal != null) {
                n++;
                if (clean(koReal).equalsIgnoreCase(clean(koTarget))) {
                    i++;
                } else if (ratcliffObershelpDistance(clean(koReal), clean(koTarget), false) > 0.95) {
                    similarity++;
                } else {
                    if(copy){
                        try {
                            FileUtils.copyFile(new File("/Users/hs/Desktop/pdfs/" + docId + ".pdf"), new File("/Users/hs/Desktop/grobid_hsp/grobid-home/trainTmp/pdf/" + docId + ".pdf"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("------------error case(kr)------------");
                    System.out.println("expected        : " + koReal);
                    System.out.println("expected(clean) : " + clean(koReal));
                    System.out.println("result          : " + koTarget);
                    System.out.println("result(clean)   : " + clean(koTarget));
                }
            }
            if (enReal != null) {
                n++;
                if (clean(enReal).equalsIgnoreCase(clean(enTarget))){
                    i++;
                } else if (ratcliffObershelpDistance(clean(enReal), clean(enTarget), false) > 0.95) {
                    similarity++;
                } else{
                    if (copy) {
                        try {
                            FileUtils.copyFile(new File("/Users/hs/Desktop/pdfs/" + docId + ".pdf"), new File("/Users/hs/Desktop/grobid_hsp/grobid-home/trainTmp/pdf/" + docId + ".pdf"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("------------error case(en)------------");
                    System.out.println("expected        : " + enReal);
                    System.out.println("expected(clean) : " + clean(enReal));
                    System.out.println("result          : " + enTarget);
                    System.out.println("result(clean)   : " + clean(enTarget));
                }
            }
        }
        System.out.println(n+"개 중, " +i+"개 일치합니다.");
        System.out.println(n+"개 중, " +similarity+"개는 95% 이상의 유사도를 가지고 있습니다.");
        System.out.println(n+"개 중, " +(n-i-similarity)+"개가 틀렸습니다.");
        
        return ((i+similarity)*100.0)/n;
    }

    private static double compareOneLang(Map<String, String> map) {
        int i = 0;
        int n = 0;
        int similarity = 0;

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String real = entry.getKey();
            String target = entry.getValue();
            if (real != null) {
                n++;
                if (clean(real).equalsIgnoreCase(clean(target))) {
                    i++;
                } else if (ratcliffObershelpDistance(clean(real), clean(target), false) > 0.95) {
                    similarity++;
                } else {
                    System.out.println("------------error case(kr)------------");
                    System.out.println("expected        : " + real);
                    System.out.println("expected(clean) : " + clean(real));
                    System.out.println("result          : " + target);
                    System.out.println("result(clean)   : " + clean(target));
                }
            }
        }
        
        System.out.println(n+"개 중, " +i+"개 일치합니다.");
        System.out.println(n+"개 중, " +similarity+"개는 95% 이상의 유사도를 가지고 있습니다.");
        System.out.println(n+"개 중, " +(n-i-similarity)+"개가 틀렸습니다.");

        return ((i+similarity)*100.0)/n;
    }
    
    private static String clean(String str){
        if (str == null) {
            return "";
        }
        if (str.matches("\\{\\\\[a-zA-Z]+\\}")) {
            str = str.replaceAll("\\{\\\\[a-zA-Z]+\\}", "");
        }
//        str = str.replaceAll("\\$sim\\$", "~");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
//            if (String.valueOf(str.charAt(i)).matches("[^\\(\\（\\[\\ \\⋅\\ㆍ\\•\\･\\*\\,\\:\\;\\?\\.\\․\\!\\/\\)\\）\\-\\−\\–\\‐\\«\\»\\„\\\"\\“\\”\\‘\\’\\'\\`\\$\\#\\@\\]\\*\\♦\\♥\\♣\\♠]")) {
            if (String.valueOf(str.charAt(i)).matches("[a-zA-Z0-9가-힇ㄱ-ㅎㅏ-ㅣぁ-ゔァ-ヴー々〆〤一-龥]")) {
                sb.append(str.charAt(i));
            }
        }
        return sb.toString();
    }


    private static double ratcliffObershelpDistance(String string1, String string2, boolean caseDependent) {
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
    
    @Test
    public void train(){
        restProcessFiles.trainPdf();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    @Ignore
    public void processStatelessReferencesDocumentReturnsValidBibTeXForKolbAndKopp() throws Exception {
        final FileDataBodyPart filePart = new FileDataBodyPart(GrobidRestService.INPUT, new File(this.getClass().getResource("/sample5/gadr.pdf").toURI()));
        FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
        //final FormDataMultiPart multipart = (FormDataMultiPart) formDataMultiPart.field("foo", "bar").bodyPart(filePart);
        final FormDataMultiPart multipart = (FormDataMultiPart) formDataMultiPart.bodyPart(filePart);
        Response response = getClient().target(baseUrl() + GrobidPaths.PATH_REFERENCES)
                                       .request(BibTexMediaType.MEDIA_TYPE)
                                       .post(Entity.entity(multipart, multipart.getMediaType()));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("@techreport{0,\n" +
            "  author = {Büchler, A},\n" +
            "  year = {2017}\n" +
            "}\n" +
            "\n" +
            "@article{1,\n" +
            "  author = {Kopp, O and Armbruster, A and Zimmermann, O},\n" +
            "  title = {Markdown Architectural Decision Records: Format and Tool Support},\n" +
            "  booktitle = {ZEUS. CEUR Workshop Proceedings},\n" + 
            "  year = {2018},\n" +
            "  volume = {2072}\n" +
            "}\n" +
            "\n" +
            "@article{2,\n" +
            "  author = {Thurimella, A and Schubanz, M and Pleuss, A and Botterweck, G},\n" +
            "  title = {Guidelines for Managing Requirements Rationales},\n" +
            "  journal = {IEEE Software},\n" +
            "  year = {Jan 2017},\n" +
            "  pages = {82--90},\n" +
            "  volume = {34},\n" +
            "  number = {1}\n" +
            "}\n" +
            "\n" +
            "@article{3,\n" +
            "  author = {Zdun, U and Capilla, R and Tran, H and Zimmermann, O},\n" +
            "  title = {Sustainable Architectural Design Decisions},\n" +
            "  journal = {IEEE Software},\n" +
            "  year = {Nov 2013},\n" +
            "  pages = {46--53},\n" +
            "  volume = {30},\n" +
            "  number = {6}\n" +
            "}\n" +
            "\n" +
            "@inbook{4,\n" +
            "  author = {Zimmermann, O and Wegmann, L and Koziolek, H and Goldschmidt, T},\n" +
            "  title = {Architectural Decision Guidance Across Projects -Problem Space Modeling, Decision Backlog Management and Cloud Computing Knowledge},\n" +
            "  booktitle = {Working IEEE/IFIP Conference on Software Architecture},\n" +
            "  year = {2015}\n" +
            "}\n" +
            "\n" +
            "@inbook{5,\n" +
            "  author = {Zimmermann, O and Miksovic, C},\n" +
            "  title = {Decisions required vs. decisions made},\n" +
            "  booktitle = {Aligning Enterprise, System, and Software Architectures},\n" +
            "  publisher = {IGI Global},\n" +
            "  year = {2013}\n" +
            "}\n" +
            "\n", response.readEntity(String.class));
    }

    private String getStrResponse(File pdf, String method) {
        assertTrue("Cannot run the test, because the sample file '" + pdf + "' does not exists.", pdf.exists());

        FormDataMultiPart form = new FormDataMultiPart();
        form.field("input", pdf, MediaType.MULTIPART_FORM_DATA_TYPE);
        form.field("consolidate", "0", MediaType.MULTIPART_FORM_DATA_TYPE);

        Response response = getClient().target(baseUrl() + method)
                .request()
                .post(Entity.entity(form, MediaType.MULTIPART_FORM_DATA));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String cont = response.readEntity(String.class);
        
        return cont;
    }

    private String getStrResponse(String key, String val, String method) {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add(key, val);

        Response response = getClient().target(baseUrl() + method)
                .request()
                .post(Entity.entity(formData, MediaType.APPLICATION_FORM_URLENCODED));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String postResp = response.readEntity(String.class);
        assertNotNull(postResp);
        return postResp;
    }

    private static File sample4() {
        return new File(getResourceDir().getAbsoluteFile() + "/sample4/sample.pdf");
    }

}
