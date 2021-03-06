angular.module('processModule', ['taskModule', 'coreApp'])

    .factory('processRest', function ($log, coreApp, $resource) {
        var restProcessUrl = coreApp.getRestUrl() + 'processes/';
        var restOperationUrl = coreApp.getRestUrl() + 'operation/';
        var rawInterceptor = coreApp.getRawInterceptor();

        return $resource(restProcessUrl + 'process/:processId', {}, {
                getTree: {url: '/rest/console/tree/process/:processId/:startTaskId', params: {}},
                //list
                query: {url: restProcessUrl + 'search', params: {}, isArray: false},
                queryList: {url: restProcessUrl, params: {}},
                //actions
                create: {url: restOperationUrl + 'process/create', method: 'PUT', params: {}, interceptor:rawInterceptor},
                recovery:  {url: restOperationUrl + 'recovery/add',method: 'POST', interceptor:rawInterceptor},
                clone: {url: restOperationUrl + 'process/clone', method: 'POST', interceptor:rawInterceptor},
                abort: {url: restOperationUrl + 'process/abort',method: 'POST', interceptor:rawInterceptor},
                //dictionaries
                dictionaryState: {url: '/scripts/process/states.json', params: {}, isArray: true, cache: true}
            }
        );
    })

    .filter('processState', function (processRest,coreApp) {
        var states;
        processRest.dictionaryState(function(list){
             states = coreApp.toObject(list);
        });
        return function (id,field) {
            if(!states){ return '...'; }
            return states[id] ? states[id][field] : states.unknown[field];
        };
    })

    .controller('processListController', function ($log, $scope, processRest, coreApp, util) {
        $log.info('processListController');

        $scope.getFullActorId = util.getFullActorId;

        function getRest(params) {
            return (params.processId || params.customId) ? processRest.query : processRest.queryList;
        }

        function loadModel(params) {
            $log.info('Load model', $scope.resourceParams = params);
            $scope.processesResource = getRest(params)(params,
                function success(value) {
                    $scope.processesModel = coreApp.parseListModel(value);//cause array or object
                    if($scope.processesModel){
                        $log.info('Successfully updated processes page');
                    }else{
                        coreApp.info('Processes not found');
                    }
                    coreApp.refreshRate(params, loadModel);
                }, function error(reason) {
                    coreApp.error('Processes page update failed',reason);
                });
        }

        //Initialization:
        $scope.formParams = coreApp.copyStateParams();
        $scope.statuses = processRest.dictionaryState(function success(){
            loadModel(angular.copy($scope.formParams)); //separate form params
        });

        //Submit form command:
        $scope.search = function () {
            $scope.formParams.pageNum = undefined;
            $scope.formParams.refreshRate = undefined;
            coreApp.reloadState($scope.formParams);
        };

        //Finalization:
        $scope.$on('$destroy', function () {
            coreApp.stopRefreshRate();
        });

        //Actions
        $scope.showStartTask = function (process) {
            coreApp.openPropertiesModal(process.startTask,'Process start task');
        };

        $scope.recovery = function (process) {
            coreApp.openConfirmModal('Process will be sent to recovery service.',
                function confirmed() {
                    processRest.recovery(process.processId, function success (value) {
                        $log.log('Process recovered', value);
                        process.$recoverySubmited = true;
                        loadModel($scope.resourceParams);
                    }, function error(reason) {
                        coreApp.error('Processes recovery error',reason);
                    });
                });
        };

        $scope.abort = function (process) {
            coreApp.openConfirmModal('Current process, it\'s graph, all tasks and decisions will be deleted.',
                function confirmed() {
                    processRest.abort(process.processId, function success(value) {
                        $log.log('Process abort success', value);
                        loadModel($scope.resourceParams);
                    }, function error(reason) {
                        coreApp.error('Process abort error',reason);
                    });
                });
        };

    })

    .controller('processCardController', function ($log, $scope, processRest, coreApp, coreTree, $state, $stateParams, util) {
        $scope.processParams = angular.copy($stateParams);
        $log.info('processCardController',$scope.processParams);

        $scope.getFullActorId = util.getFullActorId;

        function loadModel() {
            $scope.process = processRest.get($scope.processParams,
                function success(value) {
                    if(value.processId) {
                        $log.info('processCardController: successfully updated process content');
                    }else{
                        coreApp.warn('Process not found by id',$scope.processParams.processId);
                    }
                    //@todo bug not work $scope.processParams.startTaskId = $scope.process.startTaskUuid;
                    $scope.processParams.startTaskId = 'undefined';
                    loadTreeModel();
                }, function error(reason) {
                    coreApp.error('Process update failed',reason);
                });

        }

        function loadTreeModel() {
            $scope.taskTree = processRest.getTree($scope.processParams,
                function success(value) {
                    $log.info('processCardController: successfully updated process tree');
                },
                function error(reason) {
                    coreApp.error('Process tree update failed',reason);
                });
        }

        //Initialization:
        loadModel();

        //Actions
        $scope.showStartTask = function (process) {
            coreApp.openPropertiesModal(process.startTask,'Process start task');
        };

        $scope.recovery = function (process) {
            coreApp.openConfirmModal('Process will be sent to recovery service.',
                function confirmed() {
                    processRest.recovery(process.processId,
                        function success (value) {
                            $log.log('Process recovered', value);
                            loadModel();
                        }, function error(reason) {
                             coreApp.error('Process recovery error',reason);
                        });
                });
        };

        $scope.clone = function (process) {
            coreApp.openConfirmModal('A new process with the same start task arguments would be created and this process would be aborted',
                function confirmed() {
                    processRest.clone(process.processId,
                        function success(value) {
                            $log.log('Process restart success', value);
                            $state.go('process', {processId: value});
                        }, function error(reason) {
                            coreApp.error('Process restart error',reason);
                        });
                });
        };

        $scope.abort = function (process) {
            coreApp.openConfirmModal('Current process, it\'s graph, all tasks and decisions will be deleted.',
                function confirmed() {
                    processRest.abort(process.processId, function success(value) {
                        $log.log('Process abort success', value);
                        loadModel();
                    }, function error(reason) {
                        coreApp.error('Process abort error',reason);
                    });
                });
        };

    })

    .controller('processCreateController', function ($scope, $log, coreApp, processRest, $stateParams, $state) {

        $scope.task = angular.copy($stateParams);
        $log.info('processCreateController',$scope.task);
        $scope.types = ['DECIDER_START'];

        $scope.isValidForm = function() {
            return $scope.task.actorId && $scope.task.method;
        };

        //actions
        $scope.save = function(){
            $log.log('Try to create process with params', $scope.task);
            processRest.create($scope.task,
                function success(value) {
                    $log.log('Process create success', value);
                    $state.go('process', {processId: value});
                }, function error(reason) {
                    coreApp.error('Process create error',reason);
                });
        };

    });
