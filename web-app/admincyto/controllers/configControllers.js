/**
 * Created by hoyoux on 07.05.15.
 */
angular.module("cytomineUserArea")
    .controller("configCtrl", function ($scope,$location, $routeParams,$http, $resource,configService, tempSrvc) {

        $scope.configs = {error:{},success:{}};

        $scope.$on("$routeChangeSuccess", function () {
            $scope.configs.error = {};
            $scope.configs.success = {};
            $scope.getAllConfigs();
        });

        $scope.getAllConfigs = function() {

            configService.getAllConfigs(
                function(data) {
                    $scope.configs.data = data
                    for(var i=0;i<data.length;i++) {
                        $scope.configs[data[i].key] = data[i].value;
                    }
                }
            );
        };

        $scope.getAllConfigs();

        $scope.getConfig = function(key) {
            var configs = $scope.configs.data;
            for(var i=0;i<configs.length;i++) {
                if(configs[i].key == key){
                    return configs[i]
                }
            }
            return null;
        };

        $scope.saveWelcome = function() {
            var config = $scope.getConfig("WELCOME")
            var callback = function(data) {
                $scope.configs.successInfo = tempSrvc(4000);
                $scope.configs.successInfo.message = data.message
            };

            if(config != null) {
                config.value = $scope.configs.WELCOME
                configService.updateConfig("WELCOME", config, callback);
            } else {
                config = {key: "WELCOME", value: $scope.configs["WELCOME"]}
                configService.addConfig(config, callback);
            }



        }
        $scope.deleteWelcome = function() {
            var config = $scope.getConfig("WELCOME")
            var callback = function(data) {
                $scope.configs.successInfo = tempSrvc(4000);
                $scope.configs.successInfo.message = data.message
            };
            configService.deleteConfig(config, callback);
            $scope.configs.WELCOME=""
        }
    })
    .service('tempSrvc', ['$timeout', function($timeout) {

        return function(delay) {

            var result = {shown:true};
            $timeout(function() {
                result.shown=false;
            }, delay);
            return result;
        };

    }]);
