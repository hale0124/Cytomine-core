import be.cytomine.Exception.InvalidRequestException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.processing.JobParameter
import be.cytomine.processing.JobTemplate
import be.cytomine.processing.Software
import be.cytomine.processing.SoftwareParameter
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.test.BasicInstanceBuilder
import org.codehaus.groovy.grails.plugins.springsecurity.SecurityFilterPosition
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils

import grails.util.Environment
import jsondoc.JSONUtils

import java.lang.management.ManagementFactory

/**
 * Bootstrap contains code that must be execute during application (re)start
 */
class BootStrap {

    def grailsApplication
    def messageSource

    def sequenceService
    def marshallersService
    def indexService
    def triggerService
    def grantService
    def termService
    def tableService
    def secUserService

    def retrieveErrorsService
    def bootstrapTestDataService
    def bootstrapProdDataService
    def bootstrapUtilsService
    def javascriptService
    def dataSource
    def sessionFactory

    def init = { servletContext ->

        //Register API Authentifier
        log.info "Current directory2="+new File( 'test.html' ).absolutePath
        println "HeadLess:" +java.awt.GraphicsEnvironment.isHeadless();

        SpringSecurityUtils.clientRegisterFilter( 'apiAuthentificationFilter', SecurityFilterPosition.DIGEST_AUTH_FILTER.order + 1)
        log.info "###################" + grailsApplication.config.grails.serverURL + "##################"
        log.info "GrailsUtil.environment= " + Environment.getCurrent() + " BootStrap.development=" + Environment.DEVELOPMENT

        //Initialize marshallers and services
        marshallersService.initMarshallers()
        sequenceService.initSequences()
        triggerService.initTrigger()
        indexService.initIndex()
        grantService.initGrant()
        tableService.initTable()
        termService.initialize()
        retrieveErrorsService.initMethods()

        /* Print JVM infos like XMX/XMS */
        List inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (int i = 0; i < inputArgs.size(); i++) {
            log.info inputArgs.get(i)
        }

        /* Fill data just in test environment*/
        if (Environment.getCurrent() == Environment.TEST) {
            bootstrapTestDataService.initData()
        }

        //if database is empty, put minimal data
        if (SecUser.count() == 0 && Environment.getCurrent() != Environment.TEST) {
            bootstrapTestDataService.initData()
        }

        if(!SecUser.findByUsername("admin")) {
            bootstrapUtilsService.createUsers([[username : 'admin', firstname : 'Admin', lastname : 'Master', email : 'lrollus@ulg.ac.be', group : [[name : "GIGA"]], password : '123admin456', color : "#FF0000", roles : ["ROLE_USER", "ROLE_ADMIN"]]])
        }

        JSONUtils.registerMarshallers()


        //comment this after first exec
        if(Environment.getCurrent() != Environment.TEST) {
            bootstrapTestDataService.initSoftwareAndJobTemplate(57l)
        } else {
            bootstrapTestDataService.initSoftwareAndJobTemplate(BasicInstanceBuilder.getProjectNotExist(true).id)
        }

    }

    def saveDomain(def newObject, boolean flush = true) {
        newObject.checkAlreadyExist()
        if (!newObject.validate()) {
            log.error newObject.errors
            log.error newObject.retrieveErrors().toString()
            throw new WrongArgumentException(newObject.retrieveErrors().toString())
        }
        if (!newObject.save(flush: flush)) {
            throw new InvalidRequestException(newObject.retrieveErrors().toString())
        }
    }






}
