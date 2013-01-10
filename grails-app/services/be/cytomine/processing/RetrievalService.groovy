package be.cytomine.processing

import be.cytomine.AnnotationDomain
import be.cytomine.Exception.ServerException
import be.cytomine.image.server.RetrievalServer
import be.cytomine.ontology.Ontology
import be.cytomine.ontology.Term
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import be.cytomine.test.HttpClient
import be.cytomine.utils.RetrievalHttpUtils
import be.cytomine.utils.ValueComparator
import grails.converters.JSON
import groovy.sql.Sql
import groovyx.gpars.Asynchronizer
import org.apache.log4j.Logger
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.model.NotFoundException

class RetrievalService {

    static transactional = true
    def projectService
    def grailsApplication
    def dataSource

    private long printTimeAndReset(long timestamp, String name) {
        println "$name=${System.currentTimeMillis()-timestamp}ms"
        return System.currentTimeMillis()
    }

    //=>imageinstance service
    public List<Long> getAllProjectId(Ontology ontology) {
        String request = "SELECT p.id FROM project p WHERE ontology_id="+ontology.id
        def data = []
        new Sql(dataSource).eachRow(request) {
            data << it[0]
        }
        return data
    }

    /**
     * Search similar annotation and best term for an annotation
     * @param project project which will provide annotation learning set
     * @param annotation annotation to search
     * @return [annotation: #list of similar annotation#, term: #map with best term#]
     * @throws Exception
     */
    def listSimilarAnnotationAndBestTerm(Project project, AnnotationDomain annotation) throws Exception {
        def data = [:]

        if(annotation.location.numPoints<3) {
            data.term = []
            return data
        }

        //find project used for retrieval
        List<Long> projectSearch = []
        if(project.retrievalDisable) return data
        else if(project.retrievalAllOntology)
            projectSearch=getAllProjectId(annotation.project.ontology)
        else projectSearch=project.retrievalProjects.collect {it.id}

        //Get similar annotation
        def similarAnnotations = loadAnnotationSimilarities(annotation,projectSearch)
        data.annotation = similarAnnotations

        //Get all term from project
        def projectTerms = project.ontology.terms()
        def bestTermNotOrdered = getTermMap(projectTerms)
        ValueComparator bvc = new ValueComparator(bestTermNotOrdered);

        //browse annotation
        similarAnnotations.each { similarAnnotation ->
            //for each annotation, browse annotation terms
            def terms = similarAnnotation.terms()
            terms.each { term ->
                if (projectTerms.contains(term)) {
                    Double oldValue = bestTermNotOrdered.get(term)
                    //for each term, add similarity value
                    bestTermNotOrdered.put(term, oldValue + similarAnnotation.similarity)
                }
            }
        }

        //Sort [term:rate] by rate (desc)
        TreeMap<Term, Double> bestTerm = new TreeMap(bvc);
        bestTerm.putAll(bestTermNotOrdered)
        def bestTermList = []

        //Put them in a list
        for (Map.Entry<Term, Double> entry: bestTerm.entrySet()) {
            Term term = entry.getKey()
            term.rate = entry.getValue()
            bestTermList << term
        }
        data.term = bestTermList
        return data
    }

    def getTermMap(List<Term> termList) {
        def map = [:]
        termList.each {
            map.put(it, 0d)
        }
        map
    }

    def loadAnnotationSimilarities(AnnotationDomain searchAnnotation,List<Long> projectSearch) {
        log.info "get similarities for userAnnotation " + searchAnnotation.id + " on " + projectSearch
        RetrievalServer server = RetrievalServer.findByDescription("retrieval")
        def response = RetrievalHttpUtils.getPostSearchResponse(server.url,'/retrieval-web/api/retrieval/search.json', searchAnnotation, searchAnnotation.getCropUrl(grailsApplication.config.grails.serverURL),projectSearch)
        println "response=$response"
        def json = JSON.parse(response)
        println "json=$json"
        def result =  readRetrievalResponse(searchAnnotation,json)
        return result
    }

    private def readRetrievalResponse(AnnotationDomain searchAnnotation,def responseJSON) {
        def data = []
        for (int i = 0; i < responseJSON.length(); i++) {
            def annotationjson = responseJSON.get(i)  //{"id":6754,"url":"http://beimport java.util.concurrent.Futureta.cytomine.be:48/api/annotation/6754/crop.jpg","sim":6.922589484181173E-6},{"id":5135,"url":"http://beta.cytomine.be:48/api/annotation/5135/crop.jpg","sim":6.912057598973113E-6}]

            try {
                UserAnnotation annotation = UserAnnotation.read(annotationjson.id)
                if (annotation && annotation.id != searchAnnotation.id) {
                    projectService.checkAuthorization(annotation.project)
                    annotation.similarity = new Double(annotationjson.sim)
                    data << annotation
                }
            }
            catch (AccessDeniedException ex) {log.info "User cannot have access to this userAnnotation"}
            catch (NotFoundException ex) {log.info "User cannot have access to this userAnnotation"}
        }
        return data
    }


    public static def indexAnnotationSynchronous(String json, String url) {
        Logger.getLogger(this).info("index synchronous json")
        Logger.getLogger(this).info("url = " + url)
        String res = "/retrieval-web/api/resource.json"
        RetrievalHttpUtils.getPostResponse(url, res, json)
    }

    public static def indexAnnotationSynchronous(Long id) {
        Logger.getLogger(this).info("index synchronous id")
        RetrievalServer server = RetrievalServer.findByDescription("retrieval")
        String res = "/retrieval-web/api/resource.json"
        RetrievalHttpUtils.getPostResponse(server.url, res, UserAnnotation.read(id))
    }

    public static def deleteAnnotationSynchronous(Long id) {
        Logger.getLogger(this).info("delete synchronous")
        RetrievalServer server = RetrievalServer.findByDescription("retrieval")
        String res = "/retrieval-web/api/resource/"+id+".json"
        RetrievalHttpUtils.getDeleteResponse(server.url,res)
    }

    public static def deleteContainerSynchronous(Long id) {
        Logger.getLogger(this).info("delete container synchronous")
        RetrievalServer server = RetrievalServer.findByDescription("retrieval")
        String res = "/retrieval-web/api/container/" + id + ".json"
        RetrievalHttpUtils.getDeleteResponse(server.url,res)
    }

    public static def updateAnnotationSynchronous(Long id) {
        Logger.getLogger(this).info("update synchronous")
        deleteAnnotationSynchronous(id)
        indexAnnotationSynchronous(id)
    }

    public static def indexAnnotationAsynchronous(AnnotationDomain annotation,RetrievalServer server) {
        //indexAnnotationSynchronous(annotation)
        Logger.getLogger(this).info("index asynchronous")
        String url = server.url
        def json = annotation.encodeAsJSON()

        Asynchronizer.withAsynchronizer() {
            Closure indexAnnotation = {
                try {
                indexAnnotationSynchronous(json,url)
            } catch (Exception e) {throw new ServerException("Retrieval Exception: "+e)}}
            Closure annotationIndexing = indexAnnotation.async()  //create a new closure, which starts the original closure on a thread pool
            annotationIndexing()
        }
    }

    public static def deleteAnnotationAsynchronous(Long id) {
        Logger.getLogger(this).info("delete asynchronous")
        Asynchronizer.withAsynchronizer() {
            Closure deleteAnnotation = {
                try {
                    deleteAnnotationSynchronous(id)
                } catch (Exception e) {e.printStackTrace()}
            }
            Closure annotationDeleting = deleteAnnotation.async()  //create a new closure, which starts the original closure on a thread pool
            annotationDeleting()
        }
    }

    public static def deleteContainerAsynchronous(Long id) {
        Logger.getLogger(this).info("delete asynchronous")
        Asynchronizer.withAsynchronizer() {
            Closure deleteContainer = {
                try {
                    deleteContainerSynchronous(id)
                } catch (Exception e) {e.printStackTrace()}
            }
            Closure containerDeleting = deleteContainer.async()  //create a new closure, which starts the original closure on a thread pool
            containerDeleting()
        }
    }


    public static def updateAnnotationAsynchronous(Long id) {
        Logger.getLogger(this).info("update asynchronous")
        Asynchronizer.doParallel() {
            Closure deleteAnnotation = {
                try {
                    deleteAnnotationSynchronous(id)
                    indexAnnotationSynchronous(id)
                } catch (Exception e) {e.printStackTrace()}
            }
            Closure annotationUpdating = deleteAnnotation.async()  //create a new closure, which starts the original closure on a thread pool
            annotationUpdating()
        }
    }


    public void indexMissingAnnotation() {
        //Get indexed resources
        List<Long> resources = getIndexedResource()
        //Check if each annotation is well indexed
        def annotations = UserAnnotation.list()
        int i = 1
        annotations.each { annotation ->
            log.debug "Annotation $i/" + annotations.size()
            if (!resources.contains(annotation.id)) {
                log.debug "Annotation $annotation.id IS NOT INDEXED"
                try {indexAnnotationSynchronous(annotation)} catch (Exception e) {e.printStackTrace()}
            } else {
                log.debug "Annotation $annotation.id IS INDEXED"
            }
            i++
        }
    }

    List<Long> getIndexedResource() {
        RetrievalServer server = RetrievalServer.findByDescription("retrieval")
        String URL = server.url + ".json"
        List json = JSON.parse(getGetResponse(URL))
        List<Long> resources = new ArrayList<Long>()
        json.each { subserver ->
            subserver.each { resource ->
                log.debug "resource=" + Long.parseLong(resource.key)
                resources.add(Long.parseLong(resource.key))
            }
        }
        resources
    }

    public static String getGetResponse(String URL) {
        HttpClient client = new HttpClient();
        client.connect(URL, "xxx", "xxx");
        client.get()
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect();
        return response
    }

}
