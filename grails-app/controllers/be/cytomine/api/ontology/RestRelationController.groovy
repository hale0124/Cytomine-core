package be.cytomine.api.ontology

import be.cytomine.api.RestController
import be.cytomine.ontology.Relation
import jsondoc.annotation.ApiMethodLight
import org.jsondoc.core.annotation.*
import org.jsondoc.core.pojo.ApiParamType
import org.jsondoc.core.pojo.ApiVerb
import org.springframework.http.MediaType

/**
 * Controller for relation between terms (parent, synonym,...)
 * We only use "parent" now, but we could later implement CRUD to support new type of relation
 */
@Api(name = "relation services", description = "Methods for managing relations")
class RestRelationController extends RestController {


    def springSecurityService
    def relationService

    /**
     * List all relation available
     */
    @ApiMethodLight(description="List all relation available", listing = true)
    def list () {
        responseSuccess(relationService.list())
    }

    /**
     * Get a single relation with its id
     */
    @ApiMethodLight(description="Get a relation")
    @ApiParams(params=[
        @ApiParam(name="id", type="long", paramType = ApiParamType.PATH,description = "The relation id")
    ])
    def show () {
        Relation relation = relationService.read(params.long('id'))
        if (relation) {
            responseSuccess(relation)
        } else {
            responseNotFound("Relation", params.id)
        }
    }
}
