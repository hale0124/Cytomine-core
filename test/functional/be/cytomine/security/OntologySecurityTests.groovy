package be.cytomine.security

/*
* Copyright (c) 2009-2020. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.ontology.Ontology
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.OntologyAPI
import be.cytomine.test.http.ProjectAPI
import grails.converters.JSON

class OntologySecurityTests extends SecurityTestsAbstract {


    void testOntologySecurityForCytomineAdmin() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Get admin user
        User admin = BasicInstanceBuilder.getSuperAdmin(USERNAMEADMIN,PASSWORDADMIN)

        //Create new ontology (user1)
        def result = OntologyAPI.create(BasicInstanceBuilder.getOntologyNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Ontology ontology = result.data
        println "ontology="+ontology
        println "ontology.id="+ontology.id
        Infos.printRight(ontology)
        Infos.printUserRight(user1)
        Infos.printUserRight(admin)
        //check if admin user can access/update/delete
        assert (200 == OntologyAPI.show(ontology.id,USERNAMEADMIN,PASSWORDADMIN).code)
        assert (true ==OntologyAPI.containsInJSONList(ontology.id,JSON.parse(OntologyAPI.list(USERNAMEADMIN,PASSWORDADMIN).data)))
        assert (200 == OntologyAPI.update(ontology.id,ontology.encodeAsJSON(),USERNAMEADMIN,PASSWORDADMIN).code)
        assert (200 == OntologyAPI.delete(ontology.id,USERNAMEADMIN,PASSWORDADMIN).code)
    }

    void testOntologySecurityForOntologyCreator() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Create new Ontology (user1)
        def result = OntologyAPI.create(BasicInstanceBuilder.getOntologyNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Ontology ontology = result.data

        //check if user 1 can access/update/delete
        assert (200 == OntologyAPI.show(ontology.id,USERNAME1,PASSWORD1).code)
        assert (true ==OntologyAPI.containsInJSONList(ontology.id,JSON.parse(OntologyAPI.list(USERNAME1,PASSWORD1).data)))
        assert (200 == OntologyAPI.update(ontology.id,ontology.encodeAsJSON(),USERNAME1,PASSWORD1).code)
        assert (200 == OntologyAPI.delete(ontology.id,USERNAME1,PASSWORD1).code)
    }

    void testOntologySecurityForProjectUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        Ontology ontologyToAdd = BasicInstanceBuilder.getOntologyNotExist()

        //Create new Ontology (user1)
        def result = OntologyAPI.create(ontologyToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Ontology ontology = result.data

        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        project.ontology = ontology
        BasicInstanceBuilder.saveDomain(project)

        //TODO: try with USERNAME1 & PASSWORD1
        def resAddUser = ProjectAPI.addAdminProject(project.id,user1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        resAddUser = ProjectAPI.addUserProject(project.id,user2.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        Infos.printRight(ontology)
        //check if user 2 can access/update/delete
        assert (200 == OntologyAPI.show(ontology.id,USERNAME2,PASSWORD2).code)
        assert (true ==OntologyAPI.containsInJSONList(ontology.id,JSON.parse(OntologyAPI.list(USERNAME2,PASSWORD2).data)))
        assert (403 == OntologyAPI.update(ontology.id,ontology.encodeAsJSON(),USERNAME2,PASSWORD2).code)


        //remove right to user2
        resAddUser = ProjectAPI.deleteUserProject(project.id,user2.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code

        Infos.printRight(ontology)
        //currently user 2 still can access to ontology
        assert (200 == OntologyAPI.show(ontology.id,USERNAME2,PASSWORD2).code)
        assert(true==OntologyAPI.containsInJSONList(ontology.id,JSON.parse(OntologyAPI.list(USERNAME2,PASSWORD2).data)))
        //check if user 2 cannot update/delete
        assert (403 == OntologyAPI.update(ontology.id,ontology.encodeAsJSON(),USERNAME2,PASSWORD2).code)

        //delete project because we will try to delete ontology
        def resDelProj = ProjectAPI.delete(project.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert (200 == resDelProj.code)


        assert (403 == OntologyAPI.delete(ontology.id,USERNAME2,PASSWORD2).code)
    }


    void testOntologySecurityForGhest() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User ghest = BasicInstanceBuilder.getGhest("GHESTONTOLOGY","PASSWORD")

        //Create new Ontology (user1)
        def result = OntologyAPI.create(BasicInstanceBuilder.getOntologyNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Ontology ontology = result.data
        Infos.printRight(ontology)
        //check if user 2 cannot access/update/delete
        assert (403 == OntologyAPI.show(ontology.id,"GHESTONTOLOGY","PASSWORD").code)
        assert (false==OntologyAPI.containsInJSONList(ontology.id,JSON.parse(OntologyAPI.list("GHESTONTOLOGY","PASSWORD").data)))
        assert (403 == OntologyAPI.update(ontology.id,ontology.encodeAsJSON(),"GHESTONTOLOGY","PASSWORD").code)
        assert (403 == OntologyAPI.delete(ontology.id,"GHESTONTOLOGY","PASSWORD").code)
        assert (403 == OntologyAPI.create(BasicInstanceBuilder.getOntologyNotExist().encodeAsJSON(),"GHESTONTOLOGY","PASSWORD").code)

    }


    void testOntologySecurityForSimpleUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new Ontology (user1)
        def result = OntologyAPI.create(BasicInstanceBuilder.getOntologyNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Ontology ontology = result.data
        Infos.printRight(ontology)
        //check if user 2 cannot access/update/delete
        assert (403 == OntologyAPI.show(ontology.id,USERNAME2,PASSWORD2).code)
        assert (false==OntologyAPI.containsInJSONList(ontology.id,JSON.parse(OntologyAPI.list(USERNAME2,PASSWORD2).data)))
        assert (403 == OntologyAPI.update(ontology.id,ontology.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == OntologyAPI.delete(ontology.id,USERNAME2,PASSWORD2).code)

    }

    void testOntologySecurityForAnonymous() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Create new Ontology (user1)
        def result = OntologyAPI.create(BasicInstanceBuilder.getOntologyNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Ontology ontology = result.data
        Infos.printRight(ontology)
        //check if user 2 cannot access/update/delete
        assert (401 == OntologyAPI.show(ontology.id,USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == OntologyAPI.list(USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == OntologyAPI.update(ontology.id,ontology.encodeAsJSON(),USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == OntologyAPI.delete(ontology.id,USERNAMEBAD,PASSWORDBAD).code)
    }



    void testOntologySecurityForUserRemovedFromProject() {

        //if user 1 is in project 1 (ontology 1) and project 2 (ontology 1), it must have access to ontology 1 if removed from project 1 or 2.

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        Ontology ontologyToAdd = BasicInstanceBuilder.getOntologyNotExist()

        //Create new Ontology (user1)
        def result = OntologyAPI.create(ontologyToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Ontology ontology = result.data

        Project project1 = BasicInstanceBuilder.getProjectNotExist(true)
        project1.ontology = ontology
        BasicInstanceBuilder.saveDomain(project1)
        Project project2 = BasicInstanceBuilder.getProjectNotExist(true)
        project2.ontology = ontology
        BasicInstanceBuilder.saveDomain(project2)

        def resAddUser = ProjectAPI.addUserProject(project1.id,user2.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        resAddUser = ProjectAPI.addUserProject(project2.id,user2.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code

        //check if user 2 can access/update/delete
        assert (200 == OntologyAPI.show(ontology.id,USERNAME2,PASSWORD2).code)
        assert (true ==OntologyAPI.containsInJSONList(ontology.id,JSON.parse(OntologyAPI.list(USERNAME2,PASSWORD2).data)))

        //remove right to user2
        resAddUser = ProjectAPI.deleteUserProject(project1.id,user2.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code

        //check if user 2 can access/update/delete, he is still in project 2!!!!!!
        assert (200 == OntologyAPI.show(ontology.id,USERNAME2,PASSWORD2).code)
        assert (true ==OntologyAPI.containsInJSONList(ontology.id,JSON.parse(OntologyAPI.list(USERNAME2,PASSWORD2).data)))

        resAddUser = ProjectAPI.deleteUserProject(project2.id,user2.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code

        //check if user 2 cannot access/update/delete
        assert (200 == OntologyAPI.show(ontology.id,USERNAME2,PASSWORD2).code)
        assert(true==OntologyAPI.containsInJSONList(ontology.id,JSON.parse(OntologyAPI.list(USERNAME2,PASSWORD2).data)))
    }

    void testUpdateWhenOntologySharedBetweenProjects() {

        //if ontology is used into two projects, modifications can be made only if current user is manager of the two projects.

        //Get user
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        Ontology ontology = BasicInstanceBuilder.getOntologyNotExist(true)

        Project project1 = BasicInstanceBuilder.getProjectNotExist(true)
        project1.ontology = ontology
        BasicInstanceBuilder.saveDomain(project1)
        Project project2 = BasicInstanceBuilder.getProjectNotExist(true)
        project2.ontology = ontology
        BasicInstanceBuilder.saveDomain(project2)

        assert 200 == ProjectAPI.addAdminProject(project1.id,user1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code
        assert 200 == ProjectAPI.addUserProject(project2.id,user1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //check if user1 can access
        assert (200 == OntologyAPI.show(ontology.id,USERNAME1,PASSWORD1).code)
        assert (true ==OntologyAPI.containsInJSONList(ontology.id,JSON.parse(OntologyAPI.list(USERNAME1,PASSWORD1).data)))

        //check if user1 cant update/delete the ontology because he's not manager of project2
        assert (403 == OntologyAPI.update(ontology.id,ontology.encodeAsJSON(),USERNAME1,PASSWORD1).code)
        assert (400 == OntologyAPI.delete(ontology.id,USERNAME1,PASSWORD1).code)

        //user1 now manager of project2
        assert 200 == ProjectAPI.addAdminProject(project2.id,user1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //check if user1 can update/delete the ontology because he's manager of both projects
        assert (200 == OntologyAPI.update(ontology.id,ontology.encodeAsJSON(),USERNAME1,PASSWORD1).code)
        assert (400 == OntologyAPI.delete(ontology.id,USERNAME1,PASSWORD1).code)

        assert 200 == ProjectAPI.delete(project1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code
        assert 200 == ProjectAPI.delete(project2.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code
        assert (200 == OntologyAPI.delete(ontology.id,USERNAME1,PASSWORD1).code)
    }
}
