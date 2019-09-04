package be.cytomine.test.http

import be.cytomine.CytomineDomain
import be.cytomine.image.ImageInstance
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.ReviewedAnnotation
import be.cytomine.ontology.UserAnnotation
import be.cytomine.processing.Job
import be.cytomine.processing.Software
import be.cytomine.project.Project

/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
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

import be.cytomine.test.Infos
import grails.converters.JSON

class TagAPI extends DomainAPI {

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/tag/${id}.json"
        return doGET(URL, username, password)
    }

    static def list(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/tag.json"
        return doGET(URL, username, password)
    }

    static def create(String json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/tag.json"
        def result = doPOST(URL,json,username,password)
        result.data = Tag.get(JSON.parse(result.data)?.tag?.id)
        return result
    }

    static def update(def id, def json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/tag/${id}.json"
        return doPUT(URL,json,username,password)
    }

    static def delete(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/tag/${id}.json"
        return doDELETE(URL,username,password)
    }

    static def listByDomain(CytomineDomain domain, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/"
        // TODO when RESTE normalization replace the switch by domain.getClass().simpleName converted to snake case
        switch (domain.getClass()){
            case ImageInstance :
            case Project :
            case Software :
            case Job :
                URL += domain.getClass().simpleName.toLowerCase()+"/${domain.id}/"
                break;
            case UserAnnotation :
            case AlgoAnnotation :
            case ReviewedAnnotation :
                URL += "annotation/${domain.id}/"
                break;
        }
        URL += "tag.json"
        return doGET(URL, username, password)
    }

}
