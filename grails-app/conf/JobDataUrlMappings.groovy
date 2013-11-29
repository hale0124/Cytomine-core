class JobDataUrlMappings {

    static mappings = {
        /* Job */
        "/api/jobdata.$fomat"(controller: "restJobData"){
            action = [GET:"list", POST:"add"]
        }
        "/api/jobdata/$id.$format"(controller: "restJobData"){
            action = [GET:"show", PUT:"update", DELETE:"delete"]
        }

        "/api/job/$id/jobdata.$format"(controller: "restJobData"){
            action = [GET:"listByJob"]
        }

        "/api/jobdata/$id/upload.$format"(controller:"restJobData"){
            action = [PUT:"upload", POST: "upload"]
        }

        "/api/jobdata/$id/download.$format"(controller:"restJobData"){
            action = [GET: "download"]
        }

        "/api/jobdata/$id/view.$format"(controller:"restJobData"){
            action = [GET: "view"]
        }
    }
}
