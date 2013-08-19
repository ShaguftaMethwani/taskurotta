var consoleDirectives = angular.module("console.directives", []);

consoleDirectives.directive('tskPaginator', ['$http', function ($http) {
    return {
        restrict: 'ECA',//Element, Class, Attribute
        terminal: true,
        scope: {
            genericPage: "=genericPage",
            updateAction: "&updateAction"
        },
        controller: ['$scope', '$element', '$attrs', '$transclude', function ($scope, $element, $attrs, $transclude) {

            $scope.updatePageSize = function () {

                //If user is on the last page and change pageSize, we can get situation when current page number > total pages count
                if ($scope.genericPage.pageNumber > $scope.totalPages()) {
                    $scope.genericPage.pageNumber = $scope.totalPages();
                }

                $scope.updateAction();
            }

            //Show previous page
            $scope.prevPage = function () {
                if ($scope.genericPage.pageNumber > 1) {
                    $scope.genericPage.pageNumber--;
                }

                $scope.updateAction();
            };

            $scope.totalPages = function () {
                var reminder = $scope.genericPage.totalCount % $scope.genericPage.pageSize;
                var pagesCount = Math.floor($scope.genericPage.totalCount / $scope.genericPage.pageSize);
                if (reminder > 0) {
                    pagesCount++;
                }
                return pagesCount
            };

            $scope.getMinIndex = function () {
                var minIndex = ($scope.genericPage.pageNumber - 1) * $scope.genericPage.pageSize + 1;
                if ($scope.genericPage.totalCount <= 0) {
                    minIndex = 0;
                }
                return minIndex;
            };

            $scope.getMaxIndex = function () {
                var maxIndex = $scope.genericPage.totalCount;
                if ($scope.genericPage.pageNumber < $scope.totalPages()) {
                    maxIndex = $scope.genericPage.pageNumber * $scope.genericPage.pageSize;
                }
                return maxIndex
            };

            //Show next page
            $scope.nextPage = function () {
                if ($scope.genericPage.pageNumber < $scope.totalPages()) {
                    $scope.genericPage.pageNumber++;
                }

                $scope.updateAction();
            };

            $scope.firstPage = function () {
                $scope.genericPage.pageNumber = 1;
                $scope.updateAction();
            };

            $scope.lastPage = function () {
                $scope.genericPage.pageNumber = $scope.totalPages();
                $scope.updateAction();
            };
        }],
        templateUrl: "/partials/widget/paginator_bar.html",
        replace: true
    };
}]);

consoleDirectives.directive('tskAutorefresh', ['$http', function ($http) {
    return {
        restrict: 'ECA',//Element, Class, Attribute
        terminal: true,
        scope: {
            updateAction: "&updateAction"
        },
        controller: ['$scope', '$element', '$attrs', '$transclude', '$$timeUtil', function ($scope, $element, $attrs, $transclude, $$timeUtil) {
            $scope.refreshRate = 0;

            //Auto refresh feature. triggers auto refreshing on refresh rate changes
            var currentRefreshIntervalId = -1;
            $scope.$watch(function () {
                return $scope.refreshRate;
            }, function (value) {
                if (currentRefreshIntervalId > 0) {
                    $$timeUtil.clearInterval(currentRefreshIntervalId);
                }
                if ($scope.refreshRate > 0) {
                    currentRefreshIntervalId = $$timeUtil.setInterval($scope.updateAction, $scope.refreshRate * 1000, $scope);//Start autoUpdate
                }
            }, true);

        }],
        templateUrl: "/partials/widget/auto_refresher.html",
        replace: true
    };
}]);
// <ol>
//     <li ng-repeat="child in data.children" tree>
//       <div>{{child.name}}</div>
//       <ol><branch></ol>
// </ol>
//https://gist.github.com/furf/4331090
consoleDirectives.directive('tskTree', ['$compile', function ($compile) {
    'use strict';
    return {
        restrict: 'A',
        compile: function (tElement, tAttrs) {

            var branch = tElement.find('branch'),
                repeatExpr,
                childExpr,
                childrenExpr;

            if (!branch.length) {
                throw new Error('tree directive must contain a branch node.');
            }

            repeatExpr = (branch.attr('branch') || tAttrs.ngRepeat).match(/^(.*) in (?:.*\.)?(.*)$/);
            childExpr = repeatExpr[1];
            childrenExpr = repeatExpr[2];
            tElement.attr('ng-repeat', childExpr + ' in ' + childExpr + '.' + childrenExpr);

            return function link (scope, element) {

                scope.$depth = scope.$depth || 0;
                scope.$watch(childExpr, function(child) {

                    var childScope = scope.$new();

                    childScope[childrenExpr] = child[childrenExpr];
                    childScope.$depth = scope.$depth + 1;

                    element.find('branch').replaceWith($compile(tElement.clone())(childScope));
                });
            };
        }
    };
}]);


consoleDirectives.directive('tskCreateProcessForm', ['$http', function ($http) {
    return {
        restrict: 'ECA',//Element, Class, Attribute
        terminal: true,
        scope: {},
        controller: ['$scope', '$element', '$attrs', '$transclude', '$$timeUtil', '$window', '$log', '$http', function ($scope, $element, $attrs, $transclude, $$timeUtil, $window, $log, $http) {

            $scope.process = {
                actorId: "",
                method: "",
                type: "DECIDER_START"
            };

            $scope.argsJson = "[]";

            $scope.create = function() {
                $scope.process.args = angular.fromJson($scope.argsJson);
                $log.log("create process: " + angular.toJson($scope.process));
                $http.post("/rest/tasks/start?generateId=true", $scope.process);
            };

        }],
        templateUrl: "/partials/widget/start_process.html",
        replace: true
    };
}]);
