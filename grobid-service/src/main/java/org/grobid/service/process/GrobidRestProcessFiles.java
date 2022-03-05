package org.grobid.service.process;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.PatentItem;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.factory.GrobidPoolingFactory;
import org.grobid.core.layout.*;
import org.grobid.core.main.batch.GrobidMain;
import org.grobid.core.meta.BiblVO;
import org.grobid.core.meta.MetaVO;
import org.grobid.core.meta.PositionVO;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.IOUtilities;
import org.grobid.core.utilities.KeyGen;
import org.grobid.core.visualization.BlockVisualizer;
import org.grobid.core.visualization.CitationsVisualizer;
import org.grobid.core.visualization.FigureTableVisualizer;
import org.grobid.service.exceptions.GrobidServiceException;
import org.grobid.service.util.BibTexMediaType;
import org.grobid.service.util.ExpectedResponseType;
import org.grobid.service.util.GrobidRestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.*;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;

/**
 * Web services consuming a file
 */
@Singleton
public class GrobidRestProcessFiles {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidRestProcessFiles.class);

    @Inject
    public GrobidRestProcessFiles() {

    }

    /**
     * Uploads the origin document which shall be extracted into TEI and
     * extracts only the header data.
     *
     * @param inputStream the data of origin document
     * @param consolidate consolidation parameter for the header extraction
     * @return a response object which contains a TEI representation of the header part
     */
    public Response processStatelessHeaderDocument(
        final InputStream inputStream,
        final int consolidate,
        final boolean includeRawAffiliations,
        ExpectedResponseType expectedResponseType
    ) {
        LOGGER.debug(methodLogIn());
        String retVal = null;
        Response response = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md); 

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();

            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written. ", Status.INTERNAL_SERVER_ERROR);
            } 

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            BiblioItem result = new BiblioItem();

            // starts conversion process
            retVal = engine.processHeader(
                originFile.getAbsolutePath(),
                md5Str,
                consolidate,
                includeRawAffiliations,
                result
            );

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Response.Status.NO_CONTENT).build();
            } else if (expectedResponseType == ExpectedResponseType.BIBTEX) {
                response = Response.status(Response.Status.OK)
                    .entity(result.toBibTeX("-1"))
                    .header(HttpHeaders.CONTENT_TYPE, BibTexMediaType.MEDIA_TYPE + "; charset=UTF-8")
                    .build();
            } else {
                response = Response.status(Response.Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    public int trainPdf(){
        final String trainTmp = "/trainTmp";
        String homePath = GrobidProperties.getGrobidHome().getPath();
        String[] args = {"-gH", homePath, "-dIn", homePath + trainTmp + "/pdf", "-dOut", homePath + trainTmp + "/trainData", "-r", "-exe", "createTraining", "-service"};
        try {
            GrobidMain.main(args);
            return 1;
        } catch (Exception e) {
            LOGGER.error("train error", e);
            return 0;
        }
    }

    public Response getMetaData(Map<String, InputStream> paramMap) throws IOException {
        LOGGER.debug(methodLogIn());
        String headerRetVal = null;
        Document doc;
        Response response = null;
        File originFile = null;
        Engine engine = null;
        List<BibDataSet> bibDataSetList = null;
        List<MetaVO> metaVOS = new ArrayList<>();
        int lastNum = 0;
        try {

            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            for (Map.Entry<String, InputStream> map : paramMap.entrySet()) {
                String fileName = map.getKey();
                InputStream inputStream = map.getValue();
                MessageDigest md = MessageDigest.getInstance("MD5");
                DigestInputStream dis = new DigestInputStream(inputStream, md);

                originFile = IOUtilities.writeInputFile(dis);


                final String trainTmp = "/trainTmp/pdf";

                Boolean isDuplicate = false;

                File dir = new File(GrobidProperties.getGrobidHome().getPath()+trainTmp);
                File doneDir = new File(GrobidProperties.getGrobidHome().getPath()+trainTmp+"/done");
                File[] files = dir.listFiles();
                File[] doneFiles = doneDir.listFiles();
                if(files != null && files.length > 0) {
                    for (File file : files) {
                        if (fileName.equals(file.getName())) {
                            isDuplicate = true;
                        }
                    }
                }
                if(doneFiles != null && doneFiles.length >0){
                    for (File file : doneFiles) {
                        if (fileName.equals(file.getName())) {
                            isDuplicate = true;
                        }
                    }
                }
                if(!isDuplicate){
                    try {
                        FileInputStream copyStream = new FileInputStream(originFile);
                        FileOutputStream destStream = new FileOutputStream(GrobidProperties.getGrobidHome().getPath() + trainTmp + "/" + fileName);

                        FileChannel fcin = copyStream.getChannel();
                        FileChannel fcout = destStream.getChannel();

                        long size=0;
                        size = fcin.size();
                        fcin.transferTo(0, size, fcout);

                        fcout.close();
                        fcin.close();
                        copyStream.close();
                        destStream.close();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

//                    Files.copy(originFile.toPath(), Paths.get(GrobidProperties.getGrobidHome().getPath() + trainTmp + "/" + fileName));
                }


                byte[] digest = md.digest();

                if (originFile == null) {
                    LOGGER.error("The input file cannot be written.");
                    throw new GrobidServiceException(
                        "The input file cannot be written. ", Status.INTERNAL_SERVER_ERROR);
                }

                String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();
                String assetPath = GrobidProperties.getTempPath().getPath() + File.separator + KeyGen.getKey();


                GrobidAnalysisConfig config =
                    GrobidAnalysisConfig.builder()
                        .pdfAssetPath(new File(assetPath))
                        .build();

                BiblioItem result = new BiblioItem();

                doc = engine.fullTextToTEIDoc(originFile, md5Str, config);

                result = doc.getResHeader();
                bibDataSetList = doc.getBibDataSets();

                LanguageDetector detector = LanguageDetectorBuilder.fromLanguages(Language.KOREAN, Language.ENGLISH).build();



                MetaVO metaVO = new MetaVO();
                if(result.getTitle() != null){
                    metaVO.setTitle(result.getTitle());
                }
                if(result.getEnglishTitle() != null){
                    metaVO.setTitle(result.getEnglishTitle());
                }
                if(result.getAbstract() != null){
                    metaVO.setAbstract(result.getAbstract());
                }
                if(result.getOriginalAuthors() != null){
                    metaVO.setAuthor(result.getOriginalAuthors());
                }
                if(result.getKeywords() != null){
                    metaVO.setKeyword(result.getKeywords());
                }
                metaVO.setDoi(result.getDOI());
                if(result.getSubmission() != null){
                    metaVO.setSubmission(result.getSubmission());
                }
                if(result.getReference() != null){
                    metaVO.setReference(result.getReference().replaceAll("\n", ""));
                }
                if (result.getCopyright() != null) {
                    metaVO.setCopyright(result.getCopyright());
                }
                if (doc.getFigures() != null){
                    metaVO.setFigures(doc.getFigures());
                }
                if (doc.getTables() != null) {
                    metaVO.setTables(doc.getTables());
                }
                if (result.getFullAffiliations() != null) {
                    metaVO.setAffiliation(result.getFullAffiliations());
                }
                metaVO.setAssetPath(assetPath);

                metaVO.setEmail(result.getEmail());


    //            List<BibDataSet> bibDataSetList = engine.processReferences(originFile, md5Str, 0);

                List<BiblVO> biblVOS = new ArrayList<>();

                int biblId = 1;

                for (BibDataSet bibDataSet : bibDataSetList) {
                    BiblVO biblVO = new BiblVO();
                    biblVO.setId(biblId++);
                    biblVO.setTitle(bibDataSet.getResBib().getTitle() == null ? bibDataSet.getResBib().getBookTitle() : bibDataSet.getResBib().getTitle());
                    biblVO.setReport(bibDataSet.getResBib().getBookType());
                    biblVO.setPublisher(bibDataSet.getResBib().getPublisher());
                    biblVO.setDoi(bibDataSet.getResBib().getDOI());
                    if(bibDataSet.getResBib().getFullAuthors() != null){
                        biblVO.setAuthors(bibDataSet.getResBib().getOriginalAuthors());
                    }
                    biblVO.setEditor(bibDataSet.getResBib().getEditors());
                    biblVO.setPubYear(bibDataSet.getResBib().getPublicationDate());
                    biblVO.setPubPlace(bibDataSet.getResBib().getLocation());
                    biblVO.setPageRange(bibDataSet.getResBib().getPageRange());
                    biblVO.setJournalTitle(bibDataSet.getResBib().getJournal());
                    biblVO.setIssue(bibDataSet.getResBib().getIssue());
                    biblVO.setVolume(bibDataSet.getResBib().getVolumeBlock());
                    biblVO.setRawText(bibDataSet.getRawBib().replaceAll("\n",""));
                    biblVO.setInstitution(bibDataSet.getResBib().getInstitution());
                    biblVO.setUrl(bibDataSet.getResBib().getWeb());

                    biblVOS.add(biblVO);
                }

                metaVO.setBiblList(biblVOS);
                metaVOS.add(metaVO);
            }

            response = Response.status(Response.Status.OK)
                .entity(metaVOS)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                .build();
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Uploads the origin document which shall be extracted into TEI.
     *
     * @param inputStream          the data of origin document
     * @param consolidateHeader    the consolidation option allows GROBID to exploit Crossref
     *                             for improving header information
     * @param consolidateCitations the consolidation option allows GROBID to exploit Crossref
     *                             for improving citations information
     * @param startPage            give the starting page to consider in case of segmentation of the
     *                             PDF, -1 for the first page (default)
     * @param endPage              give the end page to consider in case of segmentation of the
     *                             PDF, -1 for the last page (default)
     * @param generateIDs          if true, generate random attribute id on the textual elements of
     *                             the resulting TEI
     * @return a response object mainly contain the TEI representation of the
     * full text
     */
    public Response processFulltextDocument(final InputStream inputStream,
                                          final int consolidateHeader,
                                          final int consolidateCitations,
                                          final boolean includeRawAffiliations,
                                          final boolean includeRawCitations,
                                          final int startPage,
                                          final int endPage,
                                          final boolean generateIDs,
                                          final boolean segmentSentences,
                                          final List<String> teiCoordinates) throws Exception {
        LOGGER.debug(methodLogIn());

        String retVal = null;
        Response response = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md); 

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            // starts conversion process
            GrobidAnalysisConfig config =
                GrobidAnalysisConfig.builder()
                    .consolidateHeader(consolidateHeader)
                    .consolidateCitations(consolidateCitations)
                    .includeRawAffiliations(includeRawAffiliations)
                    .includeRawCitations(includeRawCitations)
                    .startPage(startPage)
                    .endPage(endPage)
                    .generateTeiIds(generateIDs)
                    .generateTeiCoordinates(teiCoordinates)
                    .withSentenceSegmentation(segmentSentences)
                    .build();

            retVal = engine.fullTextToTEI(originFile, md5Str, config);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Response.Status.NO_CONTENT).build();
            } else {
                response = Response.status(Response.Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }

            if (originFile != null)
              IOUtilities.removeTempFile(originFile);
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    public Response processRange(InputStream inputStream) {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        File originFile = null;
        Engine engine = null;
        String assetPath = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md);

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            }

            // set the path for the asset files
            assetPath = GrobidProperties.getTempPath().getPath() + File.separator + KeyGen.getKey();

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            // starts conversion process
            GrobidAnalysisConfig config =
                GrobidAnalysisConfig.builder()
                    .pdfAssetPath(new File(assetPath))
                    .build();

            Document imageDoc = engine.fullTextToTEIDoc(originFile, md5Str, config);

            Map<String, Collection<DocumentPiece>> lb = imageDoc.getLabeledBlocks().asMap();
            List<Page> pages = imageDoc.getPages();
            int pageSize = pages.size();
            HashMap<Integer, List<PositionVO>> rangeMap = new HashMap<>();
            for (int i = 0; i < pageSize; i++) {
                rangeMap.put(i + 1, new ArrayList<PositionVO>());
            }
            ArrayList<PositionVO> pageSizeTemp = new ArrayList<>();
            pageSizeTemp.add(new PositionVO(0, imageDoc.getPages().get(0).getWidth(), 0.0, imageDoc.getPages().get(0).getHeight(), 0.0));
            rangeMap.put(0, pageSizeTemp);

            List<LayoutToken> tokens = imageDoc.getTokenizations();

            for (Map.Entry<String, Collection<DocumentPiece>> l : lb.entrySet()) {
                if (l.getKey().equals("<headnote>") || l.getKey().equals("<footnote>") || l.getKey().equals("<header>") || l.getKey().equals("<page>")) {
                    addPositionVO(l, tokens, rangeMap, pages);
                }
            }

            if(rangeMap.size() == 0){
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response
                    .ok()
                    .type("application/json")
                    .entity(rangeMap)
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (assetPath != null) {
                IOUtilities.removeTempDirectory(assetPath);
            }

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;

    }

    public Response processBodyImages(InputStream inputStream) {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        File originFile = null;
        Engine engine = null;
        String assetPath = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md);

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            }

            // set the path for the asset files
            assetPath = GrobidProperties.getTempPath().getPath() + File.separator + KeyGen.getKey();

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            // starts conversion process
            GrobidAnalysisConfig config =
                GrobidAnalysisConfig.builder()
                    .pdfAssetPath(new File(assetPath))
                    .build();

            Document imageDoc = engine.fullTextToTEIDoc(originFile, md5Str, config);

            Map<String, Collection<DocumentPiece>> lb = imageDoc.getLabeledBlocks().asMap();
            List<Page> pages = imageDoc.getPages();
            int pageSize = pages.size();
            HashMap<Integer, List<PositionVO>> rangeMap = new HashMap<>();
            for (int i = 0; i < pageSize; i++) {
                rangeMap.put(i + 1, new ArrayList<PositionVO>());
            }

            List<LayoutToken> tokens = imageDoc.getTokenizations();

            for (Map.Entry<String, Collection<DocumentPiece>> l : lb.entrySet()) {
                if (l.getKey().equals("<headnote>") || l.getKey().equals("<footnote>") || l.getKey().equals("<header>") || l.getKey().equals("<page>") || l.getKey().equals("<annex>")) {
                    addPositionVO(l, tokens, rangeMap, pages);
                }
            }
            List<GraphicObject> images = imageDoc.getImages();
            ArrayList<File> imageFiles = new ArrayList<>();

            // headernote, footnote, page, header, references
            int bodyStart = imageDoc.getLabeledBlocks().get("<body>").first().getLeft().getTokenDocPos();

            imageFor:
            for (GraphicObject image : images) {
                if (image.getType() == GraphicObjectType.BITMAP) {
                    if(image.getStartPosition() > bodyStart){
                        boolean out = false;
                        int page = image.getPage();
                        BigDecimal x1 = BigDecimal.valueOf(image.getX());
                        BigDecimal y1 = BigDecimal.valueOf(image.getY());
                        BigDecimal width = BigDecimal.valueOf(image.getWidth());
                        BigDecimal height = BigDecimal.valueOf(image.getHeight());
                        BigDecimal x2 = x1.add(width);
                        BigDecimal y2 = y1.add(height);
                        List<PositionVO> positionVOS = rangeMap.get(page);
                        if(positionVOS == null || positionVOS.size() == 0){
                            out = true;
                        }
                        for (PositionVO positionVO : positionVOS) {

                            if(x1.doubleValue() > positionVO.getX2() || x2.doubleValue() < positionVO.getX1() || y1.doubleValue() > positionVO.getY2() || y2.doubleValue() < positionVO.getY1()){
                                out=true;
                            }else{
                                out=false;
                                continue imageFor;
                            }
                        }
                        if(out){
                            String[] filePathSplit = image.getFilePath().split("/");
                            String fileName = filePathSplit[filePathSplit.length - 1];
                            imageFiles.add(new File(assetPath + File.separatorChar + fileName));
                        }
                    }
                }
            }


            if(imageFiles == null || imageFiles.size() == 0){
                response = Response.status(Status.NO_CONTENT).build();
            } else {

                response = Response.status(Status.OK).type("application/zip").build();

                ByteArrayOutputStream ouputStream = new ByteArrayOutputStream();
                ZipOutputStream out = new ZipOutputStream(ouputStream);

                byte[] buffer = new byte[1024];
                for (final File currFile : imageFiles) {
                    if (currFile.getName().toLowerCase().endsWith(".jpg")
                        || currFile.getName().toLowerCase().endsWith(".png")) {
                        if(Files.size(currFile.toPath()) < 5000){
                            continue;
                        }
                        try {
                            ZipEntry ze = new ZipEntry(currFile.getName());
                            out.putNextEntry(ze);
                            FileInputStream in = new FileInputStream(currFile);
                            int len;
                            while ((len = in.read(buffer)) > 0) {
                                out.write(buffer, 0, len);
                            }
                            in.close();
                            out.closeEntry();
                        } catch (IOException e) {
                            throw new GrobidServiceException("IO Exception when zipping", e, Status.INTERNAL_SERVER_ERROR);
                        }
                    }
                }
                out.finish();

                response = Response
                    .ok()
                    .type("application/zip")
                    .entity(ouputStream.toByteArray())
                    .header("Content-Disposition", "attachment; filename=\"result.zip\"")
                    .build();
                out.close();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (assetPath != null) {
                IOUtilities.removeTempDirectory(assetPath);
            }

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;

    }

    private void addPositionVO(Map.Entry<String, Collection<DocumentPiece>> l, List<LayoutToken> tokens, HashMap<Integer,
        List<PositionVO>> rangeMap, List<Page> pages) {

        Collection<DocumentPiece> header = l.getValue();
        for (DocumentPiece p : header) {
            int leftToken = p.getLeft().getTokenDocPos();
            int rightToken = p.getRight().getTokenDocPos()-2;
            LayoutToken leftTokenLayout = tokens.get(leftToken);
            int leftPage = leftTokenLayout.getPage();
            double x1 = Double.MAX_VALUE;
            double x2 = 0.0;
            double y1 = Double.MAX_VALUE;
            double y2 = 0.0;


            for(int i=leftToken; i<=rightToken; i++){
                LayoutToken layoutToken = tokens.get(i);
                BigDecimal bigX = new BigDecimal(layoutToken.getX());
                BigDecimal bigY = new BigDecimal(layoutToken.getY());
                BigDecimal bigWidth = new BigDecimal(layoutToken.getWidth());
                BigDecimal bigHeight = new BigDecimal(layoutToken.getHeight());

                if (bigX.equals(BigDecimal.valueOf(-1)) || bigY.equals(BigDecimal.valueOf(-1))) {
                    continue;
                }else{
                    String key = l.getKey();
                    if (Math.abs(y2 - layoutToken.getY()) > pages.get(1).getHeight() / 2 && y2 != 0.0) {
                        rangeMap.get(leftPage).add(new PositionVO(leftPage, x1,x2,y1,y2));
                        x1 = layoutToken.getX();
                        x2 = bigX.add(bigWidth).doubleValue();
                        y1 = layoutToken.getY();
                        y2 = bigY.add(bigHeight).doubleValue();
                    }
                    if (x1 > layoutToken.getX()) {
//                        x1 = BigDecimal.valueOf(layoutToken.getX()).subtract(BigDecimal.valueOf(10.0)).doubleValue();
                        x1 = layoutToken.getX();
                    }
                    if (x2 < bigX.add(bigWidth).doubleValue()) {
//                        x2 = bigX.add(bigWidth.add(BigDecimal.valueOf(10.0))).doubleValue();
                        x2 = bigX.add(bigWidth).doubleValue();
                    }
                    if (y1 > layoutToken.getY()) {
                        if (l.getKey().equals("<headnote>")) {
                            y1 = 0.0;
                        }else{
                            y1 = layoutToken.getY();
                        }
                    }
                    if (y2 < bigY.add(bigHeight).doubleValue()) {
                        if (l.getKey().equals("<footnote>")) {
                            y2 = Double.MAX_VALUE;
                        }else{
                            y2 = bigY.add(bigHeight).doubleValue();
                        }
                    }
                }
            }

            Page page = pages.get(leftPage - 1);
            BigDecimal pageY1 = BigDecimal.valueOf(page.getMainArea().getY());
            BigDecimal pageY2 = BigDecimal.valueOf(page.getMainArea().getY2());

            double pageCenter = pageY2.subtract(pageY1).divide(BigDecimal.valueOf(2.0)).doubleValue();


            if(l.getKey().equals("<headnote>")){
                if(y2 > pageCenter){
                    continue;
                }
            } else if (l.getKey().equals("<footnote>")) {
                if (y1 < pageCenter) {
                    continue;
                }
            } else if (l.getKey().equals("<header>")) {
                if (leftPage == 1){
                    if(!leftTokenLayout.getLabels().get(0).getLabel().equals("<submission>")){
                        y1 = 0;
                    }
                }
            }
            PositionVO pos = new PositionVO(leftPage, x1, x2, y1, y2);
            rangeMap.get(leftPage).add(pos);

        }
    }

    /**
     * Uploads the origin document which shall be extracted into TEI + assets in a ZIP
     * archive.
     *
     * @param inputStream          the data of origin document
     * @param consolidateHeader    the consolidation option allows GROBID to exploit Crossref
     *                             for improving header information
     * @param consolidateCitations the consolidation option allows GROBID to exploit Crossref
     *                             for improving citations information
     * @param startPage            give the starting page to consider in case of segmentation of the
     *                             PDF, -1 for the first page (default)
     * @param endPage              give the end page to consider in case of segmentation of the
     *                             PDF, -1 for the last page (default)
     * @param generateIDs          if true, generate random attribute id on the textual elements of
     *                             the resulting TEI
     * @return a response object mainly contain the TEI representation of the
     * full text
     */
    public Response processStatelessFulltextAssetDocument(final InputStream inputStream,
                                                          final int consolidateHeader,
                                                          final int consolidateCitations,
                                                          final boolean includeRawAffiliations,
                                                          final boolean includeRawCitations,
                                                          final int startPage,
                                                          final int endPage,
                                                          final boolean generateIDs,
                                                          final boolean segmentSentences) throws Exception {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        File originFile = null;
        Engine engine = null;
        String assetPath = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md); 

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            // set the path for the asset files
            assetPath = GrobidProperties.getTempPath().getPath() + File.separator + KeyGen.getKey();

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            // starts conversion process
            GrobidAnalysisConfig config =
                GrobidAnalysisConfig.builder()
                    .consolidateHeader(consolidateHeader)
                    .consolidateCitations(consolidateCitations)
                    .includeRawAffiliations(includeRawAffiliations)
                    .includeRawCitations(includeRawCitations)
                    .startPage(startPage)
                    .endPage(endPage)
                    .generateTeiIds(generateIDs)
                    .pdfAssetPath(new File(assetPath))
                    .withSentenceSegmentation(segmentSentences)
                    .build();

            retVal = engine.fullTextToTEI(originFile, md5Str, config);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {

                response = Response.status(Status.OK).type("application/zip").build();

                ByteArrayOutputStream ouputStream = new ByteArrayOutputStream();
                ZipOutputStream out = new ZipOutputStream(ouputStream);
                out.putNextEntry(new ZipEntry("tei.xml"));
                out.write(retVal.getBytes(Charset.forName("UTF-8")));
                // put now the assets, i.e. all the files under the asset path
                File assetPathDir = new File(assetPath);
                if (assetPathDir.exists()) {
                    File[] files = assetPathDir.listFiles();
                    if (files != null) {
                        byte[] buffer = new byte[1024];
                        for (final File currFile : files) {
                            if (currFile.getName().toLowerCase().endsWith(".jpg")
                                || currFile.getName().toLowerCase().endsWith(".png")) {
                                try {
                                    ZipEntry ze = new ZipEntry(currFile.getName());
                                    out.putNextEntry(ze);
                                    FileInputStream in = new FileInputStream(currFile);
                                    int len;
                                    while ((len = in.read(buffer)) > 0) {
                                        out.write(buffer, 0, len);
                                    }
                                    in.close();
                                    out.closeEntry();
                                } catch (IOException e) {
                                    throw new GrobidServiceException("IO Exception when zipping", e, Status.INTERNAL_SERVER_ERROR);
                                }
                            }
                        }
                    }
                }
                out.finish();

                response = Response
                    .ok()
                    .type("application/zip")
                    .entity(ouputStream.toByteArray())
                    .header("Content-Disposition", "attachment; filename=\"result.zip\"")
                    .build();
                out.close();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);
            
            if (assetPath != null) {
                IOUtilities.removeTempDirectory(assetPath);
            }
            
            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }


    /**
     * Process a patent document in PDF for extracting and parsing citations in the description body.
     *
     * @param inputStream the data of origin document
     * @return a response object mainly containing the TEI representation of the
     * citation
     */
    public Response processCitationPatentPDF(final InputStream inputStream,
                                             final int consolidate,
                                             final boolean includeRawCitations) throws Exception {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            originFile = IOUtilities.writeInputFile(inputStream);
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            // starts conversion process
            List<PatentItem> patents = new ArrayList<>();
            List<BibDataSet> articles = new ArrayList<>();
            retVal = engine.processAllCitationsInPDFPatent(originFile.getAbsolutePath(),
                                                           articles, patents, consolidate, includeRawCitations);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Process a patent document encoded in ST.36 for extracting and parsing citations in the description body.
     *
     * @param inputStream the data of origin document
     * @return a response object mainly containing the TEI representation of the
     * citation
     */
    public Response processCitationPatentST36(final InputStream inputStream,
                                              final int consolidate,
                                              final boolean includeRawCitations) throws Exception {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            originFile = IOUtilities.writeInputFile(inputStream);
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            // starts conversion process
            List<PatentItem> patents = new ArrayList<>();
            List<BibDataSet> articles = new ArrayList<>();
            retVal = engine.processAllCitationsInXMLPatent(originFile.getAbsolutePath(),
                    articles, patents, consolidate, includeRawCitations);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                //response = Response.status(Status.OK).entity(retVal).type(MediaType.APPLICATION_XML).build();
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);
            
            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Uploads the origin document, extract and parser all its references.
     *
     * @param inputStream          the data of origin document
     * @param consolidate          if the result has to be consolidated with CrossRef access.
     * @param includeRawCitations  determines whether the original citation (called "raw") should be included in the
     *                             output
     * @param expectedResponseType determines whether XML or BibTeX should be returned
     * @return a response object mainly contain the TEI representation of the full text
     */
    public Response processStatelessReferencesDocument(final InputStream inputStream,
                                                       final int consolidate,
                                                       final boolean includeRawCitations,
                                                       ExpectedResponseType expectedResponseType) {
        LOGGER.debug(methodLogIn());
        Response response;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md); 

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            // starts conversion process
            List<BibDataSet> bibDataSetList = engine.processReferences(originFile, md5Str, consolidate);

            if (bibDataSetList.isEmpty()) {
                response = Response.status(Status.NO_CONTENT).build();
            } else if (expectedResponseType == ExpectedResponseType.BIBTEX) {
                StringBuilder result = new StringBuilder();
                GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder().includeRawCitations(includeRawCitations).build();
                int p = 0;
                for (BibDataSet res : bibDataSetList) {
                    result.append(res.getResBib().toBibTeX(Integer.toString(p), config));
                    result.append("\n");
                    p++;
                }
                response = Response.status(Status.OK)
                                   .entity(result.toString())
                                   .header(HttpHeaders.CONTENT_TYPE, BibTexMediaType.MEDIA_TYPE + "; charset=UTF-8")
                                   .build();
            } else {
                StringBuilder result = new StringBuilder();
                // dummy header
                result.append("<TEI xmlns=\"http://www.tei-c.org/ns/1.0\" " +
                    "xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                    "\n xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">\n");
                result.append("\t<teiHeader/>\n\t<text>\n\t\t<front/>\n\t\t" +
                    "<body/>\n\t\t<back>\n\t\t\t<div>\n\t\t\t\t<listBibl>\n");
                int p = 0;
                for (BibDataSet bibDataSet : bibDataSetList) {
                    result.append(bibDataSet.toTEI(p, includeRawCitations));
                    result.append("\n");
                    p++;
                }
                result.append("\t\t\t\t</listBibl>\n\t\t\t</div>\n\t\t</back>\n\t</text>\n</TEI>\n");
                response = Response.status(Status.OK)
                                   .entity(result.toString())
                                   .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                                   .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Uploads the origin PDF, process it and return the PDF augmented with annotations.
     *
     * @param inputStream the data of origin PDF
     * @param fileName    the name of origin PDF
     * @param type        gives type of annotation
     * @return a response object containing the annotated PDF
     */
    public Response processPDFAnnotation(final InputStream inputStream,
                                         final String fileName,
                                         final int consolidateHeader,
                                         final int consolidateCitations,
                                         final boolean includeRawAffiliations,
                                         final boolean includeRawCitations,
                                         final GrobidRestUtils.Annotation type) throws Exception {
        LOGGER.debug(methodLogIn());
        Response response = null;
        PDDocument out = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            originFile = IOUtilities.writeInputFile(inputStream);
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            out = annotate(
                originFile, type, engine,
                consolidateHeader, consolidateCitations,
                includeRawAffiliations, includeRawCitations
            );
            if (out != null) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                out.save(outputStream);
                response = Response
                    .ok()
                    .type("application/pdf")
                    .entity(outputStream.toByteArray())
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .build();
            } else {
                response = Response.status(Status.NO_CONTENT).build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            try {
                out.close();
            } catch (IOException e) {
                LOGGER.error("An unexpected exception occurs. ", e);
                response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
            }

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }


    /**
     * Uploads the origin PDF, process it and return PDF annotations for references in JSON.
     *
     * @param inputStream the data of origin PDF
     * @return a response object containing the JSON annotations
     */
    public Response processPDFReferenceAnnotation(final InputStream inputStream,
                                                  final int consolidateHeader,
                                                  final int consolidateCitations,
                                                  final boolean includeRawCitations,
                                                  final boolean includeFiguresTables) throws Exception {
        LOGGER.debug(methodLogIn());
        Response response = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md); 

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            List<String> elementWithCoords = new ArrayList<>();
            elementWithCoords.add("ref");
            elementWithCoords.add("biblStruct");
            GrobidAnalysisConfig config = new GrobidAnalysisConfig
                .GrobidAnalysisConfigBuilder()
                .generateTeiCoordinates(elementWithCoords)
                .consolidateHeader(consolidateHeader)
                .consolidateCitations(consolidateCitations)
                .includeRawCitations(includeRawCitations)
                .build();

            Document teiDoc = engine.fullTextToTEIDoc(originFile, config);
            String json = CitationsVisualizer.getJsonAnnotations(teiDoc, null, includeFiguresTables);

            if (json != null) {
                response = Response
                    .ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                    .entity(json)
                    .build();
            } else {
                response = Response.status(Status.NO_CONTENT).build();
            }
            
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }
        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Annotate the citations in a PDF patent document with JSON annotations.
     *
     * @param inputStream the data of origin document
     * @return a response object mainly containing the TEI representation of the
     * citation
     */
    public Response annotateCitationPatentPDF(final InputStream inputStream,
                                              final int consolidate,
                                              final boolean includeRawCitations) throws Exception {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            originFile = IOUtilities.writeInputFile(inputStream);
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            // starts conversion process
            retVal = engine.annotateAllCitationsInPDFPatent(originFile.getAbsolutePath(), consolidate, includeRawCitations);
                    
            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                   .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }
        LOGGER.debug(methodLogOut());
        return response;
    }

    public String methodLogIn() {
        return ">> " + GrobidRestProcessFiles.class.getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    public String methodLogOut() {
        return "<< " + GrobidRestProcessFiles.class.getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    protected PDDocument annotate(File originFile, 
                                  final GrobidRestUtils.Annotation type, Engine engine,
                                  final int consolidateHeader,
                                  final int consolidateCitations,
                                  final boolean includeRawAffiliations,
                                  final boolean includeRawCitations) throws Exception {
        // starts conversion process
        PDDocument outputDocument = null;
        // list of TEI elements that should come with coordinates
        List<String> elementWithCoords = new ArrayList<>();
        if (type == GrobidRestUtils.Annotation.CITATION) {
            elementWithCoords.add("ref");
            elementWithCoords.add("biblStruct");
        } else if (type == GrobidRestUtils.Annotation.FIGURE) {
            elementWithCoords.add("figure");
        }

        GrobidAnalysisConfig config = new GrobidAnalysisConfig
            .GrobidAnalysisConfigBuilder()
            .consolidateHeader(consolidateHeader)
            .consolidateCitations(consolidateCitations)
            .includeRawAffiliations(includeRawAffiliations)
            .includeRawCitations(includeRawCitations)
            .generateTeiCoordinates(elementWithCoords)
            .build();

        DocumentSource documentSource = 
            DocumentSource.fromPdf(originFile, config.getStartPage(), config.getEndPage(), true, true, false);

        Document teiDoc = engine.fullTextToTEIDoc(documentSource, config);

        documentSource = 
            DocumentSource.fromPdf(originFile, config.getStartPage(), config.getEndPage(), true, true, false);

        PDDocument document = PDDocument.load(originFile);
        //If no pages, skip the document
        if (document.getNumberOfPages() > 0) {
            outputDocument = dispatchProcessing(type, document, documentSource, teiDoc);
        } else {
            throw new RuntimeException("Cannot identify any pages in the input document. " +
                "The document cannot be annotated. Please check whether the document is valid or the logs.");
        }
        
        documentSource.close(true, true, false);

        return outputDocument;
    }

    protected PDDocument dispatchProcessing(GrobidRestUtils.Annotation type, PDDocument document,
                                            DocumentSource documentSource, Document teiDoc
    ) throws Exception {
        PDDocument out = null;
        if (type == GrobidRestUtils.Annotation.CITATION) {
            out = CitationsVisualizer.annotatePdfWithCitations(document, teiDoc, null);
        } else if (type == GrobidRestUtils.Annotation.BLOCK) {
            out = BlockVisualizer.annotateBlocks(document, documentSource.getXmlFile(),
                teiDoc, true, true, false);
        } else if (type == GrobidRestUtils.Annotation.FIGURE) {
            out = FigureTableVisualizer.annotateFigureAndTables(document, documentSource.getXmlFile(),
                teiDoc, true, true, true, false, false);
        } 
        return out;
    }

}
