var EditProjectDialog = Backbone.View.extend({
    projectsPanel: null,
    editProjectDialog: null,
    projectMultiSelectAlreadyLoad: false,
    userMaggicSuggest : null,
    initialize: function (options) {
        this.container = options.container;
        this.projectPanel = options.projectPanel;
        _.bindAll(this, 'render');
    },
    render: function () {
        var self = this;
        require([
                "text!application/templates/project/ProjectEditDialog.tpl.html"
            ],
            function (projectEditDialogTpl) {
                self.doLayout(projectEditDialogTpl);
            });
        return this;
    },
    doLayout: function (projectEditDialogTpl) {
        var self = this;
        $("#editproject").replaceWith("");
        $("#addproject").replaceWith("");
        var dialog = _.template(projectEditDialogTpl, {});
        $(self.el).append(dialog);

        $("#editProjectButton").click(function (event) {
            event.preventDefault();
            $("#login-form-edit-project").submit();
            return false;
        });
        $("#closeEditProjectDialog").click(function (event) {
            event.preventDefault();
            $("#editproject").modal('hide');
            $("#editproject").remove();
            return false;
        });

        self.initStepy();
        self.createProjectInfo();
        self.createUserList();
        $("#project-edit-name").val(self.model.get('name'));
//        self.createUserList(usersChoicesTpl);

        //Build dialog
        self.editProjectDialog = $("#editproject").modal({
            keyboard: true,
            backdrop: true
        });
        self.open();
        self.fillForm();
        return this;

    },
    initStepy: function () {
        $('#login-form-edit-project').stepy({next: function (index) {
            //check validate name
            if (index == 2) {
                if ($("#project-edit-name").val().toUpperCase().trim() == "") {
                    window.app.view.message("User", "You must provide a valide project name!", "error");
                    return false;
                }
            }
            //show save button on last step
            if (index == $("#login-form-edit-project").find("fieldset").length) {
                $("#editProjectButton").show();
            }
        }, back: function (index) {
            //hide save button if not on last step
            if (index != $("#login-form-edit-project").find("fieldset").length) {
                $("#editProjectButton").hide();
            }
        }});
        $('#login-form-edit-project').find("fieldset").find("a.button-next").css("float", "right");
        $('#login-form-edit-project').find("fieldset").find("a.button-back").css("float", "left");
        $('#login-form-edit-project').find("fieldset").find("a").removeClass("button-next");
        $('#login-form-edit-project').find("fieldset").find("a").removeClass("button-back");
        $('#login-form-edit-project').find("fieldset").find("a").addClass("btn btn-default btn-primary");
    },
    createProjectInfo: function () {
        var self = this;
        $("#login-form-edit-project").submit(function () {
            self.editProject();
            return false;
        });
        $("#login-form-edit-project").find("input").keydown(function (e) {
            if (e.keyCode == 13) { //ENTER_KEY
                $("#login-form-edit-project").submit();
                return false;
            }
        });


    },
    createUserList: function () {
        var self = this;
        var allUser = null;
        var projectUser = null;
        var projectAdmin = null;


        var loadUser = function() {
            if(allUser == null || projectUser == null/* || defaultLayers == null*/) {
                return;
            }
            var allUserArray = [];

            allUser.each(function(user) {
                allUserArray.push({id:user.id,label:user.prettyName()});
            });

            var projectUserArray=[]
            projectUser.each(function(user) {
                projectUserArray.push(user.id);
            });

            var projectAdminArray=[]
            projectAdmin.each(function(user) {
                projectAdminArray.push(user.id);
            });

            self.userMaggicSuggest = $('#projectedituser').magicSuggest({
                data: allUserArray,
                displayField: 'label',
                value: projectUserArray,
                width: 590,
                maxSelection:null
            });

            self.adminMaggicSuggest = $('#projecteditadmin').magicSuggest({
                data: allUserArray,
                displayField: 'label',
                value: projectAdminArray,
                width: 590,
                maxSelection:null
            });
        }

        new UserCollection({}).fetch({
            success: function (allUserCollection, response) {
                allUser = allUserCollection;
                loadUser();
            }});

        new UserCollection({project: self.model.id}).fetch({
            success: function (projectUserCollection, response) {
                projectUser = projectUserCollection;
                window.app.models.projectUser = projectUserCollection;
                loadUser();
            }});

        new UserCollection({project: self.model.id, admin:true}).fetch({
            success: function (projectUserCollection, response) {
                projectAdmin = projectUserCollection;
                window.app.models.projectAddmin = projectUserCollection;
                loadUser();
            }});

    },
    fillForm: function () {

        var self = this;
        //fill project Name

    },
    refresh: function () {
    },
    open: function () {
        var self = this;
        self.clearEditProjectPanel();
        $("#editproject").modal('show');
    },
    clearEditProjectPanel: function () {
        var self = this;

        $("#projectediterrormessage").empty();
        $("#projectediterrorlabel").hide();
//        $("#project-edit-name").val("");

        //$(self.editProjectCheckedUsersCheckboxElem).attr("checked", false);
    },
    /**
     * Function which returns the result of the subtraction method applied to
     * sets (mathematical concept).
     *
     * @param a Array one
     * @param b Array two
     * @return An array containing the result
     */
    diffArray: function (a, b) {
        var seen = [], diff = [];
        for (var i = 0; i < b.length; i++) {
            seen[b[i]] = true;
        }
        for (var i = 0; i < a.length; i++) {
            if (!seen[a[i]]) {
                diff.push(a[i]);
            }
        }
        return diff;
    },
    editProjectDefaultLayers : function(projectId, users) {
        console.log("editProjectDefaultLayers");
        console.log(projectId);
        console.log(users);
        for(var i = 0; i< users.length ; i++) {
            console.log(users[i]);
        }
        new ProjectDefaultLayerCollection({project: projectId}).fetch({
            success: function (collection) {
                collection.each(function(layer) {
                    if(users.indexOf(layer.attributes.user) == -1) {
                        console.log("deletion de ");
                        console.log(layer.id);
                        layer.destroy();
                    }
                });
            }
        });
    },
    editProject: function () {

        var self = this;

        $("#projectediterrormessage").empty();
        $("#projectediterrorlabel").hide();

        var name = $("#project-edit-name").val().toUpperCase();
        var users = self.userMaggicSuggest.getValue();
        var admins = self.adminMaggicSuggest.getValue();
        var divToFill = $("#login-form-edit-project");
        divToFill.hide();
//        $("#login-form-add-project-titles").empty();
        $("#editproject").find(".modal-footer").hide();


        var project = self.model;
        new TaskModel({project: null}).save({}, {
            success: function (task, response) {
                $("#progressBarEditProjectContainer").append('<br><br><div id="task-' + response.task.id + '"></div><br><br>');
                console.log(response.task);
                var timer = window.app.view.printTaskEvolution(response.task, $("#progressBarEditProjectContainer").find("#task-" + response.task.id), 1000);
                console.log(response);
                console.log(response.task);
                console.log(response.task.id);
                var taskId = response.task.id;
                //create project
                project.task = taskId
                project.set({users: users, admins:admins,name: name});
                project.save({users:users, admins:admins, name: name}, {
                    success: function (model, response) {
                        console.log("1. Project edited!");
                        clearInterval(timer);
                        window.app.view.message("Project", response.message, "success");
                        var id = response.project.id;
                        self.editProjectDefaultLayers(id, users.concat(admins));
                        $("#editproject").modal("hide");
                        window.app.controllers.dashboard.destroyView()
                        window.app.controllers.browse.closeAll();
                        window.app.status.currentProject = undefined;
                        window.app.view.clearIntervals();
                        /*$("#editproject").remove();*/
                    },
                    error: function (model, response) {
                        var json = $.parseJSON(response.responseText);
                        clearInterval(timer);
                        window.app.view.message("Project", json.errors, "error");
                        divToFill.show();
                        $("#progressBarEditProjectContainer").empty();
                        $("#editproject").find(".modal-footer").show();
                        $('#login-form-edit-project').stepy('step', 1);
                    }
                });
            }
        });
    }
});