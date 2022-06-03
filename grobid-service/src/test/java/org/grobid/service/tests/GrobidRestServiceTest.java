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
import org.grobid.core.data.Figure;
import org.grobid.core.data.Table;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.meta.AuthorVO;
import org.grobid.core.meta.MetaVO;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.Pair;
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
import org.yaml.snakeyaml.Yaml;
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
import java.io.FileNotFoundException;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

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
    static Map<String, String> map;


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
        map = new Yaml().load(new FileReader(GrobidProperties.getGrobidHome() + "/hanja/table.yml"));

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
    
    public List<MetaVO> extractProcess(JSONArray jsonArray, boolean save) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(GrobidProperties.getMaxConcurrency());
        ArrayList<MetaVO> result = new ArrayList<>();
        List<Callable<List<MetaVO>>> threads = new ArrayList<>();
        
        for (Object o : jsonArray) {
            JSONObject jsonObj = (JSONObject) o;
            Callable<List<MetaVO>> thread = () -> {
                try {
                    String doc_id = (String) jsonObj.get("doc_id");
                    LinkedHashMap<String, InputStream> paramMap = new LinkedHashMap<>();
                    List<MetaVO> res_ = Collections.synchronizedList(new ArrayList<>());
                    File pdfFile = new File("/Users/hs/Desktop/pdfs/" + doc_id + ".pdf");
                    try (BufferedInputStream in = new BufferedInputStream(new URL("https://www.koreascience.or.kr/article/" + doc_id + ".pdf").openStream())) {
                        if(save){
                            System.out.println("non existing.. save..");
                            FileUtils.copyInputStreamToFile(in, pdfFile);
                            paramMap.put(doc_id, new FileInputStream(pdfFile));
                        } else{
                            System.out.println("non existing.. but not save");
                            paramMap.put(doc_id, in);
                        }
                        Object response = restProcessFiles.getMetaData(paramMap, 0).getEntity();
                        if (response instanceof List) {
                            return (List<MetaVO>) response;
                        }
                    } catch (IOException e){
                        System.out.println("파일이 없습니다.");
                    }
                } catch (GrobidException e) {
                    return null;
                }
                return null;
            };
            threads.add(thread);
        }
//        if(threads.size() > 5) {
//            List<List<Callable<MetaVO>>> partition = ListUtils.partition(threads, 5);
//            for (List<Callable<MetaVO>> callables : partition) {
//                List<Future<MetaVO>> futures = executorService.invokeAll(callables);
//                try {
//                    for (Future<MetaVO> future : futures) {
//                        result.add(future.get());
//                    }
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                } catch (ExecutionException e) {
//                    throw new RuntimeException(e);
//                } catch (IndexOutOfBoundsException e) {
//                    System.out.println(e);
//                }
//            }
//        } else{
//            List<Future<MetaVO>> futures = executorService.invokeAll(threads);
//            try {
//                for (Future<MetaVO> future : futures) {
//                    result.add(future.get());
//                }
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            } catch (ExecutionException e) {
//                throw new RuntimeException(e);
//            } catch (IndexOutOfBoundsException e) {
//                System.out.println(e);
//            }
//        }

        List<Future<List<MetaVO>>> futures = executorService.invokeAll(threads);
        try {
            for (Future<List<MetaVO>> future : futures) {
                List<MetaVO> r = future.get();
                if (r == null)
                    result.add(new MetaVO());
                else
                    result.add(r.get(0));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (IndexOutOfBoundsException e) {
            System.out.println(e);
        }
        return result;
    }
  
    @Test
    public void findDataSetTest() throws IOException, ParseException {
        Random rand = new Random();
        Integer consolidated = 0;

        int resultNumber = rand.nextInt(48);
        System.out.println("jsonResult-test"+resultNumber+".txt 파일을 불러옵니다");

        Reader reader = new FileReader("/Users/hs/Desktop/json-separate/jsonResult-test"+resultNumber+".txt");
//        Reader reader = new FileReader("/Users/hs/Desktop/json-separate/jsonResult-test13.txt");

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(reader);

        JSONArray jsonArr_ = (JSONArray) obj;

        JSONArray jsonArr = new JSONArray();

        HashSet<Integer> duplicate = new HashSet<>();

        // 로컬에 저장할 것인지
//        Boolean save = false;
        // TrainTmp에 저장할 것인지
//        Boolean copy = false;
//        int size = 50;


        File[] files = new File("/Users/hs/Desktop/pdfs/").listFiles();

        for (int i = 0; i < 1; i++) {
            int i1 = rand.nextInt(10000);
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

//            while (koTitle == null || !koTitle.matches(".*[一-龥].*")) {
//            while (enTitle == null || !enTitle.matches(".*\\$.*\\$.*")) {
            while (koTitle == null || !koTitle.matches(".*서브텍스트 활용.*")) {
//            while ((enTitle == null && koTitle == null) || (enAbstract == null && koAbstract == null) || authors == null) {
//            while ((enTitle != null || koTitle != null) && (enAbstract != null || koAbstract != null) && authors != null && !file.exists()) {
                i1 = rand.nextInt(10000);
                if(duplicate.contains(i1))
                    continue;
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
            duplicate.add(i1);
            System.out.println("d" + duplicate);
            System.out.println(i1);
            
            jsonArr.add(j);
        }

        System.out.println("---------------------------------------------");
        for (Object o : jsonArr) {
            JSONObject o1 = (JSONObject) o;
            JSONObject title = (JSONObject) o1.get("title");
            String titleValue = (String) title.get("ko");
            System.out.println(titleValue);

        }
        System.out.println("---------------------------------------------");
    }
    
    @Test
    public void duplicateTest() throws IOException, ParseException {
        File file = new File("/Users/hs/Desktop/grobid_hsp/grobid-home/trainTmp/pdf");
        List<String> list = new ArrayList<>();
        File[] files = file.listFiles();
        for (File f : files) {
            if (StringUtils.endsWith(f.getName(),"pdf")) {
                list.add(f.getName().replaceAll(".pdf", ""));
            }
        }

        System.out.println(list);


        JSONParser parser = new JSONParser();
        for (int i = 0; i < 48; i++) {
            Reader reader = new FileReader("/Users/hs/Desktop/json-separate/jsonResult-test"+i+".txt");
            Object obj = parser.parse(reader);
            
            parser.reset();
            reader.close();
        }

    }
    
    public Map<String, Pair<Integer,Integer>> testFile(int size, boolean save, boolean copy) throws Exception {
        Random rand = new Random();
        Integer consolidated = 0;
        
        int resultNumber = rand.nextInt(48);
        System.out.println("jsonResult-test"+resultNumber+".txt 파일을 불러옵니다");
        
        Reader reader = new FileReader("/Users/hs/Desktop/json-separate/jsonResult-test"+resultNumber+".txt");

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(reader);

        JSONArray jsonArr_ = (JSONArray) obj;

        JSONArray jsonArr = new JSONArray();

        // 로컬에 저장할 것인지
//        Boolean save = false;
        // TrainTmp에 저장할 것인지
//        Boolean copy = false;
//        int size = 50;


        File[] files = new File("/Users/hs/Desktop/pdfs/").listFiles();

        for (int i = 0; i < size; i++) {
            int i1 = rand.nextInt(10000);
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

//            while (koTitle == null || !koTitle.matches(".*[一-龥].*")) {
//            while (enTitle == null || !enTitle.matches(".*\\$.*\\$.*")) {
//            while (enTitle == null || !enTitle.matches(".*\\$.*\\$.*")) {
            while ((enTitle == null && koTitle == null) || (enAbstract == null && koAbstract == null) || authors == null) {
//            while (authors == null || !authors.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
//            while ((enTitle != null || koTitle != null) && (enAbstract != null || koAbstract != null) && authors != null && !file.exists()) {
                i1 = rand.nextInt(10000);
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
        
        boolean forFullText = false;

        String doc_id = "";
        
        HashMap<JSONObject, Map<String, String>> titleMap = new HashMap<>();
        HashMap<JSONObject, Map<String, String>> abstractMap = new HashMap<>();
        HashMap<JSONObject, Map<String, String>> keywordMap = new HashMap<>();
        HashMap<Map<String, String>, String> authorsMap = new HashMap<>();
        
        int index = 1;

        List<MetaVO> metaVOS = extractProcess(jsonArr, save);


//        for (Object o : jsonArr) {
        for (int i=0; i<jsonArr.size(); i++) {
            JSONObject jsonObj = (JSONObject) jsonArr.get(i);
            doc_id = (String) jsonObj.get("doc_id");
           MetaVO res = metaVOS.get(i);
            if (res.isConsolidated()) {
                consolidated++;
            }
//            if (res.getTables() != null && res.getFigures() != null) {
            if (res.getFigures() != null) {
                forFullText = isKoreanFigureTable(res.getTables(), res.getFigures());
                
            }

            JSONObject titleObj = (JSONObject) jsonObj.get("title");
            titleObj.put("doc_id", doc_id);
            JSONObject abstractObj = (JSONObject) jsonObj.get("abstract");
            abstractObj.put("doc_id", doc_id);
            String authorsObj = (String) jsonObj.get("authors");
            JSONObject keywordObj = (JSONObject) jsonObj.get("keywords");
            keywordObj.put("doc_id", doc_id);


            String title_ko = res.getTitle_ko();
            String title_en = res.getTitle_en();
            String abstract_ko = res.getAbstract_ko();
            String abstract_en = res.getAbstract_en();
            List<String> keywords = res.getKeywords();
            StringBuilder keyword_ko = new StringBuilder();
            StringBuilder keyword_en = new StringBuilder();
            for (String keyword : keywords) {
                if (keyword.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
                    keyword_ko.append(keyword);
                    keyword_ko.append(";");
                } else{
                    if (keyword_en.toString().contains(";"+keyword + ";")) {
                        keyword_ko.append(keyword);
                        keyword_ko.append(";");
                    } else{
                        keyword_en.append(keyword);
                        keyword_en.append(";");
                    }
                }
            }

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
            Map<String, String> aObj = new HashMap<>();
            aObj.put("authr", authorsObj);
            aObj.put("doc_id", doc_id);
            aObj.put("index", String.valueOf(i));
            
            authorsMap.put(aObj, sb.toString().replaceAll("null", ""));

            HashMap<String, String> respTitleMap = new HashMap<>();
            HashMap<String, String> respAbstractMap = new HashMap<>();
            HashMap<String, String> respKeywordMap = new HashMap<>();
            respTitleMap.put("ko", title_ko);
            respTitleMap.put("en", title_en);
            respTitleMap.put("figureTable", String.valueOf(forFullText));
            respAbstractMap.put("ko", abstract_ko);
            respAbstractMap.put("en", abstract_en);
            respKeywordMap.put("ko", keyword_ko.toString());
            respKeywordMap.put("en", keyword_en.toString());

            titleMap.put(titleObj, respTitleMap);
            abstractMap.put(abstractObj, respAbstractMap);
            keywordMap.put(keywordObj, respKeywordMap);
        }
        HashMap<String, Pair<Integer,Integer>> result = new HashMap<>();
        System.out.println("----------------TITLE------------------");
        Pair<Integer, Integer> titleCompare = compare(titleMap, copy);
        System.out.println(titleCompare.getA());
        result.put("title", titleCompare);
        System.out.println("----------------ABSTRACT------------------");
        Pair<Integer, Integer> abstractCompare = compare(abstractMap, copy);
        System.out.println(abstractCompare.getA());
        result.put("abstract", abstractCompare);
        System.out.println("----------------AUTHORS------------------");
        Pair<Integer, Integer> authorCompare = compareOneLang(authorsMap, copy);
        System.out.println(authorCompare.getA());
        result.put("author", authorCompare);
        System.out.println("----------------KEYWORD------------------");
        Pair<Integer, Integer> keywordCompare = compare(keywordMap, copy);
        System.out.println(keywordCompare.getA());
        result.put("keyword", keywordCompare);
        
        return result;
        

    }

    private Boolean isKoreanFigureTable(List<Table> tables, List<Figure> figures) {
        if (tables.size() < 3) {
            return false;
        }
        for (Table table : tables) {
            if (table.getHeader().matches(".*표[. ]?[0-9]+.*") ||
                table.getNote().matches(".*표[. ]?[0-9]+.*") ||
                table.getContent().matches(".*표[. ]?[0-9]+.*")) {
                return true;
            }
        }
        
//        for (Figure figure : figures) {
//            if (figure.getHeader().matches(".*그림.*") ||
//                figure.getCaption().matches(".*그림.*") ||
//                figure.getContent().matches(".*그림.*")) {
//                return true;
//            }
//        }
        return false;
    }

    @Test
    public void realTest() throws Exception {
        int titleMatch = 0;
        int titleTotal = 0;
        int abstractMatch = 0;
        int abstractTotal = 0;
        int authorMatch = 0;
        int authorTotal = 0;
        int keywordMatch = 0;
        int keywordTotal = 0;
        int consolidatedAvg = 0;

        int iter = 10;
        int size = 100;
        
        for (int i = 0; i < iter; i++) {
            System.out.println((i+1) + " / " + iter + "번째..");
            Map<String, Pair<Integer, Integer>> result = testFile(size, false,false);
            if (result.get("title") != null) {
                titleMatch += result.get("title").getA();
                titleTotal += result.get("title").getB();
            }
            if (result.get("abstract") != null) {
                abstractMatch += result.get("abstract").getA();
                abstractTotal += result.get("abstract").getB();
            }
            if (result.get("author") != null) {
                authorMatch += result.get("author").getA();
                authorTotal += result.get("author").getB();
            }
            if (result.get("keyword") != null) {
                keywordMatch += result.get("keyword").getA();
                keywordTotal += result.get("keyword").getB();
            }
//            authorAvg += result.get("author");
//            keywordAvg += result.get("keyword");
//            consolidatedAvg += result.get("consolidated");
        }

        System.out.println(size + "개 " + iter + "회 반복 결과");
        if (titleTotal > 0)
            System.out.println("title       : " + ((double) titleMatch/titleTotal)*100);
        if (abstractTotal > 0)
            System.out.println("abstract    : " + ((double) abstractMatch/abstractTotal)*100);
        if (authorTotal > 0)
            System.out.println("author      : " + ((double) authorMatch/authorTotal)*100);
        if (keywordTotal > 0)
            System.out.println("keyword      : " + ((double) keywordMatch/keywordTotal)*100);

//        System.out.println("author      : " + authorAvg / iter);
//        System.out.println("consolidated: " + consolidatedAvg);

        File file = new File("/Users/hs/Desktop/pdfs/");
        File[] files = file.listFiles();
        for (File f : files) {
            f.delete();
        }
    }
    
    
    
    
    private static Pair<Integer, Integer> compare(HashMap<JSONObject, Map<String,String>> map, Boolean copy) {
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

            Boolean figureTable = Boolean.valueOf(target.get("figureTable"));


            if (koTarget.matches(".*[^(]+[一-龥]+[^)]+$")) {
                koTarget = translate(koTarget);
            }

            if (koReal != null) {
                if (koReal.contains("$")) {
                    System.out.println("$");
                }
                n++;
                if (clean(koReal).equalsIgnoreCase(clean(koTarget))) {
//                    if(copy && figureTable){
//                        try {
//                            System.out.println(docId + ".pdf .. 일치하여 trainTmp 폴더에 저장합니다");
//                            FileUtils.copyFile(new File("/Users/hs/Desktop/pdfs/" + docId + ".pdf"), new File("/Users/hs/Desktop/grobid_hsp/grobid-home/trainTmp/pdf/" + docId + ".pdf"));
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
                    i++;
                } else if (ratcliffObershelpDistance(clean(koReal), clean(koTarget), false) > 0.95) {
                    similarity++;
                } else {
                    System.out.println(docId);
                    if(copy){
                        try {
                            System.out.println(docId + ".pdf .. 불일치하여 trainTmp 폴더에 저장합니다");
                            FileUtils.copyFile(new File("/Users/hs/Desktop/pdfs/" + docId + ".pdf"), new File("/Users/hs/Desktop/grobid_hsp/grobid-home/trainTmp/pdf/" + docId + ".pdf"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (StringUtils.isNotEmpty(koTarget)) {
                        System.out.println("------------error case(kr)------------");
                        System.out.println("expected        : " + koReal);
                        System.out.println("expected(clean) : " + clean(koReal));
                        System.out.println("result          : " + koTarget);
                        System.out.println("result(clean)   : " + clean(koTarget));
                    }
                }
            }
            if (enReal != null) {
                if (enReal.contains("$") || enReal.contains("$")) {
                    System.out.println("$");
                }
                n++;
                if (clean(enReal).equalsIgnoreCase(clean(enTarget))){
                    i++;
                } else if (ratcliffObershelpDistance(clean(enReal), clean(enTarget), false) > 0.95) {
                    similarity++;
                } else{
                    System.out.println(docId);
//                    if (copy) {
//                        try {
//                            System.out.println(docId + ".pdf .. 불일치하여 trainTmp 폴더에 저장합니다");
//                            FileUtils.copyFile(new File("/Users/hs/Desktop/pdfs/" + docId + ".pdf"), new File("/Users/hs/Desktop/grobid_hsp/grobid-home/trainTmp/pdf/" + docId + ".pdf"));
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
                    if (StringUtils.isNotEmpty(enTarget)) {
                        System.out.println("------------error case(en)------------");
                        System.out.println("expected        : " + enReal);
                        System.out.println("expected(clean) : " + clean(enReal));
                        System.out.println("result          : " + enTarget);
                        System.out.println("result(clean)   : " + clean(enTarget));
                    }
//                    new File("/Users/hs/Desktop/pdfs/" + docId + ".pdf").delete();
                }
            }
        }
        System.out.println(n+"개 중, " +i+"개 일치합니다.");
        System.out.println(n+"개 중, " +similarity+"개는 95% 이상의 유사도를 가지고 있습니다.");
        System.out.println(n+"개 중, " +(n-i-similarity)+"개가 틀렸습니다.");

        Pair<Integer, Integer> result = new Pair<>(i+similarity, n);

        return result;
    }

    private static Pair<Integer, Integer> compareOneLang(Map<Map<String, String>, String> map, boolean copy) {
        int i = 0;
        int n = 0;
        int similarity = 0;

        for (Map.Entry<Map<String, String>, String> entry : map.entrySet()) {
            String real = entry.getKey().get("authr");
            String target = entry.getValue();
            String docId = entry.getKey().get("doc_id");
            if (real != null) {
                n++;
                if (clean(real).equalsIgnoreCase(clean(target))) {
                    i++;
                } else if (ratcliffObershelpDistance(clean(real), clean(target), false) > 0.95) {
                    similarity++;
                } else {
                    try {
                        if (target != null && !target.equals("") && copy) {
                            System.out.println(docId + ".pdf .. 불일치하여 trainTmp 폴더에 저장합니다");
                            FileUtils.copyFile(new File("/Users/hs/Desktop/pdfs/" + docId + ".pdf"), new File("/Users/hs/Desktop/grobid_hsp/grobid-home/trainTmp/pdf/" + docId + ".pdf"));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (StringUtils.isNotEmpty(target)) {
                        System.out.println("------------error case------------");
                        System.out.println("docId           : " + docId);
                        System.out.println("expected        : " + real);
                        System.out.println("expected(clean) : " + clean(real));
                        System.out.println("result          : " + target);
                        System.out.println("result(clean)   : " + clean(target));
                    }
                }
            }
        }
        
        System.out.println(n+"개 중, " +i+"개 일치합니다.");
        System.out.println(n+"개 중, " +similarity+"개는 95% 이상의 유사도를 가지고 있습니다.");
        System.out.println(n+"개 중, " +(n-i-similarity)+"개가 틀렸습니다.");

        return new Pair<>(i+similarity, n);
    }
    
    private static String clean(String str){
        if (str == null) {
            return "";
        }
        if (str.matches("\\{\\\\[a-zA-Z]+\\}")) {
            str = str.replaceAll("\\{\\\\[a-zA-Z]+\\}", "");
        }
        if (str.matches(".*\\$.*\\$.*"))
            str = str.replaceAll("\\$\\{[a-zA-Z]+}\\$", "");
        str = str.replaceAll("<TEX>", "");
        str = str.replaceAll("</TEX>", "");
        str = str.replaceAll("<tex>", "");
        str = str.replaceAll("</tex>", "");
        
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

    private static String translate(String text) {
        StringBuilder sb = new StringBuilder();
        StringBuilder chinese = new StringBuilder();
        StringBuilder korean = new StringBuilder();
        Boolean isChinese = false;
        for(int i = 0; i < text.length(); i++) {
            int charAt = (int)text.charAt(i);
            String result = map.get(String.valueOf(text.charAt(i)));
//            if((charAt >= '\u2E80' && charAt <= '\u2EFF') || (charAt >= '\u3400' && charAt <= '\u4DB5') || (charAt >= '\u4E00' && charAt <= '\u9FBF')) {
            if(result != null) {
                korean.append(result);
                chinese.append(text.charAt(i));
                isChinese = true;
                if (i == text.length() - 1) {
                    sb.append(korean);
                    sb.append("(");
                    sb.append(chinese);
                    sb.append(")");
                }
            } else {
                if (isChinese) {
                    sb.append(korean);
                    sb.append("(");
                    sb.append(chinese);
                    sb.append(")");
                    chinese = new StringBuilder();
                    korean = new StringBuilder();
                }
                sb.append(text.charAt(i));
                isChinese = false;
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
        File file = new File("/Users/hs/Desktop/grobid_hsp/grobid-home/trainTmp/trainData");
        File[] files = file.listFiles();
        for (File f : files) {
//            if (StringUtils.endsWith(f.getName(), "header") || StringUtils.endsWith(f.getName(), "header.tei.xml")
//                || StringUtils.endsWith(f.getName(), "segmentation") || StringUtils.endsWith(f.getName(), "segmentation.tei.xml")) {
//            if (StringUtils.endsWith(f.getName(), "segmentation") || StringUtils.endsWith(f.getName(), "segmentation.tei.xml")) {
//            if (StringUtils.endsWith(f.getName(), "fulltext") || StringUtils.endsWith(f.getName(), "fulltext.tei.xml")) {
            if (StringUtils.endsWith(f.getName(), "table") || StringUtils.endsWith(f.getName(), "table.tei.xml")) {
//            if (StringUtils.endsWith(f.getName(), "figure") || StringUtils.endsWith(f.getName(), "figure.tei.xml")) {
//            if (StringUtils.endsWith(f.getName(), "header") || StringUtils.endsWith(f.getName(), "header.tei.xml")) {
                
            } else{
                f.delete();
            }
        }
    }
    
    @Test
    public void hanja() throws FileNotFoundException {
        String text = "王氷의 陰陽五行理論의 易學的 활용에 대한 연구";

        System.out.println(translate(text));
//        StringBuilder sb = new StringBuilder();
//        StringBuilder chinese = new StringBuilder();
//        StringBuilder korean = new StringBuilder();
//        Boolean isChinese = false;
//        ArrayList<Integer> startIndex = new ArrayList<>();
//        for(int i = 0; i < text.length(); i++) {
//            int charAt = (int)text.charAt(i);
//            if((charAt >= '\u2E80' && charAt <= '\u2EFF') || (charAt >= '\u3400' && charAt <= '\u4DB5') || (charAt >= '\u4E00' && charAt <= '\u9FBF')) {
//                String result = map.get(String.valueOf(text.charAt(i)));
//                korean.append(result);
//                chinese.append(text.charAt(i));
//                isChinese = true;
//                if (i == text.length() - 1) {
//                    sb.append(korean);
//                    sb.append("(");
//                    sb.append(chinese);
//                    sb.append(")");
//                }
//            } else {
//                if (isChinese) {
//                    sb.append(korean);
//                    sb.append("(");
//                    sb.append(chinese);
//                    sb.append(")");
//                    chinese = new StringBuilder();
//                    korean = new StringBuilder();
//                }
//                sb.append(text.charAt(i));
//                isChinese = false;
//            }
//        }
//
//        System.out.println(sb.toString());


//        interpreter.execfile(GrobidProperties.getGrobidHome() + "/grobid-home/hanja/impl.py");
//        interpreter.exec("translate(\'大韓民國\', \'combination-text\')");
    }









    public List<LayoutToken> reTokenizeKrAuthor(List<LayoutToken> tokens) {
        if (tokens == null || tokens.size() == 0) {
            return tokens;
        }
        List<LayoutToken> originTokens = new ArrayList<>();
        for (LayoutToken token : tokens) {
            originTokens.add(new LayoutToken(token));
        }
        List<LayoutToken> resultTokens = new ArrayList<>();
        List<String> nameList = new ArrayList<>();
//        Map<String, Pair<Integer, Integer>> nameEndIndexMap = Collections.synchronizedMap(new LinkedHashMap<>());
        Map<String, Pair<Integer, Integer>> nameEndIndexMap = new LinkedHashMap<>();
        StringBuilder name = new StringBuilder();
        int startTokenIndex = 0;
        int startNameIndex = -1;

        if(tokens.size() == 1 && tokens.get(0).getText().matches("[가-힣]+")){
            String t = tokens.get(0).getText();
            nameList.add(t.trim());
            startNameIndex = 0;
            startTokenIndex = 0;
            nameEndIndexMap.put(t, new Pair<>(0, 0));
        }

        for (int i = 0; i < tokens.size(); i++) {
            String text = tokens.get(i).t();
            String prevText = "";
            String nextText = "";
            if (i != 0)
                prevText = tokens.get(i - 1).t();
            if (i != tokens.size()-1)
                nextText = tokens.get(i + 1).t();

            if (text.matches("[가-힣]+")) {
                if(nameList.size() == 0 && name.length() == 0)
                    startTokenIndex = i;
                if(startNameIndex == -1)
                    startNameIndex = i;
                if (text.length() > 1 && name.length() == 0) {
                    nameList.add(text.trim());
                    nameEndIndexMap.put(text.trim(), new Pair<>(i, i));
                    startNameIndex = -1;
                    continue;
                }
                name.append(text);
            } else if (!prevText.matches("[가-힣]+") && !name.toString().equals("") && i >= 1) {
                nameList.add(name.insert(1, " ").toString().trim());
                nameEndIndexMap.put(name.toString().trim(), new Pair<>(startNameIndex, i-2));
                startNameIndex = -1;
                name = new StringBuilder();
            }
            if (i == tokens.size() - 1 && StringUtils.isNotEmpty(name.toString())) {
                nameList.add(name.insert(1, " ").toString().trim());
                if (i != tokens.size()-1)
                    nameEndIndexMap.put(name.toString().trim(), new Pair<>(startNameIndex, i-2));
                else
                    nameEndIndexMap.put(name.toString().trim(), new Pair<>(startNameIndex, i));

            }
        }

        if(nameList.size() == 0){
            return originTokens;
        }

        int nameIndex = 0;

        String targetName = nameList.get(nameIndex);


        ArrayList<List<LayoutToken>> lists = new ArrayList<>();

        for (Map.Entry<String, Pair<Integer, Integer>> entry : nameEndIndexMap.entrySet()) {
            Pair<Integer, Integer> range = entry.getValue();
            int b = range.getB();
            int a = range.getA();
            if(b - a == 0){
                LayoutToken targetToken = new LayoutToken(tokens.get(a));
                String tok = targetToken.getText();
                int len = tok.length();
                LayoutToken temp = new LayoutToken(targetToken);
                temp.setText(tok.substring(0, 1));
                temp.setWidth(temp.getWidth()/ len);
                targetToken.setText(tok.substring(1));
                targetToken.setX(targetToken.getX() + (targetToken.getWidth() / len));
                targetToken.setWidth((targetToken.getWidth() / len)*2);
                ArrayList<LayoutToken> tempTokens = new ArrayList<>();
                tempTokens.add(temp);
                tempTokens.add(targetToken);
                lists.add(tempTokens);

            } else {
                List<LayoutToken> tempToks = new ArrayList<>();
                for (int i = a; i <= b; i++) {
                    tempToks.add(new LayoutToken(tokens.get(i)));
                }
                LayoutToken combinedToken = LayoutTokensUtil.combineTokens(tempToks, true);
                String tok = combinedToken.getText();
                int len = tok.length();
                LayoutToken temp = new LayoutToken(combinedToken);
                temp.setText(tok.substring(0, 1));
                temp.setWidth(temp.getWidth()/ len);
                combinedToken.setText(tok.substring(1));
                combinedToken.setX(combinedToken.getX() + (combinedToken.getWidth() / len));
                combinedToken.setWidth((combinedToken.getWidth() / len)*2);
                ArrayList<LayoutToken> tempTokens = new ArrayList<>();
                tempTokens.add(temp);
                tempTokens.add(combinedToken);
                lists.add(tempTokens);
            }
        }

        int targetIndex = 0;
        for (int i = 0; i < tokens.size(); i++) {
            String n = nameList.get(targetIndex);
            Pair range = nameEndIndexMap.get(n);
            if (range == null) {
                return originTokens;
            }
            if (i >= (Integer) range.getA() && i <= (Integer) range.getB()) {
                resultTokens.addAll(lists.get(targetIndex));
                i = (Integer) range.getB();
                if (targetIndex != nameList.size()-1)
                    targetIndex++;
            } else{
                resultTokens.add(tokens.get(i));
            }

        }
        tokens.clear();
        tokens.addAll(resultTokens);

        return resultTokens;
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
