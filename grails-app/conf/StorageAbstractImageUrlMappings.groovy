
/**
 * Cytomine @ GIGA-ULG
 * User: stevben
 * Date: 20/02/13
 * Time: 10:58
 */

class StorageAbstractImageUrlMappings {

    static mappings = {
        /* Storage */
        "/api/storage_abstract_image.$format"(controller: "restStorageAbstractImage"){
            action = [POST:"add"]
        }
        "/api/storage_abstract_image/$id.$format"(controller: "restStorageAbstractImage"){
            action = [DELETE:"delete"]
        }
    }
}