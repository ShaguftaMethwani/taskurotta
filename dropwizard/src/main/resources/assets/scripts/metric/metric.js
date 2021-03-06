angular.module('metricModule', ['coreApp'])

    .factory('metricRest', function ($log, coreApp, $resource) {
        var restMetricUrl = coreApp.getRestUrl() + 'metrics/';

        return $resource(restMetricUrl + 'data/', {}, {
                //dictionaries
                dictionaryOptions: {url: restMetricUrl + 'options/', params: {}, cache: true}
                //dictionaryOptions: {url: '/scripts/metric/options.json', params: {}, cache: true}
            }
        );
    })


    .controller('metricListController', function ($log, $scope, metricRest, smoothRates, legendPositions, coreApp, $state,$stateParams) {
        $log.info('metricListController');

        function loadModel(params) {
            $log.info('Load model', $scope.resourceParams = params);
            $scope.metricsResource =  metricRest.query(params,
                function success(value) {
                    $scope.metricsModel = value; //cause array or object
                    if ($scope.metricsModel) {
                        $log.info('Successfully updated metrics data');
                    } else {
                        coreApp.info('Metrics data not found');
                    }
                    coreApp.refreshRate(params, loadModel);
                }, function error(reason) {
                    coreApp.error('Metrics data update failed', reason);
                });
        }


        //Initialization:
        $scope.smoothRates = smoothRates;
        $scope.legendPositions = legendPositions;

        $scope.formParams = coreApp.copyStateParams();
        $scope.$stateParams = coreApp.getStateParams();
        $scope.$stateParams.showLegend = !!$scope.$stateParams.legend;
        $scope.formParams.dataset = coreApp.parseObjectParam($scope.formParams.dataset);

        $scope.options = metricRest.dictionaryOptions({},
            function success(options) {
                $log.log('Loaded metric options dictionary', options);
                //function for validate form
                $scope.isValidForm = function() {
                    var params = $scope.formParams;
                    return params.metric &&
                        angular.isObject(params.dataset) &&
                        _.find(options.scopes[params.metric],
                            function(scope){ return scope.value === params.scope; }) &&
                        _.find(options.dataTypes[params.metric],
                            function(type){ return type.value === params.type; }) &&
                        _.find(options.periods[params.metric],
                            function(period){ return period.value === params.period; }) &&
                        _.find(params.dataset,
                            function(dataset){ return dataset === true; });
                };

                //function for clear form
                $scope.clearForm = function(){
                    if($scope.formParams.metric) {
                        $log.debug('clear form');
                        var scopes = $scope.options.scopes[$scope.formParams.metric];
                        var dataTypes = $scope.options.dataTypes[$scope.formParams.metric];
                        var periods = $scope.options.periods[$scope.formParams.metric];
                        if (!_.find(scopes,function(scope){
                                return scope.value === $scope.formParams.scope; })) {
                            $scope.formParams.scope = scopes[0].value;
                        }
                        if (! _.find(dataTypes,function(type){
                                return type.value === $scope.formParams.type; })) {
                            $scope.formParams.type = dataTypes[0].value;
                        }
                        if (! _.find(dataTypes,function(period){
                                return period.value === $scope.formParams.period; })) {
                            $scope.formParams.period = periods[0].value;
                        }
                        $scope.formParams.dataset = {};
                    }
                };

                if(options.metrics.length){
                    if($scope.isValidForm()){
                        loadModel(angular.extend({},$scope.formParams, {
                            dataset: coreApp.getKeys($scope.formParams.dataset).join(',')
                        }));
                    }
                }else{
                    coreApp.error('No available metrics to show', options);
                }

            }, function error(reason) {
                coreApp.error('Metrics options dictionary update failed', reason);
            });



        //Update command:
        $scope.search = function () {
            coreApp.reloadState(angular.extend({},$scope.formParams,{
                refreshRate: undefined,
                dataset: coreApp.stringifyObjectParam($scope.formParams.dataset)
            }));
        };

        //Finalization:
        $scope.$on('$destroy', function () {
            coreApp.stopRefreshRate();
        });


    })

    .directive('metricsPlot', function ( $log, metricsFormatters) {
        function getOptions(params, xFormatter, yFormatter){
            var xFormatterFunc = xFormatter ? metricsFormatters[xFormatter] : undefined;
            var yFormatterFunc = yFormatter ? metricsFormatters[yFormatter] : undefined;
            return {
                zoom: {interactive: (params && params.zoom) || false},
                pan: {interactive: (params && params.pan) || false},
                xaxis: {
                    tickFormatter: xFormatterFunc || undefined
                },
                yaxis: {
                    tickFormatter: yFormatterFunc || undefined
                },
                legend: {
                    show: (params && params.showLegend) || false,
                    backgroundOpacity: 0.2,
                    position: (params && params.legend) ? params.legend : 'ne'
                    //labelFormatter: function (label, series) {
                    //    var cb = '<input class='legendCB' type='checkbox' ';
                    //    if (series.data.length > 0){
                    //        cb += 'checked='true' ';
                    //    }
                    //    cb += 'id=''+label+'' /> ';
                    //    cb += label;
                    //    return cb;
                    //}
                },
                grid: {
                    hoverable: true,
                    clickable: true,
                    backgroundColor: { colors: ['#ddd', '#fff'] }
                },
                series: {
                    lines: {show: true ,  lineWidth: 2},
                    points: {show: (params && params.points) || false, radius: 3 }
                }
            };
        }
        var style = {
            position: 'absolute',
            display: 'none',
            border: '1px solid #fdd',
            padding: '1px',
            backgroundColor: '#FFA801',
            opacity: 0.80,
            fontSize: '12px'
        };

        return {
            restrict: 'CA',//Class, Attribute
            terminal: true,
            scope: {
                model: '=metricsPlot',
                width: '@',
                height: '@',
                params: '='
            },
            controller: function ($scope, $element, $attrs) {

                var jPlot = $($element);

                //Setting css width and height if explicitly set
                if($scope.width) { jPlot.css('width', $scope.width); }
                if($scope.height) { jPlot.css('height', $scope.height); }

                $('<div id="metrics-tooltip"></div>').css(style).appendTo('body');

                var plotElem = $.plot(jPlot, [], getOptions());

                jPlot.bind('plothover', function (event, pos, item) {

                    if ($scope.model.length>0) {
                        var posX = metricsFormatters.getFormattedValue($scope.model[0].xFormatter, pos.x, false);
                        var posY = metricsFormatters.getFormattedValue($scope.model[0].yFormatter, pos.y, false);

                       

                        //$log.info('$scope.mousePosition',$scope.mousePosition);
                        if (item) {

                            var xVal = metricsFormatters.getFormattedValue($scope.model[0].xFormatter, item.datapoint[0], false);
                            var yVal = metricsFormatters.getFormattedValue($scope.model[0].yFormatter, item.datapoint[1], false);
                            var position = '(' + xVal + ', ' + yVal + ')';

                            $('#metrics-tooltip').html(item.series.label + '<br/>' + position)
                                .css({top: item.pageY+5, left: item.pageX+5})
                                .fadeIn(200);
                            $('#metrics-hoverdata').html(position +
                                ' <span class="muted">Dataset:</span> ' + item.series.label );
                        } else {

                            $('#metrics-tooltip').hide();
                            $('#metrics-hoverdata').text('(' + posX + ', ' + posY + ')');
                        }
                    }

                });

                var updatePlotData = function(model, params) {
                    if(model && model.length>0){
                        var options = getOptions(params, model[0].xFormatter, model[0].yFormatter);
                        //$log.info('Plot options:',options);
                        plotElem = $.plot(jPlot, model, options );
                    }else{
                        $log.info('Clear datasets');
                        plotElem = $.plot(jPlot, [], getOptions());
                    }
                };

                $scope.$watch('model', function (newData) {
                   // $log.info('$watch(model)',newData);
                    if(newData.$resolved) {
                        $log.info('Updated plot datasets:');
                        updatePlotData(newData,$scope.params);
                    }
                },false);

                $scope.$watch('params', function (newParams, oldParams) {
                    if(newParams!==oldParams) {
                        $log.info('Updated plot params:', newParams);
                        updatePlotData($scope.model,newParams);
                   }
                }, true);

            },
            replace: true
        };
    })

    .factory('metricsFormatters', function ( $log) {
        var resultService = {
            time: function (val, axis) {
                return moment(new Date(val)).format('DD/MM HH:mm');
            },
            memory: function (bytes, axis) {
                if (!bytes || isNaN(parseFloat(bytes)) || !isFinite(bytes)) {
                    return '-';
                }
                var units = [' B', ' KB', ' MB', ' GB', ' TB', ' PB'],
                    number = Math.floor(Math.log(bytes) / Math.log(1024));
                var value = (bytes / Math.pow(1024, Math.floor(number))).toFixed(1);
                var metric = units[number];
                return (value || '') + ' ' + (metric || '');
            },
            getFormattedValue: function(formatterName, value, axis) {
                var result = value;
                if (formatterName && this[formatterName]) {
                    result = this[formatterName](value, axis);
                }
                return result;
            }
        };

        return resultService;
    })
;
